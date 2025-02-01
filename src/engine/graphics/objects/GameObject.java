package engine.graphics.objects;

import engine.graphics.Shader;
import engine.math.Matrix4f;
import engine.math.Vec3;

public class GameObject implements IGameObject {

	private Matrix4f actualTransformation, tempTransformation;
	private GameObject owner; // roterer og translater av position og rotation bare.
	protected Vec3 position, rotation, scale, indiePosition, indieRotation;
	public Mesh mesh;
	protected Shader shader;

	protected GameObject() {}
	
	public GameObject(Mesh mesh, Shader shader) {
		init(new Vec3(0), new Vec3(0), new Vec3(1), mesh, shader);
	}

	public GameObject(Vec3 position, Vec3 rotation, Vec3 scale, Mesh mesh, Shader shader) {
		init(position, rotation, scale, mesh, shader);
	}
	
	protected void init(Vec3 position, Vec3 rotation, Vec3 scale, Mesh mesh, Shader shader) {
		this.position = position;
		indiePosition = new Vec3(0);
		this.rotation = rotation;
		indieRotation = new Vec3(0);
		this.scale = scale;
		this.mesh = mesh;
		if (this.shader == null || !this.shader.isCreated())
			this.shader = shader;
		tempTransformation = Matrix4f.identity();
	}
	
	public Mesh getMesh() {
		return mesh;
	}
//
//	public void setMeshes(Mesh[] meshes) {
//		this.meshes = meshes;
//	}

	public Shader getShader() {
		return shader;
	}

	public void setShader(Shader shader) {
		this.shader = shader;
	}

	public Vec3 rotation() {
		return rotation;
	}
	
	public void setRotation(Vec3 rotation) {
		this.rotation = rotation;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.rotation(rotation));
	}
	
	public void updateRotation() {
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.rotation(rotation));
	}

	public Vec3 getScale() {
		return scale;
	}

	public void setScale(Vec3 scale) {
		this.scale = scale;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.scale(scale));
	}
	
	public void scale(float f) {
		scale.mul(f);
	}
	
	public Vec3 position() {
		return position;
	}

	public void setPosition(Vec3 position) {
		this.position = position;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.translate(position));
	}
	
	// Moves NOT TO but IN ADDITION to the currentposition in the transformation
	public void updatePosition() {
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.translate(position));
	}
	
	public void setIndiePositionX(float x) {
		indiePosition.x = x;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.translate(new Vec3(x, 0, 0)));
	}
	
	public void setPositionX(float x) {
		position.x = x;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.translate(new Vec3(x, 0, 0)));
	}
	
	public void setPositionY(float y) {
		position.y = y;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.translate(new Vec3(0, y, 0)));
	}
	
	public void setPositionZ(float z) {
		position.z = z;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.translate(new Vec3(0, 0, z)));
	}

	public void setRotationX(float x) {
		rotation.x = x;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.rotation(new Vec3(x, 0, 0)));
	}

	public void setRotationY(float y) {
		rotation.y = y;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.rotation(new Vec3(0, y, 0)));
	}
	
	// 360 is a whole circle
	public void setRotationZ(float z) {
		rotation.z = z;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.rotation(new Vec3(0, 0, z)));
	}
	
	// 360 is a whole circle
	public void setIndieRotationZ(float z) {
		indieRotation.z = z;
		Matrix4f.multiplyChange(tempTransformation, Matrix4f.rotation(new Vec3(0, 0, z)));
	}

	public Matrix4f getTransformation() {
		if(owner != null) {
			var tempTransformation = Matrix4f.multiply(this.tempTransformation, Matrix4f.translate(owner.position()));
			Matrix4f.multiplyChange(tempTransformation, Matrix4f.rotation(owner.rotation()));

			updateTransformation();
			Matrix4f.multiplyChange(actualTransformation, tempTransformation);
		}
		
		return (actualTransformation != null ? actualTransformation : updateTransformation());
	}
	
	/**
	 * Updates the temp matrix and combines it into the actual one.
	 * Translation, rotation and scaling happens seperately otherwise.
	 */
	public Matrix4f updateTransformation() {
		if(actualTransformation != null)
			Matrix4f.multiplyChange(actualTransformation, tempTransformation);
		else
			actualTransformation = Matrix4f.transform(position, rotation, scale);
		Matrix4f.identityChange(tempTransformation);
		
		return actualTransformation;
	}
	
	public void resetTransformation() {
		actualTransformation = null;
		Matrix4f.identityChange(tempTransformation);
	}

	public GameObject getOwner() {
		return owner;
	}

	public GameObject setOwner(GameObject owner) {
		this.owner = owner;
		return this;
	}

	public void destroy() {
		if (mesh != null)
			mesh.destroy();
		if(shader != null)
			shader.destroy();
	}


}
