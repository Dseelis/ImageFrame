package com.dseel.imageframe.network;

import com.dseel.imageframe.entity.ImageFrameEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record RemoveImageFramePacket(int entityId) implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("imageframe", "remove_image_frame");

    public static final CustomPacketPayload.Type<RemoveImageFramePacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, RemoveImageFramePacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> buf.writeInt(pkt.entityId()),
                    buf -> new RemoveImageFramePacket(buf.readInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemoveImageFramePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(pkt.entityId());

            if (entity instanceof ImageFrameEntity frame) {
                double px = frame.getX();
                double py = frame.getY();
                double pz = frame.getZ();

                List<ItemFrame> nearby = level.getEntitiesOfClass(
                        ItemFrame.class,
                        new AABB(px - 0.6, py - 0.6, pz - 0.6,
                                 px + 0.6, py + 0.6, pz + 0.6)
                );
                for (ItemFrame itemFrame : nearby) {
                    itemFrame.setInvisible(false);
                }

                frame.discard();
            }
        });
    }
}
