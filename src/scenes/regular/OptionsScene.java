package scenes.regular;

import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_NO_SCROLLBAR;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_style_pop_vec2;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_vec2;

import engine.graphics.ui.*;
import engine.io.InputHandler;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import audio.AudioMaster;
import audio.AudioTypes;
import audio.SfxTypes;
import engine.graphics.interactions.RegularTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.Renderer;
import engine.io.Window;
import main.Features;
import main.Main;
import main.Texts;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Scene;
import scenes.game.racing_subscenes.RaceVisual;
import settings_and_logging.RSet;

public class OptionsScene extends Scene {

	private final String[] explaination = { "Tutorial:", "1. Ready and start race.",
			"2. Press throttle as green lights turn on!", "3. Use NOS as quickly as possible.",
			"4. Change gear by releasing throttle, ", "   and selecting next gear.", "5. Finish race and get money",
			"6. Buy upgrades and return to step 1!" };

	private final String[] controls = { "", "Gearbox layout:", "1 3 5      -", "|-|-|  or  |", "2 4 6      +",
			"Click and Drag      Just click +" };

	public static final int[] lockFpsSet = { 0, 30, 60, 120, 144, 165, 180, 200, 240, 300, 500 };

	private UIWindowInfo window;
	private UIButton<?> gobackBtn, windowedBtn, borderlessBtn, fullscreenBtn, vsyncBtn, nextSongBtn, showHintsInRaceBtn, lockFpsBtn, controlsBtn;
	private UISlider[] sliders = { new UISlider("Master-Volume"), new UISlider("SFX-Volume"),
			new UISlider("Music-Volume") };
	// private ControlsSettings controlsSettings;
	private AudioMaster audio;

	private int btnHeight;
	private int hPadding;
	private IAction countdownAction;

	public OptionsScene(RegularTopbar topbar, IAction countdownAction) {
		super(topbar, Scenes.OPTIONS);
		// TODO fullscreen checks?
		this.countdownAction = countdownAction;

		window = createWindow(0, topbar.getHeight(), Window.WIDTH, Window.HEIGHT - topbar.getHeight());

		gobackBtn = new UIButton<>(Texts.gobackText, UIColors.DARKGRAY);
		gobackBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.PREVIOUS, true);
		});

		windowedBtn = new UIButton<>("", UIColors.DARKGRAY);
		borderlessBtn = new UIButton<>("", UIColors.DARKGRAY);
		fullscreenBtn = new UIButton<>("", UIColors.DARKGRAY);
		lockFpsBtn = new UIButton<>("", UIColors.DARKGRAY);
		vsyncBtn = new UIButton<>("", UIColors.DARKGRAY);
		nextSongBtn = new UIButton<>(Texts.nextSong, UIColors.DARKGRAY);
		showHintsInRaceBtn = new UIButton<>("", UIColors.DARKGRAY);
		controlsBtn = new UIButton<>("Edit controls", UIColors.DARKGRAY);
		controlsBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.HOTKEY_OPTIONS, false);
		});

		/*
		 * Add to a specific window
		 */

		add(controlsBtn);
		add(gobackBtn);
		add(windowedBtn);
		add(borderlessBtn);
		add(fullscreenBtn);
		add(lockFpsBtn);
		add(vsyncBtn);
		add(nextSongBtn);
		add(showHintsInRaceBtn);

		
		gobackBtn.setNavigations(nextSongBtn, controlsBtn, null, lockFpsBtn);
		controlsBtn.setNavigations(gobackBtn, showHintsInRaceBtn, null, vsyncBtn);
		showHintsInRaceBtn.setNavigations(controlsBtn, nextSongBtn, null, windowedBtn);
		nextSongBtn.setNavigations(showHintsInRaceBtn, gobackBtn, null, borderlessBtn);
		borderlessBtn.setNavigations(windowedBtn, fullscreenBtn, nextSongBtn, null);
		fullscreenBtn.setNavigations(borderlessBtn, lockFpsBtn, nextSongBtn, null);
		
		lockFpsBtn.setNavigations(fullscreenBtn, vsyncBtn, gobackBtn, null);
		vsyncBtn.setNavigations(lockFpsBtn, windowedBtn, controlsBtn, null);
		windowedBtn.setNavigations(vsyncBtn, borderlessBtn, showHintsInRaceBtn, null);
//		refreshBtn.setNavigations(gobackBtn, null, createLanBtn, null);
	}

	private void updateFPS(int fps) {
		if (fps == 0) {
			lockFpsBtn.setTitle("Unlimited FPS");
			Main.TARGET_FPS = 0;
			Main.OPTIMAL_TIME = 0;
			return;
		}
		lockFpsBtn.setTitle("FPS-target: " + lockFpsSet[fps]);
		Main.TARGET_FPS = lockFpsSet[fps];
		Main.OPTIMAL_TIME = 100_0000_000 / Main.TARGET_FPS;
	}

	public void updateVsync(boolean vsync) {
		vsyncBtn.setTitle(vsync ? "Vsync is on." : "Vsync is off.");
		GLFW.glfwSwapInterval(vsync ? 1 : 0);
	}
	
	public void updateFullscreen(int fullscreen) {
		windowedBtn.setTitle(fullscreen == 0 ? "ON Windowed" : "Windowed");
		borderlessBtn.setTitle(fullscreen == 1 ? "ON Borderless fullscreen" : "Borderless fullscreen");
		fullscreenBtn.setTitle(fullscreen == 2 ? "ON True fullscreen" : "True fullscreen");
	}

	public void initOptions(AudioMaster audio, SceneHandler sceneHandler) {
		this.audio = audio;

		windowedBtn.setPressedAction(() -> {
			var full = 0;
			RSet.set(RSet.clientFullscreen, full);
			updateFullscreen(full);
			Features.inst.getWindow().setFullscreen(full);
			sceneHandler.updateResolution();
			press();
		});
		
		borderlessBtn.setPressedAction(() -> {
			var full = 1;
			RSet.set(RSet.clientFullscreen, full);
			updateFullscreen(full);
			Features.inst.getWindow().setFullscreen(full);
			sceneHandler.updateResolution();
			press();
		});
		
		fullscreenBtn.setPressedAction(() -> {
			var full = 2;
			RSet.set(RSet.clientFullscreen, full);
			updateFullscreen(full);
			Features.inst.getWindow().setFullscreen(full);
			sceneHandler.updateResolution();
			press();
		});

		vsyncBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			boolean vsync = !RSet.getBool(RSet.vsync); // Flip
			if (vsync) {
				RSet.set(RSet.fIndex, 0);
				updateFPS(0);
			}
			RSet.set(RSet.vsync, vsync);
			updateVsync(vsync);
			press();
		});
		updateVsync(RSet.getBool(RSet.vsync));

		lockFpsBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			var fps = (RSet.getInt(RSet.fIndex) + 1) % lockFpsSet.length;
			RSet.set(RSet.fIndex, fps);
			updateFPS(fps);
			press();
		});
		updateFPS(RSet.getInt(RSet.fIndex));

		nextSongBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			audio.nextSong();
		});

		showHintsInRaceBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			boolean showHints = !RSet.getBool(RSet.showHints); // Flip
			RSet.set(RSet.showHints, showHints);
			showHintsInRaceBtn.setTitle(showHints ? "Hide race-hints?" : "Show race-hints?");
			RaceVisual.ShowHints = showHints;
			press();
		});

		sliders[0].setValue(RSet.getDouble(RSet.masterVolume));
		sliders[1].setValue(RSet.getDouble(RSet.sfxVolume));
		sliders[2].setValue(RSet.getDouble(RSet.musicVolume));
		updateFullscreen(RSet.getInt(RSet.clientFullscreen));
		boolean showHints = RSet.getBool(RSet.showHints);
		showHintsInRaceBtn.setTitle(showHints ? "Hide race-hints?" : "Show race-hints?");
		RaceVisual.ShowHints = showHints;
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		GL11.glClearColor(0.1f, 0.1f, 0.1f, 1);

		((RegularTopbar) topbar).setTitle(Texts.optionsText);
		((RegularTopbar) topbar).showButtons(false);
		press();
	}

	@Override
	public void updateResolution() {
		btnHeight = Window.HEIGHT / 16;
		hPadding = Window.WIDTH / 8;
	}

	@Override
	public void tick(float delta) {
		countdownAction.run();
	}

	@Override
	public void keyInput(int keycode, int action) {

		if (action == 1) {
			// Downstroke for quicker input
			generalHoveredButtonNavigation(gobackBtn, keycode);

			if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				sceneChange.change(Scenes.PREVIOUS, false);
			}
		}
	}

	@Override
	public void controllerInput() {
		generalHoveredButtonNavigationJoy(gobackBtn);
		if (InputHandler.BTN_B)
			gobackBtn.runPressedAction();
		
		System.out.println(InputHandler.LEFT_STICK_X);
		if (InputHandler.LEFT_STICK_X < -.1f) {
			sliders[0].setValue(sliders[0].getValue() + .0001f*InputHandler.LEFT_STICK_X);
			updateVolumeSlider(0);
		} else if (InputHandler.LEFT_STICK_X > .1f) {
			sliders[0].setValue(sliders[0].getValue() + .05f*InputHandler.LEFT_STICK_X);
			updateVolumeSlider(0);
		}
		if (InputHandler.LEFT_STICK_Y < -.1f) {
			sliders[1].setValue(sliders[1].getValue() - .05f*InputHandler.LEFT_STICK_Y);
			updateVolumeSlider(1);
		} else if (InputHandler.LEFT_STICK_Y > .1f) {
			sliders[1].setValue(sliders[1].getValue() - .0001f*InputHandler.LEFT_STICK_Y);
			updateVolumeSlider(1);
		}

		if (InputHandler.RIGHT_STICK_X < -.1f) {
			sliders[2].setValue(sliders[2].getValue() + .0001f*InputHandler.RIGHT_STICK_X);
			updateVolumeSlider(2);
		} else if (InputHandler.RIGHT_STICK_X > .1f) {
			sliders[2].setValue(sliders[2].getValue() + .05f*InputHandler.RIGHT_STICK_X);
			updateVolumeSlider(2);
		}
	}

	@Override
	public void mouseScrollInput(float x, float y) {
	}
	
	public void updateVolumeSlider(int i) {
		var audioType = AudioTypes.values()[i];
		float sliderVal = (float) sliders[i].getValue();

		if (sliderVal != audio.getVolume(audioType)) {
			audio.setVolume(audioType, sliderVal);
			switch (audioType) {
			case MASTER -> {
				RSet.set(RSet.masterVolume, sliderVal);
				audio.updateVolumeSfx();
				audio.updateVolumeMusic();
			}
			case SFX -> {
				RSet.set(RSet.sfxVolume, sliderVal);
				audio.updateVolumeSfx();
			}
			case MUSIC -> {
				RSet.set(RSet.musicVolume, sliderVal);
				audio.updateVolumeMusic();
			}
			}
		}
	}

	@Override
	public void mousePositionInput(float x, float y) {
		if (InputHandler.MOUSEACTION != GLFW.GLFW_RELEASE) {
			for (int i = 0; i < 3; i++) {
				updateVolumeSlider(i);
			}
		}
	}

	/*
	 * ========= VISUALIZATION ==========
	 */

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {

		// Set the padding of the group

		NkVec2 spacing = NkVec2.malloc(stack);
		NkVec2 padding = NkVec2.malloc(stack);

		spacing.set(btnHeight / 8f, btnHeight / 2f);
		padding.set(hPadding, btnHeight);

		nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
		nk_style_push_vec2(ctx, ctx.style().window().group_padding(), padding);

		Features.inst.pushFontColor(ctx, UIColors.WHITE);
		/*
		 * MAIN SHIT
		 */
		if (window.begin(ctx)) {
			/*
			 * GROUP OF MAIN BUTTONS
			 */

			nk_layout_row_dynamic(ctx, (Window.HEIGHT - topbar.getHeight()) - Window.HEIGHT * 0.45f, 1);

			// Groups have the same options available as windows
			int options = NK_WINDOW_NO_SCROLLBAR;

			if (nk_group_begin(ctx, "My Group", options)) {

				//
				// The group contains rows and the rows contain widgets, put
				// those here.
				//
				nk_layout_row_dynamic(ctx, btnHeight / 8f, 2);
				Nuklear.nk_label(ctx, "", Nuklear.NK_TEXT_ALIGN_RIGHT);
				Nuklear.nk_label(ctx, "Playing: " + audio.getCurrentMusic(), Nuklear.NK_TEXT_ALIGN_RIGHT);

				nk_layout_row_dynamic(ctx, btnHeight, 4); // nested row
				gobackBtn.layout(ctx, stack);
				controlsBtn.layout(ctx, stack);
				showHintsInRaceBtn.layout(ctx, stack);
				nextSongBtn.layout(ctx, stack);
				nk_layout_row_dynamic(ctx, btnHeight, 5); // nested row
				lockFpsBtn.layout(ctx, stack);
				vsyncBtn.layout(ctx, stack);
				windowedBtn.layout(ctx, stack);
				borderlessBtn.layout(ctx, stack);
				fullscreenBtn.layout(ctx, stack);

				nk_style_pop_vec2(ctx);
				nk_style_pop_vec2(ctx);

				spacing.set(0, 0);
				padding.set(hPadding, 0);

				nk_style_push_vec2(ctx, ctx.style().window().group_padding(), padding);
				nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);

				for (UISlider slider : sliders) {

					nk_layout_row_dynamic(ctx, btnHeight / 2, 1);
					Nuklear.nk_label(ctx, slider.getName(),
							Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);
					Nuklear.nk_layout_row_begin(ctx, Nuklear.NK_DYNAMIC, btnHeight / 2, 2);
					Nuklear.nk_layout_row_push(ctx, 0.9f);
					slider.layout(ctx, stack);
					Nuklear.nk_layout_row_push(ctx, 0.1f);
					Nuklear.nk_label(ctx, String.valueOf(slider.getValueActual()),
							Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);

				}

				// Unlike the window, the _end() function must be inside the
				// if() block

				nk_group_end(ctx);
			}
			nk_style_pop_vec2(ctx);
			spacing.set(Window.WIDTH * 0.27f, 0);
			nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);

			int length = explaination.length > controls.length ? explaination.length : controls.length;

			for (int i = 0; i < length; i++) {
				nk_layout_row_dynamic(ctx, btnHeight / 2f, 2);
				String s = "";
				if (i < controls.length)
					s = controls[i];
				Nuklear.nk_label(ctx, s, Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);

				if (i < explaination.length)
					s = explaination[i];
				else
					s = "";
				Nuklear.nk_label(ctx, s, Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
			}

			nk_style_pop_vec2(ctx);
			nk_style_pop_vec2(ctx);
		}
		Nuklear.nk_end(ctx);

		Features.inst.popFontColor(ctx);
	}

	@Override
	public void destroy() {
		removeGameObjects();
	}

}
