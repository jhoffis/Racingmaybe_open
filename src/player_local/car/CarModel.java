package player_local.car;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import engine.graphics.objects.Model;
import engine.math.Vec3;
import scenes.game.lobby_subscenes.CarChoiceSubscene;

public class CarModel {

	private Model model;

	private float positionX;
	private float positionZ;

	private double rotation;

	private float[] wheelRotation;

	private double rotationZ;

	private final ArrayList<Long> times = new ArrayList<>();
	private final ArrayList<Float> distances = new ArrayList<>();
	private final ArrayList<Integer> speeds = new ArrayList<>();
	private final ArrayList<Double> spdIncs = new ArrayList<>();

	private boolean finished;
	private final Queue<Float> mpsLast = new LinkedList<>();

	public boolean ai = false;
	public int timeGoal;

	// private int modelIndex;

	public static Model createModel(String carname) {
		Model car = new Model(carname + ".obj",
				new String[]{"./images/models/" + carname + "Paint.png",
						"./images/models/Tires.png"},
				"carexterior");

		car.create();
		return car;
	}

	public void reset() {
		positionX = 0;
		positionZ = 0;
		rotation = 0;
		rotationZ = 0;
		mpsLast.clear();
		distances.clear();
		speeds.clear();
		times.clear();
		finished = false;
		model.reset();
	}

	public List<Integer> getSpeeds() {
		return speeds;
	}

	public void addSpeed(int speed) {
		speeds.add(speed);
	}

	public void setPositionDistance(float positionX) {
		this.positionX = positionX;
	}

	public void addPositionDistance(double d) {
		positionX += d;
	}

	public void setPositionSide(float f) {
		positionZ = f;
	}

	public Model getModel() {
		return model;
	}

	public void setPositionToModel(double distanceFromMyCamera) {
		// model.reset();
		model.position().x = (float) (positionX - distanceFromMyCamera);
		model.position().z = positionZ;
		model.updateTransformation();
		model.rotation().x = 0;
		model.rotation().y = (float) rotation;
		model.rotation().z = (float) rotationZ;
	}

	public void setRotation(double rotation) {
		this.rotation = rotation;
	}

	public void setRotationZ(double rotationZ) {
		this.rotationZ = rotationZ;
	}

	/*
	 * // Det er helst bare loggføring utenom for siste tid. Kan brukes for
	 * replays?
	 */
	public boolean pushInformation(float distance, int speed, double spdinc,
			long time) {
		final int indexableSize = times.size() - 1;
		int indexTime = 0;
		for (int i = indexableSize; i >= 0; i--) {
			if (time >= times.get(i)) {
				indexTime = i + 1;
				break;
			}
		}

		// Don't save data when it is repeating what indicies around it contains
		if (indexableSize >= 0
				&& (
						(
							indexTime <= indexableSize
							&& distances.get(indexTime) == distance
							&& speeds.get(indexTime) == speed
						) // Right
						||
						(
							indexTime != 0
							&& distances.get(indexTime - 1) == distance
							&& speeds.get(indexTime - 1) == speed
						) // Left
					)
				) {
			return false;
		}

		distances.add(indexTime, distance);
		speeds.add(indexTime, speed);
		spdIncs.add(indexTime, spdinc);
		times.add(indexTime, time); // legg tid til slutt da det viser at all
									// data har blitt ferdig lagt til i denne
									// indexen til andre threads.
		return true;
	}

	public void updatePositionByInformation(long time, int trackLength,
			float delta, boolean affectModel) {
		// TODO set positionZ til forrige distance om etter eller anta distance
		// om f�r

		if (positionX >= trackLength || finished)
			return;

		try {
			int length = times.size() - 1;
			int found = 0;
			for (int i = length; i >= 0; i--) {
				if (time >= times.get(i)) {
					found = i;
					break;
				}
			}

			if (found == 0) {
				positionX = 0;
				if (affectModel)
					rotateWheels(model, wheelRotation, 0);
			} else {
				float timeFromFound = times.get(found) - time;

				if (found == length) {
					// calc distance
					float differenceTime = timeFromFound / 1000f;
					float mps = speeds.get(found) / 3.6f;
					float predictedDistance = mps * differenceTime;

					positionX = distances.get(found) + predictedDistance;

				} else {
					// interpolate between
					float timeToNext = times.get(found + 1) - times.get(found);
					float interpolation = timeFromFound / timeToNext; // vil
																		// v�re
																		// 1 om
																		// time
																		// helt
																		// mot
																		// next,
																		// mens
																		// 0 om
																		// helt
																		// mot
																		// found

					float distanceFromFoundToNext = distances.get(found + 1)
							- distances.get(found);

					positionX = distances.get(found)
							- (interpolation * distanceFromFoundToNext);
				}

				float currentSpeed = speeds.get(found);

				float averageSpeed = 0;
				float firstSpeed = 0;
				boolean firstCheck = true;
				boolean addCurrentSpeed = true;
				for (float spd : mpsLast) {
					if (firstCheck) {
						firstSpeed = spd;
						firstCheck = false;
					}
					if ((int) spd == (int) currentSpeed) {
						addCurrentSpeed = false;
					}
					averageSpeed += spd;
				}
				averageSpeed /= mpsLast.size();
				averageSpeed -= firstSpeed;

				// System.out.println("baseSpeed: " + firstSpeed);
				// System.out.println("AverageSpeed: " + averageSpeed);
				if (affectModel)
					bumDown(model, averageSpeed);

				if (mpsLast.size() == 0 || addCurrentSpeed)
					mpsLast.add(currentSpeed);

				while (mpsLast.size() > 6) {
					mpsLast.remove();
				}

				if (affectModel)
					rotateWheels(model, wheelRotation, currentSpeed * delta);
			}
		} catch (IndexOutOfBoundsException | NullPointerException ex) {
			System.out.println(
					"Fikk ikke rotert hjul pga feil fart. Sikkert pga finishRace "
							+ ex.getMessage());
		}

		model.updateTransformation();

	}

	public void setFinished(boolean b) {
		this.finished = b;
	}

	public boolean isFinished() {
		return finished;
	}

	public void setModel(int i) {
		if (CarChoiceSubscene.CARS[i] == null)
			return;
		this.model = CarChoiceSubscene.CARS[i];
		// this.modelIndex = i;
		wheelRotation = new float[model.getGos().size() - 1];
	}

	public static void bumDown(Model model, float spdinc) {
		var g = model.getGos().get(0);
		Vec3 pos = g.position();
		Vec3 rot = g.rotation();
		float origoX = g.getMesh().getSize().z / 4; // Ikke helt origo for vi
													// �nsker � ha baken litt
													// mer ned enn fronten.
		float ogX = pos.x;
		float ogY = pos.y;
		float ogZ = pos.z;
		float ogRotX = rot.x;
		float ogRotY = rot.y;
		float ogRotZ = rot.z;

		// Increase the needed power to push down and bound it so the car does
		// not stand vertically.
		spdinc /= 8f;
		if (Math.abs(spdinc) > 3f)
			spdinc = Math.signum(spdinc) * 3f;

		// go to origo and rotate the first mesh.
		g.resetTransformation();
		pos.x = origoX;
		pos.y = 0;
		pos.z = 0;
		rot.x = 0;
		rot.y = 0;
		rot.z = 0;
		g.updateTransformation();
		g.setIndieRotationZ(spdinc);

		// return to original pos
		g.setPositionX(-origoX); // stod ikke helt p� origo
		pos.x = ogX;
		pos.y = ogY;
		pos.z = ogZ;
		g.updatePosition();
		rot.x = ogRotX;
		rot.y = ogRotY;
		rot.z = ogRotZ;
		g.updateRotation(); // husk translate->rotate->scale
		g.updateTransformation();
	}

	public static void rotateWheels(Model model, float[] wheelRotation,
			float speed) {
		var gos = model.getGos();

		for (int i = 1; i < gos.size(); i++) {
			var g = gos.get(i);
			if (wheelRotation != null)
				g.rotation().z = 0;
			float rad = g.getMesh().getSize().x / 2f;
			float rotSpd = 3.0f*speed / (2f * (float) Math.PI * rad);
			// Flytt hjulene til origo
			Vec3 pos = g.position();
			pos.x = -g.getMesh().getPositionAvgOg().x;
			pos.y = -g.getMesh().getPositionAvgOg().y;
			g.updateTransformation();

			// Roter rundt origo
			if (wheelRotation != null)
				wheelRotation[i - 1] = g.rotation().z = wheelRotation[i - 1]
						- rotSpd;
			else
				g.rotation().z -= rotSpd;

			g.updateRotation();
			g.updateTransformation();

			// G� tilbake
			pos.invert();
			g.updatePosition();
			g.updateTransformation();
		}
	}

	public void rotateWheels(float speed) {
		rotateWheels(model, wheelRotation, speed);
	}


	public int getSpeed() {
		for (int i = speeds.size() - 1; i >= 0; i--) {
			if (speeds.get(i) > 0)
				return speeds.get(i);
		}
		return 0;
	}


	public int getSpdInc() {
		for (int i = spdIncs.size() - 1; i >= 0; i--) {
			if (spdIncs.get(i) > 0)
				return (int) Math.round(spdIncs.get(i));
		}
		return 0;
	}

	public float getPositionDistance() {
		return positionX;
	}

	public long getLastTime() {
		if (times.isEmpty())
			return 0;
		return times.get(times.size() - 1);
	}
}
