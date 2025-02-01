package engine.graphics.ui.modal;

import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_label;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.function.Consumer;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import communication.GameInfo;
import communication.Translator;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.IUIPressable;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UIWindowInfo;
import player_local.upgrades.Store;
import player_local.upgrades.Upgrade;
import player_local.upgrades.UpgradeResult;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import scenes.Scenes;

/**
 * Hjelpeklasse for � drive gjennom bonuser enten grafisk eller ei. Skal ikke
 * knyttes mot noen spesifik game eller spiller.
 * 
 * @author jh
 *
 */
public class UIBonusModal implements IUIObject, IUIPressable {

	static class ChosenBonus {
		int nameID, bonusLVL, gold;

		public ChosenBonus(int nameID, int bonusLVL, int gold) {
			this.nameID = nameID;
			this.bonusLVL = bonusLVL;
			this.gold = gold;
		}
	}

	private final UIButton<Integer> gainBoltBtn, cancelBtn;
	private final ArrayList<UIButton<Integer>> bonusBoltBtns;
	private final Consumer<Integer> okAction;
	private final UIWindowInfo window;
	private final UILabel titleLabel;
	private final UILabel[] infos;

	private final Stack<ChosenBonus> chosenBonuses = new Stack<>();
	public final Queue<Upgrade> upgrades = new LinkedList<>();
	private Player combination;
	private boolean freeBonus;
	private Upgrade initialUpgrade;
	private String revertState;
	private float alpha;
	private boolean didInput;

	public UIBonusModal(Consumer<Integer> okAction, IAction cancelAction) {
		this.okAction = okAction;
		bonusBoltBtns = new ArrayList<UIButton<Integer>>();
		gainBoltBtn = new UIButton<>("", UIColors.WEAKGOLD);
		gainBoltBtn.setPressedAction(okAction);
		gainBoltBtn.setConsumerValue(-1);
		cancelBtn = new UIButton<>(Texts.exitCancelText, UIColors.DARKGRAY);
		cancelBtn.setPressedAction(cancelAction);

		titleLabel = new UILabel();
		titleLabel.options = Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE;
		infos = new UILabel[] { new UILabel("", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_LEFT),
				new UILabel(
						"    You take the money, this bonus ends. You get to use it for whatever you want to. You take a bonus, and I show you how fast this car can go!",
						Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_LEFT),
				new UILabel()
//				new UILabel("    -The more you spend the better your car becomes, but you might not have enough " + Texts.bonusBolt + "s for other bonuses!", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_LEFT),
//				new UILabel("    -On their own " + Texts.bonusBolt + "s are useless!", Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_LEFT),
		};

		window = UISceneInfo.createWindowInfo(Scenes.LOBBY, 0, 0, Window.WIDTH, Window.HEIGHT);
		window.visible = false;
		window.z = 2;
	}

	/**
	 * @return 0 spill av failed, 1 kj�p spill avgoldType + 1 2 ny bonus!
	 */
	public UpgradeResult select(Player player, Store store, int goldType) {
		Upgrade upgrade = upgrades.peek();
		var playerCombiner = combination;
		playerCombiner.bank.set(player.bank);
		UpgradeResult res = UpgradeResult.DidntGoThrough;
		if (goldType == -1) {
			playerCombiner.bank.add(gainAmount(upgrade), Bank.MONEY);
		} else if (goldType < upgrade.getBonuses()[upgrade.getBonusLVL()].getAmountChoices()) {

			int goldCost = calcBonusCost(upgrade, goldType);
			int tileCost = upgrade.getCost(1);

			if (playerCombiner.bank.canAfford(goldCost + tileCost, Bank.MONEY)) {
				playerCombiner.bank.buy(goldCost, Bank.MONEY);
			} else {
				return UpgradeResult.DidntGoThrough;
			}
			upgrade.getBonuses()[upgrade.getBonusLVL()].upgradeWithHooks(playerCombiner, goldType, upgrade.getNameID());

			// 0 = failed, 1 = bought, 2 = newbonus
			if (freeBonus) {
				boolean more = store.isBonusToChooseFirst(playerCombiner, upgrade, true);
				res = (more || upgrades.size() > 1 ? UpgradeResult.FoundBonus : UpgradeResult.Bought);
			}
			goldType++;
		} else {
			return UpgradeResult.DidntGoThrough;
		}

		((Upgrade) playerCombiner.upgrades.getUpgradeRef(upgrade)).setBonusChoice(upgrade.getBonusLVL(), goldType);
		pushBonusChoice(upgrade.getNameID(), upgrade.getBonusLVL(), goldType);

		if (res != UpgradeResult.FoundBonus) {
			// om du kommer her s� har du ikke betalt for oppgraderingen enn�.
			res = store.upgrade(playerCombiner,
					(Upgrade) playerCombiner.upgrades.getUpgrade(initialUpgrade.getNameID()), -1, -1, true);
		}

		if (res != UpgradeResult.FoundBonus) {
			freeBonus = false;
			if (res == UpgradeResult.Bought) {
				upgrade.addToPriceTotal(upgrade.getCost(1));
				combine(player);
				upgrades.clear();
			}
		} else {
			upgrades.poll();
			freeBonus = true;
		}
		return res;
	}

	public void tick(double delta) {
		if (!isVisible())
			return;

		if (alpha < 0.95f) {
			alpha += 0.30f * delta;
		} else {
			alpha = 0.95f;
		}
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {
		if (!upgrades.isEmpty()) {
			Features.inst.pushFontColor(ctx, UIColors.WHITE);
			Features.inst.pushBackgroundColor(ctx, UIColors.BLACK, alpha);

			if (window.begin(ctx)) {
				float height = window.height;
				float heightElements = height / 10;

				nk_layout_row_dynamic(ctx, heightElements, 1); // SPACE

				Nuklear.nk_style_push_font(ctx, Window.titleFont.getFont());
				nk_layout_row_dynamic(ctx, Window.titleFont.getHeight() * 1.1f, 1);
				titleLabel.alpha = alpha;
				titleLabel.layout(ctx, stack);
				Nuklear.nk_style_pop_font(ctx);

				nk_layout_row_dynamic(ctx, heightElements, 1); // SPACE

				// HINTS:
				for (var label : infos) {
					nk_layout_row_dynamic(ctx, heightElements / 2, 1);
					label.alpha = alpha;
					label.layout(ctx, stack);
				}

				// SPACE
				nk_layout_row_dynamic(ctx, heightElements * 2 / 3, 1);

				Features.inst.pushBackgroundColor(ctx, UIColors.BLACK, 0);

				NkVec2 group_padding = NkVec2.malloc(stack);
				group_padding.set(heightElements * 0.7f, 0);
				Nuklear.nk_style_push_vec2(ctx, ctx.style().window().group_padding(), group_padding);

				/*
				 * Options
				 */
				int goldBtnsSize = bonusBoltBtns.size();
				nk_layout_row_dynamic(ctx, Window.HEIGHT / 4f, 2);
				if (Nuklear.nk_group_begin(ctx, "boltsOptions", 0)) {
					for (var btn : bonusBoltBtns) {
						if (btn == null)
							continue;
						nk_layout_row_dynamic(ctx, heightElements * (goldBtnsSize > 1 ? 0.5f : 1f), 1);
						btn.alphaFactor = alpha;
						btn.layout(ctx, stack);
					}
					Nuklear.nk_group_end(ctx);
				}
				// +1 bolt valg
				if (Nuklear.nk_group_begin(ctx, "gainBoltOption", 0)) {
					nk_layout_row_dynamic(ctx, heightElements * (goldBtnsSize > 1 ? 0.5f * goldBtnsSize : 1f), 1);
					gainBoltBtn.alphaFactor = alpha;
					gainBoltBtn.layout(ctx, stack);
					Nuklear.nk_group_end(ctx);
				}
				Nuklear.nk_style_pop_vec2(ctx);
				Features.inst.popBackgroundColor(ctx, alpha);

				// Dytt cancel knapp mot midten
				Nuklear.nk_layout_row_begin(ctx, Nuklear.NK_DYNAMIC, heightElements * 0.5f, 3);
				float x = 0.3f;
				Nuklear.nk_layout_row_push(ctx, x);
				nk_label(ctx, "", Nuklear.NK_TEXT_ALIGN_CENTERED);
				Nuklear.nk_layout_row_push(ctx, 1 - 2 * x);

				// cancel knapp
				cancelBtn.alphaFactor = alpha;
				cancelBtn.layout(ctx, stack);
				Nuklear.nk_layout_row_end(ctx);

			}
			nk_end(ctx);
			Features.inst.popBackgroundColor(ctx);
			Features.inst.popFontColor(ctx);
		}
	}

	public void pushUpgrade(Player player, Upgrade upgrade) {
		if (upgrade != null) {

			if (upgrades.isEmpty())
				initialUpgrade = upgrade;
			upgrades.add(upgrade);

			if (okAction != null) {
				bonusBoltBtns.clear();
				String[] bonusTexts = upgrade.getBonusTexts(player.upgrades)[upgrade.getBonusLVL()];
				for (int i = 0; i < bonusTexts.length; i++) {
					int cost = calcBonusCost(upgrade, i);
					var btn = new UIButton<Integer>(
							"    $" + cost + ":    " + (cost == 1 && bonusTexts.length > 1 ? " " : "") + bonusTexts[i],
							UIColors.valByInt(i, UIColors.BONUSGOLD0));
					btn.setConsumerValue(i);
					btn.setPressedAction(okAction);
					btn.setTitleAlignment(Nuklear.NK_TEXT_ALIGN_MIDDLE | Nuklear.NK_TEXT_ALIGN_LEFT);
					bonusBoltBtns.add(btn);
				}

				infos[infos.length - 1].setText("    You have currently $"
						+ (player.bank.getLong(Bank.MONEY) - upgrade.getCost(1)) + " available");

				int gainBoltAmount = gainAmount(upgrade);
				gainBoltBtn.setTitle("+$" + gainBoltAmount);
				titleLabel.setText("\"" + Texts.getUpgradeTitle(upgrade) + "\" Bonus LVL "
						+ upgrade.getBonusLVLs()[upgrade.getBonusLVL()] + ":#LBEIGE");
			}
		}
	}

	private int gainAmount(Upgrade upgrade) {
		return upgrade.bonusGainOverride != -1 ? upgrade.bonusGainOverride : 100;
	}

	public static int calcBonusCost(Upgrade upgrade, int i) {
		return (i + 1) * (upgrade.bonusCostOverride == 0 ? 50 : upgrade.bonusCostOverride);
	}

	public boolean isVisible() {
		return window.visible;
	}

	public void setVisible(boolean visible) {
		window.visible = visible;
		if (visible) {
			UISceneInfo.decideFocusedWindow(0, 0);
			alpha = 0;
			press();
		}
	}

	public void input(int keycode, int action) {
		if (action != GLFW.GLFW_RELEASE) {
			if (keycode == GLFW.GLFW_KEY_1) {
				if (bonusBoltBtns.size() >= 1) {
					bonusBoltBtns.get(0).runPressedAction();
				}
			} else if (keycode == GLFW.GLFW_KEY_2) {
				if (bonusBoltBtns.size() >= 2) {
					bonusBoltBtns.get(1).runPressedAction();
				}
			} else if (keycode == GLFW.GLFW_KEY_3) {
				if (bonusBoltBtns.size() >= 3) {
					bonusBoltBtns.get(2).runPressedAction();
				}
			} else if (keycode == GLFW.GLFW_KEY_4) {
				if (bonusBoltBtns.size() >= 4) {
					bonusBoltBtns.get(3).runPressedAction();
				}
			} else if (keycode == GLFW.GLFW_KEY_5) {
				gainBoltBtn.runPressedAction();
			} else if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				cancelBtn.hover();
			}
//			else if (keycode == GLFW.GLFW_KEY_ENTER) {
//				UISceneInfo.getHoveredButton(Scenes.GENERAL_NONSCENE).runPressedAction();
//				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
//			}
		}
	}

	private void hover(UIButton btn) {
		UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
		btn.hover();
		btn.hoverFake();
	}

	private void tryHoverUp(int target) {
		if (target >= bonusBoltBtns.size())
			target = bonusBoltBtns.size() - 1;
		hover(bonusBoltBtns.get(target));
	}

	public void controllerInput() {
		
		var l = InputHandler.BTN_LEFT || InputHandler.LEFT_STICK_X < -0.33;
		var r = InputHandler.BTN_RIGHT || InputHandler.LEFT_STICK_X > 0.33;
		var u = InputHandler.BTN_UP || InputHandler.LEFT_STICK_Y < -0.33;
		var d = InputHandler.BTN_DOWN || InputHandler.LEFT_STICK_Y > 0.33;
		
		if (didInput) {
			if (!l && !r && !u && !d)
				didInput = false;
			return;
		}
		
		if (InputHandler.BTN_B) {
			cancelBtn.runPressedAction();
			return;
		}
		if (InputHandler.BTN_A) {
			if (cancelBtn.isHovered()) {
				cancelBtn.runPressedAction();
				return;
			}
			if (gainBoltBtn.isHovered()) {
				gainBoltBtn.runPressedAction();
				return;
			}
			for (var btn : bonusBoltBtns) {
				if (btn.isHovered()) {
					btn.runPressedAction();
					return;
				}
			}
			var btn = UISceneInfo.getHoveredButton(Scenes.GENERAL_NONSCENE);
			if (btn != null) {
				btn.runPressedAction();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
			}
			return;
		}

		if (u) {
			didInput = true;
			if (cancelBtn.isHovered()) {
				cancelBtn.unhover();
				tryHoverUp(3);
				return;
			}
			for (int i = 1; i < bonusBoltBtns.size(); i++) {
				if (bonusBoltBtns.get(i).isHovered()) {
					bonusBoltBtns.get(i).unhover();
					tryHoverUp(i - 1);
					return;
				}
			}
			tryHoverUp(0);
			return;
		}
		if (d) {
			didInput = true;
			if (gainBoltBtn.isHovered()) {
				gainBoltBtn.unhover();
				hover(cancelBtn);
				return;
			}
			for (int i = 0; i < bonusBoltBtns.size(); i++) {
				if (bonusBoltBtns.get(i).isHovered()) {
					bonusBoltBtns.get(i).unhover();
					if (bonusBoltBtns.size() - 1 == i)
						hover(cancelBtn);
					else
						tryHoverUp(i + 1);
					return;
				}
			}
			
			if (!cancelBtn.isHovered())
				tryHoverUp(0);
			return;
		}

		if (l) {
			didInput = true;
			if (gainBoltBtn.isHovered() || cancelBtn.isHovered()) {
				gainBoltBtn.unhover();
				cancelBtn.unhover();
				tryHoverUp(0);
				return;
			}
			for (int i = 1; i < bonusBoltBtns.size(); i++) {
				if (bonusBoltBtns.get(i).isHovered()) {
					return;
				}
			}
			tryHoverUp(0);
			return;
		}

		if (r) {
			didInput = true;
			if (!gainBoltBtn.isHovered()) {
				for (var btn : bonusBoltBtns)
					btn.unhover();
				cancelBtn.unhover();
				hover(gainBoltBtn);
			}
			return;
		}
	}

	public void combine(Player player) {
		if (player != null && combination != null)
			Translator.setCloneString(player, combination);
		else
			System.out.println("ERROR rep or combination is null");
		setCombination(null);
	}

	public void setCombination(Player player) {
		if (player == null) {
			this.combination = null;
			chosenBonuses.clear();
		} else { // if (this.combination == null) {
			this.combination = player.getClone();
			this.revertState = Player.getCleanedHistory(player.peekHistory()).cloneString();
//			System.out.println("revertState: " + revertState);
		}
	}

	public void cancel(Player player) {
		if (revertState != null) {
			if (player != null)
				Translator.setCloneString(player, revertState);
			revertState = null;
		}
		combination = null;
		upgrades.clear();
	}

	public void pushBonusChoice(int nameID, int bonusLVL, int gold) {
		chosenBonuses.push(new ChosenBonus(nameID, bonusLVL, gold));
	}

	@Override
	public void press() {
		for (var btn : bonusBoltBtns) {
			btn.press();
		}
		gainBoltBtn.press();
		cancelBtn.press();
	}

	@Override
	public void release() {
		for (var btn : bonusBoltBtns) {
			btn.release();
		}
		gainBoltBtn.release();
		cancelBtn.release();
	}

	public void hide() {
		if (window != null)
			window.visible = false;
	}

}
