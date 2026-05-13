package com.p1nero.multiplayer_boss_fight;

import com.mojang.logging.LogUtils;
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
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        MultiplayerBossManager.init();
    }

}
