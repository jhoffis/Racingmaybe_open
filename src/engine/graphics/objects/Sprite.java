package engine.graphics.objects;

import engine.graphics.Shader;
import engine.graphics.Texture;
import engine.graphics.Vertex;
import engine.io.Window;
import engine.math.Vec2;
import engine.math.Vec3;

public class Sprite extends GameObject {

	public Texture texture;
	private float width, height;
	private boolean created;
	
	public Sprite() {}
	
	public Sprite(Vec2 topleftPoint, float heightSize, String spriteName, String shaderName) {
		Texture tex = new Texture(spriteName);
		this.init(topleftPoint, heightSize * tex.widthHeightRatio(), heightSize, tex, shaderName);
	}

	public Sprite(float heightSize, String spriteName, String shaderName) {
		this(new Vec2(0), heightSize, spriteName, shaderName);
	}
	
	public Sprite(String spriteName, String shaderName) {
		this(Window.HEIGHT, spriteName, shaderName);
	}

	public void init(Vec2 topleftPoint, float sizeX, float sizeY,
					  Texture sprite, String shaderName) {
		topleftPoint = new Vec2(convertToOrthoSpaceX(topleftPoint.x), convertToOrthoSpaceY(topleftPoint.y));
		sizeX /= (float) Window.WIDTH / Camera.ORTHO_SIDES;
		sizeY /= (float) Window.HEIGHT / Camera.ORTHO_SIZE;
//		topleftPoint.setY(topleftPoint.y() - sizeY);
		
		this.init(new Vec3(topleftPoint.x, topleftPoint.y, 0.0f),
				new Vec3(topleftPoint.x, topleftPoint.y - sizeY, 0.0f),
				new Vec3(topleftPoint.x + sizeX, topleftPoint.y- sizeY, 0.0f),
				new Vec3(topleftPoint.x + sizeX, topleftPoint.y, 0.0f),
				new Vec3(0),
				new Vec3(0),
				new Vec3(1),
				sprite, shaderName);
	}

	public void init(Vec3 topleft, Vec3 botleft, Vec3 botright,
                      Vec3 topright, Vec3 position, Vec3 rotation,
                      Vec3 scale, Texture sprite, String shaderName) {
		super.init(position, rotation, scale, new Mesh(
				new Vertex[]{
						new Vertex(topleft, null,
								new Vec2(0.0f, 0.0f)),
						new Vertex(botleft, null,
								new Vec2(0f, 1f)),
						new Vertex(botright, null,
								new Vec2(1f, 1f)),
						new Vertex(topright, null,
								new Vec2(1f, 0f))},
				new int[]{0, 1, 2, 0, 2, 3}, sprite), new Shader(shaderName));
		// setScale(Vector3f.mulX(getScale(), sprite.widthHeightRatio()));
		this.texture = sprite;
		
		width  = topright.x - botleft.x - Camera.ORTHO_HALFSIDES;
		height = topright.y - botleft.y + Camera.ORTHO_HALFSIZE;
	}
	
	public Sprite setToFullscreen() {
		var shaderName = shader != null ? shader.shaderName : "main";
		init(
			new Vec2(0, 0),
			Window.WIDTH,
			Window.HEIGHT,
			texture, 
			shaderName
		);
		created = false;
		create();
		return this;
	}
	
	public void create() {
		if (created) return;
		
		created = true;
		if (mesh != null) {
			shader.create();
			mesh.create();
		} else {
			texture.create();
		}
	}

	public float getWidth() {
		return convertToPixelSpaceX(width);
	}

	public float getHeight() {
		return Math.abs(convertToPixelSpaceY(height));
	}
	
	public float getXWidth() {
		return position().x + getWidth();
	}
	
	public float getYHeight() {
		return position().y + getHeight();
	}
	
	public float getWidthReal() {
		return width;
	}

	public float getHeightReal() {
		return height;
	}

	public void setPositionXReal(float x) {
		super.setPositionX(x);
	}
	
	public void setPositionYReal(float y) {
		super.setPositionY(y);
	}

	@Override
	public void setPositionX(float x) {
		super.setPositionX(convertToOrthoSpaceX(x) + Camera.ORTHO_HALFSIDES);
	}
	
	@Override
	public void setPositionY(float y) {
		super.setPositionY(convertToOrthoSpaceY(y) - Camera.ORTHO_HALFSIZE);
	}

	public void setPosition(Vec2 position) {
		setPositionX(position.x);
		setPositionY(position.y);
	}
	
	@Override
	public void setPosition(Vec3 position) {
		setPosition((Vec2) position);
	}
	
	@Override
	public Vec3 position() {
		Vec3 realPosition = mesh.getVertices()[0].getPosition();
		Vec3 res = new Vec3(realPosition.z);
		
		// g� fra punkt nede til venstre til oppe til venstre.
		res.x = convertToPixelSpaceX(position.x + realPosition.x);
		res.y = convertToPixelSpaceY(position.y + realPosition.y);
		
		return res;
	}
	
	public Vec3 getRealPosition() {
		return position;
	}
	
	private float convertToOrthoSpaceX(float x) {
		return x * (Camera.ORTHO_SIDES / Window.WIDTH) - Camera.ORTHO_HALFSIDES;
	}
	
	private float convertToOrthoSpaceY(float y) {
		y = Window.HEIGHT - y;
		return y * (Camera.ORTHO_SIZE / Window.HEIGHT) - Camera.ORTHO_HALFSIZE;
	}
	
	private float convertToPixelSpaceX(float x) {
		return (x + Camera.ORTHO_HALFSIDES) * (Window.WIDTH / Camera.ORTHO_SIDES);
	}
	
	private float convertToPixelSpaceY(float y) {
		return Window.HEIGHT - ((y + Camera.ORTHO_HALFSIZE) * (Window.HEIGHT / Camera.ORTHO_SIZE));
	}

	public boolean above(float x, float y) {
		return above(position(), x, y);
	}

	public boolean above(Vec2 p, float x, float y) {
		return (y <= p.y + getHeight() && y >= p.y) &&
		    (x <= p.x + getWidth()  && x >= p.x);
	}
	
	public boolean aboveWithMargin(Vec2 p, float x, float y, float margin) {
		return (y + margin <= p.y + getHeight() && y - margin >= p.y) &&
		    (x + margin <= p.x + getWidth()  && x - margin >= p.x);
	}

	public boolean isNotCreated() {
		return !created;
	}
	
	long longestTime = 0;
	long timeAmount = 0;
	long timeAvg = 0;

	public void flipX() {
//		long fromTime = System.nanoTime();
//		
//		
//		var vs = mesh.getVertices();
//		int n = vs.length - 1;
//		for (int i = 0; i < n; i++) {
//			var oldPosX = vs[i].getPosition().x;
//			vs[i].getPosition().x = vs[n].getPosition().x;
//			vs[n].getPosition().x = oldPosX;
//			n--;
//		}
//		mesh.destroy();
//		mesh.create();
		var pos = new Vec3(position);
		var rot = new Vec3(rotation);
		position.x = 0;
		position.y = 0;
		position.z = 0;
		rotation.x = 0;
		rotation.y = 0;
		rotation.z = 0;
		updateTransformation();
		setRotationY(rot.y - 180);
		updateTransformation();
		setRotation(rot);
		super.setPosition(pos);
		updateTransformation();
//		
//		long toTime = System.nanoTime();
//		timeAvg += toTime - fromTime;
//		timeAmount++;
//		if (toTime - fromTime > longestTime) {
//			longestTime = toTime - fromTime;
//			System.out.println("time taken: " + longestTime);
//		} else {
//			System.out.println("time " + (toTime - fromTime) + " is below " + longestTime);
//		}
//		System.out.println("time avg: " + (timeAvg / timeAmount));
	}
}

