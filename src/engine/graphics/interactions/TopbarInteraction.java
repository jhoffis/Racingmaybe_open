package engine.graphics.interactions;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import adt.IActionPress;
import engine.io.Window;

public class TopbarInteraction {
	private float x, y;
	private boolean held;
	private Window window;
	private float heightRatio;
	private final IActionPress pressedWithin = (float X, float Y) -> {
		// Move window
		setX(X);
		setY(Y);
		setHeld(true);
	};
	private TransparentTopbar topbar;

	public TopbarInteraction(Window window) {
		this.window = window;
	}

	public void press(float x, float y) {
		if (y < getHeight()) {
			pressedWithin.run(x, y);
		}
	}

	public boolean release() {
		held = false;
		return window.setFullscreen();
	}

	public void move(float toX, float toY) {
		if (held) {
			IntBuffer xb = BufferUtils.createIntBuffer(1);
			IntBuffer yb = BufferUtils.createIntBuffer(1);
			GLFW.glfwGetWindowPos(window.getWindow(), xb, yb);

			int x = (int) (xb.get() + (toX - this.x));
			int y = (int) (yb.get() + (toY - this.y));
			
			GLFW.glfwSetWindowPos(window.getWindow(), x, y);
		}
	}

	public float getHeight() {
		return Window.HEIGHT / heightRatio;
	}

	public boolean isHeld() {
		return held;
	}

	public void setHeld(boolean held) {
		this.held = held;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void setHeightRatio(float heightRatio) {
		this.heightRatio = heightRatio;
	}

	public void select(TransparentTopbar topbar) {
		if(this.topbar != null)
			this.topbar.setVisible(false);
		this.topbar = topbar;
		this.topbar.setVisible(true);
	}
	
}
