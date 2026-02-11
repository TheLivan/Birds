package com.thelivan.birds.client;

public class BirdPos {

    private final int x, y, z;
    private final int texture;

    public BirdPos(int texture, int x, int y, int z) {
        this.texture = texture;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getZ() {
        return z;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public int getTexture() {
        return texture;
    }
}
