package elem;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import engine.graphics.Renderer;
import engine.graphics.objects.Camera;
import engine.graphics.objects.Sprite;
import engine.io.Window;
import engine.math.Vec3;
import main.ResourceHandler;

public class MovingThings {

	static class MovingThing {
		private final Sprite sprite;
		private final float sideToSIde;
		private final float zPos;
		private final boolean rightSide;

		public MovingThing(Random ran, boolean farAway) {
			sprite = sprites[ran.nextInt(sprites.length)];
			sideToSIde = (float) (Math.pow(ran.nextInt(40), 2) - 15);
			rightSide = ran.nextBoolean();
			var length = 15f*thingsSize;
			if (!farAway) {
				zPos = 250f + ran.nextFloat(length);
			} else {
				zPos = lastDistance + length + ran.nextInt(1000);
			}
		}

		public void update(int extraLanes) {
			sprite.setPositionZ(zPos);

			sprite.setPositionXReal(rightSide ? -5*extraLanes -160 - sideToSIde : 105 + sideToSIde); // 15 is mid
			sprite.setPositionYReal(187f);
		}
	}

	private static final List<MovingThing> things = new ArrayList<>();
	private static final Sprite[] sprites = new Sprite[12];
	private static float lastDistance = 0;
	private static final int thingsSize = 500;
	public static void init() {
		for (int i = 0; i < sprites.length; i++) {
			final int finalI = i;
			ResourceHandler.LoadSprite(10f *Window.HEIGHT, "./images/buildings/building" + i + ".png", "background", (sprite) -> sprites[finalI] = sprite);
		}
	}

	public static void reset(Random ran) {
		lastDistance = 0;
		things.clear();
		while(things.size() < thingsSize) {
			things.add(new MovingThing(ran, false));
		}

		things.sort((o1, o2) -> Float.compare(o2.zPos, o1.zPos));
	}

	public static void render(Random ran, Renderer renderer, Camera camera, Vec3 actualTint, float actualAlpha, boolean grind, int extraLanes, boolean theBoss) {
		lastDistance = camera.getPosition().z;

		var added = false;
		while(things.size() < thingsSize) {
			things.add(new MovingThing(ran, true));
			added = true;
		}
		if (added)
			things.sort((o1, o2) -> Float.compare(o2.zPos, o1.zPos));

		for (int i = 0; i < things.size(); i++) {
			var t = things.get(i);
			if (t.zPos < lastDistance - 1000) {
				things.remove(t);
				continue;
			}
			t.update(extraLanes);
			if (grind)
				t.sprite.getShader().setUniform("tint", actualTint, actualAlpha);
			else {
				if (theBoss) {
					t.sprite.getShader().setUniform("tint", actualTint, .8f); 
				} else {
					var diffDistance = t.zPos - lastDistance;
					diffDistance *= .0000045f;
					if (diffDistance > .8f)
						diffDistance = .8f;
					actualTint.x += diffDistance;
					actualTint.y += diffDistance;
					actualTint.z += 1.5f*diffDistance;
					
					t.sprite.getShader().setUniform("tint", actualTint, actualAlpha * diffDistance);
				}
			}
			renderer.renderMesh(t.sprite, camera);
		}
	}
}
