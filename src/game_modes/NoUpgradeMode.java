package game_modes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import communication.Translator;
import engine.graphics.ui.UILabel;
import main.Features;
import main.Texts;
import player_local.Bank;
import player_local.Player;
import player_local.car.Rep;
import scenes.SceneHandler;
import scenes.game.Race;

public class NoUpgradeMode extends GameMode {

	private int racesLeft, typeOfUpgrade, nextTypeOfUpgrade;
	private int length;
	private final String name;
	private final GameModes gamemode;
	private boolean equalizedReps;

	public NoUpgradeMode(GameModes gamemode, int length, String name) {
		this.gamemode = gamemode;
		this.length = length;
		this.name = name;
		canSaveMoney = true;
	}
	
	@Override
	public void hostDoStuff() {
		nextTypeOfUpgrade = typeOfUpgrade = Features.ran.nextInt(7);
	}

	@Override
	public boolean isGameOver() {
		return racesLeft <= 0;
	}

	@Override
	public void startNewRaceDown() {
		racesLeft--;
		typeOfUpgrade = nextTypeOfUpgrade;
	}

	@Override
	public int getRandomRaceType() {
		return 0;
	}

	@Override
	public int newEndGoal(int gameLength) {
		endGoal = gameLength;
		racesLeft = endGoal;
		return endGoal;
	}

	@Override
	public void updateInfo() {
		String info = "Everyone gets random equal upgrades each round.\n"
				+ "Get the most points within " + endGoal + " races.\n"
				+ "If you're behind and win a race you\n"
				+ "might get extra points and a chance for comeback!";
		
		super.info = UILabel.create(info.split("\n"));
	}

	@Override
	public int getEndGoalStandard() {
		return length;
	}

	@Override
	public String getEndGoalTextDown() {
		return "Round: " + rounds + "\nRaces left: " + racesLeft + "/" + endGoal;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getExtraGamemodeRaceInfo() {
		return "Next up: " + switch (typeOfUpgrade) {
		case 0:
			yield "Horsepower";
		case 1:
			yield "Kilograms";
		case 2:
			yield "Bar";
		case 3:
			yield "Spool";
		case 4:
			yield "Aero";
		case 5:
			yield "N O S";
		case 6:
			yield "Tireboost";
		default:
			throw new IllegalArgumentException("Unexpected value: " + typeOfUpgrade);
		};
	}

	@Override
	public void rewardPlayer(int rounds, final int place, final int amountOfPlayers, int behindBy, long timeBehindFirst, Player player, boolean me) {
		Rep rep;
		if (!equalizedReps) {
			equalizedReps = true;
			var newRep = new Rep();
			for (var p : getPlayers().values()) {
				rep = p.getCarRep();
				for (int i = 0; i < Rep.size(); i++) {
					newRep.add(i, rep.get(i));
				}
			}
			for (int i = 0; i < Rep.size(); i++) {
				newRep.div(i, getPlayers().size());
			}
			
			newRep.set(Rep.nosBottles, newRep.getInt(Rep.nosBottles));
			newRep.set(Rep.gearTop, newRep.getInt(Rep.gearTop));
			
			for (var p : getPlayers().values()) {
				rep = p.getCarRep();
				for (int i = 0; i < Rep.size(); i++) {
					rep.set(i, newRep.get(i));
				}
			}
		}
		
		
		rep = player.getCarRep();
		
		rounds += 1;
		
		var everyoneGot = "Everyone got ";
		
		switch (typeOfUpgrade) {
		case 0: // Horsepower
			var kw = 100 * rounds;
			rep.add(Rep.kW, kw);
			everyoneGot += kw + " " + Texts.tags[Rep.kW];
			break;
		case 1: // Kg
			var ogKg = rep.get(Rep.kg);
			rep.mul(Rep.kg, .9);
			everyoneGot += (rep.get(Rep.kg) - ogKg) + " " + Texts.tags[Rep.kg];
			break;
		case 2: // Bar
			var bar = 1*rounds;
			
			rep.add(Rep.bar, bar);
			rep.add(Rep.turboblowRegen, 25);
			rep.add(Rep.turboblowStrength, .25);
			everyoneGot += bar + " " + Texts.tags[Rep.bar] + " and some turbo blow";
			break;
		case 3: // spool
			if (rep.getInt(Rep.bar) == 0) {
				rep.set(Rep.bar, 1);
			}
			var spool = .25*rounds;
			rep.add(Rep.spool, spool);
			rep.add(Rep.spoolStart, .1);
			everyoneGot += spool + " " + Texts.tags[Rep.spool];
			break;
		case 4: // aero
			rep.mul(Rep.aero, .75);
			everyoneGot += " -25% " + Texts.tags[Rep.aero];
			break;
		case 5: // nos
			if (rounds % 2 == 0 && rep.getInt(Rep.nosBottles) < 8) {
				rep.add(Rep.nosBottles, 1);
				if (rep.getInt(Rep.nos) == 0) {
					rep.set(Rep.nos, 1);
				}
				everyoneGot += "a nos bottle";
			} else {
				if (rep.getInt(Rep.nosBottles) == 0) {
					rep.set(Rep.nosBottles, 1);
				} else {
					rep.add(Rep.nosMs, 100);
				}
				rep.add(Rep.nos, rounds);
				everyoneGot += rounds + " nos";
			}
			break;
		case 6: // tireboost
			if (rounds >= 5) {
				rep.setBool(Rep.tbArea, true);
			}
			
			rep.add(Rep.tbMs, 100);
			rep.add(Rep.tb, rounds);
			everyoneGot += rounds + " tireboost";
			break;
		}
		rep.set(Rep.spdTop, 150 + (9+2*rounds)*rounds);
		everyoneGot += " and speed is now " + rep.get(Rep.spdTop);
		
		if (rounds >= 7) {
			rep.setBool(Rep.throttleShift, true);
			everyoneGot += " and dog-box";
		}
		if (rounds >= 14) {
			rep.setBool(Rep.sequential, true);
			everyoneGot += " and sequential";
		}
		
		if (!(amountOfPlayers == -1 || place < Race.CHEATED_NOT)) {
			
			worstTime(player);
			
			int pointsAdded = amountOfPlayers - (place + 1);
			if (place == 0 && behindBy > 0) {
				pointsAdded += Math.min((behindBy - 1) * .5, 2);
				if (racesLeft <= 1) {
					racesLeft += 1;
				}
			}
			player.bank.add(pointsAdded, Bank.POINT);
		}
		SceneHandler.showMessage(everyoneGot);
	}

	@Override
	public boolean isGameOverPossible() {
		return false;
	}

	@Override
	protected void setGeneralInfoDown(String[] input, AtomicInteger index) {
		length = Integer.parseInt(input[index.getAndIncrement()]);
		nextTypeOfUpgrade = Integer.parseInt(input[index.getAndIncrement()]);
		racesLeft = Integer.parseInt(input[index.getAndIncrement()]);
	}

	@Override
	protected void getGeneralInfoDown(StringBuilder sb) {
		sb
		.append(Translator.split)
		.append(length)
		.append(Translator.split)
		.append(typeOfUpgrade)
		.append(Translator.split)
		.append(racesLeft);

	}

	@Override
	public GameModes getGameModeEnum() {
		return gamemode;
	}

	@Override
	public List<Player> addExtraPlayers(CopyOnWriteArrayList<Player> sortedPlayers) {
		return sortedPlayers;
	}
	
}
