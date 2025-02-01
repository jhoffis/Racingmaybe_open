package engine.graphics;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_REPEAT;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_BGRA;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL40;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryUtil;

import engine.utils.BufferUtils;

public class Texture {

	private int width, height, textureID;
	private ByteBuffer pixels;

	public Texture(String path) {
		int[] widthBuffer = new int[1];
		int[] heightBuffer = new int[1];
		int[] channelsBuffer = new int[1];
		
		ByteBuffer data = STBImage.stbi_load(path, widthBuffer, heightBuffer, channelsBuffer, 4);
		this.width = widthBuffer[0];
		this.height = heightBuffer[0];
		this.pixels = data;
		
		if(data == null)
		{
			System.out.println("Could not find texture at path: " + path);
			StackTraceElement[] elements = Thread.currentThread().getStackTrace();
			for (int i = 1; i < elements.length; i++) {
				StackTraceElement s = elements[i];
				System.out.println("\tat " + s.getClassName() + "." + s.getMethodName()
		        + "(" + s.getFileName() + ":" + s.getLineNumber() + ")");
			}
		}
	}

	public void create() {
		textureID = glGenTextures();

		glBindTexture(GL_TEXTURE_2D, textureID);

		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
		
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
		
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE,
				pixels);

		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public FloatBuffer createTextureBuffer(Vertex[] vertices) {
		FloatBuffer textureBuffer = MemoryUtil.memAllocFloat(vertices.length * 2);
		float[] textureData = new float[vertices.length * 2];
		for (int i = 0; i < vertices.length; i++) {
			textureData[i * 2] = (float) vertices[i].getTextureCoord().x;
			textureData[i * 2 + 1] = (float) vertices[i].getTextureCoord().y;
		}
		// Put data into buffer
		textureBuffer.put(textureData).flip();
		return textureBuffer;
	}

	public void bind(int activeTexture) {
		GL13.glActiveTexture(activeTexture);
		glBindTexture(GL_TEXTURE_2D, textureID);
	}

	public void unbind() {
		glBindTexture(GL_TEXTURE_2D, 0);
	}

	public void destroy() {
		GL13.glDeleteTextures(textureID);
	}

	public float widthHeightRatio() {
		return (float) width / (float) height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getTextureID() {
		return textureID;
	}

}
