package com.carrot.mods;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;

/**
 * Saved servers with one-click join and delete, plus a small form to add a server or connect
 * directly. The text fields are drawn here; the EditBox widgets only handle typing and focus.
 */
public final class OonaMultiplayer extends OonaScreen {
    private static final int ROW_H = 30, GAP = 4;

    private List<Servers.Entry> list = List.of();
    private int scroll;
    private String status = "Click a server to join it.";
    private String nameValue = "", addrValue = "";
    private EditBox nameBox, addrBox;

    public OonaMultiplayer(Screen parent) {
        super("Multiplayer", parent);
    }

    private int pw() { return Math.min(this.width - 40, 480); }
    private int ph() { return Math.min(this.height - 24, 330); }
    private int px() { return (this.width - pw()) / 2; }
    private int py() { return (this.height - ph()) / 2; }
    private int rowsTop() { return py() + 44; }
    private int perPage() { return Math.max(2, (ph() - 44 - 82) / (ROW_H + GAP)); }
    private int formY() { return py() + ph() - 58; }
    private int footY() { return py() + ph() - 30; }

    @Override
    protected void build() {
        Minecraft mc = Minecraft.getInstance();
        list = Servers.all(mc);
        int px = px(), pw = pw();

        for (int i = 0; i < perPage(); i++) {
            int slot = i;
            hit(px + 10, rowsTop() + i * (ROW_H + GAP), pw - 54, ROW_H, () -> join(scroll + slot));
            hit(px + pw - 42, rowsTop() + i * (ROW_H + GAP), 32, ROW_H, () -> delete(scroll + slot));
        }

        int fieldW = (pw - 20 - 6) / 2;
        nameBox = new EditBox(mc.font, px + 12, formY() + 3, fieldW - 8, 14, Component.literal("Name"));
        nameBox.setMaxLength(48);
        nameBox.setValue(nameValue);
        nameBox.setResponder(v -> nameValue = v);
        addRenderableWidget(nameBox);
        addrBox = new EditBox(mc.font, px + 10 + fieldW + 6 + 2, formY() + 3, fieldW - 8, 14, Component.literal("Address"));
        addrBox.setMaxLength(128);
        addrBox.setValue(addrValue);
        addrBox.setResponder(v -> addrValue = v);
        addRenderableWidget(addrBox);

        int fy = footY();
        hit(px + 10, fy, 60, 22, this::onClose);
        hit(px + 76, fy, 80, 22, this::addServer);
        hit(px + 162, fy, 96, 22, this::direct);
        hit(px + 264, fy, 100, 22, () -> mc.gui.setScreen(new JoinMultiplayerScreen(this)));
        hit(px + pw - 62, fy, 24, 22, () -> turn(1));
        hit(px + pw - 36, fy, 24, 22, () -> turn(-1));
    }

    private void join(int index) {
        if (index < 0 || index >= list.size()) return;
        Servers.Entry e = list.get(index);
        if (!Servers.connect(Minecraft.getInstance(), this, e.name(), e.ip())) status = "Could not join. Use the vanilla list.";
    }

    private void delete(int index) {
        if (index < 0 || index >= list.size()) return;
        String name = list.get(index).name();
        status = Servers.remove(Minecraft.getInstance(), index) ? "Removed " + name : "Could not remove it.";
        rebuildWidgets();
    }

    private void addServer() {
        if (addrValue.isBlank()) { status = "Type an address first."; return; }
        if (Servers.add(Minecraft.getInstance(), nameValue.trim(), addrValue.trim())) {
            status = "Added " + (nameValue.isBlank() ? addrValue.trim() : nameValue.trim());
            nameValue = "";
            addrValue = "";
        } else {
            status = "Could not save it. Use the vanilla list.";
        }
        rebuildWidgets();
    }

    private void direct() {
        if (addrValue.isBlank()) { status = "Type an address first."; return; }
        String ip = addrValue.trim();
        if (!Servers.connect(Minecraft.getInstance(), this, nameValue.isBlank() ? ip : nameValue.trim(), ip)) {
            status = "Could not connect from here.";
        }
    }

    private void turn(int d) {
        int max = Math.max(0, list.size() - perPage());
        scroll = Math.max(0, Math.min(max, scroll + d * perPage()));
    }

    /** Mouse wheel; no @Override so a different signature only disables it. */
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int max = Math.max(0, list.size() - perPage());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    protected void paint(GuiGraphicsExtractor g, int mx, int my) {
        int px = px(), py = py(), pw = pw(), ph = ph();
        Look.panel(g, px, py, pw, ph);
        Look.bold(g, "MULTIPLAYER", px + 12, py + 12, Look.TEXT_HOT);
        String count = list.size() + (list.size() == 1 ? " server" : " servers");
        Look.plain(g, count, px + pw - 12 - Look.font().width(count), py + 12, Look.DIM);

        if (list.isEmpty()) {
            Look.text(g, "No saved servers yet.", px + 16, py + 54, Look.TEXT);
            Look.plain(g, "Add one below, or connect directly with an address.", px + 16, py + 68, Look.DIM);
        }
        scroll = Math.min(scroll, Math.max(0, list.size() - perPage()));
        for (int i = 0; i < perPage(); i++) {
            int idx = scroll + i;
            if (idx >= list.size()) break;
            drawRow(g, list.get(idx), px + 10, rowsTop() + i * (ROW_H + GAP), pw - 20, mx, my, i);
        }

        // add / direct connect form
        int fieldW = (pw - 20 - 6) / 2;
        field(g, px + 10, formY(), fieldW, nameBox, nameValue, "Name (optional)");
        field(g, px + 10 + fieldW + 6, formY(), fieldW, addrBox, addrValue, "Address, e.g. play.example.com");

        int fy = footY();
        Look.plain(g, Look.clip(status, pw - 24), px + 12, formY() - 12, Look.DIM);
        Look.button(g, px + 10, fy, 60, 22, "BACK", hover(60, Look.in(mx, my, px + 10, fy, 60, 22)), false);
        Look.button(g, px + 76, fy, 80, 22, "ADD", hover(61, Look.in(mx, my, px + 76, fy, 80, 22)), true);
        Look.button(g, px + 162, fy, 96, 22, "DIRECT JOIN", hover(62, Look.in(mx, my, px + 162, fy, 96, 22)), false);
        Look.button(g, px + 264, fy, 100, 22, "VANILLA LIST", hover(63, Look.in(mx, my, px + 264, fy, 100, 22)), false);
        Look.button(g, px + pw - 62, fy, 24, 22, ">", hover(64, Look.in(mx, my, px + pw - 62, fy, 24, 22)), false);
        Look.button(g, px + pw - 36, fy, 24, 22, "<", hover(65, Look.in(mx, my, px + pw - 36, fy, 24, 22)), false);
    }

    private void drawRow(GuiGraphicsExtractor g, Servers.Entry e, int x, int y, int width, int mx, int my, int slot) {
        boolean hot = Look.in(mx, my, x, y, width - 34, ROW_H);
        boolean delHot = Look.in(mx, my, x + width - 32, y, 32, ROW_H);
        float hv = hover(slot, hot || delHot);
        Look.rect(g, x, y, width, ROW_H, Look.lerp(Look.ROW, Look.ROW_HOT, hv));
        if (hv > 0.05f) Look.rect(g, x, y, 2, ROW_H, Look.lerp(0x008000FF, Look.ACCENT, hv));
        Look.tile(g, x + 6, y + 3, 24, e.name());
        int tx = x + 38;
        int right = x + width - 90;
        Look.text(g, Look.clip(e.name(), right - tx), tx, y + 5, Look.TEXT_HOT);
        Look.plain(g, Look.clip(e.ip(), right - tx), tx, y + 17, Look.DIM);
        if (hot) Look.plain(g, "JOIN", x + width - 80, y + 11, Look.ACCENT_SOFT);
        if (hv > 0.05f) Look.plain(g, "x", x + width - 20, y + 11, delHot ? Look.RED : Look.DIM);
    }

    /** Text field drawn by us; the EditBox only supplies typing, focus and the value. */
    private void field(GuiGraphicsExtractor g, int x, int y, int w, EditBox box, String value, String hint) {
        boolean focused = box != null && box.isFocused();
        Look.rect(g, x, y, w, 20, Look.GLASS);
        Look.frame(g, x, y, w, 20, focused ? Look.ACCENT_SOFT : Look.EDGE);
        String shown = value;
        if (shown.isEmpty() && !focused) {
            Look.plain(g, Look.clip(hint, w - 12), x + 6, y + 6, Look.DIM);
            return;
        }
        while (Look.font().width(shown) > w - 14 && shown.length() > 1) shown = shown.substring(1);
        Look.plain(g, shown, x + 6, y + 6, Look.TEXT_HOT);
        if (focused && (System.nanoTime() / 500_000_000L) % 2 == 0) {
            Look.rect(g, x + 7 + Look.font().width(shown), y + 4, 1, 11, Look.TEXT_HOT);
        }
    }
}
