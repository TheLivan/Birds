package com.thelivan.birds.client;

import com.thelivan.birds.client.render.RenderBird;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;

public class ClientEventHandler {

    static ClientEventHandler INSTANCE = new ClientEventHandler();
    static Minecraft MC = Minecraft.getMinecraft();

    RenderBird renderBird = new RenderBird();

    private ClientEventHandler() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev) {

    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        EntityPlayer player = MC.thePlayer;

        float x = (float) (player.prevPosX + ((player.posX - player.prevPosX) * ev.partialTicks));
        float y = (float) (player.prevPosY + ((player.posY - player.prevPosY) * ev.partialTicks));
        float z = (float) (player.prevPosZ + ((player.posZ - player.prevPosZ) * ev.partialTicks));

        renderBird.render(x, y, z);
    }
}
