package scenes.regular;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import audio.SfxTypes;
import engine.graphics.Renderer;
import engine.graphics.interactions.RegularTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UIScrollable;
import engine.graphics.ui.UITextField;
import engine.graphics.ui.UIWindowInfo;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Texts;
import scenes.Scenes;
import scenes.adt.Scene;

public class DesignerNotesScene extends Scene {

	private UIScrollable textWindow;
	
	public static boolean reset;

	private final UIWindowInfo gobackWindow;
	private final UIButton<?> gobackBtn;

	private long controllerScroll;
	private IAction countdownAction;

	public DesignerNotesScene(RegularTopbar topbar, IAction countdownAction) {
		super(topbar, Scenes.DESIGNER_NOTES);
		this.countdownAction = countdownAction;
		
		float spacer = topbar.getHeight() * 1.0f;
		gobackWindow = createWindow(spacer * 0.5f, spacer, .5f * Window.WIDTH, 0.75f * topbar.getHeight());

		gobackBtn = new UIButton<>(Texts.gobackText, UIColors.DARKGRAY);
		gobackBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.PREVIOUS, true);
		});
		add(gobackBtn);

		textWindow = new UIScrollable(sceneIndex, Window.WIDTH * .1f, gobackWindow.getYHeight() * 1.1f,
				Window.WIDTH * 0.8f, Window.HEIGHT * 0.7f);

		textWindow.addText(UILabel.split("""
Welcome to the manual:

 1. Select a singleplayer challenge or create a multiplayer lobby or join an existing lobby if there are any.
     1.1. If you created a lobby, select a desired game-mode.
 2. Choose a car you want to drive.
 3. Press ready and when everyone is ready the race will start.
 4. Press throttle as green lights turn on! Not when it's red because you get a 3 second penalty!
 5. Use NOS as quickly as possible if you want :)
 6. Change gear by releasing throttle and moving the gear-lever to another gear-slot. The gear-box is the grid to the right when racing.
 7. Finish race and get money (or win/lose and game is over)
 8. Drag tiles from the left store-tiles and place them on your board to upgrade your car.
 9. When more tiles are next to each-other on the board you can improve them, which is the same as upgrading.
 10. Jump back to step 3.
 
When you upgrade you move/improve tiles called Tile-Upgrades. These can unlock bonuses that cost extra but give something special. 
Each bonus only appear once so choose wisely. Now, when tiles are placed next to other tiles on your board they get affected. Tiles give
off neighbor bonuses which is what is affecting said tile. A neighbor bonus is just a stat-changer or stat-adder. 
Check out the tooltip when hovering around a tile and see how the values change. You can also check out the actual changes that occur to
the right in the stats-window, but if you look at the tile-values in the tooltip next to the selected tile you see how it gets affected. 
 The first line in the tooltip is green and is called base-values, these get affected by times-mod. Does change stats.
 The second line is green and the received neighbor bonuses from surrounding tiles. Does change stats. Symbolized by <=
 The third line is black (not green) because it does not change your car stats. This line is the neighbor bonuses this tile gives to
adjacent tiles. Symbolized by =>

Here I will explain a few details about this game that might be useful:

    Times-Mod:
These are the x2, x3, x4, x5, x6 on boards. These multiply base-values.

    Tile Upgrades:
Neighboring bonus doubles at improved LVL 3 and then for every LVL after it increases by 10%. The LVLs on store-tiles (those not placed)
are based on whether or not there is a max-LVL. If there is no max-LVL, the LVL number is based on how many of that type you have had and 
have on your board, otherwise it is based on how many are placed currently. Therefore selling won't reduce that number. The price of a 
store-tile is always based on how many have been placed. The selling price is based on how much have been invested in the tile (but this 
price is halved if the tile was placed this round). If the tile had a bonus it does not contain the diff-values (so you lose nothing from 
your car when selling the tile) but it also has the terrible sale price of $0, and also this placed tile will have a minimum max-LVL of 
half the bonus LVL (ceiled so 2.3 becomes 3). 

    Tools:
Unlocked based on amount of improvements you do. This is displayed as for instance 0/8. These act different from normal upgrades and
are always sold for $50. When sold, however, it gives back 3 improvement-points and reduces the improvement-point requirement by 2. This
is because the requirement is based on how many tools are currently placed on the board.

    Boost:
Acceleration method that is unrelated to horsepower and only lasts for some time. Note about the tile is that it starts off stronger
than the tiles it unlocks and it is the only tile that allows for upgrading how long tireboost and nos lasts. Otherwise you have to buy
bonuses.

    Tireboost:
This is a boost and the off-the-line push effect. The faster you are in the beginning the less of your tireboost or "tb" you lose. 
You lose therefore a certain percentage based on how far you were from starting off the race at 0 ms. If you have guaranteed tb this 
penalty mechanic basically reacts as if you started at 0 ms. 
Then again the time you have tb does not change, so if you start driving after 500 ms and you have 2000 tb ms you do only get 1500 tb ms, 
but without guaranteed tb you would only get based on the this algorithm:
(f(x, y) = y(1 - 0.001x) | y is tb ms and x is reaction-time) 
-> f(500, 2000) = 2000(1 - 0.5)
= 2000 * 0.5
= 1000
meaning you would only get a total of 1000 ms instead of 1500 ms effective tb. Also, if your reaction-time is slower than 1 second you do
not get any tireboost.
This is all irrelevant if you have two-step of course.
Another thing about tb is that it is affected by RPM. At the lowest possible RPM it will produce 20% of tb, at base-top-RPM is 100% 
produced and then RPM's above that will produce more than your tb. Therefore, starting in a higher gear will be technically less efficient
than starting in first and going through every gear, but ultimately it depends.

    Nos:
Nos is a boost and also affected by RPM just the same as Tireboost.
Snos replaces the normal way nos works by changing to a reverse version of wind-resistance, where you get more speed the more speed you 
would normally lose from wind-resistance TIMES SIX PLUS your nos as wind. Therefore, less weight is good because of more wind-resistance!
BUT, snos uses two bottles (or 50% of it's power).

    Sticky Clutch:
Makes it so your engine acts as if it is running at the highest possible RPM throughout the tachometer range. This does not mean that you can
just shift into top gear and be equally efficient because there is a higher gear penalty.

    Dog-Box, Two-Step, and Sequential:
Dog-Box allows you to shift while holding down the throttle, Two-Step allows you to hold down the throttle before the race starts and 
sequential makes it so you can click to the next gear instead of having to move the lever. These all make it easier and quicker to shift.

    Gear penalty:
The higher gear you're using the bigger the penalty but that does not mean you go slower if you have more gears, quite the opposite because
the increase of penalty will be lower for each gear the more gears you have.

    Wind-resistance:
To a degree, weight increases wind-resistance if you weigh between 600-1200 kg and reduces if you weigh more. 
Aero reduces wind-resistance by multiplication which is why less aero is good! If you have snos you'd probably want more wind-resistance, but
snos is not affected by aero at all so don't worry!

    Red misshift penalty:
When you either grind a gear or redline you get penalized. You can only grind a gear when you do not have dog-box installed. 
Grinding occurs when you shift without fully releasing your throttle. Redlining happens when your car's RPM is at its limit; at max-RPM.
If this happens, the penalty is;
only receive 30% of total acceleration for 0.75 seconds, and then a linear increase from 65% to 100% over 0.75 seconds.

    Turbos and superchargers:
These provide pressure within the cylinders which then add to your horsepower. However, it takes time for it to spool up and provide its peak-
bar. It does go beyond its peak-bar but very slowly comparatively. This speed is both increased by spool speed and having a higher bar
potential. Spooling, however, happens only because the engine outputs more horsepower, meaning, the higher the RPM the more it spools, the 
more horsepower you produce. Spool-speed can be upgraded with the Supercharger tile.
Additionally, you may wish to use turbo-blow to multiply your bar output. Turbo-blow straightforwardly multiplies current bar output with 
turbo-blow strength, unless you have more than 100 turbo-blow available. Turbo-blow availability increases every turn (this can be upgraded
with the Turbo tile), and the more turbo-blow you have the more powerful it gets, but the faster it depletes also!
If you have 200 turbo-blow it is 100% more efficient, 300 turbo-blow it is 200% more efficient etc. But 0 to 100 turbo-blow have the same 
output. Therefore, it is wise to hold on to your turbo-blow until it is necessary to use it. 
""", "\n"));
		/*
		 * "Controls:", "Player scores: TAB", "Throttle: W", "NOS: E", "Turbo Blow: Q",
		 * "Look behind: R"
		 */
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		GL11.glClearColor(0.1f, 0.1f, 0.1f, 1);

		((RegularTopbar) topbar).setVisible(false);
		press();
		((RegularTopbar) topbar).showButtons(false);
		
		if (reset) {
			textWindow.setScrollIndex(0);
			reset = false;
		}
	}

	@Override
	public void updateResolution() {
	}

	@Override
	public void tick(float delta) {
		countdownAction.run();
	}

	@Override
	public void keyInput(int keycode, int action) {

		if (action != GLFW.GLFW_RELEASE) {
			// Downstroke for quicker input
//			generalHoveredButtonNavigation(gobackBtn, keycode);
			if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				gobackBtn.runPressedAction();
			} else if (keycode == GLFW.GLFW_KEY_PAGE_DOWN) {
				textWindow.getWindow().focus = true;
				for (int i = 0; i < 5; i++)
					textWindow.scroll(-1);
			} else if (keycode == GLFW.GLFW_KEY_PAGE_UP) {
				textWindow.getWindow().focus = true;
				for (int i = 0; i < 5; i++)
					textWindow.scroll(1);
			} else if (keycode == GLFW.GLFW_KEY_DOWN) {
				textWindow.getWindow().focus = true;
				textWindow.scroll(-1);
			} else if (keycode == GLFW.GLFW_KEY_UP) {
				textWindow.getWindow().focus = true;
				textWindow.scroll(1);
			}
		}
	}
	
	@Override
	public void controllerInput() {
		if ((InputHandler.BTN_A || InputHandler.BTN_B) && InputHandler.REPEAT)
			gobackBtn.runPressedAction();
		
		if (InputHandler.BTN_UP) {
			textWindow.getWindow().focus = true;
			textWindow.scroll(1);
			return;
		} 
		if (InputHandler.BTN_DOWN) {
			textWindow.getWindow().focus = true;
			textWindow.scroll(-1);
			return;
		}
		
		if (System.currentTimeMillis() > controllerScroll) {
			if (Math.max(InputHandler.RIGHT_STICK_Y, InputHandler.LEFT_STICK_Y) > .1) {
				textWindow.getWindow().focus = true;
				textWindow.scroll(-1);
				controllerScroll = System.currentTimeMillis() + 45;
			} else if (Math.min(InputHandler.RIGHT_STICK_Y, InputHandler.LEFT_STICK_Y) < -.1) {
				textWindow.getWindow().focus = true;
				textWindow.scroll(1);
				controllerScroll = System.currentTimeMillis() + 45;
			}
		}
	}

	@Override
	public void mouseScrollInput(float x, float y) {
		textWindow.getWindow().focus = true;
		textWindow.scroll(y);
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		boolean down = super.mouseButtonInput(button, action, x, y);
		if (down) {
			InputHandler.holdOffMouse = true;
			gobackBtn.runPressedAction();
		}
		return down;
	}

	@Override
	public void mousePositionInput(float x, float y) {
	}

	/*
	 * ========= VISUALIZATION ==========
	 */

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {

		// Set the padding of the group

//		NkVec2 spacing = NkVec2.malloc(stack);
//		NkVec2 padding = NkVec2.malloc(stack);
//
//		spacing.set(btnHeight / 8f, btnHeight / 2f);
//		padding.set(hPadding, btnHeight);
//
//		nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
//		nk_style_push_vec2(ctx, ctx.style().window().group_padding(), padding);

		Features.inst.pushFontColor(ctx, UIColors.LBEIGE);

		textWindow.layout(ctx, stack);

		Features.inst.popFontColor(ctx);
	}

	@Override
	public void destroy() {
		removeGameObjects();
	}

}
