package engine.graphics.ui;

import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.util.ArrayList;
import java.util.Arrays;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import engine.io.Window;

public class UIScrollableLabel implements IUIHasWindow {

	protected UIFont font;
	private boolean dynamicRowHeight;
	
	private int rowsAmount, rowsFrom, rowsMax;
	protected float rowHeight;
	private float paddingX = 0, paddingY = 0;
	private UIWindowInfo window;
	private boolean bottomHeavy, scrollable = true;
	private ArrayList<UILabel> labels;
	
	public UIScrollableLabel(int sceneIndex, float x, float y, float width, float height) {
		window = UISceneInfo.createWindowInfo(sceneIndex,
				x, 
				y, width, height);
		labels = new ArrayList<UILabel>();
	}

	public UIScrollableLabel(UIFont uiFont, int sceneIndex, float x, float y, float width, float height) {
		this(sceneIndex, x, y, width, height);
		if (uiFont == null) {
			System.out.println("why are you giving scrollable label a null font?");
			return;
		}
		font = uiFont;
		if (dynamicRowHeight = uiFont.getHeight() <= 0)
			uiFont.resizeFont(height / 24f); 
	}

	public void setPadding(float x, float y) {
		paddingX = x;
		paddingY = y;
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {

		boolean changeFont = font != null;
		
		if(changeFont) 
			Nuklear.nk_style_push_font(ctx, font.getFont());
		
		if (window.begin(ctx, stack, paddingX, paddingY, 0, 0)) {

			if(dynamicRowHeight)
				rowHeight = font.getHeight() * 1.1f;
			else
				rowHeight = Window.HEIGHT / 32f;
			
			rowsAmount = (int) (window.height / rowHeight) - 1;
			
			synchronized (labels) {
				if(labels != null) {
					rowsMax = labels.size();
					for (int i = rowsFrom; i <  rowsFrom + rowsAmount; i++) {
						
						if(i >= rowsMax)
							break;
							nk_layout_row_dynamic(ctx, rowHeight, 1); // nested row
							labels.get(i).layout(ctx, stack);
					}
				}
			}
		}
		nk_end(ctx);
		
		if(changeFont) 
			Nuklear.nk_style_pop_font(ctx);
	}
	
	public void scroll(float y) {
		if(scrollable && window.focus) {
			int direction = y > 0 ? 1 : -1;
			int newRowsFrom = rowsFrom - direction;
			
			if(newRowsFrom >= 0 && newRowsFrom < rowsMax - rowsAmount + 3) // padding
				rowsFrom = newRowsFrom;
		}
	}

	public void addText(String text) {
		addText(text, Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
	}
	
	public void addText(String text, int options) {
		for(UILabel label : UILabel.split(text, "\n")) {
			labels.add(label);
			label.options = options;
		}
		updateRowsFrom();
	}
	
	public void addText(UILabel[] labels) {
		this.labels.addAll(Arrays.asList(labels));
		updateRowsFrom();
	}
	
	public void addText(ArrayList<UILabel> labels) {
		this.labels.addAll(labels);
		updateRowsFrom();
	}

	public String getText() {
		if(labels == null || labels.size() == 0) 
			return "";
		StringBuilder text = new StringBuilder();
		for(UILabel label : labels) {
			text.append(label.getText()).append("\n");
		}
		text.setLength(text.length() - 2);
		
		return text.toString();
	}

	public void setText(String text) {
		labels.clear();
		labels.addAll(Arrays.asList(UILabel.split(text, "\n")));
		updateRowsFrom();
	}
	
	public void setText(UILabel[] labels) {
		this.labels.clear();
		this.labels.addAll(Arrays.asList(labels));
		updateRowsFrom();
	}
	
	private void updateRowsFrom() {
		if (!bottomHeavy) {
			rowsFrom = 0;
		} else {
			rowsFrom = rowsMax - rowsAmount + 1;
			if (rowsFrom < 0)
				rowsFrom = 0;
		}
	}

	@Override
	public UIWindowInfo getWindow() {
		return window;
	}

	public void setBottomHeavy(boolean b) {
		this.bottomHeavy = b;
	}

	public void setScrollable(boolean b) {
		this.scrollable = b;
	}

}
