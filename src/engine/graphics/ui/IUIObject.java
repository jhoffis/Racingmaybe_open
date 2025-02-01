package engine.graphics.ui;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.system.MemoryStack;

public interface IUIObject {
	void layout(NkContext ctx, MemoryStack stack);
}
