package main;

import java.util.*;
import java.util.function.Consumer;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;
import com.codedisaster.steamworks.SteamMatchmaking.ChatEntryType;
import com.codedisaster.steamworks.SteamMatchmaking.ChatMemberStateChange;
import com.codedisaster.steamworks.SteamMatchmaking.ChatRoomEnterResponse;
import com.codedisaster.steamworks.SteamMatchmakingCallback;
import com.codedisaster.steamworks.SteamResult;

import adt.IAction;
import audio.AudioRemote;
import audio.SfxTypes;
import communication.GameType;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIButtonLobby;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UISceneInfo;
import engine.io.Window;
import main.steam.SteamHandler;
import player_local.Player;
import scenes.Scenes;
import scenes.game.Lobby;
import settings_and_logging.RSet;

public class Features implements SteamMatchmakingCallback, ISelectedLobby {

	// Std random for � unng� mye allokering
	public static Features inst;
	public final static Random ran = new Random();

	public final Stack<NkColor> backgroundColorCache = new Stack<>();
	private AudioRemote audio;

	private Window window;

	private SteamHandler steamHandler;
	private ArrayList<SteamID> lobbiesChecked = new ArrayList<SteamID>();
	private HashMap<SteamID, UIButtonLobby> lobbies = new HashMap<SteamID, UIButtonLobby>();
	private SteamID currentLobby = null;
	private String name;
	private byte role;
	private UIButtonLobby selectedLobby = null;
	private Lobby lobby;
	private SteamMatchmaking matchMaking;

	private IAction closeUsernameModalAction;

	private String lobbyName;

	private Consumer<UIButtonLobby> lobbyBtnAction;
	private Consumer<IUIObject[]> lobbiesListAction;

	public Features(AudioRemote audio, Window window) {
		if (inst == null) {
			inst = this;
		} else {
			System.err.println("Created more than one Features!");
			return;
		}
		
		this.audio = audio;
		this.window = window;
	}

	public static void printArr(byte[] array) {
		System.out.println();
		for (var v : array) {
			System.out.print(v + ", ");
		}
	}

	public void pushBackgroundColor(NkContext ctx, UIColors colorType, float alphaFactor) {
	NkColor color = UIColors.COLORS[colorType.ordinal()];
		int alpha = color.a();
		if (alpha < 0)
			alpha += 256;
		alpha *= alphaFactor;
		byte a = (byte) (alpha);
		backgroundColorCache.push(color);
		ctx.style().window().fixed_background().data().color().set(color.r(),
				color.g(), color.b(), a);
	}
	
	public void pushBackgroundColor(NkContext ctx, UIColors colorType) {
		NkColor color = UIColors.COLORS[colorType.ordinal()];
		backgroundColorCache.push(color);
		ctx.style().window().fixed_background().data().color().set(color);
	}

	public void popBackgroundColor(NkContext ctx, float alphaFactor) {
		backgroundColorCache.pop();
		if (backgroundColorCache.empty()) {
			ctx.style().window().fixed_background().data().color().set((byte) 0, (byte) 0, (byte) 0, (byte) 0);
			return;
		}
		NkColor color = backgroundColorCache.peek();
		int alpha = color.a();
		if (alpha < 0)
			alpha += 256;
		alpha *= alphaFactor;
		ctx.style().window().fixed_background().data().color().set(color.r(),
				color.g(), color.b(), (byte) alpha);
	}

	public void popBackgroundColor(NkContext ctx) {
		backgroundColorCache.pop();
		if (backgroundColorCache.empty()) {
			ctx.style().window().fixed_background().data().color().set((byte) 0, (byte) 0, (byte) 0, (byte) 0);
			return;
		}
		ctx.style().window().fixed_background().data().color().set(backgroundColorCache.peek());
	}
	
	public void pushFontColor(NkContext ctx, UIColors colorType) {
		NkColor color = UIColors.COLORS[colorType.ordinal()];
		Nuklear.nk_style_push_color(ctx, ctx.style().text().color(), color);
		Nuklear.nk_style_push_color(ctx, ctx.style().button().text_normal(), color);
		Nuklear.nk_style_push_color(ctx, ctx.style().button().text_active(), color);
		Nuklear.nk_style_push_color(ctx, ctx.style().button().text_hover(), color);
	}

	public void popFontColor(NkContext ctx) {
		Nuklear.nk_style_pop_color(ctx);
		Nuklear.nk_style_pop_color(ctx);
		Nuklear.nk_style_pop_color(ctx);
		Nuklear.nk_style_pop_color(ctx);
	}

	public Consumer<UIButtonLobby> createSelectableBtnAction(UIButton<?> joinBtnReference, ISelectedLobby selectedBtnContainer) {
		return (btn) -> {
			System.out.println("press lobby btn");
			audio.play(SfxTypes.REGULAR_PRESS);
			var selectedBtn = selectedBtnContainer.getSelectedLobby(); 
			if (selectedBtn != null && !selectedBtn.equals(btn)) {
				selectedBtn.setSelected(false);
			}
			
			selectedBtn = btn;
			int click = selectedBtn.click();
			selectedBtnContainer.setSelectedLobby(btn);
			
			if (click == 2) {
				joinBtnReference.setEnabled(true);
				joinBtnReference.runPressedAction();
			} else {
				joinBtnReference.setEnabled(click != 0);
			}
		};
	}
	
	public void createLobbyBtnAction(UIButton<GameType> joinOnlineBtn) {
		lobbyBtnAction = createSelectableBtnAction(joinOnlineBtn, this);
	}
	
	private void updateLobbyMatchList() {
		if (lobbies.isEmpty()) {
			lobbiesListAction.accept(new IUIObject[] {new UILabel(Texts.noLobbies, Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE)});
		} else {
			lobbiesListAction.accept(lobbies.values().toArray(new IUIObject[0]));
		}
	}

	public void requestLobbyList(Consumer<IUIObject[]> lobbiesListAction) {
		this.lobbiesListAction = lobbiesListAction;
		new SteamMatchmaking(this).requestLobbyList();
	}

	@Override
	public void onLobbyMatchList(int lobbiesMatching) {
		lobbies.clear();
		lobbiesChecked.clear();
		
//		if (Game.DEBUG) {
//			UIButtonLobby testBtn = new UIButtonLobby(this, "");
//			testBtn.setConsumerValue(testBtn);
//			testBtn.setPressedAction(lobbyBtnAction);
//			lobbies.put(new SteamID(), testBtn);
//			UISceneInfo.addPressableToScene(Scenes.MULTIPLAYER, testBtn);
//		}
		
		for (int i = 0; i < lobbiesMatching; i++) {

			UIButtonLobby btn = new UIButtonLobby("");
			SteamID lobbyID = new SteamMatchmaking(this).getLobbyByIndex(i);
			lobbies.put(lobbyID, btn);

			btn.setTitleAlignment(Nuklear.NK_TEXT_ALIGN_LEFT);
			btn.setLobby(lobbyID);
			btn.setConsumerValue(btn);
			btn.setPressedAction(lobbyBtnAction);
			UISceneInfo.addPressableToScene(Scenes.MULTIPLAYER, btn);

			boolean ack = new SteamMatchmaking(this).requestLobbyData(lobbyID);
			System.out.println(ack);
		}

		updateLobbyMatchList();
	}

	@Override
	public void onLobbyDataUpdate(SteamID steamIDLobby, SteamID steamIDMember, boolean success) {
		if (!lobbiesChecked.contains(steamIDLobby)) {

			UIButtonLobby lobbyBtn = null;
			lobbiesChecked.add(steamIDLobby);
			
			if (lobbies.containsKey(steamIDLobby)) {
				lobbyBtn = lobbies.get(steamIDLobby);
			} else {
				return;
			}
			String title = matchMaking.getLobbyData(steamIDLobby, "name");
			boolean started = Integer.parseInt(matchMaking.getLobbyData(steamIDLobby, "started")) != 0;
			boolean ended = Integer.parseInt(matchMaking.getLobbyData(steamIDLobby, "ended")) != 0;
			if (title.equals("")) {
				lobbiesChecked.remove(steamIDLobby);
				lobbies.remove(steamIDLobby);
				updateLobbyMatchList();
				return;
			}
			String[] versionCheck = title.split(", ");

			if (!ended && versionCheck.length > 0 && versionCheck[versionCheck.length - 1].equals(Main.VERSION)) {
				lobbyBtn.setEnabled(true);
				lobbyBtn.setTitle((started ? "STARTED " : "") + title + "  -  " + matchMaking.getNumLobbyMembers(steamIDLobby) + "/"
						+ matchMaking.getLobbyMemberLimit(steamIDLobby));
			} else {
				lobbiesChecked.remove(steamIDLobby);
				lobbies.remove(steamIDLobby);
				updateLobbyMatchList();
			}
		}
	}

	@Override
	public void onLobbyKicked(SteamID steamIDLobby, SteamID steamIDAdmin,
			boolean kickedDueToDisconnect) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby,
			long gameID) {
		// TODO Auto-generated method stub
		System.out.println("onLobbyInvite");

	}

	@Override
	public void onLobbyGameCreated(SteamID steamIDLobby,
			SteamID steamIDGameServer, int ip, short port) {
		// TODO Auto-generated method stub
		System.out.println("onLobbyGameCreated");
	}

	@Override
	public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions,
			boolean blocked, ChatRoomEnterResponse response) {
		currentLobby = steamIDLobby;
		lobby.joinNewLobby(name, role, getLobbyOwner(), GameType.JOINING_ONLINE, null, 0);
		closeUsernameModalAction.run();
	}
	
	public long getLobbyOwner() {
		if(currentLobby != null) {
			return SteamID.getNativeHandle(matchMaking.getLobbyOwner(currentLobby));
		}
		return 0;
	}

	@Override
	public void onLobbyCreated(SteamResult result, SteamID steamIDLobby) {
		if (result == SteamResult.OK) {
			System.out.println("Created lobby with id: " + SteamID.getNativeHandle(steamIDLobby));
			matchMaking.setLobbyData(steamIDLobby, "name", lobbyName);
			matchMaking.setLobbyData(steamIDLobby, "started", "0");
			matchMaking.setLobbyData(steamIDLobby, "ended", "0");
			System.out.println(lobbyName);

			// System.out.println("name: " + new SteamFriends(new
			// SteamFriendsCallback).getFriendPersonaName(steamIDLobby));
			currentLobby = steamIDLobby;
			lobby.createNewLobby(name, role, GameType.CREATING_ONLINE, 0);
			closeUsernameModalAction.run();
		}
	}

	@Override
	public void onLobbyChatUpdate(SteamID steamIDLobby,
			SteamID steamIDUserChanged, SteamID steamIDMakingChange,
			ChatMemberStateChange stateChange) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLobbyChatMessage(SteamID steamIDLobby, SteamID steamIDUser,
			ChatEntryType entryType, int chatID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFavoritesListChanged(int ip, int queryPort, int connPort,
			int appID, int flags, boolean add, int accountID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onFavoritesListAccountsUpdated(SteamResult result) {
		// TODO Auto-generated method stub

	}

	public UIButtonLobby[] getLobbies() {
		UIButtonLobby[] list = new UIButtonLobby[lobbies.size()];
		return lobbies.values().toArray(list);
	}

	public void leave() {
		System.out.println("leave lobby");
		if (currentLobby != null)
			matchMaking.leaveLobby(currentLobby);
		currentLobby = null;
	}

	public void clearLobbies() {
		lobbies.clear();
		selectedLobby = null;
	}

	public void setSelectedLobby(SteamID steamIDLobby) {
		selectedLobby = new UIButtonLobby("fake");
		selectedLobby.setLobby(steamIDLobby);
	}
	
	@Override
	public void setSelectedLobby(UIButtonLobby btn) {
		selectedLobby = btn;
	}

	@Override
	public UIButtonLobby getSelectedLobby() {
		return selectedLobby;
	}

	public void joinNewLobby(String name, byte role) {
		if (selectedLobby.getLobby() != null) {
			boolean ended = Integer.parseInt(matchMaking.getLobbyData(selectedLobby.getLobby(), "ended")) != 0;
			if (ended) return;
			this.name = name;
			this.role = role;
			selectedLobby.joinThisLobby();
		}
	}

	public void createNewLobby(String name, byte role, String lobbyName, boolean publicLobby, int amount) {
		this.name = name;
		this.role = role;
		this.lobbyName = lobbyName + ", " + Main.VERSION;
		matchMaking.createLobby(publicLobby ? SteamMatchmaking.LobbyType.Public : SteamMatchmaking.LobbyType.FriendsOnly, amount);
	}

	public void startLobby() {
		if (currentLobby != null)
			matchMaking.setLobbyData(currentLobby, "started", "1");
	}

	public void endLobby() {
		if (currentLobby != null)
			matchMaking.setLobbyData(currentLobby, "ended", "1");
	}

	public String getUsername() {
		String username = null;
		if (steamHandler != null) {
			username = RSet.settings.get(RSet.username.ordinal());
			if (username == null) {
				username = steamHandler.getUsername(steamHandler.getMySteamID());
			}
		} else {
			username = RSet.settings.get(RSet.username.ordinal());
			if (username == null) {
				username = "Player";
			}
		}
		
		// TODO allow for anything else but ascii
		return username; //.replaceAll("[^\\p{ASCII}]", "?");
	}

	public Window getWindow() {
		return window;
	}
	
	public void setSteamHandler(SteamHandler steamHandler) {
		this.steamHandler = steamHandler;
		if (steamHandler != null)
			matchMaking = new SteamMatchmaking(this);
	}

	public SteamHandler getSteamHandler() {
		return steamHandler;
	}

	public void setLobby(Lobby lobby) {
		this.lobby = lobby;
	}

	public void setCloseUsernameModalAction(IAction action) {
		this.closeUsernameModalAction = action;
	}

//	public void setAllowedChallenges(GameType i) {
//		if (i > allowedChallenges)
//			allowedChallenges = i;
//	}
//	
//	public int getAllowedChallenges() {
//		return allowedChallenges;
//	}

	public long getMyDestId(GameType multiplayerType) {
		if (!multiplayerType.isSinglePlayer() && Main.STEAM && multiplayerType.isSteam())
			return SteamID.getNativeHandle(steamHandler.getMySteamID());
		if (RSet.settings != null) {
			var destId = RSet.settings.getLong(RSet.discID.ordinal());
			if (destId >= 128)
				return destId;
		}
		return generateLanId(multiplayerType.isCreating());
	}
	
	public static long generateLanId(boolean creating) {
		if (creating)
			return 0;
		var id = ran.nextLong();
		if (id < 0)
			id = -id;
		if (id < 128)
			id += 128;
		return id;
	}

	public static void fillNullListSize(List list, int toIndex) {
		while(toIndex >= list.size())
			list.add(null);
	}

	public AudioRemote getAudio() {
		return audio;
	}

	public void setLobbyOwner(Player replacement) {
		matchMaking.setLobbyOwner(currentLobby, SteamID.createFromNativeHandle(replacement.steamID));
	}


}
