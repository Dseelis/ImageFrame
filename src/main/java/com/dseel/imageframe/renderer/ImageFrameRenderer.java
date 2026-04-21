package com.dseel.imageframe.renderer;

import com.dseel.imageframe.client.ImageCache;
import com.dseel.imageframe.entity.ImageFrameEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
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

        ResourceLocation tex = ImageCache.getOrLoad(url);
        if (tex == null) tex = MissingTextureAtlasSprite.getLocation();

        float w = entity.getWidth();
        float h = entity.getHeight();
        Direction facing = entity.getFacingDirection();

        pose.pushPose();

        // Шаг 1: повернуть quad чтобы он смотрел в нужном направлении.
        // После поворота quad лежит в плоскости XY, нормаль = +Z (смотрит на игрока).
        switch (facing) {
            // Стена смотрит на SOUTH (+Z) — нормаль quad = +Z, поворот не нужен
            case SOUTH -> {}
            // Стена смотрит на NORTH (-Z) — поворот 180° вокруг Y
            case NORTH -> pose.mulPose(new Quaternionf().rotationY((float) Math.PI));
            // Стена смотрит на EAST (+X) — поворот -90° вокруг Y
            case EAST  -> pose.mulPose(new Quaternionf().rotationY((float) (-Math.PI / 2)));
            // Стена смотрит на WEST (-X) — поворот +90° вокруг Y
            case WEST  -> pose.mulPose(new Quaternionf().rotationY((float) (Math.PI / 2)));
            // Пол (UP) — поворот -90° вокруг X
            case UP    -> pose.mulPose(new Quaternionf().rotationX((float) (-Math.PI / 2)));
            // Потолок (DOWN) — поворот +90° вокруг X
            case DOWN  -> pose.mulPose(new Quaternionf().rotationX((float) (Math.PI / 2)));
        }

        // Шаг 2: сдвинуть quad вперёд от стены чтобы не было z-fighting.
        // После поворота "вперёд" = +Z в локальных координатах.
        // Смещаем на 3/16 блока вперёд (увеличь если всё равно z-fighting).
        pose.translate(0, 0, 3f / 16f);

        // Шаг 3: нарисовать quad центрированный на (0,0,0)
        VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucentCull(tex));
        Matrix4f mat = pose.last().pose();
        int overlay = OverlayTexture.NO_OVERLAY;

        float x0 = -w / 2f, x1 = w / 2f;
        float y0 = -h / 2f, y1 = h / 2f;

        vc.addVertex(mat, x0, y1, 0).setColor(255,255,255,255).setUv(0,0).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0,0,1);
        vc.addVertex(mat, x0, y0, 0).setColor(255,255,255,255).setUv(0,1).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0,0,1);
        vc.addVertex(mat, x1, y0, 0).setColor(255,255,255,255).setUv(1,1).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0,0,1);
        vc.addVertex(mat, x1, y1, 0).setColor(255,255,255,255).setUv(1,0).setOverlay(overlay).setLight(light).setNormal(pose.last(), 0,0,1);

        pose.popPose();

        super.render(entity, yaw, delta, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(ImageFrameEntity entity) {
        return MissingTextureAtlasSprite.getLocation();
    }
}
