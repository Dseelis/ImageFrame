package com.dseel.imageframe.renderer;

import com.dseel.imageframe.client.ImageCache;
import com.dseel.imageframe.entity.ImageFrameEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

public class ImageFrameRenderer extends EntityRenderer<ImageFrameEntity> {

    public ImageFrameRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(ImageFrameEntity entity, float yaw, float delta,
                       PoseStack pose, MultiBufferSource buffers, int light) {

        String url = entity.getImageUrl();
        if (url == null || url.isEmpty()) return;

        ImageCache.TextureData data = ImageCache.getTexture(url, entity.getStartTime());
        ResourceLocation tex = (data != null) ? data.location() : MissingTextureAtlasSprite.getLocation();

        pose.pushPose();

        switch (entity.getFacingDirection()) {
            case NORTH -> pose.mulPose(new Quaternionf().rotationY((float)Math.PI));
            case SOUTH -> {}
            case EAST  -> pose.mulPose(new Quaternionf().rotationY((float)(-Math.PI / 2)));
            case WEST  -> pose.mulPose(new Quaternionf().rotationY((float)(Math.PI / 2)));
            case UP    -> pose.mulPose(new Quaternionf().rotationX((float)(-Math.PI / 2)));
            case DOWN  -> pose.mulPose(new Quaternionf().rotationX((float)(Math.PI / 2)));
        }

        pose.translate(0, 0, 0.001f);

        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(tex));
        Matrix4f mat = pose.last().pose();

        float w = entity.getWidth();
        float h = entity.getHeight();

        float hw = w / 2f;
        float hh = h / 2f;

        float u0 = 0, u1 = 1;
        if (entity.isMirrored()) {
            u0 = 1;
            u1 = 0;
        }

        int lightVal = LightTexture.FULL_BRIGHT;

        vc.addVertex(mat, -hw,  hh, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightVal)
                .setNormal(0, 0, 1);

        vc.addVertex(mat, -hw, -hh, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightVal)
                .setNormal(0, 0, 1);

        vc.addVertex(mat,  hw, -hh, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightVal)
                .setNormal(0, 0, 1);

        vc.addVertex(mat,  hw,  hh, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(lightVal)
                .setNormal(0, 0, 1);

        pose.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(ImageFrameEntity entity) {
        return MissingTextureAtlasSprite.getLocation();
    }
}