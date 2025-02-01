package engine.graphics;

import static org.lwjgl.opengl.GL11C.GL_BLEND;
import static org.lwjgl.opengl.GL11C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11C.glBlendFunc;
import static org.lwjgl.opengl.GL11C.glEnable;
import static org.lwjgl.opengl.GL14C.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL14C.glBlendEquation;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryStack;

import engine.graphics.objects.Camera;
import engine.graphics.objects.GameObject;
import engine.math.Matrix4f;

public class Renderer {

	private final UIRender nkUI;
	private final Camera orthoCamera;
	private final String model = "model", view = "view", proj = "projection";

	public Renderer(UIRender nkUI) {
		this.nkUI = nkUI;
		orthoCamera = new Camera(false);
	}
	
	public void renderOrthoMesh(GameObject go) {
		renderMesh(go, orthoCamera);
	}
	
	public void renderMesh(GameObject go, Camera camera, int depth) {
		renderMeshPrivate(go, camera, depth);
	}

	public void renderMesh(GameObject go, Camera camera) {
		renderMeshPrivate(go, camera, GL11.GL_LEQUAL);
	}
	
	private void renderMeshPrivate(GameObject go, Camera camera, int depth) {
		glEnable(GL_DEPTH_TEST);
		GL11.glDepthFunc(depth);
		glEnable(GL_BLEND);
		glBlendEquation(GL_FUNC_ADD);
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
		// Bind
		var mesh = go.getMesh();
		var tex = mesh.getTexture(); 
		var shader = go.getShader();
		mesh.bind();
		tex.bind(GL13.GL_TEXTURE0);
		shader.bind();

		// Set uniforms // TODO run these dynamically many times
		shader.setUniform(model, go.getTransformation());
		go.resetTransformation();
		shader.setUniform(view, camera.getView());
		shader.setUniform(proj, camera.getProjection());
		shader.runUniform();

		// Draw
		GL11.glDrawElements(GL11.GL_TRIANGLES, mesh.getIndices().length,
				GL11.GL_UNSIGNED_INT, 0);

		// Unbind
		shader.unbind();
		tex.unbind();
		mesh.unbind();
	}

	public void renderNuklear(NkContext ctx) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			nkUI.setupRender(stack);
			nkUI.bind(ctx, stack, Nuklear.NK_ANTI_ALIASING_ON);
			nkUI.draw(ctx);
			nkUI.unbind();
		}
	}

	public void destroy() {
		nkUI.destroy();
	}
}
