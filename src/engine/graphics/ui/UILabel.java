package engine.graphics.ui;

import main.Features;
import main.Texts;

import java.util.function.Consumer;

import org.lwjgl.nuklear.NkColor;
import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkImage;
import org.lwjgl.nuklear.NkRect;
import org.lwjgl.nuklear.NkVec2;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.system.MemoryStack;

import engine.io.InputHandler;
import main.ResourceHandler;

public class UILabel implements IUIObject {

    // Regular stuff
    private String text;
    private UIColors color;
    public int options;
    public float alpha = 1f;

    // Font and shadow
//	protected UIFont font;
//	public boolean shadow = false, multiline = false;

    // Tooltip
    public String tooltip;
    public Consumer<String> actionTooltip;

    // Img
    private NkImage img;
    private float imgAspect;
    public float widthDynamicOverall;
    public boolean imageToLeft, noImage;

    // Auto row
    public int rowColomns = 0;
    public float rowHeight = 0;

    public UILabel(String text, int options, String imgPath) {
        setText(text);
        this.options = options;
        if (imgPath != null) {
            ResourceHandler.LoadTexture(imgPath, (sprite) -> {
                imgAspect = sprite.texture.widthHeightRatio();
                img = NkImage.create();
                img.handle(it -> it.id(sprite.texture.getTextureID()));
            });
        }
    }

    public UILabel(String text, int options, String imgPath, int rowColomns, int rowHeight) {
        this(text, options, imgPath);
    }

    public UILabel(String text, int options) {
        this(text, options, null);
    }

    public UILabel(String text) {
        this(text, Nuklear.NK_TEXT_ALIGN_LEFT | Nuklear.NK_TEXT_ALIGN_MIDDLE, null);
    }

    public UILabel(int options) {
        this.text = "";
        this.options = options;
    }

    public UILabel() {
        this("");
    }

    public UILabel(String text, UIColors color) {
        this(text);
        setColor(color);
    }


    @Override
    public void layout(NkContext ctx, MemoryStack stack) {

        boolean setupAutomatically = rowColomns > 0 && rowHeight > 0;
        boolean additionalImage = img != null && !noImage;
        float imgW = 0, textW = 0;

        
        if (tooltip != null) {
        	
        	var pos = NkRect.malloc(stack);
        	Nuklear.nk_widget_bounds(ctx, pos);
        	if (pos.x() < InputHandler.x && pos.y() < InputHandler.y
        			&& pos.x() + pos.w() >= InputHandler.x && pos.y() + pos.h() >= InputHandler.y) {
	            Features.inst.pushFontColor(ctx, UIColors.WHITE);
	            if (actionTooltip != null)
	            	actionTooltip.accept(tooltip);
	            else 
	            	Nuklear.nk_tooltip(ctx, tooltip);
	            Features.inst.popFontColor(ctx);
        	}
        }

        if (setupAutomatically) {
            if (additionalImage) {
                imgW = imgAspect * Nuklear.nk_widget_height(ctx) / Nuklear.nk_widget_width(ctx);
                textW = widthDynamicOverall - imgW;

                Nuklear.nk_layout_row_begin(ctx, Nuklear.NK_DYNAMIC, rowHeight, rowColomns);
                if (imageToLeft) {
                    Nuklear.nk_layout_row_push(ctx, imgW);
                    Nuklear.nk_image(ctx, img);
                }
                Nuklear.nk_layout_row_push(ctx, textW);
            } else {
                Nuklear.nk_layout_row_dynamic(ctx, rowHeight, rowColomns);
            }
        }

        render(ctx, text, color, options, alpha);

        if (setupAutomatically && additionalImage) {
            if (!imageToLeft) {
                Nuklear.nk_layout_row_push(ctx, imgW);
                Nuklear.nk_image(ctx, img);
            }
            Nuklear.nk_layout_row_end(ctx);
        }
    }

    public static void render(NkContext ctx, String text, UIColors color, int options, float alphaTune) {
        if (color != null) {
        	try (var stack = MemoryStack.stackPush()) {
	        	NkColor nkColor = NkColor.malloc(stack);
	        	nkColor.set(UIColors.COLORS[color.ordinal()]);
	            int alpha = nkColor.a();
	            nkColor.a((byte) ((alpha + (alpha < 0 ? 256 : 0)) * alphaTune));
	            Nuklear.nk_label_colored(ctx, text, options, nkColor);
	            nkColor.a((byte) alpha);
        	}
        } else {
            Nuklear.nk_label(ctx, text, options);
        }
    }

    public String getText() {
        return text;
    }

    public String getTextWithoutColor() {
        return Texts.removeColor(text).second();
    }

    public void setText(String text) {
        if (text == null)
            text = "";
        
        String[] coloredText = text.split("#");
        if (coloredText.length <= 1 || coloredText[1].isBlank()) {
        	this.text = text;
        	color = null;
        	return;
        }

        UIColors colorType = null;
        try {
        	var colored = coloredText[coloredText.length - 1].toUpperCase();
            colorType = UIColors.valueOf(colored);
            text = text.substring(0, text.length() - 1 - colored.length());
        } catch (Exception e) {
        }
        this.text = text;
        color = colorType;
    }

    public static UILabel[] create(String[] texts, int options) {
        UILabel[] labels = new UILabel[texts.length];

        for (int i = 0; i < labels.length; i++) {
            labels[i] = new UILabel(texts[i], options);
        }
        return labels;
    }

    public static UILabel[] create(String[] texts) {
        UILabel[] labels = new UILabel[texts.length];

        for (int i = 0; i < labels.length; i++) {
            labels[i] = new UILabel(texts[i]);
        }
        return labels;
    }

    public static UILabel[] split(String str, String splitter) {
        String[] texts = str.split(splitter);
        return create(texts);
    }

    public static UILabel[] split(String str, String splitter, int options) {
        String[] texts = str.split(splitter);
        return create(texts, options);
    }

    public void setColor(UIColors color) {
    	this.color = color;
    }

	public UIColors getColor() {
		return color;
	}
}
