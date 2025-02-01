
package scenes.game.racing_subscenes;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import engine.graphics.ui.*;
import engine.io.InputHandler;
import engine.io.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import adt.IAction;
import audio.CarAudio;
import engine.graphics.Renderer;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Sprite;
import engine.math.Vec3;
import main.Features;
import main.ResourceHandler;
import player_local.car.Car;
import player_local.car.CarModel;
import scenes.Scenes;
import scenes.adt.Visual;

import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

public class FinishVisual extends Visual {

	private Camera perspCamera;
	private final float camRotY = -145f;
	private List<Car> finishedPlayers;
	private IUIHasWindow raceLobbyLabel;
	public final float maxDistance = 150;
	private float deleteDistance;
	private Sprite finishBack, finishBackTooFast, finishBackWayTooFast;
	private final Vec3 camPosAtFinishLine = new Vec3(-1f, 0.5f, -3.9f);
	private IAction actionAfterAllFinishCars;

	private boolean winnerWinnerChickenDinner, turnedCam;
	private String winnerText;
	private UIColors winnerTextColor;
	private float winnerAlpha;
	private UIWindowInfo winnerWindow;
	private boolean winnerCenter;
	private boolean wentTooFast, wentWayTooFast;
	public final UIScrollable moneyExplaination;
	public final int lostDistance = 20;
	private boolean spLose;

	public FinishVisual(IUIHasWindow raceLobbyLabel) {

		perspCamera = new Camera(new Vec3(-1f, 0.5f, -3.9f), new Vec3(-0.6f, -145f, 0), 70f, 0.1f, 1000f, 16f / 9f);

		this.raceLobbyLabel = raceLobbyLabel;
		finishedPlayers = new CopyOnWriteArrayList<Car>();

		ResourceHandler.LoadSprite("./images/finishBack.png", "background", (sprite) -> finishBack = sprite.setToFullscreen());
		ResourceHandler.LoadSprite("./images/finishBackTooFast.png", "background",
				(sprite) -> finishBackTooFast = sprite.setToFullscreen());
		ResourceHandler.LoadSprite("./images/finishBackWayTooFast.png", "background",
				(sprite) -> finishBackWayTooFast = sprite.setToFullscreen());
		
		winnerWindow = UISceneInfo.createWindowInfo(Scenes.RACE, 0, 0, Window.WIDTH, Window.HEIGHT * .2);
		moneyExplaination = new UIScrollable(Scenes.RACE, Window.WIDTH * 0.7f, Window.HEIGHT * 0.1f,
				Window.WIDTH * 0.1f, Window.HEIGHT * 0.3f);
	}

	@Override
	public void updateGenerally(Camera cam, int... args) {
		if (cam == null)
			return;
		var pos = cam.getPosition();
		pos.x = camPosAtFinishLine.x;
		pos.y = camPosAtFinishLine.y;
		pos.z = camPosAtFinishLine.z;
		var rot = cam.getRotation();
		rot.x = -0.6f;
		rot.y = -145f;
		rot.z = 0f;
		goBackBtn.setColor(null);
	}

	public Vec3 getCameraPosition() {
		return camPosAtFinishLine;
	}

	@Override
	public void updateResolution() {
	}

	public void clear() {
		finishedPlayers.clear();
		actionAfterAllFinishCars = null;
	}

	public void init() {
		clear();
		wentTooFast = false;
		winnerText = "";
	}

	public void keyInput(int keycode, int action) {
//		camera.move(keycode, action);
//		System.out.println("pos: " + camera.getPosition().toString());
		if (action == GLFW.GLFW_RELEASE) {

			if (keycode == GLFW.GLFW_KEY_ENTER || keycode == GLFW.GLFW_KEY_ESCAPE) {
				if (winnerWinnerChickenDinner) {
					for (var car : finishedPlayers) {
						car.getModel().addPositionDistance(-10000);
					}
				} else
					goBackBtn.runPressedAction();
			}
		}
	}

	@Override
	public void controllerInput() {
		if (!InputHandler.HOLDING && (InputHandler.BTN_A || InputHandler.BTN_B)) {
			keyInput(GLFW.GLFW_KEY_ENTER, GLFW.GLFW_RELEASE);
		}
	}

	@Override
	public void mouseScrollInput(float x, float y) {
	}

	@Override
	public boolean mouseButtonInput(int button, int action, float x, float y) {
		return false;
	}

	@Override
	public void mousePosInput(float x, float y) {
	}

	@Override
	public void tick(float delta) {
		for (int i = 0; i < finishedPlayers.size(); i++) {
			Car car = finishedPlayers.get(i);
			CarModel model = car.getModel();
			CarAudio ca = car.getAudio();
			if (spLose) {
//				delta *= 0.8;
				if (i % 2 == 0)
					model.setRotationZ(model.getModel().rotation().z + (2 + Features.ran.nextInt(25)) * delta);
				else
					model.setRotationZ(0);

				var spd = turnedCam ? 120 : 30;
				if (spd <= 1) {
					model.getSpeeds().clear();
				} else {
					model.addSpeed(spd);
				}
//				System.out.println(spd);

			}
			var speed = model.getSpeed();

			if (model.getPositionDistance() < deleteDistance) {
				if (actionAfterAllFinishCars != null) {
					Features.inst.getWindow().mouseStateHide(false);

					for (int a = 0; a < finishedPlayers.size(); a++) {
						finishedPlayers.remove(finishedPlayers.get(a));
						model.reset(); // problem om modellen er den samme her.
						ca.reset();
						ca.delete();
					}
					actionAfterAllFinishCars.run();
					break;
				}
				finishedPlayers.remove(finishedPlayers.get(i));
				model.reset(); // problem om modellen er den samme her.
				ca.reset();

				addButNotFinished(speed);
				continue;
			}

			model.addPositionDistance(-speed / 32f * delta);
			model.setPositionToModel(0);
			model.rotateWheels(speed * delta);

			if (!ca.getMotor().isPlaying()) {
				ca.motorPitch(0.9f, 1, 2, 1);
				ca.turbospoolPitch(1, 200, 1, 1, 1);
				ca.motorAcc(car.hasTurbo());
				ca.getMotor().velocity(speed / 2.2f, 0, 0);
			}

			Vec3 carPos = new Vec3(model.getModel().position());
			if (winnerWinnerChickenDinner && !turnedCam && carPos.x < -20) {
				turnedCam = true;
				perspCamera.getRotation().y = -camRotY - 20;

				for (int mi = 0; mi < finishedPlayers.size(); mi++) {
					var m = finishedPlayers.get(mi).getModel();
					m.addPositionDistance(50 + lostDistance * mi);
					m.getModel().reset();
					m.setPositionToModel(0);
				}
			}

			carPos.invert();
			ca.setMotorPosition(carPos);
		}
		perspCamera.update();

		if (winnerWinnerChickenDinner && winnerAlpha < 1f && finishedPlayers.size() > 0
				&& finishedPlayers.get(0).getModel().getPositionDistance() < maxDistance * .9) {
			winnerAlpha = 1f;
		}
	}

	@Override
	public void renderGame(Renderer renderer, Camera cam, long window, float delta) {
		for (Car car : finishedPlayers) {
			car.renderCar(renderer, perspCamera);
		}
		if (!winnerWinnerChickenDinner) {
			finishBack.setPositionZ(-1f);
			renderer.renderOrthoMesh(finishBack);
			if (wentTooFast) {
				finishBackTooFast.setPositionZ(1f);
				renderer.renderOrthoMesh(finishBackTooFast);
			}
			if (wentWayTooFast) {
				finishBackWayTooFast.setPositionZ(1f);
				renderer.renderOrthoMesh(finishBackWayTooFast);
			}
		} else {
			GL11.glClearColor(0, 0, 0, 0);
		}
	}

	@Override
	public void renderUILayout(NkContext ctx, MemoryStack stack) {
		if (!winnerText.isEmpty()) {
			winnerWindow.z = 0;
			float padding = winnerWindow.height * .1f;
			if (winnerWindow.begin(ctx, stack, padding, padding, 0, 0)) {
				Features.inst.pushFontColor(ctx, UIColors.WHITE);
				Nuklear.nk_style_push_font(ctx, Window.hugeTitleFont.getFont());

				nk_layout_row_dynamic(ctx, winnerWindow.height, 1);
				UILabel.render(ctx, winnerText, winnerTextColor,
						winnerCenter ? Nuklear.NK_TEXT_ALIGN_CENTERED | Nuklear.NK_TEXT_ALIGN_MIDDLE
								: Nuklear.NK_TEXT_ALIGN_LEFT,
						winnerAlpha);

				Nuklear.nk_style_pop_font(ctx);
				Features.inst.popFontColor(ctx);
			}
			Nuklear.nk_end(ctx);
			if (winnerWinnerChickenDinner)
				return;
		}
		Features.inst.pushBackgroundColor(ctx, UIColors.WHITE, 0.8f);
		raceLobbyLabel.layout(ctx, stack);
		if (moneyExplaination.getListArr().length != 0) {
			var moneyWidth = .15f * Window.WIDTH;
			moneyExplaination.setPadding(.02f * moneyWidth, .02f * moneyWidth);
			moneyExplaination.getWindow().setPositionSize(raceLobbyLabel.getWindow().x - 1.1f * moneyWidth,
					raceLobbyLabel.getWindow().y, moneyWidth,
					Window.HEIGHT * (0.03667f * moneyExplaination.getListArr().length));
			moneyExplaination.layout(ctx, stack);
		}
		Features.inst.popBackgroundColor(ctx);
		
		float marginY = raceLobbyLabel.getWindow().height * .05f;
		float x = raceLobbyLabel.getWindow().x;
		float y = raceLobbyLabel.getWindow().y + raceLobbyLabel.getWindow().height + marginY;
		float w = raceLobbyLabel.getWindow().width;
		float h = Window.HEIGHT - y - marginY;
		h /= 1.66f;
		
		goBackLayout(ctx, stack, x, y, w, h);

//		goBackLayout(ctx, stack, raceLobbyLabel.getWindow().x,
//				raceLobbyLabel.getWindow().y + raceLobbyLabel.getWindow().height * 1.05f,
//				raceLobbyLabel.getWindow().width, raceLobbyLabel.getWindow().width / 4);
	}

	@Override
	public boolean hasAnimationsRunning() {
		return !finishedPlayers.isEmpty();
	}

	public void addFinish(Car car) {
		addFinish(car, maxDistance);
	}

	public void addFinish(Car car, float startPosition) {
		CarModel carModel = car.getModel();
		carModel.reset();
		carModel.setPositionDistance(startPosition);
		carModel.setPositionToModel(0);
		if (carModel.getSpeed() == 0) {
			carModel.addSpeed(car.getStats().finishSpeed);
		}
		carModel.setFinished(true);
		finishedPlayers.add(car);
		perspCamera.getRotation().y = camRotY;
		turnedCam = false;

	}

	public void setAfterAllFinishCars(IAction action) {
		actionAfterAllFinishCars = action;
	}

	public void clearCars() {
		finishedPlayers.clear();
	}

	public void winnerWinnerChickenDinner(boolean winMode, String winnerText, UIColors winnerTextColor, boolean center, float winnerDelete,
			boolean spLose) {
		winnerWinnerChickenDinner = winMode;
		this.spLose = spLose;
		setWinnerText(winnerText, winnerTextColor, center);
		winnerAlpha = 0;
		deleteDistance = winMode ? -maxDistance * 1.33f * winnerDelete : -maxDistance / 1.6f;

		winnerWindow.setPosition(winnerWindow.x, center ? Window.HEIGHT / 2.25f - winnerWindow.height / 2f : 0);

		if (winMode)
			Features.inst.getWindow().mouseStateHide(true);
	}
	
	public void setWinnerText(String winnerText, UIColors winnerTextColor, boolean center) {
		this.winnerText = winnerText;
		this.winnerTextColor = winnerTextColor;
		this.winnerCenter = center;
		winnerAlpha = 1;
	}

	public void addButNotFinished(int speed) {
		if (speed > 10000 && speed < 100000) {
			wentTooFast = true;
		} else if (speed >= 100000) {
			wentWayTooFast = true;
		}
	}


}