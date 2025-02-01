package scenes.adt;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;

import engine.graphics.objects.Camera;
import engine.graphics.Renderer;

public interface ISceneManipulation {

	// run me first before any init under (except finalizeInit)
	void updateGenerally(Camera cam, int... args);
	
	void updateResolution();
	
	void keyInput(int keycode, int action);

	void controllerInput();

	boolean mouseButtonInput(int button, int action, float x, float y);

	void mousePosInput(float x, float y);

	void mouseScrollInput(float x, float y);

	void tick(float delta);
	
	abstract void renderGame(Renderer renderer, Camera cam, long window, float delta);
	
	abstract void renderUILayout(NkContext ctx, MemoryStack stack);

}
