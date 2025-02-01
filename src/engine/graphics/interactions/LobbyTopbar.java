package engine.graphics.interactions;

import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_style_pop_vec2;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_vec2;

import java.nio.FloatBuffer;
import java.util.function.Consumer;

import communication.GameInfo;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import audio.AudioRemote;
import audio.SfxTypes;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIWindowInfo;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Texts;
import scenes.Scenes;

/**
 * 
 * @author Jhoffis
 *
 */
public class LobbyTopbar extends TransparentTopbar implements IUIObject {

	public static final float HEIGHT_RATIO = 9.11f;
	public UIWindowInfo rightButtonsWindow, menuWindow;
	public UIButton<?> menuButton,
						closeMenuButton,
						leaveBtn,
						resignBtn,
						options,
						designerNotes;
	public GameInfo com;
	private UIWindowInfo subsceneTabsWindow;
	private UIButton<?>[] subsceneTabs;

	public LobbyTopbar(AudioRemote audio, TopbarInteraction topbar) {
		super(topbar, HEIGHT_RATIO);
		topbar.setHeightRatio(HEIGHT_RATIO);
		
		// Layout settings
		final float rightbuttonsWidth = 10;
		final float height = topbar.getHeight() / 3f;
		
		float playerInfoRightButtonsSplitt = Window.WIDTH - Window.WIDTH / rightbuttonsWidth;
		System.out.println("WIDTH " + playerInfoRightButtonsSplitt / 2.2f);
		subsceneTabsWindow = UISceneInfo.createWindowInfo(Scenes.GENERAL_NONSCENE,
				0, 
				0, 
				playerInfoRightButtonsSplitt / 2.2f, 
				height);
		
//		playerInfoWindow = UISceneInfo.createWindowInfo(Scenes.GENERAL_NONSCENE,
//				subsceneTabsWindow.getXWidth(), 
//				0, 
//				playerInfoRightButtonsSplitt - subsceneTabsWindow.getXWidth(), 
//				height);
	
		rightButtonsWindow = UISceneInfo.createWindowInfo(Scenes.GENERAL_NONSCENE,
				playerInfoRightButtonsSplitt, 
				0, 
				Window.WIDTH / rightbuttonsWidth, 
				height);
		
		float w = Window.WIDTH / 4.5f;
		float h = Window.WIDTH / 6f;
		menuWindow = UISceneInfo.createWindowInfo(Scenes.GENERAL_NONSCENE,
				Window.WIDTH - w,
				rightButtonsWindow.getYHeight(),
				w, 
				h);
		menuWindow.options = menuWindow.options | Nuklear.NK_WINDOW_BORDER; 
		
//		playerInfoWindow.visible = false;
		rightButtonsWindow.visible = false;
		menuWindow.visible = false;
		menuWindow.z = 1;

		menuButton = new UIButton<>(Texts.menu);
		menuButton.setPressedAction(() -> {
			if (!menuWindow.visible) {
				audio.play(SfxTypes.REGULAR_PRESS);
				rightButtonsWindow.z = 1;
				menuWindow.visible = true;
				menuButton.press();
				closeMenuButton.press();
				leaveBtn.press();
				options.press();
			} else {
				closeMenuButton.runPressedAction();
			}
		});
		UISceneInfo.addPressableToScene(Scenes.GENERAL_NONSCENE, menuButton);
		
		closeMenuButton = new UIButton<>(Texts.closeMenu);
		closeMenuButton.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			hideMenu();
		});
		closeMenuButton.setColor(UIColors.valByInt(0, UIColors.CHARCOAL));
		UISceneInfo.addPressableToScene(Scenes.GENERAL_NONSCENE, closeMenuButton);

		designerNotes = new UIButton<>(Texts.designerNotes);
		designerNotes.setColor(UIColors.valByInt(0, UIColors.CHARCOAL));
		UISceneInfo.addPressableToScene(Scenes.GENERAL_NONSCENE, designerNotes);
		
		
		topbar.setHeightRatio(22f);
	}
	
	public void hideMenu() {
		menuWindow.visible = false;
		rightButtonsWindow.z = 0;
	}

	public void layout(NkContext ctx, MemoryStack stack) {
		// Set own custom styling
		
		if (subsceneTabsWindow.begin(ctx)) {
			Nuklear.nk_layout_row_dynamic(ctx, subsceneTabsWindow.height * 0.9f, subsceneTabs.length);
			for (var btn : subsceneTabs) {
				btn.layout(ctx, stack);
			}
		}
		nk_end(ctx);
		
		int amountElem = 4;

		final float height = topbar.getHeight() / 3f;
		float refinedPositionX = getHeight() / 20f;

		/*
		 * right buttons
		 */
		float refinedPositionY = height / 14;
		boolean menuOpen = menuWindow.visible;
		if (menuOpen) {
			Features.inst.pushBackgroundColor(ctx, UIColors.VERY_BLACK_TRANSPARENT);
			Features.inst.pushFontColor(ctx, UIColors.LBEIGE);
			menuWindow.focus = true;
			var padding = menuWindow.height * .05f;
			var space = padding * .8f;

			NkVec2 spacing = NkVec2.malloc(stack);
			spacing.set(0, space);
			
//			nk_style_push_vec2(ctx, ctx.style().window().contextual_padding(), spacing);
//			nk_style_push_vec2(ctx, ctx.style().window().group_padding(), spacing);
			Nuklear.nk_style_push_color(ctx, ctx.style().window().border_color(), UIColors.valByInt(0, UIColors.DARKGRAY));
			nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);

			if(menuWindow.begin(ctx, stack, padding, padding, 0, 0)) {
				float menuElemHeight = (menuWindow.height - padding*(amountElem - 1f) - space*2f) / (float) amountElem;
				nk_layout_row_dynamic(ctx, menuElemHeight, 1);
				closeMenuButton.layout(ctx, stack);
				nk_layout_row_dynamic(ctx, menuElemHeight, 1);
				options.layout(ctx, stack);
				nk_layout_row_dynamic(ctx, menuElemHeight, 1);
				designerNotes.layout(ctx, stack);
				nk_layout_row_dynamic(ctx, menuElemHeight, 1);
				if (chooseResign()) {
					resignBtn.layout(ctx, stack);
				} else {
					leaveBtn.layout(ctx, stack);
				}
			}
			nk_end(ctx);
			Nuklear.nk_style_pop_color(ctx);
			nk_style_pop_vec2(ctx);
//			nk_style_pop_vec2(ctx);
//			nk_style_pop_vec2(ctx);

			Features.inst.popFontColor(ctx);
			Features.inst.popBackgroundColor(ctx);
		}
		
		if (rightButtonsWindow.begin(ctx, stack, refinedPositionX, refinedPositionY, refinedPositionX, refinedPositionY)) {
			// Layout
			nk_layout_row_dynamic(ctx, rightButtonsWindow.height, 2);
			Nuklear.nk_label(ctx, "", 0);
			menuButton.layout(ctx, stack);
		}
		nk_end(ctx);
		
	}
	
	@Override
	public void select() {
		topbar.select(this);
	}

	@Override
	public void setVisible(boolean visible) {
		if(
//				playerInfoWindow == null || 
				rightButtonsWindow == null)
			return;
		
//		playerInfoWindow.visible = visible;
		rightButtonsWindow.visible = visible;
	}

	public void setSubscenes(UIButton<?>[] subscenes) {
		this.subsceneTabs = subscenes;
	}

	public void setTabsVisible(boolean b) {
		subsceneTabsWindow.visible = b;
	}
	
	public float getSplitWidth() {
		return subsceneTabsWindow.width;
	}

	public void setLobbyButtons(UIButton<?> goBack, UIButton<?> resignBtn, UIButton<?> options, IAction designerAction) {
		this.leaveBtn = goBack;
		this.resignBtn = resignBtn;
		this.options = options;
		leaveBtn.setColor(UIColors.valByInt(0, UIColors.CHARCOAL));
		resignBtn.setColor(UIColors.valByInt(0, UIColors.CHARCOAL));
		options.setColor(UIColors.valByInt(0, UIColors.CHARCOAL));
		designerNotes.setPressedAction(designerAction);
	}


	public void press() {
		menuButton.press();
		closeMenuButton.press();
		leaveBtn.press();
		options.press();
		designerNotes.press();
	}

	public void input(int keycode) {
		if (keycode == GLFW.GLFW_KEY_ESCAPE) {
			closeMenuButton.runPressedAction();
		} else if (keycode == GLFW.GLFW_KEY_ENTER) {
			if (closeMenuButton.isHovered()) {
				closeMenuButton.runPressedAction();
			} else if (options.isHovered()) {
				options.runPressedAction();
			} else if (designerNotes.isHovered()) {
				designerNotes.runPressedAction();
			} else if (resignBtn.isHovered()) {
				resignBtn.runPressedAction();
			} else if (leaveBtn.isHovered()) {
				leaveBtn.runPressedAction();
			}
		} else if (keycode == GLFW.GLFW_KEY_UP) {
			if (options.isHovered()) {
				options.unhover();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				closeMenuButton.hover();
				closeMenuButton.hoverFake();
			} else if (designerNotes.isHovered()) {
				designerNotes.unhover();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				options.hover();
				options.hoverFake();
			} else if (leaveBtn.isHovered() || resignBtn.isHovered()) {
				leaveBtn.unhover();
				resignBtn.unhover();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				designerNotes.hover();
				designerNotes.hoverFake();
			} else {
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				closeMenuButton.hover();
				closeMenuButton.hoverFake();
			}
		} else if (keycode == GLFW.GLFW_KEY_DOWN) {
			if (closeMenuButton.isHovered()) {
				closeMenuButton.unhover();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				options.hover();
				options.hoverFake();
			} else if (options.isHovered()) {
				options.unhover();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				designerNotes.hover();
				designerNotes.hoverFake();
			} else if (designerNotes.isHovered()) {
				designerNotes.unhover();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				if (chooseResign()) {
					resignBtn.hover();
					resignBtn.hoverFake();
				} else {
					leaveBtn.hover();
					leaveBtn.hoverFake();
				}
			} else if (!leaveBtn.isHovered() && !resignBtn.isHovered()) {
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				closeMenuButton.hover();
				closeMenuButton.hoverFake();
			}
		} 		
	}

	private boolean chooseResign() {
		return !com.isSingleplayer() && com.isGameStarted() && !com.isGameOver() && !com.resigned;
	}

	public void controllerInput() {
		if (InputHandler.BTN_B) {
			input(GLFW.GLFW_KEY_ESCAPE);
		} else if (InputHandler.BTN_A) {
			input(GLFW.GLFW_KEY_ENTER);
		} else if (InputHandler.BTN_UP) {
			input(GLFW.GLFW_KEY_UP);
		} else if (InputHandler.BTN_DOWN) {
			input(GLFW.GLFW_KEY_DOWN);
		} 		
	}
}
