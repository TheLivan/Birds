package com.thelivan.birds.client.render;

import net.minecraft.util.ResourceLocation;

import com.thelivan.birds.client.ClientBird;

public class BirdTexture {

    public static ResourceLocation get(ClientBird b) {
        return (b != null) ? b.texture : null;
    }
}
