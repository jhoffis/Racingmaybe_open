package engine.graphics.ui;

import java.nio.IntBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

public class UISlider implements IUIObject {

	private String name;
	private int min;
	private IntBuffer val;
	private int max;
	private int step;

	public UISlider() {
		min = 0;
		max = 100;
		step = 1;
		val = BufferUtils.createIntBuffer(1);
	}

	public UISlider(String name) {
		this();
		this.name = name;
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {
		Nuklear.nk_slider_int(ctx, min, val, max, step);
	}
	
	public void setValue(double value) {
		this.val.put(0, (int) (value * 100));
	}

	public double getValue() {
		return val.get(0) / 100.0;
	}

	public int getValueActual() {
		return val.get(0);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
}
