package scenes;

import java.util.ArrayList;
import java.util.function.Consumer;

import engine.graphics.ui.*;
import engine.utils.Timer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import audio.AudioRemote;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Sprite;
import engine.graphics.ui.modal.UIConfirmModal;
import engine.graphics.ui.modal.UIMessageModal;
import engine.graphics.ui.modal.UIUsernameModal;
import engine.graphics.Renderer;
import engine.graphics.interactions.RegularTopbar;
import engine.io.InputHandler;
import engine.io.Window;
import engine.math.Vec2;
import main.Main;
import main.ResourceHandler;
import scenes.adt.ISceneManipulation;
import scenes.adt.Scene;
import scenes.adt.SceneChangeAction;
import scenes.game.Lobby;

public class SceneHandler implements ISceneManipulation {

	private static final ArrayList<Scene> scenes = new ArrayList<Scene>();
	private final UIUsernameModal usernameModal;
	private final UIConfirmModal confirmModal;
	private static final UIMessageModal messageModal = new UIMessageModal();
	private final Console console;
	private final NkColor fontColorStd;
	private static Window window;
	private Sprite origo;
	public static final Camera cam = new Camera(true);

	private float slowmotion = 1;
	private InputHandler inputHandler;
	public static boolean freeCam = false;
	public static boolean modalVisible;

	public SceneHandler(UIConfirmModal confirmModal, UIUsernameModal usernameModal, Console console, Window window) {
		this.usernameModal = usernameModal;
		this.confirmModal = confirmModal;
		this.console = console;
		SceneHandler.window = window;

		fontColorStd = NkColor.malloc().set((byte) 0, (byte) 0, (byte) 0, (byte) 255);
		ResourceHandler.LoadSprite(new Vec2(0), 15, "./images/racelight.png", "main", (sprite) -> {
			origo = sprite;
		});
	}

	public void init(Scene[] scenes, AudioRemote audio, Consumer<Integer> createNewSingleplayerGameAction, InputHandler inputHandler) {
		this.inputHandler = inputHandler;

		SceneChangeAction sceneChange = (scenenr, logCurrent, args) -> {
			changeScene(scenenr, logCurrent, args);
			return getCurrentScene();
		};

		for (var scene : scenes) {
			this.scenes.add(scene);
			if (scene == null)
				continue;
			scene.finalizeInit(audio, sceneChange);
		}

		usernameModal.hide();

		if (console != null)
			console.init(sceneChange);

		sceneChange.change(Main.TEST_GRAPHICS ? Scenes.TESTING : Scenes.MAIN_MENU, true);
	}

	@Override
	public void updateResolution() {

		UISceneInfo.updateResolution();

//		int n = 0;
		for (var scene : scenes) {
			if (scene == null)
				continue;
//			if (n == 8)
//				break;
//			n++;
			scene.updateResolution();
		}

		usernameModal.updateResolution();
	}

	/**
	 * Destroy all the meshes and shaders etc
	 */
	public void destroy() {
		for (Scene scene : scenes) {
			if (scene == null)
				continue;
			scene.destroy();
		}
		fontColorStd.free();
	}

	public static void changeScene(int scenenr, boolean logCurrent, int... args) {
		if (scenenr == Scenes.PREVIOUS) {
			do {
				scenenr = Scenes.HISTORY.pop();
			} while (!Scenes.HISTORY.isEmpty() && (Scenes.HISTORY.peek() == scenenr || scenenr == Scenes.CURRENT));
		}
		if (logCurrent)
			Scenes.HISTORY.push(Scenes.CURRENT);
		Scenes.CURRENT = scenenr;

		// Weird previous ik.
		if (Scenes.CURRENT < Scenes.OPTIONS)
			Scenes.PREVIOUS_REGULAR = Scenes.CURRENT;


		var topbar = getCurrentScene().getTopbarInteraction();
		if (topbar != null) {
			topbar.select();
			if (topbar instanceof RegularTopbar)
				((RegularTopbar) topbar).showButtons(true);
		}
		for (IUIPressable pressable : UISceneInfo.getScenePressables(Scenes.GENERAL_NONSCENE)) {
			pressable.press();
		}

		getCurrentScene().updateGenerally(cam, args); // update points
		getCurrentScene().press();

	}

	public static Scene getCurrentScene() {
		return scenes.get(Scenes.CURRENT);
	}

	public Scene getLastScene() {
		return scenes.get(Scenes.PREVIOUS);
	}

	/*
	 * =========== SCENE MANIPULATION ===========
	 */

	@Override
	public void tick(float delta) {
		getCurrentScene().tick(delta * (Main.DEBUG ? slowmotion : 1));
		cam.update();
		inputHandler.checkController();
	}

	@SuppressWarnings("static-access")
	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		getCurrentScene().renderGame(renderer, this.cam, window, delta * (Main.DEBUG ? slowmotion : 1));
		if (Main.DEBUG && freeCam) {
			renderer.renderMesh(origo, this.cam);
		}
	}

	/**
	 * TODO set global theme here... If it has to be done each cycle
	 */
	@Override
	public void renderUILayout(NkContext ctx, MemoryStack s) {
		Nuklear.nk_style_push_color(ctx, ctx.style().text().color(), fontColorStd);
		Nuklear.nk_style_push_color(ctx, ctx.style().button().text_normal(), fontColorStd);
		Nuklear.nk_style_push_color(ctx, ctx.style().button().text_active(), fontColorStd);
		Nuklear.nk_style_push_color(ctx, ctx.style().button().text_hover(), fontColorStd);

		try (var stack = MemoryStack.stackPush()) {

			/*
			 * MODALS
			 */
			if (console != null)
				console.layout(ctx, stack);
			usernameModal.layout(ctx, stack);
			messageModal.layout(ctx, stack);
			confirmModal.layout(ctx, stack);

			/*
			 * SCENE
			 */
			modalVisible = confirmModal.isVisible() || usernameModal.isVisible() || messageModal.isVisible();

			getCurrentScene().renderUILayout(ctx, stack);
			UIRisingTexts.layout(ctx, stack, Timer.lastDelta);

			/*
			 * TOPBAR
			 */
			IUIObject topbar = getCurrentScene().getTopbarRenderable();
			if (topbar != null) {
				topbar.layout(ctx, stack);
			}

		}

		Nuklear.nk_style_pop_color(ctx);
		Nuklear.nk_style_pop_color(ctx);
		Nuklear.nk_style_pop_color(ctx);
		Nuklear.nk_style_pop_color(ctx);
	}

	@Override
	public void keyInput(int keycode, int action) {
//		if (keycode == GLFW.glfwkey)
		if (Main.DEBUG) {

			if (action != GLFW.GLFW_RELEASE) {
				switch (keycode) {
					case GLFW.GLFW_KEY_PERIOD -> {
						slowmotion *= 2;
						System.out.println("Slowmotion: " + slowmotion);
					}
					case GLFW.GLFW_KEY_RIGHT_SHIFT -> {
						slowmotion = 1;
						System.out.println("Slowmotion: " + slowmotion);
					}
					case GLFW.GLFW_KEY_COMMA -> {
						slowmotion /= 2;
						System.out.println("Slowmotion: " + slowmotion);
					}
					case GLFW.GLFW_KEY_F -> {
						switchFreeCam();
					}
				}
			}
		}

		if (!Main.RELEASE && action != GLFW.GLFW_RELEASE && keycode == GLFW.GLFW_KEY_F2) {
			Main.DEBUG = !Main.DEBUG;
			((Lobby) scenes.get(Scenes.LOBBY)).updateDebug();
		}

		if (freeCam) {
			cam.move(keycode, action);
			System.out.println("pos: " + cam.getPosition().toString());
			if (Main.DEBUG)
				return;
		}

		if (console != null) {
			boolean wasBlocking = console.isBlocking();
			console.keyInput(keycode, action);
			if (wasBlocking || console.isBlocking())
				return;
		}

		if (confirmModal.isVisible()) {
			confirmModal.input(keycode);
		} else {
			if (usernameModal.isVisible())
				usernameModal.input(keycode, action);
			else if (messageModal.isVisible()) {
				messageModal.input(keycode, action);
			} else {
				getCurrentScene().keyInput(keycode, action);
			}
		}

	}

	@Override
	public void controllerInput() {
		if (confirmModal.isVisible()) {
			confirmModal.controllerInput();
		} else {
			if (usernameModal.isVisible())
				usernameModal.controllerInput();
			else if (messageModal.isVisible()) {
				messageModal.controllerInput();
			} else {
				getCurrentScene().controllerInput();
			}
		}
	}

	public static void switchFreeCam(boolean b) {
		freeCam = b;
		window.mouseStateHide(freeCam);
		cam.reset();
		System.out.println("freeCam: " + freeCam);
	}

	public static void switchFreeCam() {
		switchFreeCam(!freeCam);
	}

	@Override
	public void mouseScrollInput(float x, float y) {
		getCurrentScene().mouseScrollInput(x, y);
		for (var scrollable : UISceneInfo.getScrollables(Scenes.CURRENT)) {
			scrollable.scroll(y);
		}
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		getCurrentScene().mouseButtonInput(button, action, x, y);
		for (IUIPressable pressable : UISceneInfo.getScenePressables(Scenes.GENERAL_NONSCENE)) {
			pressable.release();
		}
		getCurrentScene().release();
		confirmModal.release();
		usernameModal.release();
		messageModal.release();

		if (console != null)
			console.mouseButtonInput(button, action, x, y);
		if (usernameModal.isVisible())
			usernameModal.mouseButtonInput(button, action, x, y);
		return false;
	}

	@Override
	public void mousePosInput(float x, float y) {
		UISceneInfo.decideFocusedWindow(x, y);
		if (confirmModal.isVisible() || messageModal.isVisible())
			return;
		getCurrentScene().mousePosInput(x, y);

		if (freeCam) {
			cam.rotateCameraMouseBased(x - Window.WIDTH / 2, y - Window.HEIGHT / 2);
//			System.out.println("rot: " + cam.getRotation().toString());
		}
	}

	public static void showMessage(String message) {
		messageModal.show(message);
	}

	public static void showMessage(String message, int seconds) {
		messageModal.show(message);
		messageModal.timeHideOkBtn = System.currentTimeMillis() + (1000*seconds);
	}

	public Scene getScene(int sceneID) {
		return scenes.get(sceneID);
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
	}

	public void charInput(String c) {
		if (usernameModal.isVisible())
			usernameModal.input(c);
		else if (messageModal.isVisible())
			return;
		getCurrentScene().charInput(c);
	}

}