package com.ghostchu.mods.epicdragonfight2.util;

import java.util.*;

public class RandomUtil {
    private static final Random RANDOM = new Random();

    public static <T> T randomPick(Collection<T> from) {
        if (from.isEmpty()) return null;
        List<T> clone = new ArrayList<>(from);
        int selected = RANDOM.nextInt(clone.size());
        return clone.get(selected);
    }

    public static <T> List<T> randomPick(List<T> from, int amount) {
        Collections.shuffle(from);
        List<T> picked = new ArrayList<>();
        Iterator<T> it = from.iterator();
        int i = 0;
        while (it.hasNext() && i < amount) {
            T t = it.next();
            picked.add(t);
            i++;
        }
        return picked;
    }
}
