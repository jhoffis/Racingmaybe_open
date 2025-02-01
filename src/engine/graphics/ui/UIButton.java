package engine.graphics.ui;

import static org.lwjgl.nuklear.Nuklear.nk_widget_is_hovered;

import java.util.function.Consumer;

import main.Features;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import engine.io.InputHandler;
import main.Main;

/**
 * Create with text that is displayed in the middle of the button with the
 * externally predefined font. Then add pressed action (0..x -> void pressed, T
 * pressed, void pressed right) Then add hover action Then add references to
 * buttons left, right, above or below (nav with arrows) (null = nothing there)
 * Then use either add(..) method from Scene.java or UISceneInfo or use
 * pressed() and unpressed() to add reference to the button so it works as
 * intended. Otherwise it can't be pressed more than once. Then use layout(..)
 * to show the button in a UIWindowInfo.
 */
public class UIButton<T> implements IUIObject, IUIPressable {

	public float alphaFactor = 1f;
	public String tooltip;
	public boolean hoverable = true, trueHover = false;
	private String title;
	private boolean visible = true;
	private int alignment;
	protected boolean mouseHover, keyHover, pressed, hasRunHover, enabled = true;
	public NkColor normalColor, activeColor, hoverColor, disabledColor;
	private static final NkColor[] colorStds = new NkColor[4];

	// Actions:
	private Consumer<T> consumerPressedAction;
	private T consumerValue;
	private IAction pressedAction, pressedActionRight, hoveredAction, hoveredExitAction, changeHoverButtonAction;
	private UINavigationAction left, right, above, below;

	public UIButton(String title, NkColor normal) {
		this.title = title;
		alignment = Nuklear.NK_TEXT_ALIGN_MIDDLE | Nuklear.NK_TEXT_ALIGN_CENTERED;
		if (!Main.NO_SOUND)
			hoveredAction = Main.hoverAction;
		setColor(normal);
	}

	public UIButton(String title, UIColors normal) {
		this(title, UIColors.COLORS[normal.ordinal()]);
	}

	public UIButton(String title) {
		this(title, (NkColor) null);
	}

	public void setColor(NkColor normal) {
		boolean useStandardColors = normal == null;
		if (useStandardColors) {
			if (colorStds[0] != null) {
				this.normalColor = colorStds[0];
				this.activeColor = colorStds[1];
				this.hoverColor = colorStds[2];
				this.disabledColor = colorStds[3];
				return;
			} else {
				normal = NkColor.create().r((byte) 204).g((byte) 194).b((byte) 165).a((byte) 255);
			}
		}
		final float inc = 1.3f;
		int nR = normal.r(), nG = normal.g(), nB = normal.b();
		if (nR < 0)
			nR += 256;
		if (nG < 0)
			nG += 256;
		if (nB < 0)
			nB += 256;
		float aR = nR * (inc - 1f), aG = nG * (inc - 1f), aB = nB * (inc - 1f);
		float hR = nR * inc, hG = nG * inc, hB = nB * inc;
		if (hR > 255)
			hR = 255;
		if (hG > 255)
			hG = 255;
		if (hB > 255)
			hB = 255;

		this.normalColor = normal;
		this.activeColor = NkColor.create().r((byte) aR).g((byte) aG).b((byte) aB).a((byte) 255);
		this.hoverColor = NkColor.create().r((byte) hR).g((byte) hG).b((byte) hB).a((byte) 0xff);
		this.disabledColor = NkColor.create().r((byte) 0x44).g((byte) 0x44).b((byte) 0x44).a((byte) 200);

		if (useStandardColors) {
			colorStds[0] = normal;
			colorStds[1] = activeColor;
			colorStds[2] = hoverColor;
			colorStds[3] = disabledColor;
		}
	}

	public void setColorUI(UIColors color) {
		setColor(UIColors.valByInt(0, color));
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {

		if (!visible) {
			Nuklear.nk_label(ctx, "", 0);
			return;
		}

		final int normalAlpha = normalColor.a();
		final int activeAlpha = activeColor.a();
		final int hoverAlpha = hoverColor.a();
		final int disabledAlpha = disabledColor.a();

		normalColor.a((byte) ((normalAlpha + (normalAlpha < 0 ? 256 : 0)) * alphaFactor));
		activeColor.a((byte) ((activeAlpha + (activeAlpha < 0 ? 256 : 0)) * alphaFactor));
		hoverColor.a((byte) ((hoverAlpha + (hoverAlpha < 0 ? 256 : 0)) * alphaFactor));
		disabledColor.a((byte) ((disabledAlpha + (disabledAlpha < 0 ? 256 : 0)) * alphaFactor));

		NkColor figuredNormalColor = null;
		if (enabled) {
			if (hoverable) {
				ctx.style().button().hover().data().color().set(hoverColor);
				ctx.style().button().active().data().color().set(activeColor);
			} else {
				ctx.style().button().hover().data().color().set(normalColor);
				ctx.style().button().active().data().color().set(normalColor);
				ctx.style().button().normal().data().color().set(normalColor);
			}
		} else {
			ctx.style().button().hover().data().color().set(disabledColor);
			ctx.style().button().active().data().color().set(disabledColor);
			ctx.style().button().normal().data().color().set(disabledColor);
		}
		/*
		 * Deal with hover stuff
		 */
		if (enabled && hoverable) {
			var above = false;
			if (trueHover) {
				var pos = NkRect.malloc(stack);
				Nuklear.nk_widget_bounds(ctx, pos);
				above = pos.x() < InputHandler.x && pos.y() < InputHandler.y && pos.x() + pos.w() >= InputHandler.x
						&& pos.y() + pos.h() >= InputHandler.y;
			} else {
				above = Nuklear.nk_widget_is_hovered(ctx);
			}

			if (above) {
				if (!mouseHover) {
					hover();

					mouseHover = true;
					keyHover = false;
				}
				if (tooltip != null && Nuklear.nk_widget_is_hovered(ctx)) {
					Features.inst.pushFontColor(ctx, UIColors.WHITE);
					Nuklear.nk_tooltip(ctx, tooltip);
					Features.inst.popFontColor(ctx);
				}

			} else if (mouseHover) {
				mouseHover = false;

				if (hoveredExitAction != null)
					hoveredExitAction.run();
			}
		}

		boolean hovered = mouseHover || keyHover;
		if (enabled) {
			if (hovered)
				figuredNormalColor = hoverColor;
			else
				figuredNormalColor = normalColor;

//			NkStyleButton.ROUNDING = 0;
//			Nuklear.nkstyle
			ctx.style().button().normal().data().color().set(figuredNormalColor);
		}
		/*
		 * Deal with pressing stuff
		 */
		ctx.style().button().text_alignment(alignment);
		ctx.style().button().border(0);
		ctx.style().button().rounding(0);

		if (Nuklear.nk_button_label(ctx, title) && !pressed) {
			if (runPressedAction(InputHandler.MOUSEBUTTON)) {
				pressed = true;
			}
		}

		normalColor.a((byte) normalAlpha);
		activeColor.a((byte) activeAlpha);
		hoverColor.a((byte) hoverAlpha);
		disabledColor.a((byte) disabledAlpha);
	}

	public void setTitleAlignment(int alignment) {
		this.alignment = alignment;
	}

	public void runPressedAction() {
		runPressedAction(GLFW.GLFW_MOUSE_BUTTON_LEFT);
	}
	
	public void runRightPressedAction() {
		runPressedAction(GLFW.GLFW_MOUSE_BUTTON_RIGHT);
	}

	private boolean runPressedAction(int button) {
		boolean res = false;

		if (enabled && visible && hoverable) {
			if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
				if (pressedAction != null) {
					pressedAction.run();
					res = true;
				} else if (consumerPressedAction != null) {
					consumerPressedAction.accept(consumerValue);
					res = true;
				}
			} else if (pressedActionRight != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
					&& InputHandler.MOUSEACTION == GLFW.GLFW_RELEASE) {
				pressedActionRight.run();
				res = true;
			}
		}

		return res;
	}

	public void runHoveredAction() {
		// Play hover sfx
		if (hoveredAction != null) {
			hoveredAction.run();
		} else
			System.out.println(title + " does not have a hover action");
	}

	public void hover() {
		if (keyHover == false) {
			keyHover = true;
			runHoveredAction();
			if (changeHoverButtonAction != null)
				changeHoverButtonAction.run();
		}
	}

	public void hoverFake() {
		keyHover = true;
		mouseHover = true;
	}

	public void unhover() {
		keyHover = false;
		mouseHover = false;
	}

	public boolean isHovered() {
		return keyHover || mouseHover;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	public void setPressedAction(IAction action) {
		this.pressedAction = action;
	}

	public void setPressedActionRight(IAction action) {
		this.pressedActionRight = action;
	}

	public void setHoverAction(IAction action) {
		this.hoveredAction = action;
	}

	public void setHoverExitAction(IAction action) {
		this.hoveredExitAction = action;
	}

	public void setChangeHoverButtonAction(IAction changeHoverButtonAction) {
		this.changeHoverButtonAction = changeHoverButtonAction;
	}

	public boolean isPressed() {
		return pressed;
	}

	/**
	 * Used to allow the button to be pressed again. The button has to be UP for it
	 * to react to press.
	 */
	@Override
	public void release() {
		pressed = false;
	}

	/**
	 * Manually press the button down: The button has to be UP for it to react to
	 * press. This presses without activating the button below. Used to avoid
	 * pressing button at same position if for instance changing scenes.
	 */
	@Override
	public void press() {
		pressed = true;
	}

	public void setNavigations(UINavigationAction left, UINavigationAction right, UINavigationAction above,
			UINavigationAction below) {
		this.left = left;
		this.right = right;
		this.above = above;
		this.below = below;
	}

	public void setNavigations(UIButton left, UIButton right, UIButton above, UIButton below) {
		if (left == null)
			this.left = null;
		else
			this.left = () -> left;

		if (right == null)
			this.right = null;
		else
			this.right = () -> right;

		if (above == null)
			this.above = null;
		else
			this.above = () -> above;

		if (below == null)
			this.below = null;
		else
			this.below = () -> below;
	}

	public void hoverNavigate(ButtonNavigation nav) {
		switch (nav) {
		case LEFT:
			navigate(left);
			break;
		case RIGHT:
			navigate(right);
			break;
		case ABOVE:
			navigate(above);
			break;
		case BELOW:
			navigate(below);
			break;
		default:
			break;
		}
	}

	private void navigate(UINavigationAction action) {
		if (action != null) {
			@SuppressWarnings("rawtypes")
			UIButton btn = action.run();
			if (btn != null && btn.isEnabled() && btn.isVisible())
				btn.hover();
		}
	}

	public void setEnabled(boolean b) {
		if (b == enabled)
			return;
		enabled = b;
		if (enabled) {
			release();
		}
	}

	public Consumer<T> setPressedAction(Consumer<T> consumerPressedAction) {
		return this.consumerPressedAction = consumerPressedAction;
	}

	public T getConsumerValue() {
		return consumerValue;
	}

	public void setConsumerValue(T consumerInteger) {
		this.consumerValue = consumerInteger;
	}

	public void setVisible(boolean b) {
		this.visible = b;
	}

	public boolean isVisible() {
		return visible;
	}

	public boolean hasChangeHoverButtonAction() {
		return changeHoverButtonAction != null;
	}

	public IAction getPressedAction() {
		return pressedAction;
	}

	public boolean isEnabled() {
		return enabled;
	}

}
