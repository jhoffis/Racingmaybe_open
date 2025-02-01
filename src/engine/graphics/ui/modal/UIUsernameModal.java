package engine.graphics.ui.modal;

import static communication.GameType.JOINING_LAN;
import static org.lwjgl.nuklear.Nuklear.NK_TEXT_ALIGN_LEFT;
import static org.lwjgl.nuklear.Nuklear.nk_end;
import static org.lwjgl.nuklear.Nuklear.nk_group_begin;
import static org.lwjgl.nuklear.Nuklear.nk_group_end;
import static org.lwjgl.nuklear.Nuklear.nk_label;
import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;
import static org.lwjgl.nuklear.Nuklear.nk_style_pop_vec2;
import static org.lwjgl.nuklear.Nuklear.nk_style_push_vec2;

import comNew.LocalRemote2;
import comNew.Remote2;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.system.MemoryStack;

import audio.AudioRemote;
import audio.SfxTypes;
import communication.GameType;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.IUIPressable;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.UITextField;
import engine.graphics.ui.UIWindowInfo;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.Texts;
import player_local.Player;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.game.Lobby;
import settings_and_logging.RSet;

public class UIUsernameModal implements IUIPressable, IUIObject {

	private AudioRemote audio;
	private static UIWindowInfo window;
	private String modalTitle;
	private UILabel usernameLabel, titleLabel, privatePublicLabel;
	private UITextField usernameField, titleField, ipField;
	private static final String ipText = "Type IP-address here";
	private float fieldHeight;
	private final UIButton<?> okBtn, cancelBtn, privatePublicBtn, amountPlayersBtn, spectatorBtn;
	private GameType typeLobby;
	
	private boolean publicLobby, spectator;
	private int amountPlayers;
	private final int amountPlayersMax = 8, usernameLength = 128;

	private void updateSpec(boolean spectator) {
		this.spectator = spectator;
		spectatorBtn.setTitle("You're a " + (spectator ? Texts.spectator : Texts.player));
	}
	
	public UIUsernameModal(AudioRemote audio) {
		this.audio = audio;
		usernameLabel = new UILabel("Username:");
		titleLabel = new UILabel("Lobbyname:");
		privatePublicLabel = new UILabel("");
		
		float x = (float) Window.WIDTH / 4.65f, y = (float) Window.HEIGHT / 3.16f, w = (float) Window.WIDTH / 1.546f; 
		fieldHeight = Window.HEIGHT / 22f;
		
		usernameField = new UITextField("", false, false, usernameLength, Scenes.GENERAL_NONSCENE,
				x, 
				y,
				w, 
				fieldHeight);
		usernameField.getWindow().z = 2;
		
		titleField = new UITextField("", false, false, 32, Scenes.GENERAL_NONSCENE,
				x, 
				y + (fieldHeight * 1.1f),
				w, 
				fieldHeight);
		titleField.getWindow().z = 2;

		ipField = new UITextField(ipText, true, false, 32, Scenes.GENERAL_NONSCENE,
				x, 
				y + (fieldHeight * 1.1f),
				w, 
				fieldHeight);
		ipField.getWindow().z = 2;
		

		// Buttons
		okBtn = new UIButton<>(Texts.exitOKText, UIColors.GRAY);
		cancelBtn = new UIButton<>(Texts.exitCancelText, UIColors.GRAY);
		privatePublicBtn = new UIButton<>(Texts.privateText, UIColors.GRAY);
		amountPlayersBtn = new UIButton<>("", UIColors.GRAY);
		spectatorBtn = new UIButton<>("", UIColors.GRAY);

		privatePublicBtn.setPressedAction(() -> setPrivatePublic(!publicLobby));
		amountPlayersBtn.setPressedAction(() -> setAmountPlayers((amountPlayers) % amountPlayersMax + 1));
		amountPlayersBtn.setPressedActionRight(() -> {
			if (amountPlayers == 1)
				amountPlayers = amountPlayersMax+1;
			setAmountPlayers(amountPlayers - 1);
		});
		spectatorBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			updateSpec(!spectator);
		});

		window = UISceneInfo.createWindowInfo(Scenes.GENERAL_NONSCENE,
				0, 
				0, 
				Window.WIDTH, 
				Window.HEIGHT);
		window.visible = false;
		window.z = 2;

		UISceneInfo.addPressableToScene(Scenes.GENERAL_NONSCENE, this);
		UISceneInfo.setChangeHoveredButtonAction(Scenes.GENERAL_NONSCENE, okBtn);
		UISceneInfo.setChangeHoveredButtonAction(Scenes.GENERAL_NONSCENE, cancelBtn);
		UISceneInfo.setChangeHoveredButtonAction(Scenes.GENERAL_NONSCENE, privatePublicBtn);
		UISceneInfo.setChangeHoveredButtonAction(Scenes.GENERAL_NONSCENE, spectatorBtn);
	}
	
	public void setButtonActions(Lobby lobby) {
		okBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			String name = getInputText();

			if (name == null || name.isBlank()) {
				setLabel("Don't leave your name empty! Try again below:");
				return;
			}

			if (name.length() > usernameLength) {
				setLabel("Your name is too long! Max " + usernameLength + " chars! Try again below:");
				return;
			}

			RSet.set(RSet.username, getInputText());

			setLabel("Joining... consider canceling if 15 seconds goes by");

			switch (typeLobby) {
				case JOINING_ONLINE -> {
					var selectedLobby = Features.inst.getSelectedLobby();
					if (selectedLobby != null) {
						if (selectedLobby.isLan()) {
							joinViaLan(lobby, selectedLobby.ip, name, JOINING_LAN);
						} else {
							Features.inst.joinNewLobby(name, spectator ? Player.COMMENTATOR : Player.PLAYER);
						}
					}
				}
				case CREATING_ONLINE -> {
					Features.inst.createNewLobby(name, spectator ? Player.COMMENTATOR : Player.HOST, titleField.getText(), publicLobby, amountPlayers);
				}
				case JOINING_LAN -> {
					String ip = ipField.getText();
					if (ip.equals(ipText)) {
						ip = "localhost";
					}
					joinViaLan(lobby, ip, name, typeLobby);
				}
				case CREATING_LAN -> {
					lobby.createNewLobby(name, spectator ? Player.COMMENTATOR : Player.HOST, typeLobby, 0);
					if (lobby.getCom() == null) {
						SceneHandler.showMessage("Could not open the lobby");
					} else {
						((LocalRemote2) lobby.getCom().getRemote().way).setTitle(titleField.getText());
					}
					hide();
				}
				default -> throw new IllegalArgumentException("Unexpected value: " + typeLobby);
			}

		});
		cancelBtn.setPressedAction(() -> {
			audio.play(SfxTypes.REGULAR_PRESS);
			hide();
			Features.inst.leave();
		});
	}

	private void joinViaLan(Lobby lobby, String ip, String name, GameType typeLobby) {
		RSet.set(RSet.ip, ip);
		lobby.joinNewLobby(name, spectator ? Player.COMMENTATOR : Player.PLAYER, 0, typeLobby, ip, 0);
		hide();
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {
		Features.inst.pushBackgroundColor(ctx, UIColors.BLACK_TRANSPARENT);
		if (window.begin(ctx)) {
			// Set own custom styling
			NkVec2 spacing = NkVec2.malloc(stack);
			NkVec2 padding = NkVec2.malloc(stack);

			float sp = Window.WIDTH / 30f;
			spacing.set(sp, 0);
			padding.set(sp * 2f, sp);
			

			nk_style_push_vec2(ctx, ctx.style().window().spacing(),
					spacing);
			nk_style_push_vec2(ctx, ctx.style().window().group_padding(),
					padding);

			int height = Window.HEIGHT * 2 / 5;
			int heightElements = height / 4;

			// Move group down a bit
			nk_layout_row_dynamic(ctx, height / 2, 1);

			// Height of group
			nk_layout_row_dynamic(ctx, height, 1);

			Features.inst.pushBackgroundColor(ctx, UIColors.BLACK);
			Features.inst.pushFontColor(ctx, UIColors.WHITE);

			if (nk_group_begin(ctx, "ExitGroup", UIWindowInfo.OPTIONS_STANDARD)) {
				if (typeLobby.isCreating()) {
					nk_layout_row_dynamic(ctx, heightElements / 2, 1);
					nk_label(ctx, modalTitle, NK_TEXT_ALIGN_LEFT);
	
					nk_layout_row_dynamic(ctx, fieldHeight, 2);
					usernameLabel.layout(ctx, stack);
	
					nk_layout_row_dynamic(ctx, fieldHeight, 2);
					titleLabel.layout(ctx, stack);
	
					nk_layout_row_dynamic(ctx, heightElements / 10, 1);	
					nk_layout_row_dynamic(ctx, heightElements / 2, 4);
					if (typeLobby.isSteam()) {
						privatePublicBtn.layout(ctx, stack);
						privatePublicLabel.layout(ctx, stack);
						amountPlayersBtn.layout(ctx, stack);
						spectatorBtn.layout(ctx, stack);
					} else {
						spectatorBtn.layout(ctx, stack);
					}
	
					nk_layout_row_dynamic(ctx, heightElements / 10, 1);	
				} else {
					nk_layout_row_dynamic(ctx, heightElements, 1);
					nk_label(ctx, modalTitle, NK_TEXT_ALIGN_LEFT);

					nk_layout_row_dynamic(ctx, heightElements, 1);
					if (typeLobby.isSteam()) {
						usernameField.layoutTextfieldItself(ctx, stack);
					}
				} 
				
				nk_layout_row_dynamic(ctx, heightElements, 2);
				okBtn.layout(ctx, stack);
				cancelBtn.layout(ctx, stack);

				// Unlike the window, the _end() function must be inside
				// the if() block
				nk_group_end(ctx);
			}

			Features.inst.popFontColor(ctx);
			Features.inst.popBackgroundColor(ctx);

			// Reset styling
			nk_style_pop_vec2(ctx);
			nk_style_pop_vec2(ctx);

		} else {
			nk_end(ctx);
			Features.inst.popBackgroundColor(ctx); // not visible
			return;
		}
		nk_end(ctx);
		Features.inst.popBackgroundColor(ctx);
		
		if (!(typeLobby.isCreating() || !typeLobby.isSteam())) return;
		usernameField.getWindow().focus = true;
		usernameField.layout(ctx, stack);

		if (typeLobby.isCreating()) {
			titleField.getWindow().focus = true;
			titleField.layout(ctx, stack);
		} else {
			ipField.getWindow().focus = true;
			ipField.layout(ctx, stack);
		}
	}

	public void release() {
		okBtn.release();
		cancelBtn.release();
		privatePublicBtn.release();
		amountPlayersBtn.release();
		spectatorBtn.release();
	}

	public void press() {
		okBtn.press();
		cancelBtn.press();
		privatePublicBtn.press();
		amountPlayersBtn.press();
		spectatorBtn.press();
	}

	public boolean isVisible() {
		return window.visible;
	}
	
	public void hide() {
		window.visible = false;
		usernameField.getWindow().visible = false;
		titleField.getWindow().visible = false;
		ipField.getWindow().visible = false;
		UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
	}

	public void show(GameType typeLobby) {
		window.visible = true;
		usernameField.getWindow().visible = true;
		titleField.getWindow().visible = true;
		ipField.getWindow().visible = true;

		window.focus = true;
		this.typeLobby = typeLobby;
		updateSpec(false);

		switch (typeLobby) {
			case CREATING_LAN, CREATING_ONLINE -> {
				modalTitle = Texts.createOnlineText + ":";
				titleField.setPretext(Features.inst.getUsername() + "'s game");
				titleField.reset();
				setPrivatePublic(true);
				setAmountPlayers(2); // 2 players
			}
			case JOINING_LAN -> {
				modalTitle = Texts.usernameText;
				var str = RSet.get(RSet.ip);
				if (str == null || str.isBlank())
					str = ipText;
				ipField.setPretext(str);
				ipField.reset();
			}
			default -> {
				modalTitle = Texts.usernameText;
			}
		}

		usernameField.focus(true);
		press();
		okBtn.hover();
	}

	public UIButton<?> getCancelBtn() {
		return cancelBtn;
	}

	public void setLabel(String string) {
		this.modalTitle = string;
	}

	public String getInputText() {
		return usernameField.getText().trim();
	}

	public void input(int keycode, int action) {

		usernameField.input(keycode, action);
		titleField.input(keycode, action);
		ipField.input(keycode, action);
		
		switch (keycode) {
			case GLFW.GLFW_KEY_UP :
			case GLFW.GLFW_KEY_LEFT :
				okBtn.hover();
				break;
			case GLFW.GLFW_KEY_DOWN :
			case GLFW.GLFW_KEY_RIGHT :
				getCancelBtn().hover();
				break;
			case GLFW.GLFW_KEY_ENTER :
				var btn = UISceneInfo.getHoveredButton(Scenes.GENERAL_NONSCENE);
				if (btn == null)
					btn = okBtn;
				btn.runPressedAction();
				UISceneInfo.clearHoveredButton(Scenes.GENERAL_NONSCENE);
				break;
		}
	}

	public void input(String c) {
		usernameField.addText(c);
		titleField.addText(c);
		ipField.addText(c);
	}

	public void controllerInput() {
		if (InputHandler.BTN_A)
			okBtn.runPressedAction();
		else if (InputHandler.BTN_B)
			cancelBtn.runPressedAction();
		else if (!InputHandler.HOLDING) {
			if (InputHandler.BTN_X || InputHandler.BTN_UP)
				amountPlayersBtn.runPressedAction();
			else if (InputHandler.BTN_DOWN)
				amountPlayersBtn.runRightPressedAction();
			else if (InputHandler.BTN_Y)
				privatePublicBtn.runPressedAction();
		}
	}

	public void setStandardInputText(String username) {
		usernameField.setPretext(username);
		usernameField.setText(username);
	}

	public void mouseButtonInput(int button, int action, float x, float y) {
		if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && action != GLFW.GLFW_RELEASE) {
			if (!typeLobby.isCreating() && typeLobby.isSteam()) {
				usernameField.focus(false);
			} else {
				usernameField.tryFocus(x, y, false);
				titleField.tryFocus(x, y, false);
				ipField.tryFocus(x, y, false);
			}
		}
	}
	
	public void runCancelAction() {
		cancelBtn.runPressedAction();
	}
	
	private void setPrivatePublic(boolean publicLobby) {
		audio.play(SfxTypes.REGULAR_PRESS);
		this.publicLobby = publicLobby;
		if (publicLobby) {
			privatePublicBtn.setTitle(Texts.privateText);
			privatePublicLabel.setText("Will be public");
		} else {
			privatePublicBtn.setTitle(Texts.publicText);
			privatePublicLabel.setText("Will be private");
		}
	}
	
	private void setAmountPlayers(int amount) {
		audio.play(SfxTypes.REGULAR_PRESS);
		amountPlayers = amount;
		amountPlayersBtn.setTitle("Players: " + amount);
	}
	
	public void updateResolution() {
		fieldHeight = usernameField.getWindow().height * 1.1f;
	}

	public static boolean isOpen() {
		return window.visible;
	}

}
