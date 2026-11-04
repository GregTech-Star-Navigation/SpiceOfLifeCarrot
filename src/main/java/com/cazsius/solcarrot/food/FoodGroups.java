package com.cazsius.solcarrot.food;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 食物 → 营养分组 的映射。
 *
 * <p>没有现成的营养 tag 可用（1.20.1 生态里没有统一标准），所以用「显式表 + 启发式」
 * 两层：常见食物列在表里，表外的按物品 id 里的关键词猜，猜不出来归 {@link NutrientGroup#OTHER}。</p>
 *
 * <p>整合包作者可以用 {@link #register(String, NutrientGroup)} 覆盖任何一条 —— 这比改代码省事。</p>
 */
public final class FoodGroups {

    private static final Map<String, NutrientGroup> OVERRIDES = new HashMap<>();

    /** 常见食物的显式分组。 */
    private static final Map<String, NutrientGroup> DEFAULTS = buildDefaults();

    private FoodGroups() {
    }

    private static Map<String, NutrientGroup> buildDefaults() {
        Map<String, NutrientGroup> map = new HashMap<>();

        // 蛋白质
        for (String id : new String[]{
                "minecraft:cooked_beef", "minecraft:beef", "minecraft:cooked_porkchop", "minecraft:porkchop",
                "minecraft:cooked_chicken", "minecraft:chicken", "minecraft:cooked_mutton", "minecraft:mutton",
                "minecraft:cooked_rabbit", "minecraft:rabbit", "minecraft:rabbit_stew",
                "minecraft:cooked_cod", "minecraft:cod", "minecraft:cooked_salmon", "minecraft:salmon",
                "minecraft:tropical_fish", "minecraft:pufferfish", "minecraft:egg",
                "minecraft:spider_eye", "minecraft:rotten_flesh"}) {
            map.put(id, NutrientGroup.PROTEIN);
        }

        // 蔬菜
        for (String id : new String[]{
                "minecraft:carrot", "minecraft:golden_carrot", "minecraft:potato", "minecraft:baked_potato",
                "minecraft:beetroot", "minecraft:beetroot_soup", "minecraft:dried_kelp",
                "minecraft:brown_mushroom", "minecraft:red_mushroom", "minecraft:mushroom_stew",
                "minecraft:suspicious_stew", "minecraft:crimson_fungus", "minecraft:warped_fungus"}) {
            map.put(id, NutrientGroup.VEGETABLE);
        }

        // 水果
        for (String id : new String[]{
                "minecraft:apple", "minecraft:golden_apple", "minecraft:enchanted_golden_apple",
                "minecraft:melon_slice", "minecraft:sweet_berries", "minecraft:glow_berries",
                "minecraft:chorus_fruit", "minecraft:popped_chorus_fruit"}) {
            map.put(id, NutrientGroup.FRUIT);
        }

        // 谷物
        for (String id : new String[]{
                "minecraft:bread", "minecraft:wheat", "minecraft:cookie", "minecraft:cake",
                "minecraft:pumpkin_pie", "minecraft:honey_bottle"}) {
            map.put(id, NutrientGroup.GRAIN);
        }

        // 乳制品（原版只有蛋糕含奶，模组食物靠关键词）
        map.put("minecraft:milk_bucket", NutrientGroup.DAIRY);

        return map;
    }

    /** 覆盖某条映射。 */
    public static void register(String itemId, NutrientGroup group) {
        if (itemId == null || group == null) {
            return;
        }
        OVERRIDES.put(itemId, group);
    }

    public static void clearOverrides() {
        OVERRIDES.clear();
    }

    /** 取某个物品的营养分组。 */
    public static NutrientGroup of(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return NutrientGroup.OTHER;
        }
        return of(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static NutrientGroup of(Item item) {
        return item == null ? NutrientGroup.OTHER : of(BuiltInRegistries.ITEM.getKey(item));
    }

    public static NutrientGroup of(ResourceLocation id) {
        if (id == null) {
            return NutrientGroup.OTHER;
        }
        String key = id.toString();
        NutrientGroup override = OVERRIDES.get(key);
        if (override != null) {
            return override;
        }
        NutrientGroup known = DEFAULTS.get(key);
        if (known != null) {
            return known;
        }
        return guess(key);
    }

    /**
     * 表外的启发式：按 id 里的词猜。
     *
     * <p>宁可猜错也不要一律归 OTHER —— 一律归 OTHER 会让大量模组食物完全不参与均衡，
     * 那等于给整合包开了个「吃垃圾食品也不掉血」的后门。</p>
     */
    private static NutrientGroup guess(String itemId) {
        String path = itemId.contains(":") ? itemId.substring(itemId.indexOf(':') + 1) : itemId;

        if (containsAny(path, "cheese", "milk", "cream", "yogurt", "butter")) {
            return NutrientGroup.DAIRY;
        }
        if (containsAny(path, "raw_", "_meat", "beef", "pork", "chicken", "mutton", "fish", "bacon",
                "sausage", "ham", "egg", "tofu", "bean", "nut")) {
            return NutrientGroup.PROTEIN;
        }
        if (containsAny(path, "bread", "rice", "flour", "dough", "pasta", "noodle", "cereal",
                "grain", "oat", "wheat", "bun", "toast")) {
            return NutrientGroup.GRAIN;
        }
        if (containsAny(path, "berry", "berries", "apple", "orange", "pear", "peach", "cherry",
                "grape", "melon", "banana", "lemon", "fruit", "plum", "mango")) {
            return NutrientGroup.FRUIT;
        }
        if (containsAny(path, "carrot", "potato", "beet", "cabbage", "lettuce", "onion", "garlic",
                "tomato", "pepper", "corn", "pumpkin", "mushroom", "kelp", "spinach", "broccoli")) {
            return NutrientGroup.VEGETABLE;
        }
        if (containsAny(path, "cake", "cookie", "candy", "chocolate", "sugar", "pie", "sweet", "jam")) {
            return NutrientGroup.OTHER;
        }
        return NutrientGroup.OTHER;
    }

    private static boolean containsAny(String path, String... needles) {
        for (String needle : needles) {
            if (path.contains(needle)) {
                return true;
            }
        }
        return false;
    }
}
