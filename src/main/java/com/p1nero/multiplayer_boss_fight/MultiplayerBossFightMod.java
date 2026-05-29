package com.p1nero.multiplayer_boss_fight;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MultiplayerBossFightMod.MOD_ID)
public class MultiplayerBossFightMod {

    public static final String MOD_ID = "multiplayer_boss_fight";
    private static final Logger LOGGER = LogUtils.getLogger();


    public MultiplayerBossFightMod(FMLJavaModLoadingContext context) {
        IEventBus bus = context.getModEventBus();
        bus.addListener(this::onCommonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::onStartSeenByPlayer);
        MinecraftForge.EVENT_BUS.addListener(this::onStopSeenByPlayer);
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
