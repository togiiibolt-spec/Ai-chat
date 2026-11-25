package com.carrot.mods;

import com.carrot.AccountsScreen;
import com.carrot.CosmeticsScreen;
import com.carrot.ModMenuScreen;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Centered logo, glass buttons with one accent button, icon strip top right, saved-server strip on
 * the left. Everything sizes itself from the window height so nothing overlaps on a short GUI.
 */
public final class FeatherTitle extends OonaScreen {
    private static final int PLAIN = 0, ACCENT = 1, GEAR = 2, GRID = 3, PERSON = 4, BOOKS = 5, QUIT = 6, SERVER = 7;

    private record Btn(String label, int x, int y, int w, int h, int kind, Runnable run) {}

    private final List<Btn> btns = new ArrayList<>();
    private int logoX, logoY, logoSize;

    public FeatherTitle() {
        super("Oona", null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void build() {
        Minecraft mc = Minecraft.getInstance();
        btns.clear();
        int W = this.width, H = this.height;
        boolean compact = H < 300;

        logoSize = compact ? 34 : 48;
        logoY = compact ? 10 : (int) (H * 0.09);
        int quitH = compact ? 20 : 26;
        int quitY = H - quitH - (compact ? 20 : 32);
        int startY = logoY + logoSize + (compact ? 12 : 24);
        int gap = compact ? 4 : 6;
        int avail = quitY - 8 - startY;
        int bh = Math.max(18, Math.min(30, (avail - 3 * gap) / 4));
        int total = 4 * bh + 3 * gap;
        int y0 = startY + Math.max(0, (avail - total) / 2);
        int bw = Math.min(190, W - 40);
        int x = (W - bw) / 2;

        btns.add(new Btn("Singleplayer", x, y0, bw, bh, PLAIN, () -> mc.gui.setScreen(new OonaSingleplayer(this))));
        btns.add(new Btn("Multiplayer", x, y0 + (bh + gap), bw, bh, PLAIN, () -> mc.gui.setScreen(new OonaMultiplayer(this))));
        btns.add(new Btn("Cosmetics", x, y0 + 2 * (bh + gap), bw, bh, PLAIN, () -> mc.gui.setScreen(new CosmeticsScreen(this))));
        btns.add(new Btn("Shader Library", x, y0 + 3 * (bh + gap), bw, bh, ACCENT, () -> mc.gui.setScreen(new LibraryScreen(this))));
        btns.add(new Btn("QUIT GAME", (W - 140) / 2, quitY, 140, quitH, QUIT, mc::stop));

        int ix = W - 40;
        btns.add(new Btn("Settings", ix, 12, 28, 28, GEAR, () -> mc.gui.setScreen(new OonaSettings(this))));
        btns.add(new Btn("Library", ix - 34, 12, 28, 28, BOOKS, () -> mc.gui.setScreen(new LibraryScreen(this))));
        btns.add(new Btn("Mods", ix - 68, 12, 28, 28, GRID, () -> mc.gui.setScreen(new ModMenuScreen(this))));
        btns.add(new Btn("Accounts", ix - 102, 12, 28, 28, PERSON, () -> mc.gui.setScreen(new AccountsScreen(this))));

        // saved servers down the left edge, one click to join
        List<Servers.Entry> servers = Servers.all(mc);
        int n = Math.min(5, servers.size());
        int py = (H - (n * 32 + 8)) / 2;
        for (int i = 0; i < n; i++) {
            Servers.Entry e = servers.get(i);
            btns.add(new Btn(e.name(), 10, py + 4 + i * 32, 28, 28, SERVER, () -> Servers.connect(mc, this, e.name(), e.ip())));
        }

        for (Btn b : btns) hit(b.x(), b.y(), b.w(), b.h(), b.run());
    }

    @Override
    protected void paint(GuiGraphicsExtractor g, int mx, int my) {
        Font font = Look.font();
        double t = time();
        float intro = Look.ease((float) Math.min(1.0, t / 0.7));

        drawLogo(g, font, t);

        int serverCount = 0;
        for (Btn b : btns) if (b.kind() == SERVER) serverCount++;
        if (serverCount > 0) {
            int py = (this.height - (serverCount * 32 + 8)) / 2;
            Look.rect(g, 6, py, 36, serverCount * 32 + 8, Look.GLASS);
            Look.frame(g, 6, py, 36, serverCount * 32 + 8, Look.EDGE);
        }

        for (int i = 0; i < btns.size(); i++) {
            Btn b = btns.get(i);
            int slide = b.kind() == SERVER ? 0 : (int) ((1f - intro) * (10 + i * 2));
            int y = b.y() + slide;
            boolean hot = Look.in(mx, my, b.x(), y, b.w(), b.h());
            float hv = hover(i, hot);
            draw(g, font, b, y, hv);
        }
        drawAccountChip(g, font);
        drawTooltips(g, font, mx, my);

        String v = "Oona 26.2";
        Look.plain(g, v, (this.width - font.width(v)) / 2, this.height - 14, Look.DIM);
        TitleFx.sweep(g, this.width, t);
    }

    private void drawLogo(GuiGraphicsExtractor g, Font font, double t) {
        String name = "OONA";
        int total = logoSize + 12 + font.width(name);
        int x = (this.width - total) / 2;
        int y = logoY + (int) Math.round(Math.sin(t * 1.4) * 1.5);
        g.blit(RenderPipelines.GUI_TEXTURED, Look.LOGO, x, y, 0f, 0f, logoSize, logoSize, logoSize, logoSize);
        int tx = x + logoSize + 12;
        int ty = y + logoSize / 2 - 8;
        Look.bold(g, name, tx, ty, 0xFFFFFFFF);
        Look.plain(g, "PvP  -  Shaders  -  Library", tx, ty + 12, Look.ACCENT_SOFT);
    }

    private void draw(GuiGraphicsExtractor g, Font font, Btn b, int y, float hot) {
        int x = b.x(), w = b.w(), h = b.h();
        switch (b.kind()) {
            case SERVER -> {
                Look.tile(g, x, y, h, b.label());
                if (hot > 0.05f) Look.frame(g, x - 1, y - 1, w + 2, h + 2, Look.lerp(0x00B66BFF, 0xFFB66BFF, hot));
            }
            case QUIT -> {
                Look.rect(g, x, y, w, h, Look.lerp(0x308000FF, 0x608000FF, hot));
                Look.center(g, b.label(), x + w / 2, y + (h - 8) / 2, Look.lerp(Look.ACCENT_SOFT, Look.TEXT_HOT, hot));
            }
            case PLAIN, ACCENT -> Look.button(g, x, y, w, h, b.label(), hot, b.kind() == ACCENT);
            default -> {
                Look.rect(g, x, y, w, h, Look.lerp(Look.GLASS, Look.GLASS_HOT, hot));
                Look.frame(g, x, y, w, h, Look.lerp(Look.EDGE, 0x99B66BFF, hot));
                icon(g, b.kind(), x + w / 2, y + h / 2, Look.lerp(Look.TEXT, Look.TEXT_HOT, hot));
            }
        }
    }

    private void drawAccountChip(GuiGraphicsExtractor g, Font font) {
        String name = playerName();
        int cw = 28 + font.width(name) + 14;
        int x = this.width - 40 - 102 - 12 - cw, y = 12;
        Look.rect(g, x, y, cw, 28, Look.GLASS);
        Look.rect(g, x + 4, y + 4, 20, 20, Look.ACCENT);
        String l = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
        g.text(font, l, x + 14 - font.width(l) / 2, y + 10, 0xFFFFFFFF, false);
        g.text(font, name, x + 30, y + 10, Look.TEXT_HOT, false);
    }

    private void drawTooltips(GuiGraphicsExtractor g, Font font, int mx, int my) {
        for (Btn b : btns) {
            if (b.kind() == PLAIN || b.kind() == ACCENT || b.kind() == QUIT) continue;
            if (!Look.in(mx, my, b.x(), b.y(), b.w(), b.h())) continue;
            int tw = font.width(b.label());
            if (b.kind() == SERVER) {
                Look.rect(g, b.x() + b.w() + 8, b.y() + 6, tw + 10, 16, 0xF0101218);
                g.text(font, b.label(), b.x() + b.w() + 13, b.y() + 10, Look.TEXT_HOT, false);
            } else {
                int x = Math.min(b.x() + b.w() - tw - 8, this.width - tw - 12);
                Look.rect(g, x, b.y() + b.h() + 4, tw + 8, 14, 0xF0101218);
                g.text(font, b.label(), x + 4, b.y() + b.h() + 7, Look.TEXT_HOT, false);
            }
        }
    }

    /** Pixel pictograms; no textures needed. */
    private static void icon(GuiGraphicsExtractor g, int kind, int cx, int cy, int c) {
        switch (kind) {
            case GEAR -> {
                Look.rect(g, cx - 5, cy - 5, 10, 10, c);
                Look.rect(g, cx - 2, cy - 2, 4, 4, Look.GLASS_HOT);
                Look.rect(g, cx - 1, cy - 8, 2, 3, c);
                Look.rect(g, cx - 1, cy + 5, 2, 3, c);
                Look.rect(g, cx - 8, cy - 1, 3, 2, c);
                Look.rect(g, cx + 5, cy - 1, 3, 2, c);
                Look.rect(g, cx - 6, cy - 6, 2, 2, c);
                Look.rect(g, cx + 4, cy - 6, 2, 2, c);
                Look.rect(g, cx - 6, cy + 4, 2, 2, c);
                Look.rect(g, cx + 4, cy + 4, 2, 2, c);
            }
            case GRID -> {
                Look.rect(g, cx - 6, cy - 6, 5, 5, c);
                Look.rect(g, cx + 1, cy - 6, 5, 5, c);
                Look.rect(g, cx - 6, cy + 1, 5, 5, c);
                Look.rect(g, cx + 1, cy + 1, 5, 5, c);
            }
            case PERSON -> {
                Look.rect(g, cx - 3, cy - 8, 6, 6, c);
                Look.rect(g, cx - 6, cy - 1, 12, 7, c);
            }
            case BOOKS -> {
                Look.rect(g, cx - 7, cy - 6, 4, 12, c);
                Look.rect(g, cx - 2, cy - 8, 4, 14, c);
                Look.rect(g, cx + 3, cy - 5, 4, 11, c);
            }
            default -> { }
        }
    }

    private static String playerName() {
        Minecraft mc = Minecraft.getInstance();
        Object name = Compat.get(Compat.get(mc, "getUser"), "getName");
        return name == null ? "Player" : String.valueOf(name);
    }
}
