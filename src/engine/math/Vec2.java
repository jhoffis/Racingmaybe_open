package engine.math;

public class Vec2 {

    public float x, y;

    public Vec2(float x, float y) {
    	this.x = x;
    	this.y = y;
    }

    public Vec2() {
        this(0, 0);
    }

    public Vec2(float xy) {
    	this(xy, xy);
    }

    public Vec2(Vec3 vec3, float modifier) {
        this(modifier * vec3.x, modifier * vec3.y);
    }

    public Vec2(Vec2 vec2, float modifier) {
    	this(modifier * vec2.x, modifier * vec2.y);
    }

    public Vec2 add(Vec2 v) {
        x += v.x;
        y += v.y;
        return this;
    }

    public Vec2 sub(Vec2 v) {
        x -= v.x;
        y -= v.y;
        return this;
    }

    public void add(float x, float y) {
        this.x += x;
        this.y += y;
    }

    public Vec2 mul(Vec2 v) {
        x *= v.x;
        y *= v.y;
        return this;
    }

    public Vec2 div(Vec2 v) {
        x /= v.x;
        y /= v.y;
        return this;
    }

    /**
     * pytagoras for vec3
     */
    public float length() {
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * Makes a vector the length of 1
     */
    public Vec2 normalize() {
        float len = length();
        x /= len;
        y /= len;
        return this;
    }

    /**
     * How much is a vector pointing with another vector If it's positive it's
     * pointing similarly to the other vector If it's 0 it's pointing
     * perpendicularly If it's negative it's pointing away from the other vector
     */
    public float dot(Vec2 v) {
        return x * v.x + y * v.y;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Vec2 other = (Vec2) obj;
        return Float.floatToIntBits(x) == Float.floatToIntBits(other.x) &&
                Float.floatToIntBits(y) == Float.floatToIntBits(other.y);
    }


    /*
     * Getters and setters
     */

    public float get(int index) {
        return switch (index) {
            case 0 -> {
                yield x;
            }
            case 1 -> {
                yield y;
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
        }
    }

    @Override
    public String toString() {
        return "Vec2{" +
                "x=" + x +
                ", y=" + y +
                '}' + super.toString();
    }
//
//	public float x() {
//		return xy[0];
//	}
//
//	public void setX(float x) {
//		this.xy[0] = x;
//	}
//
//	public float y() {
//		return xy[1];
//	}
//
//	public void setY(float y) {
//		this.xy[1] = y;
//	}

}
