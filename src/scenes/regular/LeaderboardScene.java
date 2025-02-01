package scenes.regular;

import java.util.Stack;

import engine.graphics.interactions.RegularTopbar;
import game_modes.SingleplayerChallenges;
import game_modes.SingleplayerChallengesMode;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLeaderboardEntriesHandle;
import com.codedisaster.steamworks.SteamLeaderboardEntry;
import com.codedisaster.steamworks.SteamLeaderboardHandle;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamUserStats;
import com.codedisaster.steamworks.SteamUserStats.LeaderboardDataRequest;
import com.codedisaster.steamworks.SteamUserStats.LeaderboardUploadScoreMethod;
import com.codedisaster.steamworks.SteamUserStatsCallback;

import adt.IAction;
import audio.SfxTypes;
import engine.graphics.interactions.TransparentTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UILeaderboardPlayer;
import engine.graphics.ui.UIRow;
import engine.graphics.ui.UIScrollable;
import engine.graphics.ui.UIWindowInfo;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Main;
import main.Texts;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Scene;
import settings_and_logging.RSet;

public class LeaderboardScene extends Scene implements SteamUserStatsCallback {

	static class MyScores {
		int score, ranking;
	}

	private static SteamUserStats userStats;
	private static SteamLeaderboardHandle[] leaderboardHandles, carHandles;
	private static MyScores[] leaderboardMyScores;

	// top part
	private float topRowHeight;
	private final UIButton<?> goBackBtn, refreshBtn;
	private final UIWindowInfo topWindow;
	// rest
	private final UIScrollable leaderboardList;
	private int type, pageFrom = 0, pageTo = 100;

	private final static Stack<IAction> getterMultiLeaderboard = new Stack<>();
	private static int carOnLeaderboardBuffer;
	private String yourScore = "";

	private boolean foundChallengeScore;
	private int leaderboardsToCheckIndex;

	private IAction doneLoading;
	private long controllerScroll;
	private int titleType;

	public LeaderboardScene(TransparentTopbar topbar, IAction doneLoading) {
		super(topbar, Scenes.LEADERBOARD);
		this.doneLoading = doneLoading;

		leaderboardHandles = new SteamLeaderboardHandle[Texts.singleplayerModes.length];
		carHandles = new SteamLeaderboardHandle[leaderboardHandles.length];
		leaderboardMyScores = new MyScores[leaderboardHandles.length];
		leaderboardsToCheckIndex = leaderboardHandles.length - 1;

		goBackBtn = new UIButton<>(Texts.gobackText);
		refreshBtn = new UIButton<>(Texts.refreshText);

		float width = Window.WIDTH / 2f;
		topWindow = createWindow(Window.WIDTH / 2f - width / 2, topbar.getHeight(), width, Window.HEIGHT / 9f);
		leaderboardList = new UIScrollable(sceneIndex, Window.WIDTH / 2f - width / 2, topWindow.getYHeight(), width,
				Window.HEIGHT - topWindow.getYHeight());

		goBackBtn.setPressedAction(() -> {
			sceneChange.change(Scenes.PREVIOUS, true);
			audio.play(SfxTypes.REGULAR_PRESS);
		});

		refreshBtn.setPressedAction(() -> {
			refreshLeaderboard();
			audio.play(SfxTypes.REGULAR_PRESS);
		});

		add(goBackBtn);
		add(refreshBtn);
		
		goBackBtn.setNavigations(null, refreshBtn, null, null);
		refreshBtn.setNavigations(goBackBtn, null, null, null);
	}

	private void doneLoading() {
		doneLoading.run();
		doneLoading = null;
		foundChallengeScore = true;
	}
	
	public static int convertLeaderboardIndex(int i) {
		var converted = switch (SingleplayerChallenges.values()[i]) {
			case Beginner: yield 0;
			case Casual: yield 1;
			case Intermediate: yield 2;
			case Hard: yield 3;
			case Harder: yield 14;
			case Master: yield 9;
			case Samurai: yield 10;
			case Expert: yield 4;
			case Accomplished: yield 11;
			case Sensei: yield 13;
			case Legendary: yield 5;
			case Nightmarish: yield 6;
			case Unfair: yield 7;
			case Unfaircore: yield 8;
			case TheBoss: yield 12;
			default:
				yield i;
		};
		return converted;
	}

	private boolean findHighestChallengeScore() {
		if (!foundChallengeScore) {
			// Hent ut for å oppdatere challengeLVL og å kunne vise din score uten å måtte finne den fra de 1000 scores
			if (leaderboardHandles[leaderboardsToCheckIndex - 1] == null)
				return true;
			userStats.downloadLeaderboardEntriesForUsers(leaderboardHandles[--leaderboardsToCheckIndex], new SteamID[] {Features.inst.getSteamHandler().getMySteamID()});
			if (leaderboardsToCheckIndex == 0) {
				doneLoading();
			}
		}
		return foundChallengeScore;
	}

	public void createLeaderboard() {
		try {
			userStats = new SteamUserStats(this);
			findLeaderboard(0, false);
		} catch (UnsatisfiedLinkError e) {
			SceneHandler.showMessage("You're not connected to steam!");
		}
	}

	public void findLeaderboard(int i, boolean car) {
		System.out.println("Looking for leaderboard: " + i + ", car: " + car);
		if (!car) {
			if (i != 1)
				userStats.findLeaderboard("Challenge" + i);
			else
				userStats.findLeaderboard("Score Challenge");
		} else {
			userStats.findLeaderboard("ChallengeCar" + i);
		}
	}

	public void setLeaderboard(int i) {
		this.type = convertLeaderboardIndex(i);
		titleType = i;
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		GL11.glClearColor(0, 0, 0, 1);
		if (args.length > 0) {
			type = args[0];
			titleType = args[1];
		}
		pageFrom = 0;
		pageTo = 50;
		refreshLeaderboard();
	}

	@Override
	public void updateResolution() {
		topRowHeight = Window.HEIGHT / 16f;
	}

	@Override
	public void keyInput(int keycode, int action) {
		generalHoveredButtonNavigation(goBackBtn, keycode);

		if (keycode == GLFW.GLFW_KEY_ESCAPE)
			goBackBtn.runPressedAction();
	}
	
	@Override
	public void controllerInput() {
		generalHoveredButtonNavigationJoy(goBackBtn);
		if (InputHandler.BTN_B)
			goBackBtn.runPressedAction();
		
		if (InputHandler.BTN_UP) {
			leaderboardList.getWindow().focus = true;
			leaderboardList.scroll(1);
			return;
		} 
		if (InputHandler.BTN_DOWN) {
			leaderboardList.getWindow().focus = true;
			leaderboardList.scroll(-1);
			return;
		}
		
		if (System.currentTimeMillis() > controllerScroll) {
			if (Math.max(InputHandler.RIGHT_STICK_Y, InputHandler.LEFT_STICK_Y) > .1) {
				leaderboardList.getWindow().focus = true;
				leaderboardList.scroll(-1);
				controllerScroll = System.currentTimeMillis() + 45;
			} else if (Math.min(InputHandler.RIGHT_STICK_Y, InputHandler.LEFT_STICK_Y) < -.1) {
				leaderboardList.getWindow().focus = true;
				leaderboardList.scroll(1);
				controllerScroll = System.currentTimeMillis() + 45;
			}
		}
	}

	@Override
	public void mouseScrollInput(float x, float y) {
		leaderboardList.scroll(y);
	}

	@Override
	public void mousePositionInput(float x, float y) {
	}

	@Override
	public void tick(float delta) {
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {

		if (topWindow.begin(ctx)) {
			Nuklear.nk_layout_row_dynamic(ctx, topRowHeight * .45f, 2);
			goBackBtn.layout(ctx, stack);
			refreshBtn.layout(ctx, stack);
			Features.inst.pushFontColor(ctx, UIColors.WHITE);
			Nuklear.nk_layout_row_dynamic(ctx, topRowHeight * .55f, 1);
			Nuklear.nk_label(ctx, leaderboardList.getWindow().name,
					Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);
			Nuklear.nk_layout_row_dynamic(ctx, topRowHeight * .55f, 1);
			Nuklear.nk_label(ctx, yourScore, Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);
		}
		Nuklear.nk_end(ctx);

		leaderboardList.layout(ctx, stack);
		Features.inst.popFontColor(ctx);
	}

	@Override
	public void onUserStatsReceived(long gameId, SteamID steamIDUser, SteamResult result) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onUserStatsStored(long gameId, SteamResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserStatsUnloaded(SteamID steamIDUser) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onUserAchievementStored(long gameId, boolean isGroupAchievement, String achievementName,
			int curProgress, int maxProgress) {
	}

	@Override
	public void onLeaderboardFindResult(SteamLeaderboardHandle leaderboard, boolean found) {
		System.out.println("Found leaderboard: " + found);
		if (found && leaderboard != null) {
			String name = userStats.getLeaderboardName(leaderboard).toLowerCase();
			int type = 0;
			boolean aboveTen = false;
			try {
				if (name.charAt(name.length() - 2) == '1') {
					type = Integer.parseInt(name.substring(name.length() - 2));
					aboveTen = true;
				} else {
					type = Integer.parseInt(name.substring(name.length() - 1));
				}
			} catch (Exception e) {
				type = 1;
			}
			if ((!aboveTen && name.startsWith("car", name.length() - 4))
				||	(aboveTen && name.startsWith("car", name.length() - 5))
					) {
				LeaderboardScene.carHandles[type] = leaderboard;
			} else {
				LeaderboardScene.leaderboardHandles[type] = leaderboard;
			}

			// et leaderboard om gangen
			for (int i = 0; i < Texts.singleplayerModes.length; i++) {
				if (leaderboardHandles[i] == null) { 
					findLeaderboard(i, false);
					return;
				} else if (carHandles[i] == null) {
					findLeaderboard(i, true);
					return;
				}
			}
			System.out.println("Done looking for leaderboards");

			leaderboardsToCheckIndex = leaderboardHandles.length;
			findHighestChallengeScore();

			if (Scenes.CURRENT == sceneIndex)
				refreshLeaderboard();
		}
	}

	private void refreshLeaderboard() {
		leaderboardList.setText("Loading...");
		yourScore = "...";

		if (leaderboardHandles[type] == null || carHandles[type] == null) {
			if (leaderboardHandles[0] == null)
				createLeaderboard();
			return;
		}

		userStats.downloadLeaderboardEntries(leaderboardHandles[type], LeaderboardDataRequest.Global, pageFrom, pageTo);
		leaderboardList.getWindow().name = "Leaderboard: " + Texts.leaderboardScoreName(titleType);
	}

	private static void getMulti() {
		if (!getterMultiLeaderboard.isEmpty())
			getterMultiLeaderboard.pop().run();
	}

	public static void newScore(boolean survived, SingleplayerChallenges challengeLevelUnfiltered, int score, int car) {
		int challengeLevel = convertLeaderboardIndex(challengeLevelUnfiltered.ordinal());
		if (Main.DEMO || !Main.STEAM || leaderboardHandles[challengeLevel] == null)
			return;

		LeaderboardScene.carOnLeaderboardBuffer = ((int) Math.ceil((double) score / 1500d) * 1000) + car;
		score = Main.DEBUG ? 200 : score;
		userStats.uploadLeaderboardScore(leaderboardHandles[challengeLevel],
				Main.DEBUG ? LeaderboardUploadScoreMethod.ForceUpdate : LeaderboardUploadScoreMethod.KeepBest, score,
				new int[0]);

//		if (Main.DEBUG)
//			userStats.clearAchievement("NEW_ACHIEVEMENT_1_2");

		if (survived) {
			boolean ret = false;
			var achievement = "NEW_ACHIEVEMENT_1_" + switch (challengeLevelUnfiltered) {
				case Beginner: yield "8";
				case Casual: yield "13";
				case Intermediate: yield "14";
				case Hard: yield "15";
				case Master: yield "21";
				case Expert: yield "16";
				case Legendary: yield "17";
				case Nightmarish: yield "18";
				case Unfair: yield "19";
				case Unfaircore: yield "20";
				default: {
					ret = true;
					yield "";
				}
			};
			if (ret) return;
			userStats.setAchievement(achievement);
			
//			SceneHandler.showMessage("Congratulations! You have the biggest of brains!");
		}
	}

	@Override
	public void onLeaderboardScoresDownloaded(SteamLeaderboardHandle leaderboard, SteamLeaderboardEntriesHandle entries,
			int numEntries) {
		if (type >= carHandles.length) return;

		System.out.println("Leaderboard scores downloaded: handle=" + leaderboard.toString() + ", entries="
				+ entries.toString() + ", count=" + numEntries);

		int[] details = new int[16];
		int handleIndex = -1;
		for (int i = 0; i < leaderboardHandles.length; i++) {
			if (leaderboardHandles[i] != null && leaderboardHandles[i].equals(leaderboard)) {
				handleIndex = i;
				break;
			}
		}

		if (handleIndex != -1) {

			// player score
			leaderboardList.clear();
			leaderboardList
					.addText(
							new UIRow(
									new IUIObject[] { 
											new UILabel("Name:", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE),
											new UILabel("Score:", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE),
											new UILabel("Round:", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE),
											new UILabel("Time:", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE),
											new UILabel("Car: ", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE) },
									0));
			if (numEntries == 0) {
				leaderboardList.addText("No entries in this leaderboard",
						Nuklear.NK_TEXT_ALIGN_MIDDLE | Nuklear.NK_TEXT_ALIGN_CENTERED);

				findHighestChallengeScore();
			} else {
				boolean shallChange = handleIndex == type;
				if (shallChange)
					resetButtons();

				for (int i = 0; i < numEntries; i++) {

					SteamLeaderboardEntry entry = new SteamLeaderboardEntry();
					if (userStats.getDownloadedLeaderboardEntry(entries, i, entry, details)) {
						UILeaderboardPlayer btn = null;
						if (shallChange) {
							btn = new UILeaderboardPlayer(entry, titleType);
							leaderboardList.addText(btn);
						} 

						if (entry.getSteamIDUser().equals(Features.inst.getSteamHandler().getMySteamID())) {
							var scores = new MyScores();
							scores.score = entry.getScore();
							scores.ranking = entry.getGlobalRank();
							leaderboardMyScores[handleIndex] = scores;
							if (shallChange) {
								btn.setColor(UIColors.WON);
							}
							
							if (entry.getScore() > SingleplayerChallengesMode.maxMoneyScore && RSet.getInt(RSet.challengesUnlocked) < handleIndex) {
								RSet.set(RSet.challengesUnlocked, handleIndex+1);
							}
						}
					}
				}

				if (findHighestChallengeScore()) {
					userStats.downloadLeaderboardEntries(carHandles[type], LeaderboardDataRequest.Global, pageFrom, pageTo);

					if (shallChange && leaderboardMyScores[handleIndex] != null) {
						yourScore = "Your score is " + Texts.formatNumberSimple(leaderboardMyScores[handleIndex].score)
								+ " and you're nr. " + leaderboardMyScores[handleIndex].ranking;
					}
				}
			}
		} else {
			// car
			IUIObject[] list = leaderboardList.getListArr();
			for (int i = 0; i < numEntries; i++) {
				SteamLeaderboardEntry entry = new SteamLeaderboardEntry();
				if (userStats.getDownloadedLeaderboardEntry(entries, i, entry, details)) {

					for (IUIObject elem : list) {
						if (!(elem instanceof UILeaderboardPlayer))
							continue;
						var actualElem = (UILeaderboardPlayer) elem;
						if (actualElem.getSteamID().equals(entry.getSteamIDUser())) {
							actualElem.setCarID(entry.getScore());
							break;
						}
					}

				}
			}
		}
		getMulti();
	}

	private void resetButtons() {
		removePressables();
		add(goBackBtn);
		add(refreshBtn);
	}

	@Override
	public void onLeaderboardScoreUploaded(boolean success, SteamLeaderboardHandle leaderboard, int score,
			boolean scoreChanged, int globalRankNew, int globalRankPrevious) {
		if (scoreChanged) {
			if (!leaderboard.equals(carHandles[type])) {
				userStats.uploadLeaderboardScore(carHandles[type], LeaderboardUploadScoreMethod.ForceUpdate, carOnLeaderboardBuffer,
						new int[0]);
			} else {
				userStats.downloadLeaderboardEntriesForUsers(leaderboardHandles[type], new SteamID[] {Features.inst.getSteamHandler().getMySteamID()});
			}
		}
	}

	@Override
	public void onGlobalStatsReceived(long gameId, SteamResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy() {
		if (userStats != null)
			userStats.dispose();
	}

}
