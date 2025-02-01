package player_local.upgrades;

import static org.lwjgl.nuklear.Nuklear.nk_end;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import adt.IActionRet;
import engine.graphics.objects.Sprite;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.IUIPressable;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIWindowInfo;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import engine.math.Vec2;
import engine.math.Vec3;
import main.Texts;
import player_local.Bank;
import player_local.Layer;
import player_local.Player;
import player_local.TilePiece;
import scenes.Scenes;
import scenes.game.lobby_subscenes.UpgradesSubscene;

// Hva om man visualiserer fra et annet sted og s� oppdaterer det visuelle n�r enn stats tile klassen endres.

public class TileVisual implements IUIPressable {
	static class LayoutTextStruct {

		IActionRet<String> text;
		IActionRet<UIColors> color;

	}

	public final static Vec3[] TileColors = { new Vec3(0), new Vec3(4f / 255f, 47f / 255f, 102f / 255f),
			new Vec3(184f / 255f, 46f / 255f, 8f / 255f), new Vec3(28f / 255f, 69f / 255f, 39f / 255f),
			new Vec3(0f / 255f, 141f / 255f, 145f / 255f), new Vec3(99f / 255f, 10f / 255f, 68f / 255f), };

	public static final Vec3[] TileColorsTimesMod = { new Vec3(0.8f), new Vec3(70f / 255f, 140f / 255f, 232f / 255f),
			new Vec3(227f / 255f, 14f / 255f, 49f / 255f), new Vec3(212f / 255f, 196f / 255f, 93f / 255f),
			new Vec3(0f / 255f, 209f / 255f, 175f / 255f), new Vec3(1.0f, 10f / 255f, 170f / 255f), };

	private TilePiece<?> piece;

	public boolean placed, mouseAbove, transparent, neighbour, not_placeable;
	public static final int stdZ = -1;
	public int z = stdZ;
	protected boolean dragOn, movable = true, pressable, mouseDown;

	private final int normalSprite;

	// x og y er basert p� normal posisjon, men diffx og y flytter den litt og.
	protected int diffX, diffY, dragX, dragY, diffOGX, diffOGY;
	protected float diffXBased, diffYBased;

	protected IAction actionPressedRight, actionHoveredExit, actionHovered;
	protected IActionCheckPosition actionMoveTileBuy, actionMouseAbove, actionUiUpdate;
	protected Consumer<IUIPressable> actionPressedSelf;
	protected final CopyOnWriteArrayList<LayoutTextStruct> actionLayoutTexts = new CopyOnWriteArrayList<>();
	protected IActionRet<Vec3> actionBgColor;

	private UIWindowInfo window, windowShadow, windowOutline;
	protected UILabel label;
	public int logicalX, logicalY;

	/*
	 * Init/new state parts
	 */

	public TileVisual(int normalSprite) {
		this.normalSprite = normalSprite;
		release();
	}

	public void delete() {
		for (int i = 0; i < Scenes.AMOUNT; i++) {
			UISceneInfo.removeWindowInfoReference(i, window);
			UISceneInfo.removeWindowInfoReference(i, windowShadow);
			UISceneInfo.removeWindowInfoReference(i, windowOutline);
			UISceneInfo.removePressableReference(i, this);
		}
	}

	private int playable() {
		if (piece != null) {
			if (piece.upgrade().isOpenForUse() || piece.upgrade().isPlaced())
				return 1;
		}
		return 2;
	}

	public TilePiece<?> piece() {
		return piece;
	}

	public void resetExistingPiece(Player player) {
		reset(player, piece);
	}

	public void reset(Player player, TilePiece<?> piece) {
		this.piece = piece;
		actionLayoutTexts.clear();
		actionBgColor = null;

		if (piece == null || piece.upgrade() == null)
			return;

		placed = piece.upgrade().isPlaced();
		movable = !placed && piece.upgrade().isOpenForUse();
		dragOn = false;
		not_placeable = false;

		if (piece.upgrade() instanceof Upgrade upgrade) {

			var line0 = new LayoutTextStruct();
			line0.text = () -> !upgrade.isFullyUpgraded() ? "$" + upgrade.getCost(player.layer.getSale(piece.x(), piece.y())) : "";
			line0.color = () -> upgrade.isFullyUpgraded() ? UIColors.WHITE
					: player.bank.canAfford(upgrade.getCost(player.layer.getSale(piece.x(), piece.y())), Bank.MONEY) ? UIColors.G : UIColors.R;
			actionLayoutTexts.add(line0);

			if (placed) {
				var line1 = new LayoutTextStruct();
				line1.text = () -> {
					if (player.layer.hasTimesMod(piece.x(), piece.y())) {
						float modifier = player.layer.getTimesMod(piece.x(), piece.y());
						return "x" + Texts.formatNumber(modifier);
					}
					return "";
				};
				line1.color = () -> UIColors.WHITE;
				actionLayoutTexts.add(line1);

				var line2 = new LayoutTextStruct();
				line2.text = () -> {
					if (player.layer.hasMoney(piece.x(), piece.y())) {
						float money = player.layer.getMoney(piece.x(), piece.y());
						return "+$" + Texts.formatNumber(money);
					}
					return "";
				};
				line2.color = () -> UIColors.WHITE;
				actionLayoutTexts.add(line2);

				actionBgColor = () -> {
					if (upgrade.getLVL() < TileColors.length) {
						return TileColors[upgrade.getLVL()];
					} else {
						return UIColors.infiniteColor(upgrade.getLVL());
					}
				};
			} else {
				int maxlvl = upgrade.getMaxLVL();
				if (maxlvl != 1) {
					var line1 = new LayoutTextStruct();
					line1.text = () -> upgrade.getLVL() + (maxlvl != -1 ? "/" + maxlvl : "");
					line1.color = () -> UIColors.WHITE;

					actionLayoutTexts.add(line1);
				}
			}

		} else if (piece.upgrade() instanceof Tool tool) {
			if (!placed) {
				var line = new LayoutTextStruct();
				line.text = () -> tool.isOpenForUse() ? "$" + tool.getCost(player.layer.getSale(piece.x(), piece.y())) : "";
				line.color = () -> !tool.isOpenForUse() ? UIColors.WHITE
						: player.bank.canAfford(tool.getCost(player.layer.getSale(piece.x(), piece.y())), Bank.MONEY) ? UIColors.G : UIColors.R;
				actionLayoutTexts.add(line);

			} else {
				var line = new LayoutTextStruct();
				line.text = () -> {
					if (player.layer.hasTimesMod(piece.x(), piece.y())) {
						return "x" + Texts.formatNumber(player.layer.getTimesMod(piece.x(), piece.y()));
					}
					return "";
				};
				line.color = () -> UIColors.WHITE;

				actionLayoutTexts.add(line);

				var line2 = new LayoutTextStruct();
				line2.text = () -> {
					if (player.layer.hasMoney(piece.x(), piece.y())) {
						float money = player.layer.getMoney(piece.x(), piece.y());
						return "+$" + Texts.formatNumber(money);
					}
					return "";
				};
				line2.color = () -> UIColors.WHITE;
				actionLayoutTexts.add(line2);
			}
		}

	}

	/*
	 * Upgrade parts
	 */

	public ArrayList<IUIObject> getInfo(int turn, Layer layer, Upgrades upgrades, int x, int y) {
		var res = new ArrayList<IUIObject>();

		if (piece.upgrade() instanceof Upgrade upgrade) {
			if (upgrade.getUpgradeType() == UpgradeType.NEG) {
				var text = """
						There's something wrong with my ride!
						Placing tiles next to this one will make
						it even worse! I'll have to buy its
						disappearance one day...
						""";
				Collections.addAll(res, UILabel.split(text, "\n"));
			} else {

				var maxLVL = upgrade.getMaxLVL();
				var lvl = upgrade.getLVL();
				var priceOG = Math.round(upgrade.getPriceOG());
				if (placed || maxLVL != 1) {
					var text = new StringBuilder();
					if (placed) {
						text.append("LVL: ").append(lvl).append(" / ").append(maxLVL == -1 ? "Infinite" : maxLVL);

						var timesMod = layer.getTimesMod(x, y);
						if (timesMod > 1f) {
							text.append("; x").append(Texts.formatNumber(timesMod));
						}

						text.append("; Price when placed");
					} else {
						text.append("Starting price");
					}
					text.append(": $").append(priceOG);
					res.add(new UILabel(text.toString()));
				}

				var text = new StringBuilder();
				if (!placed) {
					text.append("    ").append(Texts.getUpgradeInfo(upgrade)).append("\n");

					var regularValues = upgrade.getRegVals();
					String changeAfterUpgradeText = regularValues.getUpgradeRepString(upgrades,
							regularValues.changeAfterUpgrade, 0, 0);
					if (!changeAfterUpgradeText.isBlank()) {
						text.append("Next store-tile:#").append(UIColors.BONUSGOLD0).append("\n")
								.append(changeAfterUpgradeText).append("\n");
					}

				} else {
					var maxPercentage = new UILabel("Percentage-limit: " + Texts.formatNumber(100f*upgrade.percentLimit()) + "%");
					res.add(maxPercentage);
					var provides = new UILabel("  This tile provides:");
					provides.tooltip = "Stats below are removed from car if you sell this tile";
					res.add(provides);
					text.append(upgrade.getGainedValues().toPlainInfoString(42, UIColors.WON));
				}
				var bonuses = upgrade.getBonusLVLs().length;
				if (bonuses > 0) {
					text.append("\nBonuses:");
				}
				Collections.addAll(res, UILabel.split(text.toString(), "\n"));
				if (bonuses > 0) {
					Collections.addAll(res, upgrade.getInfoBonuses(upgrades));
				}
			}
		} else if (piece.upgrade() instanceof Tool tool) {
			Collections.addAll(res, UILabel.split(switch (tool.getTileName()) {
			case NeighborCollector -> {
				var text = """
						Like getting a free improvement every turn
						it collects neighbor bonuses.
						After each gathering it collects 50% more,
						starting at 100% and then 150% etc.
						  Additionally, it collects 25% of the 
						money laying on neighboring tiles.""";
				if (placed) {

					var regVal = Tool.collectorRegVals(layer, x, y, turn, tool);

					text = "Currently " + (int) (Tool.collectorMultiply(turn, tool) * 100d) + "%! Per turn you get:\n"
							+ RegValList.toPlainInfoString(regVal.getUpgradeRepString(upgrades, 0, 2), 42,
									UIColors.STAR_COMMAND_BLUE);
				}
				yield text;
			}
			case TimesModPlanter -> """
					Plants x0.2 on every adjacent tile and
					x0.1 on itself after each race.
					However, if it is planting on a x6 or
					higher it will only plant x0.1
					  If an affected tile is below x1.0 
					it will first reset it to x1.0 before
					planting next turn.
					""";
			case Dilator -> """
					Doubles the neighboring bonus (=>) of
					neighbors. For instance:
					=> +100 rpm becomes => +200 rpm.""";
			case NeighborTunnel -> """
					Tunnels neighboring bonus (=>) to neighbors, 
					and the neighbors' neighbors. 
					""";
			case Seeper -> """
					Spreads base values of neighboring tiles
					around to other neighboring tiles every
					round.
					  Copies initially from 10% each value.
					However, if the value type already is 
					present it will instead grow decimals by 
					+10% and percents by +5%
					Additionally it increases all neighbor
					bonuses by 5% every turn.""";
			case LeftRotator -> """
Rotates tiles counter-clock-wise around.

!! Can be placed on top of and combine with
other tools on the board!!""";
			case RightRotator -> """
			Rotates tiles clock-wise around each round.
			
			!! Can be placed on top of and combine with
			other tools on the board!!""";
			case Merchant -> (placed ? "Currently placing $" + ((turn - tool.getPlacedTurn() + 1) * 4) + " on all sides...\n"
					: "") + """
					Places $4*ROUND around each round, and
					increases the selling-point of tiles
					around by +25% or +$12 each round,
					whichever is largest, but no more than
					+$3000.
					  This tool also reduces the
					Times-Mod penalty of selling under x1;
					from -0.2x to -0.1x
					  "$4*round" means that for each round
					that has passed since you placed this
					tool, it places an additional $4.
					Therefore after 5 rounds it will have
					placed $4+$8+$12+$16+$20
					""";
			case Permanentifier -> """
					Removes 50% of what tiles around provide 
					per round. This means that if you sell 
					any of these tiles you will lose less 
					stats.
					  Removes the Times-Mod penalty of 
					selling.
					  Disperses LVLs once per turn.
					  Removes 1 LVL from one randomly picked 
					neighboring tile.
					""";
			case Yunomah -> """
					Runs direct neighboring tools on itself. 
					  For example; if you have a Times Mod 
					Planter next to a Yunomah then the Yunomah 
					will also act as a Times Mod Planter. 
					  Does not react to its own type and any 
					Neighbor Tunnel though (unless acting as a
					Dilator).
					""";
//			case Uninsion -> """
//					Uses base values into darkness,
//					and darkness makes
//					the car stronger but red-lining is worse.
//					""";

			default -> throw new IllegalStateException("Unexpected value: " + tool.getTileName());
			}, "\n"));
		}

		return res;

	}

	public String tooltip(Layer layer, Upgrades upgrades, int x, int y) {
		StringBuilder lines = new StringBuilder();
		boolean onLayer = x != -1;

		if (piece.upgrade() instanceof Upgrade upgrade) {

			RegVals regVals;
			if (onLayer)
				regVals = Upgrade.modRegValCloned(upgrade.getRegVals(), layer, x, y, upgrade.percentLimit());
			else
				regVals = upgrade.getRegVals();

			var upgradeTooltipLines = new ArrayList<String>();
			if (upgrade.getUpgradeType() != UpgradeType.NEG) {
				upgradeTooltipLines.addAll(regVals.getUpgradeRepString(upgrades, onLayer));
			}
			upgradeTooltipLines.add("=> "
					+ piece.getNeighbourModifierWithTools(layer, x, y, false).getUpgradeRepString(upgrades, 0, 1));

			for (var regValStr : upgradeTooltipLines) {
				lines.append(regValStr).append("\n");
			}
		} else if (piece.upgrade() instanceof Tool tool) {

			lines = new StringBuilder(switch (tool.getTileName()) {
				case NeighborCollector -> "Gathers 'neighboring bonus' or '=>' and money $ of adjecent tiles every turn";
				case TimesModPlanter -> "Plants 'x?' modifier every turn";
				case Dilator -> "Doubles 'neighboring bonus' or '=>' of every neighbor";
				case NeighborTunnel ->
						"Sends 'neighboring bonus' or '=>' of every neighbor to every neighbor's neighbor";
				case Seeper -> "Adds 'base values' of adjecent tiles to eachother's 'neighboring bonus' or '=>'";
				case LeftRotator -> "Rotates adjecent tiles counter-clockwise";
				case RightRotator -> "Rotates adjecent tiles clockwise";
				case Merchant -> "Makes sales better and improves land-profitability";
				case Permanentifier -> "Reduces loss from selling and disperses LVLs";
				case Yunomah -> "Runs neighboring tools on itself";
//				case Uninsion -> "Darkness";
				default -> throw new IllegalStateException("Unexpected value: " + tool.getTileName());
			});
		}

		return lines.toString();
	}

	/*
	 * Rendering
	 */

	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		if (playable() > 1)
			return;

		label.rowHeight = size() / 4f;
		label.rowColomns = 1;
		label.widthDynamicOverall = .5f;
		label.noImage = false;

		// Svarthet bak tekst
		windowShadow.focus = false;
		windowOutline.focus = false;
		window.z = z;
		windowShadow.z = z;
		windowOutline.z = z;
		if (windowShadow.begin(ctx)) {
			for (var text : actionLayoutTexts) {
				label.setText(text.text.run());
				label.setColor(UIColors.BLACK);
				label.layout(ctx, stack);
			}
		}
		nk_end(ctx);
		if (windowOutline.begin(ctx)) {
			for (var text : actionLayoutTexts) {
				label.setText(text.text.run());
				label.setColor(UIColors.BLACK_TRANSPARENT);
				label.layout(ctx, stack);
			}
		}
		nk_end(ctx);

		if (window.begin(ctx)) {
			for (var text : actionLayoutTexts) {
				label.setText(text.text.run());
				label.setColor(text.color.run());
				label.layout(ctx, stack);
			}
		}
		nk_end(ctx);
	}

	public void render(Renderer renderer) {
		Sprite sprite = null;
		if (piece != null) {
			if (piece.upgrade() instanceof EmptyTile)
				return;
			if (piece.upgrade() instanceof Tool tool && tool.hasRotator()) {
				if (!Tool.lastRenderedRotator) {
					sprite = normalSprite(tool.getRotator());
				}
			}
			if (piece.upgrade() instanceof HyperUpgrade hyperUpgrade) {
				var patterns = hyperUpgrade.getPatterns();
				var patternLevels = hyperUpgrade.getPatternLevels();
				for (int i = 0; i < patterns.length; i++) {
					sprite = normalSprite(TileNames.Pattern0_.ordinal() + patterns[i]);
					var shader = sprite.getShader();
					if (i == 0) {
						shader.setUniform("hyperback", 1);
					} else {
						shader.setUniform("hyperback", 0);
					}
					shader.setUniform("hypercolor", patternLevels[i]);
					renderNormalSprite(renderer, sprite, true);
				}
				return;
			}
		}
		if (sprite == null) {
			sprite = normalSprite(normalSprite);
		}
		renderNormalSprite(renderer, sprite, false);
	}

	private void renderNormalSprite(Renderer renderer, Sprite sprite, boolean hyper) {
		if (actionBgColor != null) {
			sprite.getShader().setUniform("improvedLVL", actionBgColor.run());
		} else {
			sprite.getShader().setUniform("improvedLVL", TileColors[0]);
		}
		sprite.getShader().setUniform("ishyper", hyper);
		sprite.getShader().setUniform("playable", playable());
		sprite.getShader().setUniform("transparent", transparent);
		sprite.getShader().setUniform("neighbour", neighbour);
		sprite.getShader().setUniform("not_placeable", not_placeable);

		float typeScheme = 0;
		// hover or pressed
		if (mouseAbove) {
			if (mouseDown)
				typeScheme = 1;
			else
				typeScheme = 2;
		}

		sprite.getShader().setUniform("mouseTypeScheme", typeScheme);
		sprite.setPositionX(diffX);
		sprite.setPositionY(diffY);
		sprite.setPositionZ(mouseAbove ? 0 : -1);
		renderer.renderOrthoMesh(sprite);
	}

	public void renderSelected(Renderer renderer, Sprite selected, float pressedTile, UpgradeType type,
			boolean transparent) {
		selected.getShader().setUniform("mouseTypeScheme", pressedTile);
		if (type == UpgradeType.NEG)
			type = UpgradeType.POWER;
		// mus over - vis farge
		// mus vekke - vis generellt merke
		selected.getShader().setUniform("typeColor", (type != null && (mouseAbove || dragOn)) ? type.ordinal() + 1 : 0);
		selected.getShader().setUniform("grayFactor", 1f);
		selected.getShader().setUniform("moneyFac", 1f);
		selected.getShader().setUniform("playable", transparent ? 0 : 100);

		selected.setPositionX(diffX);
		selected.setPositionY(diffY);
		renderer.renderOrthoMesh(selected);
	}

	/*
	 * Input
	 */

	public boolean mousePosInput(float x, float y) {
		if (piece != null && piece.upgrade() instanceof EmptyTile)
			return false;

		boolean prevMouseAbove = mouseAbove;
		Vec2 tilePos = new Vec2(diffX, diffY);
		mouseAbove = normalSprite(normalSprite).aboveWithMargin(tilePos, x, y, .5f*TileVisual.size() * (1f - UpgradesSubscene.spacing));

		if (!prevMouseAbove && mouseAbove) {
			runHoveredAction();
			if (actionMouseAbove != null) {
				actionMouseAbove.check(this, tilePos);
			}
		} else if (prevMouseAbove && !mouseAbove) {
			if (actionHoveredExit != null) {
				actionHoveredExit.run();
			}
		}

		if (dragOn) {
			setPos(diffOGX - (dragX - x), diffOGY - (dragY - y));
			actionUiUpdate.check(this, new Vec2(diffX, diffY));
		}

		return mouseAbove;
	}

	public boolean mouseButtonInput(int action, float x, float y) {
		mouseDown = action != GLFW.GLFW_RELEASE;

//		System.out.println(x + ", " + y + " - " + diffX + ", " + diffY);

		if (mouseDown) {
			release();
			if (pressable) {
				if (mouseAbove) {
//					System.out.println("pressed");
					runPressedAction();
					if (movable) {
						dragX = (int) x;
						dragY = (int) y;
					}
				}
			}
		} else if (dragOn) {
			setMoving(false);
		}

		return mouseAbove;
	}

	@Override
	public void press() {
		pressable = false;
	}

	@Override
	public void release() {
		pressable = true;
	}

	public void runPressedAction() {
		runPressedAction(GLFW.GLFW_MOUSE_BUTTON_LEFT);
		if (movable) {
//			System.out.println("mouse down");
			dragOn = true;
			diffOGX = diffX;
			diffOGY = diffY;
		}
	}

	public boolean runPressedAction(int button) {
		boolean res = false;

		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
			if (actionPressedSelf != null) {
				actionPressedSelf.accept(this);
				res = true;
			}

		} else if (actionPressedRight != null && button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
				&& InputHandler.MOUSEACTION == GLFW.GLFW_RELEASE) {
			actionPressedRight.run();
			res = true;
		}

		return res;
	}

	public void runHoveredAction() {
		// Play hover sfx
		if (actionHovered != null) {
			actionHovered.run();
		}
	}

	/*
	 * Setters, getters and helper methods
	 */

	public void setActionHoverExit(IAction action) {
		this.actionHoveredExit = action;
	}

	public void setActionPressed(Consumer<IUIPressable> action) {
		this.actionPressedSelf = action;
	}

	public void setActionMouseAbove(IActionCheckPosition actionMouseAbove) {
		this.actionMouseAbove = actionMouseAbove;
	}

	public void setActionMovedBuy(IActionCheckPosition action) {
		this.actionMoveTileBuy = action;
	}

	public void setActionUpdateUI(IActionCheckPosition uiUpdate) {
		this.actionUiUpdate = uiUpdate;
	}

	public void updateResolution() {
		diffX = (int) ((float) Window.WIDTH * diffXBased);
		diffY = (int) ((float) Window.HEIGHT * diffYBased);
	}

	public void setPos(int logicalX, int logicalY, float x, float y) {
		this.logicalX = logicalX;
		this.logicalY = logicalY;
		setPos(x, y);
	}
	
	public void setPos(float x, float y) {
		diffX = (int) x;
		diffY = (int) y;

		diffXBased = (float) diffX / (float) Window.WIDTH;
		diffYBased = (float) diffY / (float) Window.HEIGHT;

		if (piece != null && piece.upgrade() instanceof EmptyTile)
			return;

		if (label == null) {
			label = new UILabel();
			label.options = Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE;

			double w = normalSprite().getWidth(), h = normalSprite().getHeight();
			window = UISceneInfo.createWindowInfo(Scenes.LOBBY, x, y, w, h);
			windowShadow = UISceneInfo.createWindowInfo(Scenes.LOBBY, x, y, w, h);
			windowOutline = UISceneInfo.createWindowInfo(Scenes.LOBBY, x, y, w, h);
		}

		window.setPosition(diffX, diffY);
		windowShadow.setPosition(diffX + shadow(), diffY + shadow());
		windowOutline.setPosition(diffX - shadow() / 4f, diffY - shadow() / 4f);

	}

	public int getPosX() {
		return diffX;
	}

	public int getPosY() {
		return diffY;
	}

	public boolean isMoving() {
		return dragOn;
	}

	public void setMoving(boolean dragOn) {
		this.dragOn = dragOn;
		
		if (!dragOn && actionMoveTileBuy != null && !actionMoveTileBuy.check(this, new Vec2(diffX, diffY))) {
			setPos(diffOGX, diffOGY); // feilet, g� tilbake til der du kom fra
			mouseAbove = false;
			InputHandler.forceMousePos();
			// if (mouseAbove)
			actionUiUpdate.check(this, new Vec2(diffX, diffY));
			not_placeable = false;
		}
	}

	public static int size() {
		int size = Window.HEIGHT / 12;
		if (((float) Window.OG_WIDTH / (float) Window.OG_HEIGHT) < 16f / 10f) {
			size = Window.WIDTH / 20;
		}
			
		int a = 32;
		while (a < size) {
			a += 32;
		}

		return size;
	}

	private float shadow() {
		return size() / 48f;
	}

	public Sprite normalSprite() {
		return normalSprite(normalSprite);
	}

	public Sprite normalSprite(int normalSprite) {
		if (piece != null && (piece.upgrade().isOpenForUse() || piece.upgrade().isPlaced()) && (UpgradesSubscene.currentUpgrade == this || neighbour)) {
			switch (UpgradesSubscene.TileSpriteFrame) {
				case 0:
				return UpgradesSubscene.TileSprites[normalSprite];
				case 1:
				return UpgradesSubscene.TileSprites2[normalSprite];
				case 2:
				return UpgradesSubscene.TileSprites3[normalSprite];
				case 3:
				return UpgradesSubscene.TileSprites4[normalSprite];
			}
		}
		return UpgradesSubscene.TileSprites[normalSprite];
	}

}
