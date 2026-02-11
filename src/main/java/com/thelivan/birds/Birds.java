package com.thelivan.birds;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.myname.mymodid.Tags;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(modid = "birds", name = "Birds", version = Tags.VERSION)
public class Birds {

    public static final String MODID = "birds";

    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.thelivan.birds.client.ClientProxy", serverSide = "com.thelivan.birds.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent ev) {
        proxy.preInit(ev);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent ev) {
        proxy.init(ev);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent ev) {
        proxy.postInit(ev);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent ev) {
        proxy.serverStarting(ev);
    }
}
