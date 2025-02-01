package scenes.regular;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import engine.graphics.interactions.TransparentTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.ui.Font;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UIFont;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UIWindowInfo;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Texts;
import scenes.Scenes;
import scenes.adt.Scene;
import scenes.game.Lobby;

public class JoiningScene extends Scene {
	
	private final UIButton<?> cancelBtn;
	private final UIFont joiningFont;
	private final UILabel joiningLabel;
	private final UIWindowInfo window;

	public JoiningScene(Lobby lobby, TransparentTopbar topbar) {
		super(topbar, Scenes.JOINING);
		
		window = createWindow(0, 0, Window.WIDTH, Window.HEIGHT);
		
		joiningFont = new UIFont(Font.BOLD_REGULAR, 0);
		joiningLabel = new UILabel();
		joiningLabel.setText(Texts.joining);
		joiningLabel.options = Nuklear.NK_TEXT_ALIGN_MIDDLE | Nuklear.NK_TEXT_ALIGN_CENTERED;
		
		cancelBtn = new UIButton<>(Texts.gobackText);
		cancelBtn.setPressedAction(() -> {
			lobby.getGame().leaveGame = true;
			lobby.tick(0);
			sceneChange.change(Scenes.MULTIPLAYER, false);
		});
		
		add(cancelBtn);
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
//		GL11.glClearColor(0.3f, 0.3f, 0.3f, 1);
	}

	@Override
	public void updateResolution() {
		joiningFont.resizeFont(Window.HEIGHT / 20f);
	}

	@Override
	public void keyInput(int keycode, int action) {
	}

	@Override
	public void mouseScrollInput(float x, float y) {
	}

	@Override
	public void mousePositionInput(float x, float y) {
	}

	@Override
	public void tick(float delta) {
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		Features.inst.pushFontColor(ctx, UIColors.WHITE);
		if(window.begin(ctx)) {
			Nuklear.nk_style_push_font(ctx, joiningFont.getFont());
			Nuklear.nk_layout_row_dynamic(ctx, Window.HEIGHT * 0.9f, 1);
			joiningLabel.layout(ctx, stack);
			Nuklear.nk_style_pop_font(ctx);

			Nuklear.nk_layout_row_dynamic(ctx, Window.HEIGHT * 0.08f, 3);
			Nuklear.nk_label(ctx, "", 0);
			cancelBtn.layout(ctx, stack);
		}
		Nuklear.nk_end(ctx);
		Features.inst.popFontColor(ctx);
	}

	@Override
	public void destroy() {
	}

	@Override
	public void controllerInput() {
		if (InputHandler.BTN_B) {
			cancelBtn.runPressedAction();
		}		
	}

}
