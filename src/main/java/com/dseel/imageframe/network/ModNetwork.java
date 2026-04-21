package com.dseel.imageframe.network;

import com.dseel.imageframe.ImageFrameMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = ImageFrameMod.MODID)
public class ModNetwork {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                SetImagePacket.TYPE,
                SetImagePacket.CODEC,
                SetImagePacket::handle
        );
        registrar.playToServer(
                SpawnImageFramePacket.TYPE,
                SpawnImageFramePacket.CODEC,
                SpawnImageFramePacket::handle
        );
        registrar.playToServer(
                RemoveImageFramePacket.TYPE,
                RemoveImageFramePacket.CODEC,
                RemoveImageFramePacket::handle
        );
    }
}
