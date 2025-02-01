package scenes.regular;

import engine.graphics.ui.*;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import audio.SfxTypes;
import engine.graphics.Renderer;
import engine.graphics.interactions.RegularTopbar;
import engine.graphics.objects.Camera;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Texts;
import scenes.Scenes;
import scenes.adt.Scene;
import settings_and_logging.ControlsSettings;
import settings_and_logging.hotkeys.RaceKeys;

/**
 * 
 * const char* key_name = glfwGetKeyName(GLFW_KEY_W, 0);
show_tutorial_hint("Press %s to move forward", key_name);
 * 
 * @author Jens Benz
 *
 */
public class HotkeysScene extends Scene {

	private final UITextField[] textFields;
	private UITextField focusedTextField;

	private int keycode = 0;
	
	private final UIWindowInfo gobackWindow, labelWindow;
	private final UIButton<?> gobackBtn, resetBtn;
	private final UILabel ctrlExplanation = new UILabel("Some hotkeys have CTRL which is a special alternative. For instance, for selling you can skip being prompted by holding CTRL.");
	private final String[] hotkeyLabels = new String[] { 
			"Throttle:",
			"Brake:",
			"Nos:",
			"Turbo:",
			"Look behind:",
			"Shift Up:",
			"Shift Down:",
			"Quit Race:",
			"(CTRL: ready while chatting) Ready:",
			"(CTRL: redo) Undo:",
			"(CTRL: no prompt) Sell tile:",
			"Improve tile:",
	};

	public static String Throttle,
			Brake,
			Nos,
			Turbo,
			LookBehind,
			ShiftUp,
			ShiftDown,
			QuitRace,
			Ready,
			Undo,
			SellTile,
			ImproveTile;
	
	private void updateKeys(int a, ControlsSettings controlsSettings, boolean justRefresh) {
		switch (a) {
			case 0 -> {
				if (justRefresh) {
					RaceKeys.throttle = controlsSettings.getThrottle();
					return;
				}
				controlsSettings.setThrottle(keycode);
				RaceKeys.throttle = keycode;
			}
			case 1 -> {
				if (justRefresh) {
					RaceKeys.brake = controlsSettings.getBrake();
					return;
				}
				controlsSettings.setBrake(keycode);
				RaceKeys.brake = keycode;
			}
			case 2 -> {
				if (justRefresh) {
					RaceKeys.nos = controlsSettings.getNOS();
					return;
				}
				controlsSettings.setNOS(keycode);
				RaceKeys.nos = keycode;
			}
			case 3 -> {
				if (justRefresh) {
					RaceKeys.blowTurbo = controlsSettings.getTurboBlow();
					return;
				}
				controlsSettings.setTurboBlow(keycode);
				RaceKeys.blowTurbo = keycode;
			}
			case 4 -> {
				if (justRefresh) {
					RaceKeys.lookBehind = controlsSettings.getLookBehind();
					return;
				}
				controlsSettings.setLookBehind(keycode);
				RaceKeys.lookBehind = keycode;
			}
			case 5 -> {
				if (justRefresh) {
					RaceKeys.shiftUp = controlsSettings.getGearUp();
					return;
				}
				controlsSettings.setGearUp(keycode);
				RaceKeys.shiftUp = keycode;
			}
			case 6 -> {
				if (justRefresh) {
					RaceKeys.shiftDown = controlsSettings.getGearDown();
					return;
				}
				controlsSettings.setGearDown(keycode);
				RaceKeys.shiftDown = keycode;
			}
			case 7 -> {
				if (justRefresh) {
					RaceKeys.quitRace = controlsSettings.getQuitRace();
					return;
				}
				controlsSettings.setQuitRace(keycode);
				RaceKeys.quitRace = keycode;
			}
			case 8 -> {
				if (justRefresh) {
					RaceKeys.ready = controlsSettings.getReady();
					return;
				}
				controlsSettings.setReady(keycode);
				RaceKeys.ready = keycode;
			}
			case 9 -> {
				if (justRefresh) {
					RaceKeys.undo = controlsSettings.getUndo();
					return;
				}
				controlsSettings.setUndo(keycode);
				RaceKeys.undo = keycode;
			}
			case 10 -> {
				if (justRefresh) {
					RaceKeys.sell = controlsSettings.getSell();
					return;
				}
				controlsSettings.setSell(keycode);
				RaceKeys.sell = keycode;
			}
			case 11 -> {
				if (justRefresh) {
					RaceKeys.improve = controlsSettings.getImprove();
					return;
				}
				controlsSettings.setImprove(keycode);
				RaceKeys.improve = keycode;
			}
			default -> System.out.println("Unexpected value: " + a);
		}
	}
	
	public void updateTextFieldName(int a) {
		String str;
		switch (a) {
			case 0 -> {
				str = getKeyName(RaceKeys.throttle);
				textFields[a].setPretext(str);
				Throttle = str;
			}
			case 1 -> {
				str = getKeyName(RaceKeys.brake);
				textFields[a].setPretext(str);
				Brake = str;
			}
			case 2 -> {
				str = getKeyName(RaceKeys.nos);
				textFields[a].setPretext(str);
				Nos = str;
			}
			case 3 -> {
				str = getKeyName(RaceKeys.blowTurbo);
				textFields[a].setPretext(str);
				Turbo = str;
			}
			case 4 -> {
				str = getKeyName(RaceKeys.lookBehind);
				textFields[a].setPretext(str);
				LookBehind = str;
			}
			case 5 -> {
				str = getKeyName(RaceKeys.shiftUp);
				textFields[a].setPretext(str);
				ShiftUp = str;
			}
			case 6 -> {
				str = getKeyName(RaceKeys.shiftDown);
				textFields[a].setPretext(str);
				ShiftDown = str;
			}
			case 7 -> {
				str = getKeyName(RaceKeys.quitRace);
				textFields[a].setPretext(str);
				QuitRace = str;
			}
			case 8 -> {
				str = getKeyName(RaceKeys.ready);
				textFields[a].setPretext(str);
				Ready = str;
			}
			case 9 -> {
				str = getKeyName(RaceKeys.undo);
				textFields[a].setPretext(str);
				Undo = str;
			}
			case 10 -> {
				str = getKeyName(RaceKeys.sell);
				textFields[a].setPretext(str);
				SellTile = str;
			}
			case 11 -> {
				str = getKeyName(RaceKeys.improve);
				textFields[a].setPretext(str);
				ImproveTile = str;
			}
			default -> throw new IllegalArgumentException("Unexpected value: " + a);
		}
		textFields[a].reset();
	}
	

	public HotkeysScene(RegularTopbar topbar, ControlsSettings controlsSettings) {
		super(topbar, Scenes.HOTKEY_OPTIONS);
		float spacer = topbar.getHeight() * 0.5f;
		gobackWindow = createWindow(spacer, spacer, Window.WIDTH, 1.5f * topbar.getHeight());

		gobackBtn = new UIButton<>(Texts.gobackText, UIColors.DARKGRAY);
		gobackBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.OPTIONS, false);
		});
		add(gobackBtn);

		resetBtn = new UIButton<>("Reset", UIColors.DARKGRAY);
		resetBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			controlsSettings.init();
			for (int i = 0; i < hotkeyLabels.length; i++) {
				updateKeys(i, controlsSettings, true);
			}
			updateGenerally(null);
		});
		add(resetBtn);

//		throttleInput = ;

		textFields = new UITextField[hotkeyLabels.length];
		for (int i = 0; i < textFields.length; i++) {
			var height = gobackWindow.height * .5f;
			int a = i;
			updateKeys(a, controlsSettings, true);
			textFields[a] = 
					new UITextField(
							"", 
							false, 
							false, 
							1, 
							sceneIndex, 
							Window.WIDTH * .45f,
							gobackWindow.getYHeight() * 1.1f + a * 1.1f*height, 
							Window.WIDTH * 0.3f, 
							height);
			textFields[a].specialSetText = () -> {
				updateKeys(a, controlsSettings, false);
				updateTextFieldName(a);
			};
			updateTextFieldName(a);
		}
		labelWindow = createWindow(
				Window.WIDTH * .1f,
				textFields[0].getWindow().y, 
				textFields[0].getWindow().x - Window.WIDTH * .1f, 
				Window.HEIGHT - textFields[0].getWindow().y
		);

		/*
		 * "Controls:", "Player scores: TAB", "Throttle: W", "NOS: E", "Turbo Blow: Q",
		 * "Look behind: R"
		 */
	}

	public String getKeyName(int keycode) {
		var key = GLFW.glfwGetKeyName(keycode, InputHandler.SCANCODE);
		if (key != null) {
			key = key.toUpperCase();
		} else {
			key = switch (keycode) {
			case GLFW.GLFW_KEY_SPACE:
				yield "Space";
			case GLFW.GLFW_KEY_CAPS_LOCK:
				yield "Caps Lock";
			case GLFW.GLFW_KEY_LEFT_SHIFT:
				yield "Left Shift";
			case GLFW.GLFW_KEY_RIGHT_SHIFT:
				yield "Right Shift";
			case GLFW.GLFW_KEY_MENU:
				yield "Menu";
			case GLFW.GLFW_KEY_LEFT_CONTROL:
				yield "Left Control";
			case GLFW.GLFW_KEY_RIGHT_CONTROL:
				yield "Right Control";
			case GLFW.GLFW_KEY_LEFT_ALT:
				yield "Left Alt";
			case GLFW.GLFW_KEY_RIGHT_ALT:
				yield "Right Alt";
			case GLFW.GLFW_KEY_TAB:
				yield "Tab";
			case GLFW.GLFW_KEY_LEFT_SUPER:
				yield "Left Super";
			case GLFW.GLFW_KEY_RIGHT_SUPER:
				yield "Right Super";
			case GLFW.GLFW_KEY_INSERT:
				yield "Insert";
			case GLFW.GLFW_KEY_DELETE:
				yield "Delete";
			case GLFW.GLFW_KEY_PAUSE:
				yield "Pause";
			case GLFW.GLFW_KEY_HOME:
				yield "Home";
			case GLFW.GLFW_KEY_END:
				yield "End";
			case GLFW.GLFW_KEY_PAGE_UP:
				yield "Page Up";
			case GLFW.GLFW_KEY_PAGE_DOWN:
				yield "Page Down";
			case GLFW.GLFW_KEY_PRINT_SCREEN:
				yield "Print Screen";
			case GLFW.GLFW_KEY_UP:
				yield "Up Arrow";
			case GLFW.GLFW_KEY_DOWN:
				yield "Down Arrow";
			case GLFW.GLFW_KEY_LEFT:
				yield "Left Arrow";
			case GLFW.GLFW_KEY_RIGHT:
				yield "Right Arrow";
			case GLFW.GLFW_KEY_BACKSPACE:
				yield "Backspace";
			case GLFW.GLFW_KEY_ESCAPE:
				yield "ESC";
			case GLFW.GLFW_KEY_ENTER:
				yield "Enter";
			default:
				System.out.println("dont have a name for " + keycode);
				yield null;
			};
		}
		return key;
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		press();
		((RegularTopbar) topbar).showButtons(false);
		
		for (int i = 0; i < textFields.length; i++) {
			updateTextFieldName(i);
		}
	}

	@Override
	public void updateResolution() {
	}

	@Override
	public void tick(float delta) {
	}

	@Override
	public void keyInput(int keycode, int action) {

		if (action == 1) {
			// Downstroke for quicker input
			generalHoveredButtonNavigation(gobackBtn, keycode);
			if (focusedTextField != null) {
				this.keycode = keycode;
				focusedTextField.setText(getKeyName(keycode));
			}
		}

	}

	@Override
	public void controllerInput() {
		generalHoveredButtonNavigationJoy(gobackBtn);
		if (InputHandler.BTN_B)
			gobackBtn.runPressedAction();
	}


	@Override
	public void mouseScrollInput(float x, float y) {
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		var down = super.mouseButtonInput(button, action, x, y);

		focusedTextField = null;
		for (var textField : textFields) {
			textField.tryFocus(x, y, true);
			if (textField.isFocused())
				focusedTextField = textField;
		}

		return down;
	}

	@Override
	public void mousePositionInput(float x, float y) {
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
		Features.inst.pushFontColor(ctx, UIColors.WHITE);

		if (gobackWindow.begin(ctx)) {
			Nuklear.nk_layout_row_begin(ctx, Nuklear.NK_DYNAMIC, gobackWindow.height * .45f, 2);
			Nuklear.nk_layout_row_push(ctx, .25f);
			gobackBtn.layout(ctx, stack);
			Nuklear.nk_layout_row_end(ctx);

			Nuklear.nk_layout_row_begin(ctx, Nuklear.NK_DYNAMIC, gobackWindow.height * .45f, 3);
			Nuklear.nk_layout_row_push(ctx, .25f);
			resetBtn.layout(ctx, stack);
			Nuklear.nk_layout_row_push(ctx, .02f);
			Nuklear.nk_label(ctx, "", Nuklear.NK_TEXT_ALIGN_MIDDLE);
			Nuklear.nk_layout_row_push(ctx, .73f);
			ctrlExplanation.layout(ctx, stack);
			Nuklear.nk_layout_row_end(ctx);
		}
		Nuklear.nk_end(ctx);

		for (var textField : textFields) {
			textField.layout(ctx, stack);
		}

		if (labelWindow.begin(ctx, stack, 15, 0, 0, 0)) {
			var height = 1.015f*textFields[0].getWindow().height;
			for (var hotkeyLabel : hotkeyLabels) {
				Nuklear.nk_layout_row_dynamic(ctx, height, 1);
				Nuklear.nk_label(ctx, hotkeyLabel, Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
			}
		}
		Nuklear.nk_end(ctx);
		
		Features.inst.popFontColor(ctx);
	}

	@Override
	public void destroy() {
		removeGameObjects();
	}

}
