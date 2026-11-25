package com.carrot.mods;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;

/** World list: click a row to play. Editing and deleting stay on the vanilla list (one click away). */
public final class OonaSingleplayer extends OonaScreen {
    private static final int ROW_H = 30, GAP = 4;

    private volatile List<Worlds.World> worlds;
    private volatile boolean failed;
    private boolean started;
    private int scroll;
    private String status = "Click a world to play it.";

    public OonaSingleplayer(Screen parent) {
        super("Singleplayer", parent);
    }

    private int pw() { return Math.min(this.width - 40, 460); }
    private int ph() { return Math.min(this.height - 24, 320); }
    private int px() { return (this.width - pw()) / 2; }
    private int py() { return (this.height - ph()) / 2; }
    private int rowsTop() { return py() + 44; }
    private int perPage() { return Math.max(2, (ph() - 44 - 50) / (ROW_H + GAP)); }
    private int footY() { return py() + ph() - 30; }

    @Override
    protected void build() {
        Minecraft mc = Minecraft.getInstance();
        if (!started) {
            started = true;
            var future = Worlds.load(mc);
            if (future == null) failed = true;
            else future.whenComplete((list, err) -> {
                if (err != null) failed = true;
                else worlds = list;
            });
        }

        int px = px(), pw = pw(), fy = footY();
        for (int i = 0; i < perPage(); i++) {
            int slot = i;
            hit(px + 10, rowsTop() + i * (ROW_H + GAP), pw - 20, ROW_H, () -> play(scroll + slot));
        }
        hit(px + 10, fy, 60, 22, this::onClose);
        hit(px + 76, fy, 104, 22, () -> {
            if (!Worlds.createNew(mc, () -> mc.gui.setScreen(this))) vanilla();
        });
        hit(px + 186, fy, 100, 22, this::vanilla);
        hit(px + pw - 62, fy, 24, 22, () -> turn(1));
        hit(px + pw - 36, fy, 24, 22, () -> turn(-1));
    }

    private void vanilla() {
        Minecraft.getInstance().gui.setScreen(new SelectWorldScreen(this));
    }

    private void turn(int d) {
        List<Worlds.World> w = worlds;
        int max = w == null ? 0 : Math.max(0, w.size() - perPage());
        scroll = Math.max(0, Math.min(max, scroll + d * perPage()));
    }

    private void play(int index) {
        List<Worlds.World> w = worlds;
        if (w == null || index < 0 || index >= w.size()) return;
        Worlds.World world = w.get(index);
        if (world.locked()) { status = world.name() + " is open in another game."; return; }
        Minecraft mc = Minecraft.getInstance();
        if (!Worlds.open(mc, world.id(), () -> mc.gui.setScreen(this))) {
            status = "Could not open it from here. Use the vanilla list.";
        }
    }

    /** Mouse wheel; no @Override so a different signature only disables it. */
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        List<Worlds.World> w = worlds;
        int max = w == null ? 0 : Math.max(0, w.size() - perPage());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    protected void paint(GuiGraphicsExtractor g, int mx, int my) {
        int px = px(), py = py(), pw = pw(), ph = ph();
        Look.panel(g, px, py, pw, ph);
        Look.bold(g, "SINGLEPLAYER", px + 12, py + 12, Look.TEXT_HOT);
        List<Worlds.World> w = worlds;
        String count = w == null ? (failed ? "" : "loading...") : w.size() + (w.size() == 1 ? " world" : " worlds");
        Look.plain(g, count, px + pw - 12 - Look.font().width(count), py + 12, Look.DIM);

        if (w == null && failed) {
            Look.text(g, "Could not read your worlds here.", px + 16, py + 54, Look.TEXT);
            Look.plain(g, "Use the vanilla list below; nothing is lost.", px + 16, py + 68, Look.DIM);
        } else if (w != null && w.isEmpty()) {
            Look.text(g, "No worlds yet.", px + 16, py + 54, Look.TEXT);
            Look.plain(g, "Press NEW WORLD to make your first one.", px + 16, py + 68, Look.DIM);
        } else if (w != null) {
            scroll = Math.min(scroll, Math.max(0, w.size() - perPage()));
            for (int i = 0; i < perPage(); i++) {
                int idx = scroll + i;
                if (idx >= w.size()) break;
                drawRow(g, w.get(idx), px + 10, rowsTop() + i * (ROW_H + GAP), pw - 20, mx, my, i);
            }
            if (w.size() > perPage()) {
                int trackH = perPage() * (ROW_H + GAP) - GAP;
                int barH = Math.max(14, trackH * perPage() / w.size());
                int barY = rowsTop() + (trackH - barH) * scroll / Math.max(1, w.size() - perPage());
                Look.rect(g, px + pw - 6, barY, 2, barH, Look.ACCENT);
            }
        }

        int fy = footY();
        Look.plain(g, Look.clip(status, pw - 24), px + 12, fy - 12, Look.DIM);
        Look.button(g, px + 10, fy, 60, 22, "BACK", hover(60, Look.in(mx, my, px + 10, fy, 60, 22)), false);
        Look.button(g, px + 76, fy, 104, 22, "NEW WORLD", hover(61, Look.in(mx, my, px + 76, fy, 104, 22)), true);
        Look.button(g, px + 186, fy, 100, 22, "VANILLA LIST", hover(62, Look.in(mx, my, px + 186, fy, 100, 22)), false);
        Look.button(g, px + pw - 62, fy, 24, 22, ">", hover(63, Look.in(mx, my, px + pw - 62, fy, 24, 22)), false);
        Look.button(g, px + pw - 36, fy, 24, 22, "<", hover(64, Look.in(mx, my, px + pw - 36, fy, 24, 22)), false);
    }

    private void drawRow(GuiGraphicsExtractor g, Worlds.World w, int x, int y, int width, int mx, int my, int slot) {
        boolean hot = Look.in(mx, my, x, y, width, ROW_H);
        float hv = hover(slot, hot);
        Look.rect(g, x, y, width, ROW_H, Look.lerp(Look.ROW, Look.ROW_HOT, hv));
        if (hv > 0.05f) Look.rect(g, x, y, 2, ROW_H, Look.lerp(0x008000FF, Look.ACCENT, hv));
        Look.tile(g, x + 6, y + 3, 24, w.name());
        int tx = x + 38;
        int right = x + width - 70;
        Look.text(g, Look.clip(w.name(), right - tx), tx, y + 5, Look.TEXT_HOT);
        String when = w.played() > 0 ? new SimpleDateFormat("MMM d, HH:mm").format(new Date(w.played())) : "never played";
        String sub = (w.mode().isEmpty() ? "" : w.mode() + "  -  ") + when + "  -  " + w.id();
        Look.plain(g, Look.clip(sub, right - tx), tx, y + 17, Look.DIM);
        String tag = w.locked() ? "IN USE" : w.hardcore() ? "HARDCORE" : hot ? "PLAY" : "";
        int tagColor = w.locked() || w.hardcore() ? Look.RED : Look.ACCENT_SOFT;
        Look.plain(g, tag, x + width - 12 - Look.font().width(tag), y + 11, tagColor);
    }
}
