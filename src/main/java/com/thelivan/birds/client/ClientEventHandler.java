package com.thelivan.birds.client;

import com.thelivan.birds.client.render.RenderBird;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ClientEventHandler {

    static ClientEventHandler INSTANCE = new ClientEventHandler();
    static Minecraft MC = Minecraft.getMinecraft();
    static Random rnd = new Random();

    RenderBird renderBird = new RenderBird();
    List<BirdPos> birdPosList = new ArrayList<>();

    private ClientEventHandler() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev) {
        if (MC.thePlayer != null && MC.thePlayer.ticksExisted % 100 == 0) {
            birdPosList.add(
                new BirdPos(
                    rnd.nextInt(10),
                    (int) MC.thePlayer.posX + (rnd.nextInt(100) - 50),
                    (int) MC.thePlayer.posY + 40,
                    (int) MC.thePlayer.posZ + (rnd.nextInt(100) - 50)
                )
            );
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        EntityPlayer player = MC.thePlayer;

        float x = (float) (player.prevPosX + ((player.posX - player.prevPosX) * ev.partialTicks));
        float y = (float) (player.prevPosY + ((player.posY - player.prevPosY) * ev.partialTicks));
        float z = (float) (player.prevPosZ + ((player.posZ - player.prevPosZ) * ev.partialTicks));

        for (BirdPos birdPos : birdPosList) {
            renderBird.render(x, y, z, birdPos.getX(), birdPos.getY(), birdPos.getZ());
        }
    }
}
