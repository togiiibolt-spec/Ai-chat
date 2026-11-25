package com.carrot.mods;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * Settings hub. GAME tab opens the vanilla option pages (video, controls, sound, ...) under the
 * dark theme; OONA tab is an on/off switch for every module and Oona option.
 */
public final class OonaSettings extends OonaScreen {
    private static final String OPT = "net.minecraft.client.gui.screens.options.";

    private record Item(String label, String hint, BooleanSupplier on, Runnable click) {}

    private int tab;        // 0 game, 1 oona
    private int scroll;
    private String status = "";
    private final List<Item> game = new ArrayList<>();
    private final List<Item> oona = new ArrayList<>();

    public OonaSettings(Screen parent) {
        super("Settings", parent);
        game.add(page("Video Settings", "Graphics, render distance, FOV", OPT + "VideoSettingsScreen"));
        game.add(page("Controls", "Keybinds, mouse, Oona keys", OPT + "controls.ControlsScreen", OPT + "controls.KeyBindsScreen"));
        game.add(page("Sound", "Volume sliders", OPT + "SoundOptionsScreen"));
        game.add(page("Chat", "Chat size, colors, commands", OPT + "ChatOptionsScreen"));
        game.add(page("Language", "Game language", OPT + "LanguageSelectScreen"));
        game.add(page("Accessibility", "Text, motion, narrator", OPT + "AccessibilityOptionsScreen"));
        game.add(page("Skin Customization", "Cape, jacket, sleeves", OPT + "SkinCustomizationScreen"));
        game.add(new Item("Shaders and Packs", "Open the Oona library", null, this::library));
        game.add(page("All Vanilla Options", "The original settings screen", OPT + "OptionsScreen"));

        for (Modules.Entry e : Modules.ALL) {
            oona.add(new Item(e.name(), e.cat() + " module", e.get(), () -> {
                e.set().accept(!e.get().getAsBoolean());
                Cfg.save();
            }));
        }
        oona.add(toggle("Oona title screen", "Off = Carrot's old title", () -> Cfg.I.newTitle, v -> Cfg.I.newTitle = v));
        oona.add(toggle("Dark theme on pages", "Tint over vanilla menus and inventory", () -> Cfg.I.pageTheme, v -> Cfg.I.pageTheme = v));
        oona.add(toggle("Xray: ore sparkle", "Particles on every ore in range", () -> Cfg.I.xrayGlow, v -> Cfg.I.xrayGlow = v));
        oona.add(toggle("Xray: fullbright", "Night vision while xray is on", () -> Cfg.I.xrayFullbright, v -> Cfg.I.xrayFullbright = v));
        oona.add(toggle("Xray: ore radar", "Nearest ore on the action bar", () -> Cfg.I.xrayRadar, v -> Cfg.I.xrayRadar = v));
    }

    private static Item toggle(String label, String hint, BooleanSupplier get, java.util.function.Consumer<Boolean> set) {
        return new Item(label, hint, get, () -> {
            set.accept(!get.getAsBoolean());
            Cfg.save();
        });
    }

    private Item page(String label, String hint, String... classNames) {
        return new Item(label, hint, null, () -> openVanilla(classNames));
    }

    private void library() {
        try {
            Screens.openLibrary(Minecraft.getInstance());
        } catch (Throwable t) {
            status = "The library needs the Oona jar.";
        }
    }

    /** First class that exists and has a constructor we can fill: Screen -> this, Options -> mc.options. */
    private void openVanilla(String[] classNames) {
        Minecraft mc = Minecraft.getInstance();
        for (String name : classNames) {
            try {
                Class<?> c = Class.forName(name);
                List<Constructor<?>> ctors = new ArrayList<>(Arrays.asList(c.getConstructors()));
                ctors.sort((a, b) -> Integer.compare(a.getParameterCount(), b.getParameterCount()));
                for (Constructor<?> k : ctors) {
                    Class<?>[] pt = k.getParameterTypes();
                    Object[] args = new Object[pt.length];
                    boolean ok = true;
                    for (int i = 0; i < pt.length && ok; i++) {
                        Class<?> p = pt[i];
                        if (p == Screen.class) args[i] = this;
                        else if (p == Minecraft.class) args[i] = mc;
                        else if (p != Object.class && p.isInstance(mc.options)) args[i] = mc.options;
                        else if (p == boolean.class) args[i] = Boolean.FALSE;
                        else if (!p.isPrimitive()) args[i] = null;
                        else ok = false;
                    }
                    if (!ok) continue;
                    mc.gui.setScreen((Screen) k.newInstance(args));
                    return;
                }
            } catch (Throwable ignored) {
                // try the next class name
            }
        }
        status = "That page is not available in this version.";
    }

    private int pw() { return Math.min(this.width - 40, 460); }
    private int ph() { return Math.min(this.height - 24, 330); }
    private int px() { return (this.width - pw()) / 2; }
    private int py() { return (this.height - ph()) / 2; }
    private int rowH() { return this.height < 300 ? 26 : 30; }
    private int rowsTop() { return py() + 44; }
    private int perPage() { return Math.max(2, (ph() - 44 - 44) / (rowH() + 4)); }
    private int footY() { return py() + ph() - 30; }
    private List<Item> items() { return tab == 0 ? game : oona; }

    @Override
    protected void build() {
        int px = px(), pw = pw(), fy = footY();
        hit(px + 12, py() + 8, 70, 22, () -> switchTab(0));
        hit(px + 88, py() + 8, 70, 22, () -> switchTab(1));
        for (int i = 0; i < perPage(); i++) {
            int slot = i;
            hit(px + 10, rowsTop() + i * (rowH() + 4), pw - 20, rowH(), () -> click(scroll + slot));
        }
        hit(px + 10, fy, 60, 22, this::onClose);
        hit(px + pw - 62, fy, 24, 22, () -> turn(1));
        hit(px + pw - 36, fy, 24, 22, () -> turn(-1));
    }

    private void switchTab(int t) {
        tab = t;
        scroll = 0;
        status = "";
    }

    private void click(int index) {
        List<Item> l = items();
        if (index >= 0 && index < l.size()) l.get(index).click().run();
    }

    private void turn(int d) {
        int max = Math.max(0, items().size() - perPage());
        scroll = Math.max(0, Math.min(max, scroll + d * perPage()));
    }

    /** Mouse wheel; no @Override so a different signature only disables it. */
    public boolean mouseScrolled(double x, double y, double dx, double dy) {
        int max = Math.max(0, items().size() - perPage());
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    protected void paint(GuiGraphicsExtractor g, int mx, int my) {
        int px = px(), py = py(), pw = pw(), ph = ph(), rh = rowH();
        Look.panel(g, px, py, pw, ph);

        tabLabel(g, px + 12, py + 8, 70, "GAME", tab == 0, mx, my);
        tabLabel(g, px + 88, py + 8, 70, "OONA", tab == 1, mx, my);

        List<Item> l = items();
        scroll = Math.min(scroll, Math.max(0, l.size() - perPage()));
        for (int i = 0; i < perPage(); i++) {
            int idx = scroll + i;
            if (idx >= l.size()) break;
            int y = rowsTop() + i * (rh + 4);
            drawRow(g, l.get(idx), px + 10, y, pw - 20, rh, mx, my, i);
        }
        if (l.size() > perPage()) {
            int trackH = perPage() * (rh + 4) - 4;
            int barH = Math.max(14, trackH * perPage() / l.size());
            int barY = rowsTop() + (trackH - barH) * scroll / Math.max(1, l.size() - perPage());
            Look.rect(g, px + pw - 6, barY, 2, barH, Look.ACCENT);
        }

        int fy = footY();
        Look.plain(g, Look.clip(status, pw - 24), px + 12, fy - 12, Look.DIM);
        Look.button(g, px + 10, fy, 60, 22, "BACK", hover(60, Look.in(mx, my, px + 10, fy, 60, 22)), false);
        Look.button(g, px + pw - 62, fy, 24, 22, ">", hover(61, Look.in(mx, my, px + pw - 62, fy, 24, 22)), false);
        Look.button(g, px + pw - 36, fy, 24, 22, "<", hover(62, Look.in(mx, my, px + pw - 36, fy, 24, 22)), false);
    }

    private void tabLabel(GuiGraphicsExtractor g, int x, int y, int w, String label, boolean on, int mx, int my) {
        boolean hot = Look.in(mx, my, x, y, w, 22);
        Look.bold(g, label, x + 6, y + 7, on ? Look.ACCENT_SOFT : hot ? Look.TEXT_HOT : Look.DIM);
        if (on) Look.rect(g, x, y + 20, w, 2, Look.ACCENT);
    }

    private void drawRow(GuiGraphicsExtractor g, Item it, int x, int y, int w, int h, int mx, int my, int slot) {
        boolean hot = Look.in(mx, my, x, y, w, h);
        float hv = hover(slot, hot);
        Look.rect(g, x, y, w, h, Look.lerp(Look.ROW, Look.ROW_HOT, hv));
        if (hv > 0.05f) Look.rect(g, x, y, 2, h, Look.lerp(0x008000FF, Look.ACCENT, hv));
        Look.text(g, Look.clip(it.label(), w - 90), x + 10, y + (h >= 30 ? 5 : 3), Look.TEXT_HOT);
        Look.plain(g, Look.clip(it.hint(), w - 90), x + 10, y + (h >= 30 ? 17 : 14), Look.DIM);

        if (it.on() == null) {
            Look.plain(g, ">", x + w - 16, y + (h - 8) / 2, Look.lerp(Look.DIM, Look.ACCENT_SOFT, hv));
        } else {
            boolean on = it.on().getAsBoolean();
            int sx = x + w - 40, sy = y + (h - 12) / 2;
            Look.rect(g, sx, sy, 28, 12, on ? Look.ACCENT : 0xFF2A2D35);
            Look.rect(g, on ? sx + 16 : sx + 2, sy + 2, 10, 8, 0xFFFFFFFF);
        }
    }
}
