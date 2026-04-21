package com.dseel.imageframe.client;

import com.dseel.imageframe.ImageFrameMod;
import com.dseel.imageframe.common.ModEntityTypes;
import com.dseel.imageframe.renderer.ImageFrameRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

// NeoForge 21.1+ automatically routes to the correct bus based on event type.
// No need to specify bus = Bus.MOD — EntityRenderersEvent implements IModBusEvent.
@EventBusSubscriber(modid = ImageFrameMod.MODID, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.IMAGE_FRAME.get(), ImageFrameRenderer::new);
    }
}
