package player_local;

import engine.ai.AI;
import player_local.car.Rep;
/**
 * Kan ha sånn FinA3 osv for plassering av tiles kanskje
 */
public class Bot extends Player {

	static enum Priority {
		eco, speed, power, boost, tools
	}
	
	public void chooseNextChoice(Player[] others, int distance) {
		/*
		 * if reaches redline
		 */
		var simulation = AI.calculateRace(car, distance);
		long fastestOpponentTime = -1;
		double highestOpponentSpd = 0;
		for (var other : others) {
			if (other.timeLapsedInRace != -1 && (fastestOpponentTime == -1 || other.timeLapsedInRace < fastestOpponentTime)) {
				fastestOpponentTime = other.timeLapsedInRace;
			}
			if (other.getCarRep().get(Rep.spdTop) > highestOpponentSpd) {
				highestOpponentSpd = other.getCarRep().get(Rep.spdTop);
			}
		}
		
		// After buying power check for top speed?
	}

}
