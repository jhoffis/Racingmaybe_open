package scenes.regular;

import engine.graphics.ui.*;
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
import settings_and_logging.hotkeys.Controls;
import settings_and_logging.hotkeys.Hotkey;


public class HotkeysScene extends Scene {

	private final UITextField[] textFields;
	private UITextField focusedTextField;
	private int keycode = 0;
	private final UIWindowInfo gobackWindow, labelWindow;
	private final UIButton<?> gobackBtn, resetBtn;
	private final UILabel ctrlExplanation = new UILabel("Some hotkeys have CTRL which is a special alternative. For instance, for selling you can skip being prompted by holding CTRL.");
	private final Hotkey[] hotkeys = Controls.getConfigurableHotkeys();

	public HotkeysScene(RegularTopbar topbar) {
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
			for (Hotkey hotkey : Controls.getHotkeys()) {
				hotkey.resetToDefault();
			}
			updateGenerally(null);
		});
		add(resetBtn);

		textFields = new UITextField[hotkeys.length];
		for (int i = 0; i < textFields.length; i++) {
			var height = gobackWindow.height * .5f;
			int a = i;
			textFields[a] = new UITextField(
					"",
					false,
					false,
					1,
					sceneIndex,
					Window.WIDTH * .45f,
					gobackWindow.getYHeight() * 1.1f + a * 1.1f * height,
					Window.WIDTH * 0.3f,
					height);
			textFields[a].specialSetText = () -> {
				updateKeys(a, false);
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
	}

	private void updateKeys(int a, boolean justRefresh) {
		if (keycode == 0) {
			return;
		}
		Hotkey hotkey = getHotkey(a);

		if (!justRefresh) {
			hotkey.setKeycode(keycode);
		}
	}

	public void updateTextFieldName(int a) {
		Hotkey hotkey = Controls.getConfigurableHotkeys()[a];
		String str = hotkey.getKeyName();
		textFields[a].setPretext(str);
		textFields[a].reset();
	}

	private Hotkey getHotkey(int a) {
		return Controls.getHotkeys()[a];
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
			generalHoveredButtonNavigation(gobackBtn, keycode);
			if (focusedTextField != null) {
				this.keycode = keycode;
				String keyName = Hotkey.getKeyNameFromKeyCode(keycode);
				System.out.println("Keycode: " + keycode + " Keyname: " + keyName);
				focusedTextField.setText(keyName);
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

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
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
			for (var hotkey : hotkeys) {
				Nuklear.nk_layout_row_dynamic(ctx, height, 1);
				Nuklear.nk_label(ctx, hotkey.getPrefixedLabel(), Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
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