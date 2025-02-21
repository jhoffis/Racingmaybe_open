package main;

import engine.graphics.ui.UIColors;
import engine.io.InputHandler;
import engine.utils.TwoTypes;
import game_modes.SingleplayerChallenges;
import player_local.upgrades.TileNames;
import player_local.upgrades.Upgrade;
import player_local.upgrades.UpgradeGeneral;
import scenes.game.Race;
import scenes.regular.HotkeysScene;
import player_local.car.Rep;
import settings_and_logging.hotkeys.CurrentControls;

public class Texts {

    public static final String
            mainMenu = "Main Menu",
            optionsText = "Options",
            gobackText = "<- Go back",
            nextSong = "Next song",
            leaveText = "Leave game?",
            resignText = "Resign game?",
            singleplayerText = "Singleplayer",
            multiplayerText = "Multiplayer",
            exitText = "G T F O",
            replayText = "View replays",
            exitOKText = "Hell yeah!",
            exitCancelText = "C A N C E L  T H A T  S H I T",
            createOnlineText = "Create online lobby",
            createLanText = "Create LAN lobby",
            joinOnlineText = "Join selected lobby",
            joinLanText = "Join a LAN lobby",
            usernameText = "Whats your username?",
            refreshText = "Refresh",
            minimizeText = "Iconify",
            lobbiesText = "Online lobbies:",
            exitLabelText = "Sure you wanna exit?#" + UIColors.WHITE,
            joining = "Joining...",
            carRandom = "Random",
            showHints = "Show hints",
    //	bonusBolt = "bolt",
    optionsControlsText = optionsText + " and controls",
            leaderboardText = "The Leaderboard",
            tryAgain = "Try again?",
            privateText = "Private?",
            publicText = "Public?",
            nos = "nos",
            tireboost = "tb",
            weeklyText = "Weekly Challenge",
            difficultyChoose = "Choose a difficulty:",
            spectator = "Commentator",
            player = "Player",
            improveUpgrade = "Improve Tile",
            destroyUpgrade = "Destroy Tile (NB! Removes \"x?\")",
            sellUpgrade = "Sell Tile",
            buyOffUpgrade = "Liquidate",
            menu = "Menu",
            closeMenu = "Close this menu",
            historyFwd = "Next",
            historyBck = "Previous",
            historyHome = "First",
            historyEnd = "Last",
            upgradeInfo = "Info",
            tileUpgrades = "Tile upgrades",
            unlocks = "Unlocks",
            confirmLeave = "Are you sure you want to leave?",
            confirmResign = "Are you sure you want to resign? You will become a loser.",
            undo = "Undo",
            redo = "Redo",
            noLobbies = "No lobbies found",
            searching = "Searching...",
            chatHere = "Chat here...",
            seconds = "s",
            objectives = "Objectives",
            timesModTooltip = "Place a tile here to multiply its base values\nafter adding neighbouring values.",
            continueText = "Continue to the shop...",
            restartWindow = "The games resolution width to height ratio has changed.\nRestart the game if you want to avoid too many glitches.",
            designerNotes = "How to play";


    public static String[]
            tags = new String[Rep.size()],
            singleplayerModes = {
                    "Beginner",
                    "Casual",
                    "Intermediate",
                    "Hard",
                    "Harder",
                    "Master",
                    "Samurai",
                    "Expert",
                    "Accomplished",
                    "Sensei",
                    "Legendary",
                    "Nightmarish",
                    "Unfair",
                    "Mega Unfair",
                    "The Boss"
            },
            dailyModes = {
                    "Daily Fun",
                    "Daily Weird",
                    "Daily Tough",
                    "Weekly Fun",
                    "Weekly Weird",
                    "Weekly Tough",
                    "Monthly Fun",
                    "Monthly Weird",
                    "Monthly Tough",
                    "Sandbox"
            },
            backstories = {
                    """
 As the request of God was for the 
 empire to expand; they had no choice
 but to sacrifice their young ones to
 the swords. Long has it been. 
  Too long in fact, and still there is
 so much blood left to spill.
 Stamus ad lucem in aeternum, amen.
					""",
                    """
 Hailing from ancient particals, 
 the Jaetnas have been enemies, but 
 also sexual partners, to 
 the Future Gods for time immemorial. 
  Then, during a localized Ytalroak, 
 the gods of Kalroaunde was eliminated, 
 allowing the Jaetnas, with their 
 military prowess, to expand their 
 empire from Jaetnaheim into 
 the eight other realms, including 
 The Outer Realm.
  The nobility of the Jaetnas were 
 satisfied with their conquests and 
 decided to suppress their warlike 
 traditions in favor for peace, 
 which many Jaetnas disagreed with. 
  One of these was Gronkur, the bastard 
 child of Tronkur, who led an 
 insurgency, toppling the nobility and 
 positioning himself as the leader of 
 the newly founded Jaetnatium. 
  Their conquest throughout the galaxy
 has finally led them to Authragard, 
 where they now face the remnants 
 of the Outers who fled 
 all those years ago.""",
                    """
 Immortal anti-matter Bhelronan from the
 future. This guy is dangerous and we
 don't know his name. 
 Don't get in his way.
					""",
                    """
 In the deepest depths below the 
 plane-reality of Authragard layed the 
 insect-like creatures called Wess. 
  As their plane is emptied by the 
 Great Emptiness, they began their slow
 colonizing from below via the realm 
 hole that they had built over 
 1 million years, with the bodies of 
 their food. 
  Exactly when their second sun 
 inverted their presence was moved to 
 the center of the Solar Empire. 
  Ever since there has been fighting 
 between the Empire and Wess.
    				""",
                    """
	            		Created nearly four million years ago by the Ancient Humans, the beautiful Aiazom were created for one single purpose
	            		- to please their creators and realize their sexual fantasies.  Through a miracle, the immortal leader and goddess 
	            		of the Aiazom, Aifrohm, gained the ability to think independently and used this to unite all the Aiazom under
	            		a single banner in order to exact revenge on their cruel masters. 
	            		As one of the oldest and most advanced races of Earth, the Aiazom brought devastating 
	            		wars to the other factions, destroying the Solar Empire and the Eurasian Alpha Centauri colonies. 
	            		Now, Aifrohm have gathered her ardent followers in Authragard, reluctantly fighting alongside her allies, in order to fulfill her prophecy - eliminate humanity once and for all.""",

                    """
	            		Animals suffered much while humankind was at their peak; meat production, big game hunting, skinning, destruction of various habitats, even harvesting of their yet unborn children in the form of eggs! Other animals were made weak as humans domesticated them, forming a dependent bond to their masters. Then, one day, as a human experiment went awry, Otto the Gazelle, managed to escape captivity, later noticing his intelligence increasing at a rapid rate.
	            		When he reached a genius-level intellect, Otto replicated the experiment on fellow animals and founded Gazellia. As the animals gained a sense of nationalistic pride, Otto, inspired by Hitler himself, took on the mantle of dictator, reformed Nazism, and adopted the surname von Dornberg. On the battlefield of Authragard, the animals of Gazellia are prepared to reclaim their place in the food chain.""",
                    """
	    				The Thelronans were originally natives of this terrible planet, but when the humans came their existence was threatened. The humans do not like what they do not understand, and Thelronans are not
	    				to be understood by outsiders. So the people Theilron Hills decided to interbreed with the colonising humans who at the time came from the Solar Empire. Now the race is all but gone, however the absolute rule
	    				has managed to stay pure Thelronan. This includes queen Xothine IV the Faceless and her everglorious rule! And now the pathetic Jotne want to fight? Bring it.
    				"""
            };

    public static final String[]
            CAR_TYPES = {
            "Decentra",
            "Oldsroyal",
            "Fabulvania",
            "Thoroughbred",
            carRandom,
    },
            DESCRIPTION = {
                    "Best at scaling!",
                    "Strongest Tireboost beast",
                    "Got an extra NOS bottle!",
                    "Strong at power, but weak boosts",
            },
            upgradeText = new String[TileNames.values().length],
            lobbyNames = {
                    "Car Selection",
                    "Shop",
            };

    public static String getUpgradeTitle(TileNames imageType) {
        var sb = new StringBuilder();
        var chars = imageType.toString().toCharArray();
        sb.append(chars[0]);
        for (int i = 1; i < chars.length; i++) {
            if (Character.isUpperCase(chars[i])) {
                sb.append(" ").append(chars[i]);
            } else {
                sb.append(chars[i]);
            }
        }
        return sb.toString();
    }

    public static String getUpgradeTitle(UpgradeGeneral upgrade) {
        return upgrade instanceof Upgrade up
                && up.overrideName != null ? up.overrideName : getUpgradeTitle(upgrade.getTileName());
    }

    public static String getUpgradeTitle(int upgradeID) {
        return getUpgradeTitle(TileNames.values()[upgradeID]);
    }

    public static void init() {
        tags[Rep.nosBottles] = "bottle";
        tags[Rep.nosMs] = Texts.nos + " ms";
        tags[Rep.nos] = Texts.nos;
//		tags[Rep.nosBottles]        = "bottles";
        tags[Rep.nosSoundbarrier] = "nos soundbarrier";
        tags[Rep.nosAuto] = "automatic first nos";
        tags[Rep.kg] = "kg";
        tags[Rep.kW] = "HP";
        tags[Rep.spdTop] = "km/h";
        tags[Rep.rpmIdle] = "idle-RPM";
        tags[Rep.rpmTop] = "RPM";
        tags[Rep.gearTop] = "gears";
        tags[Rep.tbMs] = Texts.tireboost + " ms";
        tags[Rep.tb] = Texts.tireboost;
        tags[Rep.tbHeat] = "heat";
        tags[Rep.tbArea] = "Guarenteed Tireboost";
        tags[Rep.turboblow] = "turbo-blow";
        tags[Rep.turboblowStrength] = tags[Rep.turboblow] + " strength";
        tags[Rep.moneyPerTurn] = "$ per turn";
        tags[Rep.interest] = "interest";
//		tags[Rep.boltsPerTurn]      = boltsBonus(2) + " per turn";
        tags[Rep.bar] = "bar";
        tags[Rep.highestSpdAchived] = "highest speed achieved";
        tags[Rep.stickyclutch] = "sticky clutch";
        tags[Rep.spool] = "spool";
        tags[Rep.sequential] = "sequential";
        tags[Rep.throttleShift] = "dog-box";
        tags[Rep.twoStep] = "two-step";
//        tags[Rep.powerShift] = "power-shift";
        tags[Rep.turboblowRegen] = tags[Rep.turboblow] + " regen";
//        tags[Rep.sale] = "sale";
//        tags[Rep.freeUpgrade] = "free upgrade";
        tags[Rep.spoolStart] = "spool minimum";
        tags[Rep.aero] = "aero";
        tags[Rep.life] = "life";
        tags[Rep.snos] = "snos";

        upgradeText[TileNames.Power.ordinal()] = """
                Increases length of the power-band
                and thereby makes the car stronger
                in higher RPM's.
                Higher idle-rpm means you start off
                with more power available.
                """;
        upgradeText[TileNames.Fuel.ordinal()] = """
                More efficient fuel means
                    MORE POWERRRRR!!!
                      """;
        upgradeText[TileNames.Boost.ordinal()] = "Powerful short bursts.\nIntroduces nitros and a better launch!";
        upgradeText[TileNames.Finance.ordinal()] = """
                Economy upgrade that scales
                Money Pit's around it.
                """;
        upgradeText[TileNames.Interest.ordinal()] = """
                Makes and adds part of your fortune
                to your income after each round.""";
        upgradeText[TileNames.Clutch.ordinal()] = "Top speed scaler, but also early.";
        upgradeText[TileNames.Gears.ordinal()] = "Top speed early.";
        upgradeText[TileNames.Aero.ordinal()] = """
                Lessens wind resistance, which means
                higher speed doesn't slow you down
                as much.""";
        upgradeText[TileNames.MoneyPit.ordinal()] = "Makes you earn tons of $$$.";
        upgradeText[TileNames.BlueNOS.ordinal()] =
                """
                        Spendable short-lived boost.
                        Affected by gearing and timing.
                        Cannot be placed next to other blue
                        bottle-tiles!""";
        upgradeText[TileNames.RedNOS.ordinal()] =
                "Place next to a " + getUpgradeTitle(TileNames.BlueNOS) + " to \n" +
                        "support early " + tags[Rep.nos] + " build-up.\n" +
                        "Then, late game, improve these tiles to \n" +
                        "scale your " + tags[Rep.nos] + "!";
        upgradeText[TileNames.Tireboost.ordinal()] = """
                Initial boost to get up in RPM.
                Launches your car when the race starts!
                But its strength depends on how fast you 
                react to the lights going green!""";
        upgradeText[TileNames.TireboostHeater.ordinal()] = """
                Scales your tireboost! This heat
                depletes a bit for every round.
                Also, the more heat you have the more heat 
                your heat has, dawg.""";
        upgradeText[TileNames.WeightReduction.ordinal()] = "Makes " + Texts.tags[Rep.kW] + " more valuable!";
        upgradeText[TileNames.LighterPistons.ordinal()] = "Late game power-scaling.";
        upgradeText[TileNames.Turbo.ordinal()] = "Scales power, but is slow to spool up!";
        upgradeText[TileNames.Block.ordinal()] = """
        Best early game for power.
        Can ONLY be placed next to other Blocks!""";
        upgradeText[TileNames.Supercharger.ordinal()] = "Faster version of Turbo.";
    }


    public static String leaderboardScoreName(int type) {
        if (type < singleplayerModes.length) {
//    		type = type == SingleplayerChallenges.Master.ordinal() ? SingleplayerChallenges.MegaImpossible.ordinal() + 1
//					: (type > SingleplayerChallenges.Master.ordinal() ? type - 1
//					: type);    		
            return singleplayerModes[type] + " Score Challenge";
        } else
            return Texts.dailyModes[type - singleplayerModes.length];
    }


//	public static String boltsBonus(double i) {
//		return i != 1 ? bonusBolt + "s" : bonusBolt;
//	}

    public static String ready(boolean ready, int length) {
        CurrentControls controls = CurrentControls.getInstance();
        return (ready ? "Unr" : "R") + "eady? " + length + "m (" + (InputHandler.CONTROLLER_EFFECTIVELY ? "X" : controls.getReady().getKeyName()) + ")";
    }


    public static String readySimple(boolean ready) {
        return (ready ? "Unr" : "R") + "eady? ";
    }

    public static String formatNumber(double n) {
        String res = null;

        if (Double.isInfinite(n))
            return "infinity";

        int ending = 0; // TODO gj�r denne finere, men dette er visst kjappere enn � telle tegn av string versjonen... weird
        if (n > 100000d) {
            while (n > 1000d) {
                n /= 1000d;
                ending++;
            }
        }

        double decimals = 100d;
        if (n < 0.1)
            decimals = 1000d;
        else if (n > 100d)
            decimals = 1d;

        n = Math.round(n * decimals) / decimals;
        if (Math.round(n) == n)
            res = String.valueOf(Math.round(n));
        else
            res = String.valueOf(n);
        return res + getNumberEndings(ending);
    }

    public static String formatNumberSimple(long n) {

        if (Double.isInfinite(n))
            return "infinity";

        if (n < 1000)
            return String.valueOf(n);

        var res = new StringBuilder();
        var chars = String.valueOf(n).toCharArray();

        int size = chars.length - 1;
        for (int i = size; i >= 0; i--) {
            if (i < size && (size - i) % 3 == 0)
                res.append(",");
            res.append(chars[i]);
        }

        return res.reverse().toString();
    }


    public static String getNumberEndings(double ending) {
    	if (ending * 3 >= Math.pow(10, Math.pow(10, 100))) return " googolplex";
        if (ending * 3 >= Math.pow(10, 100)) return " googol";
        if (ending * 3 >= 303) return " centillion";
        if (ending >= 21) return " vigintillion";
        if (ending >= 20) return " novemdecillion";
        if (ending >= 19) return " octodecillion";
        if (ending >= 18) return " septendecillion";
        if (ending >= 17) return " sexdecillion";
        if (ending >= 16) return " quindecillion";
        if (ending >= 15) return " quattuordecillion";
        if (ending >= 14) return " tredecillion";
        if (ending >= 13) return " duodecillion";
        if (ending >= 12) return " undecillion";
        if (ending >= 11) return " septillion";
        if (ending >= 10) return " decillion";
        if (ending >= 9) return " nonillion";
        if (ending >= 8) return " octillion";
        if (ending >= 7) return " sextillion";
        if (ending >= 6) return " quintillion";
        if (ending >= 5) return " quadrillion";
        if (ending >= 4) return "t";
        if (ending >= 3) return "b";
        if (ending >= 2) return "m";
        if (ending >= 1) return "k";
        return "";
    }

    public static String getUpgradeInfo(Upgrade upgrade) {
        return upgradeText[upgrade.getNameID()];
    }


    /**
     * @return color and its str length
     */
    public static TwoTypes<UIColors, Integer> getColor(String text) {
        var split = text.split("#");
        if (split.length > 1) {
            var colorStr = split[split.length - 1];
            try {
                return new TwoTypes<>(UIColors.valueOf(colorStr), colorStr.length());
            } catch (IllegalArgumentException ex) {
                return null;
            }
        }
        return null;
    }

    /**
     * @return text without color ending
     */
    public static TwoTypes<TwoTypes<UIColors, Integer>, String> removeColor(String text) {
        var color = getColor(text);
        if (color != null)
            text = text.substring(0, text.length() - color.second() - 1);
        return new TwoTypes<>(color, text);
    }

    public static String setColor(String text, UIColors color) {
        return removeColor(text).second() + "#" + color;
    }

    public static String[] getPlayerListTitles() {
        return new String[]{
                "Power score",
                "NOS score",
                "Tireboost score",
                "Top speed",
                "Income"
        };
    }

    public static String raceTimeText(long thisPlayerTime) {
        if (thisPlayerTime == Race.CHEATED_TOO_EARLY) {
            return "DNF";
        } else if (thisPlayerTime == Race.CHEATED_GAVE_IN) {
            return "Gave in";
        }
        return Texts.formatNumber(thisPlayerTime / 1000d) + " seconds";
    }

    public static boolean isRandomCar(int i) {
        return Texts.CAR_TYPES[i].equals(Texts.carRandom);
    }

    public static String podiumConversion(int podium) {
        int podiumActual = (podium + 1);
        String res = String.valueOf(podiumActual);
        switch (podiumActual) {
            case 1 -> res += "st";
            case 2 -> res += "nd";
            case 3 -> res += "rd";
            default -> res += "th";
        }
        return res;
    }

}
