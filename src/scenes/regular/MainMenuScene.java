package scenes.regular;

import static org.lwjgl.nuklear.Nuklear.NK_WINDOW_NO_SCROLLBAR;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import engine.graphics.ui.*;
import main.Main;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import audio.SfxTypes;
import engine.graphics.interactions.RegularTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Sprite;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import engine.math.Vec2;
import engine.math.Vec3;
import main.Features;
import main.ResourceHandler;
import main.Texts;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Scene;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

/**
 * 
 * @author Jens Benz
 *
 */
public class MainMenuScene extends Scene {

	private final UIButton<?> singleplayerBtn, multiplayerBtn, replayBtn, designerNotesBtn, optionsBtn, exitBtn;
//	private final UILabel[] reminder;
	
	private Sprite discordBtn;

	private final UIWindowInfo window, bugfixWindow;
	public static int btnHeight;
	private int hPadding;
	private Sprite backgroundImage;
	
	public MainMenuScene(Consumer<Integer> createNewSingleplayerGameAction, RegularTopbar topbar) {
		super(topbar, Scenes.MAIN_MENU);

		ResourceHandler.LoadSprite("./images/back/lobby.png", "main", (sprite) -> backgroundImage = sprite.setToFullscreen());
		ResourceHandler.LoadSprite(
				new Vec2(0.85f * Window.WIDTH,
						 0.9f * Window.HEIGHT), 
						 0.075f*Window.HEIGHT, "./images/discord.png", "main", (sprite) -> discordBtn = sprite);

		window = createWindow(-Window.WIDTH, topbar.getHeight(), Window.WIDTH, Window.HEIGHT - topbar.getHeight());
		bugfixWindow = createWindow(
				Window.WIDTH * 0.5f, 
				Window.HEIGHT * 0.89f, 
				Window.WIDTH * 0.49f, 
				Window.HEIGHT * 0.1f
		);
		bugfixWindow.options = NK_WINDOW_NO_SCROLLBAR | Nuklear.NK_WINDOW_BORDER;
		
		singleplayerBtn = new UIButton<>(Texts.singleplayerText);
		multiplayerBtn = new UIButton<>(Texts.multiplayerText);
		replayBtn = new UIButton<>(Texts.replayText);
		designerNotesBtn = new UIButton<>(Texts.designerNotes);
		optionsBtn = new UIButton<>(Texts.optionsControlsText);
		exitBtn = new UIButton<>(Texts.exitText);

//		leaderboardBtn = new UIButton(Texts.leaderboardText);
//		leaderboardBtn
//		.setHoverAction(() -> audio.get(SfxTypes.REGULAR_HOVER).play());
//		leaderboardBtn.setPressedAction(() -> {
//			sceneChange.change(Scenes.LEADERBOARD, true);
//			audio.get(SfxTypes.REGULAR_PRESS).play();
//		});
//		add(leaderboardBtn);
//		leaderboardBtn.setNavigations(null, null, () -> singleplayerBtn, () -> multiplayerBtn);
		replayBtn.setPressedAction(() -> {
			sceneChange.change(Scenes.REPLAYLIST, true);
			audio.play(SfxTypes.REGULAR_PRESS);
		});
		designerNotesBtn.setPressedAction(() -> {
			DesignerNotesScene.reset = true;
			sceneChange.change(Scenes.DESIGNER_NOTES, true);
			audio.play(SfxTypes.REGULAR_PRESS);
		});
		optionsBtn.setPressedAction(() -> {
			sceneChange.change(Scenes.OPTIONS, true);
			audio.play(SfxTypes.REGULAR_PRESS);
		});
		exitBtn.setPressedAction(() -> {
			GLFW.glfwSetWindowShouldClose(Features.inst.getWindow().getWindow(), true);
		});

//		reminder = UILabel.split(
//				"""
//						This game is in Early Access!
//						Please read Controls before you begin!
//						If you encounter any bugs, please email me: jhoffiscreates@gmail.com""", "\n");
		
		/*
		 * Add to a specific window
		 */

		add(singleplayerBtn);
		add(multiplayerBtn);
		add(replayBtn);
		add(designerNotesBtn);
		add(optionsBtn);
		add(exitBtn);

		singleplayerBtn.setNavigations(null, multiplayerBtn, exitBtn, designerNotesBtn);
		multiplayerBtn.setNavigations(singleplayerBtn, null, exitBtn, designerNotesBtn);
		designerNotesBtn.setNavigations(singleplayerBtn, multiplayerBtn, singleplayerBtn, replayBtn);
		replayBtn.setNavigations(null, null, designerNotesBtn, optionsBtn);
		optionsBtn.setNavigations(null, null, replayBtn, exitBtn);
		exitBtn.setNavigations(null, null, optionsBtn, multiplayerBtn);

		if (Main.DEMO) {
			multiplayerBtn.setEnabled(false);
			singleplayerBtn.setPressedAction(() -> {
				createNewSingleplayerGameAction.accept(1);
				audio.play(SfxTypes.REGULAR_PRESS);
			});
		} else {
			multiplayerBtn.setPressedAction(() -> {
				sceneChange.change(Scenes.MULTIPLAYER, true);
				audio.play(SfxTypes.REGULAR_PRESS);
			});
			singleplayerBtn.setPressedAction(() -> {
				sceneChange.change(Scenes.SINGLEPLAYER, true);
				audio.play(SfxTypes.REGULAR_PRESS);
			});
		}

	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		((RegularTopbar) topbar).setTitle(Texts.mainMenu);
		window.setX(-Window.WIDTH);
	}
	
	@Override
	public void updateResolution() {
		btnHeight = Window.HEIGHT / 15;
		hPadding = Window.WIDTH / 8;
	}
	
	@Override
	public void tick(float delta) {
	}

	@Override
	public void keyInput(int keycode, int action) {
		if (action == 1) {
			// Downstroke for quicker input
			generalHoveredButtonNavigation(singleplayerBtn, keycode);
		}
	}

	@Override
	public void controllerInput() {
		if (!InputHandler.HOLDING) {
			generalHoveredButtonNavigationJoy(singleplayerBtn);

			if (InputHandler.BTN_B)
				exitBtn.runPressedAction();
		}
	}
	
	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		super.mouseButtonInput(button, action, x, y);
		
		if (discordBtn.above(x, y) && action != GLFW.GLFW_RELEASE) {
			Desktop desktop = java.awt.Desktop.getDesktop();
			try {
				//specify the protocol along with the URL
				URI oURL = new URI(
						"https://discord.gg/8g8KKmXhdF");
				desktop.browse(oURL);
			} catch (URISyntaxException e) {
				SceneHandler.showMessage("Could not open the discord link");
			} catch (IOException e) {
				SceneHandler.showMessage("Could not open the discord link");
			}
		}
		
		return false;
	}
	
	@Override
	public void mouseScrollInput(float x, float y) {
	}

	@Override
	public void mousePositionInput(float x, float y) {
	}

	/*
	 * ========= VISUALIZATION ==========
	 */
	@Override
	public void renderGame(Renderer renderer, Camera cam, long w, float delta) {
		// Begin the window
		renderer.renderOrthoMesh(backgroundImage);
		
		if (delta < 1f) {
			if (window.x < -1)
				window.setX(window.x * (1f - 0.35f * delta));
			else if (window.x != 0)
				window.setX(0);
		}
		
		renderer.renderOrthoMesh(discordBtn);
	}
	

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {

		// Set the padding of the group
		NkVec2 group_padding = NkVec2.malloc(stack);
		NkVec2 spacing = NkVec2.malloc(stack);

		group_padding.set(hPadding, btnHeight);
		spacing.set(btnHeight * .1f, btnHeight / 2f);

		Nuklear.nk_style_push_vec2(ctx, ctx.style().window().group_padding(), // 0.1mb
				group_padding);
		Nuklear.nk_style_push_vec2(ctx, ctx.style().window().spacing(), // 0.1mb
				spacing);

		
		/*
		 * MAIN SHIT
		 */
		if (window.begin(ctx)) {
			/*
			 * GROUP OF MAIN BUTTONS
			 */

			nk_layout_row_dynamic(ctx,
					Window.HEIGHT - topbar.getHeight(), 1);

			// Groups have the same options available as windows

			if (nk_group_begin(ctx, "My Group", NK_WINDOW_NO_SCROLLBAR)) {

//				nk_layout_row_dynamic(ctx, btnHeight, 1);
//				Nuklear.nk_label(ctx, "æøåÆØÅăѣ", Nuklear.NK_TEXT_ALIGN_LEFT); 
//				Nuklear.nk_label(ctx, "æøåÆØÅăѣ𝔠ծềſģȟᎥ𝒋ǩľḿꞑȯ𝘱𝑞𝗋𝘴ȶ𝞄𝜈ψ𝒙𝘆𝚣1234567890!@#$%^&*()-_=+[{]};:'\",<.>/?~𝘈Ḇ𝖢𝕯٤ḞԍНǏ𝙅ƘԸⲘ𝙉০Ρ𝗤Ɍ𝓢ȚЦ𝒱Ѡ𝓧ƳȤѧᖯć𝗱ễ𝑓𝙜Ⴙ𝞲𝑗𝒌ļṃŉо𝞎𝒒ᵲꜱ𝙩ừ𝗏ŵ𝒙𝒚ź1234567890!@#$%^&*()-_=+[{]};:'\",<.>", Nuklear.NK_TEXT_ALIGN_LEFT); 
//				Nuklear.nk_label(ctx, "hei", Nuklear.NK_TEXT_ALIGN_LEFT); 
				nk_layout_row_dynamic(ctx, btnHeight, 2);
				singleplayerBtn.layout(ctx, stack); 
				multiplayerBtn.layout(ctx, stack);  
				nk_layout_row_dynamic(ctx, btnHeight, 1);
				designerNotesBtn.layout(ctx, stack);  
				nk_layout_row_dynamic(ctx, btnHeight, 1);
				replayBtn.layout(ctx, stack);  
				nk_layout_row_dynamic(ctx, btnHeight, 1);
				optionsBtn.layout(ctx, stack);  
				nk_layout_row_dynamic(ctx, btnHeight, 1);
				exitBtn.layout(ctx, stack);
				
				nk_group_end(ctx);
			}

		}
		Nuklear.nk_end(ctx);
		
		Nuklear.nk_style_pop_vec2(ctx);
		Nuklear.nk_style_pop_vec2(ctx);
		
		
//		Features.inst.pushBackgroundColor(ctx, UIColors.RAISIN_BLACK, 1f);
//		Nuklear.nk_style_push_color(ctx, ctx.style().window().border_color(),
//				UIColors.COLORS[UIColors.LBEIGE.ordinal()]);
//		Features.inst.pushFontColor(ctx, UIColors.LBEIGE);
//		
//		if (bugfixWindow.begin(ctx, stack, bugfixWindow.width * .02f, 0, 0, 0)) {
//			nk_layout_row_dynamic(ctx, bugfixWindow.height * .4f, 1);
//			Nuklear.nk_label(ctx, "This is a hobby project of mine so there might be some bugs. Sorry about that!", Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
//			nk_layout_row_dynamic(ctx, bugfixWindow.height * .4f, 1);
//			Nuklear.nk_label(ctx, "If you encounter any, please email me the error-log at; jhoffiscreates@gmail.com", Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
//		}
//		Nuklear.nk_end(ctx);
//		
//		Features.inst.popFontColor(ctx);
//		Nuklear.nk_style_pop_color(ctx);
//		Features.inst.popBackgroundColor(ctx);
	}

	@Override
	public void destroy() {
		removeGameObjects();
	}


}
