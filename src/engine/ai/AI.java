package engine.ai;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import communication.GameInfo;
import communication.Translator;
import engine.graphics.ui.modal.UIBonusModal;
import game_modes.GameModes;
import game_modes.LeadoutMode;
import game_modes.TotalMode;
import main.Main;
import player_local.Bank;
import player_local.Layer;
import player_local.Player;
import player_local.TilePiece;
import player_local.car.Car;
import player_local.car.Rep;
import player_local.upgrades.Store;
import player_local.upgrades.Tool;
import player_local.upgrades.Upgrade;
import player_local.upgrades.UpgradeGeneral;
import player_local.upgrades.UpgradeResult;
import player_local.upgrades.Upgrades;
import scenes.game.Race;
import scenes.regular.ReplayVisual;

public class AI {
	/**
	 * // * Basert p� scorene til motstanderne s� skal en komme fram til en liste
	 * med alternative // * rekkef�lger av actions som kan v�re bedre enn andre. Se
	 * liksom flere trekk fremover. // * Den skal s� anse hvilke av hvert trekk som
	 * er mest sannsynlig � v�re best og ta de // * , men for � f� forskjeller s�
	 * kan verdier v�re krydret med litt randomness og om de med h�yest // *
	 * sannsynlighet for � vinne er lik s� trill terning om hvilken.
	 * 
	 * Tenk også slik at den går sånn 60 trekk fremover i alle mulige valg og så ser
	 * den hva som gir mest score. Score er jo skrevet i tabben. Men i tabben står
	 * det jo delt... hmmmmmmm Les på go, hvordan ser den hva som er best? Ser den
	 * bare på territorie + captures score`?? Kanskje første generasjon kan basere
	 * seg på denne men så ved å bare prøve alt så kan den selv bestemme hva som
	 * burde øke score mest?
	 * 
	 * Kanskje når man lager scoring spreadsheet så få aien til å gå igjennom alle
	 * mulige trekk og plukk ut de som vinner.
	 * 
	 * //
	 */
//    public List<Queue<List<Float>>> calcChoicesBasedOnSituation(List<Player> players, GameMode gm, Store store) {
//		/*
//		Hva med � sjekke hva som er best om en linje, s� ta de 10 beste, sjekk hva som er
//		best med de 10 beste neste, osv osv til en er "tom" for penger og s� sjekk flere runder
//		fremover ogs�. N�r man sjekker flere runder fremover s� legg til penger man skal tjene etter hver runde
//		Til slutt sammenligner du alle innerste noder
//		og ser hvem som ender best. F�lg den.
//		*/
//
//        var bestLines = new ArrayList<Queue<List<Float>>>();
//        boolean checkedEveryAlternative = false;
//        // Kanskje heller ha en liste med alle alternativ, og s� krysse ut hvilke av de som er tatt for dette stadiet.
//        // S� kan listen ha en prosent-verdier som sier hva som er mest sannsynlig best,
//        // som kommer av tidligere valg av forrige ai-generasjoner.
//        // 		Problemet er da at kanskje s� at hvordan skal en ha prosenter som hvert eneste realtall som scorene kan ha?
//        // 		Ikke bare det, men spillet kan vare uendelig lenge.
//        // Kanskje heller en liste med tidligere valg med en score bak. S� liksom bare "Move0:Supplementary on x3, Score: 3"
//        // Ogs� om det ikke er noen scorer � g� ut ifra s� kan man bare som normalt beregne hvor sterkt man synes at et visst trekk er + en rekke av trekk.
//        // Kanskje man kan ogs� ha noen score verdier som sier hva AI-en foretrekker � �ke i verdi. Liksom at den preferer � �ke tb over nos s�nn generelt.
//		/*
//		while (!checkedEveryAlternative) { // Lines � sjekke
//			var lineOfPlay = new ArrayBlockingQueue<List<Float>>(30); // Burde vel kanskje heller spre seg som et tre...
//			bestLines.add(lineOfPlay);
//
//			for (var upgrade : upgrades.getUpgrades()) { // Pr�v alle i store p� alle steder...
//				if (upgrade.isOpenForUse()) {
//					// N�r man plasserer s� lagre i tillegg til hvor hva plassen inneholder. Liksom er den x1 eller x3? Ogs� hva inneholder tilesa rundt kanskje?
//					store.attemptBuyTile(getClone(), null, null, COMMENTATOR);
//				}
//			}
//			for (var placedTile : layer.getLinArr()) { // Pr�v alle p� brettet
//				if (placedTile.getUpgrade().isOpenForUse()) {
//					store.attemptBuyUpgrade(getClone(), placedTile.getUpgrade(), placedTile.getLayerPos());
//				}
//			}
//
//		}
//		*/
//
//
//
//        for (var player : players) {
//            var rep = player.getCarRep();
//        }
//        return null;
//    }
//
//    private int difficulty;
//
//    /**
//     * @param id
//     * @param diff
//     */
//
//    public AI() {
//        super();
//        name = "AI";
//    }
//
//    private AI(String name, String id, String host, String carName) {
//        super(name, id, host, carName);
//
//    }
//
//    public void upgradeCar() {
//        // TODO implement upgrading via Car class and make AI upgrade in a smart / dumb
//        // way.
//    }
//
//
//	private static boolean tryNewTile(List<UpgradeGeneral> upgrades, Random ran, Player player, Store store,
//			UIBonusModal bonusModal) {
//		TilePiece<?> tile = null;
//		UpgradeGeneral upgrade;
//		int max = upgrades.size();
//		int index = ran.nextInt(max);
//		int tries = 0;
//		boolean found = false;
//		do {
//			upgrade = upgrades.get(index);
//			if (upgrade != null && upgrade.isOpenForUse() && player.bank.canAfford(upgrade.getCost(), Bank.MONEY)) {
//				tile = new TilePiece<>(upgrade, -1, -1);
//				break;
//			}
//			index = (index + 1) % max;
//			tries++;
//		} while (tries < max);
//
//		if (tile != null) {
//			int width = player.layer.getWidth();
//			max = width * player.layer.getHeight();
//			index = ran.nextInt(max);
//			tries = 0;
//			do {
//
//				tile = new TilePiece<>(upgrade, index % width, index / width);
//				switch (store.attemptBuyTile(player, tile, 0)) {
//				case Bought:
//					found = true;
//					break;
//				case FoundBonus:
//					int bonusChoices = 5;
//					index = ran.nextInt(bonusChoices);
//					for (int n = 0; n < bonusChoices; n++) {
//						int checkChoice = index - n;
//						if (checkChoice < -1)
//							break;
//						if (bonusModal.select(player, store, checkChoice) == UpgradeResult.Bought) {
//							found = true;
//							break;
//						}
//					}
//					if (!found)
//						bonusModal.cancel(player);
//					break;
//				default:
//					break;
//				}
//				index = (index + 1) % max;
//				tries++;
//			} while (tries < max && !found);
//		}
//
//		return found;
//	}
//
//	private static boolean tryImproveTile(Player player, Store store, Random ran) {
//		boolean found = false;
//		int width = player.layer.getWidth();
//		int max = width * player.layer.getHeight();
//		int index = ran.nextInt(max);
//		int tries = 0;
//		UpgradeGeneral upgrade = null;
//		do {
//			int x = index % width;
//			int y = index / width;
//			upgrade = player.layer.get(x, y);
//			if (upgrade != null && upgrade.isOpenForUse() && player.bank.canAfford(upgrade.getCost(), Bank.MONEY)
//					&& upgrade instanceof Upgrade up) {
//				if (store.attemptImproveUpgrade(player, up, x, y) == UpgradeResult.Bought) {
//					found = true;
//					break;
//				}
//			}
//			index = (index + 1) % max;
//			tries++;
//		} while (tries < max && !found);
//
//		return found;
//	}
//
//	private static boolean trySaleTile(Player player, Random ran) {
//		boolean found = false;
//		int width = player.layer.getWidth();
//		int max = width * player.layer.getHeight();
//		int index = ran.nextInt(max);
//		int tries = 0;
//		do {
//			int x = index % width;
//			int y = index / width;
//			var upgrade = player.layer.getPiece(x, y);
//
//			if (upgrade != null && upgrade.upgrade() != null) {
//				player.sellTile(upgrade, 0);
//				break;
//			}
//			index = (index + 1) % max;
//			tries++;
//		} while (tries < max && !found);
//
//		return found;
//	}
//
//	private static List<Scores> loadScores() {
//		var scoresAtChoice = new ArrayList<Scores>();
//
//		var scoresFile = "scores.txt";
//		var file = new File(scoresFile);
//		if (file.exists()) {
//			List<String> lines;
//			try {
//				lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
//				for (var line : lines) {
//					var score = new Scores();
//					var a = line.split("#");
//
//					var mech = a[0].split(",");
//					for (int i = 0; i < score.mechanic.length; i++)
//						score.mechanic[i] = Integer.parseInt(mech[i]);
//
//					var typeTile = a[1].split(",");
//					for (int i = 0; i < score.typeTile.length; i++)
//						score.typeTile[i] = Integer.parseInt(typeTile[i]);
//
//					var nextTo = a[2].split(",");
//					for (int i = 0; i < score.typeTile.length; i++)
//						score.nextTo[i] = Integer.parseInt(nextTo[i]);
//
//					var onto = a[3].split(",");
//					for (int i = 0; i < score.onto.length; i++)
//						score.onto[i] = Integer.parseInt(onto[i]);
//
//					scoresAtChoice.add(score);
//				}
//			} catch (IOException e) {
//			}
//		}
//		return scoresAtChoice;
//	}
//
//	static class AIPlayer {
//		public AIPlayer(Player player2, int choices) {
//			this.player = player2;
//			this.choices = new int[choices];
//			this.scores = new ArrayList<Scores>();
//		}
//
//		Player player;
//		int[] choices; // amount of choices this turn
//		List<Scores> scores;
//	}
//
	static class Scores {
		int[] mechanic = new int[3]; // eco, power, boost
		int[] typeTile = new int[20]; // namesid
		int[] nextTo = new int[4]; // namesid
		int[] onto = new int[6]; // timesmod
	}

//	public static void chooseUpgrades() {
//
//		var gm = new LeadoutMode(GameModes.LEADOUT, false, 3, "hei");
//		var allInfo = gm.getAllInfo().split(Translator.split);
//		Upgrade.pricePlacedStd = 40;
//		Upgrade.priceFactorStd = 1;
//
//		var scores = loadScores();
//		var emptyScore = new Scores();
//
//		
//		var bonusModal = new UIBonusModal(null, null);
//		var store = new Store();
//		store.setBonusModal(bonusModal);
//		
//		var ran = new Random();
//		int playerAmount = 10;
//		int generationN = 5;
//		int roundsLength = 15;
//		String basedLayer = Translator.getCloneString(new Layer());
//		var winners = new ArrayList<AIPlayer>();
//
//		for (int generations = 0; generations < generationN; generations++) {
//
//			System.out.println("GENERATION " + generations);
//			var players = new ArrayList<AIPlayer>();
//			AIPlayer ai;
//			AIPlayer best = null;
//			Player player;
//			
//			// Setup
//			for (int hone = 0; hone < roundsLength; hone++) {
//				players.clear();
//				gm.setAllInfo(allInfo, new AtomicInteger());
//				
//				for (int i = 0; i < playerAmount; i++) {
//					ai = new AIPlayer(new Player(), roundsLength);
//					players.add(ai);
//					player = ai.player;
//					
//					if (best != null) {
//						var hist = best.player.getHistory();
//
//						int amountChoices = 0;
//						for (int a = 0; a < hone; a++) {
//							amountChoices += best.choices[a];
//							ai.choices[a] = best.choices[a];
//						}
//						ai.scores.addAll(best.scores);
//
//						Translator.setCloneString(player, hist.get(0));
//						player.resetHistory();
//						for (int a = 1; a < amountChoices; a++) {
//							player.addHistory(hist.get(a));
//						}
//						player.historyBackHome();
//						player.setHistoryNow();
//						player.bank.set(0, Bank.POINT);
//					} else {
//						Translator.setCloneString(player.layer, basedLayer);
//						player.bank.add(150, Bank.MONEY);
//					}
//					for (var up : player.upgrades.getUpgrades()) {
//						if (up == null || up.getPremadePrice() != 0)
//							continue;
//						up.setPremadePrice(50);
//					}
//				}
//
//				// do the game
//				for (int rounds = hone; rounds < roundsLength; rounds++) {
//					System.out.println("Round: " + rounds);
//					var raceLength = (int) (240 * rounds * 1.15);
//					
//					
//
//					for (int i = 0; i < playerAmount; i++) {
//						ai = players.get(i);
//						player = ai.player;
//						var upgrades = player.upgrades.getUpgradesAll();
//						
//						int amountChoices = 0;
//						for (int a = 0; a < rounds; a++) {
//							amountChoices += best.choices[a];
//						}
//
//						boolean found = true;
//						while (found) {
//							Scores score = null;
//							if (amountChoices < scores.size())
//								score = scores.get(amountChoices);
//							else
//								score = emptyScore;
//							found = false;
//							
//							
//
//							float ranVal = ran.nextFloat();
//							if (ranVal < .4) {
//								// try new tile
//								found = tryNewTile(upgrades, ran, player, store, bonusModal);
//								if (!found) {
//									found = tryImproveTile(player, store, ran);
//								}
//							} else if (ranVal < .98) {
//								found = tryImproveTile(player, store, ran);
//								if (!found) {
//									found = tryNewTile(upgrades, ran, player, store, bonusModal);
//								}
//							} else {
//								trySaleTile(player, ran);
//								found = true;
//							}
//
//							if (found) {
//								player.addHistory(Translator.getCloneString(player));
//								ai.choices[rounds]++;
//								amountChoices++;
//							}
//						}
//					}
//
//					var pls = new Player[players.size()];
//
//					for (int i = 0; i < pls.length; i++) {
//						player = players.get(i).player;
//						player.car.reset();
//						if (player.getCarRep().get(Rep.spdTop) > 100) {
//							player.timeLapsedInRace = calculateRace(player.car, raceLength);
//						} else {
//							player.timeLapsedInRace = Race.CHEATED_GAVE_IN;
//						}
//						pls[i] = player;
//					}
//
//					GameInfo.determinePositioningFinishedRace(gm, pls, 1);
//					gm.prepareNextRaceManually(roundsLength);
//					
//					for (var p : pls) {
//						Tool.runToolAfterRace(p, rounds);
//					}
//				}
//				
//				AIPlayer winner = null;
//				for (var p : players) {
//					if (winner == null || p.player.bank.get(Bank.POINT) > winner.player.bank.get(Bank.POINT)) {
//						winner = p;
//					}
//				}
//				best = winner;
//				winners.add(winner);
//			}
//		}
//		
//		var sortedPlayersFinal = new ArrayList<Player>();
//		for (var p : winners) {
//			var player = p.player;
//			sortedPlayersFinal.add(player);
//			player.bank.set(calculateRace(player.car, 1500), Bank.POINT);
//			player.redoLastHistory();
//		}
//		GameInfo.sortPlayers(sortedPlayersFinal);
//
//		var players = new Player[12];
//		for (int i = 0; i < players.length; i++) {
//			int n = sortedPlayersFinal.size() - 1 - i;
//			if (n >= 0)
//				players[i] = sortedPlayersFinal.get(n);
//		}
//
//		ReplayVisual.saveReplay(new TotalMode(GameModes.TOTAL, roundsLength, "AI Test"), players);
//	}

//	public static void chooseUpgrades() {
//		// Choose twelve random lines and test them against eachother.
//		// Pick the three best and generate nine new lines.
//		// Repeat twelve times.
//		// Save all the matches so i can just see them manually for now.
//
//		Upgrade.pricePlacedStd = 40;
//		Upgrade.priceFactorStd = 1;
//		var bonusModal = new UIBonusModal(null, null);
//		var store = new Store();
//		store.setBonusModal(bonusModal);
//
//		var ran = new Random();
//		int len = 100;
//		int generationN = 5000;
//		int roundsLength = 15;
//		var scores = new int[len];
//		var scoreses = new Scores[len][];
//		var choices = new int[len][];
//		var times = new long[len];
//		var players = new Player[len];
//
//		int raceLengthBased = 240;
//
//		var sortedPlayersAll = new ArrayList<Bla>();
//		String basedLayer = Translator.getCloneString(new Layer());
//
//		try {
//			for (int generations = 0; generations < generationN; generations++) {
//
//				System.out.println("GENERATION " + generations);
//				var sortedPlayers = new ArrayList<Bla>();
//				Player player;
//				Bla best = null;
//				for (int hone = 0; hone < roundsLength; hone++) {
//					for (int i = 0; i < len; i++) {
//						player = new Player();
//						players[i] = player;
//						choices[i] = new int[roundsLength];
//						scoreses[i] = new Scores[roundsLength];
//						if (best != null) {
//							var hist = best.player.getHistory();
//
//							int amountChoices = 0;
//							for (int a = 0; a < hone; a++)
//								amountChoices += best.choices[a];
//
//							Translator.setCloneString(player, hist.get(0));
//							player.resetHistory();
//							for (int a = 1; a < amountChoices; a++) {
//								player.addHistory(hist.get(a));
//							}
//							player.historyBackHome();
//							player.setHistoryNow();
//							choices[i][0] = amountChoices;
//							player.bank.set(0, Bank.POINT);
//						} else {
//							Translator.setCloneString(player.layer, basedLayer);
//							player.bank.add(150, Bank.MONEY);
//						}
//						for (var up : player.upgrades.getUpgrades()) {
//							if (up == null || up.getPremadePrice() != 0)
//								continue;
//							up.setPremadePrice(50);
//						}
//					}
//
//					for (int rounds = hone; rounds < roundsLength; rounds++) {
//						System.out.println("Round: " + rounds);
//						var raceLength = (int) (240 * rounds * 1.15);
//
//						for (int i = 0; i < len; i++) {
//							player = players[i];
//							var upgrades = player.upgrades.getUpgradesAll();
//
//							boolean found = true;
//							while (found) {
//								found = false;
//
//								float ranVal = ran.nextFloat();
//								if (ranVal < .4) {
//									// try new tile
//									found = tryNewTile(upgrades, ran, player, store, bonusModal);
//									if (!found) {
//										found = tryImproveTile(player, store, ran);
//									}
//								} else if (ranVal < .98) {
//									found = tryImproveTile(player, store, ran);
//									if (!found) {
//										found = tryNewTile(upgrades, ran, player, store, bonusModal);
//									}
//								} else {
//									trySaleTile(player, ran);
//									found = true;
//								}
//
//								if (found) {
//									player.addHistory(Translator.getCloneString(player));
//									choices[i][rounds]++;
//									
//									
//									
//								}
//							}
//
//							player.car.reset();
//							if (player.getCarRep().get(Rep.spdTop) > 100)
//								times[i] = calculateRace(player.car, raceLength);
//							Tool.runToolAfterRace(player, rounds);
//						}
//
//						int winner = 0;
//						for (int i = 0; i < len; i++) {
//							players[i].bank.add(200 + players[i].getCarRep().getInt(Rep.moneyPerTurn)
//									+ (players[i].bank.get(Bank.MONEY) * players[i].getCarRep().getInt(Rep.interest)),
//									Bank.MONEY);
//							if (times[i] < times[winner]) {
//								winner = i;
//							}
//						}
//						scores[winner]++;
//						players[winner].bank.add(1, Bank.POINT);
//						for (var p : players)
//							p.redoLastHistory();
//					}
//
//					int winner = 0;
//					for (int i = 0; i < len; i++) {
//						if (scores[i] > scores[winner]) {
//							winner = i;
//						}
//					}
//					players[winner].bank.set(0, Bank.POINT);
//					sortedPlayers.add(new Bla(players[winner], choices[winner]));
//					long bestTime = Long.MAX_VALUE;
//					for (var bestPlayer : sortedPlayers) {
//						if (calculateRace(bestPlayer.player.car, raceLengthBased*5) < bestTime) {
//							best = bestPlayer;
//						}
//					}
//					sortedPlayersAll.add(best);
//
//				}
//			}
//		} catch (Exception e) {
//			System.out.println("ERROR");
//			System.out.println(e.getLocalizedMessage());
//		}
//
//		var sortedPlayersFinal = new ArrayList<Player>();
//		for (var p : sortedPlayersAll) {
//			var player = p.player;
//			sortedPlayersFinal.add(player);
//			player.bank.set(calculateRace(player.car, 1500), Bank.POINT);
//			player.redoLastHistory();
//		}
//		GameInfo.sortPlayers(sortedPlayersFinal);
//
//		players = new Player[12];
//		for (int i = 0; i < players.length; i++) {
//			int n = sortedPlayersFinal.size() - 1 - i;
//			if (n >= 0)
//				players[i] = sortedPlayersFinal.get(n);
//		}
//
//
//	}
//	
//	private static TreeMap<String, Integer> getTree() {
//		var path = "RMAI";
//		var file = new File(path);
//		try {
//			if (!file.createNewFile()) {
//				return ;
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		List<String> lines;
//		try {
//			lines = Files.readAllLines(path, StandardCharsets.UTF_8);
//		} catch (IOException e) {
//			return false;
//		}
//		
//	}
//	
//	public static void calculateMegaImpossible() {
//		
////		var tree
//		ReplayVisual.saveReplay(new TotalMode(roundsLength, "AI Test"), players);
//	}

	public static long calculateRace(Car car, int length) {
		long time = 0, tick = 0, tries = 0;

		var stats = car.getStats();
		var rep = car.getRep();
		car.reset(null);

		// Race
		car.clutch(false);
		if (car.hasTireboost()) {
			Car.funcs.tireboost(stats, rep, time, 1, 1);
		}
		float prevNosValue = 0;
		while (stats.distance < length) {

			/*
			 * Idea here is to wait until nos provides the most optimal power and speed. But
			 * how can you know?
			 * 
			 * hm, no this is wrong because if you think of a curve - this will probably
			 * give very little? ... Maybe the driving also needs general ai? Because when
			 * is it best to use turbo blow? When is it best to use nos?
			 */

			if (!car.getStats().NOSON) {
				Car.funcs.nos(stats, time, 1);
			}

			if (Car.funcs.isGearCorrect(car.getStats(), car.getRep()) && !car.getStats().grinding
					|| Car.funcs.isTopGear(stats, rep)) {
				if (car.hasTurbo()) {
					if (stats.stats[Rep.turboblow] > 0 && stats.spool > 0.2) {
						stats.turboBlowON = true;
					} else {
						stats.turboBlowON = false;
					}
				}
				car.throttle(true, true);
			} else {
				car.getStats().grinding = false;
				car.getStats().grindingTime = 0;
				car.clutch(true);
				car.shiftUp(time);
				if (!car.getStats().sequentialShift) {
					tick += 3;
					car.updateSpeed(1f, time);
				}
				car.clutch(false);
			}

			car.updateSpeed(1f, time);

			if (car.getStats().speed < 1) {
				tries++;
				if (tries > 1000) {
					time = Long.MAX_VALUE;
					break;
				}
			} else {
				tries = 0;
			}

			/*
			 * So, with decentra you hit 10m at around 4500ms and shift at around 5200ms but
			 * here it is 2300ms and 2475ms, like basically half the amount...
			 */
//            System.out.println(tick + "tick / " +time + "ms: \n"
//                    + stats.distance + " m\n"
//                    + stats.rpm + " rpm\n"
//                    + stats.gear + " gear\n"
//                    + stats.speed + " speed\n"
//                    + stats.spdinc + " spdinc\n"
//                    + stats.turboBlowON + " turboblowON\n"
//                    + stats.stats[Rep.turboblow] + " turboblow\n"
//            );
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
			tick++;
			time = (long) ((double) tick / (double) Main.TICK_STD * 1000d);
		}

		car.reset(null);

		return time;
	}
//
//    private boolean engagedGear(int finetune) {
//        int ranval = Features.ran.nextInt(100);
//        return ranval + difficulty - finetune >= 50;
//    }
//
//    /**
//     * @return name#ready#car#...
//     */
//    @Override
//    public String getRaceInfo(boolean allFinished) {
//        return name + "#" + 2 + "#" + timeLapsedInRace + "#, +" + pointsAdded + " points, +$" + moneyAdded + "#"
//                + carName.toLowerCase();
//    }

}
