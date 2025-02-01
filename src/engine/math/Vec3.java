package engine.math;

import java.awt.Color;

public class Vec3 extends Vec2 {

	public float z;

	public Vec3(Vec3 vec3, float modifier) {
		super(vec3, modifier);
		z = modifier * vec3.z;
	}

	public Vec3(Vec3 position) {
		this(position.x, position.y, position.z);
	}
	
	public Vec3(Vec2 position, float z) {
		this(position.x, position.y, z);
	}

	public Vec3(Vec2 position) {
		this(position.x, position.y, 0);
	}
	
	public Vec3(Color c) {
		this((float) c.getRed() / 255f, (float) c.getGreen() / 255f, (float) c.getBlue() / 255f);
	}

	public Vec3(float xyz) {
		this(xyz, xyz, xyz);
	}

	public Vec3(float x, float y, float z) {
		super(x, y);
		this.z = z;
	}

	public Vec3(float r, float g, float b, boolean by255) {
		this(r * (by255 ? 1f / 255f : 1f), g * (by255 ? 1f / 255f : 1f), b * (by255 ? 1f / 255f : 1f));
	}

	public Vec3 add(Vec3 v) {
		x += v.x;
		y +=v.y;
		z +=v.z;
		return this;
	}

	public Vec3 sub(Vec3 v) {
		x += v.x;
		y +=v.y;
		z +=v.z;
		return this;
	}

	public Vec3 mul(Vec3 v) {
		x *= v.x;
		y *=v.y;
		z *=v.z;
		return this;
	}
	
	public Vec3 mul(float value) {
		x *= value;
		y *= value;
		z *= value;
		return this;
	}

	public Vec3 div(Vec3 v) {
		x /= v.x;
		y /=v.y;
		z /=v.z;
		return this;
	}

	public Vec3 div(float value) {
		x /= value;
		y /= value;
		z /= value;
		return this;
	}


	/**
	 * pytagoras for vec3
	 */
	public float length() {
		return (float) Math.sqrt(x * x + y * y + z * z);
	}

	/**
	 * Makes a vector the length of 1
	 */
	public Vec3 normalize() {
		return div(length());
	}

	/**
	 * How much is a vector pointing with another vector If it's positive it's
	 * pointing similarly to the other vector If it's 0 it's pointing
	 * perpendicularly If it's negative it's pointing away from the other vector
	 */
	public float dot(Vec3 v) {
		return x * v.x + y * v.y + z * v.z;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Vec3 other = (Vec3) obj;
		if (Float.floatToIntBits(x) != Float.floatToIntBits(other.x) ||
				Float.floatToIntBits(y) != Float.floatToIntBits(other.y) ||
				Float.floatToIntBits(z) != Float.floatToIntBits(other.z)) {
			return false;
		}

		return true;
	}
	
	@Override
	public String toString() {
		return "x: " + x + ", y: " + y + ", z: " + z;
	}

	/*
	 * Getters and setters
	 */

	public float get(int index) {
		if(index < 0 || index > 2) {
			System.out.println("Out of range Vec3");
			return -1;
		}
		return switch(index) {
			case 0 -> {
				yield x;
			}
			case 1 -> {
				yield y;
			}
			case 2 -> {
				yield z;
			}
			default -> throw new IllegalArgumentException("Unexpected value: " + index);
		};
	}

	public void set(int index, float value) {
		switch (index) {
			case 0:
				x = value;
				break;
			case 1:
				y = value;
				break;
			case 2:
				z = value;
				break;
		}
	}

	public void set(int value) {
		x = value;
		y = value;
		z = value;
	}
	
	public void invert() {
		x = -x;
		y = -y;
		z = -z;
	}

}