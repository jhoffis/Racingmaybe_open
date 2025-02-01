package test;

import java.util.HashMap;

import org.junit.jupiter.api.Test;

import game_modes.GameMode;
import game_modes.LeadoutMode;
import game_modes.SingleplayerChallenges;
import game_modes.SingleplayerChallengesMode;
import player_local.Bank;
import player_local.Player;

public class TestSP {
	
	@Test
    void TestScore() {
//		var p = new Player();
//		p.fastestTimeLapsedInRace = 0;
//		p.bank.add(Integer.MAX_VALUE, Bank.MONEY);
//
//		var map = new HashMap<Byte, Player>();
//		map.put(p.id, p);
//
//		var sp = new SingleplayerChallengesMode(SingleplayerChallenges.Master.ordinal());
//		sp.init(p, map);
//		sp.prepareNextRaceManually(200);
//		sp.updateInfo();
//		sp.setAttemptsLeft(1);
//
//
//
//		var score = sp.getCreateScoreNum(p);
//
//		System.out.println(score);
	}
	
	@Test
    void TestLeadoutTimePenalty() {
		for (int i = 5000; i < 20000; i += 1000) {
			var tp0 = GameMode.timePenalty(i, 0);
			var tp1 = GameMode.timePenalty(i, 5);
			System.out.println("TID: " + i + " = " + tp0 + " vs. " + tp1);
		}
		
		var tp2 = GameMode.timePenalty(25000, 0);
		var tp3 = GameMode.timePenalty(25000, 5);
		
	}
}
