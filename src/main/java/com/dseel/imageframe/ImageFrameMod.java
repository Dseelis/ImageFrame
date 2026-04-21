package com.dseel.imageframe;

import com.dseel.imageframe.common.ModEntityTypes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(ImageFrameMod.MODID)
public class ImageFrameMod {

    public static final String MODID = "imageframe";

    public ImageFrameMod(IEventBus modEventBus) {
        ModEntityTypes.ENTITY_TYPES.register(modEventBus);
        // Network packets are registered via @SubscribeEvent in ModNetwork
    }
}
