package com.carrot.mods;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Saved servers (servers.dat) and joining, reached by reflection so a rename can't break the build. */
final class Servers {
    record Entry(String name, String ip) {}

    private Servers() {}

    private static Object list(Minecraft mc) {
        try {
            Object l = Class.forName("net.minecraft.client.multiplayer.ServerList")
                .getConstructor(Minecraft.class).newInstance(mc);
            Compat.invoke(l, "load");
            return l;
        } catch (Throwable t) {
            return null;
        }
    }

    static List<Entry> all(Minecraft mc) {
        List<Entry> out = new ArrayList<>();
        Object l = list(mc);
        if (l == null) return out;
        Object size = Compat.invoke(l, "size");
        int n = size instanceof Integer i ? i : 0;
        for (int i = 0; i < n; i++) {
            Object d = Compat.invoke(l, "get", i);
            Object name = Compat.field(d, "name");
            Object ip = Compat.field(d, "ip");
            if (ip != null) out.add(new Entry(name == null ? String.valueOf(ip) : String.valueOf(name), String.valueOf(ip)));
        }
        return out;
    }

    private static Object makeData(String name, String ip) throws ReflectiveOperationException {
        Class<?> sd = Class.forName("net.minecraft.client.multiplayer.ServerData");
        for (Constructor<?> k : sd.getConstructors()) {
            Class<?>[] t = k.getParameterTypes();
            if (t.length == 3 && t[0] == String.class && t[1] == String.class && t[2].isEnum()) {
                Object other = null;
                for (Object c : t[2].getEnumConstants()) {
                    if (((Enum<?>) c).name().equals("OTHER")) other = c;
                }
                return k.newInstance(name, ip, other);
            }
        }
        return null;
    }

    static boolean add(Minecraft mc, String name, String ip) {
        try {
            Object l = list(mc);
            Object data = makeData(name.isBlank() ? ip : name, ip);
            if (l == null || data == null) return false;
            Compat.call(l, new String[] {"add"}, data, Boolean.FALSE);
            Compat.call(l, new String[] {"save"});
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    static boolean remove(Minecraft mc, int index) {
        Object l = list(mc);
        if (l == null) return false;
        Object d = Compat.invoke(l, "get", index);
        if (d == null) return false;
        Compat.call(l, new String[] {"remove"}, d);
        return Compat.call(l, new String[] {"save"});
    }

    /** ConnectScreen.startConnecting, with arguments picked by parameter type. */
    static boolean connect(Minecraft mc, Screen from, String name, String ip) {
        try {
            Object data = makeData(name, ip);
            Object addr = Class.forName("net.minecraft.client.multiplayer.resolver.ServerAddress")
                .getMethod("parseString", String.class).invoke(null, ip);
            Class<?> cs = Class.forName("net.minecraft.client.gui.screens.ConnectScreen");
            for (Method m : cs.getDeclaredMethods()) {
                if (!m.getName().equals("startConnecting") || !Modifier.isStatic(m.getModifiers())) continue;
                Class<?>[] pt = m.getParameterTypes();
                Object[] args = new Object[pt.length];
                boolean ok = true;
                for (int i = 0; i < pt.length && ok; i++) {
                    Class<?> c = pt[i];
                    if (c == Screen.class) args[i] = from;
                    else if (c == Minecraft.class) args[i] = mc;
                    else if (c.isInstance(addr) && c != Object.class) args[i] = addr;
                    else if (data != null && c.isInstance(data) && c != Object.class) args[i] = data;
                    else if (c == boolean.class) args[i] = Boolean.FALSE;
                    else if (!c.isPrimitive()) args[i] = null;
                    else ok = false;
                }
                if (!ok) continue;
                m.setAccessible(true);
                m.invoke(null, args);
                return true;
            }
        } catch (Throwable t) {
            System.err.println("[carrotmods] could not connect to " + ip + ": " + t);
        }
        return false;
    }
}
