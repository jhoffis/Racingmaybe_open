package engine.graphics.ui;

import static org.lwjgl.nuklear.Nuklear.NK_UTF_INVALID;
import static org.lwjgl.nuklear.Nuklear.nk_style_set_font;
import static org.lwjgl.nuklear.Nuklear.nnk_utf_decode;
import static org.lwjgl.opengl.GL11C.GL_LINEAR;
import static org.lwjgl.opengl.GL11C.GL_RGBA;
import static org.lwjgl.opengl.GL11C.GL_RGBA8;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11C.glBindTexture;
import static org.lwjgl.opengl.GL11C.glGenTextures;
import static org.lwjgl.opengl.GL11C.glTexImage2D;
import static org.lwjgl.opengl.GL11C.glTexParameteri;
import static org.lwjgl.opengl.GL12C.GL_UNSIGNED_INT_8_8_8_8_REV;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointHMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_GetFontVMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_GetPackedQuad;
import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;
import static org.lwjgl.stb.STBTruetype.stbtt_PackBegin;
import static org.lwjgl.stb.STBTruetype.stbtt_PackEnd;
import static org.lwjgl.stb.STBTruetype.stbtt_PackFontRange;
import static org.lwjgl.stb.STBTruetype.stbtt_PackSetOversampling;
import static org.lwjgl.stb.STBTruetype.stbtt_ScaleForPixelHeight;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import org.lwjgl.nuklear.NkContext;
import org.lwjgl.nuklear.NkUserFont;
import org.lwjgl.nuklear.NkUserFontGlyph;
import org.lwjgl.nuklear.Nuklear;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import adt.IActionRet;
import engine.io.Window;
import engine.utils.FileUtils;

public class UIFont {
	
	private NkUserFont font = NkUserFont.create();
	private ByteBuffer ttf;
	private IActionRet<Float> fontHeightCreator;
	private float prevFontHeight;

	public UIFont(Font font, int fontHeight) {
		try {
			
			String fontname = "fonts/" + "iosevka-fixed-curly-" + switch (font) {
				case REGULAR:
					yield "light.ttf";// "regular.ttf";
				case ITALIC :
					yield "italic.ttf";
				case BOLD_REGULAR :
					yield "bold.ttf";
				case BOLD_ITALIC :
					yield "bolditalic.ttf";
			};
			
            this.ttf = FileUtils.ioResourceToByteBuffer(fontname, 512 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
		
		this.resizeFont(fontHeight);
		
		UISceneInfo.pushFont(this);
	}
	
	public void resizeFont(float fontHeight) {
		float calcedFontHeight = fontHeight / (float) Window.HEIGHT;
		this.fontHeightCreator = () -> (float) Window.HEIGHT * calcedFontHeight;
		updateResizeFont();
	}
	
	public void updateResizeFont() {
		resizeFontActually(getHeight());
	}
	
	private void resizeFontActually(int fontHeight) {
		if (fontHeight <= 0 || prevFontHeight == fontHeight) return;
		prevFontHeight = fontHeight;
		
		System.out.println("resizeFontActually");
		
		int BITMAP_W = (int) ((float) fontHeight / 24f * 1024f);
		int BITMAP_H = BITMAP_W;

		int fontTexID = glGenTextures();

		STBTTFontinfo          fontInfo = STBTTFontinfo.create();
		STBTTPackedchar.Buffer cdata    = STBTTPackedchar.create(256);
//		STBTTPackedchar.Buffer cdata    = STBTTPackedchar.create(ttf.capacity());
//fontInfo.
		float scale;
		float descent;
//		
//		Nuklear.nk_filter_ascii(null, fontTexID)
		
//		this.ttf.reset();
		var ttf = this.ttf.duplicate(); 

		try (MemoryStack stack = stackPush()) {
		    stbtt_InitFont(fontInfo, ttf);
		    scale = stbtt_ScaleForPixelHeight(fontInfo, fontHeight);

		    IntBuffer d = stack.mallocInt(1);
		    stbtt_GetFontVMetrics(fontInfo, null, d, null);
		    descent = d.get(0) * scale;

		    ByteBuffer bitmap = memAlloc(BITMAP_W * BITMAP_H);

		    STBTTPackContext pc = STBTTPackContext.malloc(stack);
		    stbtt_PackBegin(pc, bitmap, BITMAP_W, BITMAP_H, 0, 1, NULL);
		    stbtt_PackSetOversampling(pc, 4, 4);
		    stbtt_PackFontRange(pc, ttf, 0, fontHeight, 32, cdata);
		    stbtt_PackEnd(pc);

		    // Convert R8 to RGBA8
		    ByteBuffer texture = memAlloc(BITMAP_W * BITMAP_H * 4);
		    for (int i = 0; i < bitmap.capacity(); i++) {
		        texture.putInt((bitmap.get(i) << 24) | 0x00FFFFFF);
		    }
		    texture.flip();

		    glBindTexture(GL_TEXTURE_2D, fontTexID);
		    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, BITMAP_W, BITMAP_H, 0, GL_RGBA, GL_UNSIGNED_INT_8_8_8_8_REV, texture);
		    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);

		    memFree(texture);
		    memFree(bitmap);
		}

		font
		    .width((handle, h, text, len) -> {
		    	
		        float text_width = 0;
		        try (MemoryStack stack = stackPush()) {
		            IntBuffer unicode = stack.mallocInt(1);

		            int glyph_len = nnk_utf_decode(text, memAddress(unicode), len);
		            int text_len  = glyph_len;

		            if (glyph_len == 0) {
		                return 0;
		            }

		            IntBuffer advance = stack.mallocInt(1);
		            while (text_len <= len && glyph_len != 0) {
		                if (unicode.get(0) == NK_UTF_INVALID) {
		                    break;
		                }

		                /* query currently drawn glyph information */
		                stbtt_GetCodepointHMetrics(fontInfo, unicode.get(0), advance, null);
		                text_width += advance.get(0) * scale;

		                /* offset next glyph */
		                glyph_len = nnk_utf_decode(text + text_len, memAddress(unicode), len - text_len);
		                text_len += glyph_len;
		            }
		        }
		        
		        return text_width;
		    })
		    .height(fontHeight)
		    .query((handle, font_height, glyph, codepoint, next_codepoint) -> {
		        try (MemoryStack stack = stackPush()) {
		            FloatBuffer x = stack.floats(0.0f);
		            FloatBuffer y = stack.floats(0.0f);

		            STBTTAlignedQuad q       = STBTTAlignedQuad.malloc(stack);
		            IntBuffer        advance = stack.mallocInt(1);
//		            try {
//		            	
//		            	for (int i = 0; i < cdata.sizeof(); i++) {
//		            		if (cdata.get(i).isNull(0)) {
//		            			System.out.println("oh oh");
//		            			return;
//		            		}
//		            	}
////		            	cdata.reset();
//					} catch (BufferUnderflowException e) {
//						System.out.println(e.getMessage());
//						System.out.println(e.getCause());
//					}
		            if (codepoint >= 32) {
		            	if (cdata.capacity() <= codepoint - 32)
		            		codepoint = 63; // turn unknown chars into ?
			            stbtt_GetPackedQuad(cdata, BITMAP_W, BITMAP_H, codepoint - 32, x, y, q, false);
			            
			            stbtt_GetCodepointHMetrics(fontInfo, codepoint, advance, null);
	
			            NkUserFontGlyph ufg = NkUserFontGlyph.create(glyph);
	
			            ufg.width(q.x1() - q.x0());
			            ufg.height(q.y1() - q.y0());
			            ufg.offset().set(q.x0(), q.y0() + (fontHeight + descent));
			            ufg.xadvance(advance.get(0) * scale);
			            ufg.uv(0).set(q.s0(), q.t0());
			            ufg.uv(1).set(q.s1(), q.t1());
		            }
		        }
		    })
		    .texture(it -> it
		        .id(fontTexID));
	}
	
	public void use(NkContext ctx) {
		nk_style_set_font(ctx, font);
	}

	public NkUserFont getFont() {
		return font;
	}

	public int getHeight() {
		return fontHeightCreator.run().intValue();
	}

	public float getHeightFloat() {
		return fontHeightCreator.run();
	}
	
	public void destroy() {
		try {
			font.free();
		} catch (Exception e) {
			// do nothing
		}
	}
	
}
