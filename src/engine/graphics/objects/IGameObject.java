package engine.graphics.objects;

import engine.math.Matrix4f;
import engine.math.Vec3;

public interface IGameObject {

	Vec3 position();
	Vec3 rotation();
	void setRotation(Vec3 rotation);
	Matrix4f updateTransformation();
	void resetTransformation();
}
