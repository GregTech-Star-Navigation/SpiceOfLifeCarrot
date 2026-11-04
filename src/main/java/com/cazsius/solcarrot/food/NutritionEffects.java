package com.cazsius.solcarrot.food;

import com.cazsius.solcarrot.SOLCarrot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 营养不均衡的负面效果。
 *
 * <p>每 10 秒检查一次（不是每 tick —— 这个判定要遍历已食食物列表，没必要高频）。
 * 判据是 {@link NutritionApi#balance}：</p>
 *
 * <table>
 *   <tr><th>均衡度</th><th>效果</th></tr>
 *   <tr><td>&lt; 0.15</td><td>虚弱 I + 缓慢 I（30 秒）</td></tr>
 *   <tr><td>&lt; 0.30</td><td>缓慢 I（20 秒）</td></tr>
 *   <tr><td>≥ 0.80</td><td>饱和 I（10 秒）—— 均衡饮食的回报</td></tr>
 * </table>
 *
 * <p>刻意不用「掉血」：惩罚是<b>能力受限</b>，不是直接伤害。玩家该觉得「我最近吃得不对」，
 * 而不是「这 mod 在扣我血」。</p>
 */
@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID)
public final class NutritionEffects {

    /** 检查间隔（tick）。 */
    public static final int CHECK_INTERVAL = 200;

    private NutritionEffects() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL != 0) {
            return;
        }
        double balance = NutritionApi.balance(player);
        if (NutritionApi.total(player) < 3) {
            // 刚开始吃，别惩罚 —— 否则开局第一分钟就在挨罚。
            return;
        }

        if (balance < 0.15) {
            apply(player, MobEffects.WEAKNESS, 600, 0);
            apply(player, MobEffects.MOVEMENT_SLOWDOWN, 600, 0);
            notifyOnce(player, "营养严重失衡（缺" + NutritionApi.weakest(player).zhName() + "）");
        } else if (balance < 0.30) {
            apply(player, MobEffects.MOVEMENT_SLOWDOWN, 400, 0);
            notifyOnce(player, "营养失衡（缺" + NutritionApi.weakest(player).zhName() + "）");
        } else if (balance >= 0.80) {
            apply(player, MobEffects.SATURATION, 200, 0);
        }
    }

    private static void apply(ServerPlayer player, net.minecraft.world.effect.MobEffect effect, int ticks, int amplifier) {
        // 不加粒子、不加图标：这是环境状态，不是一次性事件。
        player.addEffect(new MobEffectInstance(effect, ticks, amplifier, true, false, false));
    }

    private static void notifyOnce(ServerPlayer player, String message) {
        if (player.tickCount % (CHECK_INTERVAL * 5) == 0) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
        }
    }
}
