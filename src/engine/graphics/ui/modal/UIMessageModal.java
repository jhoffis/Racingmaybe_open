package engine.graphics.ui.modal;

import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_LEFT;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_label;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_style_pop_vec2;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_vec2;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

import engine.graphics.ui.IUIObject;
import engine.graphics.ui.IUIPressable;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIWindowInfo;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Texts;
import scenes.Scenes;

public class UIMessageModal implements IUIObject, IUIPressable {

	private UIWindowInfo window;
	private String[] labels;
	private UIButton<?> okBtn;
	public long timeHideOkBtn;

	public UIMessageModal() {

		// Buttons
		okBtn = new UIButton<>(Texts.exitOKText);

		okBtn.setPressedAction(() -> hide());

		window = UISceneInfo.createWindowInfo(Scenes.GENERAL_NONSCENE,
				0, 
				0, 
				Window.WIDTH, 
				Window.HEIGHT);
		window.visible = false;
		window.z = 3;

		UISceneInfo.addPressableToScene(Scenes.GENERAL_NONSCENE, this);
		UISceneInfo.setChangeHoveredButtonAction(Scenes.GENERAL_NONSCENE, okBtn);

	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {
		if (labels == null)
			return;
		// Create a rectangle for the window
		Features.inst.pushBackgroundColor(ctx, UIColors.BLACK_TRANSPARENT);

		if(window.begin(ctx)) {
			// Set own custom styling
			NkVec2 spacing = NkVec2.malloc(stack);
			NkVec2 padding = NkVec2.malloc(stack);

			float sp = Window.WIDTH / 30f;
			spacing.set(sp, 0);
			padding.set(sp * 2f, sp);

			nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
			nk_style_push_vec2(ctx, ctx.style().window().group_padding(), padding);

			int height = Window.HEIGHT * 2 / 5;

			// Move group down a bit
			nk_layout_row_dynamic(ctx, height / 2, 1);

			// Height of group
			nk_layout_row_dynamic(ctx, height, 1);

			Features.inst.pushBackgroundColor(ctx, UIColors.BLACK);

			if (nk_group_begin(ctx, "MessageGroup", UIWindowInfo.OPTIONS_STANDARD)) {
				Features.inst.pushFontColor(ctx, UIColors.WHITE);
				for (String label : labels) {
					nk_layout_row_dynamic(ctx, (height * .35f) / labels.length, 1);
					nk_label(ctx, label, NK_TEXT_ALIGN_LEFT);
				}

				nk_layout_row_dynamic(ctx, height / 10f, 1);
				
				nk_layout_row_dynamic(ctx, height / 5f, 1);
				long now = System.currentTimeMillis();
				if (now < timeHideOkBtn) {
					nk_label(ctx, "Patience young racer...", NK_TEXT_ALIGN_LEFT);
					Features.inst.popFontColor(ctx);
				} else {
					Features.inst.popFontColor(ctx);
					okBtn.layout(ctx, stack);
				}

				// Unlike the window, the _end() function must be inside
				// the if() block
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
	}
	
	@Override
	public void press() {
		okBtn.press();
	}

	public boolean isVisible() {
		return window.visible;
	}

	public void show(String message) {
		if (message != null && message.length() > 0) {
			this.labels = message.split("\n");
			window.visible = true;
			press();
		}
	}

	public void hide() {
		window.visible = false;
	}

	public void input(int keycode, int action) {
		if (System.currentTimeMillis() > timeHideOkBtn)
			return;

		if (action == 1) {
			if (keycode == GLFW.GLFW_KEY_ENTER) {
				okBtn.runPressedAction();
			}
		}
	}

	public void controllerInput() {
		if (System.currentTimeMillis() > timeHideOkBtn)
			return;

		if (InputHandler.BTN_A || InputHandler.BTN_B || InputHandler.BTN_X || InputHandler.BTN_Y)
			okBtn.runPressedAction();
	}

}
