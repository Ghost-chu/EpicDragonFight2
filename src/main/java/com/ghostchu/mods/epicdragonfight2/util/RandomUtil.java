package com.ghostchu.mods.epicdragonfight2.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

public class RandomUtil {
    private static final Random RANDOM = new Random();
    public static  <T> T randomPick(Collection<T> from){
        List<T> clone = new ArrayList<>(from);
        int selected = RANDOM.nextInt(clone.size());
        return clone.get(selected);
    }
}
