package scenes.game.racing_subscenes;

import static org.lwjgl.nuklear.Nuklear.nk_begin;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_vec2;

import java.awt.Color;
import java.util.List;
import java.util.Random;

import elem.MovingThings;
import game_modes.GameMode;
import main.Texts;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryStack;

import audio.AudioRemote;
import elem.Animation;
import engine.graphics.interactions.Gearbox;
import engine.graphics.objects.Camera;
import engine.graphics.objects.GameObject;
import engine.graphics.objects.Road;
import engine.graphics.objects.Sprite;
import engine.graphics.objects.SpriteNumeric;
import engine.graphics.ui.Font;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UIFont;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIScrollable;
import engine.graphics.ui.UIWindowInfo;
import engine.graphics.Renderer;
import engine.graphics.Texture;
import engine.io.InputHandler;
import engine.io.Window;
import engine.math.Vec2;
import engine.math.Vec3;
import main.Features;
import main.Main;
import main.ResourceHandler;
import player_local.Player;
import player_local.car.Car;
import player_local.car.CarModel;
import player_local.car.CarStats;
import player_local.car.Rep;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Visual;
import scenes.game.Race;
import scenes.regular.HotkeysScene;

public class RaceVisual extends Visual {

	private final int whiteBound = 20000;
	private int mouseDelay;

	private Race race;
	private Car[] opponents;
	private Car myCar;
	private Sprite myCarBase, myHandsBase0, myHandsBase1, myHandsBase2, myHandsBase3, currentHands;
	private float spdIncActual;
	private Texture myCarLighting;
	private Vec2 baseMyCarPos;
	private final float baseCamPosY = 1.5f, baseCamRotY = 180f;
	
	private Random looksRandom;

	public final Road road = new Road();
	private int extraLanes;

	private Vec3 tintColor;
	private float tintAlpha, raceInfoHeight, leftPadding;
	private Sprite background;
	private Animation nitros;
	private Sprite fastness;
	private float lightDistance;

	// top left and right info
	private UIFont infoFont;
	private UILabel currentDistance, lapsedDistance, lapsedTime, extraGamemodeInfo;
	public static float infoSpacingY, infoPaddingX, infoPaddingY, infoRowHeight;
	public static boolean ShowHints = true;
	private final UIWindowInfo hintsWindow, turboBarWindow;
	private final UILabel hintsLabel;
	private final UIScrollable playerListPlacement;

	// bottom right info
	private SpriteNumeric tachoSpeed;
	private SpriteNumeric tachoGear;
	private SpriteNumeric turboBlow;

	private float lifes;
	private Sprite heart, halfHeart;

	private Sprite tachoBase;
	private Sprite tachoPointer;
	private Sprite tachoDotSmall, tachoDotLarge, tachoDotRedline;
	private Vec2 tachoPointerOrigo;
	private Vec2 tachoPointerProperPosition;

	private Sprite turbo, mirror;
	private Vec2 turboPointerProperPosition;

	private Gearbox gearbox;
	private UIScrollable gearboxTimeShift;

	// middle info
	private int ballcount;
	public boolean hasBeenGreen;
	private Sprite racelight; // multiple
	private Sprite tireboost;
	private UIWindowInfo tireboostWindow;
	private UILabel tireboostInfoLabel;

	// bottom left info
	private Sprite nosbottle; // multiple
	private Vec3 nosStrengthColor;
	private float blurShake;
	private float frameScale;
	private boolean tireboostInfoShow;
	private long tireboostInfoShowTime;
	private float templight = 10;
	private float turboPointerScaleup;
	private float turboPointerScaledown;
	private float whiteify;
	private float lane;
	public boolean lookBehind, burnout;

	private boolean theBoss;

//	private Sprite myCarWheel;
//	private Sprite stabilityArrow;
//	private UIWindowInfo stabilityWindow;
//	private UILabel stabilityLabel;

	public static float convertPixels(float pixels) {
		return (Window.HEIGHT / 5.5f) * (pixels / 24f);
//    	return (Window.HEIGHT / 6f) * (pixels / 24f); 
	}

	public RaceVisual(Race race) {
		this.race = race;
		paddingUpdate();

		ResourceHandler.LoadSprite("./images/road0.png", "background", (sprite) -> background = sprite);
		ResourceHandler.LoadSprite("./images/uglymirror.png", "background", (sprite) -> mirror = sprite);
		nitros = new Animation("nitros", "main", 4, 0, Window.HEIGHT);

		float hintsWidth = Window.WIDTH * 0.5f;
		float hintsHeight = Window.HEIGHT * 0.05f;
		hintsWindow = UISceneInfo.createWindowInfo(Scenes.RACE, Window.WIDTH * 0.5f - hintsWidth * 0.5f,
				hintsHeight * 0.2f, hintsWidth, hintsHeight);
		turboBarWindow = UISceneInfo.createWindowInfo(Scenes.RACE, Window.WIDTH * .65f, Window.HEIGHT * .72f,
				Window.WIDTH * .1f, Window.HEIGHT * .1f);
		playerListPlacement = new UIScrollable(Scenes.RACE, infoPaddingX, Window.HEIGHT * .2f,
				Window.WIDTH * .1f, Window.HEIGHT * .8f);
		playerListPlacement.setScrollable(false);
		hintsWindow.options = Nuklear.NK_WINDOW_BORDER | Nuklear.NK_WINDOW_NO_SCROLLBAR;
		hintsLabel = new UILabel("", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);
		currentDistance = new UILabel();
		lapsedDistance = new UILabel();
		lapsedTime = new UILabel(Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE);
		extraGamemodeInfo = new UILabel(Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE);

//		stabilityWindow = UISceneInfo.createWindowInfo(Scenes.RACE, Window.WIDTH * 0.5f - hintsWidth * 0.5f, Window.HEIGHT - hintsHeight * 0.2f, hintsWidth, hintsHeight);
//		stabilityWindow.options = Nuklear.NK_WINDOW_BORDER | Nuklear.NK_WINDOW_NO_SCROLLBAR;
//		stabilityLabel = new UILabel("lakdsj");
//		ResourceHandler.LoadSprite("./images/arrow.png", "stability", (sprite) -> stabilityArrow = sprite);

		blurShake = 3.0f;

		tireboostInfoLabel = new UILabel("");
		tireboostInfoLabel.options = Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE;

		ResourceHandler.LoadSprite("./images/fastness.png", "fastness", (sprite) -> fastness = sprite);

		final float pixels12 = convertPixels(12);
		final float pixels24 = convertPixels(24);
		ResourceHandler.LoadSprite(new Vec2(0), pixels12, "./images/heart.png", "main", (sprite) -> heart = sprite);
		ResourceHandler.LoadSprite(new Vec2(0), pixels12, "./images/halfHeart.png", "main",
				(sprite) -> halfHeart = sprite);

		ResourceHandler.LoadSprite(new Vec2(Window.WIDTH / 2f - pixels12, pixels24), pixels24, "./images/racelight.png",
				"racelight", (sprite) -> {
					racelight = sprite;

					float tbSize = convertPixels(10);
					ResourceHandler.LoadSprite(new Vec2(0, racelight.position().y + racelight.getHeight()), tbSize,
							"./images/tireboost.png", "tireboost", (sprite2) -> {
								tireboost = sprite2;

								tireboost.setPositionX(Window.WIDTH / 2 - tireboost.getWidth() / 2);
								float tbWindowWidth = tireboost.getWidth();
								tireboostWindow = UISceneInfo.createWindowInfo(Scenes.RACE,
										Window.WIDTH / 2 - tbWindowWidth / 2,
										tireboost.position().y + tireboost.getHeight(), tbWindowWidth,
										tireboost.getHeight());
							});
				});
		// have to place nos bottle every time as it is based on camera pos.
		float size = convertPixels(38);
		ResourceHandler.LoadSprite(new Vec2(0, Window.HEIGHT - size * 1.1f), size, "./images/nosbottle.png",
				"nosbottle", (sprite) -> nosbottle = sprite);

		ResourceHandler.LoadSprite(Window.HEIGHT / 2.5f, "./images/tachometer.png", "tachometer", (sprite) -> {
			tachoBase = sprite;

			tachoBase.setPositionX(Window.WIDTH - tachoBase.getWidth());
			tachoBase.setPositionY(Window.HEIGHT - tachoBase.getHeight());

			ResourceHandler.LoadSprite(tachoBase.getHeight() / 10.1f, "./images/tachometerPointer.png", "tachometer",
					(s) -> tachoPointer = s);

			// dots
			Vec2 pos = new Vec2((float) Window.WIDTH / 2f - tachoBase.getHeight() / 2.7f, (float) Window.HEIGHT / 2f);
			ResourceHandler.LoadSprite(pos, tachoBase.getHeight() / 85.33f, "./images/tachometerDotSmall.png", "main",
					(s) -> tachoDotSmall = s);
			ResourceHandler.LoadSprite(pos, tachoBase.getHeight() / 42.67f, "./images/tachometerDotLarge.png", "main",
					(s) -> tachoDotLarge = s);
			ResourceHandler.LoadSprite(pos, tachoBase.getHeight() / 32f, "./images/tachometerDotRedline.png", "main",
					(s) -> tachoDotRedline = s);
		});
		ResourceHandler.LoadSprite(Window.HEIGHT / 4.2f, "./images/turbometer.png", "tachometer",
				(sprite) -> turbo = sprite);

//		goal.setPositionYReal(goal.getHeightReal() / 2);

		// tachometerPointer.setPositionX(tachometerPointX + distanceX / 1.6f -
		// tachometerPointerRotationX);
//		tachometerPointer.setPositionX(tachometerPointX + distanceX / 1.6f);
//		tachometerPointer.setPositionY(tachometerPointY - distanceY / 2);
		// tachometerBase.scale(8f);
		// tachometerBase.setPositionX(orthoCamera.getRight() / 10);
		// tachometerBase.setPositionY(10f);
		// tachometerBase.setPositionX(1.5f);
		// tachometerBase.setPositionX(orthoCamera.getRight() -
		// tachometerBase.getScale().x());

		infoFont = new UIFont(Font.REGULAR, Window.HEIGHT / 38);

	}

	public void initRest(Player myPlayer, AudioRemote audio) {
		this.player = myPlayer;

		if (myCarBase != null) {
			myCarBase.destroy();
			myCarLighting.destroy();
			gearbox.destroy();
			myHandsBase0.destroy();
			myHandsBase1.destroy();
			myHandsBase2.destroy();
			myHandsBase3.destroy();
		}
		// TODO flytt disse ut siden de tar veldig lite plass i minnet. Bare noen kb.
		String carname = player.getCarRep().getName();
		myCarBase = new Sprite(Window.HEIGHT, "./images/" + carname + ".png", "carinterior");
		myHandsBase0 = new Sprite(Window.HEIGHT, "./images/hands/" + carname + "_hand0.png", "carinterior");
		myHandsBase1 = new Sprite(Window.HEIGHT, "./images/hands/" + carname + "_hand1.png", "carinterior");
		myHandsBase2 = new Sprite(Window.HEIGHT, "./images/hands/" + carname + "_hand2.png", "carinterior");
		myHandsBase3 = new Sprite(Window.HEIGHT, "./images/hands/" + carname + "_hand3.png", "carinterior");
		myCarBase.create();
		myHandsBase0.create();
		myHandsBase1.create();
		myHandsBase2.create();
		myHandsBase3.create();
		baseMyCarPos = myCarBase.position();
		myCarLighting = new Texture("./images/" + carname + "Light.png");
		myCarLighting.create();
//		myCarWheel = new Sprite(myCarBase.getHeight() / 2.7f,"./images/" + carname + "_wheel.png", "main");
//		myCarWheel.create();

		gearbox = new Gearbox(player.car, audio);

		Sprite gearboxSprite = this.gearbox.getGearbox();
		float height = gearboxSprite.getHeight(), width = height;
		gearboxTimeShift = new UIScrollable(Scenes.RACE, (float) gearboxSprite.position().x - width,
				(float) gearboxSprite.position().y, width, height);
		gearboxTimeShift.setScrollable(false);

		for (var nitro : nitros.getFrames()) {
			nitro.setToFullscreen();
		}
		fastness.setToFullscreen();
		mirror.setToFullscreen();
	}

	public void initBeforeNewRace(List<Player> sortedOpponents, Camera camera, 
			int distance, float lifes, boolean commentator, long randomSeed, boolean theBoss) {
		this.theBoss = theBoss;
		looksRandom = new Random(randomSeed);
		lightDistance = 55f;
		MovingThings.reset(looksRandom);
		
		SceneHandler.switchFreeCam(commentator);
		
		playerListPlacement.clear();
		
		if (sortedOpponents.size() > 2) {
			playerListPlacement.addText("<= Left");
			for (var p : sortedOpponents) {
				if (p.id == this.player.id)
					playerListPlacement.addText("Me: " + p.name);
				else
					playerListPlacement.addText(p.name);
			}
			playerListPlacement.addText("Right =>");
		}
		
		var stats = player.car.getStats();
		var dogbox = stats.stats[Rep.throttleShift] == 0;
		hintsLabel.setText("Drive: " + HotkeysScene.Throttle + "  " + "Shift: "
				+ (dogbox ? "Release " + HotkeysScene.Throttle + " and " : "")
				+ (stats.sequentialShift
						? (!dogbox ? "C" : "c") + "lick gear lever or " + HotkeysScene.ShiftUp + "/"
								+ HotkeysScene.ShiftDown
						: (!dogbox ? "D" : "d") + "rag gear lever")
				+ (stats.nosBottleAmountLeft != 0 ? "  NOS: " + HotkeysScene.Nos : "")
				+ (stats.stats[Rep.bar] != 0 ? "  Turbo: " + HotkeysScene.Turbo : "") + "  Quit: "
				+ HotkeysScene.QuitRace);
		lookBehind = false;
		this.lifes = lifes;
		spdIncActual = 0;

		var pos = camera.getPosition();
		pos.z = 0;
		pos.y = baseCamPosY;
		var rot = camera.getRotation();
		rot.x = 0;
		rot.y = baseCamRotY;
		rot.z = 0f;
		road.init(sortedOpponents.size());
		road.goal.setPositionZ(distance);
		extraLanes = Math.max(sortedOpponents.size() - 2, 0);

		float nosStrengthBased = (float) player.getCarRep().get(Rep.nos) / 60f;
		float max = .94f;
		if (nosStrengthBased > max) {
			nosStrengthBased = max;
		}

		nosStrengthColor = new Vec3(Color.getHSBColor(nosStrengthBased, 1f, 1f));

		// TODO perhaps create the camera and update whenever you're told to
		// update,
		// instead of every time.

		var opponentsCars = new Car[sortedOpponents.size() - (player.role != Player.COMMENTATOR ? 1 : 0)];
		int n = 0;
		for (int i = 0; i < sortedOpponents.size(); i++) {
			Car car;
			Player otherPlayer;
			try {
				otherPlayer = sortedOpponents.get(i);
			} catch (ArrayIndexOutOfBoundsException ignore) {
				System.out.println("out of bounds at index " + i);
				break;
			}
			if (otherPlayer.equals(player)) {
				this.lane = road.createLanePos(i);
				car = myCar = otherPlayer.car;
			} else {
				car = opponentsCars[n] = otherPlayer.car;
				n++;
			}
			var model = car.getModel();
			model.setPositionSide(road.createLanePos(i));
			model.setRotation(-90);
			model.setRotationZ(0);
			switch (car.getRep().getNameID()) {
			case 0:
				model.getModel().position().y = 1.52f;
				break;
			case 1:
				model.getModel().position().y = 1.36f;
				break;
			case 2:
				model.getModel().position().y = 1.35f;
				break;
			case 3:
				model.getModel().position().y = 1.17f;
				break;
			}
		}
		this.opponents = opponentsCars;

		if (gearbox != null) {
			gearbox.resetAndUpdateGearTop(false);
			updateGearPosition();
		}

		// TODO find the sprites and the model for your car and the model for
		// the opponent
		// You only show the model of the other car and your model is used for
		// cinematics at the beginning, for burnout and for finishing.
		// But I guess for finishing you need all cars. Eh, the car models arnt
		// that expensive.
		ballcount = 0;
		hasBeenGreen = false;
		tireboostInfoLabel.setText("");
		tireboostInfoShow = false;
		tireboostInfoShowTime = 0;

		templight = 10;
		if (turboBlow == null)
			createTurboBlowPercentage();
		// backgroundImage.getShader().setUniform//new Vector3f(0.05f, 0.05f, 0.1f));

		if (theBoss) {
			tintColor = new Vec3(.3f, 0, 0);
			tintAlpha = 0.3f;
			GL11.glClearColor(0.0f, 0, 0, 1);
			return;
		}
		tintColor = new Vec3(looksRandom.nextFloat(), looksRandom.nextFloat() / 2f, looksRandom.nextFloat());
		tintAlpha = looksRandom.nextFloat() / 15f + 0.03f;

		GL11.glClearColor(0f, 16f / 255f * (1 - tintAlpha) + tintColor.y * tintAlpha,
				20f / 255f * (1 - tintAlpha) + tintColor.z * tintAlpha, 1);
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
//		GL11.glClearColor(1f, 1f, 1f, 1f);
	}
	
	private void paddingUpdate() {
		infoSpacingY = Window.HEIGHT / 60;
		infoPaddingX = Window.HEIGHT / 33.75f;
		infoPaddingY = Window.HEIGHT / 25.71f;
	}

	@Override
	public void updateResolution() {
		infoRowHeight = infoFont.getHeight() * 1.1f;
		paddingUpdate();

		float rotX = 5.1f, rotY = 2;
		float tachometerPointerRotationX = tachoPointer.getWidth() / rotX;
		float tachometerPointerRotationY = tachoPointer.getHeight() / rotY;
		tachoPointerOrigo = new Vec2(Window.WIDTH / 2 - tachometerPointerRotationX,
				Window.HEIGHT / 2 - tachometerPointerRotationY);
		tachoPointerProperPosition = new Vec2(tachoBase.position().x + tachoBase.getWidth() * 0.5f // * 0.63f
				- tachoPointerOrigo.x - tachometerPointerRotationX,
				tachoBase.position().y + tachoBase.getHeight() / 1.95f - tachoPointerOrigo.y
						- tachometerPointerRotationY);

		float turbometerDistanceX = 0.8f;
		float turbometerDistanceY = 1.1f;

		float scale = turbo.getWidth() * 0.95f;
		turboPointerScaledown = scale / tachoBase.getWidth();
		turboPointerScaleup = tachoBase.getWidth() / scale;

		float turbometerPointerRotationX = tachoPointer.getWidth() * turboPointerScaledown / rotX;
		float turbometerPointerRotationY = tachoPointer.getHeight() * turboPointerScaledown / rotY;
		Vec2 turbometerPointerOrigo = new Vec2(Window.WIDTH / 2 - turbometerPointerRotationX,
				Window.HEIGHT / 2 - turbometerPointerRotationY);
		turbo.setPosition(new Vec2(tachoBase.position().x - turbo.getWidth() * turbometerDistanceX,
				tachoBase.getHeight() + tachoBase.position().y - turbo.getHeight() * turbometerDistanceY));
		turboPointerProperPosition = new Vec2(
				turbo.position().x + turbo.getWidth() * 0.5f - turbometerPointerOrigo.x - turbometerPointerRotationX,
				turbo.position().y + turbo.getHeight() / 2f - turbometerPointerOrigo.y - turbometerPointerRotationY);

		frameScale = ((float) Window.WIDTH / (float) Window.HEIGHT) * (9f / 16f);

		road.setScale(frameScale);

		if (tachoSpeed != null) {
			tachoGear.destroy();
			tachoSpeed.destroy();
		}

		Vec2 tachpoint = tachoBase.position();
		float textHeight = Window.HEIGHT / 18f;
		float x = tachpoint.x + tachoBase.getWidth() / 2f + textHeight / 3.8f;
		float height = Window.HEIGHT / 10.5f + textHeight / 2f; // was 5.5f
		// width of tachometer size. Height is text height
		tachoSpeed = new SpriteNumeric(new Vec2(x, Window.HEIGHT - height), textHeight, 490, true);

		textHeight *= 0.8f;
		x = Window.WIDTH - textHeight;
		height = Window.HEIGHT / 9.5f - textHeight / 2f;
		tachoGear = new SpriteNumeric(new Vec2(x, 0), textHeight, 0, false);
		updateGearPosition();
		createTurboBlowPercentage();

		addGameObject(tachoGear);
		addGameObject(tachoSpeed);

		tachoDotSmall.setPositionX(-tachoBase.getWidth() / 2f);
		tachoDotLarge.setPositionX(-tachoBase.getWidth() / 2f);
		tachoDotRedline.setPositionX(-tachoBase.getWidth() / 2f);

		raceInfoHeight = 2 * infoRowHeight + infoSpacingY + infoPaddingY;
		leftPadding = raceInfoHeight * .1f;
		if (gearbox != null)
			gearbox.resetAndUpdateGearTop(true);
	}

	private void updateGearPosition() {
		if (gearbox != null) {
			tachoGear.getTopleft().y = gearbox.getGearbox().getYHeight() - tachoGear.getHeight() * 1.5f;
			gearbox.updateResolution();
		}
	}

	private void createTurboBlowPercentage() {
		if (turboBlow != null)
			turboBlow.destroy();
		float margin = 0.6f;
		float textHeight = Window.HEIGHT / 36f;
		float x = turbo.position().x + turbo.getWidth() * margin * 1.1f,
				y = turbo.position().y + turbo.getHeight() * margin;
		turboBlow = new SpriteNumeric(new Vec2(x, y), textHeight, 490, true);
		addGameObject(turboBlow);
	}

	/**
	 * if <= 3 then red balls otherwise green.
	 *
	 * @param i
	 */
	public void setBallCount(int i) {
		ballcount = i;
	}

	@Override
	public void mouseScrollInput(float x, float y) {
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		// TODO Auto-generated method stub
		// if (action == GLFW.GLFW_RELEASE) {
		// this.x += 1f / 4f;
		// if (this.x > 10f)
		// this.x = -10f;
		// tachometerBase.setPositionX((float) this.x);
		// System.out.println(this.x);
		// }
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			if (action != GLFW.GLFW_RELEASE)
				gearbox.press(x, y);
			else
				gearbox.release(x, y);
		}
		return false;
	}

	@Override
	public void mousePosInput(float x, float y) {
//		 perspCamera.rotateCameraMouseBased(x - Window.WIDTH / 2,
//		 y - Window.HEIGHT / 2);
		if (mouseDelay > 0) {
			gearbox.release(x, y);
			if (InputHandler.CONTROLLER_EFFECTIVELY) {
				mouseDelay--;
				return;
			} else {
				mouseDelay = 0;
			}
		}

		gearbox.move(x, y);
	}

	@Override
	public void tick(float tickFactor) {
		if (race != null) {

			currentDistance.setText("Distance: " + race.getCurrentLength() + "m");
			if (System.currentTimeMillis() >= race.getStartTime()) {
				lapsedTime.setText("Time: " + String
						.format("%.3f", (float) (System.currentTimeMillis() - race.getStartTimeAltered()) / 1000)
						.replace(',', '.') + " sec");
			} else {
				lapsedTime.setText("Waiting");
			}

			gearboxTimeShift.setText(gearbox.getTimeShifted());

			if (player != null) {
//				goal.setPositionZ((float) (-(race.getCurrentLength() - player.getCar().getStats().distance) * 8f)); // why the fuck do i have to time this by 3 to make it look right?
//				float distanceY = 3f * 240f / race.getCurrentLength();
//				float distanceDivide =  (float) (player.getCar().getStats().distance / (float)race.getCurrentLength());
//				float y =  (1f - distanceDivide) * (goal.getHeightReal() / distanceY); // + (-goal.getHeightReal() / 6.8f * distanceDivide) 
//				goal.setPositionYReal(y);

//				float scale = (player.getCar().getStats().distance / (float)race.getCurrentLength()) + 0.5f;
//				goal.setScale(new Vector3f(scale, scale, scale));

				lapsedDistance.setText("Distance covered: " + Texts.formatNumber(player.car.getStats().distance) + "m");

				double speed = player.car.getStats().speed;
				tachoSpeed.setNumber((int) speed);

				if (speed > whiteBound) {
					whiteify = (float) ((speed - whiteBound + ((whiteBound / 10f) * Features.ran.nextFloat()))
							/ (whiteBound * 4f));
				} else {
					whiteify = 0;
				}

//				String gear = null;
//				if (player.getCar().getStats().gear == 0)
//					gear = "N";
//				else
//					gear = String.valueOf(player.getCar().getStats().gear);
				tachoGear.setNumber(player.car.getStats().gear);
				if (player.car.hasTurbo() && turboBlow != null) {
					turboBlow.setNumber((int) player.car.getStats().stats[Rep.turboblow]);
				}

//				System.out.println(gearbox.getTimeShifted());
			}
		}

//		perspCamera.setPosition(new Vector3f((float) player.getCar().getStats().distance, 0, 1f));

		// x += delta / 8;
		// if (x > 10)
		// x = -10;
		// tachometerBase.setPositionX((float) x);

		// TODO change me into generated 3d elements.
//		background.setCurrentFrame((background.getCurrentFrame() + (player.getCar().getStats().speed / 100) * tickFactor)
//				% background.getFramesAmount());

		gearbox.tick(tickFactor);

	}

	@Override
	public boolean hasAnimationsRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		Car car = null;
		CarStats stats = null;
		double speed = 0, grindingCurrent = 1d, myDistance = 0;

		if (player.role != Player.COMMENTATOR) {
			car = myCar;
			stats = car.getStats();
			grindingCurrent = stats.grindingCurrent;
			speed = stats.speed;
			myDistance = stats.distance;

			if (stats.sequentialShift) {
				var held = gearbox.getHeld();
				if (held == 0) currentHands = myHandsBase1;
				else if (held == 1) currentHands = myHandsBase2;
				else currentHands = myHandsBase3;
			} else if (!gearbox.isHolding()) {
				currentHands = myHandsBase0;
			} else {
			    if (stats.gear == 0) currentHands = myHandsBase1;
				else if (stats.gear % 2 == 1) currentHands = myHandsBase2;
				else currentHands = myHandsBase3;
			}

			if (lookBehind) {
				var rot = cam.getRotation();
				rot.y = 0;
				rot.x = 0;
			}
		} else {
			myDistance = cam.getPosition().z;
		}
		
		boolean grinding = grindingCurrent < 1d;
		var actualTint = new Vec3(tintColor);
		var alphaBound = 0.5f;
		var actualAlpha = tintAlpha > whiteify ? tintAlpha : Math.min(whiteify, alphaBound);
		if (grinding) {
			actualTint.x = 1f;
			actualTint.y = 0.f;
			actualTint.z = 0.f;
			actualAlpha = (float) (1f - grindingCurrent);
		} else {
			for (int i = 0; i < 3; i++) {
				float oldTint = actualTint.get(i) * (1f - (4f * whiteify));
				if (oldTint < 0)
					oldTint = 0;
				float newTint = oldTint + whiteify;
				if (newTint > 1)
					newTint = 1;
				actualTint.set(i, newTint);
			}
		}

		var lookBehind = this.lookBehind;
		if (player.role == Player.COMMENTATOR) {
			var rot = SceneHandler.cam.getRotation();
			if (Math.abs(rot.y) < 90)
				lookBehind = true;
		}

		if (!SceneHandler.freeCam) {
			cam.getPosition().x = lane;
			if (stats != null)
				cam.getPosition().z = (float) stats.distance;
		}
		
		road.shBack.setUniform("tint", tintColor, tintAlpha);

		road.getGrass().setPositionX(-cam.getPosition().x - road.getLength() / 2f);
		road.getGrass().setPositionZ(-cam.getPosition().z - road.getLength() / 2f);
		renderer.renderMesh(road.getGrass(), cam, GL11.GL_ALWAYS);

//		if (!lookBehind) {
		MovingThings.render(looksRandom, renderer, cam, actualTint, actualAlpha, grinding, extraLanes, theBoss);
//		}
		
		
		double actualRotation = Math.abs(cam.getRotation().y) % 360;
		int direction = (actualRotation < 270 && actualRotation >= 90) ? 1 : -1;
		int chunkLoad = 0;
		
		for (int i = -1 * direction; Math.abs(i) < Math.abs(3 * direction); i += direction) {
			chunkLoad = (int) ((myDistance + Math.signum(myDistance) * road.getLength() * 0.5f)/ road.getLength());
			road.getPavement().setPositionZ((i + chunkLoad) * road.getLength());
			renderer.renderMesh(road.getPavement(), cam, GL11.GL_ALWAYS);
		}
		
		if (road.getRailing() != null) {
			for (int i = -1 * direction; Math.abs(i) < 30; i += direction) {
				 // * (laneSize * 2f) - laneSize;
				chunkLoad = (int) ((myDistance + Math.signum(myDistance) * road.getRailingLength() * 0.5f)/ road.getRailingLength());
				road.getRailing().position().x = (i + chunkLoad) * road.getRailingLength();
				road.getRailing().render(renderer, cam);
			}
		}
		
		
		for (var opponent : opponents) {
			try {
				opponent.getModel().setPositionToModel(0);
				if (opponent.getModel().ai) {
					if (race.getStartTime() > 0) {
						var raceTime = System.currentTimeMillis() - race.getStartTime();
					}
				} else {
					opponent.getModel().updatePositionByInformation(System.currentTimeMillis() - race.getStartTime(),
							race.getCurrentLength(), delta, true);
				}
//				var ca = opponent.getAudio();
//				if (!ca.getMotor().isPlaying()) {
//					ca.motorAcc(car.hasTurbo());
//				}
//				ca.motorPitch(opponent.getModel().getSpdInc(), .1f, 2, 1);
////				audio.motorPitch(stats.rpm, rep.get(Rep.rpmBaseTop), 
////						switch (rep.getNameID()) {
////							case 3:
////								yield 1.2;
////							default :
////								yield 1.5;
////						},
////						throttlePerc*((stats.NOSON || stats.tireboostON) ? 3 : (stats.NOSDownPressed ? 0.5f : 1)));
//
////				ca.turbospoolPitch(1, 200, 1, 1, 1);
//				ca.getMotor().velocity((float) (speed / 2.2f), 0, 0);
//				var pos = new Vec3(0, 0, (float) (myDistance + opponent.getModel().getPositionDistance()));
////				pos.x = 0;
////				System.out.println(pos);
//				ca.setMotorPosition(pos);
				opponent.renderCar(renderer, cam); // FIXME baklengs distance
			} catch (NullPointerException e) {
				System.out.println("nullptr at opponent");
				race.finishRace(Race.CHEATED_TOO_EARLY);
			}
		}

		
		
		road.lightingTime += delta;
		if (road.lightingTime > road.lightingRanNext) {
			road.lightingRan = Features.ran.nextLong();
			road.lightingTime = 0;
			road.lightingRanNext = Features.ran.nextFloat(6f, 10f);
		}
		var ran = new Random(road.lightingRan);
//		var lampMyDist = player.role == Player.COMMENTATOR ? -500 : stats.distance
		boolean drewGoal = false;
		for (float i = 15; i >= 0; i--) {
			var posX = -1.1f*road.getSize() + 15;
	//		lamppost.setPositionYReal(10f);
			
			var z = i*lightDistance + Math.round((float) myDistance / lightDistance - 0.5f) * lightDistance + 17f;
			if (!drewGoal && road.getGoal().getRealPosition().z > z) {
				drewGoal = true;
				road.getGoal().getShader().setUniform("tint", tintColor, tintAlpha);
				renderer.renderMesh(road.getGoal(), cam);
			}
	//		lamppost.flipX();
			float mod = (float) (myDistance % lightDistance * 2f / 100f);
			float a = 0.85f;
			var diff = z - myDistance;
			if (diff < 10f) {
				if (diff < 0.3f && direction == 1) // 0.3 er 0.2 fra høyre lampe og ellers vil den flickre
					continue;
				a *= ((z - myDistance) / 10f);
//				System.out.println(z - distanceCovered);
			}
			
			road.lamppost.setPositionXReal(posX);
			road.lamppost.setPositionZ(z);
			road.lamppost.updateTransformation();
			road.lamppost.getShader().setUniform("flick", ran.nextFloat(.95f, 1.05f));
			road.lamppost.getShader().setUniform("trans", a);
			road.lamppost.getShader().setUniform("tint", new Vec3(0), (i-mod) / 10f);
			renderer.renderMesh(road.lamppost, cam, GL11.GL_LEQUAL);
			
			road.lamppost.setPositionXReal(-posX);
			road.lamppost.setPositionZ(z - .1f);
			road.lamppost.flipX();
			road.lamppost.updateTransformation();
			renderer.renderMesh(road.lamppost, cam, GL11.GL_LEQUAL);

			if (!drewGoal && i == 0 && road.getGoal().getRealPosition().z <= z) {
				drewGoal = true;
				road.getGoal().getShader().setUniform("tint", tintColor, tintAlpha);
				renderer.renderMesh(road.getGoal(), cam);
			}
		}

		if (player.role != Player.COMMENTATOR) {
			boolean moving = stats.speed > 1;
//			if (burnout) {
//				car.getModel().setPositionToModel(0);
//				if (!SceneHandler.freeCam) {
//					cam.getPosition().x = lane + 2.5f;
//					cam.getPosition().y = 2.25f;
//					cam.getPosition().z = -5.96f;
//					cam.getRotation().x = -0.6f;
//					cam.getRotation().y = 154.0f;
//					cam.getRotation().z = 0f;
//				}
//				car.renderCar(renderer, cam);
//			} else {

				if (!lookBehind) {
					myCarBase.resetTransformation();
					currentHands.resetTransformation();

					zoomMyCar(cam, stats, moving, delta);

					if (!moving || player.car.getStats().redlinedThisGear) {
						double power = car.getRep().getTotalKW() / 300.0;
						if (power > 18)
							power = 18;
						var rpm = stats.rpm / car.getRep().get(Rep.rpmTop);
						if (Math.abs(rpm) > 1)
							rpm = 1;
						float comparedValue = (float) (rpm * power);

						rotateIdle(myCarBase, comparedValue, blurShake);
						rotateIdle(currentHands, comparedValue, blurShake);
					} else {
						var z = myCarBase.rotation().z;
						myCarBase.setRotationZ(z - (z * 0.5f * delta));
						currentHands.setRotationZ(z - (z * 0.5f * delta));
					}
					if (moving) {
						shakeHighSpeed(speed);
					}
//				}
			}
		}

//		stabilityArrow.getShader().setUniform("stability", (float) stats.stabilityPunishment);
//		stabilityArrow.setScale(new Vec3(0.03f));
//		stabilityArrow.setPositionX((float) stats.stability * (500f / stabilityArrow.getScale().x));
//		renderer.renderOrthoMesh(stabilityArrow);
		if (player.role != Player.COMMENTATOR) {
			if (!burnout && !lookBehind) {
				/*
				 * dashboard lights
				 */
				myCarBase.getShader().setUniform("lightingTexture", 1);
				currentHands.getShader().setUniform("lightingTexture", 1);
				float tillNext = (float) (templight - stats.distance + 0f);
				// la oss si at det g�r fra 10 fram til -10 bak.
				var halfLen = lightDistance / 2f;
				if (Math.abs(tillNext) <= halfLen) {
					tillNext += halfLen; // 0 til 20
					tillNext = -(tillNext - lightDistance) / lightDistance;
				} else {
					tillNext = 0.0f;
					templight += lightDistance;
				}
				tillNext = tillNext * 1.5f - 0.25f;

				// System.out.println(tillNext);

				myCarLighting.bind(GL13.GL_TEXTURE1);
				myCarBase.getShader().setUniform("lightDistanceNext", tillNext);
				myCarBase.getShader().setUniform("lightDistancePrev", 0);
				myCarBase.getShader().setUniform("tint", tintColor, tintAlpha);
				currentHands.getShader().setUniform("lightDistanceNext", 10f);
				currentHands.getShader().setUniform("lightDistancePrev", 0);
				currentHands.getShader().setUniform("tint", tintColor, tintAlpha);
				if (!SceneHandler.freeCam) {
					renderer.renderOrthoMesh(myCarBase);
					renderer.renderOrthoMesh(currentHands);
				}
				myCarLighting.unbind();
				if (player.car.getStats().NOSON) {
					nitros.setCurrentFrame(Features.ran.nextInt(nitros.getFramesAmount()));
					renderer.renderOrthoMesh(nitros.getFrame());
				}

				// renderer.renderOrthoMesh(myCarWheel);

				double fastnessBoundry = 250d;

				if (speed > fastnessBoundry) {
					float speedPercentage = (float) (speed - fastnessBoundry) / 1000f;
					if (speedPercentage > 1f)
						speedPercentage = 1f;
					
//					System.out.println(speedPercentage);

					fastness.getShader().setUniform("speedPercentage", speedPercentage);
					renderer.renderOrthoMesh(fastness);
				}
			} else {
				renderer.renderOrthoMesh(mirror);
			}
			/*
			 * middle
			 */

			if (!lookBehind) {
				if (!burnout)
					tireboost(car, renderer);

				/*
				 * nos
				 */
				if (!burnout)
					nosbottles(car, renderer);

				/*
				 * turbo
				 */
				if (car.hasTurbo()) {
					renderer.renderOrthoMesh(turbo);
					if (turboBlow != null)
						turboBlow.render(renderer);
					tachoPointer.scale(turboPointerScaledown);
					meterPointerRotation(tachoPointer, tachoPointerOrigo, turboPointerProperPosition,
							car.getTurbometer());
					tachoPointer.scale(turboPointerScaleup);
					renderer.renderOrthoMesh(tachoPointer);
				}

				/*
				 * tachometer
				 */
				renderer.renderOrthoMesh(tachoBase);

				float rpm = (float) (car.getRep().get(Rep.rpmTop) / 1000f) - 1f;
				float rpmJump = (float) (5f * Math.floor(rpm / 25f));
				float len = 5f * rpm;
				float degree = 180f / (((float) len + 5f) * 6f / 8f); // det er alltid 5 i mellom hver 1000rpm og 6 / 8
																		// er
																		// innenfor 180
				// grader. Hvor mange grader er det i mellom hver.
				int rpmK = 0;
				int dot = -1;
				var oldTurboBlowTopLeft = turboBlow.getTopleft();
				turboBlow.shader.setUniform("specialColor", new Vec3(0.9294f, 0.9333f, 0.9412f));

				for (int i = 0; i <= len + 5; i += 1 + rpmJump) {
					float diff = tachoBase.getWidth() - tachoBase.getHeight();
					diff /= 6f;
					Vec3 rot = new Vec3(0, 0, i * degree - 30);
					Vec2 pos = new Vec2(
							(float) -Window.WIDTH / 2f + tachoBase.position().x + tachoBase.getWidth() / 2f + diff / 2f,
							(float) -Window.HEIGHT / 2f + tachoBase.position().y + tachoBase.getHeight() / 2f + diff);
					Sprite chosenDot = null;
					boolean big = false;
					dot++;
					if (rot.z < 170) {
						if (dot % 5 == 0) {
							chosenDot = tachoDotLarge;
							big = true;
						} else
							chosenDot = tachoDotSmall;
					} else {
						if (dot % 5 == 0) {
							chosenDot = tachoDotRedline;
							big = true;
						} else
							continue;
					}

					chosenDot.setRotation(rot);
					chosenDot.updateTransformation();
					chosenDot.setPosition(pos);
					chosenDot.updateTransformation();

					renderer.renderOrthoMesh(chosenDot);
					chosenDot.setPosition(new Vec2(0));

					if (big) {
						turboBlow.leanRight = rot.z >= 90;
						var origo = new Vec2(tachoBase.getXWidth() - tachoBase.getWidth() / 2f,
								tachoBase.getYHeight() - tachoBase.getHeight() / 2f);
						float hypotenus = tachoBase.getHeight() * 0.283f;
						float lengstUnnaHosliggendeKatet = hypotenus * (float) Math.cos(0);

						float grader = (float) Math.toRadians(rot.z);
						float hosliggendeKatet = hypotenus * (float) Math.cos(grader);
						float motKatet = hypotenus * (float) Math.sin(grader);
						float a = (lengstUnnaHosliggendeKatet + hosliggendeKatet) / 2f;
						float skyverX = 1f - a / lengstUnnaHosliggendeKatet;
						var endPoint = new Vec2(origo.x - hosliggendeKatet - 0.1f * hypotenus * skyverX,
								origo.y - motKatet - 0.1f * hypotenus * skyverX);

						turboBlow.setTopleft(endPoint);
						turboBlow.setNumber(rpmK * (rpmJump > 0 ? (int) rpmJump : 1));
						turboBlow.render(renderer);

						rpmK++;
					}
				}

				turboBlow.setTopleft(oldTurboBlowTopLeft);
				turboBlow.leanRight = true;

				if (!lookBehind) {

					meterPointerRotation(tachoPointer, tachoPointerOrigo, tachoPointerProperPosition,
							car.getTachometer());
					renderer.renderOrthoMesh(tachoPointer);

					tachoSpeed.render(renderer);

					if (!burnout) {
						/*
						 * gearbox
						 */
						gearbox.render(renderer);
						if (player.car.getStats().sequentialShift)
							tachoGear.render(renderer);

						var lifes = this.lifes;
						int i = 0;
						while (lifes > 0f) {

							var heart = (lifes < 1f ? halfHeart : this.heart);
							heart.setPositionX(heart.getWidth() * (i % 5) + leftPadding);
							heart.setPositionY(raceInfoHeight * (1.1f + (float) Math.floor((float) i / 5f)));
							renderer.renderOrthoMesh(heart);
							lifes--;
							i++;
						}
					}

				}
			}
		}
		if (!burnout)
			racelights(renderer);
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {

		if (player.role == Player.COMMENTATOR)
			return;
		
		Features.inst.pushFontColor(ctx, UIColors.WHITE);
		
		playerListPlacement.layout(ctx, stack);
		
		if (!burnout) {
			raceInfo(ctx, stack);

			gearboxTimeShift.layout(ctx, stack);

			if (tireboostWindow.begin(ctx)) {
				nk_layout_row_dynamic(ctx, infoRowHeight, 1);
				tireboostInfoLabel.layout(ctx, stack);
			}
			nk_end(ctx);
		}

		if (player.car.hasTurbo()) {
			if (turboBarWindow.begin(ctx)) {
				var stats = player.car.getStats();
				nk_layout_row_dynamic(ctx, infoRowHeight, 1);
				Nuklear.nk_label(ctx,
						"+" + 
								Texts.formatNumber(Car.funcs.turboHorsepower(stats, player.car.getRep(), 1d, false))
						+ " HP",
						Nuklear.NK_TEXT_ALIGN_LEFT);
				nk_layout_row_dynamic(ctx, infoRowHeight, 1);
				Nuklear.nk_label(ctx, 
						Texts.formatNumber(stats.spool * stats.stats[Rep.bar] * (stats.turboBlowON ? Car.funcs.turboFactor(stats) + stats.stats[Rep.turboblowStrength] : 1d)) + " bar",
						Nuklear.NK_TEXT_ALIGN_LEFT);
			
			}
			nk_end(ctx);
		}

//		road.init(3, 100);

		if (tireboostInfoShow && tireboostInfoShowTime < System.currentTimeMillis()) {
			tireboostInfoShow = false;
			tireboostInfoLabel.setText("");
		}

		if (ShowHints && !InputHandler.CONTROLLER_EFFECTIVELY) {
			Features.inst.pushBackgroundColor(ctx, UIColors.BLACK_TRANSPARENT);
			if (hintsWindow.begin(ctx)) {
				nk_layout_row_dynamic(ctx, hintsWindow.height * 0.7f, 1);
				hintsLabel.layout(ctx, stack);
			}
			nk_end(ctx);
			Features.inst.popBackgroundColor(ctx);
		}

//		if (stabilityWindow.begin(ctx)) {
//			nk_layout_row_dynamic(ctx, stabilityWindow.height * 0.7f, 1);
//			stabilityWindow.setPosition(stabilityWindow.x, Window.HEIGHT * 0.5f + stabilityWindow.height);
//			stabilityLabel.setText("Stability: " + (int) (player.car.getStats().stability * 100.0) + "   " + ((int) ((player.car.getStats().stabilityPunishment - 1.0) * 100.0)) + "%");
//			stabilityLabel.options = Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE; 
//			stabilityLabel.layout(ctx, stack);
//		}
//		nk_end(ctx);

		Features.inst.popFontColor(ctx);
	}

	private void zoomMyCar(Camera cam, CarStats stats, boolean moving, float tickFactor) {

//		float zoom = 1;
		float spdInc = (float) (player.car.getStats().spdinc / tickFactor) * 0.5f;

		float diff = spdInc - spdIncActual;
//		System.out.println("spdIncActual: " + spdIncActual + ", diff: " + diff + ", spdInc: " + spdInc);
		if (Math.abs(diff) > 0.3) {
			spdIncActual = spdIncActual + diff * 0.5f;
		} else {
			spdIncActual = spdInc;
		}
		spdInc = spdIncActual;
		var lesserBound = -Features.ran.nextFloat(14.85f, 15.15f);
		if (spdInc < lesserBound)
			spdInc = lesserBound;

		float zoom = 2.3f;
		if (moving) {
			zoom = zoom / (spdInc * 0.1f * (1 - (float) player.car.getStats().clutchPercent) + 1);
			if (zoom < 1.05f)
				zoom = 1.05f;
			// 4 -> 1
		}
		myCarBase.setScale(new Vec3(zoom, zoom, 0));
		currentHands.setScale(new Vec3(zoom, zoom, 0));
//		System.out.println(baseMyCar.getScale().toString() + ", spdinc " + player.getCar().getStats().spdinc);

		if (!SceneHandler.freeCam) {
			var pos = cam.getPosition();
			pos.y = baseCamPosY + spdInc * 0.05f;
			var rot = cam.getRotation();
			rot.y = -180;
			rot.x = spdInc;
		}

		spdInc *= 10f;
		float carPosY = baseMyCarPos.y - spdInc;

		var carX = (((float) Window.WIDTH / (float) Window.HEIGHT) - (16f / 9f));
		carX *= (float) Window.HEIGHT / 2f;

		myCarBase.setPosition(new Vec2(carX, carPosY));
		currentHands.setPosition(new Vec2(carX, carPosY));
		background.setPositionY(-carPosY);
		background.setPositionX(carX);

//		myCarWheel.resetTransformation();
//		final int wheelRotStd = 30;
//		zoom = 1;
//		myCarWheel.setScale(new Vec3(zoom, zoom, 0));
//		myCarWheel.setPositionX(Window.WIDTH / 2f - myCarWheel.getWidth() / 2f);
//		myCarWheel.setPositionY(Window.HEIGHT / 2f - myCarWheel.getHeight() / 2f);
//		myCarWheel.setRotationZ((stats.stabilityLeft ? -wheelRotStd : 0) + (stats.stabilityRight ? wheelRotStd : 0));
//		myCarWheel.updateTransformation();
//		
//		float y = carPosY + Window.HEIGHT / 4f * zoom;
//		myCarWheel.setPositionY(y);
////		System.out.println(carPosY + ", " + y + ", " + zoom);
//		
//		myCarWheel.updateTransformation();
	}

	private void shakeHighSpeed(double speed) {
		Vec2 ogPos = myCarBase.position();
		double shakeBoundry = 0.4;
		float x = shakeValue(speed, -shakeBoundry, shakeBoundry);
		float y = shakeValue(speed, -shakeBoundry, shakeBoundry);
		myCarBase.setPosition(new Vec2(ogPos.x + x, ogPos.y + y));
//		System.out.println(baseMyCarPos.y() + ", " + y);
	}

	/**
	 * shakes values to a value from 0 to 1.
	 */
	private float shakeValue(double comparedValue, double fromValue, double tillValue) {

		double scaleForDecimals = 1000;
		int maxRandomBoundryScaled = (int) ((Math.abs(fromValue) + Math.abs(tillValue)) * scaleForDecimals);
		double res = Features.ran.nextInt(maxRandomBoundryScaled) / scaleForDecimals - fromValue;

		double significanceShake = comparedValue / 8000;

		if (significanceShake > 1) {
			significanceShake = 1;
		}

		res *= significanceShake;

		return (float) res;
	}

	private void rotateIdle(GameObject go, float comparedValue, float shake) {
		double finetuneShake = 16.0;
		comparedValue = comparedValue * comparedValue;

		int shakeFrom = (int) (shake * 100 * comparedValue);
		if (shakeFrom < 1)
			shakeFrom = 1;

		double ranShake = Features.ran.nextInt(shakeFrom) / (100 * finetuneShake);
		double degrees = ranShake - (shake / (2 * finetuneShake));

		go.setRotationZ((float) degrees);
	}

	private void raceInfo(NkContext ctx, MemoryStack stack) {
		NkVec2 spacing = NkVec2.malloc(stack);
		NkVec2 padding = NkVec2.malloc(stack);

		spacing.set(0, infoSpacingY);
		padding.set(infoPaddingX, infoPaddingY);

		nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
		nk_style_push_vec2(ctx, ctx.style().window().padding(), padding);

		NkRect rect = NkRect.malloc(stack);
		rect.x(0).y(0).w(Window.WIDTH).h(raceInfoHeight);

		Nuklear.nk_window_set_focus(ctx, "raceInfo");
		if (nk_begin(ctx, "raceInfo", rect, Nuklear.NK_WINDOW_NO_SCROLLBAR | Nuklear.NK_WINDOW_NO_INPUT)) {
			Nuklear.nk_style_push_font(ctx, infoFont.getFont());

			nk_layout_row_dynamic(ctx, infoRowHeight, 2);
			lapsedDistance.layout(ctx, stack);
			lapsedTime.layout(ctx, stack);
			nk_layout_row_dynamic(ctx, infoRowHeight, 2);
			currentDistance.layout(ctx, stack);
			extraGamemodeInfo.layout(ctx, stack);

			Nuklear.nk_style_pop_font(ctx);
		}
		nk_end(ctx);

		Nuklear.nk_style_pop_vec2(ctx);
		Nuklear.nk_style_pop_vec2(ctx);
	}

	private void meterPointerRotation(GameObject pointer, Vec2 tachometerPointerOrigo,
			Vec2 tachometerPointerProperPosition, float rotation) {
		pointer.setPosition(new Vec3(tachometerPointerOrigo));
		pointer.setRotation(new Vec3(0, 0, rotation));
		pointer.updateTransformation();

		pointer.setPosition(new Vec3(tachometerPointerProperPosition));
		pointer.updateTransformation();
	}

	private void racelights(Renderer renderer) {
		if (hasBeenGreen && ballcount == 0)
			return;

		int amount = ballcount;
		boolean green = false;

		if (ballcount == GameMode.raceLightsLength) {
			// green racelights
			green = true;
			hasBeenGreen = true;
			amount--;
		}

		float posW = -racelight.getWidth();
		float posX = 2f*posW;

		for (int i = 0; i < GameMode.raceLightsLength - 1; i++) {
			racelight.getShader().setUniform("green", green);
			racelight.getShader().setUniform("darkAlpha", i < amount ? 1f : .4f);
			racelight.setPositionX(posX + Math.abs(posW) * i);
			renderer.renderOrthoMesh(racelight);
		}
	}

	private void tireboost(Car car, Renderer renderer) {
		if (!car.hasTireboost() || !car.isTireboostRunning())
			return;

		float width = tireboost.getWidth();
		float left = tireboost.position().x;
		float percent = car.getStats().getTBPercentageLeft();
		float tbLevel = width * percent + left;
//		System.out.println("tbLevel: " + tbLevel + " vs. " + (Window.WIDTH / 2f));

		tireboost.getShader().setUniform("tbAmountLevelPositionX", tbLevel);

		tireboost.getShader().setUniform("hitTB", car.isTireboostRight());
		renderer.renderOrthoMesh(tireboost);
	}

	private void nosbottles(Car car, Renderer renderer) {
		if (!car.hasNOS())
			return;

		float posX = leftPadding;

		for (int i = 0; i < car.getRep().get(Rep.nosBottles); i++) {
			nosbottle.setPositionX(posX);
			float height = nosbottle.getHeight();
			float diff = height * 7f / 40f;
			height = height - diff;
			float top = Window.HEIGHT - nosbottle.position().y - diff;
			float bot = top - height;
			float percent = car.getStats().getNosPercentageLeft(i);
			float nosLevel = top * percent + bot * (1f - percent);

			nosbottle.getShader().setUniform("nosAmountLevelPositionY", nosLevel);

			nosbottle.getShader().setUniform("nosStrength", nosStrengthColor);
			renderer.renderOrthoMesh(nosbottle);
			posX += nosbottle.getWidth() * 1.1f;
		}
	}

	@Override
	public void keyInput(int keycode, int action) {
		if (player.role == Player.COMMENTATOR) {
			if (action != GLFW.GLFW_RELEASE) {
				var pos = SceneHandler.cam.getPosition();
				var rot = SceneHandler.cam.getRotation();
				SceneHandler.cam.movespeed = 1;
				switch (keycode) {
				case GLFW.GLFW_KEY_1:
					pos.z = 0;
					pos.x = -pos.x;
					rot.y = (rot.y - 180f) % 360f;
					break;
				case GLFW.GLFW_KEY_2:
					pos.z = race.getCurrentLength() * .25f;
					pos.x = -pos.x;
					rot.y = (rot.y - 180f) % 360f;
					break;
				case GLFW.GLFW_KEY_3:
					pos.z = race.getCurrentLength() * .5f;
					pos.x = -pos.x;
					rot.y = (rot.y - 180f) % 360f;
					break;
				case GLFW.GLFW_KEY_4:
					pos.z = race.getCurrentLength() * .75f;
					pos.x = -pos.x;
					rot.y = (rot.y - 180f) % 360f;
					break;
				case GLFW.GLFW_KEY_5:
					pos.z = race.getCurrentLength() - 5;
					pos.x = -pos.x;
					rot.y = (rot.y - 180f) % 360f;
					break;
				}
			}
		}
	}

	public void setStartboostTime(long reactionTime, long timeloss) {
		if (!tireboostInfoShow) {
			tireboostInfoLabel.setText(reactionTime + "ms | - " + Math.abs(timeloss) + "%");
			tireboostInfoShow = true;
			tireboostInfoShowTime = System.currentTimeMillis() + player.getCarRep().getInt(Rep.tbMs) + 2000;
		}
	}

	public void setStartboostTime(int reactionTime) {
		if (!tireboostInfoShow) {
			tireboostInfoLabel.setText(reactionTime + "ms");
			tireboostInfoShow = true;
			tireboostInfoShowTime = System.currentTimeMillis() + 2000;
		}
	}

	public void setExtraGamemodeInfoText(GameMode gm) {
		extraGamemodeInfo.setText(gm.getExtraGamemodeRaceInfo());
	}

	public void setWarning(String string) {
		tireboostInfoLabel.setText(string);
	}

	public Gearbox getGearbox() {
		return gearbox;
	}

	public void addLog(String s) {
		gearbox.addLog(s);
	}

	@Override
	public void controllerInput() {
		mouseDelay = 100;
	}

}
