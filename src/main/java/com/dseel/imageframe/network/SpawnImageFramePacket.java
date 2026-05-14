package com.dseel.imageframe.network;

import com.dseel.imageframe.common.ModEntityTypes;
import com.dseel.imageframe.entity.ImageFrameEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SpawnImageFramePacket(
        int frameId,
        String url,
        int width,
        int height,
        boolean mirrored
) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("imageframe", "spawn_image_frame");

    public static final Type<SpawnImageFramePacket> TYPE = new Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SpawnImageFramePacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeInt(pkt.frameId());
                        buf.writeUtf(pkt.url(), 512);
                        buf.writeInt(pkt.width());
                        buf.writeInt(pkt.height());
                        buf.writeBoolean(pkt.mirrored());
                    },
                    buf -> new SpawnImageFramePacket(
                            buf.readInt(),
                            buf.readUtf(512),
                            buf.readInt(),
                            buf.readInt(),
                            buf.readBoolean()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpawnImageFramePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ServerLevel level = player.serverLevel();

            String url = pkt.url().trim();
            if (url.isEmpty()) return;

            // 💥 берём РОВНО ту рамку
            Entity entity = level.getEntity(pkt.frameId());
            if (!(entity instanceof ItemFrame itemFrame)) return;

            // ❌ уже используется
            if (itemFrame.isInvisible()) return;

            // ❌ уже есть картинка
            boolean hasImage = !level.getEntitiesOfClass(
                    ImageFrameEntity.class,
                    itemFrame.getBoundingBox().inflate(0.1)
            ).isEmpty();

            if (hasImage) return;

            // 🔥 создаём картинку
            ImageFrameEntity img = new ImageFrameEntity(
                    ModEntityTypes.IMAGE_FRAME.get(), level
            );

            img.setPos(
                    itemFrame.getX(),
                    itemFrame.getY(),
                    itemFrame.getZ()
            );

            img.setFacingDirection(itemFrame.getDirection());

            img.setImageUrl(url);
            img.setWidth(pkt.width());
            img.setHeight(pkt.height());
            img.setMirrored(pkt.mirrored());
            img.setStartTime((int)(System.currentTimeMillis() / 50));

            level.addFreshEntity(img);

            // 👻 скрываем рамку
            itemFrame.setInvisible(true);
        });
    }
}