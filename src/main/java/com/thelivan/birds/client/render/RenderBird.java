package com.thelivan.birds.client.render;

import java.util.Collection;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.thelivan.birds.client.ClientBird;
import com.thelivan.birds.util.Vec3d;

public class RenderBird {

    public static void renderAll(Collection<ClientBird> birds, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || birds.isEmpty()) return;

        RenderManager rm = RenderManager.instance;
        Vec3d camPos = new Vec3d(rm.viewerPosX, rm.viewerPosY, rm.viewerPosZ);

        // Save current fog enabled state so we don't break the world renderer
        boolean fogWasEnabled = GL11.glIsEnabled(GL11.GL_FOG);

        GL11.glPushMatrix();

        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper
            .glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDisable(GL11.GL_CULL_FACE);

        GL11.glDisable(GL11.GL_LIGHTING);
        mc.entityRenderer.enableLightmap(0.0D);

        // Only enable fog for our draw, then restore
        GL11.glEnable(GL11.GL_FOG);

        for (ClientBird b : birds) {
            renderOne(mc, b, camPos, partialTicks);
        }

        // Restore fog exactly as it was
        if (!fogWasEnabled) GL11.glDisable(GL11.GL_FOG);

        mc.entityRenderer.disableLightmap(0.0D);

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL11.glPopMatrix();
    }

    private static void renderOne(Minecraft mc, ClientBird b, Vec3d camPos, float partialTicks) {
        Vec3d p0 = (b.prevPos != null) ? b.prevPos : b.pos;
        Vec3d p1 = b.pos;

        double ix = p0.x + (p1.x - p0.x) * partialTicks;
        double iy = p0.y + (p1.y - p0.y) * partialTicks;
        double iz = p0.z + (p1.z - p0.z) * partialTicks;

        double x = ix - camPos.x;
        double y = iy - camPos.y;
        double z = iz - camPos.z;

        ResourceLocation tex = BirdTexture.get(b);
        if (tex == null) {
            // No texture => skip drawing rather than crash
            return;
        }

        float alpha = fogFadeAlpha(mc, camPos, ix, iy, iz);
        if (alpha <= 0.01f) return;

        mc.getTextureManager()
            .bindTexture(tex);

        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);

        GL11.glPushMatrix();
        GL11.glTranslated(x, y, z);

        double scale = b.species.scale;
        GL11.glScaled(scale, scale, scale);

        // Anti-invisibility trick: tilt the quad slightly toward the camera.
        // The further away and the closer the camera is to the bird's altitude, the stronger the tilt.
        double cx = camPos.x - ix;
        double cy = camPos.y - iy;
        double cz = camPos.z - iz;

        double dist = Math.sqrt(cx * cx + cy * cy + cz * cz);

        double invLen = (dist > 1e-6) ? (1.0 / dist) : 0.0;
        double dxN = cx * invLen;
        double dyN = cy * invLen;
        double dzN = cz * invLen;

        // 0 when close, 1 when far
        float distFactor = (float) clamp01((dist - 12.0) / 64.0);

        // stronger when the camera is at a similar altitude
        float horizonFactor = 1.0f - (float) Math.min(1.0, Math.abs(dyN));

        float tiltStrength = distFactor * horizonFactor;
        float maxTiltDeg = 25.0f;

        float tiltX = (float) (dzN * maxTiltDeg * tiltStrength);
        float tiltZ = (float) (-dxN * maxTiltDeg * tiltStrength);

        // Applied BEFORE yaw/pitch/roll so it works in world space
        GL11.glRotatef(tiltZ, 0f, 0f, 1f);
        GL11.glRotatef(tiltX, 1f, 0f, 0f);

        // Apply lightmap brightness based on the bird's world position.
        // 1.7.10 packs it as (sky << 20 | block << 4); out-of-range Y and unloaded chunks fall back to a default.
        int packedLight = mc.theWorld
            .getLightBrightnessForSkyBlocks((int) Math.floor(ix), (int) Math.floor(iy), (int) Math.floor(iz), 0);

        int lightU = packedLight & 0xFFFF;
        int lightV = (packedLight >>> 16) & 0xFFFF;
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lightU, lightV);

        float yaw = lerpAngle(b.prevYaw, b.orientation.yawDeg, partialTicks);
        float pitch = lerpAngle(b.prevPitch, b.orientation.pitchDeg, partialTicks);
        float roll = lerpAngle(b.prevRoll, b.orientation.rollDeg, partialTicks);

        // Quad is flat in the XZ plane with forward = +Z (top of the PNG = head).
        GL11.glRotatef(yaw, 0f, 1f, 0f);
        GL11.glRotatef(pitch, 1f, 0f, 0f);
        GL11.glRotatef(roll, 0f, 0f, 1f);

        // Small "flap" / wing wobble: vary width slightly
        double t = mc.theWorld.getTotalWorldTime() + partialTicks;
        double amp = b.species.flapAmplitude;
        double spd = b.species.flapSpeed;
        double flap = amp * Math.sin((t + (b.getId() & 255L)) * spd);

        double halfW = 1.2 + flap; // wings (left-right, X)
        double halfL = 0.7; // length (tail->head, Z)

        double zHead = +halfL;
        double zTail = -halfL;

        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        tess.setColorRGBA_F(1.0f, 1.0f, 1.0f, alpha);
        tess.addVertexWithUV(-halfW, 0.0, zHead, 0.0, 0.0); // left-head
        tess.addVertexWithUV(+halfW, 0.0, zHead, 1.0, 0.0); // right-head
        tess.addVertexWithUV(+halfW, 0.0, zTail, 1.0, 1.0); // right-tail
        tess.addVertexWithUV(-halfW, 0.0, zTail, 0.0, 1.0); // left-tail
        tess.draw();

        GL11.glPopMatrix();
    }

    private static float fogFadeAlpha(Minecraft mc, Vec3d camPos, double wx, double wy, double wz) {
        double dx = wx - camPos.x;
        double dy = wy - camPos.y;
        double dz = wz - camPos.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

        double fogEnd = mc.gameSettings.renderDistanceChunks * 16.0;
        boolean underwater = mc.thePlayer != null && mc.thePlayer.isInsideOfMaterial(Material.water);

        double fogStart = underwater ? fogEnd * 0.10 : fogEnd * 0.65;
        fogEnd = underwater ? fogEnd * 0.35 : fogEnd;

        double a = (fogEnd - dist) / (fogEnd - fogStart);
        if (a < 0) a = 0;
        if (a > 1) a = 1;
        if (underwater) a *= 0.7; // extra haze on top of the shorter range

        return (float) a;
    }

    private static double clamp01(double v) {
        if (v < 0.0) return 0.0;
        if (v > 1.0) return 1.0;
        return v;
    }

    private static float lerpAngle(float a, float b, float t) {
        float delta = wrapDegrees(b - a);
        return a + delta * t;
    }

    private static float wrapDegrees(float deg) {
        deg = deg % 360.0f;
        if (deg >= 180.0f) deg -= 360.0f;
        if (deg < -180.0f) deg += 360.0f;
        return deg;
    }
}
