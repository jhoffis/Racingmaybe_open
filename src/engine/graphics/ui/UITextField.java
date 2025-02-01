package engine.graphics.ui;

import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import engine.io.InputHandler;
import main.Features;

public class UITextField implements IUIHasWindow {

	private boolean focused, firstTimeFocused, removeAtFirstTime;
	private final UILabel label = new UILabel(Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_CENTERED);
	private String text;
	private String preText;
	private Consumer<Integer> specialInput;
	private IAction specialUnfocused;
	public IAction specialSetText;

	private UIWindowInfo window;

	private int maxLength;
	private String cursor;
	public UIColors background = UIColors.BLACK_TRANSPARENT;

	public boolean leanTowardsLastInfinite = false;

	/**
	 * Creates Textfield to be used in a window. It does not create its own. But it
	 * cannot be selected with mouse
	 */
	public UITextField(String preText, boolean removeAtFirstTime, boolean justNumbers, int maxLength) {
		this.preText = preText;
		this.text = preText;
		this.maxLength = maxLength;
		cursor = "";
		firstTimeFocused = true;
		this.removeAtFirstTime = removeAtFirstTime;
	}

	/**
	 * Creates Textfield to be used outside of a window. It does create its own.
	 */
	public UITextField(String preText, boolean removeAtFirstTime, boolean justNumbers, int maxLength, int sceneIndex,
			float x, float y, float width, float height) {
		this(preText, removeAtFirstTime, justNumbers, maxLength);

		window = UISceneInfo.createWindowInfo(sceneIndex, x, y, width, height);
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {
		if (window != null) {
			Features.inst.pushBackgroundColor(ctx, background);
			if (window.begin(ctx)) {
				nk_layout_row_dynamic(ctx, window.height, 1); // nested row
				layoutTextfieldItself(ctx, stack);
			}
			nk_end(ctx);
			Features.inst.popBackgroundColor(ctx);
		} else {
			layoutTextfieldItself(ctx, stack);
		}

	}

	public void layoutTextfieldItself(NkContext ctx, MemoryStack stack) {
		var text = this.text + cursor;
		if (leanTowardsLastInfinite && text.length() >= maxLength) {
			text = text.substring(text.length() - maxLength);
		}
		label.setText(text + "#" + UIColors.WHITE);
		label.layout(ctx, stack);
	}

	public void setSpecialInputAction(Consumer<Integer> specialInput) {
		this.specialInput = specialInput;
	}

	public void setUnfocuedAction(IAction specialUnfocused) {
		this.specialUnfocused = specialUnfocused;
	}

	public void input(int keycode, int action) {
		if (focused && (window == null || window.visible)) {

//			switch (keycode) {
//				case GLFW_KEY_LEFT_SHIFT :
//				case GLFW_KEY_RIGHT_SHIFT :
//					shift = !shift;
//					return;
//			}

			if (action != GLFW.GLFW_RELEASE) {
				if (specialInput != null)
					specialInput.accept(keycode);

				if (keycode == GLFW.GLFW_KEY_DELETE || keycode == GLFW.GLFW_KEY_BACKSPACE) {
					if (text.length() > 0) {
						if (InputHandler.CONTROL_DOWN) {
							int i = text.lastIndexOf(' ');
							if (i < 0)
								i = 0;
							text = text.substring(0, i);
						} else {
							text = text.substring(0, text.length() - 1);
						}
					}
				} else if (maxLength == -1 || text.length() < maxLength || leanTowardsLastInfinite) {

//					if (keycode == GLFW.GLFW_KEY_CAPS_LOCK) {
//						caps = !caps;
//						return;
//					}

//					if (keycode >= (justNumbers ? 48 : 32)
//							&& keycode <= (justNumbers ? 57 : 126)) {
//						String key = String.valueOf((char) keycode);
//
//						if (shift || caps) {
//							// key
//							if(keycode == 49)
//								key = "!";
//							else
//								key = key.toUpperCase();
//						} else {
//							key = key.toLowerCase();
//						}
//
//						text += key;
//					} else {
//						String specialKey = null;
//						switch(keycode) {
//							case 320:
//								specialKey = "0";
//								break;
//							case 321:
//								specialKey = "1";
//								break;
//							case 322:
//								specialKey = "2";
//								break;
//							case 323:
//								specialKey = "3";
//								break;
//							case 324:
//								specialKey = "4";
//								break;
//							case 325:
//								specialKey = "5";
//								break;
//							case 326:
//								specialKey = "6";
//								break;
//							case 327:
//								specialKey = "7";
//								break;
//							case 328:
//								specialKey = "8";
//								break;
//							case 329:
//								specialKey = "9";
//								break;
//							case 330:
//								specialKey = ".";
//								break;
//						}
//						
//						if(specialKey != null)
//						text += specialKey;
//					}

				}
			}

		}
	}

	public String getText() {
		return (String) text;
	}

	public void setText(String text) {
		this.text = text;
		if (specialSetText != null)
			specialSetText.run();
	}

	public boolean tryFocus(double x, double y, boolean remove) {
		if (window != null) {
			if ((window.x <= x && window.y <= y) && (window.x + window.width >= x && window.y + window.height >= y)) {
				focus(remove);
			} else {
				unfocus(remove);
			}
		}

		return focused;
	}

	public boolean isFocused() {
		return focused;
	}

	public void reset() {
		text = preText;
		firstTimeFocused = true;
	}

	public int getMaxLength() {
		return maxLength;
	}

	public void setPretext(String text) {
		preText = text;
	}

	public void unfocus(boolean remove) {
		/// Unfocused
		if (remove)
			text = preText;
		if (specialUnfocused != null)
			specialUnfocused.run();
		cursor = "";
		focused = false;
		if (window != null)
			window.focus = false;
	}

	public void focus(boolean remove) {
		// Focused
		if (!focused) {
			if ((removeAtFirstTime && firstTimeFocused) || remove) {
				text = "";
				firstTimeFocused = false;
			}
			cursor = "|";
			focused = true;
			if (window != null)
				window.focus = true;
		}
	}

	@Override
	public UIWindowInfo getWindow() {
		return window;
	}

	public void addText(String text) {
		if (text == null)
			return;
		if (!focused || (window != null && !window.visible))
			return;

		if (maxLength == -1 || text.length() < maxLength || leanTowardsLastInfinite) {
//			text = text.replaceAll("[^\\p{ASCII}]", "?");
			this.text += text;
		}
	}

}
