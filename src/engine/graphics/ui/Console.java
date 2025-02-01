package engine.graphics.ui;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;

import engine.graphics.objects.Camera;
import engine.graphics.Renderer;
import engine.io.Window;
import main.Main;
import scenes.Scenes;
import scenes.adt.ISceneManipulation;
import scenes.adt.SceneChangeAction;

public class Console implements IUIObject, ISceneManipulation {

	private final UITextField textfield;
	private SceneChangeAction sceneChange;
	
	public Console() {
		textfield = new UITextField("", false, false, -1, Scenes.GENERAL_NONSCENE, 0, 0, Window.WIDTH, Window.HEIGHT / 32);
		textfield.getWindow().visible = false;
		textfield.getWindow().z = 4;
		textfield.background = UIColors.BLACK;
	}
	
	public void init(SceneChangeAction sceneChange) {
		this.sceneChange = sceneChange;
	}
	
	@Override
	public void layout(NkContext ctx, MemoryStack stack) {
		textfield.layout(ctx, stack);
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
	}
	
	@Override
	public void updateResolution() {
	}

	public boolean isBlocking() {
		return textfield.getWindow().visible && textfield.isFocused();
	}
	
	@Override
	public void keyInput(int keycode, int action) {
		if (action != GLFW.GLFW_RELEASE) {
			if (keycode == GLFW.GLFW_KEY_ENTER) {// keycode = | 
				runCommand(textfield.getText());
				
//				textfield.reset();
//				textfield.getWindow().visible = false;
//				textfield.focus(false);
				return;
			}

			if (keycode == 96) {// keycode = | 
				boolean open = !textfield.getWindow().visible;			
				if (!open)
					textfield.reset();
				textfield.getWindow().visible = open;
				textfield.focus(open);
				return;
			}
		}
		
		textfield.input(keycode, action);
	}
	
	private void runCommand(String command) {
		String[] cmds = command.toLowerCase().split(" ");
		if (cmds.length == 1) {
			if (cmds[0] == "d") {
				Main.DEBUG = !Main.DEBUG;
			}
		} else if (cmds.length == 2 && cmds[1].matches("-?\\d+")) {
			
			switch(cmds[0]) {
			case "mp":
				int amountPlayers = Integer.parseInt(cmds[1]);
				System.out.println("create game with " + amountPlayers);
				break;
			case "goto":
				int scenenr = Integer.parseInt(cmds[1]);
				if (scenenr >= 0 && scenenr < Scenes.AMOUNT)
					sceneChange.change(scenenr, false);
				else
					System.out.println("Tried to go to non-existent scene");
				break;
			}
		}
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		textfield.tryFocus(x, y, false);
		return false;
	}

	@Override
	public void mousePosInput(float x, float y) {
	}

	@Override
	public void mouseScrollInput(float x, float y) {
	}

	@Override
	public void tick(float delta) {
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		
	}

	@Override
	public void controllerInput() {
		// TODO Auto-generated method stub
		
	}

}
