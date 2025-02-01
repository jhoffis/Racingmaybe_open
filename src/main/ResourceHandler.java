package main;

import java.util.ArrayList;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import engine.graphics.objects.Sprite;
import engine.graphics.Texture;
import engine.math.Vec2;

public class ResourceHandler { // Burde ta imot meshes og texture og shaders og thats it.

	static class SpriteLoader {
		Sprite preparedSprite;
		Vec2 topleftPoint;
		float heightSize;
		String texturePath, shaderName;
		Consumer<Sprite> afterLoaded;
	}
	private static ArrayList<Stack<SpriteLoader>> spriteStacks;
	
	private static final int amountThreads = 3;
	private static int pushIndex;
	private static Stack<SpriteLoader> preparedSpriteStack;
	private static AtomicInteger finishedThreads;
	public static int amount;
	public static boolean running;
    
    public static void init() {
    	spriteStacks = new ArrayList<>(amountThreads);
    	preparedSpriteStack = new Stack<>();
    	finishedThreads = new AtomicInteger();
    	running = true;
    	for(int i = 0; i < amountThreads; i++) {
    		final Stack<SpriteLoader> spriteLoaderStack = new Stack<>();
    		spriteStacks.add(spriteLoaderStack);
    		new Thread(() -> {
    			while (running) { // FIXME endless loop here
    				if (spriteLoaderStack.isEmpty()) continue;
    				var loader = spriteLoaderStack.pop();
	    			
    				Sprite sprite = null;
	    			if (loader.topleftPoint != null) {
	    				sprite = new Sprite(loader.topleftPoint, loader.heightSize, loader.texturePath, loader.shaderName); // Problemet ligger her. Det er hardkoda.
	    			} else if (loader.heightSize != 0) {
	    				sprite = new Sprite(loader.heightSize, loader.texturePath, loader.shaderName);
	    			} else if (loader.shaderName != null) {
	    				sprite = new Sprite(loader.texturePath, loader.shaderName);
	    			} else {
	    				Texture tex = new Texture(loader.texturePath);
	    				sprite = new Sprite();
	    				sprite.texture = tex;
	    			}
	    			loader.preparedSprite = sprite;
	    			preparedSpriteStack.push(loader);
    			}
    			finishedThreads.incrementAndGet();
    		}).start();
    	}
    }

    public static void LoadSprite(Vec2 topleftPoint, float heightSize, String spriteName, String shaderName, Consumer<Sprite> afterLoaded) {
    	if (!running)
    		return;
    	amount++;
    	var loader = new SpriteLoader();
    	loader.topleftPoint = topleftPoint;
    	loader.heightSize = heightSize;
    	loader.texturePath = spriteName;
    	loader.shaderName = shaderName;
    	loader.afterLoaded = afterLoaded;
    	spriteStacks.get(pushIndex).add(loader);
    	pushIndex = (pushIndex + 1) % amountThreads;
    }

    public static void LoadSprite(float heightSize, String spriteName, String shaderName, Consumer<Sprite> afterLoaded) {
    	if (!running)
    		return;
    	amount++;
    	var loader = new SpriteLoader();
    	loader.heightSize = heightSize;
    	loader.texturePath = spriteName;
    	loader.shaderName = shaderName;
    	loader.afterLoaded = afterLoaded;
    	spriteStacks.get(pushIndex).add(loader);
    	pushIndex = (pushIndex + 1) % amountThreads;
    }

    public static void LoadSprite(String spriteName, String shaderName, Consumer<Sprite> afterLoaded) {
    	if (!running)
    		return;
    	amount++;
    	var loader = new SpriteLoader();
    	loader.texturePath = spriteName;
    	loader.shaderName = shaderName;
    	loader.afterLoaded = afterLoaded;
    	spriteStacks.get(pushIndex).add(loader);
    	pushIndex = (pushIndex + 1) % amountThreads;
    }

	public static void LoadTexture(String imgPath, Consumer<Sprite> afterLoaded) {
		if (!running)
    		return;
		
		if (spriteStacks == null) {
			Texture tex = new Texture(imgPath);
			tex.create();
			var sprite = new Sprite();
			sprite.texture = tex;
			afterLoaded.accept(sprite);
		} else {
			amount++;
	    	var loader = new SpriteLoader();
	    	loader.texturePath = imgPath;		
	    	loader.afterLoaded = afterLoaded;
	    	spriteStacks.get(pushIndex).add(loader);
	    	pushIndex = (pushIndex + 1) % amountThreads;
		}
	}

    public static boolean isNotDone() {
        return amount > 0;
    }
    
    public static void createNext() {
        if (!preparedSpriteStack.isEmpty()) {
            amount--;
            var loader = preparedSpriteStack.pop();
            loader.preparedSprite.create();
            loader.afterLoaded.accept(loader.preparedSprite);
        }
    }
    
    public static void destroy() {
    	running = false;
    	while (finishedThreads.get() != amountThreads);
    	preparedSpriteStack = null;
    	spriteStacks.clear();
    	spriteStacks = null;
    	finishedThreads = null;
    }

}
