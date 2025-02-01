package scenes.game.lobby_subscenes;

import adt.IActionDouble;
import audio.SfxTypes;
import comNew.LocalRemote2;
import communication.Translator;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Model;
import engine.graphics.objects.Sprite;
import engine.graphics.ui.*;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import engine.math.Vec2;
import engine.math.Vec3;
import game_modes.GameMode;
import game_modes.GameModes;
import game_modes.SingleplayerChallenges;
import game_modes.SingleplayerChallengesMode;
import main.Features;
import main.Main;
import main.ResourceHandler;
import main.Texts;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;
import player_local.car.CarModel;
import player_local.car.Rep;
import player_local.upgrades.TileVisual;
import player_local.Player;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Subscene;

import java.util.function.Consumer;

import static org.lwjgl.nuklear.Nuklear.nk_end;

/**
 * Shows upgrades for the engine. Have a different one for boost and fuel
 *
 * @author Jens Benz
 */

public class CarChoiceSubscene extends Subscene {

    public static final Model[] CARS = new Model[Texts.CAR_TYPES.length];
    public static final Sprite[] CHARACTERS = new Sprite[Texts.CAR_TYPES.length];
    private float rowHeight, rowSpacingY;

    public int selectedCarIndex;
    private final UIButton<Boolean> prevCarBtn, nextCarBtn;

    private final UIWindowInfo carSelectionWindow, backstoryWindow, gamemodeInfoWindow;

    private final UIScrollable carInfoWindow;

    private float rotation;
    private final UIButton<Integer> prevGamemodeBtn, nextGamemodeBtn;
    private final UIButton<?> backstoryBtn, ipBtn;
    // private final ArrayList<IUIObject> gameModeInformation;
//	private final Road road = new Road();

    private UIScrollable pricesWindow;
    private boolean showingCarInfo;
//	private float[] oldPrices;

    public CarChoiceSubscene(int sceneIndex) {
        super(sceneIndex);

//		gameModeInformation = new ArrayList<>();

        float x = Window.WIDTH / 4;
        float w = Window.WIDTH - 2f * x;
        float h = Window.HEIGHT / 10f;
        float y = Window.HEIGHT - h;

        float size = Window.HEIGHT / 2.5f;

        carSelectionWindow = UISceneInfo.createWindowInfo(Scenes.LOBBY, x, y, w, h);
        var paddingSides = Window.WIDTH * 0.15f;
        gamemodeInfoWindow = UISceneInfo.createWindowInfo(Scenes.LOBBY, paddingSides, 0,
                Window.WIDTH - 2f * paddingSides, Window.HEIGHT / 2.8f);
        carInfoWindow = new UIScrollable(sceneIndex, 0, Window.HEIGHT - size, size, size);
        carInfoWindow.getWindow().options = gamemodeInfoWindow.options = Nuklear.NK_WINDOW_BORDER
                | Nuklear.NK_WINDOW_NO_SCROLLBAR;

        backstoryWindow = createWindow(0, 0, 0, 0);

        w = size * 0.6f;
        h = size * 1.2f;
        var pricesHeight = Window.HEIGHT / 2.8f;
        pricesWindow = new UIScrollable(sceneIndex, 0, 0, w, pricesHeight);
        pricesWindow.getWindow().options = gamemodeInfoWindow.options = Nuklear.NK_WINDOW_BORDER
                | Nuklear.NK_WINDOW_NO_SCROLLBAR;

        backstoryBtn = new UIButton<>("Backstory");
        backstoryBtn.setPressedAction(() -> {
            if (showingCarInfo) {
                carInfoWindow.clear();
                carInfoWindow.setText(UILabel.split(Texts.backstories[selectedCarIndex], "\n"));
                showingCarInfo = false;
            } else {
                updateCarInfoUI(com.player.getCarRep());
            }
            Features.inst.getAudio().play(SfxTypes.REGULAR_PRESS);
        });
        ipBtn = new UIButton<>("Show my IP");
        ipBtn.setPressedAction(() -> {
            Features.inst.getAudio().play(SfxTypes.REGULAR_PRESS);
            SceneHandler.showMessage(LocalRemote2.getLocalIP());
        });

        prevCarBtn = new UIButton<>("Previous car");
        nextCarBtn = new UIButton<>("Next car");
        prevCarBtn.setConsumerValue(false);
        nextCarBtn.setConsumerValue(true);
        prevGamemodeBtn = new UIButton<>("Previous gamemode", UIColors.GRAY);
        nextGamemodeBtn = new UIButton<>("Next gamemode", UIColors.GRAY);

        prevGamemodeBtn.setConsumerValue(-1);
        nextGamemodeBtn.setConsumerValue(+1);

        add(prevCarBtn);
        add(nextCarBtn);
        add(prevGamemodeBtn);
        add(nextGamemodeBtn);
        add(backstoryBtn);
        add(ipBtn);

        selectedCarIndex = 0;

        if (!ResourceHandler.running)
            return;

        for (int i = 0; i < CARS.length; i++) {
            final int carID = i;
            ResourceHandler.LoadSprite(new Vec2(0), TileVisual.size(),
                    "./images/" + Texts.CAR_TYPES[i] + "_character.png", "main", (s) -> {
                        CHARACTERS[carID] = s;
                    });
            if (Texts.isRandomCar(i))
                continue;
            CARS[i] = CarModel.createModel(Texts.CAR_TYPES[i]); // TODO last inn modeller bare en gang
        }

//		road.init(1);
    }

    @Override
    public void init() {
        nextCarBtn.setPressedAction(prevCarBtn.setPressedAction((forward) -> {
            audio.play(SfxTypes.NEXTCAR);
            if (forward) {
                selectedCarIndex = (selectedCarIndex + 1) % CARS.length;
            } else {
                if (selectedCarIndex == 0)
                    selectedCarIndex = CARS.length - 1;
                else
                    selectedCarIndex--;
            }
            if (Texts.isRandomCar(selectedCarIndex))
                com.carSelectUpdate(com.player, Features.ran.nextInt(CARS.length - 1), true, true, true);
            else
                com.carSelectUpdate(com.player, selectedCarIndex, true, false, true);

            updateCarInfoUI(com.player.getCarRep());
        }));

//		prevCarBtn.setNavigations(null, () -> nextCarBtn, null, () -> readyBtn);
//		nextCarBtn.setNavigations(() -> prevCarBtn, () -> readyBtn, null, () -> readyBtn);
    }

    public void initGameModeManipulation(IActionDouble<Player, Integer> gamemodeChange) {
        Consumer<Integer> actualGamemodeChange = (i) -> {
            gamemodeChange.run(com.player, i);
        };
        prevGamemodeBtn.setPressedAction(actualGamemodeChange);
        nextGamemodeBtn.setPressedAction(actualGamemodeChange);
    }

    @Override
    public void updateGenerally(Camera cam, int... args) {
        for (var car : CARS) {
            if (car != null)
                car.resetTransformation();
        }

        rotation = 0;
        if (cam == null)
            return;
        var pos = cam.getPosition();
        pos.x = 0f;
        pos.y = 0.4f;
        pos.z = 8f;
        var rot = cam.getRotation();
        rot.x = 2.2f;
        rot.y = 0f;
        rot.z = 0f;
//		road.newRace(240, 0);
        float heightBackstory = Window.HEIGHT / 28f;
        float widthBackstory = Window.WIDTH / 14f;
        backstoryWindow.setPositionSize(0, carInfoWindow.getWindow().getYHeight() - heightBackstory,
                2f*widthBackstory, heightBackstory);
        press();
    }

    @Override
    public void updateResolution() {
        rowSpacingY = Window.HEIGHT / 192.75f;
        rowHeight = Window.HEIGHT / 16f;
    }

    public void updateCarInfoUI(Rep rep) {
        carInfoWindow.clear();
        if (rep.isRandom()) {
            carInfoWindow.setText("You are any one of the cars.");
        } else {
            carInfoWindow.setText(rep.getCarChoiceInfo());
        }
        showingCarInfo = true;
    }

    @Override
    public void createWindowsWithinBounds(float x, float y, float w, float h, float ssX) {
    }

    @Override
    public void keyInput(int keycode, int action) {
        if (action == GLFW.GLFW_PRESS) {
            if (keycode == GLFW.GLFW_KEY_RIGHT) {
                nextCarBtn.runPressedAction();
            } else if (keycode == GLFW.GLFW_KEY_LEFT) {
                prevCarBtn.runPressedAction();
            }
        }
    }

    @Override
    public void controllerInput() {
        if (InputHandler.HOLDING)
            return;

        if (InputHandler.BTN_RIGHT && (com.player.isHost() || !com.isCoop())) {
            nextCarBtn.runPressedAction();
        } else if (InputHandler.BTN_LEFT && (com.player.isHost() || !com.isCoop())) {
            prevCarBtn.runPressedAction();
        } else if (!com.isSingleplayer() && com.player.isHost()) {
            if (InputHandler.BTN_BACK_TOP_LEFT || InputHandler.LEFT_TRIGGER > 0 && !prevGamemodeBtn.isPressed()) {
                prevGamemodeBtn.runPressedAction();
                prevGamemodeBtn.press();
            } else if (InputHandler.BTN_BACK_TOP_RIGHT
                    || InputHandler.RIGHT_TRIGGER > 0 && !nextGamemodeBtn.isPressed()) {
                nextGamemodeBtn.runPressedAction();
                nextGamemodeBtn.press();
            } else if (InputHandler.RIGHT_TRIGGER < 0 && nextGamemodeBtn.isPressed()) {
                nextGamemodeBtn.release();
            } else if (InputHandler.LEFT_TRIGGER < 0 && prevGamemodeBtn.isPressed()) {
                prevGamemodeBtn.release();
            }
        }
    }

    @Override
    public void mouseScrollInput(float x, float y) {
    }

    @Override
    public void mousePositionInput(float x, float y) {
    }

    @Override
    public void tick(float delta) {
        if (com.isCoop() && !com.player.isHost()) {
            var rep = com.getHost().getCarRep();
            var hostSelectedCarIndex = rep.isRandom() ? Texts.CAR_TYPES.length - 1 : rep.getNameID();
            if (hostSelectedCarIndex != selectedCarIndex) {
                selectedCarIndex = hostSelectedCarIndex;
                com.player.car.switchTo(selectedCarIndex, rep.isRandom());
            }
            if (nextCarBtn.isEnabled()) {
                nextCarBtn.setEnabled(false);
                prevCarBtn.setEnabled(false);
            }
        } else if (!nextCarBtn.isEnabled()) {
            nextCarBtn.setEnabled(true);
            prevCarBtn.setEnabled(true);
        }
//		updateGenerally(SceneHandler.cam);
        if (Texts.isRandomCar(selectedCarIndex))
            return;
        rotation += 3f * delta;
        CARS[selectedCarIndex].rotation().y = rotation;
//		CARS[selectedCarIndex].rotation().z = -0.7f;
//		System.out.println(selectedCarIndex);

//		CARS[selectedCarIndex].position().z = 4.5f;
//		CarModel.bumDown(CARS[selectedCarIndex], 10);
        CarModel.rotateWheels(CARS[selectedCarIndex], null, 5 * delta);
    }

    @Override
    public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
//		 renderer.renderOrthoMesh(characters[selectedCarIndex]);
//		 GL11.glClearColor(0.5f, 0.5f, 0.5f, 1);
//		 renderer.renderOrthoMesh(backgroundImage);
//		 road.render(renderer, cam, delta, cam.getPosition().z, new Vec3(0), 0);
        if (!Texts.isRandomCar(selectedCarIndex))
            CARS[selectedCarIndex].render(renderer, cam);
//		 System.out.println("z: " + cam.getPosition().z);
//		 road.newRace(240, 0);
//		 Window.HEIGHT - size
        var myIcon = CHARACTERS[selectedCarIndex];
        myIcon.setPositionY(carInfoWindow.getWindow().y - myIcon.getHeight());
        myIcon.setPositionX(0);
        renderer.renderOrthoMesh(myIcon);
    }

    @Override
    public void renderUILayout(NkContext ctx, MemoryStack stack) {
        if (com == null || com.getGamemode() == null)
            return;

        Features.inst.pushFontColor(ctx, UIColors.WHITE);
        Features.inst.pushBackgroundColor(ctx, UIColors.BLACK);

        var gm = com.getGamemode();
        var infos = gm.getGameModeInformation();
        gamemodeInfoWindow.setHeight((infos.length + 1) * rowHeight * 0.55f);
        if (gamemodeInfoWindow.begin(ctx)) {
            if (com.player != null && com.player.isHost()) {
                if (gm.isCanSwitchBetweenGamemodes()) {
                    Nuklear.nk_layout_row_dynamic(ctx, rowHeight / 2f, 3);
                    prevGamemodeBtn.layout(ctx, stack);
                    Nuklear.nk_label(ctx, gm.getName(), Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);
                    nextGamemodeBtn.layout(ctx, stack);
                }
            } else {
                Nuklear.nk_layout_row_dynamic(ctx, rowHeight / 2f, 1);
                Nuklear.nk_label(ctx, gm.getName(), Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);
            }

            for (var info : infos) {
                Nuklear.nk_layout_row_dynamic(ctx, rowHeight / 2f, 1);
                info.layout(ctx, stack);
            }
        }
        nk_end(ctx);

//	        if (carInfoWindow.begin(ctx, stack, 0, 0, rowSpacingY, 0)) {
//	            for (var info : carInfo) {
//	                Nuklear.nk_layout_row_dynamic(ctx, rowHeight / 2f, 1);
//	                info.layout(ctx, stack);
//	//				labelImageTest.layout(ctx, stack);
//	            }
//	        }
//	        nk_end(ctx);
//        if (!Texts.isRandomCar(selectedCarIndex)) {
            carInfoWindow.layout(ctx, stack);
//        }

        backstoryWindow.focus = true;
        Features.inst.pushFontColor(ctx, UIColors.RAISIN_BLACK);
        if (backstoryWindow.begin(ctx)) {
            Nuklear.nk_layout_row_dynamic(ctx, backstoryWindow.height * .9f, 2);
            if (!Texts.isRandomCar(selectedCarIndex)) {
                backstoryBtn.layout(ctx, stack);
            } else {
                Nuklear.nk_label(ctx, "", 0);
            }
            if (com.isLAN()) {
                ipBtn.layout(ctx, stack);
            }
        }
        nk_end(ctx);
        Features.inst.popFontColor(ctx);

        if (!com.isSingleplayer()) {
//        	pricesWindow.getWindow().setPositionSize(selectedCarIndex, rowSpacingY, rowSpacingY, rowHeight);
//        	Window.WIDTH - w, Window.HEIGHT - gamemodeInfoWindow.height, w, gamemodeInfoWindow.height
//        	 Window.HEIGHT / 2.8f        	
            pricesWindow.layout(ctx, stack);
        }

        Features.inst.popBackgroundColor(ctx);
        Features.inst.popFontColor(ctx);

        if (com.player != null) {
            if (carSelectionWindow.begin(ctx)) {
                Nuklear.nk_layout_row_dynamic(ctx, carSelectionWindow.height * 0.9f, 3);
                prevCarBtn.layout(ctx, stack);
                boolean lockedIn = com.player.ready != 0;
                if (lockedIn)
                    Features.inst.pushFontColor(ctx, UIColors.WHITE);
                readyBtn.layout(ctx, stack);
                if (lockedIn)
                    Features.inst.popFontColor(ctx);
                nextCarBtn.layout(ctx, stack);
            }
            nk_end(ctx);
            if (com.player.ready != 0) {
                readyBtn.setTitle(Texts.readySimple(true));
                readyBtn.setColorUI(UIColors.WON);
            } else {
                readyBtn.setTitle(Texts.readySimple(false));
                readyBtn.setColorUI(UIColors.R);
            }
        }

    }

    @Override
    public void createBackground() {
        ResourceHandler.LoadSprite("./images/back/carselection.png", "background",
                (sprite) -> backgroundImage = sprite.setToFullscreen());
    }

    @Override
    public UIButton<?> intoNavigationSide() {
        return nextCarBtn;
    }

    @Override
    public UIButton<?> intoNavigationBottom() {
        return nextCarBtn;
    }

    @Override
    public void destroy() {
        for (var m : CARS) {
            if (m != null)
                m.destroy();
        }
        for (Sprite s : CHARACTERS)
            s.destroy();
    }

    @Override
    public void setVisible(boolean visible) {
        carInfoWindow.getWindow().visible = visible;
    }

    public void afterJoined() {
        selectedCarIndex = com.player.getCarNameID();
        updateCarInfoUI(com.player.getCarRep());
        if (com.isSingleplayer()) {
            var challengeLvl = ((SingleplayerChallengesMode) com.getGamemode()).getChallengeLevel();
            if (challengeLvl == SingleplayerChallenges.TheBoss.ordinal())
                backgroundImage.getShader().setUniform("tint", new Vec3(.0f, 0f, 0f), .8f);
            else if (challengeLvl >= SingleplayerChallenges.Nightmarish.ordinal() && challengeLvl < Texts.singleplayerModes.length)
                backgroundImage.getShader().setUniform("tint", new Vec3(.5f, 0f, 0f), .5f);
            else backgroundImage.getShader().setUniform("tint", new Vec3(0f, 0f, 0f), 0);
        } else {
            backgroundImage.getShader().setUniform("tint", new Vec3(0f, 0f, 0f), 0);
        }
    }

    public void updatePrices() {
        pricesWindow.clear();
        pricesWindow.addText("Prices:");
        for (var up : com.player.upgrades.getUpgradesNonNulls()) {
            pricesWindow.addText("$" + up.getCost(1) + " - " + Texts.getUpgradeTitle(up));
        }
    }

}
