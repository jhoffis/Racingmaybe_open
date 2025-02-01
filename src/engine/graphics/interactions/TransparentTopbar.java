package engine.graphics.interactions;

import engine.io.Window;

public class TransparentTopbar {

	private final float heightRatio;
	protected TopbarInteraction topbar;

	public TransparentTopbar(TopbarInteraction topbar, float heightRatio) {
		this.topbar = topbar;
		this.heightRatio = heightRatio;
		select();
	}

	public void select() {
		topbar.select(this);
		topbar.setHeightRatio(heightRatio);
	}
	
	public void press(float x, float y) {
		topbar.press(x, y);
	}

	public boolean release() {
		return topbar.release();
	}

	public void move(float x, float y) {
		topbar.move(x, y);
	}

	public int getHeight() {
		return (int) (Window.HEIGHT / heightRatio);
	}
	
	public void setVisible(boolean visible) { // FIXME this does not do anything
	}

}
