package com.thelivan.birds.client;

import net.minecraftforge.common.MinecraftForge;

import com.thelivan.birds.CommonProxy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent ev) {
        MinecraftForge.EVENT_BUS.register(ClientEventHandler.INSTANCE);
        FMLCommonHandler.instance()
            .bus()
            .register(ClientEventHandler.INSTANCE);
        super.preInit(ev);
    }
}
