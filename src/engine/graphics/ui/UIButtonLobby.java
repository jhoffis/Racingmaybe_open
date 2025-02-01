package engine.graphics.ui;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;

import main.Features;

/**
 * Allows for selection and double clicking
 * 
 * @author Jens Benz
 *
 */
public class UIButtonLobby extends UIButton<UIButtonLobby> {

	private SteamID lobby;
	public String ip;
	private boolean selected;
	private long lastTimePressed;

	public UIButtonLobby(String title) {
		super(title);
	}

	public UIButtonLobby(String title, String ip) {
		super(title);
		this.ip = ip;
	}

	public void setSelected(boolean selected) {
		ColorBytes normal = null;
		ColorBytes active = null;
		ColorBytes hover = null;

		this.selected = selected;
		
		if (selected) {
			normal = new ColorBytes(0x44, 0x44, 0x44, 0xbb);
			active = new ColorBytes(0x33, 0x33, 0x33, 0xbb);
			hover = new ColorBytes(0x55, 0x55, 0x55, 0xbb);
		} else {
			normal = new ColorBytes(0x22, 0x22, 0x22, 0x55);
			active = new ColorBytes(0x11, 0x11, 0x11, 0xff);
			hover = new ColorBytes(0x55, 0x55, 0x55, 0xdd);
		}

		super.normalColor = normal.create();
		super.activeColor = active.create();
		super.hoverColor = hover.create();
	}

	/**
	 * 0 = unselect, 1 = select, 2 = doubleclick and run
	 */
	public int click() {
		long now = System.currentTimeMillis();
		System.out.println("Time: " + (now - lastTimePressed));
		if (now - lastTimePressed < 250) {
			setSelected(true);
			return 2;
		}
		lastTimePressed = now;
		
		setSelected(!selected);
		return selected ? 1 : 0;
	}
	
	public void joinThisLobby() {
		new SteamMatchmaking(Features.inst).joinLobby(lobby);
	}
	
	public SteamID getLobby() {
		return lobby;
	}

	public void setLobby(SteamID lobby) {
		this.lobby = lobby;
	}

	public boolean isSelected() {
		return selected;
	}
	public boolean isLan() {
		return ip != null;
	}
}
