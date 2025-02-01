package scenes.regular;

import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import audio.SfxTypes;
import elem.Animation;
import engine.graphics.interactions.RegularTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Sprite;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIWindowInfo;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import game_modes.SingleplayerChallenges;
import game_modes.SingleplayerChallengesMode;
import main.Features;
import main.ResourceHandler;
import main.Texts;
import scenes.Scenes;
import scenes.adt.Scene;
import settings_and_logging.RSet;

public class SingleplayerScene extends Scene {

	private UIButton<?> lastControllerBtn;
	private final UIButton<?> gobackBtn;
	private UILabel title;
	private UIButton<Integer>[] modesBtns, leaderboardBtns;
	private UIButton<Integer> sandboxBtn;
	private UIButton<Integer>[] dailyWeeklyMonthly;

	private UIWindowInfo window;
	private float btnHeight;
//	private Sprite backgroundImage;
	private Animation dudeDriving;

	public SingleplayerScene(Consumer<Integer> createNewSingleplayerGameAction, RegularTopbar topbar) {
		super(topbar, Scenes.SINGLEPLAYER);

//		ResourceHandler.LoadSprite("./images/back/singleplayerChallenges.png", "background", (sprite) -> {
//			backgroundImage = sprite;
//			float x = Window.WIDTH - sprite.getWidth();
//			sprite.setPositionX(x);
//		});
		
		window = createWindow(0, 0, 0, 0);
		dudeDriving = new Animation("back/main_menu", "main", 24, 0, Window.HEIGHT);


		title = new UILabel("Loading...#" + UIColors.WHITE);

		gobackBtn = new UIButton<>(Texts.gobackText);
		gobackBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.MAIN_MENU, true);
		});
		add(gobackBtn);
		gobackBtn.setNavigations(null, null, null, () -> modesBtns[0]);

		Consumer<Integer> leaderboardAction = (i) -> {
			sceneChange.change(Scenes.LEADERBOARD, true, LeaderboardScene.convertLeaderboardIndex(i), i);
			audio.play(SfxTypes.REGULAR_PRESS);
		};
		int len = Texts.singleplayerModes.length;
		modesBtns = new UIButton[len];
		leaderboardBtns = new UIButton[len];
		for (int i = 0; i < len; i++) {
			modesBtns[i] = new UIButton<>(Texts.singleplayerModes[i]);
			modesBtns[i].setPressedAction(createNewSingleplayerGameAction);
			modesBtns[i].setConsumerValue(i);
			add(modesBtns[i]);

			leaderboardBtns[i] = new UIButton<>("Leaderboard");
			leaderboardBtns[i].setPressedAction(leaderboardAction);
			leaderboardBtns[i].setConsumerValue(i);
			add(leaderboardBtns[i]);
		}
		
		dailyWeeklyMonthly = new UIButton[9];
		for (int i = 0; i < dailyWeeklyMonthly.length; i++) {
			dailyWeeklyMonthly[i] = new UIButton<>(Texts.dailyModes[i]);
			dailyWeeklyMonthly[i].setPressedAction(createNewSingleplayerGameAction);
			dailyWeeklyMonthly[i].setConsumerValue(len + i);
			add(dailyWeeklyMonthly[i]);
		}
		
		sandboxBtn = new UIButton<>("Sandbox");
		sandboxBtn.setPressedAction(createNewSingleplayerGameAction);
		sandboxBtn.setConsumerValue(len + 9);
		sandboxBtn.setColorUI(UIColors.BLUSH);
		add(sandboxBtn);

		for (int i = 0; i < len; i++) {
			modesBtns[i].setNavigations(
					null, 
					leaderboardBtns[i], 
					i != 0 ? modesBtns[i - 1] : gobackBtn, 
					(i + 1) != len ? modesBtns[i + 1] : dailyWeeklyMonthly[0]
			);
			leaderboardBtns[i].setNavigations(
					modesBtns[i], 
					null, 
					i != 0 ? leaderboardBtns[i - 1] : gobackBtn, 
							(i + 1) != len ? leaderboardBtns[i + 1] : dailyWeeklyMonthly[0]
					);
		}
		for (int i = 0; i < dailyWeeklyMonthly.length; i++) {
			dailyWeeklyMonthly[i].setNavigations(
					i >= 3 ? dailyWeeklyMonthly[i - 3] : null, 
					i <= 5 ? dailyWeeklyMonthly[i + 3] : null, 
					i % 3 == 0 ? modesBtns[len - 1] : dailyWeeklyMonthly[i - 1], 
					(i + 1) < dailyWeeklyMonthly.length ? dailyWeeklyMonthly[i + 1] : null
					);
		}
		
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		((RegularTopbar) topbar).setTitle(Texts.singleplayerText);

		var challengeUnlocked = RSet.getInt(RSet.challengesUnlocked);
		if (challengeUnlocked < 0)
			challengeUnlocked = 0;
		for (int i = 0; i < modesBtns.length; i++) {
			modesBtns[i].setEnabled(challengeUnlocked >= i);
			modesBtns[i].setColor(
					challengeUnlocked > i ? UIColors.COLORS[UIColors.WON.ordinal()]
							: challengeUnlocked == i
									? UIColors.COLORS[UIColors.DNF.ordinal()]
									: null);
		}
		for (int i = 0; i < dailyWeeklyMonthly.length; i++) {
			if (challengeUnlocked <= 1)
				dailyWeeklyMonthly[i].setEnabled(false);
			
			var settingVal = RSet.values()[RSet.challengeDayFun.ordinal() + i];
			var completed = RSet.get(settingVal);
			if (completed != null 
					&& completed.equals(SingleplayerChallengesMode.createSeed(settingVal))) {
				dailyWeeklyMonthly[i].setColor(UIColors.COLORS[UIColors.WON.ordinal()]);
			} else {
				dailyWeeklyMonthly[i].setColor(UIColors.COLORS[UIColors.DNF.ordinal()]);
			}
		}

		UISceneInfo.clearHoveredButton(sceneIndex);
		sandboxBtn.setEnabled(challengeUnlocked > 0);
//		window.focus = false;
	}

	public void doneLoading() {
		title.setText(Texts.difficultyChoose);
		title.setColor(UIColors.WHITE);

		updateGenerally(null);
	}

	@Override
	public void updateResolution() {
		btnHeight = Window.HEIGHT / 29f;
		
		float w = Window.WIDTH - dudeDriving.getFrame().getWidth();
		for (var frame : dudeDriving.getFrames()) {
			frame.setPositionX(w);
		}
		if (Window.HEIGHT / w > 2.5) {
			w = Window.WIDTH * .5f;
		}
		window.setPositionSize(0, 0, w, Window.HEIGHT);
	}

	@Override
	public void tick(float delta) {
	}

	@Override
	public void keyInput(int keycode, int action) {
		if (action != GLFW.GLFW_RELEASE) {
			// Downstroke for quicker input
			generalHoveredButtonNavigation(gobackBtn, keycode);

			if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				sceneChange.change(Scenes.MAIN_MENU, false);
			}
		}
	}

	@Override
	public void controllerInput() {
		generalHoveredButtonNavigationJoy(lastControllerBtn != null ? lastControllerBtn : gobackBtn);
		lastControllerBtn = UISceneInfo.getHoveredButton(sceneIndex);
		if (InputHandler.BTN_B)
			gobackBtn.runPressedAction();
	}


	@Override
	public void mouseScrollInput(float x, float y) {
	}

	@Override
	public void mousePositionInput(float x, float y) {
	}

	/*
	 * ========= VISUALIZATION ==========
	 */
	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		dudeDriving.incrementCurrentFrame(delta / 0.9f);
		renderer.renderOrthoMesh(dudeDriving.getFrame());
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {

		// Set the padding of the group
		NkVec2 padding = NkVec2.malloc(stack);
		NkVec2 spacing = NkVec2.malloc(stack);

		padding.set(btnHeight / 8f, btnHeight);
		spacing.set(btnHeight / 12f, btnHeight / 4f);

		Nuklear.nk_style_push_vec2(ctx, ctx.style().window().padding(), padding);
		Nuklear.nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);

		Features.inst.pushBackgroundColor(ctx,
//				window.focus
//				?
//				UIColors.DARKGRAY
				UIColors.DARK_RAISIN_BLACK
		);
		/*
		 * MAIN SHIT
		 */
		if (window.begin(ctx)) {

			nk_layout_row_dynamic(ctx, btnHeight, 2);
			gobackBtn.layout(ctx, stack);
			sandboxBtn.layout(ctx, stack); 

			nk_layout_row_dynamic(ctx, btnHeight, 1);
			title.layout(ctx, stack);

			for (int i = 0; i < modesBtns.length; i++) {
				if (i < modesBtns.length) {
					Nuklear.nk_layout_row_begin(ctx, Nuklear.NK_DYNAMIC, btnHeight, 4);
					Nuklear.nk_layout_row_push(ctx, 0.066f);
					Nuklear.nk_label(ctx, "", 0);
					Nuklear.nk_layout_row_push(ctx, 0.6f);
					modesBtns[i].layout(ctx, stack);
					Nuklear.nk_layout_row_push(ctx, 0.066f);
					Nuklear.nk_label(ctx, "", 0);
					Nuklear.nk_layout_row_push(ctx, 0.2f);
					leaderboardBtns[i].layout(ctx, stack);
					Nuklear.nk_layout_row_push(ctx, 0.066f);
					Nuklear.nk_label(ctx, "", 0);
					continue;
				}
				nk_layout_row_dynamic(ctx, btnHeight, 1);
				modesBtns[i].layout(ctx, stack);
			}

			Features.inst.pushFontColor(ctx, UIColors.WHITE);
			nk_layout_row_dynamic(ctx, .2f*btnHeight, 1);
			nk_layout_row_dynamic(ctx, .8f*btnHeight, 1);
			Nuklear.nk_label(ctx, "Here are some fun, completely random challenges:", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_BOTTOM);
			Features.inst.popFontColor(ctx);
			for (int i = 0; i < 3; i++) {
				Nuklear.nk_layout_row_begin(ctx, Nuklear.NK_DYNAMIC, btnHeight, 4);
				Nuklear.nk_layout_row_push(ctx, 0.066f);
				Nuklear.nk_label(ctx, "", 0);
				Nuklear.nk_layout_row_push(ctx, 0.26667f);
				dailyWeeklyMonthly[i].layout(ctx, stack);
				Nuklear.nk_layout_row_push(ctx, 0.033f);
				Nuklear.nk_label(ctx, "", 0);
				Nuklear.nk_layout_row_push(ctx, 0.26667f);
				dailyWeeklyMonthly[i + 3].layout(ctx, stack);
				Nuklear.nk_layout_row_push(ctx, 0.033f);
				Nuklear.nk_label(ctx, "", 0);
				Nuklear.nk_layout_row_push(ctx, 0.26667f);
				dailyWeeklyMonthly[i + 6].layout(ctx, stack);
				Nuklear.nk_layout_row_push(ctx, 0.066f);
				Nuklear.nk_label(ctx, "", 0);
			}

		}
		Nuklear.nk_end(ctx);
		Features.inst.popBackgroundColor(ctx);

		Nuklear.nk_style_pop_vec2(ctx);
		Nuklear.nk_style_pop_vec2(ctx);
	}

	@Override
	public void destroy() {
		removeGameObjects();
	}

}
