package scenes.game.racing_subscenes;

import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_vec2;

import java.util.ArrayList;
import java.util.Arrays;

import audio.SfxTypes;
import engine.graphics.interactions.LobbyTopbar;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import communication.GameInfo;
import engine.graphics.objects.Camera;
import engine.graphics.ui.Font;
import engine.graphics.ui.IUIHasWindow;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UIFont;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIScrollable;
import engine.graphics.ui.UIWindowInfo;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Main;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import player_local.car.Rep;
import scenes.Scenes;
import scenes.adt.Visual;
import scenes.game.Lobby;
import scenes.game.Race;
import scenes.game.lobby_subscenes.UpgradesSubscene;

public class WinVisual extends Visual {

	private UIFont statsFont;
	private IUIHasWindow raceLobbyLabel;
	private UIScrollable winStats;

	private boolean singleplayer;
	private UIWindowInfo leaderboardWindow;
	private UIButton<?> leaderboardBtn;
	private UILabel leaderboardScoreLabel1, leaderboardScoreLabel2;

	private UIButton<?> tryAgainBtn;

//	private Store store;
	private int currentPlayerIndex;
	private Player[] players;
	private UpgradesSubscene upgradesVisualization;
	private UIWindowInfo upgradesControlPanel;
	private UIButton<?> flipToUpgradesBtn, nextPlayerBtn;
	private IUIObject[] playerInfos;
	private LobbyTopbar winTopbar;
	private boolean leaderboardShow;
	public Race race;

	public WinVisual(IUIHasWindow raceLobbyLabel, LobbyTopbar winTopbar) {
		this.winTopbar = winTopbar;
		this.raceLobbyLabel = raceLobbyLabel;
		statsFont = new UIFont(Font.BOLD_REGULAR, Window.WIDTH / 45);
		leaderboardScoreLabel1 = new UILabel();
		leaderboardScoreLabel2 = new UILabel();

		upgradesControlPanel = UISceneInfo.createWindowInfo(Scenes.RACE, Window.WIDTH * 0.7, 0, Window.WIDTH * 0.3,
				Window.HEIGHT);
		flipToUpgradesBtn = new UIButton<>("See upgrade history");
		nextPlayerBtn = new UIButton<>("Next player");

		flipToUpgradesBtn.setPressedAction(() -> {
			Features.inst.getAudio().play(SfxTypes.REGULAR_PRESS);
			if (player == null) {
				flipToUpgradesBtn.setTitle("Go back to the overview");
				if (players == null || players.length == 0)
					return;
				if (currentPlayerIndex >= players.length)
					currentPlayerIndex = players.length - 1;
				else if (currentPlayerIndex < 0)
					currentPlayerIndex = 0;
				playerInfos = players[currentPlayerIndex].getPlayerWinHistoryInfo();
				player = players[currentPlayerIndex];
				upgradesVisualization.viewPlayer(player, true, false);
				GL11.glClearColor(0.5f, 0.5f, 0.5f, 1);
			} else {
				flipToUpgradesBtn.setTitle("See upgrade history");
				player = null;
				GL11.glClearColor(0, 0, 0, 1);
			}
			updateGenerally(null);
		});
		flipToUpgradesBtn.setNavigations(() -> leaderboardBtn, null, () -> goBackBtn, () -> tryAgainBtn);

		nextPlayerBtn.setPressedAction(() -> {
			Features.inst.getAudio().play(SfxTypes.REGULAR_PRESS);
			currentPlayerIndex = (currentPlayerIndex + 1) % players.length;
			player = players[currentPlayerIndex];
			playerInfos = player.getPlayerWinHistoryInfo();
			upgradesVisualization.viewPlayer(player, true, false);
		});

		UISceneInfo.addPressableToScene(Scenes.RACE, flipToUpgradesBtn);
		UISceneInfo.addPressableToScene(Scenes.RACE, nextPlayerBtn);

		winStats = new UIScrollable(statsFont, Scenes.RACE, 0, 0, Window.WIDTH / 2f, 0);
		leaderboardWindow = UISceneInfo.createWindowInfo(Scenes.RACE, 0, 0, 0, 0);

	}

	public void setLeaderboardBtn(UIButton<?> leaderboardBtn) {
		this.leaderboardBtn = leaderboardBtn;
		leaderboardBtn.setNavigations(null, () -> goBackBtn, () -> goBackBtn, () -> tryAgainBtn);
	}

	public void setTryAgainBtn(UIButton<?> tryAgainBtn) {
		this.tryAgainBtn = tryAgainBtn;
		tryAgainBtn.setNavigations(() -> leaderboardBtn, null, () -> flipToUpgradesBtn, null);
	}

	public void initRest(boolean singleplayer, boolean leaderboard) {
		this.singleplayer = singleplayer;
		leaderboardBtn.setEnabled(singleplayer);
		tryAgainBtn.setEnabled(true);
		tryAgainBtn.setTitle(singleplayer ? Texts.tryAgain : "Rematch?");

		this.leaderboardShow = leaderboard;
		winStats.getWindow().setHeight(singleplayer ? Window.HEIGHT * 0.8f : Window.HEIGHT * 0.99f);

		float paddingAmount = Window.HEIGHT / 60f;
		winStats.setPadding(paddingAmount / 2f, paddingAmount);
		leaderboardWindow.setPositionSize(0, winStats.getWindow().getYHeight(), winStats.getWindow().width,
				Window.HEIGHT - winStats.getWindow().getYHeight());
		goBackBtn.setNavigations(() -> leaderboardBtn, null, null, () -> flipToUpgradesBtn);
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		var color = player == null ? UIColors.valByInt(0, UIColors.RAISIN_BLACK) : null;
		goBackBtn.setColor(color);
		leaderboardBtn.setColor(color);
		flipToUpgradesBtn.setColor(color);
		tryAgainBtn.setColor(color);
		winTopbar.menuButton.setColor(color);
		Lobby.setChatPosition(raceLobbyLabel.getWindow().x, -1);
	}

	@Override
	public void updateResolution() {
		upgradesVisualization.updateResolution();
	}

	@Override
	public void mouseScrollInput(float x, float y) {
		if (player != null)
			upgradesVisualization.mouseScrollInput(x, y);
		else if (InputHandler.CONTROLLER_EFFECTIVELY && !winStats.getWindow().focus) {
			winStats.getWindow().focus = true;
			winStats.scroll(y);
			winStats.getWindow().focus = false;
		}
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		if (player != null) {
			upgradesVisualization.mouseButtonInput(button, action, x, y);
			return false;
		}

		if (!singleplayer && Lobby.chatInput.tryFocus(x, y, true))
			return false;
		return false;
	}

	@Override
	public void mousePosInput(float x, float y) {
		if (player != null)
			upgradesVisualization.mousePosInput(x, y);
	}

	@Override
	public void tick(float delta) {
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		if (player != null) {
			upgradesVisualization.renderGame(renderer, cam, window, delta);
			return;
		}
		GL11.glClearColor(0, 0, 0, 1);
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		if (player != null) {
			Features.inst.pushBackgroundColor(ctx, UIColors.WHITE, 0.25f);
			if (upgradesControlPanel.begin(ctx)) {
				nk_layout_row_dynamic(ctx, 100, 1);
				flipToUpgradesBtn.layout(ctx, stack);
				if (!singleplayer) {
					nk_layout_row_dynamic(ctx, 100, 1);
					nextPlayerBtn.layout(ctx, stack);
				}
				nk_layout_row_dynamic(ctx, 30, 1);

				if (playerInfos != null) {
					for (var playerLabel : playerInfos) {
						nk_layout_row_dynamic(ctx, 30, 1);
						playerLabel.layout(ctx, stack);
					}
				}
			}
			Nuklear.nk_end(ctx);
			Features.inst.popBackgroundColor(ctx);

			upgradesVisualization.renderUILayout(ctx, stack);
			return;
		}
		Features.inst.pushFontColor(ctx, UIColors.LBEIGE);
		winTopbar.layout(ctx, stack);
		Features.inst.pushBackgroundColor(ctx, UIColors.CHARCOAL, 0.98f);

//		Features.inst.pushBackgroundColor(ctx, UIColors.WHITE, 0.8f);
		raceLobbyLabel.layout(ctx, stack);
//		Features.inst.popBackgroundColor(ctx);

		float marginY = raceLobbyLabel.getWindow().height * .025f;
		float x = raceLobbyLabel.getWindow().x;
		float y = raceLobbyLabel.getWindow().y + raceLobbyLabel.getWindow().height + marginY;
		float w = raceLobbyLabel.getWindow().width;
		marginY /= 2f;
//		y -= marginY;
		float h = Window.HEIGHT - y; // 2 per knapp som skal v�re under
		h /= 3f;
		h -= 1.33f * marginY;

		goBackLayout(ctx, stack, x, y, w, h);
		/*
		 * try again
		 */
		NkVec2 spacing = NkVec2.malloc(stack);
		NkVec2 padding = NkVec2.malloc(stack);

		spacing.set(0, 0);
		padding.set(0, 0);

		nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
		nk_style_push_vec2(ctx, ctx.style().window().padding(), padding);

		NkRect rect = NkRect.malloc(stack);
		rect.x(x).y(y + h + marginY).w(w).h(h);

		Nuklear.nk_window_set_focus(ctx, "switchToUpgrades");
		if (nk_begin(ctx, "switchToUpgrades", rect, Nuklear.NK_WINDOW_NO_SCROLLBAR | Nuklear.NK_WINDOW_NO_INPUT)) {

			nk_layout_row_dynamic(ctx, h, 1);
			flipToUpgradesBtn.layout(ctx, stack);

		}
		nk_end(ctx);

		Nuklear.nk_style_pop_vec2(ctx);
		Nuklear.nk_style_pop_vec2(ctx);

		Features.inst.popBackgroundColor(ctx);

		/*
		 * singleplayer
		 */
		if (singleplayer) {
			/*
			 * try again
			 */
			spacing.set(0, 0);
			padding.set(0, 0);

			nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
			nk_style_push_vec2(ctx, ctx.style().window().padding(), padding);

			NkRect rect2 = NkRect.malloc(stack);
			rect2.x(x).y(y + 2f * h + 2f * marginY).w(w).h(h);

			Nuklear.nk_window_set_focus(ctx, "tryagain");
			if (nk_begin(ctx, "tryagain", rect2, Nuklear.NK_WINDOW_NO_SCROLLBAR | Nuklear.NK_WINDOW_NO_INPUT)) {

				nk_layout_row_dynamic(ctx, h, 1);
				tryAgainBtn.layout(ctx, stack);

			}
			nk_end(ctx);

			Nuklear.nk_style_pop_vec2(ctx);
			Nuklear.nk_style_pop_vec2(ctx);

			/*
			 * leaderboard
			 */
			if (leaderboardWindow.begin(ctx)) {
				float leaderboardScoreHeight = leaderboardWindow.height / 10f;
				Features.inst.pushFontColor(ctx, UIColors.WHITE);
				Nuklear.nk_layout_row_dynamic(ctx, leaderboardScoreHeight, 1);
				leaderboardScoreLabel1.layout(ctx, stack);
				Nuklear.nk_layout_row_dynamic(ctx, leaderboardScoreHeight, 1);
				leaderboardScoreLabel2.layout(ctx, stack);
				Features.inst.popFontColor(ctx);
				if (leaderboardBtn != null && leaderboardShow) {
					Nuklear.nk_layout_row_dynamic(ctx, leaderboardWindow.height / 3f, 1);
					leaderboardBtn.layout(ctx, stack);
				}
			}
			Nuklear.nk_end(ctx);
		} else {
			Lobby.renderChat(ctx, stack);
		}

		Features.inst.popFontColor(ctx);
		/*
		 * Stats
		 */
		winStats.layout(ctx, stack);
	}

	@Override
	public boolean hasAnimationsRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	public void setEveryoneDone(boolean everyoneDone) {
		// TODO Auto-generated method stub

	}

	public void claimWinner(GameInfo com, String leaderboardScoreText, String leaderboardScoreExplaination) {
		leaderboardScoreLabel1.setText(leaderboardScoreText);
		leaderboardScoreLabel2.setText(leaderboardScoreExplaination);

		winStats.setText(UILabel.split(com.getWinner(com.player), ";"));
		
		players = com.getAllPlayersExceptCommentator();

		Arrays.sort(players, (o1, o2) -> {
			if (o1.resigned) return 1;
			else if (o2.resigned) return -1;
			if (o1.bank.get(Bank.POINT) > o2.bank.get(Bank.POINT)) {
				return -1;
			} else if (o1.bank.get(Bank.POINT) < o2.bank.get(Bank.POINT)) {
				return 1;
			}
			return 0;
		});
		Arrays.sort(players, (o1, o2) -> {
			if (o1.bank.get(Bank.POINT) == o2.bank.get(Bank.POINT)) {
				if (o1.fastestTimeLapsedInRace > o2.fastestTimeLapsedInRace)
					return -1;
				else if (o1.fastestTimeLapsedInRace < o2.fastestTimeLapsedInRace)
					return 1;
			}
			return 0;
		});

		ArrayList<UILabel> myStats = new ArrayList<>();
        for (int j = 0, playersLength = players.length; j < playersLength; j++) {
            Player p = players[j];
            myStats.add(new UILabel(""));
			if (p.resigned) {
				myStats.add(new UILabel("RESIGNED!", UIColors.DNF));
			} else {
				myStats.add(new UILabel(Texts.podiumConversion(j) + " PLACE!", j == 0 ? UIColors.MEDIUM_SPRING_GREEN : UIColors.AERO_BLUE));
			}
            myStats.add(new UILabel((com.player.id == p.id ? " > " : "") + p.name + ":#" + UIColors.WHITE));

            var otherCars = new Rep[players.length - 1];
            int n = 0;
            for (int i = 0; i < players.length; i++) {
                if (players[i].getCarRep().equals(p.getCarRep()))
                    continue;
                otherCars[n] = players[i].getCarRep();
                n++;
            }
            myStats.addAll(p.getInfoWin(otherCars));
            p.wasHost = p.isHost();
			if (!com.resigned)
            	p.role = Player.COMMENTATOR;
        }
		winStats.addText(myStats);
		if (Main.DEBUG) {
			for (int i = 0; i < 25; i++) {
				winStats.addText(new UILabel("test" + i + "#" + UIColors.WHITE));
			}
		}

		currentPlayerIndex = com.player.id;
		player = null;
	}

	@Override
	public void keyInput(int keycode, int action) {
		if (player != null) {
			upgradesVisualization.keyInput(keycode, action);
			if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				flipToUpgradesBtn.runPressedAction();
			}
			return;
		}

		if (Lobby.chatKeyInput(keycode, action)) {
			return;
		}

		if (action != GLFW.GLFW_RELEASE) {

			if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				winTopbar.menuButton.runPressedAction();
				return;
			}

			if (winTopbar.menuWindow.visible) {
				winTopbar.input(keycode);
				return;
			}

			race.generalHoveredButtonNavigation(flipToUpgradesBtn, keycode);
		}
	}

	@Override
	public void controllerInput() {
		if (player != null) {
			upgradesVisualization.controllerInput();
			if (!InputHandler.HOLDING) {
				if (InputHandler.BTN_B)
					flipToUpgradesBtn.runPressedAction();
				else if (InputHandler.BTN_A || InputHandler.BTN_X || InputHandler.BTN_BACK_TOP_RIGHT
						|| InputHandler.BTN_BACK_TOP_LEFT)
					nextPlayerBtn.runPressedAction();
			}
			return;
		}

		if (InputHandler.HOLDING)
			return;

		if (InputHandler.BTN_Y) {
			winTopbar.menuButton.runPressedAction();
			return;
		}

		if (winTopbar.menuWindow.visible) {
			winTopbar.controllerInput();
			return;
		}

		race.generalHoveredButtonNavigationJoy(flipToUpgradesBtn);

	}

	public void setUpgrades(UpgradesSubscene upgradesSubscene) {
		this.upgradesVisualization = upgradesSubscene;
		upgradesSubscene.add(Scenes.RACE, () -> playerInfos = players[currentPlayerIndex].getPlayerWinHistoryInfo());
	}

	public boolean hasPlayer() {
		return player != null;
	}

}
