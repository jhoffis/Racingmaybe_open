package engine.graphics.ui.modal;

import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_style_pop_vec2;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_vec2;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.IUIPressable;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIWindowInfo;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Texts;
import scenes.Scenes;

public class UIConfirmModal implements IUIObject, IUIPressable {

	private static UIWindowInfo window;
	private static UILabel[] label;
	private final static UIButton<?> okBtn = new UIButton<>(Texts.exitOKText),
			cancelBtn = new UIButton<>(Texts.exitCancelText);

	public UIConfirmModal() {
		window = UISceneInfo.createWindowInfo(Scenes.GENERAL_NONSCENE, 0, 0, Window.WIDTH, Window.HEIGHT);
		window.visible = false;
		window.z = 4;

		UISceneInfo.addPressableToScene(Scenes.GENERAL_NONSCENE, this);
		UISceneInfo.setChangeHoveredButtonAction(Scenes.GENERAL_NONSCENE, okBtn);
		UISceneInfo.setChangeHoveredButtonAction(Scenes.GENERAL_NONSCENE, cancelBtn);
	}

	public static void show(String text, IAction okAction) {
		label = UILabel.split(text, "\n");
		window.visible = true;
		UISceneInfo.decideFocusedWindow(window.x, window.y);
		okBtn.press();
		cancelBtn.press();
		okBtn.setPressedAction(() -> {
			window.visible = false;
			okAction.run();
		});
	}

	public void setCancelAction(IAction cancelAction) {
		cancelBtn.setPressedAction(() -> {
			cancelAction.run();
			window.visible = false;
		});
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {

		Features.inst.pushBackgroundColor(ctx, UIColors.BLACK_TRANSPARENT);

		if (window.begin(ctx)) {
			// Set own custom styling
			var spacing = NkVec2.malloc(stack);
			var padding = NkVec2.malloc(stack);

			float sp = Window.WIDTH / 30f;
			spacing.set(sp, 0);
			padding.set(sp * 2f, sp);

			nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
			nk_style_push_vec2(ctx, ctx.style().window().group_padding(), padding);

			int height = Window.HEIGHT * 2 / 5;
			int heightElements = height / 4;

			// Move group down a bit
			nk_layout_row_dynamic(ctx, height / 2f, 1);

			// Height of group
			nk_layout_row_dynamic(ctx, height, 1);

			Features.inst.pushBackgroundColor(ctx, UIColors.BLACK);

			if (nk_group_begin(ctx, "ExitGroup", UIWindowInfo.OPTIONS_STANDARD)) {
				Features.inst.pushFontColor(ctx, UIColors.WHITE);
				if (label != null) {
					for (var l : label) {
						nk_layout_row_dynamic(ctx, (float) heightElements / (float) label.length, 1);
						l.layout(ctx, stack);
					}
				}
				Features.inst.popFontColor(ctx);

				nk_layout_row_dynamic(ctx, heightElements, 2);
				okBtn.layout(ctx, stack);
				cancelBtn.layout(ctx, stack);

				// Unlike the window, the _end() function must be inside the
				// if() block
				nk_group_end(ctx);
			}

			Features.inst.popBackgroundColor(ctx);

			// Reset styling
			nk_style_pop_vec2(ctx);
			nk_style_pop_vec2(ctx);

		}
		nk_end(ctx);

		Features.inst.popBackgroundColor(ctx);
	}

	@Override
	public void release() {
		okBtn.release();
		cancelBtn.release();
	}

	@Override
	public void press() {
		okBtn.press();
		cancelBtn.press();
	}

	public boolean isVisible() {
		return window.visible;
	}

	public void setVisible(boolean v) {
		window.visible = v;
	}

	public void input(int keycode) {
		switch (keycode) {
		case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_W -> okBtn.hover();
		case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D, GLFW.GLFW_KEY_S -> cancelBtn.hover();
		case GLFW.GLFW_KEY_ENTER -> {
			UIButton<?> hoveredButton = UISceneInfo.getHoveredButton(Scenes.GENERAL_NONSCENE);
			if (hoveredButton != null) {
				hoveredButton.runPressedAction();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
			} else {
				okBtn.hover();
			}
		}
		}
	}

	public void controllerInput() {
		if (!InputHandler.HOLDING) {
			if (InputHandler.BTN_A) {
				input(GLFW.GLFW_KEY_ENTER);
				return;
			}
			if (InputHandler.BTN_B) {
				cancelBtn.runPressedAction();
				return;
			}
		}
		if (InputHandler.BTN_LEFT || InputHandler.LEFT_STICK_X < -0.5)
			input(GLFW.GLFW_KEY_LEFT);
		else if (InputHandler.BTN_RIGHT || InputHandler.LEFT_STICK_X > 0.5)
			input(GLFW.GLFW_KEY_RIGHT);
	}

}
