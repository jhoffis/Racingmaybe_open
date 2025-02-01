package scenes.adt;

import audio.AudioRemote;
import communication.GameInfo;
import engine.graphics.Renderer;
import engine.graphics.objects.Sprite;
import engine.graphics.ui.UIButton;
import main.Texts;
import scenes.game.GameRemoteMaster;

public abstract class Subscene extends Scene {

	protected GameRemoteMaster game;
	protected GameInfo com;
	protected Sprite backgroundImage;
	private int subsceneIndex;
	public UIButton<?> readyBtn;

	public Subscene(int sceneIndex) {
		super(null, sceneIndex);
	}
	
	public String getName() {
		return Texts.lobbyNames[subsceneIndex];
	}

	public void setAudio(AudioRemote audio) {
		this.audio = audio;
	}

	public void setSceneChangeAction(SceneChangeAction sceneChange) {
		this.sceneChange = sceneChange;
	}

	public void setIndex(int index) {
		this.subsceneIndex = index;
	}
	
	public int getIndex() {
		return subsceneIndex;
	}

	public void setCom(GameInfo com) {
		this.com = com;
	}
	public abstract void init();
	
	public abstract void createWindowsWithinBounds(float x, float y, float width, float height, float ssX);

	public abstract void createBackground();
	
	public abstract UIButton<?> intoNavigationSide();

	public abstract UIButton<?> intoNavigationBottom();
	
	public abstract void setVisible(boolean visible);
	
	public void renderBackground(Renderer renderer) {
        backgroundImage.setPositionZ(-1);
        renderer.renderOrthoMesh(backgroundImage);		
	}
}
