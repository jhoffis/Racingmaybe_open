package scenes.game;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UIScrollable;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import scenes.Scenes;

public class GameRemoteMaster {

	private final IAction endAllAction;
	public boolean placeChecked, started, leaveGame, isEnding, inLobby,
				showPlayerList, showPlayerListTabbing, darkmode;
	public final UIScrollable playerList;
	private long controllerScroll;
	
	public GameRemoteMaster(IAction endAllAction) {
		this.endAllAction = endAllAction;
		float playerListX = Window.WIDTH / 5f;
		float playerListY = Window.HEIGHT / 5f;
		float playerListWidth = Window.WIDTH - 2*playerListX;
		float playerListHeight = Window.HEIGHT - 2*playerListY;
		playerList = new UIScrollable(Scenes.GENERAL_NONSCENE, playerListX, playerListY, playerListWidth, playerListHeight);
	}
	
	public void init() {
		placeChecked = false; 
		started = false;
		leaveGame = false;
		isEnding = false;
		inLobby = true;
	}
	
	public void endAll() {
		if (!isEnding) {
			if (endAllAction != null)
				endAllAction.run();
			else
				System.out.println("No endAllAction available");
			isEnding = true;
			inLobby = false;
		}
	}
	
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		if (showPlayerList) {
			Features.inst.pushBackgroundColor(ctx, darkmode ? UIColors.CHARCOAL : UIColors.LBEIGE, 0.98f);
			Features.inst.pushFontColor(ctx, darkmode ? UIColors.LBEIGE : UIColors.RAISIN_BLACK);
			
			playerList.getWindow().focus = true;
			playerList.layout(ctx, stack);

			Features.inst.popFontColor(ctx);
			Features.inst.popBackgroundColor(ctx);
		}
	}

	public void keyInput(int keycode, int action) {
		if (keycode == GLFW.GLFW_KEY_TAB) {
			showPlayerList = action != GLFW.GLFW_RELEASE;
			showPlayerListTabbing = showPlayerList;
		}
	}

	public void controllerInput() {
		if (!InputHandler.HOLDING) {
			if (InputHandler.BTN_MENU) {
				keyInput(GLFW.GLFW_KEY_TAB, GLFW.GLFW_PRESS);
			} else if (showPlayerListTabbing) {
				keyInput(GLFW.GLFW_KEY_TAB, GLFW.GLFW_RELEASE);
			}
		}
		
		if (System.currentTimeMillis() > controllerScroll) {
			if (Math.max(InputHandler.RIGHT_STICK_Y, InputHandler.LEFT_STICK_Y) > .1) {
				playerList.getWindow().focus = true;
				playerList.scroll(-1);
				controllerScroll = System.currentTimeMillis() + 45;
			} else if (Math.min(InputHandler.RIGHT_STICK_Y, InputHandler.LEFT_STICK_Y) < -.1) {
				playerList.getWindow().focus = true;
				playerList.scroll(1);
				controllerScroll = System.currentTimeMillis() + 45;
			}
		}
	}
}
