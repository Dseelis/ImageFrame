package com.dseel.imageframe.network;

import com.dseel.imageframe.common.ModEntityTypes;
import com.dseel.imageframe.entity.ImageFrameEntity;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

public record SpawnImageFramePacket(double px, double py, double pz,
                                    int facingIdx, String url,
                                    int width, int height)
        implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("imageframe", "spawn_image_frame");

    public static final CustomPacketPayload.Type<SpawnImageFramePacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SpawnImageFramePacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeDouble(pkt.px());
                        buf.writeDouble(pkt.py());
                        buf.writeDouble(pkt.pz());
                        buf.writeInt(pkt.facingIdx());
                        buf.writeUtf(pkt.url(), 512);
                        buf.writeInt(pkt.width());
                        buf.writeInt(pkt.height());
                    },
                    buf -> new SpawnImageFramePacket(
                            buf.readDouble(), buf.readDouble(), buf.readDouble(),
                            buf.readInt(), buf.readUtf(512),
                            buf.readInt(), buf.readInt()
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
            if (url.isEmpty() || (!url.startsWith("http://") && !url.startsWith("https://"))) {
                return;
            }

            Direction facing = Direction.from3DDataValue(pkt.facingIdx());

            ImageFrameEntity frame = new ImageFrameEntity(
                    ModEntityTypes.IMAGE_FRAME.get(), level);
            frame.setPos(pkt.px(), pkt.py(), pkt.pz());
            frame.setFacingDirection(facing);
            frame.setImageUrl(url);
            frame.setWidth(pkt.width());
            frame.setHeight(pkt.height());
            level.addFreshEntity(frame);

            // Скрываем ItemFrame который находится в том же месте
            List<ItemFrame> nearby = level.getEntitiesOfClass(
                    ItemFrame.class,
                    new AABB(pkt.px() - 0.6, pkt.py() - 0.6, pkt.pz() - 0.6,
                             pkt.px() + 0.6, pkt.py() + 0.6, pkt.pz() + 0.6)
            );
            for (ItemFrame itemFrame : nearby) {
                itemFrame.setInvisible(true);
            }
        });
    }
}
