package engine.math;

import java.util.Arrays;

public class Matrix4f {

	public static final int SIZE = 4;
	private final float[] elements = new float[SIZE * SIZE]; // Really a 2d array

	/**
	 * 1000 0100 0010 0001
	 */
	public static Matrix4f identity() {
		Matrix4f result = new Matrix4f();

		for (int i = 0; i < 4; i++) {
			result.set(i, i, 1);
		}

		return result;
	}

	/**
	 * 1000 0100 0010 0001
	 */
	public static void identityChange(Matrix4f mat) {
		for (int i = 0; i < mat.elements.length; i++) {
			mat.elements[i] = 0;
		}
		for (int i = 0; i < 4; i++) {
			mat.set(i, i, 1);
		}
	}

	/**
	 * 100x 010y 001z 0001
	 */
	public static Matrix4f translate(Vec3 translate) {
		Matrix4f result = Matrix4f.identity();

		for (int i = 0; i < 3; i++) {
			result.set(3, i, translate.get(i));
		}

		return result;
	}

	/**
	 * 100x 010y 001z 0001
	 */
	public static void translateChange(Matrix4f mat, float x, float y, float z) {
		mat.set(3, 0, x);
		mat.set(3, 1, y);
		mat.set(3, 2, z);
	}

	public static Matrix4f rotateX(float angle) {
		Matrix4f result = Matrix4f.identity();

		double cos = Math.cos(Math.toRadians(angle));
		double sin = Math.sin(Math.toRadians(angle));
		double arcCos = 1 - cos;

		result.set(0, 0, cos + arcCos);
		result.set(1, 1, cos);
		result.set(1, 2, -sin);
		result.set(2, 1, sin);
		result.set(2, 2, cos);

		return result;
	}

	/**
	 * @param angle is how much you want to rotate the object
	 * @param axis  how were gonna rotate it (x or y or z)
	 */
	public static Matrix4f rotateY(float angle) {
		Matrix4f result = Matrix4f.identity();

		double cos = Math.cos(Math.toRadians(angle));
		double sin = Math.sin(Math.toRadians(angle));
		double arcCos = 1 - cos;

		result.set(0, 0, cos);
		result.set(0, 2, sin);
		result.set(1, 1, cos + arcCos);
		result.set(2, 0, -sin);
		result.set(2, 2, cos);
		return result;
	}

	/**
	 * @param angle is how much you want to rotate the object
	 * @param axis  how were gonna rotate it (x or y or z)
	 */
	public static Matrix4f rotateZ(float angle) {
		Matrix4f result = Matrix4f.identity();

		double cos = Math.cos(Math.toRadians(angle));
		double sin = Math.sin(Math.toRadians(angle));
		double arcCos = 1 - cos;

		result.set(0, 0, cos);
		result.set(0, 1, -sin);
		result.set(1, 0, sin);
		result.set(1, 1, cos);
		result.set(2, 2, cos + arcCos);

		return result;
	}

	public static Matrix4f rotation(Vec3 rotation) {

		var rotXMatrix = Matrix4f.rotateX(rotation.x);
		var rotYMatrix = Matrix4f.rotateY(rotation.y);
		var rotZMatrix = Matrix4f.rotateZ(rotation.z);

		Matrix4f.multiplyChange(rotYMatrix, rotXMatrix);
		Matrix4f.multiplyChange(rotZMatrix, rotYMatrix);

		return rotZMatrix;
	}

	/**
	 * x000 0y00 00z0 0001
	 */
	public static Matrix4f scale(Vec3 scale) {
		Matrix4f result = Matrix4f.identity();

		for (int i = 0; i < 3; i++) {
			result.set(i, i, scale.get(i));
		}

		return result;
	}

	/**
	 * @return combination of the two matrices
	 */
	public static Matrix4f multiply(Matrix4f matrix, Matrix4f other) {
		Matrix4f result = Matrix4f.identity();

		for (int i = 0; i < SIZE; i++) {
			for (int j = 0; j < SIZE; j++) {
				result.set(i, j, matrix.get(i, 0) * other.get(0, j) + matrix.get(i, 1) * other.get(1, j)
						+ matrix.get(i, 2) * other.get(2, j) + matrix.get(i, 3) * other.get(3, j));
			}
		}

		return result;
	}

	public static void multiplyChange(Matrix4f matrix, Matrix4f other) {
		float[] result = new float[16];
		float[] elements = matrix.elements;
		float[] otherElements = other.elements;

		for (int i = 0; i < 4; i++) {
			float ai0 = elements[i];
			float ai1 = elements[i + 4];
			float ai2 = elements[i + 8];
			float ai3 = elements[i + 12];
			result[i] = ai0 * otherElements[0] + ai1 * otherElements[1] + ai2 * otherElements[2]
					+ ai3 * otherElements[3];
			result[i + 4] = ai0 * otherElements[4] + ai1 * otherElements[5] + ai2 * otherElements[6]
					+ ai3 * otherElements[7];
			result[i + 8] = ai0 * otherElements[8] + ai1 * otherElements[9] + ai2 * otherElements[10]
					+ ai3 * otherElements[11];
			result[i + 12] = ai0 * otherElements[12] + ai1 * otherElements[13] + ai2 * otherElements[14]
					+ ai3 * otherElements[15];
		}

		System.arraycopy(result, 0, elements, 0, 16);
	}

	public static Matrix4f transform(Vec3 position, Vec3 rotation, Vec3 scale) {
		var translationMatrix = Matrix4f.translate(position);
		var rotationMatrix = Matrix4f.rotation(rotation);
		var scaleMatrix = Matrix4f.scale(scale);
		return Matrix4f.multiply(translationMatrix, Matrix4f.multiply(rotationMatrix, scaleMatrix));
	}

	/**
	 * 1.0f 0.0f 0.0f 0.0f 0.0f 1.0f 0.0f 0.0f 0.0f 0.0f -((far + near) / range)
	 * -((2 * far * near) / range) 0.0f 0.0f -1.0f 0.0f
	 * 
	 * @param aspect width / height
	 */
	public static Matrix4f perspectiveProjection(float fov, float aspect, float near, float far) {
		Matrix4f result = Matrix4f.identity();

		double tanFOV = Math.tan(Math.toRadians(fov / 2));
		double range = far - near;

		result.set(0, 0, 1.0f / (aspect * tanFOV));
		result.set(1, 1, 1.0f / tanFOV);
		result.set(2, 2, -((far + near) / range));
		result.set(2, 3, -1.0f);
		result.set(3, 2, -((2 * far * near) / range));
		result.set(3, 3, 0.0f);

		return result;
	}

	public static Matrix4f orthographicProjection(float top, float bottom, float right, float left, float near,
			float far) {
		Matrix4f result = Matrix4f.identity();

		result.set(0, 0, 2.0f / (right - left));
		result.set(1, 1, 2.0f / (top - bottom));
		result.set(2, 2, 2.0f / (near - far));
		result.set(0, 3, (left + right) / (left - right));
		result.set(1, 3, (bottom + top) / (bottom - top));
		result.set(2, 3, (far + near) / (far - near));

		return result;
	}

	public static void view(Matrix4f view, Vec3 position, Vec3 rotation) {
		Matrix4f.identityChange(view);
		Matrix4f.translateChange(view, -1f * position.x, -1f * position.y, -1f * position.z);
		Matrix4f.multiplyChange(view, Matrix4f.rotation(rotation));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(elements);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Matrix4f other = (Matrix4f) obj;
		if (!Arrays.equals(elements, other.elements))
			return false;
		return true;
	}

	public float get(int x, int y) {
		return elements[y * SIZE + x];
	}

	public void set(int x, int y, double value) {
		set(x, y, (float) value);
	}

	public void set(int x, int y, float value) {
		elements[y * SIZE + x] = value;
	}

	public float[] getAll() {
		return elements;
	}

}
