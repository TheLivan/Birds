package com.thelivan.birds.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderWorldLastEvent;

import com.thelivan.birds.client.render.RenderBird;
import com.thelivan.birds.util.BlockPos;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ClientEventHandler {

    static final ClientEventHandler INSTANCE = new ClientEventHandler();
    static final Minecraft MC = Minecraft.getMinecraft();
    static final Random rnd = new Random();

    RenderBird renderBird = new RenderBird();
    List<BlockPos> blockPosList = new ArrayList<>();

    private ClientEventHandler() {}

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent ev) {
        if (MC.thePlayer != null && MC.thePlayer.ticksExisted % 100 == 0) {
            blockPosList.add(
                new BlockPos(
                    (int) MC.thePlayer.posX + (rnd.nextInt(100) - 50),
                    (int) MC.thePlayer.posY + 5,
                    (int) MC.thePlayer.posZ + (rnd.nextInt(100) - 50)));
        }
    }

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent ev) {
        EntityPlayer player = MC.thePlayer;

        float x = (float) (player.prevPosX + ((player.posX - player.prevPosX) * ev.partialTicks));
        float y = (float) (player.prevPosY + ((player.posY - player.prevPosY) * ev.partialTicks));
        float z = (float) (player.prevPosZ + ((player.posZ - player.prevPosZ) * ev.partialTicks));

        for (BlockPos blockPos : blockPosList) {
            renderBird.render(x, y, z, blockPos.getX(), blockPos.getY(), blockPos.getZ());
        }
    }
}
