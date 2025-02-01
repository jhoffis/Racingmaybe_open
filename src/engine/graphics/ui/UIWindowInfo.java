package engine.graphics.ui;

import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_rect;
import static org.lwjgl.nuklear.Nuklear.nk_style_pop_vec2;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_vec2;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import engine.io.Window;

/**
 * Contains position and size of window and also its name.
 * 
 * @author jhoffis
 *
 */
public class UIWindowInfo {

	public static final int OPTIONS_STANDARD = Nuklear.NK_WINDOW_NO_INPUT | Nuklear.NK_WINDOW_NO_SCROLLBAR;
	public String name;
	public float x, y, width, height;
	public int z, options;
	public boolean focus, visible;
	private final NkRect rect;
	private float xBased, yBased, wBased, hBased;
	
	public UIWindowInfo(String name, int options, float x, float y, float w, float h) {
		this.name = name;
		this.options = options;
		rect = NkRect.create();
		setPositionSize(x, y, w, h);
		visible = true;
	}

	public void setPositionSize(float x, float y, float w, float h) {
		xBased = x / Window.WIDTH;
		yBased = y / Window.HEIGHT;
		wBased = w / Window.WIDTH; 
		hBased = h / Window.HEIGHT;
		
		updateResolution();
	}
	
	public void setPosition(float x, float y) {
		xBased = x / Window.WIDTH;
		yBased = y / Window.HEIGHT;
		
		updateResolution();
	}
	

	public void setHeight(float h) {
		hBased = h / Window.HEIGHT;
		updateResolution();
	}

	public void setX(float x) {
		xBased = x / Window.WIDTH;
		updateResolution();
	}

	public void addY(float y) {
		yBased = (this.y + y) / (float) Window.HEIGHT;
		updateResolution();
	}

	public void updateResolution() {
		this.x = xCalc();
		this.y = yCalc();
		this.width = wCalc();
		this.height = hCalc();
		
		nk_rect(x, y, width, height, rect);
	}
	
	public boolean begin(NkContext ctx) {
		if (!visible)
			return false;
		if (focus && UISceneInfo.isHighestWindow(z)) {
			Nuklear.nk_window_set_focus(ctx, name);
		}
		return nk_begin(ctx, name, rect, options);
	}

	public boolean begin(NkContext ctx, MemoryStack stack, float paddingX, float paddingY, float spacingX, float spacingY) {
		NkVec2 spacing = NkVec2.malloc(stack);
		NkVec2 padding = NkVec2.malloc(stack);
		
		padding.set(paddingX, paddingY);
		spacing.set(spacingX, spacingY);
		
		nk_style_push_vec2(ctx, ctx.style().window().padding(), padding);
		nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
		
		boolean drawn = begin(ctx);
		
		nk_style_pop_vec2(ctx);
		nk_style_pop_vec2(ctx);
		
		return drawn;
	}
	
	public boolean begin(NkContext ctx, MemoryStack stack, float paddingX, float paddingY, float spacingX, float spacingY, UIFont font) {
		Nuklear.nk_style_push_font(ctx, font.getFont());
		boolean drawn = begin(ctx, stack, paddingX, paddingY, spacingX, spacingY);
		Nuklear.nk_style_pop_font(ctx);
		return drawn;
	}

	public float getXWidth() {
		return x + width;
	}
	
	public float getYHeight() {
		return y + height;
	}

	public boolean setInFocus(float x, float y) {
		return focus = isWithinBounds(x, y);
	}
	
	public boolean isWithinBounds(float x, float y) {
		return (x <= this.x + width && x >= this.x) && (y <= this.y + height && y >= this.y);
	}

	public NkRect getRect() {
		return rect;
	}

	private float xCalc() {
		return (float) Window.WIDTH * xBased;
	}
	
	private float yCalc() {
		return (float) Window.HEIGHT * yBased;
	}
	
	private float wCalc() {
		return Window.WIDTH * wBased;
	}
	
	private float hCalc() {
		return Window.HEIGHT * hBased;
	}

}
