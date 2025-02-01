package scenes.regular;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import audio.SfxTypes;
import communication.GameInfo;
import communication.GameType;
import communication.Translator;
import engine.graphics.ui.*;
import engine.io.InputHandler;
import engine.io.Window;
import game_modes.GameModes;
import main.Features;
import main.Main;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import engine.ai.AI;
import engine.graphics.Renderer;
import engine.graphics.objects.Camera;
import game_modes.GameMode;
import player_local.Player;
import player_local.car.Car;
import scenes.Scenes;
import scenes.adt.ISceneManipulation;
import scenes.game.lobby_subscenes.UpgradesSubscene;

public class ReplayVisual implements ISceneManipulation {

	private final UIButton<?> prevPlayerBtn, nextPlayerBtn;
	private ReplayData replayData;
	private int currentPlayerIndex = 0;
	private final UpgradesSubscene upgradesVisualization;
	private static Thread benchmarkThread;

	private final UIScrollable rightStuff;
	private final UIRow btnsRow;

	public static final String folderName = "replays";
	private static final String gmPrefix = "gm: ", updatePrefix = "update: ", playerPrefix = "p: ";

	private UILabel playerLabel;
	private GameInfo com;

	public ReplayVisual() {
		upgradesVisualization = null;
		rightStuff = null;
		btnsRow = null;
		nextPlayerBtn = null;
		prevPlayerBtn = null;
	}

	public ReplayVisual(UpgradesSubscene upgradesVisualization) {
		this.upgradesVisualization = upgradesVisualization;

		nextPlayerBtn = new UIButton<>("Next player");
		nextPlayerBtn.tooltip = "j";
		prevPlayerBtn = new UIButton<>("Previous player");
		prevPlayerBtn.tooltip = "h";
		playerLabel = new UILabel(Nuklear.NK_TEXT_ALIGN_RIGHT);
		btnsRow = new UIRow(new IUIObject[] { null, prevPlayerBtn, nextPlayerBtn, }, 0);

		nextPlayerBtn.setPressedAction(() -> {
			Features.inst.getAudio().play(SfxTypes.REGULAR_PRESS);
			currentPlayerIndex = (currentPlayerIndex + 1) % replayData.players().size();
			var player = replayData.players().get(currentPlayerIndex);
			updateVisual(player);
		});
		prevPlayerBtn.setPressedAction(() -> {
			Features.inst.getAudio().play(SfxTypes.REGULAR_PRESS);
			currentPlayerIndex--;
			if (currentPlayerIndex < 0)
				currentPlayerIndex = replayData.players().size() - 1;
			var player = replayData.players().get(currentPlayerIndex);
			updateVisual(player);
		});

		float w = Window.WIDTH * .3f;
		float h = Window.HEIGHT * .98f;
		float x = (Window.WIDTH - w) * .95f;
		float y = (Window.HEIGHT - h) / 2f;
		rightStuff = new UIScrollable(Scenes.REPLAYLIST, x, y, w, h);
		rightStuff.setScrollable(false);
		rightStuff.rowHeightBased = 28f;
	}

	private void redrawVisual() {
		
		rightStuff.clear();
		rightStuff.addText(btnsRow);
		rightStuff.addText(new UIRow(new IUIObject[] { new UILabel(" - " + replayData.update()), playerLabel, }, 0));
		rightStuff.addText(" - " + replayData.gm().getName());
		rightStuff.addText(" ======================================== ", Nuklear.NK_TEXT_ALIGN_CENTERED);
		rightStuff.addText(upgradesVisualization.getViewedPlayer().getPlayerWinHistoryInfo());
		
//		if (lonnnng < 60) {
//			rightStuff.addText(new UIRow(new IUIObject[] { new UILabel("100 km:    ", Nuklear.NK_TEXT_ALIGN_RIGHT),
//					new UILabel((AI.calculateRace(upgradesVisualization.getViewedPlayer().car, 100000) / 1000d) + " sec") },
//					0));
//			}
//		rightStuff.addText(new UIRow(
//				new IUIObject[] { new UILabel("100 km:    ", Nuklear.NK_TEXT_ALIGN_RIGHT), new UILabel(
//						(AI.calculateRace(upgradesVisualization.getViewedPlayer().car, 100000) / 1000d) + " sec") },
//				0));
	}

//	@SuppressWarnings("removal")
	public static IUIObject[] carInfoAICalc(IUIObject[] objs, Car car) {
		var res = new IUIObject[objs.length + 4];
		for (int i = 0; i < objs.length; i++) {
			res[i] = objs[i];
		}
		
//		if (benchmarkThread != null && benchmarkThread.isAlive()) {
//			benchmarkThread.stop();
//		}
//		benchmarkThread = new Thread(() -> {
//			try {
				res[objs.length] = new UIRow(new IUIObject[] { new UILabel("Benchmarks:    240 m:    ", Nuklear.NK_TEXT_ALIGN_RIGHT),
						new UILabel((AI.calculateRace(car, 240) / 1000d)
								+ " sec") },
						0);
				res[objs.length + 1] = new UIRow(new IUIObject[] { new UILabel("720 m:    ", Nuklear.NK_TEXT_ALIGN_RIGHT),
						new UILabel((AI.calculateRace(car, 720) / 1000d)
								+ " sec") },
						0);
				res[objs.length + 2] = new UIRow(new IUIObject[] { new UILabel("3 km:    ", Nuklear.NK_TEXT_ALIGN_RIGHT),
						new UILabel((AI.calculateRace(car, 3000) / 1000d)
								+ " sec") },
						0);
				var lonnnng = (AI.calculateRace(car, 12000) / 1000d);
				res[objs.length + 3] = new UIRow(new IUIObject[] { new UILabel("12 km:    ", Nuklear.NK_TEXT_ALIGN_RIGHT),
						new UILabel(lonnnng + " sec") }, 0);
//			} catch (ThreadDeath e) {
//			}
//		});
//		benchmarkThread.start();
		return res;
	}

	private void updateVisual(Player player) {
		upgradesVisualization.viewPlayerId = Byte.MIN_VALUE;
		upgradesVisualization.viewPlayer(player, true, false);
		upgradesVisualization.updateGenerally(null);
		redrawVisual();
		for (var btn : btnsRow.row) {
			if (btn instanceof IUIPressable pressable)
				UISceneInfo.addPressableToScene(Scenes.REPLAYLIST, pressable);
		}
		playerLabel.setText("Player: " + (currentPlayerIndex + 1) + "/" + replayData.players().size() + "  ");
		upgradesVisualization.regValsTooltipRender = true;
	}

	public ReplayData loadReplay(List<String> lines) {
		String update = null;
		GameMode gm = null;
		Player currentPlayer = null;
		var players = new ArrayList<Player>();

		try {
			for (var line : lines) {
				if (line.startsWith(gmPrefix)) {
					line = line.substring(gmPrefix.length());

					var splitLine = line.split(Translator.split);
					int ordinalNum = Integer.parseInt(splitLine[0]);
					gm = GameInfo.createGameMode(GameModes.values()[ordinalNum], -1);
					gm.setGeneralInfo(splitLine, new AtomicInteger(1));
				} else if (line.startsWith(updatePrefix)) {
					// update
					update = line.substring(updatePrefix.length());
				} else if (line.startsWith(playerPrefix)) {
					currentPlayer = new Player();
					players.add(currentPlayer);
				} else {
					if (currentPlayer != null) {
						currentPlayer.addHistoryClean(line);
					}
				}
			}

			for (var player : players) {
				player.historyBackHome();
				player.role = Player.COMMENTATOR;
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
			return null;
		}

		return new ReplayData(update, gm, players);
	}

	public boolean loadReplay(File file) {
		if (file == null)
			return false;
		// maybe gamemode information, initial player inf, then for each line is a
		// dection with a indicator or something like "Name: " and "Choice: "
		// and then in the end it can be compressed I guess.
		List<String> lines;
		try {
			lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			return false;
		}

		var replay = loadReplay(lines);
		if (replay == null || replay.update() == null || replay.gm() == null || replay.players().size() < 1)
			return false;
		replayData = replay;

		if (upgradesVisualization != null) {
			upgradesVisualization.add(Scenes.CURRENT, this::redrawVisual);
			com = new GameInfo(null, GameType.NONE);
			upgradesVisualization.setCom(com);
			currentPlayerIndex = 0;
			updateVisual(replay.players().get(0));
			nextPlayerBtn.setEnabled(replay.players().size() > 1);
			prevPlayerBtn.setEnabled(replay.players().size() > 1);
		}
		// do stuff
		return true;
	}

	public static String createContent(GameMode gm, Player[] players) {
		var fileContents = new StringBuilder();
		fileContents.append(updatePrefix).append(Main.VERSION).append("\n");
		fileContents.append(gmPrefix).append(gm.getGameModeEnum().ordinal()).append(Translator.split)
				.append(gm.getGeneralInfo()).append("\n");
		for (var player : players) {
			if (player == null)
				continue;
			fileContents.append(playerPrefix).append("\n");
			var testPlayer = new Player(); 
			for (var choice : player.getHistory()) {
				if (choice == null)
					continue;
				
				try {
					Translator.setCloneString(testPlayer, Player.getCleanedHistory(choice).cloneString());
				} catch (Exception e) {
					System.out.println(e.getMessage());
					for (var str : e.getStackTrace()) {
						System.out.println("\t at " + str.toString());
					}
					continue;
				}
				
				fileContents.append(choice).append("\n");
			}
		}

		// compress losslessly

		return fileContents.toString();
	}

	public static File getReplayFolder() {
		var folder = new File(folderName);
		if (!folder.exists()) {
			if (!folder.mkdir()) {
				return null;
			}
		}
		return folder;
	}

	public static File saveReplay(GameMode gm, Player[] players) {
		var now = LocalDateTime.now();
		var dtf = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH.mm");
		String timestamp = dtf.format(now);

		if (getReplayFolder() == null) {
			return null;
		}

		var fileContents = createContent(gm, players);
//        LZ4.LZ4_compress_default();
//        file.

		var sb = new StringBuilder().append(gm.getNameFull()).append(" ").append(timestamp);
		if (players.length > 1) {
			for (int i = 0; i < players.length; i++) {
				if (players[i] == null)
					continue;
				if (i == 0)
					sb.append(" (");
				else
					sb.append(", ");
				sb.append(players[i].name);
			}
			sb.append(")");
		}
        sb.append(".replay");

		var cantContainThese = new char[] { '\'', '/', ':', '*', '?', '"', '<', '>', '|', };
		var content = sb.toString();
		for (var badChar : cantContainThese) {
			content = content.replace(badChar, 'X');
		}

		File file = null;
		try {
			int n = 0;
			do {
				file = new File(folderName + "/" + content + (n != 0 ? String.valueOf(n) : ""));
				if (file.createNewFile()) break;
				else {
					if (n > 1000)
						return null;
					n++;
				}
			} while (true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			var pw = new PrintWriter(file);
			pw.print(fileContents);
			pw.flush();
			pw.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return file;
	}

	public static void saveReplay(GameInfo com) {
		boolean someChoices = false;
		var players = com.getPlayersIncludingLostOnes();
		for (var player : players) {
			if (player.getHistory().size() > 1) {
				someChoices = true;
				break;
			}
		}
		if (!someChoices) {
			return;
		}
		saveReplay(com.getGamemode(), players);
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		upgradesVisualization.updateGenerally(cam, args);
		currentPlayerIndex = 0;
	}

	@Override
	public void updateResolution() {
		upgradesVisualization.updateResolution();
	}

	@Override
	public void keyInput(int keycode, int action) {
		upgradesVisualization.keyInput(keycode, action);
		 if (action != GLFW.GLFW_RELEASE) {
			 if (keycode == GLFW.GLFW_KEY_J) {
				 nextPlayerBtn.runPressedAction();
             } else if (keycode == GLFW.GLFW_KEY_H) {
            	 prevPlayerBtn.runPressedAction();
             }
		 }
	}
	
	@Override
	public void controllerInput() {
		upgradesVisualization.controllerInput();
		if (!InputHandler.HOLDING) {
			if (InputHandler.BTN_A || InputHandler.BTN_BACK_TOP_RIGHT) {
				nextPlayerBtn.runPressedAction();
			} else if (InputHandler.BTN_X  || InputHandler.BTN_BACK_TOP_LEFT) {
				prevPlayerBtn.runPressedAction();
			}
		}
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		upgradesVisualization.mouseButtonInput(button, action, x, y);
		return false;
	}

	@Override
	public void mousePosInput(float x, float y) {
		upgradesVisualization.mousePosInput(x, y);
	}

	@Override
	public void mouseScrollInput(float x, float y) {
		upgradesVisualization.mouseScrollInput(x, y);
	}

	@Override
	public void tick(float delta) {
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		upgradesVisualization.renderGame(renderer, cam, window, delta);

		GL11.glClearColor(.15f, .50f, .30f, 1);
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		upgradesVisualization.renderUILayout(ctx, stack);
		
		rightStuff.rowHeightBased = 34f;

		Features.inst.pushBackgroundColor(ctx, UIColors.PAOLO_VERONESE_GREEN);
		rightStuff.setPadding(rightStuff.getWindow().width * .02f, rightStuff.getWindow().height * .02f);
		rightStuff.layout(ctx, stack);
		Features.inst.popBackgroundColor(ctx);
//		Nuklear.nk_buffer_clear(null);
	}

	public void setGoBackBtn(UIButton<?> gobackBtn) {
		btnsRow.row[0] = gobackBtn;
	}

	public void close() {
		if (com != null) 
			com.close();
		else
			GameInfo.exists = false;
	}
}
