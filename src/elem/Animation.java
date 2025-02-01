package elem;

import engine.graphics.objects.Sprite;
import main.ResourceHandler;

public class Animation {

	protected double currentFrame;
	protected int framesAmount;
	protected String frameName;
	protected Sprite[] frames;

	public Animation(String frameName, String shader, int framesAmount, int currentFrame, float size) {
		init(frameName, shader, framesAmount, currentFrame, size);
	}
	
	public void init(String frameName, String shader, int framesAmount, int currentFrame, float size) {
		this.frameName = frameName;
		this.framesAmount = framesAmount;
		this.currentFrame = currentFrame;

		if (framesAmount > 0) {
			frames = new Sprite[framesAmount];
			// Hent bilde
			for (int i = 0; i < frames.length; i++) {
				final int frame = i;
				ResourceHandler.LoadSprite(size,"./images/" +  frameName + i +  ".png", shader, (sprite) -> this.frames[frame] = sprite);
			}

		}
	}

//	public int getHalfWidth() {
//		return width / 2;
//	}
//
//	public int getHalfHeight() {
//		return height / 2;
//	}
//
//	public int getWidth() {
//		return width;
//	}
//
//	public int getHeight() {
//		return height;
//	}

//	public void setSize(int width, int height) {
//		this.width = width;
//		this.height = height;
//	}
//	
//	public void scale(double amount) {
//		width = (int) (width * amount);
//		height = (int) (height * amount);
//	}

	public Sprite getFrame() {
		if((int) this.currentFrame >= framesAmount)
			incrementCurrentFrame(1);
		
		return frames[(int) this.currentFrame];
	}

	public void incrementCurrentFrame(double tickFactor) {
		currentFrame = Math.abs((currentFrame + (1 * tickFactor)) % framesAmount);
	}
	
	public void setPos(float x, float y) {
		for (var frame : frames) {
			frame.setPositionX(x);
			frame.setPositionY(y);
		}
	}

	public double getCurrentFrame() {
		return currentFrame;
	}

	public void setCurrentFrame(double currentFrame) {
		this.currentFrame = currentFrame;
	}

	public int getFramesAmount() {
		return framesAmount;
	}

	public void setFramesAmount(int size) {
		this.framesAmount = Math.abs(size);
	}

	public String getFrameName() {
		return frameName;
	}

	public void setFrameName(String frameName) {
		this.frameName = frameName;
	}

	public Sprite[] getFrames() {
		return frames;
	}

	public void setFrames(Sprite[] frames) {
		this.frames = frames;
	}

}
