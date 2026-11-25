package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Shared look of every Oona screen: palette, background, glass panels, buttons, text. */
final class Look {
    static final Identifier BG = Identifier.fromNamespaceAndPath("carrotclient", "textures/gui/background.png");
    static final Identifier LOGO = Identifier.fromNamespaceAndPath("carrotclient", "textures/gui/logo.png");

    static final int ACCENT = 0xFF8000FF, ACCENT_DARK = 0xFF4A00A8, ACCENT_SOFT = 0xFFB66BFF;
    static final int GLASS = 0xC8101218, GLASS_HOT = 0xE61D2029, ROW = 0xB3141720, ROW_HOT = 0xE61F2330;
    static final int TEXT = 0xFFB9BEC9, TEXT_HOT = 0xFFFFFFFF, DIM = 0xFF6D7380, EDGE = 0x22FFFFFF;
    static final int GREEN = 0xFF5CD66B, RED = 0xFFE5484D;

    private static final int[] ICON_COLORS = {0xFF8000FF, 0xFF3B82F6, 0xFF14B8A6, 0xFFE11D74, 0xFFF59E0B};

    private Look() {}

    static Font font() {
        return Minecraft.getInstance().font;
    }

    /** Stretched parallax background, dark tint, embers, vignette. */
    static void background(GuiGraphicsExtractor g, int w, int h, int mx, int my, double t) {
        int ox = (int) ((mx / (float) w - 0.5f) * -24f) - 24;
        int oy = (int) ((my / (float) h - 0.5f) * -14f) - 14;
        g.blit(RenderPipelines.GUI_TEXTURED, BG, ox, oy, 0f, 0f, w + 48, h + 28, w + 48, h + 28);
        g.fill(0, 0, w, h, 0xC4000000);
        TitleFx.embers(g, w, h, t, 26);
        TitleFx.vignette(g, w, h);
    }

    static void rect(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + h, c);
    }

    static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x, y, x + w, y + 1, c);
        g.fill(x, y + h - 1, x + w, y + h, c);
        g.fill(x, y, x + 1, y + h, c);
        g.fill(x + w - 1, y, x + w, y + h, c);
    }

    static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        rect(g, x, y, w, h, 0xD40D0F14);
        frame(g, x, y, w, h, EDGE);
        rect(g, x, y, w, 2, ACCENT);
    }

    static void gradientV(GuiGraphicsExtractor g, int x, int y, int w, int h, int top, int bottom) {
        for (int r = 0; r < h; r++) g.fill(x, y + r, x + w, y + r + 1, lerp(top, bottom, r / (float) Math.max(1, h)));
    }

    /** Glass button; accent = filled purple. hot is the animated 0..1 hover value. */
    static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, float hot, boolean accent) {
        if (accent) {
            gradientV(g, x, y, w, h, lerp(ACCENT, ACCENT_SOFT, hot * 0.35f), lerp(ACCENT_DARK, ACCENT, hot * 0.35f));
            g.fill(x, y, x + w, y + 1, 0x66FFFFFF);
        } else {
            rect(g, x, y, w, h, lerp(GLASS, GLASS_HOT, hot));
            frame(g, x, y, w, h, lerp(EDGE, 0x99B66BFF, hot));
        }
        center(g, label, x + w / 2, y + (h - 8) / 2, accent ? TEXT_HOT : lerp(TEXT, TEXT_HOT, hot));
    }

    static void text(GuiGraphicsExtractor g, String s, int x, int y, int c) {
        g.text(font(), s, x, y, c, true);
    }

    static void plain(GuiGraphicsExtractor g, String s, int x, int y, int c) {
        g.text(font(), s, x, y, c, false);
    }

    static void bold(GuiGraphicsExtractor g, String s, int x, int y, int c) {
        g.text(font(), s, x, y, c, true);
        g.text(font(), s, x + 1, y, c, false);
    }

    static void center(GuiGraphicsExtractor g, String s, int cx, int y, int c) {
        g.text(font(), s, cx - font().width(s) / 2, y, c, true);
    }

    static String clip(String s, int maxPx) {
        Font f = font();
        if (f.width(s) <= maxPx) return s;
        String t = s;
        while (t.length() > 1 && f.width(t + "...") > maxPx) t = t.substring(0, t.length() - 1);
        return t + "...";
    }

    /** Square tile with the first letter, colored from the name. */
    static void tile(GuiGraphicsExtractor g, int x, int y, int size, String name) {
        int c = ICON_COLORS[Math.floorMod(name.hashCode(), ICON_COLORS.length)];
        rect(g, x, y, size, size, c);
        rect(g, x, y, size, 1, 0x55FFFFFF);
        String l = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        g.text(font(), l, x + (size - font().width(l)) / 2, y + (size - 8) / 2, 0xFFFFFFFF, true);
    }

    static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static float ease(float t) {
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    static int lerp(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
            | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
}
