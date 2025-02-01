package scenes.game;

import java.util.function.Consumer;

import elem.MovingThings;
import engine.graphics.interactions.LobbyTopbar;
import engine.graphics.ui.modal.UIConfirmModal;
import engine.math.Vec3;
import main.Main;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import audio.AudioMaster;
import audio.SfxTypes;
import communication.GameInfo;
import communication.Translator;
import engine.graphics.interactions.TransparentTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.ui.CursorType;
import engine.graphics.ui.Font;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UIFont;
import engine.graphics.ui.UIScrollable;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import engine.utils.Timer;
import game_modes.CoopMode;
import game_modes.GameMode;
import game_modes.SingleplayerChallenges;
import game_modes.SingleplayerChallengesMode;
import main.Features;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import player_local.car.Car;
import player_local.car.CarStats;
import player_local.car.Rep;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Scene;
import scenes.adt.Visual;
import scenes.game.lobby_subscenes.UpgradesSubscene;
import scenes.game.racing_subscenes.FinishVisual;
import scenes.game.racing_subscenes.RaceVisual;
import scenes.game.racing_subscenes.WinVisual;
import settings_and_logging.hotkeys.RaceKeys;

/**
 * Kj�r thread p� lobby n�r du skal tilbake
 *
 * @author jonah
 */
public class Race extends Scene {

	/**
	 * Generated value
	 */

	public static final int CHEATED_TOO_EARLY = -1, CHEATED_GAVE_IN = -2, CHEATED_NOT = 0;

	private GameRemoteMaster game;
	private GameInfo com;
	private Lobby lobby;

	private RaceVisual raceVisual;
	private FinishVisual finishVisual;
	private final WinVisual winVisual;
	protected Visual currentVisual;

	public final RaceKeys keys;
	private long startTime;
	private long waitTime, burnoutTimeLocal;
	private boolean running;
	private int raceLights;
	private long[] raceLightsLocalTime;
	private int burnoutTime = 00;
	private boolean finished, everyoneFinished;

	private int waitingDNF;
	private long waitingDNFTime;

	private boolean initiated;
	private boolean close;

	// FIXME temp fps counter for racing
	// private long timer;
	// private int frames;

	private final UIScrollable raceLobbyLabel;
	private boolean finishedFirstRace;
	private long raceInformationTick;

	private String trackLengthText = "";

	private boolean finalizedTextLocally;

	private boolean nosControllerWasDown;

	private float controllerX, controllerY;

	private boolean controllerShifted;

	private int prevGear;

	private float gearboxControllerDelay;

	private long controllerScroll;

	public Race(TransparentTopbar topbar, LobbyTopbar winTopbar) {
		super(topbar, Scenes.RACE);
//        SpriteNumeric.CreateNumbers();
		keys = new RaceKeys();

		var w = Window.HEIGHT / 2.14f;
		raceLobbyLabel = new UIScrollable(new UIFont(Font.BOLD_REGULAR, -1), Scenes.RACE,
				Window.WIDTH - w - .0146f * Window.WIDTH, Window.HEIGHT / 24f, w, Window.HEIGHT / 1.5f);

		raceVisual = new RaceVisual(this);

		var goBackBtn = new UIButton<>(Texts.continueText);
		goBackBtn.setPressedAction(() -> {
			if (currentVisual instanceof WinVisual) {
				if (com.player.isHost() && com.isLAN() && com.isGameStarted() && !com.isGameOver()) {
					UIConfirmModal.show("YOU ARE THE HOSTING A LAN LOBBY! IF YOU LEAVE THE SERVER WILL BE SHUT DOWN! ARE YOU REALLY SURE?", () -> {
						audio.play(SfxTypes.LEFT);
						close = true;
						game.leaveGame = true;
					});
				} else {
					audio.play(SfxTypes.LEFT);
					close = true;
					game.leaveGame = true;
				}
			} else {
				audio.play(SfxTypes.OPEN_STORE);
				closeCache();
				sceneChange.change(Scenes.LOBBY, false);
				lobby.updateBackgroundColor();
//				lobby.doneRacing();
			}
		});

		finishVisual = new FinishVisual(raceLobbyLabel);
		winVisual = new WinVisual(raceLobbyLabel, winTopbar);
		winVisual.race = this;
		Visual.setGoback(goBackBtn);
		add(goBackBtn);

		if (!Main.DEMO) {
			UIButton<?> leaderboardBtn = new UIButton<>(Texts.leaderboardText);
			leaderboardBtn.setPressedAction(() -> {
				sceneChange.change(Scenes.LEADERBOARD, true);
				audio.play(SfxTypes.REGULAR_PRESS);
			});
			add(leaderboardBtn);
			winVisual.setLeaderboardBtn(leaderboardBtn);
		}

		MovingThings.init();
	}

	public void initWinVisual(UpgradesSubscene upgradesSubscene) {
		winVisual.setUpgrades(upgradesSubscene);
	}

	private void closeCache() {
		for (var player : com.getPlayers()) {
			player.car.getModel().reset();
			if (player.car.getAudio() == null)
				com.setChosenCarAudio(audio);
			else
				player.car.getAudio().reset();
		}
		currentVisual.showGoBackBtn(true);
		audio.setListenerData(0, 0, 0);
		finishVisual.clear();
	}

	public void initRestBeforeFirstRace(GameInfo com, GameRemoteMaster game) {
		this.com = com;
		this.game = game;
		initiated = true;

		raceVisual.initRest(com.player, audio);
		finishVisual.setPlayer(com.player);
		winVisual.setPlayer(com.player);
		if (com.getGamemode() instanceof SingleplayerChallengesMode sp) {
			winVisual.initRest(true, sp.getChallengeLevel() <= SingleplayerChallenges.TheBoss.ordinal());
		} else {
			winVisual.initRest(false, false);
		}

		finishVisual.winnerWinnerChickenDinner(false, null, null, false, 1, false);

		com.setChosenCarAudio(audio);
		com.setChosenCarModels();

		com.setActionFinishPlayerAnimation((car, dnf, speed) -> {
			if (dnf || speed < 5)
				return;
			if (!finished) {
				finishVisual.addButNotFinished(speed);
				return;
			}
			finishVisual.addFinish(car);
		});

		com.setActionEveryoneFinished(() -> {
			if (everyoneFinished)
				return;
			everyoneFinished = true;

			if (com.player.role == Player.COMMENTATOR)
				finished = true;


			currentVisual.showGoBackBtn(true);
			// Stop race aka make ready the next race
			// This is still in racewindow, but is to initialize
			// the closing of the racing part.

			game.placeChecked = false;
			com.updateRaceCountdown(com.player, false, false);
			game.started = false;
			raceLightsLocalTime = null;

			if (com.isGameOver()) {

//                understandRaceLobbyFromServer(com.updateRaceLobby(com.player, true));

				if (Scenes.CURRENT != Scenes.RACE) {
					sceneChange.change(Scenes.RACE, false);
				}

				String leaderboardScoreText = "";
				String leaderboardScoreExplaination = "";

				if (com.isSingleplayer()) {
					leaderboardScoreText = ((SingleplayerChallengesMode) com.getGamemode()).getCreateScore(com.player);
				}

				winVisual.claimWinner(com, leaderboardScoreText, leaderboardScoreExplaination);

				for (Player player : com.getPlayers()) {
					player.car.getAudio().reset();
				}

				finishVisual.clearCars();
				var winners = com.getGamemode().getWinners();

				if (winners.isEmpty() || com.isSingleplayerLost()) {
					var winnerText = "You lost!";
					finishVisual.winnerWinnerChickenDinner(true, winnerText, UIColors.R, false, 1, true);
					finishVisual.addFinish(com.player.car, finishVisual.maxDistance);
					finishVisual.addFinish(com.player.car, finishVisual.maxDistance + 5);
					finishVisual.addFinish(com.player.car, finishVisual.maxDistance + 10);
					finishVisual.addFinish(com.player.car, finishVisual.maxDistance + 15);
					finishVisual.addFinish(com.player.car, finishVisual.maxDistance + 20);
				} else {
					var winnerCar = winners.get(0).car;
					final int min = 80;
					if (winnerCar.getStats().finishSpeed < min) {
						System.out.println("Winner speed going to 80 was " + winnerCar.getStats().finishSpeed);
						winnerCar.getStats().finishSpeed = min;
					} else if (winnerCar.getStats().finishSpeed > 450) {
						System.out.println("Winner speed going to 450 was " + winnerCar.getStats().finishSpeed);
						winnerCar.getStats().finishSpeed = 450;
					}
					var tuneDistance = winnerCar.getStats().finishSpeed / min;
					var distance = finishVisual.maxDistance * tuneDistance;
					finishVisual.addFinish(winnerCar, distance);

					var imWinner = com.getGamemode().isWinner(com.player);

					if (!imWinner) {
						com.player.car.getStats().finishSpeed = winnerCar.getStats().finishSpeed;
						finishVisual.addFinish(com.player.car, distance + finishVisual.lostDistance);
					}

					var winnerText = imWinner ? "WINNER WINNER WINNER" : "You lost!";
					finishVisual.winnerWinnerChickenDinner(true, winnerText, UIColors.WHITE, imWinner,
							(float) winnerCar.getStats().finishSpeed / min, false);

				}
				currentVisual.showGoBackBtn(false);
				finishVisual.setAfterAllFinishCars(() -> {
					changeVisual(winVisual);
					currentVisual.showGoBackBtn(true);
					if (com.getGamemode().isWinner(com.player)) {
						audio.play(SfxTypes.WON);
						if (Main.DEMO)
							SceneHandler.showMessage(
									"Well done! If you want more content you can buy the game cheaply on Steam. You'll get multiplayer, more cars and more challenges! Enjoy :)");
					} else {
						audio.play(SfxTypes.LOST);
					}
				});

				changeVisual(finishVisual);

//				com.endRemote();
			} else {
				if (com.isSingleplayer() && com.getGamemode() instanceof SingleplayerChallengesMode sp) {
					lobby.setExtraPlayersListText(singleplayerRaceLobbyAfter());
					if (com.player.timeLapsedInRace < 0) {
						finishVisual.setWinnerText("DNF!", UIColors.R, false);
					} else {
						var madeIt = com.player.timeLapsedInRace <= sp.prevtimeToBeat;
						finishVisual.setWinnerText(madeIt ? "You made it!" : "Too slow!",
								madeIt ? UIColors.GREENER_WON : UIColors.R, false);
					}
				} else if (com.getGamemode() instanceof CoopMode coop) {
					var madeIt = coop.averageTime <= coop.prevtimeToBeat();
					finishVisual.setWinnerText(madeIt ? "You made it!" : "Too slow!",
							madeIt ? UIColors.GREENER_WON : UIColors.R, false);
				} else {
					if (com.player.timeLapsedInRace < 0)
						finishVisual.setWinnerText("DNF!", UIColors.R, false);
					else if (com.player.podiumRace + 1 == com.getPlayers().length)
						finishVisual.setWinnerText("Last place!", UIColors.R, false);
					else
						finishVisual.setWinnerText(Texts.podiumConversion(com.player.podiumRace) + " place!",
								com.player.podiumRace == 0 ? UIColors.GREENER_WON : UIColors.PICTON_BLUE, false);
				}
//				lobby.doneRacing();
				if (com.getGamemode().moneyExplaination != null)
					finishVisual.moneyExplaination.setText(com.getGamemode().moneyExplaination);

			}
		});
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		if (com.player.resigned) {
			finalizedTextLocally = false;
			audio.play(SfxTypes.LOST);
			finished = true;
			winVisual.setPlayer(null);
			winVisual.claimWinner(com, "", "");
			changeVisual(winVisual);
			for (Player player : com.getPlayers()) {
				player.car.getAudio().reset();
			}
		} else if (com.isGameOver()) {
			finalizedTextLocally = false;
			everyoneFinished = false;
			com.getActionEveryoneFinished().run();
		}
		currentVisual.updateGenerally(cam);
	}

	@Override
	public void updateResolution() {
		float padding = Window.HEIGHT / 140f;
		raceLobbyLabel.setPadding(padding / 2f, padding);

		finishVisual.updateResolution();
		winVisual.updateResolution();
		raceVisual.updateResolution();

	}

	public boolean isInitiated() {
		return initiated;
	}

	public void initWindow() {
		controllerX = controllerY = 0;

		System.out.println("New race reset");
		close = false;

		everyoneFinished = false;
		running = false;
		finished = false;
		startTime = -1;

		com.player.car.clutch(true);

		finishVisual.init();
		raceLobbyLabel.clear();

		changeVisual(raceVisual);
		closeCache();
		
		com.goInTheRace(com.player);

		raceVisual.initBeforeNewRace(com.getSortedPlayersExtra(), SceneHandler.cam, com.getTrackLength(),
				com.getGamemode().getLifes(com.player), com.player.role == Player.COMMENTATOR, com.raceSeed,
				(com.isSingleplayer() && ((SingleplayerChallengesMode) com.getGamemode()).getChallengeLevel() == SingleplayerChallenges.TheBoss.ordinal()));
		AudioMaster.dontLoadMusic = true;
		game.darkmode = true;
//		player.setOpponent((byte) 0);
		com.player.car.getAudio().startAlwaysOnSounds(com.player.car.getStats());
		trackLengthText = "Tracklength: " + com.getTrackLength() + " meters.\n";
		finalizedTextLocally = false;

		raceVisual.burnout = true;

		finishVisual.setWinnerText("", UIColors.WHITE, false);
		
		nosControllerWasDown = false;
	}

	public void createTryAgainButton(Consumer<Integer> createNewSingleplayerGameAction, IAction rematchAction) {
		var tryAgainBtn = new UIButton<>("");
		tryAgainBtn.setPressedAction(() -> {
			if (com.isSingleplayer()) {
				game.endAll();
				createNewSingleplayerGameAction
						.accept(((SingleplayerChallengesMode) com.getGamemode()).getChallengeLevel());
				return;
			}
			
//			initiated = false;
//			com.reset();
//			rematchAction.run();
//			lobby.reset();
		});

		add(tryAgainBtn);

		winVisual.setTryAgainBtn(tryAgainBtn);
	}

	public void setRaceLightsFromNow() {
		long[] timesDifferences = com.getRaceLights();
		if (timesDifferences != null) {

			raceLights = 0;
			raceLightsLocalTime = new long[timesDifferences.length];

			long now = System.currentTimeMillis() + burnoutTime;
			burnoutTimeLocal = now;
			for (int i = 0; i < raceLightsLocalTime.length; i++) {
				raceLightsLocalTime[i] = timesDifferences[i] + now;
				System.out.println("RACE TIME " + i + ": " + raceLightsLocalTime[i] + ", " + timesDifferences[i]);
			}
		} else {
			System.out.println("setRaceLightsFromNow has null racelights");
			game.leaveGame = true;
		}
	}

	private void controlLights() {
		if (raceLights == GameMode.raceLightsLength) {
			waitTime = System.currentTimeMillis() + 1000;
			startTime = raceLightsLocalTime[GameMode.raceLightsLength - 1];
			running = true;

			audio.play(SfxTypes.GREENLIGHT);

			if (!com.player.car.hasTireboost()) {
				raceVisual.setWarning("");
			}
		} else {
			audio.play(SfxTypes.REDLIGHT);
		}

		raceVisual.setBallCount(raceLights);
	}

	private boolean controlRaceLightsCountdown() {
		// Controls countdown and cheating and such shait.
		boolean tooEarly = false, startedNow = false;
		var now = System.currentTimeMillis();

		if (raceVisual != null && !running) {

			if (raceVisual.burnout && now > burnoutTimeLocal) {
				raceVisual.burnout = false;
			}

			if (raceLightsLocalTime != null && now >= raceLightsLocalTime[raceLights]) {

				this.raceLights++;

				// CONTROL LIGHTS
				controlLights();
			}

			// CHEATING
			if (raceLights < GameMode.raceLightsLength && com.player.car.getStats().speed > 2d) {
				if (com.getGamemode().isDNFWhenCheating())
					finishRace(CHEATED_TOO_EARLY);
				else {
					raceLights = GameMode.raceLightsLength;
					controlLights();
					raceVisual.hasBeenGreen = true;
					startTime = System.currentTimeMillis() - 3000;
					waitTime = 0;
					raceVisual.addLog("PENALTY! You drove before green!#" + UIColors.R);
					audio.play(SfxTypes.LOSTLIFE);
					tooEarly = true;
				}
			}

//            startedNow = running;
		}

		if (raceLights == GameMode.raceLightsLength) {
			if (waitTime < now) {
				raceLights = 0;
				raceVisual.setBallCount(raceLights);
			}

			int reactionTime = !tooEarly ? (int) (now - startTime) : 500;
			if (com.player.car.startBoost(reactionTime, now))
				raceVisual.setStartboostTime(reactionTime, com.player.car.tireboostLoss(reactionTime));
			else if (com.player.car.getStats().throttle)
				raceVisual.setStartboostTime(reactionTime);

			if (com.player.car.getRep().is(Rep.twoStep)) {
				com.player.car.clutch(false);
			}
		}

		return finished;
	}

	private boolean checkFinishLineCrossed() {
		if (com.player.car.getStats().distance >= com.getTrackLength()) {
			// Push results and wait for everyone to finish. Then get a winner.'
			finishRace(CHEATED_NOT);
		}
		return finished;
	}

	@Override
	public void tick(float delta) {

		if (game.leaveGame) {
			game.endAll();
			return;
		}

		Car car = com.player.car;

		prevGear = car.getStats().gear;

		if (currentVisual != null)
			currentVisual.tick(delta);

		var now = System.currentTimeMillis();
		
		if (InputHandler.CONTROLLER_EFFECTIVELY && currentVisual != winVisual || !winVisual.hasPlayer()) {
			controllerInput();
		}

		if (!finished) {
			if (com.player.role == Player.COMMENTATOR && !com.getGamemode().isRacing()) {
				finishRace(CHEATED_TOO_EARLY);
				return;
			}

			car.updateSpeed(delta, now);

			if (controlRaceLightsCountdown() || checkFinishLineCrossed())
				return;

			raceVisual.setExtraGamemodeInfoText(com.getGamemode());
            if (!com.isSingleplayer()) {
                if (car.getStats().distance > 0 && raceInformationTick < now) {
                    com.raceInformation(com.player, Math.round(-car.getStats().distance * 10d) / 10f, car.getSpeed(),
                            car.getStats().spdinc, car.getStats().brake != 0, now - getStartTime(), true);
                    raceInformationTick = now + 50;
                }
            }

        } else if (!com.resigned) {
			if (com.player.role == Player.COMMENTATOR && currentVisual == raceVisual) {
				finishRace(CHEATED_TOO_EARLY);
				return;
			}
			understandRaceLobbyFromServer();
			if (!com.isSingleplayer() && !com.isGameOver()) {
				com.player.car.getModel().updatePositionByInformation(now - getStartTime(), com.getTrackLength() - 10,
						delta, false);
				if (com.isEveryoneDone()) {
					lobby.countdown();
				} else {
					controlRaceLightsCountdown();
					for (Player p : com.getPlayers()) {
						p.car.getModel().updatePositionByInformation(now - getStartTime(), com.getTrackLength(), delta,
								false);
					}
				}
			}
		}
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {

		if (currentVisual != null && !close) {
			currentVisual.renderGame(renderer, cam, window, delta);
		}

		// if (System.currentTimeMillis() - timer > 1000) {
		// System.out.println("FPS RACE: " + frames);
		// timer = System.currentTimeMillis() + 1000;
		// frames = 0;
		// }
		//
		// frames++;

	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		if (currentVisual != null && !close) {
			game.renderUILayout(ctx, stack);
			currentVisual.renderUILayout(ctx, stack);
		}

	}

	public void understandRaceLobbyFromServer() {
		if (finalizedTextLocally)
			return;
		var lastText = com.raceLobbyStringFinalized;
		var codedString = com.updateRaceLobby(false);
		if (codedString == null)
			return;

		String[] outputs = codedString.split("#");

		String result = trackLengthText;

		result += raceLobbyLabelDecode(outputs);
		raceLobbyLabel.setText(result);

		if (lastText)
			finalizedTextLocally = true;
	}

	private String[] singleplayerRaceLobbyAfter() {
		String codedString = raceLobbyLabel.getText();

		if (codedString == null)
			return null;

		return codedString.split("\n");
	}

	private String raceLobbyLabelDecode(String[] outputs) {
		int n;
		int stageLength = 5;
		boolean finished = false;
		String color = "";
		String nameLine = "";
		StringBuilder infoLine = new StringBuilder();
		StringBuilder timeLine = new StringBuilder();
		StringBuilder result = new StringBuilder();

		var startTime = getStartTime();
		var singleplayer = com.isSingleplayer();
		if (!singleplayer)
			result.append("Players:");
		else
			result.append("\n");
		for (int i = 1; i < outputs.length; i++) {
			n = (i - 1) % stageLength + 1;

			switch (n) {
				case 1 -> {
					color = "";
					nameLine = "\n     " + outputs[i].replaceAll(Translator.hashtag, "#");
					infoLine = new StringBuilder();
					timeLine = new StringBuilder();
				}
				case 2 -> {

					// Controlling whether player has finished or not

					if (i < stageLength)
						color = String.valueOf(UIColors.WON);
					else
						color = String.valueOf(UIColors.BLACK);
					if (Integer.parseInt(outputs[i]) == 1) {
						finished = true;
					} else {
						finished = false;
						color = String.valueOf(UIColors.NF);
					}
				}
				case 3 -> {
					final long thisPlayerTime = Long.parseLong(outputs[i]);
					if (thisPlayerTime < Race.CHEATED_NOT) {

						timeLine.append(Texts.raceTimeText(thisPlayerTime));
						color = String.valueOf(UIColors.DNF);

					} else if (finished) {

						timeLine.append(Float.parseFloat(outputs[i]) / 1000).append(" seconds");

					} else if (startTime != 0 && System.currentTimeMillis() >= startTime) {

						timeLine.append((float) (System.currentTimeMillis() - startTime) / 1000)
								.append(" seconds");

					} else {
						// This will only happen if you are dnf-ed
						if (raceLightsLocalTime == null && com.updateRaceLights())
							setRaceLightsFromNow();

						timeLine.append("Waiting");

						// Dots ...
						if (waitingDNFTime < System.currentTimeMillis()) {
							waitingDNFTime = System.currentTimeMillis() + 300;
							waitingDNF = (waitingDNF + 1) % 4;
						}
						timeLine.append(".".repeat(Math.max(0, waitingDNF)));
					}
					if (com.isSingleplayerLostLastRace()) {
						color = String.valueOf(UIColors.DNF);
					}
				}
				case 4 -> infoLine.append(outputs[i]); // whos ahead?
				case 5 -> {
					String gold = outputs[i];
					if (!gold.equals("x") && gold.length() != 0)
						infoLine.append(", ").append(gold);
					
					if (!singleplayer)
						result.append(nameLine).append("#").append(color).append("\n");
					
					result.append(" ").append(timeLine).append("#")
							.append(color).append("\n ").append(infoLine).append("#").append(color);
				}
			}
		}
		if (com.getGamemode() instanceof CoopMode coop) {
			var str = result.toString();
			result = new StringBuilder()
					.append("\nAverage time: " + SingleplayerChallengesMode.timeToBeat(coop.averageTime))
					.append("\nTime to beat was: ").append(SingleplayerChallengesMode.timeToBeat(coop.prevtimeToBeat()))
					.append("\nWin: ").append(SingleplayerChallengesMode.timeToBeat(coop.getEndGoal())).append(str);
		} else if (com.getGamemode() instanceof SingleplayerChallengesMode sp) {
			result.append("\n\nTime to beat was: ").append(SingleplayerChallengesMode.timeToBeat(sp.prevtimeToBeat));

			if (sp.showTimeChange && !sp.isGameOver()) {
				var timeDiff = (sp.timeToBeat - sp.prevtimeToBeat) / 1000d;

				String tier;
				if (!sp.showTimeMedal || com.player.timeLapsedInRace < 0
						|| com.player.timeLapsedInRace > sp.prevtimeToBeat) {
					tier = "";
				} else {
					var minTimeBeat = sp.prevtimeToBeat - sp.minimumPenaltyTimeNum(sp.prevtimeToBeat);
					int timePlayerBeat = (int) (sp.prevtimeToBeat - com.player.timeLapsedInRace);

					int diamond = (int) (minTimeBeat * 0.1);
					int gold = (int) (minTimeBeat * 0.33);
					int silver = (int) (minTimeBeat * 0.66);

					int tierNr = 0;

					if (timePlayerBeat < diamond) {
						tierNr = 0;
						tier = "Diamond!#" + UIColors.TUR2;
					} else if (timePlayerBeat < gold) {
						tierNr = 1;
						tier = "Gold#" + UIColors.GOLD;
					} else if (timePlayerBeat < silver) {
						tierNr = 2;
						tier = "Silver#" + UIColors.SILVER;
					} else if (timePlayerBeat < minTimeBeat * 1) {
						tierNr = 3;
						tier = "Bronze#" + UIColors.BRONZE;
					} else {
						tierNr = 4;
						tier = "Iron#" + UIColors.BONUSGOLD1;
					}
					tier =
//							"Quality: " +
							"    " + (tierNr == 0 ? " > " : "") + "Diamond: "
									+ SingleplayerChallengesMode.timeToBeat(sp.prevtimeToBeat - diamond) + "#"
									+ (tierNr == 0 ? UIColors.TUR2 : UIColors.DNF) + "\n" + "    "
									+ (tierNr == 1 ? " > " : "") + "Gold: "
									+ SingleplayerChallengesMode.timeToBeat(sp.prevtimeToBeat - gold) + "#"
									+ (tierNr == 1 ? UIColors.GOLD : UIColors.DNF) + "\n" + "    "
									+ (tierNr == 2 ? " > " : "") + "Silver: "
									+ SingleplayerChallengesMode.timeToBeat(sp.prevtimeToBeat - silver) + "#"
									+ (tierNr == 2 ? UIColors.SILVER : UIColors.DNF) + "\n" + "    "
									+ (tierNr == 3 ? " > " : "") + "Bronze: "
									+ SingleplayerChallengesMode.timeToBeat(sp.prevtimeToBeat - minTimeBeat) + "#"
									+ (tierNr == 3 ? UIColors.BRONZE : UIColors.DNF) + "\n" + "    "
									+ (tierNr == 4 ? " > " : "") + "Iron: less than "
									+ SingleplayerChallengesMode.timeToBeat(sp.prevtimeToBeat - minTimeBeat) + "#"
									+ (tierNr == 4 ? UIColors.BONUSGOLD1 : UIColors.DNF) + "\n";
				}

				result.append("\nTime change: ").append(timeDiff >= 0 ? "+" : "-")
						.append(Texts.formatNumber(Math.abs(timeDiff))).append(" sec -> ")
						.append(SingleplayerChallengesMode.timeToBeat(sp.timeToBeat)).append("\n").append(tier);
			}
			result.append("\nWin: less than ").append(SingleplayerChallengesMode.timeToBeat(sp.getEndGoal()));
		}
		
		if (com.isGameOver()) {
			result.append("\n\n").append(Lobby.newlineText(com.getGamemode().getEndGoalText(), 40)).append("\n");
		}
		
		if (Main.DEBUG) {
			for (int i = 0; i < 32; i++)
				result.append("\ntest");
		}

		return result.toString();
	}

	public void finishRace(int cheated) {
		System.out.println("Finished");
		SceneHandler.freeCam = false;

		if (com.getRound() == 0) {
			com.player.resetHistory(com.getRound());
		}

		com.ready(com.player, (byte) 0);

		finished = true;
		Features.inst.getWindow().mouseStateHide(false);

		com.player.car.regenTurboBlow();

		raceLights = 0;
		Features.inst.getWindow().setCursor(CursorType.cursorNormal);

		finishVisual.moneyExplaination.clear();
		changeVisual(finishVisual);

		long time = cheated != 0 ? cheated : System.currentTimeMillis() - startTime;
		com.finishRace(com.player, time, com.player.car.getSpeed());

		AudioMaster.dontLoadMusic = false;
	}

	public void changeVisual(Visual newVisual) {
        Features.inst.getWindow().setCursor(CursorType.cursorNormal);

		if (newVisual.equals(finishVisual)) {
			Visual.setGobackName(Texts.continueText);
			audio.setListenerData(finishVisual.getCameraPosition());
		} else if (newVisual.equals(winVisual)) {
			Visual.setGobackName("Leave...");
			com.winClose();
		}

		if (newVisual.equals(currentVisual)) {
			System.out.println("Same visual");
			return;
		}

		press();
		currentVisual = newVisual;
		newVisual.updateGenerally(SceneHandler.cam);
	}
	

	@Override
	public void charInput(String c) {
		if (currentVisual instanceof WinVisual)
			Lobby.chatInput(c);
	}

	@Override
	public void keyInput(int key, int action) {
		game.keyInput(key, action);

		if (finished || com.player.role == Player.COMMENTATOR) {
			currentVisual.keyInput(key, action);
			return;
		}

		Car car = com.player.car;

		if (raceVisual.burnout) {
			return;
		}

		if (keys.isNos(key))
			car.nos(action != GLFW.GLFW_RELEASE);

		if (action != GLFW.GLFW_RELEASE) {
			/*
			 * PRESS
			 */
			if (keys.isThrottle(key)) {
				if (!car.getRep().is(Rep.manualClutch)
						&& (running || !car.getRep().is(Rep.twoStep) && (raceLights > GameMode.raceLightsCanStartDriving || Main.DEBUG))) {
					car.clutch(false);
				}

				raceVisual.getGearbox().updateThrottleStats(true);
				car.throttle(true, true);
			}
			// else if (keys.isStompThrottle(key)) {
			// car.stompThrottle(false);
			// }
			else if (keys.isBrake(key)) {
				car.brake(true);
			} else if (keys.isClutch(key) && car.getRep().is(Rep.manualClutch)) {
				car.clutch(true);
			} else if (keys.isBlowTurbo(key)) {
				car.blowTurbo(true);
			} else if (!com.isSingleplayer() && key == RaceKeys.lookBehind) {
				raceVisual.lookBehind = true;
			} else if (car.getStats().sequentialShift) {
				if (keys.isShiftUp(key)) {
					// up arrow
					car.shiftUp(System.currentTimeMillis());
				}
				if (keys.isShiftDown(key)) {
					// down arrow
					car.shiftDown(System.currentTimeMillis());
				}
			}
			// Gearbox
			// else if (!car.getStats().sequentialShift) {
			// for (int i = 0; i <= car.getRep().getGearTop(); i++) {
			// if (keys.isGear(key, i)) {
			// car.shift(i);
			// }
			// }
			// } else {
			// if (keys.isShiftUp(key)) {
			// // up arrow
			// car.shiftUp(true);
			// }
			// if (keys.isShiftDown(key)) {
			// // down arrow
			// car.shiftDown(true);
			// }
			// }

		} else {
			/*
			 * RELEASE
			 */
			if (keys.isThrottle(key)) {

				if (!car.getRep().is(Rep.manualClutch) && ((raceLights > GameMode.raceLightsCanStartDriving || Main.DEBUG) || running)) {
					car.clutch(true);
				}

				car.throttle(false, true);
				raceVisual.getGearbox().updateThrottleStats(false);
				if (!running)
					raceVisual.setWarning("");
			}
			// else if (keys.isStompThrottle(key)) {
			// car.stompThrottle(true);
			// }
			else if (keys.isBrake(key)) {
				car.brake(false);
			} else if (keys.isClutch(key) && car.getRep().is(Rep.manualClutch)) {
				car.clutch(false);
			} else if (keys.isBlowTurbo(key)) {
				car.blowTurbo(false);
			} else if (key == RaceKeys.lookBehind) {
				raceVisual.lookBehind = false;
			}
			// else if (keys.isEngineON(key)) {
			// car.setEngineON(true);
			// }
			if (key == RaceKeys.quitRace) {
				finishRace(CHEATED_GAVE_IN);
			}

			if (Main.DEBUG) {
				if (key == GLFW.GLFW_KEY_F4) {
					com.win();
					finishRace(CHEATED_GAVE_IN);
				} else if (key == GLFW.GLFW_KEY_F5) {
					com.player.bank.set(-2, Bank.POINT);
					com.getGamemode().endGameToWin();
					finishRace(CHEATED_GAVE_IN);
				}
			}
		}
	}

	@Override
	public void controllerInput() {
		game.controllerInput();

		if (finished || com.player.role == Player.COMMENTATOR) {
			currentVisual.controllerInput();
			
			if (System.currentTimeMillis() > controllerScroll) {
				if (Math.max(InputHandler.RIGHT_STICK_Y, InputHandler.LEFT_STICK_Y) > .1) {
					mouseScrollInput(0, -1);
					raceLobbyLabel.getWindow().focus = true;
					raceLobbyLabel.scroll(-1);
					controllerScroll = System.currentTimeMillis() + 66;
				} else if (Math.min(InputHandler.RIGHT_STICK_Y, InputHandler.LEFT_STICK_Y) < -.1) {
					mouseScrollInput(0, 1);
					raceLobbyLabel.getWindow().focus = true;
					raceLobbyLabel.scroll(1);
					controllerScroll = System.currentTimeMillis() + 66;
				}
			}
			return;
		}

		Car car = com.player.car;
		CarStats stats = car.getStats();

		if (raceVisual.burnout) {
			return;
		}
		
		if (running || raceLights > GameMode.raceLightsCanStartDriving) {
			if (InputHandler.BTN_X) {
				car.nos(true);
				nosControllerWasDown = true;
			} else if (nosControllerWasDown) {
				car.nos(false);
				nosControllerWasDown = false;
			}
		}

		if (InputHandler.BTN_A) {
			car.blowTurbo(true);
		} else if (stats.turboBlowON) {
			car.blowTurbo(false);
		}
		
		if (!com.isSingleplayer())
			raceVisual.lookBehind = InputHandler.BTN_Y;

		float throttlePerc = (InputHandler.RIGHT_TRIGGER + 1f) / 2f;
		if (throttlePerc > 0.95f)
			throttlePerc = 1f;
		else if (throttlePerc < 0.05f)
			throttlePerc = 0f;
		if (InputHandler.RIGHT_TRIGGER > -1) {
			if ((running || !car.getRep().is(Rep.twoStep) && raceLights > GameMode.raceLightsCanStartDriving)) {
				car.clutch(false);
			}

			raceVisual.getGearbox().updateThrottleStats(true);
		} else if (stats.throttle) {
			if ((raceLights > GameMode.raceLightsCanStartDriving || running)) {
				car.clutch(true);
			}

			raceVisual.getGearbox().updateThrottleStats(false);
			if (!running)
				raceVisual.setWarning("");
		}
		
		car.throttle(throttlePerc, true);

		if (InputHandler.LEFT_TRIGGER > -1) {
			car.brake(true);
		} else if (stats.brake > 0) {
			car.brake(false);
		}

		if (InputHandler.BTN_BACK_TOP_LEFT && InputHandler.BTN_BACK_TOP_RIGHT) {
			finishRace(CHEATED_GAVE_IN);
		}

		var gearbox = raceVisual.getGearbox();
		var gearboxSprite = gearbox.getGearbox();
		var gearboxPos = gearboxSprite.position();
		var maxX = gearboxSprite.getXWidth();
		var maxY = gearboxSprite.getYHeight();
		if (!stats.sequentialShift) {
			if (gearboxControllerDelay > 0f) {
				gearboxControllerDelay -= Timer.lastDelta;
				return;
			}
			
			final var sensitivity = .2f;
			final var spd = 60f;
			if (Math.abs(InputHandler.LEFT_STICK_X) > sensitivity) {
				gearbox.pressPerfectly();
				controllerX += spd * .66f * InputHandler.LEFT_STICK_X * Timer.lastDelta;
			} else if (Math.abs(InputHandler.RIGHT_STICK_X) > sensitivity) {
				gearbox.pressPerfectly();
				controllerX += spd * .66f * InputHandler.RIGHT_STICK_X * Timer.lastDelta;
			}
			if (Math.abs(InputHandler.LEFT_STICK_Y) > sensitivity) {
				gearbox.pressPerfectly();
				controllerY += spd * InputHandler.LEFT_STICK_Y * Timer.lastDelta;
			} else if (Math.abs(InputHandler.RIGHT_STICK_Y) > sensitivity) {
				gearbox.pressPerfectly();
				controllerY += spd * InputHandler.RIGHT_STICK_Y * Timer.lastDelta;
			}

			if (controllerX > maxX) {
				controllerX = maxX;
			} else if (controllerX < gearboxPos.x) {
				controllerX = gearboxPos.x;
			}
			if (controllerY > maxY) {
				controllerY = maxY;
			} else if (controllerY < gearboxPos.y) {
				controllerY = gearboxPos.y;
			}

			gearbox.move(controllerX, controllerY);
			
			if (stats.gear != prevGear && stats.gear != 0) {
				var leverPos = gearbox.getGearboxLever().position();
				controllerX = leverPos.x;
				controllerY = leverPos.y;
				
//				if (Features.ran.nextFloat() <= 0.02) {
//					car.shift(0, System.currentTimeMillis());
//					gearbox.release(0, 0);
//					gearbox.tick(0);
//					leverPos = gearbox.getGearboxLever().position();
//					controllerX = leverPos.x;
//					controllerY = leverPos.y;
//					
//					gearboxControllerDelay = 10f;
//				}
			}
		} else {
			var controllerMoveDown = InputHandler.BTN_DOWN || InputHandler.LEFT_STICK_Y > .5
					|| InputHandler.RIGHT_STICK_Y > .5;
			var controllerMoveUp = InputHandler.BTN_UP || InputHandler.LEFT_STICK_Y < -.5
					|| InputHandler.RIGHT_STICK_Y < -.5;

			if (!controllerShifted) {
				if (controllerMoveDown) {
					car.shiftUp(System.currentTimeMillis());
					controllerShifted = true;
				} else if (controllerMoveUp) {
					car.shiftDown(System.currentTimeMillis());
					controllerShifted = true;
				}
			} else if (!controllerMoveDown && !controllerMoveUp) {
				controllerShifted = false;
			}
		}
	}

	@Override
	public void mouseScrollInput(float x, float y) {
		currentVisual.mouseScrollInput(x, y);
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		super.mouseButtonInput(button, action, x, y);
		currentVisual.mouseButtonInput(button, action, x, y);
		return false;
	}

	@Override
	public void mousePositionInput(float x, float y) {
		currentVisual.mousePosInput(x, y);
	}

	public int getCurrentLength() {
		return com.getTrackLength();
	}

	public long getStartTimeAltered() {
		if (running)
			return startTime;
		else
			return 0;
	}

	public long getStartTime() {
		if (raceLightsLocalTime != null)
			synchronized (raceLightsLocalTime) {
				return raceLightsLocalTime[GameMode.raceLightsLength - 1];
			}
		return 0;
	}

	public long[] getRaceLights() {
		return raceLightsLocalTime;
	}

	public boolean isFinishedFirstRace() {
		return finishedFirstRace;
	}

	public void setLobby(Lobby lobby) {
		this.lobby = lobby;
	}

	public void turnOff() {
		System.out.println("Turn off");
		finishedFirstRace = false;
		raceLightsLocalTime = null;
		initiated = false;
		close = false;
		finished = false;
		if (com != null)
			com.close();
	}

	@Override
	public void destroy() {
		if (finishVisual != null) {
			finishVisual.removeAllGameObjects();
			finishVisual.removeAllUIObjects();
		}
		if (raceVisual != null) {
			raceVisual.removeAllGameObjects();
			raceVisual.removeAllUIObjects();
		}
		raceVisual = null;
		finishVisual = null;
		currentVisual = null;

		removeGameObjects();
	}

}
