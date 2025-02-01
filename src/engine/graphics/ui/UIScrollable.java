package engine.graphics.ui;

import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import main.Features;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import engine.io.Window;

public class UIScrollable implements IUIHasWindow {

	protected UIFont font;
	private boolean dynamicRowHeight;
	public float rowHeightOverrule;

	private int rowsAmount, rowsFrom;
	protected float rowHeight;
	private float paddingX = 0, paddingY = 0;
	private final UIWindowInfo window, shadowW;
	private boolean bottomHeavy, scrollable = true;
	private final List<IUIObject> rows;
	private Consumer<Integer> scrollingAction;

	public boolean shadow = false;
	public float rowHeightBased = 32f;

	public UIScrollable(int sceneIndex, float x, float y, float width, float height) {
		window = UISceneInfo.createWindowInfo(sceneIndex, x, y, width, height);
		shadowW = UISceneInfo.createWindowInfo(sceneIndex, x, y, width, height);
		shadowW.z = -1;
		UISceneInfo.addScrollable(this);
		rows = new CopyOnWriteArrayList<>();
	}

	public UIScrollable(UIFont uiFont, int sceneIndex, float x, float y, float width, float height) {
		this(sceneIndex, x, y, width, height);
		if (uiFont == null) {
			System.out.println("why are you giving scrollable label a null font?");
			return;
		}
		font = uiFont;
		if (dynamicRowHeight = uiFont.getHeight() <= 0) // TODO sjekk ut det med getHeight
			uiFont.resizeFont(height / 24f);
	}

	public void setPadding(float x, float y) {
		paddingX = x;
		paddingY = y;
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {

		boolean changeFont = font != null;

		if (changeFont)
			Nuklear.nk_style_push_font(ctx, font.getFont());

		if (shadow) {
			Features.inst.pushBackgroundColor(ctx, UIColors.BLACK_TRANSPARENT);

			shadowW.setPositionSize(window.x + 10, window.y + 10, window.width, window.height);
			shadowW.focus = false;
			if (shadowW.begin(ctx)) {
			}
			nk_end(ctx);
			Features.inst.popBackgroundColor(ctx);
		}

		if (window.begin(ctx, stack, paddingX, paddingY, 0, 0)) {

			if (rowHeightOverrule != 0)
				rowHeight = rowHeightOverrule;
			else if (dynamicRowHeight)
				rowHeight = font.getHeight() * 1.0f;
			else
				rowHeight = Window.HEIGHT / rowHeightBased;

			rowsAmount = (int) (window.height / rowHeight);

			for (int i = rowsFrom; i < rowsFrom + rowsAmount; i++) {

				synchronized (rows) {
					if (rows != null) {
						if (i >= rows.size())
							break;
						IUIObject row = null;
						try {
							row = rows.get(i);
						} catch (ArrayIndexOutOfBoundsException e) {
							break;
						}
						if (row == null)
							break;

						if (row instanceof UILeaderboardPlayer playerRow) {
							playerRow.row.height = rowHeight;
						} else if (row instanceof UIRow uirow) {
							uirow.height = rowHeight;
						} else if (!(row instanceof UILabelRow)) {
							nk_layout_row_dynamic(ctx, rowHeight, 1); // nested row
						}
						row.layout(ctx, stack);
					}
				}
			}

		}
		nk_end(ctx);

		if (changeFont)
			Nuklear.nk_style_pop_font(ctx);
	}

	private void scrollPoint(int newRowsFrom) {
		if (newRowsFrom >= 0 && newRowsFrom < rows.size() - rowsAmount + 5) // padding
			rowsFrom = newRowsFrom;

		if (scrollingAction != null)
			scrollingAction.accept(rowsFrom);
	}

	public void scroll(float y) {
		if (scrollable && window.focus && window.visible) {
			int direction = y > 0 ? 1 : -1;
			int newRowsFrom = rowsFrom - direction;
			scrollPoint(newRowsFrom);
		}
	}
	
	
	public void showHovered() {
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i) instanceof UIButton btn && btn.isHovered()) {
				if (i < rowsFrom || i >= rowsFrom + .85*rowsAmount) {
					scrollPoint(i);
				}
				break;
			}
		}
	}

	public void addText(IUIObject obj) {
		rows.add(obj);
		updateRowsFrom();
	}

	public void addText(String text) {
		addText(text, Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
	}

	public void addText(String text, int options) {
		for (UILabel label : UILabel.split(text, "\n")) {
			rows.add(label);
			label.options = options;
		}
		updateRowsFrom();
	}

	public void addText(IUIObject[] labels) {
		Collections.addAll(this.rows, labels);
		updateRowsFrom();
	}

	public void addText(List<IUIObject> labels) {
		rows.addAll(labels);
		updateRowsFrom();
	}

	public void addText(ArrayList<UILabel> labels) {
		this.rows.addAll(labels);
		updateRowsFrom();
	}

	public String getText() {
		if (rows == null || rows.size() == 0)
			return "";
		StringBuilder text = new StringBuilder();
		for (IUIObject row : rows) {
			if (row instanceof UILabel)
				text.append(((UILabel) row).getText()).append("\n");
		}

		return text.toString();
	}

	public void setText(String text) {
		rows.clear();
		Collections.addAll(rows, UILabel.split(text, "\n"));
		updateRowsFrom();
	}

	public void setText(String text, int options) {
		rows.clear();
		Collections.addAll(rows, UILabel.split(text, "\n", options));
		updateRowsFrom();
	}

	public void setText(IUIObject[] labels) {
		this.rows.clear();
		Collections.addAll(this.rows, labels);
		updateRowsFrom();
	}

	public void setText(List<IUIObject> labels) {
		rows.clear();
		rows.addAll(labels);
		updateRowsFrom();
	}

	public void clear() {
		this.rows.clear();
		updateRowsFrom();
	}

	private void updateRowsFrom() {
		if (!bottomHeavy) {
			rowsFrom = 0;
		} else {
			rowsFrom = rows.size() - rowsAmount + 2;
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

	public float getRowHeight() {
		return rowHeight;
	}

	public int getScrollIndex() {
		return rowsFrom;
	}

	public void setScrollIndex(int scrollIndex) {
		rowsFrom = scrollIndex;
	}

	public void addScrollingAction(Consumer<Integer> action) {
		scrollingAction = action;
	}

	public IUIObject[] getListArr() {
		return rows.toArray(new IUIObject[0]);
	}

	public List<IUIObject> getList() {
		return rows;
	}

	public void addNavigationToScrollableList(int sceneIndex, UIButton outBtn, boolean outBottom, boolean outSide) {
		for (int i = 0; i < rows.size(); i++) {
			if (rows.get(i) instanceof UIButton btn) {
				btn.setNavigations(
						outSide ? outBtn : null, 
						outSide ? outBtn : null, 
						i > 0 ? 
								(UIButton) rows.get(i - 1) 
								: null,
						(i + 1) < rows.size() 
								? (UIButton) rows.get(i + 1)
						: outBottom && (i + 1) == rows.size() ? 
								outBtn : null
				);
				UISceneInfo.addPressableToScene(sceneIndex, btn);
			}
		}
	}
	public int getRowsFrom() {
		return rowsFrom;
	}

	public void remove(IUIObject btn) {
		if (btn == null) return;
		rows.remove(btn);
	}
}
