package com.p1nero.multiplayer_boss_fight;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MultiplayerBossFightMod.MOD_ID)
public class MultiplayerBossManager {

    public static void init() {

    }

    @SubscribeEvent
    public static void onStartSeenByPlayer(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        Player player = event.getEntity();
        int range = target.getType().clientTrackingRange();
        //TODO 根据附近玩家数量修改数据
    }

    @SubscribeEvent
    public static void onStopSeenByPlayer(PlayerEvent.StopTracking event) {
        Entity target = event.getTarget();
        Player player = event.getEntity();
        int range = target.getType().clientTrackingRange();
        //TODO 根据附近玩家数量修改数据
    }
}
