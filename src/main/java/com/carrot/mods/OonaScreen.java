package com.carrot.mods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Base of the Oona pages: background, fade-in, invisible click areas, hover animation,
 * Esc goes back to the parent. Subclasses lay out in build() and draw in paint().
 */
abstract class OonaScreen extends Screen {
    protected final Screen parent;
    private final long born = System.nanoTime();
    private final float[] hov = new float[96];

    protected OonaScreen(String title, Screen parent) {
        super(Component.literal(title));
        this.parent = parent;
    }

    protected abstract void build();

    protected abstract void paint(GuiGraphicsExtractor g, int mx, int my);

    @Override
    protected void init() {
        build();
    }

    protected final double time() {
        return (System.nanoTime() - born) / 1.0e9;
    }

    /** Transparent click area; everything visible is drawn in paint(). */
    protected final void hit(int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.empty(), b -> action.run()).bounds(x, y, w, h).build());
    }

    /** Animated 0..1 hover value for slot id (ids must be below 96). */
    protected final float hover(int id, boolean hot) {
        hov[id] += ((hot ? 1f : 0f) - hov[id]) * 0.25f;
        return hov[id];
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float dt) {
        double t = time();
        Look.background(g, this.width, this.height, mx, my, t);
        paint(g, mx, my);
        int fade = (int) ((1f - Look.ease((float) Math.min(1.0, t / 0.5))) * 255f);
        if (fade > 0) g.fill(0, 0, this.width, this.height, fade << 24);
    }
}
