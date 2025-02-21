package scenes.game.lobby_subscenes;

import adt.IAction;
import audio.SfxTypes;
import communication.GameInfo;
import communication.Translator;
import elem.Animation;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Sprite;
import engine.graphics.ui.*;
import engine.graphics.ui.modal.UIConfirmModal;
import game_modes.SingleplayerChallenges;
import player_local.upgrades.*;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import engine.math.Vec2;
import engine.math.Vec3;
import engine.utils.Timer;
import game_modes.SingleplayerChallengesMode;
import main.Features;
import main.Main;
import main.ResourceHandler;
import main.Texts;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import player_local.Bank;
import player_local.car.Rep;
import player_local.Layer;
import player_local.Player;
import player_local.TilePiece;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Subscene;
import scenes.regular.HotkeysScene;
import settings_and_logging.RSet;
import settings_and_logging.hotkeys.CurrentControls;

import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

import static org.lwjgl.nuklear.Nuklear.*;

/**
 * visualizer for store - main communicator between the two.
 *
 * @author Jens Benz
 */

public class UpgradesSubscene extends Subscene {

    private final Vec3[] backgroundColors = new Vec3[] {
            new Vec3(118.403435f, 74.34054f, 142.61656f),
            new Vec3(107.611496f, 32.288006f, 85.28174f),
            new Vec3(72.702286f, 100.3498f, 179.80923f),
            new Vec3(51.197937f, 126.468796f, 114.72961f),
            new Vec3(55.492f, 121.19009f, 113.4204f),
            new Vec3(38.144283f, 170.47789f, 160.38531f),
            new Vec3(48.12183f, 33.468117f, 179.66324f),
            new Vec3(53.675396f, 123.51192f, 122.30012f),
            new Vec3(50.597694f, 41.19589f, 172.18161f),
            new Vec3(120.22266f, 36.750954f, 85.33477f),
            new Vec3(145.02136f, 42.057884f, 135.05023f),
            new Vec3(153.74042f, 52.151222f, 95.637054f),
            new Vec3(115.21764f, 58.899315f, 145.84247f),
            new Vec3(45.576187f, 83.16505f, 152.7133f),
            new Vec3(56.679703f, 115.762054f, 98.96431f),
            new Vec3(124.31613f, 51.715477f, 153.22528f),
            new Vec3(158.96307f, 37.602905f, 113.80214f),
            new Vec3(38.708755f, 38.7259f, 174.47873f),
    };

    private int currentBackground, nextBackground;
    private boolean currentBackgroundUpsideDown, nextBackgroundUpsideDown;
    private float backgroundAlpha;
    private Sprite[] extraBackgroundImages;
    private float backgroundWait;

    private boolean startBuyAnimation;
    private int tileSpriteSpeed = 100;
    private long nextTileSpriteFrameSwitch = 0;
    public static int TileSpriteFrame = 0;
    public static Sprite[] TileSprites;
    public static Sprite[] TileSprites2;
    public static Sprite[] TileSprites3;
    public static Sprite[] TileSprites4;
    private static UIFont saleFont, immediateUpgradesFont;
    public static float marginX, marginY,
    // spacing = 1 + (1f / 24f), // 4px
    spacing, // 8px
    // spacingBig = 1f + (1f/2f); // 48px
    spacingBig; // 64px
    private static TileVisual nullTile;

    private boolean needToUpdateBackgroundColor;

    private static int tooltipTime = 0;

    public static TileVisual currentUpgrade;
    // public Queue<TileVisual> prevUpgrades;
    // TODO Burde ha en liste med tidligere oppgraderinger man hovret over og så
    // rendre alle sammen med gradvis synking av alfa.
    // TODO custom tooltip der en sjekker om en label har blitt hoveret og så rendre
    // en generell tooltip greie basert på xy som står inn under InputHandler.
    private TileVisual pressedTile;
    private final HashMap<Integer, IAction> extraStateChangeActions;
    private UIScrollable upgradeInfoDetails;
    private static UIScrollable regValsTooltip;
    private UIScrollable unlocks;
    private static boolean showExtraTooltip;
    private int detailsScrollIndex;
    private Store store;
    private UIWindowInfo historyWindow, tabsWindow, improvementsWindow;
    private final UIButton<?> undoBtn, redoBtn, historyHomeBtn, historyEndBtn, historyFwdBtn, historyBckBtn, historyFwdRoundBtn, historyBckRoundBtn;
    private final UIButton<TileVisual> improveTileBtn, sellTileBtn, runToolBtn;
    private final UIButton<UIButton<?>> tileUpgradesBtn, unlocksBtn;
    private UIButton<?> upgradeTabsCurrentBtn;
    private final UIRow upgradeTabsRow, placedTileBtnRow;
    private Animation moneyAni;
//    private Sprite moneySprite; // , boltsSprite;
    private UIWindowInfo moneyWindow, improveButtonsWindow; // , boltsWindow;
    public UIWindowInfo menu = null;
    private UIWindowInfo carInfoWindow;
    private Sprite backgroundLayer, backgroundLayer2;
    private UILabel[] carInfo = new UILabel[0];
    private int lastCarInfoHovered = -1;
    private Animation arrow;

    private boolean hasHoveredOverLayer, hasHoveredOverUpgrades;

    private boolean showedExplaination;

    private long longestTimeShowUpgrades;
    private float upgradeInfoDetailsX, upgradeInfoDetailsY, upgradeInfoDetailsW, upgradeInfoDetailsH,
            upgradeInfoDetailsH2;

    private float tileRandomFactor;

    public boolean viewPlayerMinimally;
    public byte viewPlayerId = -1;
    public boolean regValsTooltipRender;

    private long controllerMovingX, controllerMovingY;
    private int controllerX, controllerY;
    private float controllerCursorX, controllerCursorY;
    private long controllerScroll;
	private boolean hasBought;
    private final CurrentControls controls = CurrentControls.getInstance();
    public UpgradesSubscene(int sceneIndex) {
        super(sceneIndex);

        extraStateChangeActions = new HashMap<>();

        if (saleFont == null) {
//			costFont = new UIFont(Font.BOLD_ITALIC, Window.HEIGHT / 28);
            saleFont = new UIFont(Font.BOLD_REGULAR, Window.HEIGHT / 58);
            immediateUpgradesFont = new UIFont(Font.BOLD_ITALIC, Window.HEIGHT / 44);
        }

        undoBtn = new UIButton<>(Texts.undo, UIColors.UNBLEACHED_SILK);
        redoBtn = new UIButton<>(Texts.redo, UIColors.UNBLEACHED_SILK);
        undoBtn.trueHover = true;
        redoBtn.trueHover = true;
        historyFwdBtn = new UIButton<>(Texts.historyFwd);
        historyFwdBtn.tooltip = "Hotkey: Right-arrow ->";
        historyBckBtn = new UIButton<>(Texts.historyBck);
        historyBckBtn.tooltip = "Hotkey: Left-arrow <-";
        historyHomeBtn = new UIButton<>(Texts.historyHome);
        historyHomeBtn.tooltip = "Hotkey: Comma ,";
        historyEndBtn = new UIButton<>(Texts.historyEnd);
        historyEndBtn.tooltip = "Hotkey: Period .";
        historyFwdRoundBtn = new UIButton<>("+1 round");
        historyFwdRoundBtn.tooltip = "Hotkey: M";
        historyBckRoundBtn = new UIButton<>("-1 round");
        historyBckRoundBtn.tooltip = "Hotkey: N";
        improveTileBtn = new UIButton<>(Texts.improveUpgrade, UIColors.TUR);
        runToolBtn = new UIButton<>("Rotate $20", UIColors.AERO_BLUE);
        sellTileBtn = new UIButton<>(Texts.destroyUpgrade);

        tileUpgradesBtn = new UIButton<>(Texts.tileUpgrades);
        unlocksBtn = new UIButton<>(Texts.unlocks);
        upgradeTabsRow = new UIRow(new UIButton[]{tileUpgradesBtn, unlocksBtn}, 0);
        placedTileBtnRow = new UIRow(new UIButton[]{improveTileBtn, sellTileBtn}, 0);

        // Ikke lag mer enn en gang statically
        if (TileSprites != null)
            return;
        
        arrow = new Animation("arrow", "main", 5, 0, Window.HEIGHT / 20f);

        final float size = TileVisual.size();
        var pos = new Vec2(0);
        int len = TileNames.values().length;

        TileSprites = new Sprite[len + 2];
        TileSprites2 = new Sprite[len];
        TileSprites3 = new Sprite[len];
        TileSprites4 = new Sprite[len];

        for (TileNames t : TileNames.values()) {
            int upgradeID = t.ordinal();
            ResourceHandler.LoadSprite(pos, size, "./images/upgrade/upgrade_" + t.toString().toLowerCase() + "1.png",
                    "upgrade", (sprite) -> TileSprites[upgradeID] = sprite);
            ResourceHandler.LoadSprite(pos, size, "./images/upgrade/upgrade_" + t.toString().toLowerCase() + "2.png",
                    "upgrade", (sprite) -> TileSprites2[upgradeID] = sprite);
            ResourceHandler.LoadSprite(pos, size, "./images/upgrade/upgrade_" + t.toString().toLowerCase() + "3.png",
                    "upgrade", (sprite) -> TileSprites3[upgradeID] = sprite);
            ResourceHandler.LoadSprite(pos, size, "./images/upgrade/upgrade_" + t.toString().toLowerCase() + "4.png",
                    "upgrade", (sprite) -> TileSprites4[upgradeID] = sprite);
        }
        ResourceHandler.LoadSprite("./images/back/lobby2.png", "lobbyBackground",
                (sprite) -> backgroundImage = sprite.setToFullscreen());
        extraBackgroundImages = new Sprite[25];
        for (int i = 0; i < extraBackgroundImages.length; i++) {
            int a = i;
            ResourceHandler.LoadSprite("./images/back/lobby" + (i + 3) + ".png", "lobbyBackground",
                (sprite) -> extraBackgroundImages[a] = sprite.setToFullscreen());
        }

        ResourceHandler.LoadSprite(pos, size, "./images/upgrade/upgradeNull.png", "upgrade", (s1) -> {
            TileSprites[len] = s1;
            nullTile = new TileVisual(len);
            ResourceHandler.LoadSprite(pos, size, "./images/upgrade/upgradeFrame.png", "upgrade", (s2) -> {
                TileSprites[len + 1] = s2;
            });
        });

        float bankSize = immediateUpgradesFont.getHeight() * 2.5f;

        moneyAni = new Animation("money", "main", 10, 0, bankSize);

//        ResourceHandler.LoadSprite(pos, bankSize, "./images/money.png", "main", (sprite) -> {
//
//            moneySprite = sprite;
//
//            float bankSpriteHeight = sprite.getHeight();
//            float bankSpriteWidth = sprite.getWidth();
//
//            float y = marginY - bankSpriteHeight * 1f - TileVisual.size() * (spacingBig - 1f);
//            float extraMargin = TileVisual.size() * (spacing - 1f);
//            var bottomRightTilePos = genRealPos(new Vec2(Layer.STD_W, Layer.STD_H), null);
//            float boardXWidth = bottomRightTilePos.x - extraMargin;
//
//            float x = marginX + bankSpriteWidth * 1.1f;
//            moneyWindow = createWindow(x, y, boardXWidth - x, bankSpriteHeight);
//            moneyWindow.options = Nuklear.NK_WINDOW_BORDER | Nuklear.NK_WINDOW_NO_SCROLLBAR;
            moneyWindow = createWindow(0,0,0,0);
            moneyWindow.options = NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR;
//
//            sprite.setPositionX(marginX);
//            sprite.setPositionY(moneyWindow.y);
//        });

//		ResourceHandler.LoadSprite(pos, bankSize,
//				"./images/bolt.png", "main", (sprite) -> {
//
//					boltsSprite = sprite;
//					
//					float bankSpriteHeight = sprite.getHeight();
//					float bankSpriteWidth = sprite.getWidth();
//					float y = marginY - bankSpriteHeight * 1f - TileVisual.size() * (spacingBig - 1f);
//							y, 
//					boltsWindow = createWindow(
//							marginX + bankSpriteWidth, 
//							y,
//							bankSpriteWidth * 10f, 
//							bankSpriteHeight);
//					boltsWindow.options = Nuklear.NK_WINDOW_NO_SCROLLBAR;
//					
//					sprite.setPositionX(marginX);
//					sprite.setPositionY(y);
//					sprite.setPositionY(y);
//				}); 

    }

    @Override
    public void updateGenerally(Camera cam, int... args) {
        newBackground();
        setCurrentTabBtn(tileUpgradesBtn);
        updateBackgroundColor();
        showNoUpgrades();
        hasHoveredOverLayer = true;
        hasHoveredOverUpgrades = true;
        hasBought = true;
        if (com.getGamemode() instanceof SingleplayerChallengesMode gm) {
            if (!showedExplaination) {
            	showedExplaination = true;
                if (gm.getChallengeLevel() == 0) { 
                	hasHoveredOverLayer = false;
                	hasHoveredOverUpgrades = false;
                	hasBought = false;
                	if (RSet.getInt(RSet.challengesUnlocked) == 0) {
	                    SceneHandler.showMessage(
	                            """
	                                    Upgrade your car by dragging tiles, located on the left, to the board in the middle.
	                                    You can place them anywhere you want, but they become better when next to each other.
	                                    Grayed out tiles must be unlocked for you to buy them. Some of these can only be unlocked in later challenges to decrease initial complexity!
	                                    By the way; hover your mouse-pointer anywhere to get tooltips and check out objectives to remind yourself what this game-mode is about.
	                                    Good luck!""",
	                            5);
                	}
                }
            }
        }
        press();

        improveTileBtn.tooltip = "  Hotkey: Press " + controls.getImprove().getKeyName() + " while hovering mouse over tile";
        sellTileBtn.tooltip = "  Hotkey: Press " + controls.getSell().getKeyName() + " while hovering mouse over tile";

        if (canAffordSomething(com.player) == null) {
            moneyAni.setCurrentFrame(moneyAni.getFramesAmount() - 1);
        } else {
            moneyAni.setCurrentFrame(0);
        }
    }

    @Override
    public void updateResolution() {

        float aspectDiff = (16f / 9f) / ((float) Window.WIDTH / (float) Window.HEIGHT);
        if (aspectDiff < 1f)
            aspectDiff = 1f;
        spacing = 1 + (1f / 12f / aspectDiff);
        spacingBig = 1f + (2f / 3f / aspectDiff);

//		marginX = Store.FromX() + TileVisual.size() * 1.2f;
        marginX = ((float) Window.WIDTH / 2f) - (TileVisual.size() * spacing * (float) Layer.STD_W / 2f);
//		marginY = ((float) Window.HEIGHT / 2f) - (TileVisual.size() * spacing * (float) Layer.h / 2f);
        marginY = Store.FromY() + TileVisual.size();

        if (com != null) {
            for (var btn : store.getAllTilesNonNull()) {
                btn.updateResolution();
            }
        }

        if (carInfoWindow == null)
            return;

        float extraMarginBig = TileVisual.size() * (spacingBig - 1f);
        float infoX = marginX;
        float infoY = carInfoWindow.getYHeight() + extraMarginBig;
        upgradeInfoDetailsX = infoX;
        upgradeInfoDetailsY = infoY;

        if (moneyAni.getFrame() == null)
            return;
        float bankSpriteHeight = moneyAni.getFrame().getHeight();
        float bankSpriteWidth = moneyAni.getFrame().getWidth();

        float y = marginY - bankSpriteHeight - TileVisual.size() * (spacingBig - 1f);
        float extraMargin = TileVisual.size() * (spacing - 1f);
        var bottomRightTilePos = genRealPos(new Vec2(Layer.STD_W, Layer.STD_H), com != null ? com.player != null ? com.player.layer : null: null);
        float boardXWidth = bottomRightTilePos.x - extraMargin;

        float x = marginX + bankSpriteWidth * 1.1f;
        moneyWindow.setPositionSize(x, y, boardXWidth - x, bankSpriteHeight);

        float improveHeight = extraMarginBig * 0.8f;

        upgradeInfoDetailsW = boardXWidth - infoX;
        upgradeInfoDetailsH = Window.HEIGHT - .5f * extraMarginBig - improveHeight - extraMargin - infoY;
        upgradeInfoDetailsH2 = Window.HEIGHT - extraMarginBig - infoY;

        var improveBtnsY = upgradeInfoDetailsY + upgradeInfoDetailsH + extraMargin;
        improveHeight = Window.HEIGHT - improveBtnsY - 3f * extraMargin;
        improveButtonsWindow.setPositionSize(infoX, improveBtnsY, upgradeInfoDetailsW, improveHeight);
    }

    public void updateBackgroundColor() {
        needToUpdateBackgroundColor = true;
    }

    private void actuallyUpdateBackgroundColor() {
        /*
         * if (com.player == null) return;
         *
         *
         * float r = 0.3f, g = 0.3f, b = 0.3f; for (var t :
         * com.player.layer.getLinArr()) { if (t instanceof Upgrade upgrade) { int lvl =
         * upgrade.getLVL() + 1; float change = 0.005f * lvl; switch
         * (upgrade.getUpgradeType()) { // pga placed tile case POWER -> { r += 2 *
         * change; g -= change; b -= change; } case ECO -> { r -= change; g += 2 *
         * change; b -= change; } case BOOST -> { r -= change; g -= change; b += 2 *
         * change; } } } if (r > 1f) r = 1f; else if (r < 0f) r = 0f; if (g > 1f) g =
         * 1f; else if (g < 0f) g = 0f; if (b > 1f) b = 1f; else if (b < 0f) b = 0f;
         * GL11.glClearColor(r, g, b, 1); }
         */
        if (com.isSingleplayer()) {
            var challengeLvl = ((SingleplayerChallengesMode) com.getGamemode()).getChallengeLevel();
            if (challengeLvl == SingleplayerChallenges.TheBoss.ordinal()) {
                GL11.glClearColor(60 / 255f, 0 / 255f, 0 / 255f, 1);
                return;
            }
        }

        needToUpdateBackgroundColor = false;

        var color = backgroundColors[Features.ran.nextInt(backgroundColors.length)];
        GL11.glClearColor(color.x / 255f, color.y / 255f, color.z / 255f, 1);
//        var ran = Features.ran.nextFloat();
//        var r = ran * 140f + 32f;
//        ran = Features.ran.nextFloat();
//        var g = ran * 140f + 32f;
//        ran = Features.ran.nextFloat();
//        var b = ran * 100f + 80f;
//        GL11.glClearColor(r / 255f, g / 255f, b / 255f, 1);
//        System.out.println(r + "f, " + g + "f, " + b + "f");
    }

    private void updateHistory(byte oldRole) {
        audio.play(SfxTypes.REGULAR_PRESS);
        currentUpgrade = null;
        pressedTile = null;
        if (extraStateChangeActions.containsKey(Scenes.CURRENT))
            extraStateChangeActions.get(Scenes.CURRENT).run();
        store.resetTowardsPlayer(com.player);
        com.player.role = oldRole;

        if (com.getGamemode() == null) {
            try {
                com.overrideRound = Integer.parseInt(com.player.getCurrentHistoryRound());
            } catch (Exception e) {
                com.overrideRound = -1;
            }
        }
    }

    @Override
    public void setCom(GameInfo com) {
        this.com = com;
        store = com.store;
    }

    @Override
    public void init() {
        final Consumer<IUIPressable> pressTile = (tile) -> {
            var t = pressedTile = (TileVisual) tile;
            detailsScrollIndex = 0;
            audio.playUpgrade(t.piece().upgrade().getTileName());
        };

        final IActionCheckPosition moveTileBuy = (t, pos) -> {
            t.not_placeable = false;

            // om innenfor map; flytt XY til der du vil egt ha den, som ikke er 0.
            if (com.player.isPlayer()
                    && !com.resigned
                    && (pos = checkTilePos(pos, com.player.layer)) != null
                    && com.player.isHistoryNow()) {
                int x = (int) pos.x, y = (int) pos.y;
                if (attemptBuyUpgrade(t, x, y, com.player.layer.hasMoney(x, y))) {
                    return true;
                } else {
                    audio.play(SfxTypes.BUY_FAILED);
                }
            }
        	Features.inst.getWindow().setCursor(CursorType.cursorNormal);
            return false;
        };

        final IActionCheckPosition uiUpdateMoving = (t, pos) -> {
            var foundPos = checkTilePos(pos, com.player.layer);
            foundPos = checkTilePos(pos, com.player.layer);
            showUpgrades(t, foundPos);

            if (foundPos == null) {
                t.not_placeable = true;
                return false;
            }
            var x = (int) foundPos.x;
            var y = (int) foundPos.y;
            if (Tool.checkPlaceRotator(t.piece().upgrade(), x, y, com.player.layer)) {
                t.not_placeable = false;
                return false;
            }

            t.not_placeable = !com.player.layer.isOpen(x, y, t.piece().upgrade().getTileName());
            return false;
        };

        final IActionCheckPosition mouseAbove = (tile, pos) -> {
            if (!tile.placed && !GameInfo.bonusModal.isVisible() && !SceneHandler.modalVisible) {
                Features.inst.getWindow()
                        .setCursor(tile.isMoving() ? CursorType.cursorIsHold : CursorType.cursorCanHold);
            }

            if (currentUpgrade != null) {
//				audio.playUpgradeHover(CurrentUpgrade.getUpgrade());
                if (tile == currentUpgrade) {
                    return false;
                }
            }

            pos = checkTilePos(pos, com.player.layer);
            showUpgrades(tile, pos);

            if (tile.placed) {
                improveTileBtn.hoverFake();
            }
            return false;
        };

        final IAction hoverExit = () -> {
            if (!((currentUpgrade != null && currentUpgrade.mouseAbove)
                    && (pressedTile == null || !pressedTile.equals(currentUpgrade)))) {
                showUpgrades(pressedTile, pressedTile != null ? pressedTile.piece().pos() : null);
            }
            improveTileBtn.unhover();
            Features.inst.getWindow().setCursor(CursorType.cursorNormal);
        };

        Store.tileInit = (tile) -> {
            if (tile.piece().upgrade() instanceof EmptyTile)
                return;
            tile.setActionPressed(pressTile);
            tile.setActionMovedBuy(moveTileBuy);
            tile.setActionMouseAbove(mouseAbove);
            tile.setActionHoverExit(hoverExit);
            tile.setActionUpdateUI(uiUpdateMoving);
            add(tile);
        };

        historyFwdBtn.setPressedAction(() -> {
            var oldRole = com.player.role;
            if (com.player.historyForward())
                updateHistory(oldRole);
        });

        historyBckBtn.setPressedAction(() -> {
            var oldRole = com.player.role;
            if (com.player.historyBack())
                updateHistory(oldRole);
        });

        historyHomeBtn.setPressedAction(() -> {
            var oldRole = com.player.role;
            if (com.player.historyBackHome())
                updateHistory(oldRole);
        });

        historyEndBtn.setPressedAction(() -> {
            var oldRole = com.player.role;
            if (com.player.setHistoryNow())
                updateHistory(oldRole);
        });
        
        historyFwdRoundBtn.setPressedAction(() -> {
        	var oldRole = com.player.role;
        	if (com.player.historyForwardRound())
        		updateHistory(oldRole);
        });
        
        historyBckRoundBtn.setPressedAction(() -> {
        	var oldRole = com.player.role;
        	if (com.player.historyBackRound())
        		updateHistory(oldRole);
        });

        undoBtn.setPressedAction(() -> {
            if (!com.player.canUndoHistory(com.getRound()))
                return;
            currentUpgrade = null;
            this.pressedTile = null;
            int index = com.player.undoLastHistory(com.getRound());
            if (index == -1) {
                audio.play(SfxTypes.BUY_FAILED);
                return;
            }

            com.player.undoTime++;
            audio.play(SfxTypes.UNDO);
            com.ready(com.player, (byte) 0);
            com.undoHistory(com.player, index + 1, false);
            store.resetTowardsPlayer(com.player);
//            updateReadyBtnEnabled(readyBtn, com.player);
            showNoUpgrades();

            if (canAffordSomething(com.player) == null) {
                moneyAni.setCurrentFrame(moneyAni.getFramesAmount() - 1);
            } else {
                moneyAni.setCurrentFrame(0);
            }
        });

        redoBtn.setPressedAction(() -> {
            currentUpgrade = null;
            this.pressedTile = null;
            int index = com.player.redoLastHistory();
            if (index == -1) {
                audio.play(SfxTypes.BUY_FAILED);
            }

            com.player.undoTime++;
            audio.play(SfxTypes.UNDO);
            com.ready(com.player, (byte) 0);
            com.undoHistory(com.player, index, true);
            store.resetTowardsPlayer(com.player);
//            updateReadyBtnEnabled(readyBtn, com.player);
            showNoUpgrades();
            if (canAffordSomething(com.player) == null) {
                moneyAni.setCurrentFrame(moneyAni.getFramesAmount() - 1);
            } else {
                moneyAni.setCurrentFrame(0);
            }
        });

        improveTileBtn.setPressedAction((tile) -> {
            if (tile.piece().upgrade() instanceof Upgrade upgrade) {
                var result = com.attemptImproveTile(com.player, upgrade, tile.piece().x(), tile.piece().y());
                if (result != UpgradeResult.DidntGoThrough) {
                    store.createRemoveTiles(com.player);
                    reactAfterBuy(tile, tile.piece().x(), tile.piece().y(), result, false);
                    if (Tool.justUnlocked(com.player.layer))
                        audio.play(SfxTypes.BOLT_BONUS4);
                }
            } else
                throw new RuntimeException("Tried to improve a tile that isnt a Upgrade");
        });

        runToolBtn.setPressedAction((tile) -> {
            if (tile.piece().upgrade() instanceof Tool tool) {
                var result = tool.buyRunTool(com.player, com.getRound(), tile.piece().x(), tile.piece().y());
                if (result != UpgradeResult.DidntGoThrough) {
                    store.resetTowardsPlayer(com.player);
                    reactAfterBuy(tile, tile.piece().x(), tile.piece().y(), result, false);
                } else {
                    audio.play(SfxTypes.BUY_FAILED);
                }
            }
        });

        sellTileBtn.setPressedAction((tile) -> {

            if (!com.player.bank.canAfford(-tile.piece().upgrade().getSellPrice(com.getRound()), Bank.MONEY)) {
                audio.play(SfxTypes.CLOSE_STORE);
                audio.play(SfxTypes.BUY_FAILED);
                return;
            }

            IAction selling = () -> {
//				audio.play(SfxTypes.UNDO);
                audio.play(SfxTypes.CLOSE_STORE);
                audio.play(SfxTypes.BUY);
                com.player.sellTile(tile.piece(), com.getRound());

                com.updateCloneToServer(com.player, Translator.getCloneString(com.player));
                store.resetTowardsPlayer(com.player);
                com.ready(com.player, (byte) 0);
//                updateReadyBtnEnabled(readyBtn, com.player);
                showNoUpgrades();
            };

            tile.mouseAbove = false;

            if (InputHandler.CONTROL_DOWN) {
                selling.run();
            } else {
                audio.play(SfxTypes.REGULAR_PRESS);
                audio.play(SfxTypes.OPEN_STORE);
                var text = "Want to sell this \"" + Texts.getUpgradeTitle(tile.piece().upgrade())
                        + "\" tile for a mere $" + tile.piece().upgrade().getSellPrice(com.getRound()) + "?";
                if (tile.piece().upgrade() instanceof Upgrade upgrade) {
                    var whatToLose = upgrade.getGainedValues().toPlainInfoString(120, UIColors.R);
                    text += "\nYou will lose " + (whatToLose.length() == 0 ? "nothing!" : whatToLose);
                }
                UIConfirmModal.show(text, selling);
            }
        });

        Consumer<UIButton<?>> tabSelectAction = (btn) -> {
            audio.play(SfxTypes.REGULAR_PRESS);
            setCurrentTabBtn(btn);
        };

        tileUpgradesBtn.setPressedAction(tabSelectAction);
        tileUpgradesBtn.setConsumerValue(tileUpgradesBtn);
        unlocksBtn.setPressedAction(tabSelectAction);
        unlocksBtn.setConsumerValue(unlocksBtn);

        add(undoBtn);
        add(redoBtn);
        add(historyFwdBtn);
        add(historyBckBtn);
        add(historyHomeBtn);
        add(historyEndBtn);
        add(improveTileBtn);
        add(runToolBtn);
        add(sellTileBtn);
        add(tileUpgradesBtn);
        add(unlocksBtn);
    }

    private void setCurrentTabBtn(UIButton<?> btn) {
        if (upgradeTabsCurrentBtn != null) {
            upgradeTabsCurrentBtn.setColor(null);
        }
        upgradeTabsCurrentBtn = btn;
        btn.setColor(UIColors.COLORS[UIColors.DBEIGE.ordinal()]);
        showUpgrades(currentUpgrade, currentUpgrade != null ? currentUpgrade.piece().pos() : null);
    }

    public void add(int sceneIndex, IAction extraStateChangeAction) {
        extraStateChangeActions.put(sceneIndex, extraStateChangeAction);
        UISceneInfo.addPressableToScene(sceneIndex, historyFwdBtn);
        UISceneInfo.addPressableToScene(sceneIndex, historyBckBtn);
        UISceneInfo.addPressableToScene(sceneIndex, historyHomeBtn);
        UISceneInfo.addPressableToScene(sceneIndex, historyEndBtn);
        UISceneInfo.addPressableToScene(sceneIndex, historyFwdRoundBtn);
        UISceneInfo.addPressableToScene(sceneIndex, historyBckRoundBtn);
        UISceneInfo.addWindowToScene(sceneIndex, historyWindow);
        UISceneInfo.addWindowToScene(sceneIndex, tabsWindow);
        UISceneInfo.addWindowToScene(sceneIndex, improvementsWindow);
    }

    public static void showTooltip(String regValsText, float x, float y, boolean overlap, boolean showExtra,
                                   float maxX) {
        showExtraTooltip = showExtra;
        regValsTooltip.setText(regValsText);

        float regValsTextLength = 0f;
        for (var uiElem : regValsTooltip.getListArr()) {
            var textLen = ((UILabel) uiElem).getTextWithoutColor().length();
            if (textLen > regValsTextLength) {
                regValsTextLength = textLen;
            }
        }

        float fontHeight = immediateUpgradesFont.getHeightFloat(), fontWidth = fontHeight * 0.45f,
                regValsW = fontWidth * (regValsTextLength + 1),
                regValsH = regValsTooltip.getListArr().length * fontHeight * 1.6f, regValsX = x,
                regValsY = y - regValsH;

        if (!overlap && regValsX + regValsW >= maxX) {
            regValsX = maxX - regValsW;
        }

        regValsTooltip.getWindow().setPositionSize(regValsX, regValsY, regValsW, regValsH);
        regValsTooltip.getWindow().focus = false;
        regValsTooltip.getWindow().z = -3;
    }

    public static void showTooltip(String text) {
//		System.out.println("tool: x=" + InputHandler.x + ", y=" + InputHandler.y);
        regValsTooltip.getWindow().visible = true;
        tooltipTime = 5;
        showTooltip(text, InputHandler.x, InputHandler.y, false, false, Window.WIDTH);
    }

    private void showTooltip(String regValsText, float x, float y, boolean overlap, boolean showExtra) {
        showTooltip(regValsText, x, y, overlap, showExtra, carInfoWindow.x);
    }

    public void showNoUpgrades() {
//		if (pressedTile == null)
//			return;
        pressedTile = null;
        regValsTooltip.getWindow().visible = false;
        showUpgrades(null, -1, -1);
    }

    private void showUpgrades(TileVisual tile, Vec2 pos) {
        int x, y;
        if (pos != null) {
            hasHoveredOverLayer = true;
            x = (int) pos.x;
            y = (int) pos.y;
        } else {
            x = -1;
            y = -1;
        }

        showUpgrades(tile, x, y);
    }

    private void showUpgrades() {
        if (pressedTile == null)
            showNoUpgrades();
        else
            showUpgrades(pressedTile, pressedTile.piece().x(), pressedTile.piece().y());
    }

    private void showUpgrades(TileVisual tile, int x, int y) {

        if (!com.resigned) {
            undoBtn.setEnabled(com.player.canUndoHistory(com.getRound()));
            redoBtn.setEnabled(com.player.canRedoHistory());
            undoBtn.press();
        }

        currentUpgrade = tile;
        boolean selected = tile != null && tile.equals(pressedTile);
        boolean placed = tile != null && tile.placed;
        Rep comparedRep = com.player.getCarRep();
        if (tile != null) {
        	if (!hasHoveredOverUpgrades) {
        		hasHoveredOverUpgrades = true;
        	}

            upgradeInfoDetails.clear();

            upgradeInfoDetails
                    .addText(new UILabel("\"" + Texts.getUpgradeTitle(tile.piece().upgrade()) + "\"#" + UIColors.AI,
                            NK_TEXT_ALIGN_CENTERED | NK_TEXT_ALIGN_MIDDLE));

            float lowest = TileVisual.size() / 2f;
            float highest = Window.HEIGHT - lowest;

            var detailsText = tile.getInfo(com.getRound(), com.player.layer, com.player.upgrades, x, y);
            upgradeInfoDetails.addText(detailsText);

            if (x != -1 && tile.piece().upgrade() instanceof Upgrade upgrade) {
                if (com.player.layer.isEmpty(x, y)) {
                    showTooltip(
                            "This is an empty tile.\nWhen a nearby tile is improved to LVL "
                                    + com.player.layer.placedUnlockEmptyLVL + " it will be unlocked.",
                            tile.getPosX() + TileVisual.size() - 32, tile.getPosY() - 4, true, false);
                    return;
                }
                var clonedPlayer = com.player.getClone();
                upgrade.upgrade(clonedPlayer, x, y, true);
                comparedRep = clonedPlayer.getCarRep();
            }

            var regValsText = tile.tooltip(com.player.layer, com.player.upgrades, x, y);

            float tooltipY = 0;
            if (tile.piece().x() != -1 && tile.piece().upgrade().getUpgradeType() != UpgradeType.NEG) {
                var neighs = TilePiece.getAllNeighbours(com.player.layer, tile.piece());
                var highestY = tile.piece().y();
                for (var n : neighs) {
                    if (n.y() < highestY)
                        highestY = n.y();
                }
                tooltipY = genRealPos(0, highestY, com.player.layer).y - 4;
            } else {
                tooltipY = tile.getPosY() - 4;
            }
            var hyper = com.player.layer.getWidth() == 7;
            var upindeY = upgradeInfoDetailsY;
            var upindeH = tile.piece().x() != -1 ? upgradeInfoDetailsH : upgradeInfoDetailsH2;
            if (hyper && tile.placed) {
                var hyperDiff = TileVisual.size()*(spacingBig-1f);
                upindeY += hyperDiff;
                upindeH -= hyperDiff;
            }

            upgradeInfoDetails.getWindow().setPositionSize(upgradeInfoDetailsX, upindeY,
                    upgradeInfoDetailsW, upindeH);

            showTooltip(regValsText, tile.getPosX() + TileVisual.size() - 32, tooltipY, false, true);

            if (com.player.isPlayer()) {
                if (com.player.isHistoryNow() && tile.placed) {
                    if (tile.piece().upgrade() instanceof Upgrade u && u.getUpgradeType() != UpgradeType.NEG) {
                        runToolBtn.setVisible(false);
                        placedTileBtnRow.row[0] = improveTileBtn;
                        improveTileBtn.setVisible(true);
                        improveTileBtn.setEnabled(tile.piece().upgrade().isOpenForUse());
                        improveTileBtn.setConsumerValue(tile);
                        improveTileBtn.press();
                    } else if (tile.piece().upgrade() instanceof Tool t
                            && (t.getTileName() == TileNames.LeftRotator || t.getTileName() == TileNames.RightRotator)) {
                        improveTileBtn.setVisible(false);
                        placedTileBtnRow.row[0] = runToolBtn;
                        runToolBtn.setVisible(true);
                        runToolBtn.setConsumerValue(tile);
                        runToolBtn.press();
                    } else {
                        improveTileBtn.setVisible(false);
                        runToolBtn.setVisible(false);
                    }
                    sellTileBtn.setConsumerValue(tile);
                    var price = tile.piece().upgrade().getSellPrice(com.getRound());
                    if (price >= 0) {
                        sellTileBtn.setColorUI(UIColors.R);
                        sellTileBtn.setTitle(Texts.sellUpgrade + " $" + price);
                    } else {
                        sellTileBtn.setColorUI(UIColors.WON);
                        sellTileBtn.setTitle(Texts.buyOffUpgrade + " -$" + (-1 * price));
                    }
                    sellTileBtn.press();
                }
            }
        } else {
            pressedTile = null;
        }
        this.carInfo = com.player.getLobbyStatsInfo(comparedRep);
        for (var carInfoLine : carInfo) {
            if (carInfoLine.actionTooltip != null)
                break;
            carInfoLine.actionTooltip = UpgradesSubscene::showTooltip;
        }
    }

    public static Vec2 genRealPos(Vec2 pos, Layer layer) {
        return genRealPos(pos.x, pos.y, layer);
    }

    public static Vec2 genRealPos(float x, float y, Layer layer) {
        return genRealPos(x, y, layer, marginX, marginY);
    }

    /**
     * Gen window pos based on layer pos
     */
    public static Vec2 genRealPos(float x, float y, Layer layer, float marginX, float marginY) {
        if (layer != null && layer.getWidth() == 7) {
            x--;
            y--;
        }
        return new Vec2(marginX + (x * TileVisual.size() * spacing), marginY + (y * TileVisual.size() * spacing));
    }

    private Vec2 checkTilePos(Vec2 pos, Layer layer) {
        return checkTilePos(pos.x, pos.y, false, layer);
    }

    /**
     * Gen layer pos based on window pos
     */
    public static Vec2 checkTilePos(float x, float y, boolean middleOfTile, Layer layer) {
        final var size = TileVisual.size() * spacing;
        final var halfSize = middleOfTile ? 0f : size / 2f;
        final var marginX = UpgradesSubscene.marginX - (layer.getWidth() == 7 ? size : 0);
        final var marginY = UpgradesSubscene.marginY - (layer.getHeight() == 7 ? size : 0);
        Vec2 res = null;

        var boardPos = genRealPos(0, 0, layer);

        float xL = boardPos.x + (size * layer.getWidth()) + halfSize;
        float xG = boardPos.x - halfSize;
        float yL = boardPos.y + (size * layer.getHeight()) + halfSize;
        float yG = boardPos.y - halfSize;

        if ((x < xL && x > xG) && (y < yL && y > yG)) {
            // sjekk X ved 0
            int newX = (int) ((x + halfSize - marginX) / size);
            if (newX < 0 || newX >= layer.getWidth())
                return null;
            int newY = (int) ((y + halfSize - marginY) / size);
            if (newY < 0 || newY >= layer.getHeight())
                return null;
            res = new Vec2(newX, newY);
        }
        return res;
    }

    /**
     * @return placed succesfully
     */
    private boolean attemptBuyUpgrade(TileVisual tileVisual, int x, int y, boolean earned) {
        var newPiece = new TilePiece<>(tileVisual.piece().upgrade(), x, y);
        var ret = com.attemptBuyTile(com.player, newPiece);
        if (ret != UpgradeResult.DidntGoThrough) {
            reactAfterBuy(store.getTileAt(x, y), x, y, ret, earned);
            hasBought = true;
            return true;
        }
        return false;
    }

    private void reactAfterBuy(TileVisual tile, int x, int y, UpgradeResult result, boolean earned) {
        if (result == UpgradeResult.Bought) {
            if (!earned)
                audio.play(SfxTypes.BUY);
            else
                audio.play(SfxTypes.BUY_EARNED);
            pressedTile = tile;
            controllerCursorX = pressedTile.getPosX();
            controllerCursorY = pressedTile.getPosY();
            controllerX = tile.piece().x();
            controllerY = tile.piece().y();
            showUpgrades(tile, x, y);
            startBuyAnimation = true;
            moneyAni.setCurrentFrame(1);

//            if (tile.placed && tile.piece().upgrade() instanceof Upgrade upgrade) {
//                if (upgrade.getLVL() == Upgrade.placedNeighbourChangeLVL) {
//                    UIRisingTexts.pushText(tile.getPosX(), tile.getPosY(), "x2 neighbor =>", UIColors.LBEIGE);
//                } else if (upgrade.getLVL() > Upgrade.placedNeighbourChangeLVL)  {
//                    UIRisingTexts.pushText(tile.getPosX(), tile.getPosY(), "+10% neighbor =>", UIColors.LBEIGE);
//                }
//            }
        } else {
            // Actually before tile is bought because you must select gold or normal bonus
            audio.play(SfxTypes.NEW_BONUS);
            GameInfo.bonusModal.setVisible(true);
            Features.inst.getWindow().setCursor(CursorType.cursorNormal);
        }

//        updateReadyBtnEnabled(readyBtn, com.player);
        if (com.player.isReady()) {
        	 audio.play(SfxTypes.UNREADY);
             com.ready(com.player, (byte) 0);
        }
    }

    public static TilePiece<?> canAffordSomething(Player player) {
        int openX = -1, openY = -1;

        var tiles = player.layer.getDobArr();
        for (var x = 0; x < tiles.length; x++) {
            for (var y = 0; y < tiles[x].length; y++) {
                if (tiles[x][y] == null) {
                    openX = x;
                    openY = y;
                } else if (player.bank.canAfford(tiles[x][y].getCost(player.layer.getSale(x,y)), Bank.MONEY) && tiles[x][y].isOpenForUse()) {
                    return new TilePiece<>(tiles[x][y], x, y);
                }
            }
        }

        if (openX != -1) {
            for (var storeUpgrade : player.upgrades.getUpgrades()) {
                if (storeUpgrade.isOpenForUse() && player.bank.canAfford(storeUpgrade.getCost(player.layer.getSale(openX, openY)), Bank.MONEY)) {
                    return new TilePiece<>(storeUpgrade, openX, openY);
                }
            }
        }
        return null;
    }

//    public static boolean updateReadyBtnEnabled(GameInfo com, Player player) {
//        if (!Lobby.decideEnableReadyBtn(com)) return false;
//        if (com.getGamemode().isRacing()) {
//            return false;
//        } else if (com.isSingleplayer()) {
//            return com.getGamemode().canSaveMoney || (canAffordSomething(player) == null);
//        }
//        return true;
//    }

//    public void updateReadyBtnEnabled(UIButton<?> readyBtn, Player player) {
//        readyBtn.setEnabled(updateReadyBtnEnabled(com, player));
//        readyBtn.press();
//    }

    /**
     * Runs after you've bought and potentially not canceled a chain of bonus
     * choices.
     */
    public void reactBonus(boolean successful) {
        store.resetTowardsPlayer(com.player);
        if (successful) {
            if (currentUpgrade != null) {
//				CurrentUpgrade.setFreeUpgrades(com.player.upgrades.getUpgrade(CurrentUpgrade.getUpgradeId()).popFreeUpgradeStats());
                showUpgrades(currentUpgrade, currentUpgrade.piece().x(), currentUpgrade.piece().y());
            }
        }
    }

    @Override
    public void createWindowsWithinBounds(float x, float y, float width, float height, float joiningPlayersX) {
        updateResolution();
        float extraMargin = TileVisual.size() * (spacing - 1f);
        float extraMarginBig = TileVisual.size() * (spacingBig - 1f);

        x = extraMarginBig;
        y = Store.FromY();
        float w = Store.FromX() - extraMarginBig * 2f;

        regValsTooltip = new UIScrollable(sceneIndex, 0, 0, TileVisual.size() * 1.5f, TileVisual.size() / 2f);
        regValsTooltip.getWindow().options = NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR;
        regValsTooltip.shadow = true;
        regValsTooltip.setScrollable(false);

        var topLeftTilePos = genRealPos(new Vec2(0, 0), null);
        var bottomRightTilePos = genRealPos(new Vec2(Layer.STD_W, Layer.STD_H), null);

        float boardXWidth = bottomRightTilePos.x - extraMargin;
        float boardYWidth = bottomRightTilePos.y - extraMargin;
        var carInfoX = boardXWidth + extraMarginBig;
        var carInfoH = bottomRightTilePos.y - marginY - extraMargin;
        var carInfoW = (Window.WIDTH - TileVisual.size() * 3f) - extraMarginBig - carInfoX;
        var aspect = (float) Window.WIDTH / (float) Window.HEIGHT;

        var minHeight = Player.rightLobbyStatsInfoSize * Main.standardFont.getHeightFloat();
        if (carInfoH < minHeight) {
            carInfoH = minHeight;
        }
        if (aspect > 16f / 9f) {
            carInfoW = TileVisual.size() * 3.5f * (aspect / (21f / 9f));
        }
        carInfoWindow = createWindow(carInfoX, marginY, carInfoW, carInfoH);
        carInfoWindow.options = NK_WINDOW_BORDER | NK_WINDOW_NO_SCROLLBAR;

        upgradeInfoDetails = new UIScrollable(Scenes.GENERAL_NONSCENE, 0, 0, 0, 0);
        upgradeInfoDetails.rowHeightBased = 36f;

        improveButtonsWindow = createWindow(0, 0, 0, 0);

        tabsWindow = createWindow(extraMarginBig, carInfoWindow.y - extraMarginBig, marginX - 2 * extraMarginBig,
                extraMarginBig);
        unlocks = new UIScrollable(sceneIndex, tabsWindow.x, marginY, marginX - 2 * extraMarginBig, boardYWidth);
        improvementsWindow = createWindow(tabsWindow.x - .75f * TileVisual.size(), marginY * .83f,
                3f * TileVisual.size(), 0.45f * TileVisual.size());

        x = genRealPos(0, 0, null).x;
        width = (genRealPos(5, 0, null).x) - extraMargin - x;
        height = TileVisual.size() * .64f;
        y = Window.HEIGHT - height;
        historyWindow = createWindow(x - TileVisual.size() / 2f, y, width + TileVisual.size(), height);

        var margin = extraMarginBig / 3f;
        backgroundLayer = new Sprite(new Vec2(topLeftTilePos.x - margin, topLeftTilePos.y - margin),
                boardXWidth - topLeftTilePos.x + 2f * margin, "./images/back/layer.png", "main");
        backgroundLayer.create();
        backgroundLayer2 = new Sprite(new Vec2(topLeftTilePos.x - TileVisual.size() - extraMargin - margin, topLeftTilePos.y - TileVisual.size() - extraMargin - margin),
                boardXWidth - topLeftTilePos.x + 2f*spacing*TileVisual.size() + 2f * margin, "./images/back/layer2.png", "main");
        backgroundLayer2.create();
    }

    // private void upgradeSelect(int upgradeIndex) {
    // press();
    // storeTiles.get(upgradeIndex).setSelected(true);
    // storeTiles.get(upgradeIndex).runPressedAction();
    // }

    @Override
    public void keyInput(int keycode, int action) {
        if (action != GLFW.GLFW_RELEASE) {
            if (!com.resigned && com.player.role < Player.COMMENTATOR) {
                if (currentUpgrade != null) {
                    if (currentUpgrade.mouseAbove && currentUpgrade.placed) {
                        if (keycode == controls.getImprove().getKeycode()) {
                            improveTileBtn.runPressedAction();
                            return;
                        }
                        if (keycode == controls.getSell().getKeycode()) {
                            sellTileBtn.runPressedAction();
                            return;
                        }
                    }
                }
                if (keycode == controls.getUndo().getKeycode()) {
                    if (InputHandler.CONTROL_DOWN)
                        redoBtn.runPressedAction();
                    else
                        undoBtn.runPressedAction();
                    return;
                }
                if (InputHandler.CONTROL_DOWN) {
                    if (keycode == GLFW.GLFW_KEY_Y)
                        redoBtn.runPressedAction();
                    else if (keycode == GLFW.GLFW_KEY_Z)
                        undoBtn.runPressedAction();
                }
            } else {
                if (keycode == GLFW.GLFW_KEY_RIGHT) {
                    historyFwdBtn.runPressedAction();
                } else if (keycode == GLFW.GLFW_KEY_LEFT) {
                    historyBckBtn.runPressedAction();
                } else if (keycode == GLFW.GLFW_KEY_PERIOD) {
                    historyEndBtn.runPressedAction();
                } else if (keycode == GLFW.GLFW_KEY_COMMA) {
                    historyHomeBtn.runPressedAction();
	            } else if (keycode == GLFW.GLFW_KEY_M) {
	            	historyFwdRoundBtn.runPressedAction();
	            } else if (keycode == GLFW.GLFW_KEY_N) {
	            	historyBckRoundBtn.runPressedAction();
	            }
            }
        }
    }

    @Override
    public void controllerInput() {
        if (com.player.role >= Player.COMMENTATOR) {
            if (InputHandler.BTN_RIGHT) {
                historyFwdBtn.runPressedAction();
            } else if (InputHandler.BTN_LEFT) {
                historyBckBtn.runPressedAction();
            } else if (InputHandler.BTN_UP) {
                historyEndBtn.runPressedAction();
            } else if (InputHandler.BTN_DOWN) {
                historyHomeBtn.runPressedAction();
            }
            return;
        }

        if (!InputHandler.HOLDING) {
            if (readyBtn.isHovered()) {
                showNoUpgrades();
                regValsTooltip.getWindow().visible = false;
                return;
            }

            if (InputHandler.BTN_RIGHT) {
                System.out.println("redo");
                redoBtn.runPressedAction();
                return;
            } else if (InputHandler.BTN_LEFT) {
                System.out.println("undo");
                undoBtn.runPressedAction();
                return;
            }
        }

//			System.out.println(InputHandler.BTN_A);
        /*
         * Fiks bevegelse til neste tile i layer Fiks at når man kjøper en tile så skal
         * den presses etterpå Fiks at når man er i layer så beveger man seg en tile om
         * gangen, mens i butikken så går det kjapt. weird fungerte ikke helt med
         * unlock!!!
         */
        // FLYTTER OG KJØPER
        if (currentUpgrade != null) {
            if (System.currentTimeMillis() > controllerScroll) {
                if (InputHandler.RIGHT_STICK_Y > .1) {
                    mouseScrollInput(0, -1);
                    controllerScroll = System.currentTimeMillis() + 66;
                } else if (InputHandler.RIGHT_STICK_Y < -.1) {
                    mouseScrollInput(0, 1);
                    controllerScroll = System.currentTimeMillis() + 66;
                }
            }
            if (InputHandler.BTN_A || InputHandler.RIGHT_TRIGGER > 0f) {

                if (!currentUpgrade.placed && currentUpgrade.piece().upgrade().isOpenForUse()) {
                    pressedTile = currentUpgrade;
                    if (!currentUpgrade.isMoving()) {
                        controllerCursorX = currentUpgrade.getPosX();
                        controllerCursorY = currentUpgrade.getPosY();
                    }
//						System.out.println(InputHandler.LEFT_STICK_X);
                    final var spd = 25f;
                    if (Math.abs(InputHandler.LEFT_STICK_X) > .1f) {
                        controllerCursorX += spd * InputHandler.LEFT_STICK_X * Timer.lastDelta;
                    } else if (Math.abs(InputHandler.RIGHT_STICK_X) > .1f) {
                        controllerCursorX += spd * InputHandler.RIGHT_STICK_X * Timer.lastDelta;
                    }
                    if (Math.abs(InputHandler.LEFT_STICK_Y) > .1f) {
                        controllerCursorY += spd * InputHandler.LEFT_STICK_Y * Timer.lastDelta;
                    } else if (Math.abs(InputHandler.RIGHT_STICK_Y) > .1f) {
                        controllerCursorY += spd * InputHandler.RIGHT_STICK_Y * Timer.lastDelta;
                    }

                    if (!currentUpgrade.isMoving())
                        currentUpgrade.runPressedAction();
                    currentUpgrade.setPos(controllerCursorX, controllerCursorY);
                    currentUpgrade.setMoving(true);
                    var pos = checkTilePos(controllerCursorX, controllerCursorY, false, com.player.layer);
                    showUpgrades(currentUpgrade, pos);

                    return;
                }
            }
            if (currentUpgrade.isMoving()) {
                currentUpgrade.setMoving(false);
                return;
            }

        }

        // VELGER
        if (!InputHandler.HOLDING) {

            var oldControllerX = controllerX;
            var oldControllerY = controllerY;

            var speed = 200;

            boolean movingStickX = false;
            boolean movingStickY = false;

            if (InputHandler.LEFT_STICK_X < -.5f) {
                if (System.currentTimeMillis() > controllerMovingX) {
                    controllerX--;
                }
                movingStickX = true;
            } else if (InputHandler.LEFT_STICK_X > .5f) {
                if (System.currentTimeMillis() > controllerMovingX) {
                    if (controllerX < com.player.layer.getWidth())
                        controllerX++;
                }
                movingStickX = true;
            }
            if (InputHandler.LEFT_STICK_Y < -.5f) {
                if (System.currentTimeMillis() > controllerMovingY) {
                    if (controllerY > 0)
                        controllerY--;
                }
                movingStickY = true;
            } else if (InputHandler.LEFT_STICK_Y > .5f) {
                if (System.currentTimeMillis() > controllerMovingY) {
                    controllerY++;
                }
                movingStickY = true;
            }

            if (InputHandler.BTN_BACK_TOP_LEFT) {
                controllerX = -3;
                controllerY = 0;
            }

            if (controllerX >= 0) {
                if (movingStickX || movingStickY) {
                    controllerMovingX = controllerMovingY = Long.MAX_VALUE;
                } else {
                    controllerMovingX = -1;
                    controllerMovingY = -1;
                }
            } else {
                if (movingStickX) {
                    if (System.currentTimeMillis() > controllerMovingX)
                        controllerMovingX = System.currentTimeMillis() + speed;
                } else
                    controllerMovingX = 0;
                if (movingStickY) {
                    if (System.currentTimeMillis() > controllerMovingY)
                        controllerMovingY = System.currentTimeMillis() + speed;
                } else
                    controllerMovingY = 0;
            }
            int changeX = controllerX - oldControllerX;
            int changeY = controllerY - oldControllerY;
            var tile = (changeX == 0 && changeY == 0) ? currentUpgrade :

                    store.getStoreTileAt(controllerX, controllerY,
                            Math.max(Math.abs(InputHandler.LEFT_STICK_X), Math.abs(InputHandler.LEFT_STICK_Y)), changeX,
                            changeY);
            if (tile != null) {
                if (currentUpgrade != tile) {
                    if (currentUpgrade != null)
                        currentUpgrade.mouseAbove = false;
                    tile.mouseAbove = true;
                    tile.runHoveredAction();
                    currentUpgrade = tile;
                    showUpgrades(tile, tile.piece().x(), tile.piece().y());
                    audio.play(SfxTypes.REGULAR_HOVER);
                    regValsTooltip.getWindow().visible = true;

                    if (currentUpgrade.placed) {
                        controllerX = currentUpgrade.piece().x();
                        controllerY = currentUpgrade.piece().y();
                    } else {
                        controllerX = currentUpgrade.logicalX;
                        controllerY = currentUpgrade.logicalY;
                    }
                }
            } else if (currentUpgrade != null) {
                if (currentUpgrade.placed) {
                    controllerX = currentUpgrade.piece().x();
                    controllerY = currentUpgrade.piece().y();
                } else {
                    controllerX = currentUpgrade.logicalX;
                    controllerY = currentUpgrade.logicalY;
                }
            } else {
                if (controllerX > com.player.layer.getWidth())
                    controllerX = com.player.layer.getWidth();
                if (controllerY > com.player.layer.getHeight())
                    controllerY = com.player.layer.getHeight();
            }
//			System.out.println("x: " + controllerX + ", y: " + controllerY);

            if (currentUpgrade != null) {
                if (InputHandler.BTN_A) {
                    if (currentUpgrade.placed) {
                        if (currentUpgrade.piece().upgrade() instanceof Upgrade up) {
                            if (!up.isOpenForUse()) {
                                if (up.getUpgradeType() == UpgradeType.NEG)
                                    sellTileBtn.runPressedAction();
                                else
                                    currentUpgrade.runPressedAction();
                                return;
                            }
                            if (!com.player.bank.canAfford(up.getCost(com.player.layer.getSale(controllerX, controllerY)), Bank.MONEY)) {
                                audio.play(SfxTypes.BUY_FAILED);
                            }
                        }
                        improveTileBtn.runPressedAction();
                    } else {
                        currentUpgrade.runPressedAction();
                        controllerCursorX = currentUpgrade.getPosX();
                        controllerCursorY = currentUpgrade.getPosY();
                    }
                } else if (InputHandler.BTN_B) {
                    sellTileBtn.runPressedAction();
                }
                return;
            }
        }
    }

    @Override
    public void mouseScrollInput(float x, float y) {
        var indirectScrollInfo = currentUpgrade != null && currentUpgrade.mouseAbove;
        if (indirectScrollInfo)
            upgradeInfoDetails.getWindow().focus = true;
        upgradeInfoDetails.scroll(y);
        if (indirectScrollInfo)
            upgradeInfoDetails.getWindow().focus = false;
    }

    @Override
    public boolean mouseButtonInput(int button, int action, float x, float y) {
        if (menu != null && menu.visible)
            return false;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {

            if (upgradeTabsCurrentBtn == tileUpgradesBtn) {
                boolean hitATile = false;

                for (var btn : store.getAllTilesNonNull()) {
                    if (btn.mouseButtonInput(action, x, y)) {
                        hitATile = true;
                        break;
                    }
                }
                if (action == GLFW.GLFW_RELEASE && !hitATile && !upgradeInfoDetails.getWindow().focus
                        && !improveButtonsWindow.focus)
                    showNoUpgrades();

                if (currentUpgrade != null) {
                    if (!currentUpgrade.placed && currentUpgrade.mouseAbove && !GameInfo.bonusModal.isVisible()
                            && !SceneHandler.modalVisible) {
                        if (action != GLFW.GLFW_RELEASE)
                            Features.inst.getWindow().setCursor(CursorType.cursorIsHold);
                        else
                            Features.inst.getWindow().setCursor(CursorType.cursorCanHold);
                    } else {
                        Features.inst.getWindow().setCursor(CursorType.cursorNormal);
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void mousePosInput(float x, float y) {
        if (menu != null && menu.visible)
            return;

//		if (upgradeTabsCurrentBtn == tileUpgradesBtn) {
        if (currentUpgrade != null && currentUpgrade.isMoving()) {
            currentUpgrade.mousePosInput(x, y);
            return;
        }
        boolean found = false;
        for (var btn : store.getAllTilesNonNull()) {
            if (!found) {
            	if (btn.placed && !hasHoveredOverLayer) continue;
                if (btn.mousePosInput(x, y)) {
                    found = true;
                }
            } else {
                btn.mouseAbove = false;
            }
        }
        if (found && currentUpgrade == null) {
            nextTileSpriteFrameSwitch = System.currentTimeMillis() + tileSpriteSpeed;
            TileSpriteFrame = 0;
        }

        if (!found && currentUpgrade == null) {
            // show tip about timesmod
            var pos = checkTilePos(x, y, true, com.player.layer);
            if (pos != null) {
                var timesMod = com.player.layer.getTimesMod(pos);
                if (com.player.layer.get((int) pos.x, (int) pos.y) instanceof EmptyTile) {
                    showTooltip(
                            "This is an empty tile.\nWhen a nearby tile is improved to LVL "
                                    + com.player.layer.placedUnlockEmptyLVL + " it will be unlocked.",
                            x, y, true, false);
                    found = true;
                } else {
                    var sb = new StringBuilder();
                    if (timesMod != 0f) {
                        sb.append(Texts.timesModTooltip);
                        found = true;
                    }
                    if (com.player.layer.getMoney((int) pos.x, (int) pos.y) > 0) {
                        if (found)
                            sb.append("\n");
                        sb.append(
                                "This open tile has some money laying on it.\nPlacing a tile here would be cheap cheap.");
                        found = true;
                    }
                    if (found)
                        showTooltip(sb.toString(), x, y, true, false);
                }
            } else if (!undoBtn.isHovered() && !redoBtn.isHovered() && moneyWindow.isWithinBounds(x, y)
                    || moneyAni.getFrame().above(x, y)) {
                var interest = com.player.getCarRep().get(Rep.interest);
                showTooltip("+" + Texts.formatNumber(interest * 100d) + "% " + Texts.tags[Rep.interest]);
            }
        }

        if (tooltipTime > 1) {
            tooltipTime--;
        } else if (tooltipTime == 1) {
            if (pressedTile != null)
                showUpgrades();
            tooltipTime--;
        } else {
            regValsTooltip.getWindow().visible = pressedTile != null || found;
        }
//		}
    }

    @Override
    public void tick(float delta) {
        if (needToUpdateBackgroundColor)
            actuallyUpdateBackgroundColor();

        regValsTooltipRender = !menu.visible && !GameInfo.bonusModal.isVisible();

        var now = System.currentTimeMillis();
        if (nextTileSpriteFrameSwitch < now) {
            nextTileSpriteFrameSwitch = now + tileSpriteSpeed;
            TileSpriteFrame = (TileSpriteFrame + 1) % 4;
        }
    }
    
    /**
     * Use me to render the engine
     */
    @Override
    public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
//		 renderer.renderMesh(car, camera);

        if (com.player == null)
            return;
    	var gameOver = com.resigned || com.isGameOver();
        if (gameOver)
            viewPlayerMinimally = false;
        else if (com.player.role == Player.COMMENTATOR && !viewPlayerMinimally)
            return;
        var layer = viewPlayerMinimally ? com.getPlayer(viewPlayerId).layer : com.player.layer;
        var hyper = layer.getWidth() == 7;

        boolean showLittle = viewPlayerMinimally && !com.isCoop() && com.player.role != Player.COMMENTATOR;

        if (!showLittle && !gameOver && hasHoveredOverUpgrades) {
            float y = marginY - moneyAni.getFrame().getHeight() - TileVisual.size() * (spacingBig - 1f);
            if (layer.getWidth() == 7) y -= .6f*TileVisual.size();
            if (startBuyAnimation) {
                moneyAni.incrementCurrentFrame(delta);
                if ((int) moneyAni.getCurrentFrame() == moneyAni.getFramesAmount() - 1) {
                    if (canAffordSomething(com.player) == null) {
                        startBuyAnimation = false;
                    }
                }
                if ((int) moneyAni.getCurrentFrame() == 0) {
                    startBuyAnimation = false;
                }
            }
            moneyAni.getFrame().setPositionX(marginX);
            moneyAni.getFrame().setPositionY(y);
            moneyAni.getFrame().setPositionZ(-1);
            renderer.renderOrthoMesh(moneyAni.getFrame());
        }
//
//		if (upgradeTabsCurrentBtn == unlocksBtn) {
//
//		} else if (upgradeTabsCurrentBtn == tileUpgradesBtn) {
        
        if (com.getGamemode() instanceof SingleplayerChallengesMode sp && sp.getChallengeLevel() == 0) {
	        if (!hasHoveredOverUpgrades) {
	        	arrow.incrementCurrentFrame(.25d * delta);
	        	arrow.getFrame().setRotationZ(0);
	        	arrow.setPos(marginX, marginY);
	        	renderer.renderOrthoMesh(arrow.getFrame());
	        } else if (!hasHoveredOverLayer) {
	        	arrow.incrementCurrentFrame(.25d * delta);
	        	arrow.getFrame().setRotationZ(0);
	        	arrow.setPos(marginX + 6*TileVisual.size(), marginY + TileVisual.size());
	        	renderer.renderOrthoMesh(arrow.getFrame());
	        } else if (hasBought && com.getRound() == 1) {
	        	arrow.incrementCurrentFrame(.25d * delta);
	        	arrow.getFrame().setRotationZ(180);
	        	arrow.setPos(4f*TileVisual.size(), .5f*TileVisual.size());
	        	renderer.renderOrthoMesh(arrow.getFrame());
	        }
        }
        

        if (hasHoveredOverUpgrades) {
            if (hyper) {
                renderer.renderOrthoMesh(backgroundLayer2);
                backgroundLayer2.setPositionZ(-1);
            } else {
                renderer.renderOrthoMesh(backgroundLayer);
                backgroundLayer.setPositionZ(-1);
            }
        }

        /*
         * Circle around tile
         */
        List<TilePiece<?>> neighbours = null;
        var layerTiles = store.getLayerTiles();
        if (layerTiles == null)
            return;
        var now = System.currentTimeMillis();
        if (now > Tool.nextTimeSwitchRender) {
            Tool.nextTimeSwitchRender = now + (Tool.lastRenderedRotator ? 1000 : 3000);
            Tool.lastRenderedRotator = !Tool.lastRenderedRotator;
        }

        var currentUpgrade = this.currentUpgrade;
        if (viewPlayerMinimally)
            currentUpgrade = null;

        var pressedTile = this.pressedTile;
        if (viewPlayerMinimally)
            pressedTile = null;

        if (currentUpgrade != null) {
            for (var btn : store.getAllStoreTilesNonNull()) {
//					if (viewPlayerMinimally && btn.piece() != null && btn.piece().upgrade() instanceof Tool) {
//						continue;
//					}
                btn.render(renderer);

                /*
                 * Unlock sirkel
                 */
                if (!currentUpgrade.placed) {
                    if (currentUpgrade.piece().upgrade() instanceof Upgrade cu
                            && btn.piece().upgrade() instanceof Upgrade upgrade) {
                        if (cu.unlocks(upgrade)) {
                            btn.renderSelected(renderer, getSelected(), 0f, upgrade.getUpgradeType(), false);
                        } else if (upgrade.unlocks(cu)) {
                            btn.renderSelected(renderer, getSelected(), 0f, upgrade.getUpgradeType(), true);
                        }
                    }
                }
            }

            var piece = currentUpgrade.piece();
            if (piece.x() >= 0) {

                neighbours = TilePiece.getAllNeighbours(com.player.layer, piece);
                for (var x : layerTiles) {
                    for (var y : x) {
                        if (y == null)
                            continue;
                        if (currentUpgrade.placed) {
                            y.transparent = !TilePiece.contains(neighbours, y.piece());
                            y.neighbour = y != currentUpgrade //&& y.piece().upgrade() instanceof Upgrade
                                    && !y.transparent;
                        } else {
                            y.neighbour = false;
                            y.transparent = false;
                        }
                        y.render(renderer);
                    }
                }
            } else {
                for (var x : layerTiles) {
                    for (var y : x) {
                        if (y == null)
                            continue;
                        y.neighbour = false;
                        y.transparent = false;
                        y.render(renderer);
                    }
                }
            }

            currentUpgrade.renderSelected(renderer, getSelected(), 0f, piece.upgrade().getUpgradeType(), true);
        } else if (!hasHoveredOverUpgrades) {
            for (var btn : store.getAllStoreTilesNonNull()) {
                if (showLittle && !btn.placed && btn.piece().upgrade() instanceof Tool)
                    continue;

                btn.neighbour = false;
                btn.transparent = false;
                btn.render(renderer);
            }
        } else {
            for (var btn : store.getAllTilesNonNull()) {
                if (btn == null || btn.piece() == null)
                    continue;
                if (showLittle && !btn.placed && btn.piece().upgrade() instanceof Tool)
                    continue;

                btn.neighbour = false;
                btn.transparent = false;
                btn.render(renderer);
//					if (currentUpgrade != null && !currentUpgrade.placed) {
//						if (currentUpgrade.piece().upgrade() instanceof Upgrade upgrade
//								&& upgrade.unlocks(btn.piece().upgrade())) {
//							btn.renderSelected(renderer, getSelected(), 0f, btn.piece().upgrade().getUpgradeType());
//						}
//					}
            }
        }

        for (int x = 0; x < layerTiles.length; x++) {
            for (int y = 0; y < layerTiles[x].length; y++) {

                /*
                 * Regular tile
                 */

                int timesMod = Math.round(layer.getTimesMod(x, y));
                if (layerTiles[x][y] != null) {
                    if (timesMod <= 1 || layerTiles[x][y].piece().upgrade() instanceof EmptyTile
                            || (currentUpgrade != null && currentUpgrade.placed && (neighbours == null
                            && layerTiles[x][y].piece().isNeighbour(currentUpgrade.piece())
//											|| neighbours != null && TilePiece.contains(neighbours, layerTiles[x][y].piece())
                    ))) { // make
                        // x?
                        // more
                        // visible
                        // even
                        // when
                        // there
                        // are
                        // other
                        // tiles
                        // there
                        continue;
                    }
                }

                /*
                 * None tile
                 */
                if (!hasHoveredOverUpgrades)
                	continue;

                var shader = nullTile.normalSprite().getShader();
                var above = false;
                if (layerTiles[x][y] == null) {
                    if (currentUpgrade != null) {
                        var pos = checkTilePos(currentUpgrade.getPosX(), currentUpgrade.getPosY(), false, layer);
                        if (pos != null && (int) pos.x == x && (int) pos.y == y) {
                            shader.setUniform("mouseTypeScheme", 3f);
                            above = true;
                        }
                    }
                }

                float moneyFac = 1f + (viewPlayerMinimally ? 0 : (5f * layer.getMoney(x, y) / (layer.getMoney(x, y) + 76f)));
                if (timesMod > 1) {
                    timesMod--; // for � ogs� bruke index 1 av farge-arrayet
                    if (!above) {
                        if (layerTiles[x][y] == null) {
                            shader.setUniform("mouseTypeScheme", 2f);
                        } else {
                            shader.setUniform("mouseTypeScheme", 0.3f);
                            shader.setUniform("typeColor", 0);
//								continue;
                        }
                    }
                } else if (!above) {
                    timesMod = 0;
                    shader.setUniform("mouseTypeScheme", 1f);
                }
//					var moneyFac = (((tileRandomFactor + 132f*(x*x+1)*y + 32f*(y+1)*x*x) % 1000f) + 200f) / 8000f;	
//					moneyFac *= 0.75f + (((x+y) % 2f) * 0.5f); 
//					moneyFac += 0.85f;
//					System.out.println(moneyFac);
                shader.setUniform("moneyFac", moneyFac);
                var tm = layer.getTimesMod(x, y);
                shader.setUniform("grayFactor", tm != 0 && tm < 1f ? tm : 1f);

                shader.setUniform("playable", 0);
                boolean transparent = true;
                if (currentUpgrade != null && currentUpgrade.placed) { 
	                if (Math.abs(x - currentUpgrade.piece().x()) <= 1
                		&& Math.abs(y - currentUpgrade.piece().y()) <= 1
                		&& (Math.abs(x - currentUpgrade.piece().x()) + Math.abs(y - currentUpgrade.piece().y())) != 2) {
                		transparent = false;
                	} else {
	                	for (var neigh : TilePiece.getAllNeighbours(layer, currentUpgrade.piece())) {
	                		if (neigh.upgrade().getTileName() != TileNames.NeighborTunnel)
	                			continue;
	                		if (Math.abs(x - neigh.x()) <= 1
	                    		&& Math.abs(y - neigh.y()) <= 1
	                    		&& (Math.abs(x - neigh.x()) + Math.abs(y - neigh.y())) != 2) {
	                    		transparent = false;
	                    		break;
	                    	}
	                	}
                	}
                } else {
                	transparent = false;
                }
            	shader.setUniform("transparent", transparent);
            	shader.setUniform("semitransparent", timesMod > 1. ? .4f : 0);
                
                if (viewPlayerMinimally && !gameOver) {
                    shader.setUniform("improvedLVL", TileVisual.TileColorsTimesMod[0]);
                } else {
                    var color = timesMod < TileVisual.TileColorsTimesMod.length ? TileVisual.TileColorsTimesMod[timesMod]
                            : TileVisual.TileColorsTimesMod[TileVisual.TileColorsTimesMod.length - 1];
                    if (currentUpgrade != null && currentUpgrade.isMoving()) {
                        color = new Vec3(color);
                        color.mul(1.1f);
                    }
                    shader.setUniform("improvedLVL", color);
                }
                var pos = genRealPos(x, y, layer);

                nullTile.setPos(pos.x, pos.y);
                nullTile.render(renderer);

            }
        }

        // int size = 3;
//		for (int i = 0; i < size; i++) {
//			nullTile.setPos(marginX + (((float) i - ((float) size / 2f)) * TileVisual.size()) + ((float) Layer.w / 2f * TileVisual.size()), marginY + ((Layer.h + 1) * TileVisual.size()));
//			nullTile.render(renderer);
//		}

        /*
         * Circle around selected whenever hovering over another tile
         */
        if (pressedTile != null && (currentUpgrade == null || !currentUpgrade.equals(pressedTile))) {
            pressedTile.renderSelected(renderer, getSelected(), 1f, pressedTile.piece().upgrade().getUpgradeType(),
                    true);
        }
//		}

        // Tinted background based on your power!!

    }

    /**
     * Use me to render the buttons and stuff
     */
    @Override
    public void renderUILayout(NkContext ctx, MemoryStack stack) {
//        if (!com.player.resigned && !com.isGameOver() && com.player.role == Player.COMMENTATOR && !viewPlayerMinimally)
//            return;

        var viewPlayer = viewPlayerMinimally ? com.getPlayer(viewPlayerId) : com.player;
        if (viewPlayer == null)
            return;

        boolean commentatorRole = com.player.role == Player.COMMENTATOR;
        if (com.resigned) {
            commentatorRole = true;
        }
        var playing = !commentatorRole;
        var layer = viewPlayer.layer;
        var hyper = layer.getWidth() == 7;
        boolean showLittle = viewPlayerMinimally && !com.isCoop() && playing;

        if (upgradeTabsCurrentBtn == unlocksBtn) {
            Features.inst.pushBackgroundColor(ctx, UIColors.R, 1f);

            unlocks.rowHeightOverrule = TileVisual.size() * .5f;
            unlocks.layout(ctx, stack);

            Features.inst.popBackgroundColor(ctx);
        } else if (upgradeTabsCurrentBtn == tileUpgradesBtn) {
            /*
             * Tiles
             */

            if (showLittle)
                return;
//		Nuklear.nk_style_push_font(ctx, immediateUpgradesFont.getFont());

            var storeTiles = store.getStoreTiles();
            if (storeTiles == null)
                return;
            for (var btn : storeTiles) {
                if (btn == null)
                    return;
                btn.renderUILayout(ctx, stack);
            }
            var toolTiles = store.getToolsTiles();
            if (toolTiles == null)
                return;
            for (var btn : toolTiles) {
                if (btn == null)
                    return;
                btn.renderUILayout(ctx, stack);
            }
            var hyperTiles = store.getHyperTiles();
            if (hyperTiles == null)
                return;
            for (var btn : hyperTiles) {
                if (btn == null)
                    return;
                btn.renderUILayout(ctx, stack);
            }
            var tiles = store.getLayerTiles();
            if (tiles == null || !hasHoveredOverUpgrades)
                return;

            int ranName = 0;
            for (int x = 0; x < layer.getWidth(); x++) {
                for (int y = 0; y < layer.getHeight(); y++) {
                    var isntNull = tiles[x][y] != null;

                    var pos = genRealPos(x, y, null,
                            marginX - (hyper ? TileVisual.size()*spacing : 0),
                            marginY - (hyper ? TileVisual.size()*spacing : 0));

                    // Sale:
                    if ((!isntNull || !(tiles[x][y].piece().upgrade() instanceof EmptyTile)) && layer.hasSale(x, y)) {

                        var saleText = "-" + (int) (100f*(1f-layer.getSale(x, y))) + "%";

                        float size = TileVisual.size();
                        nk_style_push_font(ctx, saleFont.getFont());
                        Features.inst.pushFontColor(ctx, UIColors.RAISIN_BLACK);
                        NkRect shadow = NkRect.malloc(stack);
                        shadow.x(pos.x + .70f * size).y(pos.y - .095f * size).w(size).h(size);
                        if (nk_begin(ctx, "sale" + ranName++, shadow, NK_WINDOW_NO_INPUT | NK_WINDOW_NO_SCROLLBAR)) {
                            nk_layout_row_dynamic(ctx, saleFont.getHeightFloat() * 0.9f, 1);
                            nk_label(ctx, saleText, NK_TEXT_ALIGN_LEFT);
                        }
                        nk_end(ctx);

                        Features.inst.popFontColor(ctx);
                        Features.inst.pushFontColor(ctx, UIColors.WHITE);

                        NkRect front = NkRect.malloc(stack);
                        front.x(pos.x + .68f * size).y(pos.y - .118f * size).w(size).h(size);

                        var name = "sale" + ranName++;
                        if (!SceneHandler.modalVisible && !menu.visible)
                        	nk_window_set_focus(ctx, name);
                        if (nk_begin(ctx, name, front, NK_WINDOW_NO_INPUT | NK_WINDOW_NO_SCROLLBAR)) {
                            nk_layout_row_dynamic(ctx, saleFont.getHeightFloat() * 0.9f, 1);
                            nk_label(ctx, saleText, NK_TEXT_ALIGN_LEFT);
                        }
                        nk_end(ctx);

                        Features.inst.popFontColor(ctx);
                        nk_style_pop_font(ctx);
                    }

                    // render text on tiles:
                    if (isntNull) {
                        tiles[x][y].renderUILayout(ctx, stack);
                    } else {
                        NkRect rect = NkRect.malloc(stack);

                        float size = TileVisual.size();
                        rect.x(pos.x).y(pos.y).w(size).h(size);
                        if (nk_begin(ctx, "timesModRanName" + ranName++, rect,
                                NK_WINDOW_NO_INPUT | NK_WINDOW_NO_SCROLLBAR)) {
                            if (layer.hasTimesMod(x, y)) {
                                nk_layout_row_dynamic(ctx, immediateUpgradesFont.getHeightFloat() * 0.9f, 1);
                                nk_label(ctx, "x" + layer.getTimesModText(x, y), NK_TEXT_ALIGN_LEFT);
                            }
                            if (layer.hasMoney(x, y)) {
                                nk_layout_row_dynamic(ctx, immediateUpgradesFont.getHeightFloat() * 0.9f, 1);
                                nk_label(ctx, "+$" + layer.getMoney(x, y), NK_TEXT_ALIGN_LEFT);
                            }
                        }
                        nk_end(ctx);
                    }
                }
            }
        }
        var ogBorder = ctx.style().window().border();

        ctx.style().window().border(4f * Window.HEIGHT / 1440f);
        /*
         * Tooltip
         */
        if (regValsTooltipRender && regValsTooltip.getWindow().visible) {
            if (currentUpgrade != null && currentUpgrade.not_placeable) {
                Features.inst.pushBackgroundColor(ctx,
                        UIColors.LGRAY, 1f);
                nk_style_push_color(ctx, ctx.style().window().border_color(),
                        UIColors.COLORS[UIColors.R.ordinal()]);
            } else {
                Features.inst.pushBackgroundColor(ctx,
                        currentUpgrade == null || currentUpgrade.placed ? UIColors.LBEIGE : UIColors.EGGSHELL, 1f);
                nk_style_push_color(ctx, ctx.style().window().border_color(),
                        UIColors.COLORS[UIColors.DARKGRAY.ordinal()]);
            }
            if (showExtraTooltip) {
                upgradeInfoDetails.getWindow().z = 0;
                upgradeInfoDetails.setPadding(immediateUpgradesFont.getHeight() / 2f,
                        immediateUpgradesFont.getHeight() / 2f);
                upgradeInfoDetails.layout(ctx, stack);
            }

            regValsTooltip.getWindow().focus = true;
            regValsTooltip.getWindow().z = 0;
            regValsTooltip.layout(ctx, stack);
            Features.inst.popBackgroundColor(ctx);

            if (playing || viewPlayerMinimally) {

                Features.inst.pushBackgroundColor(ctx, UIColors.RICH_BLACK, 1f);
                if (currentUpgrade != null && currentUpgrade.placed
                        && !(currentUpgrade.piece().upgrade() instanceof EmptyTile)) {
                    float padding = improveButtonsWindow.height * .1f;
                    if (improveButtonsWindow.begin(ctx, stack, padding, padding, 0, 0)) {
                        placedTileBtnRow.height = improveButtonsWindow.height * .8f;
                        placedTileBtnRow.layout(ctx, stack);
                    }
                    nk_end(ctx);
                }

                nk_style_pop_color(ctx);
                Features.inst.popBackgroundColor(ctx);
            }
        }

        if (com.player.upgrades.hasTools) {
            Features.inst.pushFontColor(ctx, UIColors.WHITE);
            if (hyper)
                improvementsWindow.setPosition(tabsWindow.x - 1.33f * TileVisual.size(), marginY * .64f);
            else
                improvementsWindow.setPosition(tabsWindow.x - .75f * TileVisual.size(), marginY * .83f);

            if (improvementsWindow.begin(ctx)) {
//				Nuklear.nk_style_push_font(ctx, Window.titleFont.getFont());

                nk_layout_row_dynamic(ctx, improvementsWindow.height, 1);
                nk_label(ctx,
                        layer.getImprovementPoints() + "/" + Tool.improvementPointsNeeded(layer) + " improvements",
                        NK_TEXT_ALIGN_RIGHT | NK_TEXT_ALIGN_TOP);
//				Nuklear.nk_style_pop_font(ctx);
            }
            nk_end(ctx);
            Features.inst.popFontColor(ctx);
        }

        /*
         * Regular windows
         */
        if (playing && !viewPlayerMinimally && !com.isGameOver()) {
            if (com.player.isReady()) {
                readyBtn.setTitle(Texts.ready(true, com.getGamemode().getRaceGoal()));
                readyBtn.setColorUI(UIColors.WON);
            } else {
                readyBtn.setTitle(Texts.ready(false, com.getGamemode().getRaceGoal()));
                readyBtn.setColor(null);
            }
            readyBtn.hoverColor = UIColors.valByInt(0, UIColors.CASTLETON_GREEN);

//			ctx.style().window().spacing().set(0, 0);

            var spacing = NkVec2.malloc(stack);
            spacing.set(0, 0);
            nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);

            Features.inst.pushBackgroundColor(ctx, UIColors.RAISIN_BLACK);
            nk_style_push_color(ctx, ctx.style().window().border_color(),
                    UIColors.COLORS[UIColors.FAWN.ordinal()]);
            Features.inst.pushFontColor(ctx, UIColors.WHITE);
            if (hasHoveredOverLayer && !viewPlayerMinimally) {
                carInfoWindow.focus = false;
                float extraMargin = TileVisual.size() * (UpgradesSubscene.spacing - 1f);
                float extraMarginBig = TileVisual.size() * (spacingBig - 1f);
                var bottomRightTilePos = genRealPos(new Vec2(layer.getWidth(), layer.getHeight()), layer);
                float boardXWidth = bottomRightTilePos.x - extraMargin;
                if (hyper) {
                    boardXWidth += 2f*extraMargin;
                } else {
                    boardXWidth += extraMarginBig;
                }
                carInfoWindow.setPosition(boardXWidth, carInfoWindow.y);
                if (carInfoWindow.begin(ctx)) {
                    for (var info : carInfo) {
                        nk_layout_row_dynamic(ctx, Main.standardFont.getHeightFloat()*.96f, 1);
                        info.layout(ctx, stack);
                    }
                }
                nk_end(ctx);
            }
            // Money
            var groupPadding = NkVec2.malloc(stack);
            var padding = NkVec2.malloc(stack);

            groupPadding.set(0, 0);
            padding.set(moneyWindow.height * 0.05f, moneyWindow.height * 0.1f);

            nk_style_push_vec2(ctx, ctx.style().window().group_padding(), groupPadding);

            nk_style_push_vec2(ctx, ctx.style().window().padding(), padding);

            // Nuklear.nkstyle

            float lineHeight = moneyWindow.height * 0.368f;
            moneyWindow.focus = false;

            float y = marginY - moneyAni.getFrame().getHeight() - TileVisual.size() * (spacingBig - 1f);
            if (layer.getWidth() == 7) y -= .6f*TileVisual.size();
            moneyWindow.setPosition(moneyWindow.x, y);

            if (moneyWindow.begin(ctx)) {

                var player = viewPlayerMinimally ? com.getPlayer(viewPlayerId) : com.player;

                nk_layout_row_begin(ctx, NK_DYNAMIC, lineHeight * 2f, 2);
                nk_layout_row_push(ctx, 0.6f);

                if (nk_group_begin(ctx, "MoneyGroup", NK_WINDOW_NO_SCROLLBAR)) {
                    lineHeight = moneyWindow.height * 0.32f;
                    var moneyPerTurn = player.getCarRep().get(Rep.moneyPerTurn);
                    var interest = player.getCarRep().get(Rep.interest);
                    var fortune = "$" + player.bank.getLong(Bank.MONEY);
                    if (InputHandler.CONTROLLER_EFFECTIVELY)
                        fortune += " (" + (interest >= 0 ? "+" : "") + Texts.formatNumber(100d * interest) + "% int.)";
                    if (moneyPerTurn != 0) {
                        nk_layout_row_dynamic(ctx, lineHeight, 1);
                        nk_label(ctx, fortune, NK_TEXT_ALIGN_LEFT | NK_TEXT_ALIGN_TOP);
                        nk_layout_row_dynamic(ctx, lineHeight, 1);
                        nk_label(ctx,
                                (moneyPerTurn > 0 ? "  +" : "  ") + Texts.formatNumber(moneyPerTurn) + " "
                                        + Texts.tags[Rep.moneyPerTurn],
                                NK_TEXT_ALIGN_LEFT | NK_TEXT_ALIGN_TOP);
                    } else {
                        nk_layout_row_dynamic(ctx, lineHeight * 2f, 1);
                        nk_label(ctx, fortune, NK_TEXT_ALIGN_LEFT | NK_TEXT_ALIGN_MIDDLE);
                    }

                    nk_group_end(ctx);
                }

                undoBtn.hoverable = !menu.visible && !GameInfo.bonusModal.isVisible() && !SceneHandler.modalVisible;
                redoBtn.hoverable = undoBtn.hoverable;

                Features.inst.popFontColor(ctx);
                if (!viewPlayerMinimally) {

                    var whiteText = !undoBtn.isEnabled();
                    if (whiteText)
                        Features.inst.pushFontColor(ctx, UIColors.WHITE);
                    nk_layout_row_push(ctx, 0.195f);
                    undoBtn.layout(ctx, stack);
                    if (whiteText)
                        Features.inst.popFontColor(ctx);

                    nk_layout_row_push(ctx, 0.01f);
                    nk_label(ctx, "", 0);

                    whiteText = !redoBtn.isEnabled();
                    if (whiteText)
                        Features.inst.pushFontColor(ctx, UIColors.WHITE);
                    nk_layout_row_push(ctx, 0.195f);
                    redoBtn.layout(ctx, stack);
                    if (whiteText)
                        Features.inst.popFontColor(ctx);
                }
            } else {
                Features.inst.popFontColor(ctx);
            }
            nk_end(ctx);

            nk_style_pop_vec2(ctx);
            nk_style_pop_vec2(ctx);
            nk_style_pop_vec2(ctx);
            nk_style_pop_color(ctx);

            Features.inst.popBackgroundColor(ctx);
        } else {
            if (historyWindow.begin(ctx, stack, 0, 0, 0, 0)) {

                nk_layout_row_dynamic(ctx, historyWindow.height * 0.85f, 6);
                historyHomeBtn.layout(ctx, stack);
                historyEndBtn.layout(ctx, stack);
                historyBckBtn.layout(ctx, stack);
                historyFwdBtn.layout(ctx, stack);
                historyBckRoundBtn.layout(ctx, stack);
                historyFwdRoundBtn.layout(ctx, stack);
            }
            nk_end(ctx);
        }

        ctx.style().window().border(ogBorder);

    }

    @Override
    public void mousePositionInput(float x, float y) {
    }

//	public void removeLastUpgrade() {
////		audio.get(SfxTypes.BUY_FAILED);
//		store.removeTile(com.player, currentUpgrade);
//		// FIXME trykking av cancel og så endre CurrentUpgrade før du får slettet den du la ned.
//		currentUpgrade = null;
//		pressedTile = null;
//	}

    @Override
    public void createBackground() {
    }

    @Override
    public UIButton<?> intoNavigationSide() {
        return null;
    }

    @Override
    public UIButton<?> intoNavigationBottom() {
        return null;
    }

    @Override
    public void setVisible(boolean visible) {
//		if (upgradeDetails != null)
//			upgradeDetails.getWindow().visible = visible;
    }

    public Sprite getSelected() {
        return TileSprites[TileSprites.length - 1];
    }

    @Override
    public void destroy() {
        for (var tileSprite : TileSprites) {
            tileSprite.destroy();
        }
    }

    public void reset() {
        currentUpgrade = null;
        pressedTile = null;
        hasHoveredOverLayer = false;
        showedExplaination = false;
    }

    public float getCarInfoX() {
        return carInfoWindow.x;
    }

    public float getCarInfoWidth() {
        return carInfoWindow.width;
    }

    public float getTooltipY() {
        return upgradeInfoDetailsY;
    }

    public float getTooltipHeight() {
        return upgradeInfoDetailsH;
    }

    public void closeAllBeforeRace() {
        var bm = store.getBonusModal();
        if (bm.isVisible()) {
            bm.cancel(com.player);
            bm.hide();
            reactBonus(false);
        }

    }

    public void viewPlayer(Player player, boolean setPlayer, boolean viewPlayerMinimally) {
        if (viewPlayerId == player.id)
            return;
        viewPlayerId = player.id;
        if (setPlayer)
            this.com.player = player;
        this.viewPlayerMinimally = viewPlayerMinimally && player.id != com.player.id;
        store.resetTowardsPlayer(player);
    }

    public Player getViewedPlayer() {
        return this.com.player;
    }

    public float getMoneyWindowY() {
        return moneyWindow.y;
    }

    public void afterJoined() {
        tileRandomFactor = 1000f * Features.ran.nextFloat();
        com.setActionRemakeUpgradeView(() -> {
            if (this.viewPlayerMinimally) {
                store.resetTowardsPlayer(com.getPlayer(viewPlayerId));
                showNoUpgrades();
            } else if (com.isCoop()) {
                showUpgrades();
            }
        });
    }

    public boolean hasPressedUpgrade() {
        return pressedTile != null;
    }

    private Sprite getBackground(int i) {
        return switch (i) {
            case 0: yield backgroundImage;
            default:
                yield extraBackgroundImages[i-1];
        };
    }

    private void newBackground() {
        backgroundAlpha = 1f;
        backgroundWait = 2f;
        currentBackground = nextBackground;
        currentBackgroundUpsideDown = nextBackgroundUpsideDown;
        nextBackground = Features.ran.nextInt(extraBackgroundImages.length + 1);
        nextBackgroundUpsideDown = Features.ran.nextBoolean();
        System.out.println("Next background:" + nextBackground);
    }

	@Override
	public void renderBackground(Renderer renderer) {
        var spd = Timer.lastDelta / 150f;
        if (backgroundWait > 0f) {
            backgroundWait -= spd;
        } else {
            backgroundAlpha -= spd;

            if (backgroundAlpha <= 0f) {
                newBackground();
            }
        }

        //current
        var sprite = getBackground(currentBackground);
        sprite.getShader().setUniform("alpha", backgroundAlpha);
        sprite.setPositionZ(-1);
        if (currentBackgroundUpsideDown)
            sprite.setRotation(new Vec3(0, 0, 180));
        else
            sprite.setRotation(new Vec3(0, 0, 0));
        renderer.renderOrthoMesh(sprite);

        // next
        sprite = getBackground(nextBackground);
        sprite.getShader().setUniform("alpha", 1f - backgroundAlpha);
        sprite.setPositionZ(-1);
        if (nextBackgroundUpsideDown)
            sprite.setRotation(new Vec3(0, 0, 180));
        else
            sprite.setRotation(new Vec3(0, 0, 0));
        renderer.renderOrthoMesh(sprite);
    }
    
}
