package engine.graphics.ui;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

public class UIRow implements IUIObject {

	public IUIObject[] row;
	public float height;
	
	public UIRow(IUIObject[] row, float height) {
		this.row = row;
		this.height = height;
	}
	
	@Override
	public void layout(NkContext ctx, MemoryStack stack) {
		Nuklear.nk_layout_row_dynamic(ctx, height, row.length);
		for (var elem : row) {
			elem.layout(ctx, stack);
		}
	}

	public IUIObject get(int i) {
		return row[i];
	}

}
