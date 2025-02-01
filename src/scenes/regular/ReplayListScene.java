package scenes.regular;

import static org.lwjgl.nuklear.Nuklear.nk_end;
import static scenes.regular.ReplayVisual.folderName;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Consumer;

import engine.graphics.ui.modal.UIConfirmModal;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import audio.SfxTypes;
import engine.graphics.Renderer;
import engine.graphics.interactions.TransparentTopbar;
import engine.graphics.objects.Camera;
import engine.graphics.ui.IUIObject;
import engine.graphics.ui.UIButton;
import engine.graphics.ui.UIButtonLobby;
import engine.graphics.ui.UIColors;
import engine.graphics.ui.UILabel;
import engine.graphics.ui.UINavigationAction;
import engine.graphics.ui.UIScrollable;
import engine.graphics.ui.UIWindowInfo;
import engine.io.InputHandler;
import engine.io.Window;
import main.Features;
import main.ISelectedLobby;
import main.Texts;
import scenes.SceneHandler;
import scenes.Scenes;
import scenes.adt.Scene;

public class ReplayListScene extends Scene implements ISelectedLobby {

	private final ReplayVisual replay;
	private boolean viewingReplay;
	private final UIScrollable replayList, btnsWindow;
	private final UILabel title;
	private final UIButton<?> loadBtn = new UIButton<>("Load file"), 
			                  deleteBtn = new UIButton<>("Delete"),
			                  folderBtn = new UIButton<>("Open replay folder");
	private final UIWindowInfo titleWindow;
	private UIButtonLobby selectedReplay;
	private Consumer<UIButtonLobby> actionReplayBtn;
	private UIButton<?> gobackBtn;
	private UIButton<?> defaultBtn;
	private UIButton gobackVisualBtn;

	public ReplayListScene(TransparentTopbar topbar, ReplayVisual replay) {
		super(topbar, Scenes.REPLAYLIST);
		this.replay = replay;

		gobackBtn = new UIButton<>(Texts.gobackText);
		gobackBtn.setPressedAction(() -> {
			viewingReplay = false;
			audio.play(SfxTypes.REGULAR_PRESS);
			sceneChange.change(Scenes.MAIN_MENU, true);
		});
		gobackVisualBtn = new UIButton<>(Texts.gobackText);
		gobackVisualBtn.setPressedAction(() -> {
			viewingReplay = false;
			updateListState();
			replay.close();
			audio.play(SfxTypes.LEFT);
		});
		replay.setGoBackBtn(gobackVisualBtn);

		loadBtn.setPressedAction(() -> {
			if (selectedReplay == null)
				return;

			var filename = folderName + "/" + selectedReplay.getTitle();
			if (!filename.endsWith(".replay")) {
				filename += ".replay";
			}
			var file = new File(filename);

			if (replay.loadReplay(file)) {
				audio.play(SfxTypes.JOINED);
				viewingReplay = true;
			} else {
				audio.play(SfxTypes.REGULAR_PRESS);
				viewingReplay = false;
				SceneHandler.showMessage("Could not load file \"" + filename + "\"");
			}
		});

		deleteBtn.setPressedAction(() -> {
			if (selectedReplay == null)
				return;
			audio.play(SfxTypes.REGULAR_PRESS);

			UIConfirmModal.show("Are you sure you want to delete this replay?", () -> {
				audio.play(SfxTypes.REGULAR_PRESS);
				var filename = folderName + "/" + selectedReplay.getTitle();
				var file = new File(filename);
				var deleted = file.delete();
				if (deleted) {
					updateGenerally(null);
					setSelectedLobby(null);
					SceneHandler.showMessage("Deleted replay");
				} else
					SceneHandler.showMessage("Could not delete replay");
			});
		});
		deleteBtn.setColor(UIColors.COLORS[UIColors.DNF.ordinal()]);
		loadBtn.setColor(UIColors.COLORS[UIColors.WON.ordinal()]);
		
		folderBtn.setPressedAction(() -> {
			var folder = ReplayVisual.getReplayFolder();
			Desktop desktop = Desktop.getDesktop();
			try {
				desktop.open(folder);
			} catch (IOException e) {
				SceneHandler.showMessage("Could not open folder \"" + folder.getAbsolutePath() + "\"");
			}
		});

		float w = Window.WIDTH * .4f;
		float h = Window.HEIGHT * .6f;
		float x = (Window.WIDTH - 2f * w) / 4f;
		float y = (Window.HEIGHT - h) / 2f;
		replayList = new UIScrollable(sceneIndex, 1.5f * x, y, w, h);
		btnsWindow = new UIScrollable(sceneIndex, 2.5f * x + w, y, w, h);

		btnsWindow.addText(gobackBtn);
		btnsWindow.addText(folderBtn);
		btnsWindow.addText(loadBtn);
		btnsWindow.addText(deleteBtn);

		titleWindow = createWindow(1.5f * x, 0, 2f * w, y);
		title = new UILabel(Texts.replayText);
		title.setColor(UIColors.WHITE);

		actionReplayBtn = Features.inst.createSelectableBtnAction(loadBtn, this);

		UINavigationAction toList = () -> {
			var list = replayList.getList();
			var fromRows = replayList.getRowsFrom();
			if (fromRows >= list.size())
				fromRows = 0;
			return list.size() > 0 && list.get(fromRows) instanceof UIButton btn ? btn : null;
		};
		gobackBtn.setNavigations(toList, null, null, () -> {
			return folderBtn;
		});
		folderBtn.setNavigations(toList, () -> gobackBtn, () -> gobackBtn, () -> {
			if (loadBtn.isEnabled())
				return loadBtn;
			return null;
		});
		loadBtn.setNavigations(toList, () -> {
			if (!deleteBtn.isEnabled())
				return gobackBtn;
			return null;
		}, () -> folderBtn, () -> {
			if (deleteBtn.isEnabled())
				return deleteBtn;
			return gobackBtn;
		});
		deleteBtn.setNavigations(toList, () -> {
			if (!deleteBtn.isEnabled())
				return gobackBtn;
			return null;
		}, () -> {
			if (loadBtn.isEnabled())
				return loadBtn;
			return gobackBtn;
		}, () -> {
			if (!deleteBtn.isEnabled())
				return gobackBtn;
			return null;
		});

	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		updateListState();
		removePressables();
		for (var element : btnsWindow.getListArr()) {
			if (element instanceof UIButton<?> btn) {
				add(btn);
			}
		}

		var folder = ReplayVisual.getReplayFolder();
		if (folder == null) {
			return;
		}

		var files = Arrays.stream(folder.listFiles()).sorted(Comparator.comparing(File::lastModified).reversed())
				.toList();

		replayList.clear();
		for (var file : files) {
			if (file.isFile()) {
				var splittedPath = file.getPath().substring(folder.getPath().length() + 1);
				if (splittedPath.endsWith(".replay")) {
					splittedPath = splittedPath.substring(0, splittedPath.lastIndexOf('.'));
				}
				replayList.addText(replayBtn(splittedPath));
			}
		}
		replayList.addNavigationToScrollableList(sceneIndex, gobackBtn, false, true);

		var list = replayList.getList();
		defaultBtn = list.size() > 0 && list.get(0) instanceof UIButton btn ? btn : gobackBtn;
	}

	public void updateListState() {
		GL11.glClearColor(0.1f, 0.1f, 0.1f, 1);

		loadBtn.setEnabled(false);
		deleteBtn.setEnabled(false);
	}

	private IUIObject replayBtn(String splittedPath) {
		var btn = new UIButtonLobby(splittedPath);
		btn.setConsumerValue(btn);
		btn.setPressedAction(actionReplayBtn);
		add(btn);
		return btn;
	}

	@Override
	public void updateResolution() {
		btnsWindow.rowHeightOverrule = MainMenuScene.btnHeight;
		replayList.rowHeightOverrule = btnsWindow.rowHeightOverrule / 2f;
	}

	@Override
	public void tick(float delta) {
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		if (viewingReplay) {
			replay.renderGame(renderer, cam, window, delta);
		}
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		if (viewingReplay) {
			replay.renderUILayout(ctx, stack);
		} else {
			Nuklear.nk_style_push_font(ctx, Window.titleFont.getFont());
			if (titleWindow.begin(ctx)) {
				Nuklear.nk_layout_row_dynamic(ctx, titleWindow.height, 1);
				title.layout(ctx, stack);
			}
			nk_end(ctx);
			Nuklear.nk_style_pop_font(ctx);

			replayList.layout(ctx, stack);
			btnsWindow.layout(ctx, stack);
		}
	}

	@Override
	public void keyInput(int keycode, int action) {
		if (viewingReplay) {
			if (keycode == GLFW.GLFW_KEY_ESCAPE) {
				gobackVisualBtn.runPressedAction();
				return;
			}
			replay.keyInput(keycode, action);
		} else {
			if (action == GLFW.GLFW_PRESS) {
				if (keycode == GLFW.GLFW_KEY_ESCAPE) {
					gobackBtn.runPressedAction();
					return;
				}
				// Downstroke for quicker input
				generalHoveredButtonNavigation(defaultBtn, keycode);
			}
		}
	}

	@Override
	public void controllerInput() {
		if (viewingReplay) {
			if (InputHandler.BTN_B) {
				gobackVisualBtn.runPressedAction();
			} else {
				replay.controllerInput();
			}
		} else {
			generalHoveredButtonNavigationJoy(defaultBtn);
			if (InputHandler.BTN_B) {
				gobackBtn.runPressedAction();
			} else if (InputHandler.BTN_UP || InputHandler.BTN_DOWN) {
				replayList.showHovered();
			}

			if (InputHandler.LEFT_STICK_Y < -.5f || InputHandler.RIGHT_STICK_Y < -.5f) {
				replayList.getWindow().focus = true;
				replayList.scroll(1);
			} else if (InputHandler.LEFT_STICK_Y > .5f || InputHandler.RIGHT_STICK_Y > .5f) {
				replayList.getWindow().focus = true;
				replayList.scroll(-1);
			}
		}
	}

	@Override
	public void mouseScrollInput(float x, float y) {
		replayList.scroll(y);
		if (viewingReplay) {
			replay.mouseScrollInput(x, y);
		}
	}

	@Override
	public void mousePositionInput(float x, float y) {
		if (viewingReplay) {
			replay.mousePosInput(x, y);
		}
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		super.mouseButtonInput(button, action, x, y);
		if (viewingReplay) {
			replay.mouseButtonInput(button, action, x, y);
		}
		return false;
	}

	@Override
	public UIButtonLobby getSelectedLobby() {
		return selectedReplay;
	}

	@Override
	public void setSelectedLobby(UIButtonLobby btn) {
		if (selectedReplay != null) {
			selectedReplay.setColor(null);
		}

		selectedReplay = btn;
		deleteBtn.setEnabled(selectedReplay != null);
	}

	@Override
	public void destroy() {
	}

}
