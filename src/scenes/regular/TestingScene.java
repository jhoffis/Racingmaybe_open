package scenes.regular;

import engine.graphics.Renderer;
import engine.graphics.interactions.TransparentTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Model;
import main.Texts;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import player_local.car.CarModel;
import scenes.Scenes;
import scenes.adt.Scene;

public class TestingScene extends Scene {

    public final Model car;
    public TestingScene(TransparentTopbar transparentTopbar) {
        super(transparentTopbar, Scenes.TESTING);

        car = CarModel.createModel(Texts.CAR_TYPES[1]);
        car.position().z = -5;
    }

    @Override
    public void updateGenerally(Camera cam, int... args) {
        GL11.glClearColor(.5f, .5f, .5f, 1f);
        cam.getRotation().x = -15;
        cam.getRotation().y = -42.4f;
        cam.getRotation().z = 0;
        cam.getPosition().x = -4.63f;
        cam.getPosition().y = 1.0f;
        cam.getPosition().z = -0.927f;
    }

    @Override
    public void updateResolution() {

    }

    @Override
    public void keyInput(int keycode, int action) {

    }

    @Override
    public void mouseScrollInput(float x, float y) {

    }

    @Override
    public void tick(float delta) {

    }

    @Override
    public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
        car.render(renderer, cam);
    }

    @Override
    public void renderUILayout(NkContext ctx, MemoryStack stack) {

    }

    @Override
    public void mousePositionInput(float x, float y) {

    }

    @Override
    public void destroy() {

    }

	@Override
	public void controllerInput() {
		// TODO Auto-generated method stub
		
	}
}
