package engine.graphics.objects;

import engine.graphics.Renderer;
import engine.graphics.Shader;
import engine.math.Vec2;
import engine.math.Vec3;

public class SpriteNumeric extends GameObject {

	private final Sprite[] numbers;
	
	// for later creation
	private Vec2 topleft;
	
	// what is shown
	private long number;
	public boolean leanRight;
	private Sprite[] representation;
	public final Shader shader;
	
	public SpriteNumeric(Vec2 topleft, float height, int number, boolean leanRight) {
		this.topleft = topleft;
		this.leanRight = leanRight;
		
		numbers = new Sprite[10]; // 0-9 number images
		
		shader = new Shader("numeric");
		
		for (int i = 0; i < 10; i++) {
			numbers[i] = new Sprite(
					new Vec2(0),
					height, "./images/numbers/" + i + ".png", "main");
			numbers[i].setShader(shader);
			numbers[i].create();
		}
		shader.setUniform("specialColor", new Vec3(1, 1, 1));
		
		setNumber(number);
	}
	
	public void render(Renderer renderer) {
		if(representation == null)
			return;
		
		for (int i = 0; i < representation.length; i++) {
			int a = leanRight ? representation.length - (1 + i) : i;
			try {
				representation[a].setPosition(
						new Vec2(
								topleft.x + ((leanRight ? -1 : 1) * (i != 0 ? (representation[i - 1].getWidth() * 1.1f) * i : 0)), 
								topleft.y
								));
				renderer.renderOrthoMesh(representation[a]);
			} catch (NullPointerException ex) {
				System.out.println(ex.getMessage() + " ved SpriteNumeric. Sikkert pga dritkjapp bil");
			}
		}
	}
	
	// create the sprites
	public void setNumber(long number) {
		if(number == this.number)
			return;

		this.number = number;
		String stringRep = String.valueOf(number);
		representation = new Sprite[number < 0 ? stringRep.length() - 1 : stringRep.length()];
		int n = 0;
		for (var c : stringRep.toCharArray()) {
			if (c < 48 || c > 57)
				continue;

			byte numberIndex = (byte) c;
			numberIndex -= 48;
			representation[n] = numbers[numberIndex];
			n++;
		}
	}
	
	public void destroy() {
		if(numbers != null) {
			for (Sprite s : numbers) {
				s.destroy();
			}
		}
	}

	public float getWidth() {
		return numbers[0].getWidth();
	}

	public float getHeight() {
		return numbers[0].getHeight();
	}

	public void setTopleft(Vec2 topleft) {
		this.topleft = topleft;
	}

	public Vec2 getTopleft() {
		return topleft;
	}

	
 }