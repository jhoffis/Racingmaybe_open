package main.steam;

import java.util.function.Consumer;

import com.codedisaster.steamworks.*;
import com.codedisaster.steamworks.SteamAuth.AuthSessionResponse;
import com.codedisaster.steamworks.SteamFriends.PersonaChange;

import communication.GameType;
import main.Features;
import settings_and_logging.RSet;

public class SteamHandler implements SteamUserCallback, SteamFriendsCallback{


	
	public SteamHandler() {

	}

	@Override
	public void onSetPersonaNameResponse(boolean success, boolean localSuccess, SteamResult result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPersonaStateChange(SteamID steamID, PersonaChange change) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGameOverlayActivated(boolean active) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {

	}

	@Override
	public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onGameServerChangeRequested(String server, String password) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onValidateAuthTicket(SteamID steamID, AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onEncryptedAppTicket(SteamResult result) {
		// TODO Auto-generated method stub
		
	}
	
	public SteamID getMySteamID() {
		return null;
	}
	
	public String getUsername(SteamID steamID) {
		return "";
	}

	public void setJoinActions(Consumer<GameType> initMovingIntoALobby) {

	}

	public void initUsername() {
		if (RSet.settings.get(RSet.username.ordinal()) == null) 
			RSet.set(RSet.username, getUsername(getMySteamID()));
	}
	
}
