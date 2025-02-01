package engine.graphics.ui;

import java.io.UnsupportedEncodingException;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLeaderboardEntry;

import game_modes.SingleplayerChallenges;
import game_modes.SingleplayerChallengesMode;
import main.Features;
import main.Texts;

public class UILeaderboardPlayer implements IUIObject {

	private final SteamID steamID;
	private int carID = -1;
	public final UIRow row;

	public UILeaderboardPlayer(SteamLeaderboardEntry entry, int challengeLvl) {
		this.steamID = entry.getSteamIDUser();
		row = new UIRow(new IUIObject[] {
				new UILabel(), 
				new UILabel(Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE),
				new UILabel(Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE),
				new UILabel(Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE),
				new UILabel(Nuklear.NK_TEXT_ALIGN_RIGHT | Nuklear.NK_TEXT_ALIGN_MIDDLE)
		}, 0);
		setBaseTitle(entry, challengeLvl);
	}

	private void setBaseTitle(SteamLeaderboardEntry entry, int challengeLvl) {
		((UILabel) row.row[0]).setText(entry.getGlobalRank() + ". " + hexToAscii(Features.inst.getSteamHandler().getUsername(steamID))
				+ ":    ");
//		System.out.println("challengeLVL " + challengeLvl);
		var score = entry.getScore();
		((UILabel) row.row[1]).setText(Texts.formatNumberSimple(score));
		if (score >= 10_000_000) {
			var round = Math.round((double) score / 10_000_000d);
			var beatTime = switch (SingleplayerChallenges.values()[challengeLvl]) {
				case Beginner: yield SingleplayerChallengesMode.beginnerTime;
				case Casual: yield SingleplayerChallengesMode.casualTime;
				case Intermediate: yield SingleplayerChallengesMode.intermediateTime;
				case Hard: yield SingleplayerChallengesMode.hardTime;
				case Harder: yield SingleplayerChallengesMode.hardTime;
				case Master: yield SingleplayerChallengesMode.masterTime;
				case Samurai: yield SingleplayerChallengesMode.samuraiTime;
				case Expert: yield SingleplayerChallengesMode.expertTime;
				case Accomplished: yield SingleplayerChallengesMode.accomplishedTime;
				case Sensei: yield SingleplayerChallengesMode.accomplishedTime;
				case Legendary: yield SingleplayerChallengesMode.legendaryTime;
				case Nightmarish: yield SingleplayerChallengesMode.nightmarishTime;
				case Unfair: yield SingleplayerChallengesMode.unfairTime;
				case Unfaircore: yield SingleplayerChallengesMode.megaunfairTime;
				case TheBoss: yield SingleplayerChallengesMode.thebossTime;
			default:
				throw new IllegalArgumentException("Unexpected value: " + challengeLvl);
			};
			var time = beatTime - (score - (round*10_000_000d)) / 1000d;
			if (time > 0 && time <= beatTime) {
				((UILabel) row.row[2]).setText(String.valueOf(round)  );
				((UILabel) row.row[3]).setText((int) time + " ms");
				setTitle();
				return;
			}
		}

		((UILabel) row.row[2]).setText("");
		((UILabel) row.row[3]).setText("");
		setTitle();
	}

	private String hexToAscii(String hexStr) {
		try {
			return new String(hexStr.getBytes("US-ASCII"));
		} catch (UnsupportedEncodingException e) {
			System.out.println("Skipping unsupported encoding: " + hexStr);
			return "";
		}
	}

	private void setTitle() {
		if (carID != -1) {
			int n = 0;
			for (var elem : row.row) {
				if (elem instanceof UILabel label) {
					n++;
					var ogColor = label.getColor();
					if (n == row.row.length) {
						if (carID > Texts.CAR_TYPES.length || carID < 0) {
							var str = String.valueOf(carID);
							carID = str.charAt(str.length() - 1) - '0';
						}
						label.setText(Texts.CAR_TYPES[carID]);
					}
					if (ogColor != null)
						label.setColor(ogColor);
				}
			}
		}
	}

	public void setColor(UIColors color) {
		for (var elem : row.row) {
			if (elem instanceof UILabel label) {
				label.setColor(color);
			}
		}
	}

	public SteamID getSteamID() {
		return steamID;
	}

	public void setCarID(int id) {
		if (id >= 1000) {
			var str = String.valueOf(id);
			id = Integer.parseInt(str.substring(str.length() - 3));
		}
		this.carID = id;
		if (((UILabel) row.row[0]).getText() != null)
			setTitle();
	}

	@Override
	public void layout(NkContext ctx, MemoryStack stack) {
		row.layout(ctx, stack);
	}

}
