package com.thelivan.birds.client.render;

import com.thelivan.birds.Birds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class RenderBird {
    static Minecraft MC = Minecraft.getMinecraft();

    static final ResourceLocation BIRD_1 = new ResourceLocation(Birds.MODID, "textures/bird_1.png");
    static final ResourceLocation BIRD_2 = new ResourceLocation(Birds.MODID, "textures/bird_2.png");

    public void render(float x, float y, float z) {
        GL11.glPushMatrix();
        GL11.glTranslated(-x, -y, -z);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GL11.glTranslated(x + 0.5D, y + 0.22D, z + 0.5D);
        GL11.glRotatef(-RenderManager.instance.playerViewY, 0F, 1.0F, 0.0F);
        GL11.glRotatef(-RenderManager.instance.playerViewX, -1F, 0.0F, 0.0F);
        GL11.glRotatef(180, 0F, 0.0F, 1.0F);
        GL11.glScaled(0.032f, 0.032f, 0.032f);
        draw(BIRD_1, -20 / 2f, -20 + 7, 20, 20, 1.0f);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
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
        MC.getTextureManager().bindTexture(texture);
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
