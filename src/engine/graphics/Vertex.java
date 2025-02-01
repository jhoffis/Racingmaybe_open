package engine.graphics;

import engine.math.Vec2;
import engine.math.Vec3;

public class Vertex {

	private Vec3 position, normal;
	private Vec2 textureCoord;
	
	
	public Vertex(Vec3 position, Vec2 textureCoord) {
		this(position, null, textureCoord);
	}

	public Vertex(Vec3 position, Vec3 normal, Vec2 textureCoord) {
		this.position = position;
		this.textureCoord =textureCoord;
		this.normal = normal;
	}

	public Vec3 getPosition() {
		return position;
	}

	public Vec2 getTextureCoord() {
		return textureCoord;
	}

	public Vec3 getNormal() {
		return normal;
	}
	
}
