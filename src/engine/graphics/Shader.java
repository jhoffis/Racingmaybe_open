package engine.graphics;

import static org.lwjgl.opengl.GL20C.glGetAttribLocation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import engine.math.Vec2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import engine.math.Matrix4f;
import engine.math.Vec3;
import engine.utils.FileUtils;

public class Shader {

	public final String shaderName;
	private final Stack<IAction> uniforms;
	private final String vertexFile, fragmentFile;
	private int programID;
	private boolean destroyed;

	public Shader(String shaderName) {
		this.shaderName = shaderName;
		uniforms = new Stack<>();

		vertexFile = FileUtils
				.loadShaderAsString("/shaders/" + shaderName + "Vertex.glsl");
		fragmentFile = FileUtils
				.loadShaderAsString("/shaders/" + shaderName + "Fragment.glsl");
	}

	public void create() {
		if (programID != 0)
			return;
		
		programID = GL20.glCreateProgram();
		int vertexID = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);

//		System.out.println("Created shader: " + shaderName + " " + programID);
		
		GL20.glShaderSource(vertexID, vertexFile);
		GL20.glCompileShader(vertexID);

		// get shader info
		if (GL20.glGetShaderi(vertexID,
				GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.out.println(
					"Vertex shader: " + GL20.glGetShaderInfoLog(vertexID));
			return;
		}

		int fragmentID = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);

		GL20.glShaderSource(fragmentID, fragmentFile);
		GL20.glCompileShader(fragmentID);

		// get shader info
		if (GL20.glGetShaderi(fragmentID,
				GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
			System.out.println(
					"Fragment shader: " + GL20.glGetShaderInfoLog(fragmentID));
			return;
		}

		GL20.glAttachShader(programID, vertexID);
		GL20.glAttachShader(programID, fragmentID);

		GL20.glLinkProgram(programID);
		if (GL20.glGetProgrami(programID,
				GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
			System.out.println(
					"Program Linking: " + GL20.glGetProgramInfoLog(programID));
			return;
		}

		GL20.glValidateProgram(programID);
		if (GL20.glGetProgrami(programID,
				GL20.GL_VALIDATE_STATUS) == GL11.GL_FALSE) {
			System.out.println("Program Validation: "
					+ GL20.glGetProgramInfoLog(programID));
		}
		
		GL20.glDetachShader(programID, vertexID);
		GL20.glDetachShader(programID, fragmentID);
		GL20.glDeleteShader(vertexID);
		GL20.glDeleteShader(fragmentID);
	}

	public int getAttribLocation(String name) {
		return glGetAttribLocation(programID, name);
	}

	public int getUniformLocation(String name) {
		return GL20.glGetUniformLocation(programID, name);
	}

	public void setUniform(String name, float value) {
		uniforms.push(() -> GL20.glUniform1f(getUniformLocation(name), value));
	}

	public void setUniform(String name, int value) {
		uniforms.push(() -> GL20.glUniform1i(getUniformLocation(name), value));
	}

	public void setUniform(String name, boolean value) {
		uniforms.push(() -> GL20.glUniform1i(getUniformLocation(name),
				value ? 1 : 0));
	}

	public void setUniform(String name, Vec2 value) {
		uniforms.push(() -> GL20.glUniform2f(getUniformLocation(name),
				(float) value.x, (float) value.y));
	}

	public void setUniform(String name, Vec3 value) {
		uniforms.push(() -> GL20.glUniform3f(getUniformLocation(name),
				(float) value.x, (float) value.y, (float) value.z));
	}
	
	public void setUniform(String name, Vec3 vec3, float a) {
		uniforms.push(() -> GL20.glUniform4f(getUniformLocation(name),
				(float) vec3.x, (float) vec3.y, (float) vec3.z, a));
	}

	// Convert row major order to colomn major order
	public void setUniform(String name, Matrix4f value) {
		try (MemoryStack stack = MemoryStack.stackPush()) {
			GL20.glUniformMatrix4fv(getUniformLocation(name), true, stack.floats(value.getAll()));
		}
	}

	public void runUniform() {
		while (!uniforms.isEmpty())
			uniforms.pop().run();
	}

	public void bind() {
		GL20.glUseProgram(programID);
	}

	public void unbind() {
		GL20.glUseProgram(0);
	}

	public void destroy() {
		if (destroyed || programID == 0)
			return;
		GL20.glDeleteProgram(programID);
		destroyed = true;
	}

	public boolean isCreated() {
		return programID != 0;
	}

}
