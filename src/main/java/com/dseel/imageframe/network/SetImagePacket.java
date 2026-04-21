package com.dseel.imageframe.network;

import com.dseel.imageframe.entity.ImageFrameEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SetImagePacket(int entityId, String url, int width, int height)
        implements CustomPacketPayload {

    public static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("imageframe", "set_image");

    public static final CustomPacketPayload.Type<SetImagePacket> TYPE =
            new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SetImagePacket> CODEC =
            StreamCodec.of(
                    (buf, pkt) -> {
                        buf.writeInt(pkt.entityId());
                        buf.writeUtf(pkt.url(), 512);
                        buf.writeInt(pkt.width());
                        buf.writeInt(pkt.height());
                    },
                    buf -> new SetImagePacket(
                            buf.readInt(),
                            buf.readUtf(512),
                            buf.readInt(),
                            buf.readInt()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetImagePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) ctx.player();
            ServerLevel level = player.serverLevel();
            Entity entity = level.getEntity(pkt.entityId());

            if (entity instanceof ImageFrameEntity frame) {
                // Basic URL validation
                String url = pkt.url().trim();
                if (!url.isEmpty() && (url.startsWith("http://") || url.startsWith("https://"))) {
                    frame.setImageUrl(url);
                    frame.setWidth(pkt.width());
                    frame.setHeight(pkt.height());
                }
            }
        });
    }
}
