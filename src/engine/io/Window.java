package engine.io;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_DECORATED;
import static org.lwjgl.glfw.GLFW.GLFW_DONT_CARE;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_DEBUG_CONTEXT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetMonitorPos;
import static org.lwjgl.glfw.GLFW.glfwGetMonitors;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetWindowIcon;
import static org.lwjgl.glfw.GLFW.glfwSetWindowMonitor;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.ARBDebugOutput.GL_DEBUG_SEVERITY_LOW_ARB;
import static org.lwjgl.opengl.ARBDebugOutput.GL_DEBUG_SOURCE_API_ARB;
import static org.lwjgl.opengl.ARBDebugOutput.GL_DEBUG_TYPE_OTHER_ARB;
import static org.lwjgl.opengl.ARBDebugOutput.glDebugMessageControlARB;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;
import org.lwjgl.opengl.GL43;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.KHRDebug;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Platform;

import engine.graphics.ui.CursorType;
import engine.graphics.ui.Font;
import engine.graphics.ui.UIFont;
import main.Main;
import main.Texts;
import scenes.SceneHandler;
import settings_and_logging.RSet;
import utils.FileValidator;

public class Window {

	public static int WIDTH, HEIGHT, OG_WIDTH, OG_HEIGHT;
	private int client_width, client_height;

	public static UIFont titleFont, hugeTitleFont;

	private final long updatingWindowTimeAdd = Main.DEBUG ? 0 : 500;
	public long updatingWindowTime;
	
	// private Action closingProtocol;
	private SceneHandler sceneHandler;
	private boolean updateViewport;
	private int fullscreen = -1;
	private long window, monitor;
	private boolean previousMouseState;
	private long cursorNormal, cursorCanPoint, cursorIsPoint, cursorCanHold, cursorIsHold;
	private CursorType cursorTypeSelected;
	private boolean focused;
	private int currentMonitorN;

	public Window() {
	}

	public void init() {

		if (!glfwInit()) {
			throw new IllegalStateException("Unable to initialize glfw");
		}

		long beforeGraphicsDev = System.currentTimeMillis();
		// New
		
		long primaryMonitor = RSet.settings.getLong(RSet.lastMonitor.ordinal());
		var monitors = glfwGetMonitors();
		if (primaryMonitor == 0 || !RSet.settings.getBool(RSet.clientFullscreen.ordinal())) {
			primaryMonitor = glfwGetPrimaryMonitor();
			RSet.set(RSet.lastMonitor, 0);
		} else {
			int n = 0;
			while (true) {
				try {
					long realMonitor = monitors.get();
					if (n == primaryMonitor) {
						primaryMonitor = realMonitor;
						break;
					}
				} catch (BufferUnderflowException e) {
					primaryMonitor = glfwGetPrimaryMonitor();
					RSet.set(RSet.lastMonitor, 0);
					break;
				}
				n++;
			}
		}
		GLFWVidMode mode = glfwGetVideoMode(primaryMonitor);
		int currWidth = mode.width();
		// Set client size to one resolution lower than the current one

		// Old
//		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment()
//				.getDefaultScreenDevice();
//		int currWidth = gd.getDisplayMode().getWidth();

		updateWithinWindow(currWidth, mode.height());

		long afterGraphicsDev = System.currentTimeMillis();
		System.out.println("Graphics time: " + (afterGraphicsDev - beforeGraphicsDev) + "ms");

		GLFWErrorCallback.createPrint().set();

		long beforeHints = System.currentTimeMillis();

//		glfwDefaultWindowHints();
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
		glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
		glfwWindowHint(GLFW_DECORATED, GLFW_FALSE);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
		glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
		glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
		if (Platform.get() == Platform.MACOSX) {
			glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
		}
		glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, Main.DEBUG ? GLFW_TRUE : GLFW_FALSE);

		long afterHints = System.currentTimeMillis();
		System.out.println("Hints time: " + (afterHints - beforeHints) + "ms");

		window = glfwCreateWindow(client_width, client_height, "Racingmaybe", NULL, NULL);
		if (window == NULL)
			throw new RuntimeException("Failed to create the glfw window");

		long afterWindow = System.currentTimeMillis();
		System.out.println("Creation of Window time: " + (afterWindow - afterHints) + "ms");

		GLFW.glfwSetWindowFocusCallback(window, (window, focused) -> {
			this.focused = focused;
		});

		FileValidator.throwErrorOnMissingDir("./images");

		// ICON
		setIcon("./images/icon.png");

		// Cursor
		cursorNormal = createCursor("./images/cursor.png", 0);
		float xPercentCursorHand = 0.27f;
		cursorCanPoint = createCursor("./images/cursorCanPoint.png", xPercentCursorHand);
		cursorIsPoint = createCursor("./images/cursorIsPoint.png", xPercentCursorHand);
		cursorCanHold = createCursor("./images/cursorCanHold.png", xPercentCursorHand);
		cursorIsHold = createCursor("./images/cursorIsHold.png", xPercentCursorHand);
		setCursor(CursorType.cursorNormal);

		// Get the thread stack and push a new frame
		try (MemoryStack stack = stackPush()) {
			IntBuffer pWidth = stack.mallocInt(1);
			IntBuffer pHeight = stack.mallocInt(1);

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);
		}

		// center
		setFullscreen(RSet.settings.getInt(RSet.clientFullscreen.ordinal()),
				primaryMonitor);

		long beforeOpenGL = System.currentTimeMillis();
		// Make the OpenGL context current
		glfwMakeContextCurrent(window);

		// Opengl
		GLCapabilities caps = GL.createCapabilities();

		long afterOpenGL = System.currentTimeMillis();
		System.out.println("OpenGL time: " + (afterOpenGL - beforeOpenGL) + "ms");

		if (caps.OpenGL43) {
			GL43.glDebugMessageControl(GL43.GL_DEBUG_SOURCE_API, GL43.GL_DEBUG_TYPE_OTHER,
					GL43.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
		} else if (caps.GL_KHR_debug) {
			KHRDebug.glDebugMessageControl(KHRDebug.GL_DEBUG_SOURCE_API, KHRDebug.GL_DEBUG_TYPE_OTHER,
					KHRDebug.GL_DEBUG_SEVERITY_NOTIFICATION, (IntBuffer) null, false);
		} else if (caps.GL_ARB_debug_output) {
			glDebugMessageControlARB(GL_DEBUG_SOURCE_API_ARB, GL_DEBUG_TYPE_OTHER_ARB, GL_DEBUG_SEVERITY_LOW_ARB,
					(IntBuffer) null, false);
		}

		updateViewport = true;

		System.out.println("WIDTH: " + WIDTH + ", HEIGHT: " + HEIGHT);

		titleFont = new UIFont(Font.BOLD_ITALIC, 48 * HEIGHT / 1080);
		hugeTitleFont = new UIFont(Font.BOLD_REGULAR, 182 * HEIGHT / 1080);
	}

	public void updateWithinWindow(int width, int height) {
		
		client_width = (int) (width / 1.25f);

		final int incVal = 64;
		int foundIndex;
		int i = 1;
		do {
			foundIndex = incVal * i;
			if (client_width <= foundIndex)
				break;
			i++;
		} while (true);

		client_width = foundIndex;

		client_height = (int) ((float) client_width * ((float) height / (float) width));

		WIDTH = client_width;
		HEIGHT = client_height;

		if (sceneHandler != null)
			sceneHandler.updateResolution();
		
		delayRendering();
	}

	public void setCursor(CursorType cursor) {
		if (cursorTypeSelected != null && cursorTypeSelected == cursor)
			return;

		cursorTypeSelected = cursor;

		GLFW.glfwSetCursor(window, switch (cursor) {
		case cursorCanHold:
			yield this.cursorCanHold;
		case cursorCanPoint:
			yield this.cursorCanPoint;
		case cursorIsHold:
			yield this.cursorIsHold;
		case cursorIsPoint:
			yield this.cursorIsPoint;
		case cursorNormal:
			yield this.cursorNormal;
		});
	}

	private long createCursor(String path, float xPercent) {
		// STBImage.stbi_set_flip_vertically_on_load(false);
		int[] widthBuffer = new int[1];
		int[] heightBuffer = new int[1];
		int[] channelsBuffer = new int[1];

		ByteBuffer data = STBImage.stbi_load(path, widthBuffer, heightBuffer, channelsBuffer, 4);
		GLFWImage cursor = GLFWImage.malloc();
		cursor.set(widthBuffer[0], heightBuffer[0], data);
		return GLFW.glfwCreateCursor(cursor, (int) (cursor.width() * xPercent), 0);
	}

	public void update() {
		glfwPollEvents();
		GL40.glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL20.GL_STENCIL_BUFFER_BIT); // Clear the
		// framebuffer
	}

	public void swapBuffers() {
		glfwSwapBuffers(window);
	}
	
	public boolean setFullscreen() {
		return setFullscreen(fullscreen);
	}

	/**
	 * changes state of the window
	 */
	public boolean setFullscreen(int fullscreen) {
		long monitor = getCurrentMonitor();

		if (this.monitor == monitor && this.fullscreen == fullscreen)
			return false;
		return setFullscreen(fullscreen, monitor);
	}
	
	public void delayRendering() {
		updatingWindowTime = System.currentTimeMillis() + updatingWindowTimeAdd;
	}

	private boolean setFullscreen(int fullscreen, long monitor) {
		int width = 0;
		int height = 0;
		GLFWVidMode vidmode = null;
		vidmode = glfwGetVideoMode(monitor);
		int heightDiff = 0;
		long alteredMonitor = monitor;
		if (fullscreen != 0) {
			// switch to fullscreen
			if (fullscreen == 1) {
				alteredMonitor = NULL;
			}
			
			// set width based on the right monitor
			width = vidmode.width();
			height = vidmode.height();
		} else {
			// switch to windowed
			updateWithinWindow(vidmode.width(), vidmode.height());
			getCurrentMonitor();
			width = client_width;   // TODO Start in the same ratio as this monitor? And send a message if you see that the ratio is different that you should restart the game? 
			height = client_height;
			alteredMonitor = NULL;
		}
		
		if (this.monitor == monitor && this.fullscreen == fullscreen && client_width == width && client_height == height)
			return false;

		glfwWindowHint(GLFW.GLFW_RED_BITS, vidmode.redBits());
		glfwWindowHint(GLFW.GLFW_GREEN_BITS, vidmode.greenBits());
		glfwWindowHint(GLFW.GLFW_BLUE_BITS, vidmode.blueBits());
		
		this.monitor = monitor;
		monitor = alteredMonitor;
		this.fullscreen = fullscreen;
		
		if (OG_WIDTH == 0) {
			OG_WIDTH = width;
			OG_HEIGHT = height;
		} else if ((float) OG_WIDTH / (float) OG_HEIGHT != (float) width / (float) height) {
			SceneHandler.showMessage(Texts.restartWindow);
		}
		
		client_width = WIDTH = width;
		client_height = HEIGHT = height;

		IntBuffer xb = BufferUtils.createIntBuffer(1);
		IntBuffer yb = BufferUtils.createIntBuffer(1);
		GLFW.glfwGetMonitorPos(this.monitor, xb, yb);

		int x = xb.get();
		int y = yb.get();

		glfwSetWindowMonitor(window, monitor, x, y - heightDiff, width, height,
				monitor == NULL ? GLFW_DONT_CARE : vidmode.refreshRate());
		

		// if windowed
		if (fullscreen == 0 && monitor == NULL && x == 0 && y == 0) {
			glfwSetWindowPos(window, (vidmode.width() - width) / 2, (vidmode.height() - height) / 2);
		}

		// move drawing of graphics to the right place
		updateViewport = true;

		delayRendering();
		return true;
	}

	/**
	 * Determines the current monitor that the specified window is being displayed
	 * on. If the monitor could not be determined, the primary monitor will be
	 * returned.
	 *
	 * @return The current monitor on which the window is being displayed, or the
	 *         primary monitor if one could not be determined
	 * @author <a href="https://stackoverflow.com/a/31526753/2398263">Shmo</a><br>
	 *         Ported to LWJGL by Brian_Entei edited by Jhoffis
	 */
	private long getCurrentMonitor() {
		int[] wx = { 0 }, wy = { 0 }, ww = { 0 }, wh = { 0 };
		int[] mx = { 0 }, my = { 0 }, mw = { 0 }, mh = { 0 };
		int overlap, bestoverlap;
		long bestmonitor;
		PointerBuffer monitors;
		GLFWVidMode mode;

		bestoverlap = 0;
		bestmonitor = monitor;

		glfwGetWindowPos(window, wx, wy);
		glfwGetWindowSize(window, ww, wh);
		monitors = glfwGetMonitors();

		int n = 0;
		int bestN = 0;
		while (monitors.hasRemaining()) {
			long monitor = monitors.get();
			mode = glfwGetVideoMode(monitor);
			glfwGetMonitorPos(monitor, mx, my);
			mw[0] = mode.width();
			mh[0] = mode.height();

			overlap = Math.max(0, Math.min(wx[0] + ww[0], mx[0] + mw[0]) - Math.max(wx[0], mx[0]))
					* Math.max(0, Math.min(wy[0] + wh[0], my[0] + mh[0]) - Math.max(wy[0], my[0]));

			if (bestoverlap < overlap) {
				bestoverlap = overlap;
				bestmonitor = monitor;
				bestN = n;
			}
			n++;
		}

		if (bestN != currentMonitorN && Main.running) {
			RSet.set(RSet.lastMonitor, bestN);
			currentMonitorN = bestN;
		}

		return bestmonitor;
	}

	public void mouseStateHide(boolean lock) {
		if (InputHandler.CONTROLLER_EFFECTIVELY)
			lock = true;
		previousMouseState = GLFW.glfwGetInputMode(window, GLFW_CURSOR) == GLFW_CURSOR_DISABLED;
		glfwSetInputMode(window, GLFW_CURSOR, lock ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
	}

	public void mouseStateToPrevious() {
		mouseStateHide(previousMouseState);
	}

	public long getWindow() {
		return window;
	}

	public boolean isFullscreen() {
		return fullscreen != 0;
	}

	public boolean shouldUpdateViewport() {
		return updateViewport;
	}

	public void updateViewport() {
//		IntBuffer width = IntBuffer.allocate(1);
//		IntBuffer height = IntBuffer.allocate(1);
//		GLFW.glfwGetFramebufferSize(window, width, height);
		glfwMakeContextCurrent(window);
		GL20.glViewport(0, 0, WIDTH, HEIGHT);
		updateViewport = false;
	}

	public void destroy() {
		// if (closingProtocol != null)
		// closingProtocol.run();
		glfwDestroyWindow(window);
		GLFW.glfwDestroyCursor(cursorNormal);
		GLFW.glfwDestroyCursor(cursorCanPoint);
		GLFW.glfwDestroyCursor(cursorIsPoint);
		GLFW.glfwDestroyCursor(cursorCanHold);
		GLFW.glfwDestroyCursor(cursorIsHold);
	}

	public boolean isClosing() {
		return glfwWindowShouldClose(window);
	}

	public void setSceneHandler(SceneHandler sceneHandler) {
		this.sceneHandler = sceneHandler;
	}

	public boolean isFocused() {
		return focused;
	}

	public void setIcon(String path) {
		GLFWImage.Buffer icons = GLFWImage.malloc(1);
		// STBImage.stbi_set_flip_vertically_on_load(false);
		int[] widthBuffer = new int[1];
		int[] heightBuffer = new int[1];
		int[] channelsBuffer = new int[1];

		ByteBuffer data = STBImage.stbi_load(path, widthBuffer, heightBuffer, channelsBuffer, 4);
		GLFWImage icon = GLFWImage.malloc();
		icon.set(widthBuffer[0], heightBuffer[0], data);
		icons.put(0, icon);
		icon.free();

		glfwSetWindowIcon(window, icons);
		icons.free();
	}
}
