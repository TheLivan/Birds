package com.thelivan.birds.client.sound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.util.ResourceLocation;

import com.thelivan.birds.client.ClientBird;

/**
 * A one-shot bird call that follows a {@link ClientBird} and fades with distance.
 * <p>
 * Implements {@link ITickableSound} directly instead of extending {@code PositionedSound}/{@code MovingSound}: those
 * base classes expose several of their fields under unmapped SRG names in this MCP mapping version, so implementing
 * the interface directly keeps every field name under our control.
 */
public class BirdCallSound implements ITickableSound {

    private static final int OUT_OF_RANGE_GRACE_TICKS = 20; // ~1s, avoids a hard cut right at maxDist
    private static final float STOP_HYSTERESIS = 4.0f; // extra blocks past maxDist before we start the grace timer

    private final ResourceLocation location;
    private final ClientBird bird;
    private final long birdId;

    private final float baseVolume;
    private final float basePitch;
    private final float maxDist;
    private final float fadeStart;
    private final float fadePower;

    private float xPosF;
    private float yPosF;
    private float zPosF;
    private float volume;
    private float pitch;

    private boolean donePlaying = false;
    private int outOfRangeTicks = 0;

    public BirdCallSound(ClientBird bird, ResourceLocation location, float volume, float pitch, float maxDist,
        float fadeStart, float fadePower) {
        this.bird = bird;
        this.birdId = bird.getId();
        this.location = location;

        this.baseVolume = volume;
        this.basePitch = pitch;
        this.maxDist = Math.max(0.001f, maxDist);
        this.fadeStart = Math.max(0f, Math.min(fadeStart, this.maxDist));
        this.fadePower = Math.max(0.01f, fadePower);

        this.xPosF = (float) bird.pos.x;
        this.yPosF = (float) bird.pos.y;
        this.zPosF = (float) bird.pos.z;

        this.pitch = basePitch;
        this.volume = baseVolume;
    }

    @Override
    public void update() {
        if (bird == null || bird.pos == null) {
            donePlaying = true;
            return;
        }

        xPosF = (float) bird.pos.x;
        yPosF = (float) bird.pos.y;
        zPosF = (float) bird.pos.z;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) return;

        double dx = xPosF - mc.thePlayer.posX;
        double dy = yPosF - (mc.thePlayer.posY + mc.thePlayer.getEyeHeight());
        double dz = zPosF - mc.thePlayer.posZ;
        float dist = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        volume = baseVolume * fadeGain(dist);

        if (dist > maxDist + STOP_HYSTERESIS) {
            if (++outOfRangeTicks >= OUT_OF_RANGE_GRACE_TICKS) donePlaying = true;
        } else {
            outOfRangeTicks = 0;
        }
    }

    private float fadeGain(float dist) {
        if (dist <= fadeStart) return 1.0f;

        float denom = Math.max(0.001f, maxDist - fadeStart);
        float t = clamp01((dist - fadeStart) / denom);
        return (float) Math.pow(1.0f - t, fadePower);
    }

    private static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    public long getBirdId() {
        return birdId;
    }

    @Override
    public boolean isDonePlaying() {
        return donePlaying;
    }

    @Override
    public ResourceLocation getPositionedSoundLocation() {
        return location;
    }

    @Override
    public boolean canRepeat() {
        return false;
    }

    @Override
    public int getRepeatDelay() {
        return 0;
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public float getXPosF() {
        return xPosF;
    }

    @Override
    public float getYPosF() {
        return yPosF;
    }

    @Override
    public float getZPosF() {
        return zPosF;
    }

    @Override
    public ISound.AttenuationType getAttenuationType() {
        return ISound.AttenuationType.LINEAR;
    }
}
