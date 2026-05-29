package com.p1nero.multiplayer_boss_fight;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;

@Mod(MultiplayerBossFightMod.MOD_ID)
public class MultiplayerBossFightMod {

    public static final String MOD_ID = "multiplayer_boss_fight";
    private static final Logger LOGGER = LogUtils.getLogger();


    public MultiplayerBossFightMod(IEventBus modEventBus) {
        modEventBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onStartSeenByPlayer);
        NeoForge.EVENT_BUS.addListener(this::onStopSeenByPlayer);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("Initializing {}", MOD_ID);
            MultiplayerBossManager.init();
        });
    }

    private void onStartSeenByPlayer(PlayerEvent.StartTracking event) {
        MultiplayerBossManager.refreshBossAttributes(event.getTarget(), event.getEntity(), true);
    }

    private void onStopSeenByPlayer(PlayerEvent.StopTracking event) {
        MultiplayerBossManager.refreshBossAttributes(event.getTarget(), event.getEntity(), false);
    }

}
