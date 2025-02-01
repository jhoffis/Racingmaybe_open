package engine.io;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetClipboardString;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.nglfwGetClipboardString;
import static org.lwjgl.nuklear.Nuklear.NK_BUTTON_LEFT;
import static org.lwjgl.nuklear.Nuklear.NK_BUTTON_MIDDLE;
import static org.lwjgl.nuklear.Nuklear.NK_BUTTON_RIGHT;
import static org.lwjgl.nuklear.Nuklear.nk_input_button;
import static org.lwjgl.nuklear.Nuklear.nk_input_motion;
import static org.lwjgl.nuklear.Nuklear.nk_input_unicode;
import static org.lwjgl.nuklear.Nuklear.nnk_strlen;
import static org.lwjgl.nuklear.Nuklear.nnk_textedit_paste;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memCopy;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.charset.StandardCharsets;
import java.util.function.DoubleConsumer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWCursorPosCallbackI;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import engine.graphics.ui.UISceneInfo;
import main.Main;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.ISceneManipulation;
import scenes.adt.Scene;
import settings_and_logging.ControlsSettings;

public class InputHandler {

	public static int MOUSEBUTTON, MOUSEACTION;
	public static boolean CONTROL_DOWN;
	public static int SCANCODE;

	public static float x, y;
	private static ISceneManipulation currentScene;
	private ControlsSettings keys;
	public static boolean CONTROLLER, HOLDING, REPEAT, WAS_BUTTON, CONTROLLER_EFFECTIVELY, CHANGE;
	private static int CONTROLLER_JID = -1;
	private static long REPEAT_TIME;

	public static float LEFT_STICK_X, LEFT_STICK_Y, RIGHT_STICK_X, RIGHT_STICK_Y, LEFT_TRIGGER, RIGHT_TRIGGER;

	public static boolean BTN_A, BTN_B, BTN_X, BTN_Y, BTN_BACK_TOP_LEFT, BTN_BACK_TOP_RIGHT, BTN_MENU, BTN_UP, BTN_DOWN,
			BTN_LEFT, BTN_RIGHT;
	
	private final Window win;
	
	private static GLFWCursorPosCallbackI mouseInput;
	public static boolean holdOffMouse;
	
	public InputHandler(Window win, NkContext ctx) {
		this.win = win;
		keys = new ControlsSettings();

		long myWindow = win.getWindow();

//		glfwSetCursorEnterCallback(myWindow,
//				GLFWCursorEnterCallback.create((window, entered) -> {
//					this.currentScene.mouseEnterWindowInput(entered);
//				}));

		glfwSetKeyCallback(myWindow, GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
			if (CONTROLLER_EFFECTIVELY)
				win.mouseStateHide(false);
			CONTROLLER_EFFECTIVELY = false;
			SCANCODE = scancode;
			CONTROL_DOWN = action == GLFW.GLFW_PRESS && mods == (mods | GLFW.GLFW_MOD_CONTROL);

			InputHandler.currentScene.keyInput(key, action);
//					Nuklear.nk_input_unicode(ctx, key);				
		}));

		glfwSetMouseButtonCallback(myWindow, GLFWMouseButtonCallback.create((window, button, action, mods) -> {
			if (holdOffMouse) {
				if (action == GLFW.GLFW_RELEASE) {
					holdOffMouse = false;
					try (MemoryStack stack = stackPush()) {
						nk_input_button(ctx, NK_BUTTON_LEFT, 0, 0, false);
					}
				}
				return;
			}
			
			CONTROLLER_EFFECTIVELY = false;
			win.mouseStateHide(false);
			MOUSEBUTTON = button;
			MOUSEACTION = action;
			InputHandler.currentScene.mouseButtonInput(button, action, x, y);
			try (MemoryStack stack = stackPush()) {
				DoubleBuffer cx = stack.mallocDouble(1);
				DoubleBuffer cy = stack.mallocDouble(1);

				glfwGetCursorPos(window, cx, cy);

				int x = (int) cx.get(0);
				int y = (int) cy.get(0);

				int nkButton;
				switch (button) {
				case GLFW_MOUSE_BUTTON_RIGHT:
					nkButton = NK_BUTTON_RIGHT;
					break;
				case GLFW_MOUSE_BUTTON_MIDDLE:
					nkButton = NK_BUTTON_MIDDLE;
					break;
				default:
					nkButton = NK_BUTTON_LEFT;
				}
				nk_input_button(ctx, nkButton, x, y, action != GLFW.GLFW_RELEASE);
			}
		}));

		mouseInput = (window, xpos, ypos) -> {
			if (CONTROLLER_EFFECTIVELY)
				return;
			nk_input_motion(ctx, (int) xpos, (int) ypos);
//					System.out.println("x: " + xpos);
//					System.out.println("y: " + ypos);
			x = (float) xpos;
			y = (float) ypos;
			if (InputHandler.currentScene != null)
				InputHandler.currentScene.mousePosInput(x, y);
		};
		
		glfwSetCursorPosCallback(myWindow, GLFWCursorPosCallback.create(mouseInput));

		GLFW.glfwSetScrollCallback(myWindow, GLFWScrollCallback.create((window, xoffset, yoffset) -> {

			float x = (float) xoffset;
			float y = (float) yoffset;

			InputHandler.currentScene.mouseScrollInput(x, y);
		}));

		// Connects
		GLFW.glfwSetJoystickCallback(InputHandler::initController);
		for (int jid = 0; jid < 16; jid++) {
			CONTROLLER = GLFW.glfwJoystickPresent(jid);
			if (CONTROLLER) {
				initController(jid, GLFW.GLFW_CONNECTED);
				break;
			}
		}

		glfwSetCharCallback(myWindow, (window, codepoint) -> {
//					nk_input_unicode(ctx, codepoint);
			if (InputHandler.currentScene instanceof SceneHandler s) {
//						s.charInput(Integer.toUnsignedString(codepoint));

				s.charInput(Character.toString(codepoint));
			}
		});

		ctx.clip().copy((handle, text, len) -> {
			if (len == 0) {
				return;
			}

			try (MemoryStack stack = stackPush()) {
				ByteBuffer str = stack.malloc(len + 1);
				memCopy(text, memAddress(str), len);
				str.put(len, (byte) 0);

				glfwSetClipboardString(myWindow, str);
			}
		}).paste((handle, edit) -> {
			long text = nglfwGetClipboardString(myWindow);
			if (text != NULL) {
				nnk_textedit_paste(edit, text, nnk_strlen(text));
			}
		});
	}

	private static void initController(int jid, int event) {
		System.out.println("jid: " + jid + ", event: " + event);
		CONTROLLER = event == GLFW.GLFW_CONNECTED;
		if (CONTROLLER) {
			CONTROLLER_JID = jid;
			mouseInput.invoke(0, 0, 500);
//			InputHandler.currentScene.mousePosInput(x, y);
		} else if (jid == CONTROLLER_JID) {
			CONTROLLER_JID = -1;
		}
	}

	public void checkController() {
		if (!CONTROLLER)
			return;
		try {
			boolean wasInput = false;

			var axes = GLFW.glfwGetJoystickAxes(CONTROLLER_JID);
			var i = 0;
			while (axes.hasRemaining()) {
				var b = Math.round(100f * axes.get()) / 100f;

				switch (i) {
				case 0:
					if (LEFT_STICK_X != b) {
						LEFT_STICK_X = b;
						wasInput = true;
					}
					break;
				case 1:
					if (LEFT_STICK_Y != b) {
						LEFT_STICK_Y = b;
						wasInput = true;
					}
					break;
				case 2:
					if (RIGHT_STICK_X != b) {
						RIGHT_STICK_X = b;
						wasInput = true;
					}
					break;
				case 3:
					if (RIGHT_STICK_Y != b) {
						RIGHT_STICK_Y = b;
						wasInput = true;
					}
					break;
				case 4:
					if (LEFT_TRIGGER != b) {
						LEFT_TRIGGER = b;
						wasInput = true;
					}
					break;
				case 5:
					if (RIGHT_TRIGGER != b) {
						RIGHT_TRIGGER = b;
						wasInput = true;
					}
					break;
				}

				i++;
			}
			var wasAxes = wasInput;

			var btns = GLFW.glfwGetJoystickButtons(CONTROLLER_JID);
			i = 0;
			while (btns.hasRemaining()) {
				var b = btns.get();

//				if (b != 0)
//					System.out.println(i);

				switch (i) {
				case 0:
					if (BTN_A != (b != 0)) {
						BTN_A = !BTN_A;
						wasInput = true;
					}
					break;
				case 1:
					if (BTN_B != (b != 0)) {
						BTN_B = !BTN_B;
						wasInput = true;
					}
					break;
				case 2:
					if (BTN_X != (b != 0)) {
						BTN_X = !BTN_X;
						wasInput = true;
					}
					break;
				case 3:
					if (BTN_Y != (b != 0)) {
						BTN_Y = !BTN_Y;
						wasInput = true;
					}
					break;
				case 4:
					if (BTN_BACK_TOP_LEFT != (b != 0)) {
						BTN_BACK_TOP_LEFT = !BTN_BACK_TOP_LEFT;
						wasInput = true;
					}
					break;
				case 5:
					if (BTN_BACK_TOP_RIGHT != (b != 0)) {
						BTN_BACK_TOP_RIGHT = !BTN_BACK_TOP_RIGHT;
						wasInput = true;
					}
					break;
				case 7:
					if (BTN_MENU != (b != 0)) {
						BTN_MENU = !BTN_MENU;
						wasInput = true;
					}
					break;
				case 10:
					if (BTN_UP != (b != 0)) {
						BTN_UP = !BTN_UP;
						wasInput = true;
					}
					break;
				case 11:
					if (BTN_RIGHT != (b != 0)) {
						BTN_RIGHT = !BTN_RIGHT;
						wasInput = true;
					}
					break;
				case 12:
					if (BTN_DOWN != (b != 0)) {
						BTN_DOWN = !BTN_DOWN;
						wasInput = true;
					}
					break;
				case 13:
					if (BTN_LEFT != (b != 0)) {
						BTN_LEFT = !BTN_LEFT;
						wasInput = true;
					}
					break;
				}
				i++;
			}

			WAS_BUTTON = wasInput && !wasAxes;

			REPEAT = BTN_A || BTN_B || BTN_X || BTN_Y
					|| BTN_MENU || BTN_UP || BTN_DOWN 
					|| BTN_LEFT || BTN_RIGHT || BTN_BACK_TOP_RIGHT || BTN_BACK_TOP_LEFT;
//					|| Math.abs(LEFT_STICK_X) > .3f || Math.abs(LEFT_STICK_Y) > .3f;

			if (HOLDING && !REPEAT)
				HOLDING = false;

			CHANGE = !wasInput;

			if (wasInput) {
				if (!CONTROLLER_EFFECTIVELY) {
					mouseInput.invoke(0, 0, 500);
					InputHandler.CONTROLLER = true;
					CONTROLLER_EFFECTIVELY = true;
					win.mouseStateHide(true);
					UISceneInfo.clearHoveredButton(Scenes.CURRENT);
				}
				currentScene.controllerInput();
				if (!HOLDING) {
					HOLDING = true;
					REPEAT_TIME = System.currentTimeMillis() + 500;
				}
			} else if (REPEAT && System.currentTimeMillis() > REPEAT_TIME) {
				REPEAT_TIME = System.currentTimeMillis() + 50;
				currentScene.controllerInput();
			}
		} catch (Exception e) {
			if (Main.DEBUG)
				e.printStackTrace();
			CONTROLLER = false;
		}
	}

	public void destroy(long win) {
		glfwFreeCallbacks(win);
	}

	public void setCurrent(ISceneManipulation scene) {
		currentScene = scene;
	}

	public ControlsSettings getKeys() {
		return keys;
	}

	public static void forceMousePos() {
		currentScene.mousePosInput(x, y);
	}
}
