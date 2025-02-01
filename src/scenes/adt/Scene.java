package scenes.adt;

import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;

import java.util.ArrayList;

import org.lwjgl.glfw.GLFW;

import audio.AudioRemote;
import engine.graphics.interactions.TransparentTopbar;
import engine.graphics.objects.GameObject;
import engine.graphics.ui.ButtonNavigation;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.IUIPressable;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIWindowInfo;
import engine.io.InputHandler;
import main.Features;

public abstract class Scene implements ISceneManipulation, IUIPressable {

	protected SceneChangeAction sceneChange;
	protected AudioRemote audio;

	protected final ArrayList<GameObject> gameObjects = new ArrayList<>();
	protected int sceneIndex;
	protected TransparentTopbar topbar;
	
	public Scene(TransparentTopbar topbar, int sceneIndex) {
		this.sceneIndex = sceneIndex;

		if(topbar != null) {
			this.topbar = topbar;
			topbar.select();
		}
	}
	
	public void finalizeInit(AudioRemote audio, SceneChangeAction sceneChange) {
		this.audio = audio;
		this.sceneChange = sceneChange;
	}
	
	protected UIWindowInfo createWindow(float x, float y, float w, float h) {
		return UISceneInfo.createWindowInfo(sceneIndex, x, y, w, h);
	}

	public void generalHoveredButtonNavigation(UIButton<?> defaultButton, int keycode) {

		UIButton<?> hoveredButton = UISceneInfo.getHoveredButton(sceneIndex);

		if (hoveredButton == null) {
			hoveredButton = defaultButton;
			hoveredButton.hover();
			return;
		} else if (keycode == GLFW.GLFW_KEY_ENTER) {
			hoveredButton.runPressedAction();
			return;
		}

        switch (keycode) {
            case GLFW.GLFW_KEY_LEFT -> hoveredButton.hoverNavigate(ButtonNavigation.LEFT);
            case GLFW.GLFW_KEY_RIGHT -> hoveredButton.hoverNavigate(ButtonNavigation.RIGHT);
            case GLFW.GLFW_KEY_UP -> hoveredButton.hoverNavigate(ButtonNavigation.ABOVE);
            case GLFW.GLFW_KEY_DOWN -> hoveredButton.hoverNavigate(ButtonNavigation.BELOW);
        }
	}
	
	public void generalHoveredButtonNavigationJoy(UIButton<?> defaultButton) {
		UIButton<?> hoveredButton = UISceneInfo.getHoveredButton(sceneIndex);
		
		if (hoveredButton == null) {
			hoveredButton = defaultButton;
			hoveredButton.hover();
			return;
		} else if (InputHandler.BTN_A) {
			hoveredButton.runPressedAction();
			return;
		}
		if (InputHandler.BTN_LEFT)
			hoveredButton.hoverNavigate(ButtonNavigation.LEFT);
		else if (InputHandler.BTN_RIGHT)
			hoveredButton.hoverNavigate(ButtonNavigation.RIGHT);
		else if (InputHandler.BTN_UP)
			hoveredButton.hoverNavigate(ButtonNavigation.ABOVE);
		else if (InputHandler.BTN_DOWN)
			hoveredButton.hoverNavigate(ButtonNavigation.BELOW);
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		boolean down = action != GLFW_RELEASE;

		if(topbar != null && Features.inst != null && !Features.inst.getWindow().isFullscreen()) {
			if (down) {
				topbar.press(x, y);
			} else if (topbar.release()) {
				press();
			}
		}
		
		return down;
	}
	
	@Override
	public void mousePosInput(float x, float y) {
		if(topbar != null && Features.inst != null && !Features.inst.getWindow().isFullscreen())
			topbar.move(x, y);
		mousePositionInput(x, y);
	}
	
	public abstract void mousePositionInput(float x, float y);
	
	/**
	 * Pulls the buttons up again so they can be pressed
	 */
	@Override
	public void release() {
		for (IUIPressable uiObj : UISceneInfo.getScenePressables(sceneIndex)) {
			uiObj.release();
		}
	}

	/**
	 * Used to avoid pressing button at same position if for instance changing
	 * scenes
	 */
	@Override
	public void press() {
		for (IUIPressable uiObj : UISceneInfo.getScenePressables(sceneIndex)) {
			uiObj.press();
		}
	}

	public void add(GameObject go) {
		gameObjects.add(go);
	}

	public void add(IUIPressable pressable) {
		try {
			UISceneInfo.addPressableToScene(sceneIndex, pressable);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * recieve this topbar only to render it
	 */
	public IUIObject getTopbarRenderable() {
		return topbar instanceof IUIObject ? (IUIObject) topbar : null;
	}
	
	public TransparentTopbar getTopbarInteraction() {
		return topbar;
	}

	public void removePressables() {
		UISceneInfo.getScenePressables(sceneIndex).clear();
	}

	public void removeGameObjects() {
		
		for (GameObject go : gameObjects) {
			go.destroy();
		}
		
		gameObjects.clear();
	}
	
	public abstract void destroy();
	
	public void charInput(String c) {}
}
