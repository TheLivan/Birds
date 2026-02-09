package com.thelivan.birds.client;

import com.thelivan.birds.CommonProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent ev) {
        MinecraftForge.EVENT_BUS.register(new ClientEventHandler());
        super.preInit(ev);
    }
}
