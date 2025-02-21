package scenes.game;

import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.util.List;
import java.util.function.Consumer;

import comNew.LocalRemote2;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import adt.IActionDouble;
import audio.AudioRemote;
import audio.SfxTypes;
import audio.Source;
import communication.GameInfo;
import communication.GameType;
import communication.Translator;
import engine.graphics.Renderer;
import engine.graphics.interactions.LobbyTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Sprite;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UILabelRow;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIScrollable;
import engine.graphics.ui.UITextField;
import engine.graphics.ui.UIWindowInfo;
import engine.graphics.ui.modal.UIBonusModal;
import engine.graphics.ui.modal.UIConfirmModal;
import engine.io.InputHandler;
import engine.io.Window;
import engine.math.Vec3;
import game_modes.CoopMode;
import game_modes.SingleplayerChallengesMode;
import main.Features;
import main.Main;
import main.Texts;
import player_local.Bank;
import player_local.Layer;
import player_local.Player;
import player_local.upgrades.TileVisual;
import player_local.upgrades.UpgradeResult;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Scene;
import scenes.adt.SceneChangeAction;
import scenes.adt.Subscene;
import scenes.game.lobby_subscenes.CarChoiceSubscene;
import scenes.game.lobby_subscenes.UpgradesSubscene;
import settings_and_logging.RSet;
import settings_and_logging.hotkeys.CurrentControls;

/**
 * Visualization of a game
 */
public class Lobby extends Scene {


	private long nextStatusUpdate;
	private static long nextCountdownUpdate;

	public static class CountdownStats {
		int tickTime;
		
		public void reset() {
			tickTime = Integer.MAX_VALUE;
		}
	}
	private CountdownStats cds = new CountdownStats();
	private boolean countdownHurry;
	private long lastReadysUpdate;
	private int lastCountdownTick;

	// GROUPS
	private int btnHeight;

	// PLAYERS
	private float joiningPlayersX, joiningPlayersY, joiningPlayersW, joiningPlayersH, joiningPlayersN = 5;

	// readyspot
	private final UIWindowInfo controlWindow;

	// LOBBY
	private final UILabel countdownLabel;

	private final UIButton<?> readyBtn, objectivesBtn, returnToFinishWindowBtn;

	public static UITextField chatInput;
	private static UIScrollable chatOutput;

	private final Race race;

	private GameRemoteMaster game;

	private GameInfo com;

	// SUBSCENE

	private final Subscene[] subscenes;
	private final UpgradesSubscene upgradesSubscene;

	private final CarChoiceSubscene carChoiceSubscene;

	private int currentSubscene;

	private String[] extraPlayersListText;
	private final UIScrollable objectives;

	private final LobbyTopbar lobbyTopbar;

	private int aTimes;

	private int controllerHoverIndex, controllerHoverIndexMine;
	private boolean changingShowPlayer;

	public void setCurrentSubscene(int currentSubscene) {
		this.currentSubscene = currentSubscene;
		if (chatOutput == null)
			return;
		if (currentSubscene == upgradesSubscene.getIndex()) {
			setChatPosition(upgradesSubscene.getCarInfoX(), 0);
			controlWindow.visible = true;
		} else {
			setChatPosition(Window.WIDTH, -1f);
			controlWindow.visible = false;
		}
	}
	
	public static void setChatPosition(float x, float widthSelf) {
		chatOutput.getWindow().setPosition(x + widthSelf * chatOutput.getWindow().width, chatOutput.getWindow().y);
		chatInput.getWindow().setPosition(x + widthSelf * chatOutput.getWindow().width, chatInput.getWindow().y);
	}

	public Lobby(Race race, LobbyTopbar topbar) {
		super(topbar, Scenes.LOBBY);
		this.race = race;
		this.lobbyTopbar = topbar;

		// Init shit

		readyBtn = new UIButton<>("");
		objectivesBtn = new UIButton<>(Texts.objectives);
		returnToFinishWindowBtn = new UIButton<>("Previous results");
		countdownLabel = new UILabel(Nuklear.NK_TEXT_ALIGN_RIGHT);

		updateResolution();
		float ssX = 0;
		float sceneChangeH = btnHeight * 1.1f;
		float ssY = (topbar != null ? topbar.getHeight() : 0);
		float ssW = Window.WIDTH * 3f / 5f;
		float ssH = Window.HEIGHT - ssY - sceneChangeH;

		float rectHeight = (Window.HEIGHT - (topbar != null ? topbar.getHeight() : 0)) / 2f;

		float playerListX = Window.WIDTH / 3f;
		float playerListY = Window.HEIGHT / 3f;
		float playerListWidth = Window.WIDTH - 2 * playerListX;
		float playerListHeight = Window.HEIGHT - 2 * playerListY;
		objectives = new UIScrollable(Scenes.GENERAL_NONSCENE, playerListX, playerListY, playerListWidth,
				playerListHeight);
		objectives.getWindow().z = 0;
		objectives.getWindow().visible = false;
		objectivesBtn.setHoverAction(() -> objectives.getWindow().visible = true);
		objectivesBtn.setHoverExitAction(() -> objectives.getWindow().visible = false);
		objectives.shadow = true;
		objectives.setPadding(24, 24);

		/*
		 * Subscene creation
		 */
		upgradesSubscene = new UpgradesSubscene(sceneIndex);
		upgradesSubscene.menu = topbar.menuWindow;
		carChoiceSubscene = new CarChoiceSubscene(sceneIndex);

		IActionDouble<Player, Integer> gamemodeChange = (player, i) -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			com.init(i);
			for (Player p : com.getPlayers()) {
				p.car.completeReset();
			}
		};

		subscenes = new Subscene[2];

		subscenes[0] = carChoiceSubscene;
		subscenes[1] = upgradesSubscene;

		for (int i = 0; i < subscenes.length; i++) {
			subscenes[i].setIndex(i);
			subscenes[i].createWindowsWithinBounds(ssX, ssY, ssW, ssH, joiningPlayersX);
			subscenes[i].createBackground();
			subscenes[i].setVisible(false);
			subscenes[i].readyBtn = readyBtn;
			subscenes[i].init();
		}

		carChoiceSubscene.initGameModeManipulation(gamemodeChange);
		setCurrentSubscene(carChoiceSubscene.getIndex());

		/*
		 * changing subscenes
		 */
		@SuppressWarnings("unchecked")
		final UIButton<Integer>[] subsceneTabs = new UIButton[subscenes.length];
		for (int i = 0; i < subscenes.length; i++) {
			subsceneTabs[i] = new UIButton<>(subscenes[i].getName());
			subsceneTabs[i].setPressedAction((newIndex) -> {
				audio.play(SfxTypes.REGULAR_PRESS);
				subscenes[currentSubscene].setVisible(false);
				setCurrentSubscene(newIndex);
				subscenes[currentSubscene].updateGenerally(SceneHandler.cam);
				subscenes[currentSubscene].setVisible(true);
			});
			subsceneTabs[i].setConsumerValue(i);
			add(subsceneTabs[i]);
		}

		topbar.setSubscenes(subsceneTabs);
		if (race != null)
			race.initWinVisual(upgradesSubscene);

		/*
		 * float rowSpacingY = Window.HEIGHT / 192.75f; String[] endGoalLines =
		 * endGoal.getText().split(";"); for (String line : endGoalLines) {
		 * nk_layout_row_dynamic(ctx, btnHeight / endGoalLines.length - rowSpacingY, 1);
		 * Nuklear.nk_label(ctx, line, Nuklear.NK_TEXT_ALIGN_LEFT |
		 * Nuklear.NK_TEXT_ALIGN_MIDDLE); } countdownLabel.layout(ctx, stack);
		 */
		var controlW = TileVisual.size() * 2.90f;
		var controlWX = TileVisual.size() * 3.028f;
		var controlX = Window.WIDTH - controlWX;
		var controlH = (1f + 1.2f + .2f) * TileVisual.size();
		var controlY = Window.HEIGHT - (controlH + .1f * TileVisual.size());
		controlWindow = createWindow(controlX, controlY, controlW, controlH);

		upgradesSubscene.updateResolution();
		var chatY = upgradesSubscene.getTooltipY();
		var chatHeight = Window.HEIGHT - chatY;
		float chatInputHeight = rectHeight / 10f;
		float chatOutputHeight = chatHeight - chatInputHeight;
		chatOutput = new UIScrollable(Scenes.GENERAL_NONSCENE, ssW, chatY, upgradesSubscene.getCarInfoWidth(), chatOutputHeight);
		chatOutput.setBottomHeavy(true);
		chatOutput.getWindow().options = Nuklear.NK_WINDOW_NO_INPUT | Nuklear.NK_WINDOW_NO_SCROLLBAR
				| Nuklear.NK_WINDOW_TITLE;

		chatInput = new UITextField(Texts.chatHere, true, false, 28, Scenes.GENERAL_NONSCENE, ssW, chatY + chatOutputHeight,
				upgradesSubscene.getCarInfoWidth(), chatInputHeight);
		chatInput.leanTowardsLastInfinite = true;
		chatInput.setSpecialInputAction((keycode) -> {
			// Pressed enter
			if (chatInput.isFocused() && !chatInput.getText().isEmpty())
				if (keycode == GLFW.GLFW_KEY_ENTER || keycode == GLFW.GLFW_KEY_KP_ENTER) {
					String text = chatInput.getText() + "#"
							+ String.format("%.1f", 0.4f + (float) Features.ran.nextInt(160) / 100f);
					com.sendChat(text);
					chatInput.setText("");
					// SceneHandler.instance.getWindows().requestFocus();
				}
		});

		/*
		 * Right manipulation
		 */
		IAction readyAction = () -> {
			boolean ready = (com.player.ready + 1) % 2 != 0;

			if (ready) {
				audio.play(SfxTypes.READY);
				com.player.setHistoryNow();
				if (com.isSingleplayer()) {
					raceStarted();
				}
			} else {
				audio.play(SfxTypes.UNREADY);
			}
			UISceneInfo.clearHoveredButton(Scenes.LOBBY);

			// and show car spinning
			com.ready(com.player, ready ? (byte) 1 : (byte) 0);
		};

		readyBtn.setPressedAction(readyAction);
		UIButton<?> goBack = new UIButton<>(Texts.leaveText);
		goBack.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			if (com.player.isReady()) {
				readyAction.run();
			}
			topbar.hideMenu();

			UIConfirmModal.show(Texts.confirmLeave, () -> {
				if (com.player.isHost() && com.isLAN() && com.isGameStarted() && !com.isGameOver()) {
					audio.play(SfxTypes.REGULAR_PRESS);
					UIConfirmModal.show("YOU ARE THE HOSTING A LAN LOBBY! IF YOU LEAVE THE SERVER WILL BE SHUT DOWN! ARE YOU REALLY SURE?", () -> {
						game.leaveGame = true;
						audio.play(SfxTypes.LEFT);
					});
				} else {
					audio.play(SfxTypes.LEFT);
					game.leaveGame = true;
				}
			});
		});
		UIButton<?> resignBtn = new UIButton<>(Texts.resignText);
		resignBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			if (com.player.isReady()) {
				readyAction.run();
			}
			topbar.hideMenu();

			UIConfirmModal.show(Texts.confirmResign, () -> {
				audio.play(SfxTypes.REGULAR_PRESS);
				com.resign();
				sceneChange.change(Scenes.RACE, false);
			});
		});

		UIButton<?> options = new UIButton<>(Texts.optionsText);
		options.setPressedAction(() -> {
			com.ready(com.player, (byte) 0);
			topbar.hideMenu();
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.OPTIONS, true);
		});
		returnToFinishWindowBtn.setPressedAction(() -> {
			com.ready(com.player, (byte) 0);
			topbar.hideMenu();
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.RACE, true);
		});

		/*
		 * Quit
		 */
		game = new GameRemoteMaster(() -> {
			if (game != null) {
				game.leaveGame = false;
			}
			System.out.println("goback game exit");

			topbar.hideMenu();
			boolean sp = com.isSingleplayer();
			if (com != null) {
				if (com.player != null) {
					com.leave(com.player, true, false);
					Features.inst.leave();

					com.player = null;
//				comparedStats = null;
				}
				com.close();
				com = null;
			} else {
				Features.inst.leave();
			}

			upgradesSubscene.reset();

			extraPlayersListText = null;

			race.turnOff();

//			FIXME sceneChange.change(Scenes.PREVIOUS_REGULAR, false);

			sceneChange.change(sp && !Main.DEMO ? Scenes.SINGLEPLAYER : Scenes.MAIN_MENU, false);
		});

		add(readyBtn);
		add(objectivesBtn);
		add(returnToFinishWindowBtn);
		add(goBack);
		add(resignBtn);
		add(options);
		UISceneInfo.addPressableToScene(Scenes.RACE, options);
		UISceneInfo.addPressableToScene(Scenes.RACE, goBack);

		topbar.setLobbyButtons(goBack, resignBtn, options, () -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.DESIGNER_NOTES, true);
		});

		goBack.setNavigations(null, null, null, () -> options);
		readyBtn.setNavigations(() -> {
			UIButton<?> btn = subscenes[currentSubscene].intoNavigationSide();
			if (btn == null)
				btn = readyBtn;
			return btn;
		}, null, () -> options, null);
		options.setNavigations(null, null, () -> goBack, () -> readyBtn);

		GameInfo.bonusModal = new UIBonusModal((goldType) -> {

			var res = GameInfo.bonusModal.select(com.player, com.store, goldType);
			if (res != UpgradeResult.DidntGoThrough) {
				audio.play(SfxTypes.boltBonus(goldType + 1));
				if (res == UpgradeResult.Bought) {
					GameInfo.bonusModal.setVisible(false);
					com.updateCloneToServer(com.player, Translator.getCloneString(com.player));
					upgradesSubscene.reactBonus(true);
//					upgradesSubscene.updateReadyBtnEnabled(readyBtn, com.player);
				} else {
					audio.play(SfxTypes.NEW_BONUS);
					GameInfo.bonusModal.press();
				}
			} else {
				audio.play(SfxTypes.BUY_FAILED);
			}
		}, () -> {
			audio.stop(SfxTypes.NEW_BONUS);
			audio.play(SfxTypes.CANCEL_BONUS);
			GameInfo.bonusModal.cancel(com.player);
			GameInfo.bonusModal.hide();
			upgradesSubscene.reactBonus(false);
			press();
		});

		// createOnlineBtn.setNavigations(joinOnlineBtn, null, null,
		// refreshBtn);
		// gobackBtn.setNavigations(null, refreshBtn, joinOnlineBtn, null);
		// refreshBtn.setNavigations(gobackBtn, null, createOnlineBtn, null);
	}

	@Override
	public void finalizeInit(AudioRemote audio, SceneChangeAction sceneChange) {
		this.audio = audio;
		this.sceneChange = sceneChange;

		for (Subscene ss : subscenes) {
			ss.setAudio(audio);
			ss.setSceneChangeAction(sceneChange);
		}
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		press();
		subscenes[currentSubscene].updateGenerally(cam);
		subscenes[currentSubscene].setVisible(true);

		if (!Main.NO_SOUND)
			audio.setListenerData(0, 0, 0);

		for (var car : CarChoiceSubscene.CARS) {
			if (car == null)
				continue;
			car.reset();
			car.setRotation(new Vec3(0));
			car.resetTransformation();
		}

		objectives.getWindow().visible = false;

		game.darkmode = false;
		lobbyTopbar.menuButton.setColor(null);
		lobbyTopbar.press();

		aTimes = 0;
	}

	@Override
	public void updateResolution() {
		btnHeight = Window.HEIGHT / 12;

		if (subscenes != null) {
			for (Subscene ss : subscenes) {
				ss.updateResolution();
			}
		}

		var icon = CarChoiceSubscene.CHARACTERS[0];
		if (icon == null)
			return;
		joiningPlayersW = 2f * icon.getWidth();
		joiningPlayersH = icon.getHeight();
		joiningPlayersX = Window.WIDTH - joiningPlayersW - icon.getWidth();
	}

//	FIXME BRUKES DENNE? public void showNoUpgrades() {
//		upgradesSubscene.setUpgradeDetails(new IUIObject[0]);
//	}
//	
//	public void showUpgrades(TileUpgrade currentUpgrade, Vec2 pos, boolean selected, boolean placed) {
//		if (currentUpgrade != null) {
////			float cost = storeHandler.getSelectedUpgradeCost(player.bank);
////			comparedStats = storeHandler.getSelectedUpgradeCarRep(player, currentUpgrade.getUpgrade(), 
////					currentUpgrade.getLayerPos() != null ? currentUpgrade.getLayerPos() : pos);
//		} else {
//			showNoUpgrades();
//		}
//	}

	/**
	 * - outtext from server
	 */
	private void updatePlayerList(List<PlayerLobbyInfo> playerLobbyInfo, String endGoal, String[] extraPlayersListText,
			UIScrollable playerList, UIScrollable objectives) {
		objectives.setText(endGoal);

		playerList.clear();
		playerList.addText(new UILabelRow(Texts.getPlayerListTitles(), playerList.getRowHeight(),
				Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_TOP));

		var options = Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE;
		for (var playerInfo : playerLobbyInfo) {
			playerList.addText(new UILabel(playerInfo.basicInfo(), options));
			var carInfoRow = new UILabelRow(playerInfo.carInfo(), playerList.getRowHeight(), options);
			playerList.addText(carInfoRow);
			playerList.addText(new UILabel());
		}

		if (extraPlayersListText != null) {
			playerList.addText(UILabel.create(extraPlayersListText));
		}
		if (Main.DEBUG) {
			for (int i = 0; i < 40; i++)
				playerList.addText(new UILabel("test"));
		}
	}

	private void updatePlayerListState(List<PlayerLobbyInfo> playerLobbyInfo, String endGoal) {
		updatePlayerList(playerLobbyInfo, endGoal, extraPlayersListText, game.playerList, objectives);
	}

	public static String newlineText(String newText, final int nlJump) {
		var sb = new StringBuilder();
		int p = nlJump, prevP = 0, firstSpace = newText.indexOf(' ');
		while (p < newText.length()) {
			var splicedStr = newText.substring(prevP, p);
			int lastNL = splicedStr.indexOf('\n');
			if (lastNL != -1) {
				splicedStr = splicedStr.substring(0, lastNL);
				sb.append(splicedStr).append('\n');
				prevP += lastNL + 1;
				p = prevP + nlJump;
				continue;
			}
			int lastSpace = splicedStr.lastIndexOf(' ');
			if (lastSpace == -1 || lastSpace == firstSpace) {
				sb.append(splicedStr);
				sb.append("\n");
				prevP = p;
				p += nlJump;
				continue;
			}

			sb.append(splicedStr, 0, lastSpace);
			sb.append("\n");

			int nextIndex = -1;
			int i = 0;
			for (var c : newText.substring(prevP + lastSpace).toCharArray()) {
				if (c != ' ') {
					nextIndex = prevP + lastSpace + i;
					break;
				}
				i++;
			}
			if (nextIndex == -1) {
				prevP = p;
				p += nlJump;
			} else {
				prevP = nextIndex;
				p = nextIndex + nlJump;
			}
		}
		sb.append(newText.substring(prevP));
		return sb.toString();
	}

	private void addChatToChatWindow(String newText) {
		if (newText != null) {

			// Adding text to the chatwindow
			String[] pitchSplit = newText.split("#");
			if (pitchSplit.length > 1)
				newText = newText.substring(0, newText.length() - 1 - pitchSplit[pitchSplit.length - 1].length());

			chatOutput.addText(newlineText(newText, 30) + "\n");

			/*
			 * TAUNT(s)
			 */
			String[] tauntCheck = newText.split(": ");

			Source taunt = null;
			String pattern = "\\d+";
			String twice = null;
			if (tauntCheck.length > 1 && tauntCheck[1].length() >= 1) {
				String single = tauntCheck[1].substring(0, 1);

				if (tauntCheck[1].length() >= 2) {
					twice = tauntCheck[1].substring(0, 2);
				}

				if (twice != null && twice.matches(pattern)) {
					int twiceTaunt = Integer.parseInt(twice);
					taunt = audio.getTaunt(twiceTaunt);
				} else if (single.matches(pattern)) {
					int singleTaunt = Integer.parseInt(single);
					taunt = audio.getTaunt(singleTaunt);
				}

			}
			if (taunt != null)
				taunt.play(Float.parseFloat(pitchSplit[pitchSplit.length - 1].replace(',', '.')));
			else
				audio.play(SfxTypes.CHAT);
		}
	}

	/**
	 * @param name - username
	 * @param role - int value (0,1) represents boolean
	 */
	public void createNewLobby(String name, byte role, GameType multiplayerType, int arg) {
		var player = joinNewLobby(name, role, 0, multiplayerType, null, arg);

		if (player == null || player.id == Player.DEFAULT_ID) { // if -200 its failed
			game.endAll();
			return;
		}

		afterJoined();
		if (sceneChange != null)
			sceneChange.change(Scenes.LOBBY, true);
	}

	/**
	 * @param name   - username
	 * @param hostID - int value (0,1) represents boolean
	 */
	public Player joinNewLobby(String name, byte role, long hostID, GameType multiplayerType, String ip, int arg) {
		if (GameInfo.exists)
			return null;

		com = new GameInfo(game, multiplayerType, ip);
		var player = new Player(name, Player.DEFAULT_ID, role, Features.inst.getMyDestId(multiplayerType));
		cds.reset();
		lastCountdownTick = -1;
		lastReadysUpdate = 0;
		countdownHurry = false;

		/*
		 * Bonus modal
		 */
		for (var ss : subscenes)
			ss.setCom(com);

		// Client
		Consumer<Player> afterJoined = null;
		if (role < Player.HOST) {
			afterJoined = p -> {
				if (p == null) {
					System.out.println("Could not join! Ending in afterjoined");
					game.endAll();
					return;
				}

				afterJoined();
				sceneChange.change(Scenes.LOBBY, true);
			};
		}

		try {
			player = com.join(player, player.isHost() ? GameInfo.JOIN_TYPE_VIA_CREATOR : GameInfo.JOIN_TYPE_VIA_CLIENT,
					afterJoined, hostID, arg);

		} catch (NullPointerException e) {
			if (Main.DEBUG)
				e.printStackTrace();
			System.out.println("ERROR IN JOINING NEW LOBBY: closed down! " + e.getMessage());
			game.leaveGame = true;
		}

		return player;
	}

	private void afterJoined() {
		UISceneInfo.clearHoveredButton(sceneIndex);
		lobbyTopbar.com = com;

		if (!com.isGameStarted()) {
			setCurrentSubscene(carChoiceSubscene.getIndex());
//			readyBtn.setEnabled(true);
			carChoiceSubscene.afterJoined();
		} else {
			setCurrentSubscene(upgradesSubscene.getIndex());
//			if (com.getGamemode().isRacing()) {
//				raceStarted();
//			} else {
//			upgradesSubscene.updateReadyBtnEnabled(readyBtn, com.player);
//			}
		}

		upgradesSubscene.afterJoined();

		if (!com.isSteam())
			RSet.set(RSet.discID, com.player.steamID);

		com.setActionUpdatedGameMode((gm) -> {
			if (!gm.isGameBegun()) {
				carChoiceSubscene.updatePrices();
			}
			com.updateLobbyString();
		});
		if (!com.runUpdatedGameModeAction()) {
			game.endAll();
			return;
		}

		com.setActionUpdateLobbyString(this::updatePlayerListState);
		com.updateLobbyString();

		com.setActionNewChat(this::addChatToChatWindow);

		chatOutput.clear();
		if (com.isSingleplayer())
			chatOutput.getWindow().name = "Log:";
		else
			chatOutput.getWindow().name = "Chat:";

		chatOutput.setText("To use taunts write numbers!\n");

		updateDebug();
	}

	public void updateDebug() {
		((LobbyTopbar) topbar).setTabsVisible(Main.DEBUG);

		if (com == null)
			return;

		if (Main.DEBUG) {
//			com.player.bank.replaceSale(-1, 0, 0);
			com.player.bank.add(410, Bank.MONEY);
		}
		// else {
//			com.player.bank.replaceSale(1, 0, 0);
//		}
	}


	/**
	 * @return start race
	 */
	public static boolean countdownTechnical(GameInfo com, boolean startedRace, CountdownStats cds) {
		// Start the countdown
		var now = System.currentTimeMillis();
		var paused = com.isCountdownPaused() || !com.isBehindRounds();
		if (paused)
			com.roundRaceCountdown();
		var countdownTemp = com.getCountdown() - now;
		if (com.doubleCheckStartedRace() || (!paused && countdownTemp <= 0)) {
            return com.isBehindRounds() || !startedRace;
		}

		cds.tickTime = (int) (countdownTemp / 1000) + 1;

		if (!paused && com.isEveryoneReady()) {
			if (!com.isGameStarted()) {
				return true;
			}
			com.setCountdown(now);
		}
		if (now > nextCountdownUpdate) {
			nextCountdownUpdate = now + 1000;
			com.updateRaceCountdown(com.player, true, true);
		}

		return false;
	}

	public void countdown() {
		if (countdownTechnical(com, game.started, cds)) {
			raceStarted();
			return;
		}
		if (cds.tickTime <= 10) {
			if (!countdownHurry) {
				countdownHurry = true;
			}
			if (cds.tickTime > 0 && lastCountdownTick != cds.tickTime) {
				lastCountdownTick = cds.tickTime;
				audio.play(SfxTypes.COUNTDOWN);
			}
		}
		countdownLabel.setText("Starting: " + cds.tickTime);

		if (System.currentTimeMillis() > lastReadysUpdate || lastReadysUpdate == 0) {
			lastReadysUpdate = System.currentTimeMillis() + (Main.DEBUG ? 1000000 : 3000);
			com.updateRaceCountdown(com.player, true, true);
		}
	}

	private void raceStarted() {
		if (!race.isInitiated()) {
			Layer.FINALIZED_SEED = 0;
			if (com.isSingleplayer()) {
				com.carSelectUpdate(com.player, com.player.getCarNameID(), false, com.player.getCarRep().isRandom(),
						false);
			} else if (com.player.isHost()) {
				if (com.getRemote().way instanceof LocalRemote2 lanRemote) {
					lanRemote.closeUDP();
				} else {
					Features.inst.startLobby();
				}
			}

			if (com.getGamemode() instanceof CoopMode coop) {
				coop.initPlayers();
			}
			race.initRestBeforeFirstRace(com, game);
		}
		if (com.player.resigned)
			return;

		if (com.startRace(System.currentTimeMillis())) {
			audio.play(SfxTypes.START);
			audio.updateCarAudio(com.player.car);
			game.started = true;
			countdownHurry = false;

			upgradesSubscene.closeAllBeforeRace();

			com.clearRaceCountdown();
			setCurrentSubscene(upgradesSubscene.getIndex());
			subscenes[currentSubscene].setVisible(true);
			com.player.car.reset(com.player.layer);

			race.setRaceLightsFromNow();
			race.initWindow();
			game.showPlayerList = false;

			com.getGamemode().moneyExplaination = null;
			com.player.clearRedo();
		}
		chatInput.unfocus(true);
		sceneChange.change(Scenes.RACE, false);
		lastCountdownTick = Integer.MAX_VALUE;
		System.out.println("create");
	}

	public static void chatInput(String c) {
		if (chatInput.isFocused()) {
			chatInput.addText(c);
		}
	}
	
	@Override
	public void charInput(String c) {
		chatInput(c);
	}

	@Override
	public void keyInput(int keycode, int action) {
		CurrentControls controls = CurrentControls.getInstance();
		game.keyInput(keycode, action);

		if (GameInfo.bonusModal.isVisible()) {
			GameInfo.bonusModal.input(keycode, action);
			return;
		}

		if (chatKeyInput(keycode, action)) {

			if (InputHandler.CONTROL_DOWN) {
				if (keycode == controls.getReady().getKeycode()) {
					readyBtn.runPressedAction();
				} else if (keycode == GLFW.GLFW_KEY_V) {
					var c = Toolkit.getDefaultToolkit().getSystemClipboard();
					var t = c.getContents(this);
					if (t == null || !t.isDataFlavorSupported(DataFlavor.stringFlavor))
						return;
					try {
						chatInput.addText((String) t.getTransferData(DataFlavor.stringFlavor));
					} catch (Exception e) {
						if (Main.DEBUG)
							e.printStackTrace();
						System.out.println("failed at pasting text in chatbox");
						return;
					}
				}
			}

			return;
		}
		currentSubscene().keyInput(keycode, action);

		if (action != GLFW.GLFW_RELEASE) {
			if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				((LobbyTopbar) topbar).menuButton.runPressedAction();
				return;
			}

			if (((LobbyTopbar) topbar).menuWindow.visible) {
				((LobbyTopbar) topbar).input(keycode);
				return;
			}

			if (currentSubscene == carChoiceSubscene.getIndex() || (com.player.historyHasBought() || Main.DEBUG)) {
				// Downstroke for quicker input
				generalHoveredButtonNavigation(readyBtn, keycode);

				if (keycode == controls.getReady().getKeycode()) {
					if (Main.DEBUG) {
						readyBtn.setEnabled(true);
					}
					readyBtn.runPressedAction();
				}
			}

			if (Main.DEBUG) {
				if (keycode == GLFW.GLFW_KEY_F3) {
					com.getGamemode().prepareNextRaceManually(Integer.MAX_VALUE);
				} else if (keycode == GLFW.GLFW_KEY_F4) {
					com.win();
				}
			}
		}
	}

	public static boolean chatKeyInput(int keycode, int action) {
		if (chatInput.isFocused()) {
			if (action != GLFW.GLFW_RELEASE && keycode == GLFW.GLFW_KEY_ESCAPE) {
				chatInput.unfocus(true);
			}

			chatInput.input(keycode, action);
			return true;
		}
		return false;
	}

	@Override
	public void controllerInput() {

		if (GameInfo.bonusModal.isVisible()) {
			GameInfo.bonusModal.controllerInput();
			return;
		}

		if (SceneHandler.modalVisible)
			return;
		game.controllerInput();
		if (!com.isSingleplayer()
				&& (upgradesSubscene.viewPlayerMinimally || InputHandler.BTN_MENU && currentSubscene == 1)) {
			if (!changingShowPlayer) {
				if (InputHandler.BTN_UP) {
					if (game.showPlayerList) {
						game.showPlayerList = false;
						controllerHoverIndex = controllerHoverIndexMine;
					}
					if (controllerHoverIndex > 0)
						controllerHoverIndex--;
					if (controllerHoverIndex == controllerHoverIndexMine && controllerHoverIndex > 0)
						controllerHoverIndex--;
					upgradesSubscene.viewPlayer(com.getSortedPlayers().get(controllerHoverIndex), false, true);
					changingShowPlayer = true;
					return;
				}
				if (InputHandler.BTN_DOWN) {
					if (game.showPlayerList) {
						game.showPlayerList = false;
						controllerHoverIndex = controllerHoverIndexMine;
					}
					if (controllerHoverIndex + 1 < com.getSortedPlayers().size())
						controllerHoverIndex++;
					if (controllerHoverIndex == controllerHoverIndexMine
							&& controllerHoverIndex + 1 < com.getSortedPlayers().size())
						controllerHoverIndex++;
					upgradesSubscene.viewPlayer(com.getSortedPlayers().get(controllerHoverIndex), false, true);
					changingShowPlayer = true;
					return;
				}
			}
			System.out.println("upp!" + InputHandler.BTN_UP);
			changingShowPlayer = InputHandler.BTN_UP || InputHandler.BTN_DOWN;
			if (!game.showPlayerListTabbing && upgradesSubscene.viewPlayerMinimally) {
				upgradesSubscene.viewPlayer(com.player, false, false);
			}
		}

		if (!InputHandler.HOLDING) {
			var tb = ((LobbyTopbar) topbar);
			if (InputHandler.BTN_Y) {
				tb.menuButton.runPressedAction();
			}

			if (InputHandler.BTN_B && currentSubscene == 0 && com.isSingleplayer())
				tb.leaveBtn.runPressedAction();

			if (tb.menuWindow.visible) {
				tb.controllerInput();
				return;
			}
		}

		currentSubscene().controllerInput();

		if (!InputHandler.HOLDING) {
			if (InputHandler.REPEAT || Math.abs(InputHandler.LEFT_STICK_X) > .3f
					|| Math.abs(InputHandler.LEFT_STICK_Y) > .3f || Math.abs(InputHandler.RIGHT_STICK_X) > .3f
					|| Math.abs(InputHandler.RIGHT_STICK_Y) > .3f || InputHandler.LEFT_TRIGGER > -.8f
					|| InputHandler.RIGHT_TRIGGER > -.8f) {

				if ((currentSubscene == 0 && InputHandler.BTN_A) || (currentSubscene != 0 && InputHandler.BTN_X)) {
					InputHandler.BTN_X = false;
					if (Main.DEBUG) {
						readyBtn.setEnabled(true);
					}
					if (aTimes == 1 || currentSubscene == 0) {
						readyBtn.runPressedAction();
					} else if (readyBtn.isEnabled()) {
						readyBtn.hover();
						readyBtn.hoverFake();
						aTimes++;
						audio.play(SfxTypes.READY);
					}
				} else {
					aTimes = 0;
					readyBtn.unhover();

				}
			}

			if (currentSubscene == 1) {
				if (InputHandler.BTN_BACK_TOP_RIGHT || InputHandler.BTN_UP) {
					objectivesBtn.unhover();
					objectivesBtn.hover();
				} else if (objectives.getWindow().visible) {
					objectives.getWindow().visible = false;
				}
			}
		}
	}

	@Override
	public void mouseScrollInput(float x, float y) {
		currentSubscene().mouseScrollInput(x, y);
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		boolean down = super.mouseButtonInput(button, action, x, y);

		if (lobbyTopbar.menuWindow.visible && !lobbyTopbar.menuWindow.isWithinBounds(x, y)) {
			lobbyTopbar.menuWindow.visible = false;
		}

		if (GameInfo.bonusModal.isVisible()) {
			if (down) {
				GameInfo.bonusModal.press();
			} else {
				GameInfo.bonusModal.release();
			}
		} else if (upgradesSubscene.menu == null || !upgradesSubscene.menu.visible) {
			currentSubscene().mouseButtonInput(button, down ? 1 : 0, x, y);
			if (down) {
				if (!com.isSingleplayer() && chatInput.tryFocus(x, y, true))
					updateGenerally(null);

				if (com.player.isHost()) {
					int n = 0;
					float halfHeight = CarChoiceSubscene.CHARACTERS[0].getHeight()
							* (com.getSortedPlayers().size() > joiningPlayersN ? .5f : 1f);
					for (var player : com.getSortedPlayers()) {
						if (x > joiningPlayersX && y >= joiningPlayersY + n * halfHeight
								&& y < joiningPlayersY + (n + 1) * halfHeight) {
							if (player.id == com.player.id)
								break;
							UIConfirmModal.show("Are you sure you want to kick " + player.name + "?",
									() -> com.leave(player, true, true));
							break;
						}
						n++;
					}
				}
			}

		}

		return false;
	}

	@Override
	public void mousePositionInput(float x, float y) {
		if (!GameInfo.bonusModal.isVisible() && (upgradesSubscene.menu == null || !upgradesSubscene.menu.visible)) {
			currentSubscene().mousePosInput(x, y);

			int n = com.getSortedPlayers().size();
			float halfHeight = n > joiningPlayersN ? .5f : 1f;

			if (!com.isGameStarted())
				return;

			if (!game.showPlayerListTabbing && x > joiningPlayersX
					&& (y > joiningPlayersY && y < (joiningPlayersY + n * joiningPlayersH))) {
				if (x < Window.WIDTH - CarChoiceSubscene.CHARACTERS[0].getWidth()) {
					game.showPlayerList = true;
				} else {
//					if (com.player.isReady()) {
					int p = (int) Math
							.floor((y - joiningPlayersY) / CarChoiceSubscene.CHARACTERS[0].getHeight() * halfHeight);
//					System.out.println("y: " + y);
//					System.out.println("joiningPlayersY: " + joiningPlayersY);
//					System.out.println("p: " + p);
					upgradesSubscene.viewPlayer(com.getSortedPlayers().get(p), false, true);
					game.showPlayerList = false;
				}
			} else {
				if (!game.showPlayerListTabbing) {
					game.showPlayerList = false;
				}
				if (upgradesSubscene.viewPlayerMinimally) {
					upgradesSubscene.viewPlayer(com.player, false, false);
				}
			}

		}
	}

	public static boolean decideEnableReadyBtn(GameInfo com) {
		if (com.getGamemode() instanceof SingleplayerChallengesMode sp) {
			return sp.canSaveMoney || UpgradesSubscene.canAffordSomething(com.player) == null;
		}
		
		if (com.getGamemode().isRacing()) return false;

		for (var p : com.getSortedPlayers()) {
			if (p.id != com.player.id && !p.isHost() && !p.joined) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void tick(float delta) {
		if (game.leaveGame) {
			game.endAll();
			return;
		}

		if (InputHandler.CONTROLLER_EFFECTIVELY) {
			controllerInput();
		}

		currentSubscene().tick(delta);
		GameInfo.bonusModal.tick(delta);

		if (com != null) {
			if (!com.isSingleplayer()) {
				countdown();
				com.updateLobbyString();
			}
			readyBtn.setEnabled(decideEnableReadyBtn(com));

			var now = System.currentTimeMillis();
			if (nextStatusUpdate < now) {
				nextStatusUpdate = now + 1000;
				com.sendStatusUpdate();
			}
		}
	}
	

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {

		currentSubscene().renderBackground(renderer);
		
		if (objectives.getWindow().visible)
			return;
		
		currentSubscene().renderGame(renderer, cam, window, delta);
		if (com == null || (com.isSingleplayer() && !com.isGameStarted()))
			return;
		int n = 0;

		float halfHeight = com.getSortedPlayers().size() > joiningPlayersN ? .5f : 1f;
		for (var player : com.getSortedPlayers()) {
//			if (player.equals(this.player)) continue;

			Sprite icon;
			var rep = player.getCarRep();
			if (currentSubscene() instanceof CarChoiceSubscene && rep.isRandom()) {
				icon = CarChoiceSubscene.CHARACTERS[CarChoiceSubscene.CHARACTERS.length - 1];
			} else {
				icon = CarChoiceSubscene.CHARACTERS[rep.getNameID()];
			}

			icon.setPositionY(joiningPlayersY + n * icon.getHeight() * halfHeight);
			icon.setPositionX(Window.WIDTH - icon.getWidth());
			renderer.renderOrthoMesh(icon);
			n++;
		}
	}
	
	public static void renderChat(NkContext ctx, MemoryStack stack) {
		Features.inst.pushBackgroundColor(ctx, UIColors.BLACK_TRANSPARENT);
		Features.inst.pushFontColor(ctx, UIColors.LBEIGE);

		chatOutput.layout(ctx, stack);
		chatInput.layout(ctx, stack);
		Features.inst.popFontColor(ctx);
		Features.inst.popBackgroundColor(ctx);
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		if (com == null)
			return;

		boolean singleplayerChallenge = com.getGamemode() instanceof SingleplayerChallengesMode;
		boolean onlyObjectives = objectives.getWindow().visible;
		if (onlyObjectives) {
			Features.inst.pushBackgroundColor(ctx, UIColors.LBEIGE);
			objectives.layout(ctx, stack);
			Features.inst.popBackgroundColor(ctx);
		} else {
			GameInfo.bonusModal.layout(ctx, stack);

			/*
			 * MAIN SHIT
			 */

			// subscene ( left side )
			currentSubscene().renderUILayout(ctx, stack);

			// now we begin on the right side

			/*
			 * Player list at the right top side
			 */
			if (!(currentSubscene() instanceof CarChoiceSubscene)) {
				game.renderUILayout(ctx, stack);
			}

			/*
			 * Chat / log
			 */
			if (!upgradesSubscene.viewPlayerMinimally) {
				if (!singleplayerChallenge) {
					renderChat(ctx, stack);
				}
			}

			var players = com.getSortedPlayers();
			int n = 0;
			int len = players.size();
			joiningPlayersY = len > 8 ? upgradesSubscene.getMoneyWindowY() : UpgradesSubscene.marginY;
			float height = joiningPlayersH;
			boolean halfHeight = len > joiningPlayersN;
			if (halfHeight) {
				height /= 2;
			}
			while (n < len) {
				Player player;
				try {
					player = players.get(n);
				} catch (IndexOutOfBoundsException ex) {
					n++;
					continue;
				}
				boolean lockedIn = player.ready != 0;
				boolean me = player.equals(com.player);
				if (me && !upgradesSubscene.viewPlayerMinimally)
					controllerHoverIndexMine = n;

				if (player.resigned) {
					Features.inst.pushFontColor(ctx, UIColors.BLACK);
					Features.inst.pushBackgroundColor(ctx, UIColors.DNF);
				} else if (lockedIn) {
					Features.inst.pushFontColor(ctx, UIColors.BLACK);
					if (!me) {
						if (upgradesSubscene.viewPlayerMinimally && player.id == upgradesSubscene.viewPlayerId)
							Features.inst.pushBackgroundColor(ctx, UIColors.MEDIUM_SPRING_GREEN);
						else
							Features.inst.pushBackgroundColor(ctx, UIColors.WON);
					} else
						Features.inst.pushBackgroundColor(ctx, UIColors.WON2);
				} else {
					Features.inst.pushFontColor(ctx, UIColors.WHITE);
					if (!me) {
						if (upgradesSubscene.viewPlayerMinimally && player.id == upgradesSubscene.viewPlayerId)
							Features.inst.pushBackgroundColor(ctx, UIColors.BLUSH);
						else
							Features.inst.pushBackgroundColor(ctx, UIColors.BLACK);
					} else
						Features.inst.pushBackgroundColor(ctx, UIColors.GUNMETAL);
				}

				if (!(singleplayerChallenge && !com.isGameStarted())) {

					Nuklear.nk_style_push_color(ctx, ctx.style().window().border_color(),
							UIColors.COLORS[UIColors.GRAY.ordinal()]);

					NkRect rect = NkRect.malloc(stack);
					rect.x(joiningPlayersX).y(joiningPlayersY + n * height).w(joiningPlayersW)
							.h(height * (halfHeight && n == len - 1 ? 2f : 1f));

					// Begin the window
					if (Nuklear.nk_begin(ctx, "player" + n, rect,
							Nuklear.NK_WINDOW_BORDER | Nuklear.NK_WINDOW_NO_SCROLLBAR)) {
						Nuklear.nk_layout_row_dynamic(ctx, height * 0.45f, 1);
						var name = player.name;
						if (!com.isSingleplayer()) {
							name = (player.isHost() ? "»" : "") + (me ? "> " : "") + name;
						}
						Nuklear.nk_label(ctx, name, Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE);

						var points = player.bank.getLong(Bank.POINT);
						String pointsStr;
						if (singleplayerChallenge)
							pointsStr = points + " attempt" + (points != 1 ? "s" : "") + " left";
						else
							pointsStr = points + " point" + (points != 1 ? "s" : "");
						if (!halfHeight) {
							Nuklear.nk_layout_row_dynamic(ctx, height * 0.45f, 1);
							Nuklear.nk_label(ctx, pointsStr, Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
						}
					}
					nk_end(ctx);

					Nuklear.nk_style_pop_color(ctx);
				}

				Features.inst.popBackgroundColor(ctx);
				Features.inst.popFontColor(ctx);
				n++;
			}
		}

		/*
		 * Buttons
		 */
		if (!upgradesSubscene.viewPlayerMinimally) {
			if (controlWindow.begin(ctx, stack, 0, 0, 0, 0)) {

				var h1 = .4f * TileVisual.size();
				var h2 = TileVisual.size();

				nk_layout_row_dynamic(ctx, h1, 1);
				objectivesBtn.alphaFactor = onlyObjectives ? .25f : 1f;
				objectivesBtn.layout(ctx, stack);
				if (!onlyObjectives) {
					nk_layout_row_dynamic(ctx, h1, 1);
					returnToFinishWindowBtn.layout(ctx, stack);
					nk_layout_row_dynamic(ctx, h1, 1);
					countdownLabel.layout(ctx, stack);
					nk_layout_row_dynamic(ctx, h2, 1);
					if (readyBtn.isHovered())
						Features.inst.pushFontColor(ctx, UIColors.LBEIGE);
					else
						Features.inst.pushFontColor(ctx, UIColors.BLACK);
					readyBtn.layout(ctx, stack);
					Features.inst.popFontColor(ctx);
				}
			}
			nk_end(ctx);
		}
	}

	/*
	 * Getters and setters
	 */
	public void setExtraPlayersListText(String[] extraPlayersListText) {
		this.extraPlayersListText = extraPlayersListText;
	}

	public Subscene currentSubscene() {
		return subscenes[currentSubscene];
	}

	public GameRemoteMaster getGame() {
		return game;
	}

	public GameInfo getCom() {
		return com;
	}

	@Override
	public void destroy() {
		for (Subscene ss : subscenes) {
			ss.destroy();
		}
	}

	public void updateBackgroundColor() {
		upgradesSubscene.updateBackgroundColor();
	}

//	public void doneRacing() {
//		upgradesSubscene.updateReadyBtnEnabled(readyBtn, com.player);
//	}

	@Override
	public IUIObject getTopbarRenderable() {
		return objectives.getWindow().visible ? null : upgradesSubscene.viewPlayerMinimally ? null : (IUIObject) topbar;
	}

	public UpgradesSubscene getUpgradesSubscene() {
		return upgradesSubscene;
	}
	
	public void reset() {
		cds.reset();
		lastCountdownTick = -1;
		lastReadysUpdate = 0;
		countdownHurry = false;
		afterJoined();
	}

}