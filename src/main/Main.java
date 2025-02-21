package main;

import static org.lwjgl.glfw.GLFW.glfwIconifyWindow;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.function.Consumer;

import com.codedisaster.steamworks.SteamAPI;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import adt.IAction;
import audio.AudioMaster;
import audio.SfxTypes;
import communication.GameType;
import engine.graphics.interactions.LobbyTopbar;
import engine.graphics.interactions.RegularTopbar;
import engine.graphics.interactions.TopbarInteraction;
import engine.graphics.interactions.TransparentTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.ui.Console;
import engine.graphics.ui.Font;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIFont;
import engine.graphics.ui.UISceneInfo;
import engine.graphics.ui.modal.UIConfirmModal;
import engine.graphics.ui.modal.UIUsernameModal;
import engine.graphics.Renderer;
import engine.graphics.UIRender;
import engine.io.InputHandler;
import engine.io.Window;
import engine.utils.Timer;
import main.steam.SecondaryThread;
import player_local.Player;
import player_local.upgrades.TileVisual;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Scene;
import scenes.game.Lobby;
import scenes.game.Race;
import scenes.regular.*;
import settings_and_logging.RSet;
import settings_and_logging.hotkeys.CurrentControls;

import javax.swing.*;

/*

MÃ¥l for Ã¥ fullfÃ¸re spillet:
Engine:
- Skygge
- instanced rendering
- Enklere Ã¥ lage normal animasjon
- Lettere Ã¥ flytte meshes uten Ã¥ mÃ¥tte update hele tiden og feil system
- Pixel-perfect rendering av ortho sprites.
- Same size pixel-art rendering av sprites slik at jeg kan lettere passe pÃ¥ at
  f. eks racelights og nos bottle har samme stÃ¸rrelse per tex-pixel.
- Dynamisk oppretting og rendering av UI elements (og i samme metode som all annen rendering)
  som skal skje statisk men ikke helt pÃ¥ samme mÃ¥te som nuklear, mer grid based.
  Men poenget er Ã¥ kunne lettere utnytte hot-reloading og Ã¥ legge til ting uten sÃ¥ mye stress.
- Textured buttons.
- StÃ¸tte opp til 100 spillere, men bare tillat 16 for Ã¥ vÃ¦re sikker.
- Fikse og rydde opp i RegVals
- Bytte RegVals double til RegVal
- Bytte Rep stats double til InfVal
- Blink tile rÃ¸d om du prÃ¸ver Ã¥ plassere den et sted som ikke gÃ¥r an slik som i aoe
Racing changes:
- 3D bil man kjÃ¸rer (Kanskje endre til 3rd person da jeg kan lettere legge til nye biler)
- Hus, tunneler, broer, bedre bakgrunn
- VÃ¦r
- Modifiserende lyd pÃ¥ motor
- Risting av gui elements
- Bedre grafikk ved sprites som tachometer
- Motion blur ved hÃ¸yere fart
- Burnout scene
- Ha tekst i fjeset ditt litt ala chicken dinner som sier lengde osv og om det er siste runde sÃ¥ "Match Point"
Lobby changes:
- Kjekkere bakgrunn slik som Wingspan ved oppgradering og kosligere farger og tekstur pÃ¥ layer tiles som en Ã¸y ute pÃ¥ havet elns
  og vis bedre til brukeren at her gÃ¥r det ogsÃ¥ an Ã¥ plassere ting.
- Tilfeldig figur av map slik at det blir mer replayable.
- Fuel/Oil i bakken?
- Ha 3 gullbiter som kan spawne hvor som helst og oppÃ¥ enannen. For hver slik pÃ¥ en tile sÃ¥ er det -$5.
- Bonuser som knapper pÃ¥ et nytt vindu til venstre
- FÃ¥ tilbake sale bare at det er for hver tile
- Nos refill?
- Nummer animasjon som viser income etter hver runde
- Lasting av replay som lagrer alt som vises pÃ¥ slutten av en kamp og selve racingen og gamemode settings.
Tools:
Kanskje heller ha at etter 4n+8 improvements sÃ¥ kan du kjÃ¸pe unlock av en tool og sÃ¥ gÃ¥r prisen opp pÃ¥ de individuelt slik som ved vanlige tiles?
- En som gjÃ¸r at salg gir tilbake 100%
- En som gjÃ¸r at det blir rabatt pÃ¥ nabo tiles (-1% per runde elns med en maks -50%) og gir +10 maxlvl
- En som kan bare plasseres pÃ¥ kanten av kartet og legger til nye tiles der. De forblir om du selger den igjen,
  men om du lar vÃ¦r Ã¥ selge den sÃ¥ vil den fortsette som en timesmod planter pÃ¥ de nye tilsa.
- En som "graver" i timesmod per runde og gir inntekt basert pÃ¥ hvor hÃ¸y timesmoden er. Den graver like mye som en planter
  sÃ¥ 0.2x kanskje?
- En som Ã¸ker styrken pÃ¥ nabo tools med 10% per runde elns.
- En som oversetter la oss si kw til mpt slik som merchant? Kanskje ogsÃ¥ at den er basert pÃ¥ avstand til neste merchant.
Options:
- Kunne endre hotkeys
- Automatisk oppdatering av volum da folk blir hele tiden forvirret
- Dynamisk GameMode funksjonalitet + visualisering
- AI + generert campaign
 */
public class Main {

	static class MyOutputStream extends OutputStream {
		private final Consumer<String> consumer;
		private final StringBuffer stringBuffer = new StringBuffer();

		public MyOutputStream(Consumer<String> consumer) {
			this.consumer = consumer;
		}

		@Override
		public void write(int b) {
			stringBuffer.append((char) b);
		}

		@Override
		public void flush() {
			if (!stringBuffer.isEmpty()) {
				consumer.accept(stringBuffer.toString());
				stringBuffer.delete(0, stringBuffer.length());
			}
		}
	}

	public static final boolean RELEASE
	= true
//	= false
	, STEAM = false
	, NO_SOUND
//	= true
	= false
	, TEST_GRAPHICS
//	= true
	= false
	, DEMO
//	= true;
	= false;
	public static boolean DEBUG = !RELEASE;

	public static final String NAME = "Racingmaybe" + (DEMO ? "DEMO" : "") + " | ", VERSION = "Update 49";
	public static final int TICK_STD = 25;
	public static int TARGET_FPS;
	public static long OPTIMAL_TIME;

	public static IAction hoverAction;

	private AudioMaster audio;
	private SceneHandler sceneHandler;
	private Timer timer;
	private Renderer renderer;
	private UIRender ui;

	private final Window window;
	private SecondaryThread secondaryThread;
	private static final StringBuilder error = new StringBuilder();
	public static boolean running;
	private boolean confirmedExit;
	public static UIFont standardFont;
	private static boolean showedErrorMessage = false, donePrinting = false;

	public static void main(String[] args) {
		long startuptime = System.currentTimeMillis();
		System.setProperty("org.lwjgl.librarypath", new File("natives").getAbsolutePath());
		if (DEBUG) {
//			System.setProperty("org.lwjgl.util.Debug", "true");
//			System.setProperty("org.lwjgl.util.DebugFunctions", "true");
//			System.setProperty("org.lwjgl.util.DebugFunctions", "true");
//			System.setProperty("org.lwjgl.util.DebugAllocator", "true");
//			System.setProperty("org.lwjgl.util.DebugAllocator.internal", "true");
//			System.setProperty("org.lwjgl.util.DebugStack", "true");
		}
		new Main(startuptime);

		if (showedErrorMessage) {
			while (!donePrinting) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException ignored) {
				}
			}

			showErrorMessage();
		}

		Runtime.getRuntime().halt(0);
		System.exit(0);
	}

	public Main(long startuptime) {
		PrintStream logFile = null;
		window = new Window();
		InputHandler input = null;
		try {
//			if (RELEASE) {
//
//				try {
//					String logname = "errorLog_" + VERSION;
//					File file = new File(logname + ".txt");
//					if (!file.exists())
//						file.createNewFile();
//					else {
//						int i = 2;
//						do {
//							file = new File(logname + "_" + i + ".txt");
//							i++;
//						} while (file.exists() && file.length() > 0);
//					}
//					PrintStream errFile = new PrintStream(file);
//					logFile = errFile;
//					System.setOut(new PrintStream(new MyOutputStream(errFile::print), true));
//					System.setErr(new PrintStream(new MyOutputStream(str -> {
//						errFile.print(str);
//						errFile.flush();
////						if (error.isEmpty())
////							error.append("Please validate your game. An error occured! (・_・ヾ\n\n");
////						error.append(str);
////						if (!running && !showedErrorMessage) {
////							showedErrorMessage = true;
////							new Thread(() -> {
////								try {
////									Thread.sleep(500);
////								} catch (InterruptedException e) {
////								}
////								errFile.close();
////								donePrinting = true;
////							}).start();
////						}
//					}), true));
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}

			timer = new Timer();

			// 250-300 ms

			if (!Main.NO_SOUND) {
				audio = new AudioMaster();
			} else {
				audio = null;
			}
			new Features(audio, window);
			window.init();

			ResourceHandler.init();
			Texts.init();

			// Buttons
			hoverAction = () -> audio.play(SfxTypes.REGULAR_HOVER);
			UIButton<?> minimizeButton = new UIButton<>("-");
			minimizeButton.setPressedAction(() -> {
				audio.play(SfxTypes.REGULAR_PRESS);
				glfwIconifyWindow(window.getWindow());
			});

			UIButton<?> closeButton = new UIButton<>("X");
			closeButton.setPressedAction(() -> {
				audio.play(SfxTypes.REGULAR_PRESS);
				GLFW.glfwSetWindowShouldClose(window.getWindow(), true);
			});

			UISceneInfo.addPressableToScene(Scenes.GENERAL_NONSCENE, minimizeButton);
			UISceneInfo.addPressableToScene(Scenes.GENERAL_NONSCENE, closeButton);

			/*
			 * Join modal
			 */
			UIUsernameModal usernameModal = new UIUsernameModal(audio);
			UIConfirmModal confirmModal = new UIConfirmModal();

			sceneHandler = new SceneHandler(confirmModal, usernameModal, !RELEASE ? new Console() : null, window);

			ui = new UIRender();

			input = new InputHandler(window, ui.getNkContext());
			input.setCurrent(sceneHandler);
			renderer = new Renderer(ui);

			// finally set the font as gl has been init
			standardFont = new UIFont(Font.REGULAR, (int) (TileVisual.size() / 3.64f)); // Window.HEIGHT / 42);
			standardFont.use(ui.getNkContext());

			/*
			 * Topbars
			 */
			TopbarInteraction topbar = new TopbarInteraction(window);

			RegularTopbar regularTopbar = new RegularTopbar(minimizeButton, closeButton, topbar);
			LobbyTopbar lobbyTopbar = new LobbyTopbar(audio, new TopbarInteraction(window));
			TransparentTopbar transparentTopbar = new TransparentTopbar(topbar, 18);

			/*
			 * Global exitmodal
			 */
			Consumer<GameType> initMovingIntoALobby = (type) -> {
				audio.play(SfxTypes.REGULAR_PRESS);
				if (type.isSinglePlayer()
						&& (Features.inst.getSelectedLobby() == null || !Features.inst.getSelectedLobby().isSelected()))
					return;

				usernameModal.show(type);
				usernameModal.setStandardInputText(Features.inst.getUsername());
			};

			confirmModal.setCancelAction(() -> {
				audio.play(SfxTypes.REGULAR_PRESS);
				sceneHandler.getCurrentScene().press();
			});

			long scenesNow = System.currentTimeMillis();
			System.out.println("Before scenes: " + (scenesNow - startuptime) + "ms");
			/*
			 * Scenes
			 */
			var scenes = new Scene[Scenes.AMOUNT - 1];
			scenes[Scenes.RACE] = new Race(transparentTopbar, lobbyTopbar);
			scenes[Scenes.LOBBY] = new Lobby((Race) scenes[Scenes.RACE], lobbyTopbar);

			Consumer<Integer> createNewSingleplayerGameAction = (challengeLevel) -> {
				((Lobby) scenes[Scenes.LOBBY]).createNewLobby("Player", Player.HOST, GameType.SINGLEPLAYER,
						challengeLevel);
				((LeaderboardScene) scenes[Scenes.LEADERBOARD]).setLeaderboard(challengeLevel);
				audio.play(SfxTypes.START_ENGINE);
			};
			
			IAction countdownAction = () -> {
				Lobby lobby = (Lobby) scenes[Scenes.LOBBY];
				if (lobby.getGame().inLobby && !lobby.getCom().isSingleplayer()) {
					lobby.countdown();
				}
			};

			CurrentControls controls = CurrentControls.getInstance();

			if (!TEST_GRAPHICS) {
				scenes[Scenes.MAIN_MENU] = new MainMenuScene(createNewSingleplayerGameAction, regularTopbar);
				scenes[Scenes.SINGLEPLAYER] = new SingleplayerScene(createNewSingleplayerGameAction, regularTopbar);
				((Race) scenes[Scenes.RACE]).createTryAgainButton(createNewSingleplayerGameAction,
						() -> {
							sceneHandler.changeScene(Scenes.LOBBY, false);
						});
				var replay = new ReplayVisual(((Lobby) scenes[Scenes.LOBBY]).getUpgradesSubscene());
				scenes[Scenes.REPLAYLIST] = new ReplayListScene(transparentTopbar, replay);
				scenes[Scenes.OPTIONS] = new OptionsScene(regularTopbar, countdownAction);
				scenes[Scenes.HOTKEY_OPTIONS] = new HotkeysScene(regularTopbar, controls);
				scenes[Scenes.DESIGNER_NOTES] = new DesignerNotesScene(regularTopbar, countdownAction);
				scenes[Scenes.JOINING] = new JoiningScene((Lobby) scenes[Scenes.LOBBY], transparentTopbar);
				scenes[Scenes.MULTIPLAYER] = new MultiplayerScene(regularTopbar, initMovingIntoALobby);
				scenes[Scenes.LEADERBOARD] = new LeaderboardScene(regularTopbar,
						() -> ((SingleplayerScene) scenes[Scenes.SINGLEPLAYER]).doneLoading());
				((Race) scenes[Scenes.RACE]).setLobby((Lobby) scenes[Scenes.LOBBY]);

				usernameModal.setButtonActions((Lobby) scenes[Scenes.LOBBY]);
				Features.inst.setLobby((Lobby) scenes[Scenes.LOBBY]);
				Features.inst.setCloseUsernameModalAction(() -> {
					usernameModal.hide();
					if (Scenes.CURRENT < Scenes.LOBBY) {
						scenes[Scenes.JOINING].updateGenerally(null);
						sceneHandler.changeScene(Scenes.JOINING, false);
					}
				});
			} else {
				scenes[Scenes.TESTING] = new TestingScene(transparentTopbar);
			}

			window.setSceneHandler(sceneHandler);

			System.out.println("Through scenes: " + (System.currentTimeMillis() - scenesNow) + "ms");


			secondaryThread = null;

			timer.init();
			long shaderCreationNow = System.currentTimeMillis();
			while (ResourceHandler.isNotDone()) {
				ResourceHandler.createNext();
			}
			ResourceHandler.destroy();
			sceneHandler.init(scenes, audio, createNewSingleplayerGameAction, input);
			sceneHandler.updateResolution();

			System.out.println("ShaderCreationNow: " + (System.currentTimeMillis() - shaderCreationNow) + "ms");

		} catch (Exception e) {
			e.printStackTrace();
			return;
		}

		/*
		 * Show the window until it is closed
		 */
		window.delayRendering();
		glfwShowWindow(window.getWindow());
		running = true;
		System.out.println("Startup: " + (System.currentTimeMillis() - startuptime) + "ms");
		if (!TEST_GRAPHICS) {
			((OptionsScene) sceneHandler.getScene(Scenes.OPTIONS)).initOptions(audio, sceneHandler);
		}
		gameLoop();

		/*
		 * After gameloops ends
		 */
		if (STEAM)
			secondaryThread.destroy();
		window.destroy();
		if (audio != null)
			audio.destroy();
		sceneHandler.destroy();

		if (logFile != null) {
			logFile.flush();
			logFile.close();
		}
	}

	private static void showErrorMessage() {
		try {
			error.append(
					"\n\nPlease send me an email at jhoffiscreates@gmail.com\nYou can find the errors in a .txt in the local files");
			JOptionPane.showMessageDialog(null, error.toString());
		} catch (StringIndexOutOfBoundsException e) {
		}
	}

	private void gameLoop() {
		while (running) {
			if (window.isClosing()) {
				if (!confirmedExit && window.isFocused()) {
					audio.play(SfxTypes.REGULAR_PRESS);
					UIConfirmModal.show(Texts.exitLabelText, () -> {
						Features.inst.leave();
						glfwSetWindowShouldClose(window.getWindow(), true);
						confirmedExit = true;
					});
					UISceneInfo.decideFocusedWindow(window.getWindow());
					window.mouseStateHide(false);
					GLFW.glfwSetWindowShouldClose(window.getWindow(), false);
				} else if (Scenes.CURRENT < Scenes.LOBBY
						|| ((Lobby) sceneHandler.getScene(Scenes.LOBBY)).getGame().leaveGame) {
					running = false;
					return;
				} else {
					((Lobby) sceneHandler.getScene(Scenes.LOBBY)).getGame().leaveGame = true;
				}
			} else if (window.shouldUpdateViewport()) {
				window.updateViewport();
			} else if (!error.isEmpty()) {
				running = false;
				showErrorMessage();
				return;
			}

			if (window.updatingWindowTime >= System.currentTimeMillis())
				continue;

			window.update();
			audio.checkMusic();

			long timeBeforeRun = System.nanoTime();

			float delta = timer.getDelta();
			// update game
			sceneHandler.tick(delta);
			Camera.updateViews();

			// draw the game
			render(delta);

			int err = GL11.glGetError();
			if (err != GL11.GL_NO_ERROR) {
				if (err == 1285) {
					if (RSet.getBool(RSet.vsync)) {
						RSet.set(RSet.vsync, false);
						((OptionsScene) sceneHandler.getScene(Scenes.OPTIONS)).updateVsync(false);
					} 
					if (RSet.getInt(RSet.clientFullscreen) == 2) {
						Features.inst.getWindow().setFullscreen(1);
						sceneHandler.updateResolution();
						((OptionsScene) sceneHandler.getScene(Scenes.OPTIONS)).updateFullscreen(1);
					}
				}
				System.out.println("GLERROR: " + err);
			}

			window.swapBuffers();

			if (OPTIMAL_TIME != 0) {
				try {
					double sleep = (double) (timeBeforeRun - System.nanoTime() + OPTIMAL_TIME) / 1_200_000d; // 1_315_000d;
					if (sleep >= 1d) {
						if (sleep > 1000)
							sleep = 1000;
						Thread.sleep((long) sleep);
					}
				} catch (InterruptedException e) {
				}
			}
		}
	}

	private void render(float delta) {
		sceneHandler.renderGame(renderer, null, window.getWindow(), delta);

		sceneHandler.renderUILayout(ui.getNkContext(), null);
		renderer.renderNuklear(ui.getNkContext());
	}

}
