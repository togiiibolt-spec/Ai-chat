package com.carrot.mods;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;

/** Saved worlds: listing and opening, reached by reflection; null/false means "use the vanilla screen". */
final class Worlds {
    record World(String name, String id, long played, String mode, boolean hardcore, boolean locked) {}

    private Worlds() {}

    static CompletableFuture<List<World>> load(Minecraft mc) {
        Object src = Compat.get(mc, "getLevelSource");
        Object candidates = Compat.invoke(src, "findLevelCandidates");
        if (candidates == null) return null;
        Object future = Compat.invoke(src, "loadLevelSummaries", candidates);
        if (!(future instanceof CompletableFuture<?> f)) return null;
        return f.thenApply(Worlds::convert);
    }

    private static List<World> convert(Object summaries) {
        List<World> out = new ArrayList<>();
        if (summaries instanceof List<?> l) {
            for (Object s : l) {
                String name = String.valueOf(Compat.get(s, "getLevelName"));
                String id = String.valueOf(Compat.get(s, "getLevelId"));
                Object played = Compat.get(s, "getLastPlayed");
                Object gm = Compat.get(s, "getGameMode");
                Object modeName = Compat.get(gm, "getName");
                String mode = modeName == null ? "" : String.valueOf(modeName);
                if (!mode.isEmpty()) mode = Character.toUpperCase(mode.charAt(0)) + mode.substring(1);
                out.add(new World(name, id, played instanceof Long v ? v : 0L, mode,
                    Boolean.TRUE.equals(Compat.get(s, "isHardcore")),
                    Boolean.TRUE.equals(Compat.get(s, "isLocked"))));
            }
        }
        out.sort(Comparator.comparingLong(World::played).reversed());
        return out;
    }

    static boolean open(Minecraft mc, String levelId, Runnable onCancel) {
        Object flows = Compat.get(mc, "createWorldOpenFlows");
        return Compat.call(flows, new String[] {"openWorld"}, levelId, onCancel);
    }

    /** CreateWorldScreen.openFresh(minecraft, onCancel). */
    static boolean createNew(Minecraft mc, Runnable onCancel) {
        try {
            Class<?> c = Class.forName("net.minecraft.client.gui.screens.worldselection.CreateWorldScreen");
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals("openFresh") || !Modifier.isStatic(m.getModifiers())) continue;
                Class<?>[] pt = m.getParameterTypes();
                Object[] args = new Object[pt.length];
                boolean ok = true;
                for (int i = 0; i < pt.length && ok; i++) {
                    if (pt[i] == Minecraft.class) args[i] = mc;
                    else if (pt[i] == Runnable.class) args[i] = onCancel;
                    else if (!pt[i].isPrimitive()) args[i] = null;
                    else ok = false;
                }
                if (!ok) continue;
                m.setAccessible(true);
                m.invoke(null, args);
                return true;
            }
        } catch (Throwable t) {
            System.err.println("[carrotmods] could not open world creation: " + t);
        }
        return false;
    }
}
