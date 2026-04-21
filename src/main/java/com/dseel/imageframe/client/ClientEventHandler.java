package com.dseel.imageframe.client;

import com.dseel.imageframe.ImageFrameMod;
import com.dseel.imageframe.common.ModEntityTypes;
import com.dseel.imageframe.entity.ImageFrameEntity;
import com.dseel.imageframe.screen.ImageFrameScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;

@EventBusSubscriber(modid = ImageFrameMod.MODID, value = Dist.CLIENT)
public class ClientEventHandler {

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != 1 || event.getAction() != 1) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if (!player.isShiftKeyDown()) return;

        HitResult hit = mc.hitResult;
        if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;
        EntityHitResult entityHit = (EntityHitResult) hit;

        if (entityHit.getEntity() instanceof ImageFrameEntity frame) {
            mc.setScreen(new ImageFrameScreen(frame));
            event.setCanceled(true);
            return;
        }

        if (entityHit.getEntity() instanceof ItemFrame frame) {
            openSpawnScreen(frame.getDirection(), frame.position());
            event.setCanceled(true);
        } else if (entityHit.getEntity() instanceof Painting painting) {
            openSpawnScreen(painting.getDirection(), painting.position());
            event.setCanceled(true);
        }
    }

    private static void openSpawnScreen(Direction facing, Vec3 pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        ImageFrameEntity preview = new ImageFrameEntity(
                ModEntityTypes.IMAGE_FRAME.get(), mc.level);
        preview.setPos(pos.x, pos.y, pos.z);
        preview.setFacingDirection(facing);

    }
}
