package com.cazsius.solcarrot.food;

import com.cazsius.solcarrot.tracking.FoodInstance;
import com.cazsius.solcarrot.tracking.FoodList;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * 营养 API —— 本 fork 相对上游新增的核心。
 *
 * <p>上游只有一个数字「吃过多少种食物」。这里在它之上再算一层<b>均衡度</b>：
 * 把吃过的食物按 {@link NutrientGroup} 分组，看最弱的那组相对平均水平差多少。</p>
 *
 * <pre>
 * 均衡度 = min(各组种类数) / avg(各组种类数)      // 0 = 有一组完全没吃，1 = 完全均匀
 * </pre>
 *
 * <p>所以「吃 30 种甜食」的均衡度是 0，而不是满分 —— 这正是上游缺失的那一半。</p>
 */
public final class NutritionApi {

    private NutritionApi() {
    }

    /** 各营养组的已食种类数。 */
    public static Map<NutrientGroup, Integer> counts(Player player) {
        Map<NutrientGroup, Integer> counts = new EnumMap<>(NutrientGroup.class);
        for (NutrientGroup group : NutrientGroup.values()) {
            counts.put(group, 0);
        }
        if (player == null) {
            return counts;
        }
        try {
            FoodList list = FoodList.get(player);
            for (FoodInstance food : list.getEatenFoods()) {
                counts.merge(FoodGroups.of(food.item), 1, Integer::sum);
            }
        } catch (RuntimeException e) {
            // 能力还没挂上（刚登录那一帧）：按空处理，不要炸掉调用方的 tick。
            return counts;
        }
        return counts;
    }

    /** 某一组的种类数。 */
    public static int countIn(Player player, NutrientGroup group) {
        return counts(player).getOrDefault(group, 0);
    }

    /** 均衡度，0–1。还没开始吃任何东西时为 0。 */
    public static double balance(Player player) {
        Map<NutrientGroup, Integer> counts = counts(player);
        int min = Integer.MAX_VALUE;
        int sum = 0;
        int groups = 0;
        for (NutrientGroup group : NutrientGroup.values()) {
            if (!group.countsForBalance()) {
                continue;
            }
            int value = counts.get(group);
            min = Math.min(min, value);
            sum += value;
            groups++;
        }
        if (groups == 0 || sum == 0) {
            return 0.0;
        }
        double average = sum / (double) groups;
        return Math.max(0.0, Math.min(1.0, min / average));
    }

    /** 当前最弱的营养组（用于提示玩家「你缺蔬菜」）。 */
    public static NutrientGroup weakest(Player player) {
        Map<NutrientGroup, Integer> counts = counts(player);
        NutrientGroup weakest = NutrientGroup.OTHER;
        int lowest = Integer.MAX_VALUE;
        for (NutrientGroup group : NutrientGroup.values()) {
            if (!group.countsForBalance()) {
                continue;
            }
            int value = counts.get(group);
            if (value < lowest) {
                lowest = value;
                weakest = group;
            }
        }
        return weakest;
    }

    /** 已食种类总数（与上游的 {@code getEatenFoodCount()} 一致）。 */
    public static int total(Player player) {
        int sum = 0;
        for (int value : counts(player).values()) {
            sum += value;
        }
        return sum;
    }

    /** 一句话摘要（调试命令 / 提示用）。 */
    public static String describe(Player player) {
        StringBuilder sb = new StringBuilder();
        counts(player).forEach((group, count) -> {
            if (group.countsForBalance()) {
                sb.append(group.zhName()).append(' ').append(count).append("  ");
            }
        });
        sb.append("｜均衡度 ").append(String.format("%.0f%%", balance(player) * 100));
        return sb.toString();
    }
}
