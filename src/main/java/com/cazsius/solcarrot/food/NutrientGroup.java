package com.cazsius.solcarrot.food;

/**
 * 营养分组。
 *
 * <p>上游 Spice of Life: Carrot Edition 只统计「吃过多少种食物」，没有分组概念 ——
 * 吃 30 种不同的蛋糕和吃 30 种均衡的饭菜收益一样。这显然不对。</p>
 *
 * <p>这里把食物按营养角色分成六组：玩家需要让<b>每一组</b>都有足够的种类，
 * 而不是只堆总量。均衡度因此成为独立于「食物种类数」的第二个维度。</p>
 */
public enum NutrientGroup {

    /** 蛋白质：肉、鱼、蛋、豆。 */
    PROTEIN("protein", "蛋白质"),
    /** 蔬菜：根茎、叶菜、菌类。 */
    VEGETABLE("vegetable", "蔬菜"),
    /** 水果：树果、瓜类。 */
    FRUIT("fruit", "水果"),
    /** 谷物：麦、米、面、面包。 */
    GRAIN("grain", "谷物"),
    /** 乳制品：奶、奶酪。 */
    DAIRY("dairy", "乳制品"),
    /** 其他：甜食、加工食品 —— 算种类但不计入均衡。 */
    OTHER("other", "其他");

    private final String id;
    private final String zhName;

    NutrientGroup(String id, String zhName) {
        this.id = id;
        this.zhName = zhName;
    }

    public String id() {
        return id;
    }

    public String zhName() {
        return zhName;
    }

    /** 参与均衡度计算的组（OTHER 不算，它只是记账用的桶）。 */
    public boolean countsForBalance() {
        return this != OTHER;
    }

    /** 参与均衡的组数量。 */
    public static int balancedGroupCount() {
        int count = 0;
        for (NutrientGroup group : values()) {
            if (group.countsForBalance()) {
                count++;
            }
        }
        return count;
    }
}
