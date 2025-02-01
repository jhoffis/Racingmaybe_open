package engine.graphics.objects;


import engine.graphics.Shader;
import engine.graphics.Texture;
import engine.graphics.Vertex;
import engine.io.Window;
import engine.math.Vec2;
import engine.math.Vec3;
import main.ResourceHandler;

public class Road {

	private float laneSize;
	private int amountOfPlayers;
	
	private float length = 1000f, size, railingLength;
	
	private GameObject pavement, grass;
	private Model railing;
	public Sprite goal, lamppost;
	private Texture texRoad, texGrass, texGoal;
	public final Shader shBack;

	public float lightingTime = 0;
	public float lightingRanNext = 5;
	public long lightingRan = 0;
	
	public Road() {
		texRoad = new Texture("./images/road.png");
		texRoad.create();
		texGrass = new Texture("./images/grass.jpg");
		texGrass.create();
		
		shBack = new Shader("background");
		shBack.create();
		
		ResourceHandler.LoadSprite(new Vec2(0), Window.HEIGHT * 0.4f, "./images/lamppost.png", "lamppost",
				(s) -> {
					s.setPositionXReal(0);
					s.setPositionY(Window.HEIGHT * 0.1f);
					lamppost = s;	
				});
	}
	
	public void init(int amountOfPlayers) {
		if (amountOfPlayers != 0 && this.amountOfPlayers == amountOfPlayers) return;
		this.amountOfPlayers = amountOfPlayers;
		
		texGoal = new Texture("./images/goal"+ (amountOfPlayers <= 4 ? "0" : amountOfPlayers <= 8 ? "4" : amountOfPlayers <= 12 ? "8" : "12") +".png");
		texGoal.create();
		
		if (amountOfPlayers <= 2) {
			size = 10f;
			laneSize = size / 4f;
			var rod = "./images/railingRod.png";
			var railingTex = "./images/railing2.png";
			railing = new Model("railing.obj", new String[] {
			railingTex,
			rod,
			rod,
			rod,
			rod,
			railingTex,
			rod,
				}, shBack);
			railing.create();
			railing.rotation().y = 90;
			railing.position().z = -size / 2f;
			railingLength = 20f;
		} else {
			size = 5f * (float) amountOfPlayers;
			laneSize = size / (2f * amountOfPlayers);
			if (railing != null)
				railing.destroy();
			railing = null;
		}
		float left = -size / 2f;
		pavement = new GameObject(new Mesh(new Vertex[]{
				new Vertex(new Vec3(left, 0, 0), new Vec2(0.0f, 0.0f)), 
				new Vertex(new Vec3(left, 0, length), new Vec2(0f, length / size)),
				new Vertex(new Vec3(-left, 0, length), new Vec2(1f, length / size)),
				new Vertex(new Vec3(-left, 0, 0), new Vec2(1f, 0f))},
		new int[]{0, 1, 2, 0, 2, 3}, texRoad), shBack);
		pavement.getMesh().create();
		
		var grassSize = length;
		grass = new GameObject(new Mesh(new Vertex[]{
				new Vertex(new Vec3(grassSize, 0, 0), new Vec2(0.0f, 0.0f)), 
				new Vertex(new Vec3(grassSize, 0, grassSize), new Vec2(0f, 1f)),
				new Vertex(new Vec3(-grassSize, 0, grassSize), new Vec2(1f, 1f)),
				new Vertex(new Vec3(-grassSize, 0, 0), new Vec2(1f, 0f))},
				new int[]{0, 1, 2, 0, 2, 3}, texGrass), shBack);
		grass.getMesh().create();
		grass.setRotationX(180);
		grass.setPositionY(0.01f);
		
//		pavement.setPositionX(x);
//		pavement.setPositionY(y);
//		pavement.setRotationY(90);
//		ResourceHandler.LoadSprite(100, "./images/nosbottle.png", "main", (sprite) -> pavement = sprite);


		
		var wh = (float) texGoal.getHeight() / (float) texGoal.getWidth();
		
		goal = new Sprite();
		left *= 1.2f;
		float size = this.size * 1.2f;
		goal.init(
				new Vec3(-left, size * wh, 0.0f),
				new Vec3(-left, 0, 0.0f),
				new Vec3(left, 0, 0.0f),
				new Vec3(left, size * wh, 0.0f),
				new Vec3(0),
				new Vec3(0),
				new Vec3(1)
				, texGoal, "goal");
		goal.create();
//		goal.setPositionY(180);
//		goal.setRotation(new Vec3(0, -90f, 0)); // + (4f * ((float) Window.HEIGHT / (float) Window.WIDTH) * 16f / 9f), 0));
	}
	
	public float createLanePos(int lane) {
		float distanceToLeftLane = laneSize;
		if (amountOfPlayers >= 2) {
			distanceToLeftLane = amountOfPlayers * distanceToLeftLane / (1f + (1f / (amountOfPlayers-1)));
		}
		return distanceToLeftLane - lane * 2f * laneSize;
	}
	public void setScale(float frameScale) {
		if (goal == null)
			return;
		goal.setScale(new Vec3(1f, 1f + (1f - frameScale), 1f));		
	}

	public float getLaneSize() {
		return laneSize;
	}

	public int getAmountOfPlayers() {
		return amountOfPlayers;
	}

	public float getLength() {
		return length;
	}

	public float getSize() {
		return size;
	}

	public float getRailingLength() {
		return railingLength;
	}

	public GameObject getPavement() {
		return pavement;
	}

	public GameObject getGrass() {
		return grass;
	}

	public Model getRailing() {
		return railing;
	}

	public Sprite getGoal() {
		return goal;
	}

	public Sprite getLamppost() {
		return lamppost;
	}

	public Texture getTexRoad() {
		return texRoad;
	}

	public Texture getTexGrass() {
		return texGrass;
	}

	public Texture getTexGoal() {
		return texGoal;
	}

	public float getLightingTime() {
		return lightingTime;
	}

	public float getLightingRanNext() {
		return lightingRanNext;
	}

	public long getLightingRan() {
		return lightingRan;
	}
	
	
}
