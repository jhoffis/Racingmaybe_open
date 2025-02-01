package scenes.regular;

import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import com.codedisaster.steamworks.SteamID;
import comNew.UDPClient;
import engine.graphics.ui.*;
import engine.graphics.ui.modal.UIUsernameModal;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import audio.SfxTypes;
import communication.GameType;
import engine.graphics.interactions.RegularTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Sprite;
import engine.graphics.Renderer;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Main;
import main.ResourceHandler;
import main.Texts;
import scenes.Scenes;
import scenes.adt.Scene;

public class MultiplayerScene extends Scene {

	private float refreshTimer;
	private UIButton<GameType> joinOnlineBtn, createOnlineBtn, joinLanBtn, createLanBtn;
	private UIButton<Void> gobackBtn, refreshBtn;

	private UIWindowInfo window;
	private UIScrollable lobbies;
	private int btnHeight;
	private Sprite backgroundImage;

	public MultiplayerScene(RegularTopbar topbar, Consumer<GameType> initMovingIntoALobby) {
		super(topbar, Scenes.MULTIPLAYER);
		updateResolution();
		float wFactor = 0.9f;
		float x = (1f - wFactor) / 2f * Window.WIDTH;
		float y = topbar.getHeight(); // + Window.HEIGHT * 2 / 3 - btnHeight / 0.5f;
		float w = Window.WIDTH * wFactor;
		float h = Window.HEIGHT * 0.58f - y;
		lobbies = new UIScrollable(sceneIndex, x, y, w, h);
		lobbies.getWindow().name = Texts.lobbiesText;
		lobbies.getWindow().options = Nuklear.NK_WINDOW_BORDER | Nuklear.NK_WINDOW_TITLE
				| Nuklear.NK_WINDOW_NO_SCROLLBAR;
		lobbies.rowHeightBased = 24;
		ResourceHandler.LoadSprite("./images/back/titlebackground.png", "main",
				(sprite) -> backgroundImage = sprite.setToFullscreen());
		y = lobbies.getWindow().getYHeight() + btnHeight / 2;
		window = createWindow(0, y, Window.WIDTH, Window.HEIGHT - y);

		joinOnlineBtn = new UIButton<>(Texts.joinOnlineText);
		joinOnlineBtn.setPressedAction(initMovingIntoALobby);
		joinOnlineBtn.setConsumerValue(GameType.JOINING_ONLINE);
		add(joinOnlineBtn);

		createOnlineBtn = new UIButton<>(Texts.createOnlineText);
		createOnlineBtn.setPressedAction(initMovingIntoALobby);
		createOnlineBtn.setConsumerValue(GameType.CREATING_ONLINE);
		createOnlineBtn.setEnabled(Main.STEAM);
		add(createOnlineBtn);

		joinLanBtn = new UIButton<>(Texts.joinLanText);
		joinLanBtn.setPressedAction(initMovingIntoALobby);
		joinLanBtn.setEnabled(true);
		joinLanBtn.setConsumerValue(GameType.JOINING_LAN);
		add(joinLanBtn);

		createLanBtn = new UIButton<>(Texts.createLanText);
		createLanBtn.setPressedAction(initMovingIntoALobby);
		createLanBtn.setConsumerValue(GameType.CREATING_LAN);
		add(createLanBtn);

		gobackBtn = new UIButton<>(Texts.gobackText);
		gobackBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.MAIN_MENU, true);
		});
		add(gobackBtn);

		refreshBtn = new UIButton<>(Texts.refreshText);
		refreshBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			updateGenerally(null);
		});
		add(refreshBtn);

		UINavigationAction toLobbies = () -> {
			var list = lobbies.getListArr();
			if (list.length > 0 && list[0] instanceof UIButtonLobby btn) {
				joinOnlineBtn.unhover();
				return btn;
			}
			return null;
		};
		
		joinOnlineBtn.setNavigations(null, () -> createOnlineBtn, toLobbies, () -> joinLanBtn);
		createOnlineBtn.setNavigations(() -> joinOnlineBtn, null, toLobbies, () -> createLanBtn);

		joinLanBtn.setNavigations(null, createLanBtn, joinOnlineBtn, gobackBtn);
		createLanBtn.setNavigations(joinLanBtn, null, createOnlineBtn, refreshBtn);
		gobackBtn.setNavigations(null, refreshBtn, joinLanBtn, null);
		refreshBtn.setNavigations(gobackBtn, null, createLanBtn, null);

		Features.inst.createLobbyBtnAction(joinOnlineBtn);
	}
	
	@Override
	public void updateGenerally(Camera cam, int... args) {
		
		for (var elem : lobbies.getListArr()) {
			if (elem instanceof IUIPressable p) {
				UISceneInfo.removePressableReference(sceneIndex, p);
			}
		}
		
		((RegularTopbar) topbar).setTitle(Texts.multiplayerText);
		lobbies.setText(Texts.searching, Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE);
		joinOnlineBtn.setEnabled(false);
		if (Main.STEAM) {
			Features.inst.requestLobbyList((lobbiesBtns) -> {
				lobbies.setText(lobbiesBtns);
				lobbies.addNavigationToScrollableList(sceneIndex, joinOnlineBtn, true, false);
			});
		} else if (Main.DEBUG) {
			var testList = new UIButtonLobby[4];
			for (int i = 0; i < testList.length; i++) {
				testList[i] = new UIButtonLobby("lobby nr. " + i);
			}
			lobbies.setText(testList);
			joinOnlineBtn.setEnabled(true);
			lobbies.addNavigationToScrollableList(sceneIndex, joinOnlineBtn, true, false);
		}
		refreshLanLobby();
	}

	private List<UIButtonLobby> getLanBtns() {
		var list = new ArrayList<UIButtonLobby>();
		for (var btn : lobbies.getList()) {
			if (btn instanceof UIButtonLobby buttonLobby && buttonLobby.isLan()) {
				list.add(buttonLobby);
			}
		}
		return list;
	}

	public void refreshLanLobby() {
		var oldLanBtns = getLanBtns();
		HashMap<UIButtonLobby, AtomicBoolean> founds = new HashMap<>(oldLanBtns.size());
		for (int i = 0; i < oldLanBtns.size(); i++) {
			founds.put(oldLanBtns.get(i), new AtomicBoolean(false));
		}
		UDPClient.sendEcho((ip, name) -> {
			Map.Entry<UIButtonLobby, AtomicBoolean> entry = null;
			for (var found : founds.entrySet()) {
				if (ip.equals(found.getKey().ip)) {
					entry = found;
					break;
				}
			}
			System.out.println("found ip: " + ip);
			var title = "LAN game: " + name;
			if (entry != null) {
				entry.getValue().set(true);
				entry.getKey().setTitle(title);
				entry.getKey().ip = ip;
			} else {
				// add lan button
				var list = lobbies.getListArr();
				if (list.length == 1 && list[0] instanceof UILabel) {
					lobbies.clear();
				}
				var lanBtn = new UIButtonLobby(title, ip);
				lanBtn.setConsumerValue(lanBtn);
				add(lanBtn);
				lanBtn.setPressedAction(Features.inst.createSelectableBtnAction(joinOnlineBtn, Features.inst));
				lobbies.addText(lanBtn);
			}
		});
		new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
			for (var found : founds.entrySet()) {
				if (!found.getValue().get()) {
					// remove lan button
					lobbies.remove(found.getKey());
				}
			}
        }).start();
	}

	@Override
	public void updateResolution() {
		btnHeight = Window.HEIGHT / 12;
	}

	@Override
	public void tick(float delta) {
		refreshTimer += delta;
		if (refreshTimer > 5f * Main.TICK_STD && !UIUsernameModal.isOpen()) {
			refreshTimer = 0;
			System.out.println("refresh");
			if (Main.STEAM) {

				Features.inst.requestLobbyList((lobbiesBtns) -> {
					SteamID selectedLobby = null;
					for (var iuiObject : lobbies.getList()) {
						if (iuiObject instanceof UIButtonLobby lobbyBtn && lobbyBtn.isSelected()) {
							selectedLobby = lobbyBtn.getLobby();
							break;
						}
					}
					if (selectedLobby != null) {
						for (var iuiObject : lobbiesBtns) {
							if (iuiObject instanceof UIButtonLobby lobbyBtn && lobbyBtn.getLobby().getAccountID() == selectedLobby.getAccountID()) {
								lobbyBtn.setSelected(true);
								break;
							}
						}
					}
					lobbies.setText(lobbiesBtns);
					lobbies.addNavigationToScrollableList(sceneIndex, joinOnlineBtn, true, false);
				});
			}
			refreshLanLobby();
		}

	}

	@Override
	public void keyInput(int keycode, int action) {
		if (action == GLFW.GLFW_PRESS) {
			// Downstroke for quicker input
			generalHoveredButtonNavigation(joinOnlineBtn, keycode);
			
			if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				sceneChange.change(Scenes.MAIN_MENU, false);
			}
		}
	}

	@Override
	public void controllerInput() {
		generalHoveredButtonNavigationJoy(joinOnlineBtn);
		if (InputHandler.BTN_B) {
			var hoveredBtn = UISceneInfo.getHoveredButton(sceneIndex);
			for (var elem : lobbies.getList()) {
				if (elem == hoveredBtn) {
					joinOnlineBtn.hover();
					return;
				}
			}
			gobackBtn.runPressedAction();
		} else if (InputHandler.BTN_UP || InputHandler.BTN_DOWN) {
			lobbies.showHovered();
		}
	}

	@Override
	public void mouseScrollInput(float x, float y) {
	}

	@Override
	public void mousePositionInput(float x, float y) {
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		renderer.renderOrthoMesh(backgroundImage);
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {

		// Set the padding of the group
//		ctx.style().window().spacing().set(0, btnHeight / 2);
		Features.inst.pushBackgroundColor(ctx, UIColors.BLACK_TRANSPARENT);
		lobbies.layout(ctx, stack);
		Features.inst.popBackgroundColor(ctx);

		/*
		 * MAIN SHIT
		 */
		NkVec2 spacing = NkVec2.malloc(stack);
		NkVec2 padding = NkVec2.malloc(stack);
		spacing.set(btnHeight / 2, btnHeight / 2);
		padding.set(btnHeight / 2, 0);
		Nuklear.nk_style_push_vec2(ctx, ctx.style().window().spacing(), spacing);
		Nuklear.nk_style_push_vec2(ctx, ctx.style().window().padding(), padding);

		if (window.begin(ctx)) {
			nk_layout_row_dynamic(ctx, btnHeight, 2);
			joinOnlineBtn.layout(ctx, stack);
			createOnlineBtn.layout(ctx, stack);

			nk_layout_row_dynamic(ctx, btnHeight, 2);
			joinLanBtn.layout(ctx, stack);
			createLanBtn.layout(ctx, stack);

			nk_layout_row_dynamic(ctx, btnHeight, 2);
			gobackBtn.layout(ctx, stack);
			refreshBtn.layout(ctx, stack);

		}
		Nuklear.nk_end(ctx);

		Nuklear.nk_style_pop_vec2(ctx);
		Nuklear.nk_style_pop_vec2(ctx);
	}

	@Override
	public void destroy() {
		removeGameObjects();
	}

}
