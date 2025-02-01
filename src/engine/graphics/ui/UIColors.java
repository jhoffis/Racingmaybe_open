package engine.graphics.ui;

import engine.math.Vec3;
import org.lwjgl.nuklear.NkColor;

import java.util.Random;

public enum UIColors {
	WEAKGOLD, G, TUR, BUR, R, WON, WON2, AI, WHITE, DNF, NF, BLACK, PEARL, LBEIGE, DBEIGE, GRAY, LGRAY, 
	BONUSGOLD0, BONUSGOLD1, BONUSGOLD2, BONUSGOLD3, STRONGER_BONUSGOLD0, STRONGER_BONUSGOLD1, STRONGER_BONUSGOLD2, STRONGER_BONUSGOLD3, BLACK_TRANSPARENT, DARKGRAY,
	ORANGE, RAISIN_BLACK, DARK_RAISIN_BLACK, BLUSH, SKY_BLUE_CRAYOLA, CHAMPANGE, STAR_COMMAND_BLUE, AERO_BLUE, EGGSHELL, UNBLEACHED_SILK, GUNMETAL, JET, CHARCOAL, MOUNTBATTEN_PINK, OLD_LEVANDER,
	BLUE_GREEN_COLOR_WHEEL, CASTLETON_GREEN, TROPICAL_RAIN_FOREST, PAOLO_VERONESE_GREEN, POWER_RED, MEDIUM_SPRING_GREEN, VERY_BLACK_TRANSPARENT, SILVER, BRONZE, TUR2, GOLD, FAWN, RICH_BLACK, GREENER_WON, PICTON_BLUE;


	public static final NkColor[] COLORS = {
//		WEAKGOLD
			
			NkColor.create().r((byte) 133).g((byte) 117).b((byte) 78).a((byte) 255),
//		G
			NkColor.create().r((byte) 0).g((byte) 235).b((byte) 0).a((byte) 255),
//		TUR
			NkColor.create().r((byte) 0).g((byte) 235).b((byte) 223).a((byte) 255),
//		BUR
			NkColor.create().r((byte) 255).g((byte) 0).b((byte) 191).a((byte) 255),
//		R
			NkColor.create().r((byte) 235).g((byte) 0).b((byte) 0).a((byte) 255),
//		WON
			NkColor.create().r((byte) 14).g((byte) 170).b((byte) 128).a((byte) 255),
//		WON2
			NkColor.create().r((byte) 25).g((byte) 191).b((byte) 97).a((byte) 255),
//		AI
			NkColor.create().r((byte) 25).g((byte) 29).b((byte) 144).a((byte) 255),
//		WHITE			
			NkColor.create().r((byte) 255).g((byte) 255).b((byte) 255).a((byte) 255),
//		DNF
			NkColor.create().r((byte) 200).g((byte) 21).b((byte) 21).a((byte) 255),
//		NF
			NkColor.create().r((byte) 64).g((byte) 64).b((byte) 64).a((byte) 255),
//		BLACK
			NkColor.create().r((byte) 0).g((byte) 0).b((byte) 0).a((byte) 255),
//		PEARL
			NkColor.create().r((byte) 247).g((byte) 238).b((byte) 230).a((byte) 255),
//		LBEIGE
			NkColor.create().r((byte) 227).g((byte) 213).b((byte) 202).a((byte) 255),
//		DBEIGE
			NkColor.create().r((byte) 163).g((byte) 154).b((byte) 142).a((byte) 255),
//		GRAY
			NkColor.create().r((byte) 150).g((byte) 150).b((byte) 150).a((byte) 255),
//		LGRAY
			NkColor.create().r((byte) 210).g((byte) 210).b((byte) 210).a((byte) 255),
//		GOLD0
			NkColor.create().r((byte) 91).g((byte) 130).b((byte) 86).a((byte) 255),
//		GOLD1
			NkColor.create().r((byte) 130).g((byte) 130).b((byte) 130).a((byte) 255),
//		GOLD2
			NkColor.create().r((byte) 148).g((byte) 137).b((byte) 86).a((byte) 255),
//		GOLD3
			NkColor.create().r((byte) 0).g((byte) 148).b((byte) 124).a((byte) 255),
//		GOLD0 STRONGER
			NkColor.create().r((byte) 85).g((byte) 209).b((byte) 69).a((byte) 255),
//		GOLD1
			NkColor.create().r((byte) 130).g((byte) 130).b((byte) 130).a((byte) 255),
//		GOLD2
			NkColor.create().r((byte) 212).g((byte) 196).b((byte) 93).a((byte) 255),
//		GOLD3
			NkColor.create().r((byte) 0).g((byte) 209).b((byte) 175).a((byte) 255),
//		BLACK_TRANSPARENT
			NkColor.create().r((byte) 0).g((byte) 0).b((byte) 0).a((byte) 0x66),
//		DARKGRAY
			NkColor.create().r((byte) 40).g((byte) 40).b((byte) 40).a((byte) 255),
//		ORANGE
			NkColor.create().r((byte) 255).g((byte) 187).b((byte) 0).a((byte) 255),
//		RAISIN_BLACK
			NkColor.create().r((byte) 36).g((byte) 36).b((byte) 46).a((byte) 255),
//		DARK_RAISIN_BLACK
			NkColor.create().r((byte) 18).g((byte) 18).b((byte) 23).a((byte) 255),
//		BLUSH
			NkColor.create().r((byte) 218).g((byte) 102).b((byte) 123).a((byte) 255),
//		SKY_BLUE_CRAYOLA
			NkColor.create().r((byte) 83).g((byte) 216).b((byte) 251).a((byte) 255),
//		CHAMPANGE
			NkColor.create().r((byte) 241).g((byte) 224).b((byte) 197).a((byte) 255),
//		STAR_COMMAND_BLUE
			NkColor.create().r((byte) 34).g((byte) 116).b((byte) 165).a((byte) 255),
//		AERO_BLUE
			NkColor.create().r((byte) 184).g((byte) 242).b((byte) 230).a((byte) 255),
//		EGGSHELL
			NkColor.create().r((byte) 248).g((byte) 244).b((byte) 242).a((byte) 255),
//		UNBLEACHED_SILK
			NkColor.create().r((byte) 250).g((byte) 219).b((byte) 199).a((byte) 255),
//		GUNMETAL
			NkColor.create().r((byte) 45).g((byte) 49).b((byte) 57).a((byte) 255),
//		JET
			NkColor.create().r((byte) 48).g((byte) 50).b((byte) 60).a((byte) 255),
//		CHARCOAL
			NkColor.create().r((byte) 54).g((byte) 59).b((byte) 69).a((byte) 255),
//		MOUNTBATTEN_PINK
			NkColor.create().r((byte) 145).g((byte) 120).b((byte) 128).a((byte) 255),
//		OLD_LEVANDER
			NkColor.create().r((byte) 135).g((byte) 110).b((byte) 118).a((byte) 255),
//		BLUE_GREEN_COLOR_WHEEL
			NkColor.create().r((byte) 7).g((byte) 75).b((byte) 58).a((byte) 255),
//		CASTLETON_GREEN
			NkColor.create().r((byte) 9).g((byte) 93).b((byte) 72).a((byte) 255),
//		TROPICAL_RAIN_FOREST
			NkColor.create().r((byte) 9).g((byte) 113).b((byte) 85).a((byte) 255),
//		PAOLO_VERONESE_GREEN
			NkColor.create().r((byte) 12).g((byte) 151).b((byte) 114).a((byte) 255),
//		POWER_RED
			NkColor.create().r((byte) 255).g((byte) 34).b((byte) 12).a((byte) 255),
//		MEDIUM_SPRING_GREEN
			NkColor.create().r((byte) 86).g((byte) 240).b((byte) 158).a((byte) 255),
//		VERY_BLACK_TRANSPARENT
			NkColor.create().r((byte) 0).g((byte) 0).b((byte) 0).a((byte) 0xcc),
//		SILVER
			NkColor.create().r((byte) 142).g((byte) 133).b((byte) 123).a((byte) 0xcc),
//		BRONZE
			NkColor.create().r((byte) 205).g((byte) 127).b((byte) 50).a((byte) 0xcc),
//		TUR2
			NkColor.create().r((byte) 0).g((byte) 190).b((byte) 204).a((byte) 0xcc),
//		GOLD
			NkColor.create().r((byte) 177).g((byte) 150).b((byte) 27).a((byte) 0xcc),
//		FAWN
			NkColor.create().r((byte) 247).g((byte) 178).b((byte) 103).a((byte) 255),
//		RICH_BLACK
			NkColor.create().r((byte) 3).g((byte) 25).b((byte) 39).a((byte) 255),
//		GREENER_WON
			NkColor.create().r((byte) 21).g((byte) 244).b((byte) 69).a((byte) 255),
//		PICTON_BLUE
			NkColor.create().r((byte) 10).g((byte) 173).b((byte) 255).a((byte) 255),
	};
	
	public static String strByInt(int i, UIColors startWith) {
		return "#" + values()[startWith.ordinal() + i].toString();
	}

	public static NkColor valByInt(int i, UIColors startWith) {
		return COLORS[startWith.ordinal() + i];
	}

	public static Vec3 infiniteColor(int i) {
		var TileColorRandomizer = new Random(i);
		return new Vec3(TileColorRandomizer.nextFloat(), TileColorRandomizer.nextFloat(), TileColorRandomizer.nextFloat());
	}
}
