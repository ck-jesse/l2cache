package com.github.l2cache.test;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author chenck
 * @date 2021/3/24 21:54
 */
public class Test4 {

    public static void main(String[] args) {
        Map<String, String> l1KeyMap = new HashMap<>();
        l1KeyMap.put("key1", "key1");
        l1KeyMap.put("key2", "key2");
//        l1KeyMap.put("key3", null);
        l1KeyMap.put("key4", "key4");

        Map<String, Object> l2HitMap = new HashMap<>();
        l2HitMap.put("key1", 1);
        l2HitMap.put("key2", 2);
        l2HitMap.put("key3", 3);
        l2HitMap.put("key4", null);
        Map<String, Object> l2HitMapTemp = l2HitMap.entrySet().stream()
                .filter(entry -> l1KeyMap.containsKey(entry.getKey()))
//                .collect(Collectors.toMap(entry -> l1KeyMap.get(entry.getKey()), entry -> entry.getValue()));
                .collect(HashMap::new, (map, entry) -> map.put(l1KeyMap.get(entry.getKey()), entry.getValue()), HashMap::putAll);
        System.out.println(l2HitMapTemp);
    }
}
