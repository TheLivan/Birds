package com.thelivan.birds.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import com.thelivan.birds.Birds;

public class RenderBird {

    static Minecraft MC = Minecraft.getMinecraft();

    static final ResourceLocation BIRD_1 = new ResourceLocation(Birds.MODID, "textures/bird_1.png");
    static final ResourceLocation BIRD_2 = new ResourceLocation(Birds.MODID, "textures/bird_2.png");
    static final ResourceLocation BIRD_3 = new ResourceLocation(Birds.MODID, "textures/bird_3.png");
    static final ResourceLocation BIRD_4 = new ResourceLocation(Birds.MODID, "textures/bird_4.png");
    static final ResourceLocation BIRD_5 = new ResourceLocation(Birds.MODID, "textures/bird_5.png");

    public void render(float x, float y, float z, float birdX, float birdY, float birdZ) {
        GL11.glPushMatrix();
        GL11.glTranslated(-x, -y, -z);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glTranslated(birdX, birdY, birdZ);
        GL11.glRotatef(90, -1F, 0F, 0F);
        GL11.glRotatef(180, 0F, 0F, 1F);
        int birdSize = 1;
        draw(BIRD_2, -birdSize / 2f, -birdSize, birdSize, birdSize, 1.0f);
        GL11.glRotatef(180, 1F, 0F, 0F);
        GL11.glRotatef(180, 0F, 0F, 1F);
        draw(BIRD_2, -birdSize / 2f, -birdSize, birdSize, birdSize, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    static void draw(
        ResourceLocation texture,
        double textureX,
        double textureY,
        double width,
        double height,
        float alpha
    ) {
        MC.getTextureManager()
            .bindTexture(texture);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, alpha);
        Tessellator tes = Tessellator.instance;
        tes.startDrawingQuads();
        tes.setColorOpaque(255, 255, 255);
        tes.setColorRGBA(255, 255, 255, 255);
        tes.addVertexWithUV(textureX, textureY + height, 0.0, 0.0, 1.0);
        tes.addVertexWithUV(textureX + width, textureY + height, 0.0, 1.0, 1.0);
        tes.addVertexWithUV(textureX + width, textureY, 0.0, 1.0, 0.0);
        tes.addVertexWithUV(textureX, textureY, 0.0, 0.0, 0.0);
        tes.draw();
    }
}
