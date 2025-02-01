package engine.graphics.objects;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import engine.graphics.Texture;
import engine.graphics.Vertex;
import engine.math.Vec3;

public class Mesh {

	private Vec3 positionAvgOg, size;
	private Vertex[] vertices;
	private int[] indices;
	private Texture texture;
	// vao (vertex array object), pbo ((vertex) position buffer object), ibo
	// (indices buffer object), cbo (color buffer object) is the buffers of a vertex
	// to be sent to the gpu.
	private int vao, pbo, ibo, tbo;

	public Mesh(Vertex[] vertices, int[] indices, Texture texture) {
		this.vertices = vertices;
		this.indices = indices;
		this.texture = texture;
	}

	public void create() {
		texture.create();

		vao = GL30.glGenVertexArrays();
		GL30.glBindVertexArray(vao);

		// make data readable by opengl
		positionAvgOg = new Vec3(0);
		
		// close is one point that is furthest away from far.
		float closeX = 0, closeY = 0, closeZ = 0, farX = 0, farY = 0, farZ = 0;
		FloatBuffer positionBuffer = MemoryUtil.memAllocFloat(vertices.length * 3);
		float[] positionData = new float[vertices.length * 3];
		for (int i = 0; i < vertices.length; i++) {
			float x = vertices[i].getPosition().x,
					y = vertices[i].getPosition().y,
					z = vertices[i].getPosition().z;
			
			positionData[i * 3] = x;
			positionData[i * 3 + 1] = y;
			positionData[i * 3 + 2] = z;
			
			// finn tall som er lengst unna 0 og lengst unna det tallet.
			if (Math.abs(farX) < Math.abs(x)) {
				farX = x;
			} else if (closeX == 0 || Math.abs(farX - x) > Math.abs(farX - closeX) ) {
				closeX = x;
			}
			if (Math.abs(farY) < Math.abs(y)) {
				farY = y;
			} else if (closeY == 0 || Math.abs(farY - y) > Math.abs(farY - closeY) ) {
				closeY = y;
			}
			if (Math.abs(farZ) < Math.abs(z)) {
				farZ = z;
			} else if (closeZ == 0 || Math.abs(farZ - z) > Math.abs(farZ - closeZ) ) {
				closeZ = z;
			}
		}
		
		//Kanskje den misforst�r om y verdien den g�r ut i fra er over 0? og s� leiter den etter den som er n�rmest 0 og ikke 
		
		//fjern luftrommet fra origo til objektet for � f� st�rrelsen.
		size = new Vec3(Math.abs(farX - closeX),
						Math.abs(farY - closeY),
						Math.abs(farZ - closeZ));
		
		// finn midtpunktet av objektet ved � g� tilbake 1 radian.
		positionAvgOg.x = (farX >= 0 ? 1f : -1f) * (Math.abs(farX) - (size.x / 2f));
		positionAvgOg.y = (farY >= 0 ? 1f : -1f) * (Math.abs(farY) - (size.y / 2f));
		positionAvgOg.z = (farZ >= 0 ? 1f : -1f) * (Math.abs(farZ) - (size.z / 2f));

		// Put data into buffer
		positionBuffer.put(positionData).flip();

		pbo = storeData(positionBuffer, 0, 3);

//		if (vertices[0].getColor() != null) {
//			// make data readable by opengl
//			FloatBuffer colorBuffer = MemoryUtil.memAllocFloat(vertices.length * 3);
//			float[] colorData = new float[vertices.length * 3];
//			for (int i = 0; i < vertices.length; i++) {
//				colorData[i * 3] = new Random().nextInt(100) / 100f;
//				colorData[i * 3 + 1] = new Random().nextInt(100) / 100f;
//				colorData[i * 3 + 2] = new Random().nextInt(100) / 100f;
//			}
//			// Put data into buffer
//			colorBuffer.put(colorData).flip();
//
//			cbo = storeData(colorBuffer, 1, 3);
//			// make data readable by opengl
//		}
		
		tbo = storeData(texture.createTextureBuffer(vertices), 1, 2);

		IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
		indicesBuffer.put(indices).flip();

		ibo = GL15.glGenBuffers();
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
		GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);

	}

	/**
	 * @return what the buffer id is
	 */
	private int storeData(FloatBuffer buffer, int index, int size) {
		// Create a opengl buffer
		int bufferID = GL15.glGenBuffers();

		// Bind our buffer
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, bufferID);
		GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
		GL20.glVertexAttribPointer(index, size, GL11.GL_FLOAT, false, 0, 0);

		// Unbind
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);

		return bufferID;
	}
	
	public void bind() {
		GL30.glBindVertexArray(vao);
		
		// Position
		GL30.glEnableVertexAttribArray(0);
		// texture coord
		GL30.glEnableVertexAttribArray(1);
		
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, ibo);
				
	}

	public void unbind() {
//		GL30.glUnmapBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER);
		GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0);
		GL30.glDisableVertexAttribArray(0);
		GL30.glDisableVertexAttribArray(1);
		GL30.glBindVertexArray(0);		
	}

	public void destroy() {
		GL15.glDeleteBuffers(pbo);
		GL15.glDeleteBuffers(ibo);
		GL15.glDeleteBuffers(tbo);

		GL30.glDeleteVertexArrays(vao);
		texture.destroy();
	}

	public Vertex[] getVertices() {
		return vertices;
	}

	public int[] getIndices() {
		return indices;
	}

	public int getVAO() {
		return vao;
	}

	public int getPBO() {
		return pbo;
	}

	public int getIBO() {
		return ibo;
	}

	public int getTBO() {
		return tbo;
	}

	public Texture getTexture() {
		return texture;
	}

	public Vec3 getPositionAvgOg() {
		return positionAvgOg;
	}

	public Vec3 getSize() {
		return size;
	}

}
