package engine.graphics.interactions;

import static org.lwjgl.nuklear.Nuklear.nk_end;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIWindowInfo;
import engine.io.Window;
import main.Main;
import scenes.Scenes;

public class RegularTopbar extends TransparentTopbar implements IUIObject {

	private final UIWindowInfo window;
	private final UILabel title;
	private final UIButton<?> minimizeButton, closeButton;
	private boolean showButtons;

	public RegularTopbar(UIButton<?> minimizeButton, UIButton<?> closeButton, TopbarInteraction topbar) {
		super(topbar, 13);
		
		this.minimizeButton = minimizeButton;
		this.closeButton = closeButton;
		
		title = new UILabel();
		title.options = Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_TOP;
		
		window = UISceneInfo.createWindowInfo(Scenes.GENERAL_NONSCENE,
				0, 
				0, 
				Window.WIDTH, 
				topbar.getHeight());
		//window.visible = false;
	}

	public void setTitle(String title) {
		this.title.setText(Main.NAME + " " + Main.VERSION + " - " + title + "#" + UIColors.WHITE);
	}
	
	float a = 0f;

	public void layout(NkContext ctx, MemoryStack stack) {
		if (a < 1f)
			a+= 0.011f;
		float height = 32f * Window.HEIGHT / 1280f;
		float percent = 2f*2f*height / window.width;
		window.focus = true;
		minimizeButton.alphaFactor = a;
		closeButton.alphaFactor = a;
		if(window.begin(ctx)) { //, stack, height, (int) (height / 3 * 0.65), height, 0)) {
			// Layout
			Nuklear.nk_layout_row_begin(ctx, Nuklear.NK_DYNAMIC, height, 3);
			Nuklear.nk_layout_row_push(ctx, 1f - percent);
			title.layout(ctx, stack);
			
			if (showButtons) {
				// Empty space
				Nuklear.nk_layout_row_push(ctx, percent * 0.5f);
				minimizeButton.layout(ctx, stack);
				Nuklear.nk_layout_row_push(ctx, percent * 0.5f);
				closeButton.layout(ctx, stack);
				Nuklear.nk_layout_row_end(ctx);
			}
		}
		nk_end(ctx);
	}
	
	@Override
	public void setVisible(boolean visible) {
		if(window != null)
			window.visible = visible;
	}

	public void showButtons(boolean b) {
		showButtons = b;
	}

}
