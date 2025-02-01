package engine.graphics.ui;

import engine.math.Vec2;
import main.Features;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;
import scenes.Scenes;

import java.util.ArrayList;

import static org.lwjgl.nuklear.Nuklear.nk_layout_row_dynamic;

public class UIRisingTexts {

    static class RisingText extends UILabel {
        public double timeLeft;
        public final int scene;
        private final UIWindowInfo window;
        public RisingText(int scene, double x, double y, String text) {
            super(text);
            this.scene = scene;
            window = UISceneInfo.createWindowInfo(scene,
                    x,
                    y,
                    text.length() * font.getHeightFloat(),
                    2d * (double) font.getHeightFloat());
            timeLeft = 40;
        }
        public RisingText(double x, double y, String text) {
            this(Scenes.CURRENT, x, y, text);
        }

        @Override
        public void layout(NkContext ctx, MemoryStack stack) {
            if (window.begin(ctx)) {
                Nuklear.nk_style_push_font(ctx, font.getFont());
                nk_layout_row_dynamic(ctx, 1.1f*window.height, 1);
                super.layout(ctx, stack);
                Nuklear.nk_style_pop_font(ctx);
            }
            Nuklear.nk_end(ctx);
        }
    }
    public static final UIFont font = new UIFont(Font.BOLD_ITALIC, 24);
    private static final ArrayList<RisingText> texts = new ArrayList<>();

    public static void pushText(double x, double y, String text, UIColors color) {
        var label = new RisingText(x, y, text);
        label.setColor(color);
        texts.add(label);
    }

    public static void pushText(int scene, Vec2 vec2, String text, UIColors color) {
        var label = new RisingText(scene, vec2.x, vec2.y, text);
        label.setColor(color);
        texts.add(label);
    }

    public static void layout(NkContext ctx, MemoryStack stack, float delta) {
        for (var i = 0; i < texts.size(); i++) {
            var text = texts.get(i);
            if (Scenes.CURRENT != text.scene)
                continue;
            if (text.timeLeft < 0) {
                texts.remove(text);
                continue;
            }
            text.timeLeft -= delta;
            text.window.addY(-2f*delta);
            text.layout(ctx, stack);
        }
    }
}
