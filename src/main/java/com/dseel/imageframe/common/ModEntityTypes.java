package com.dseel.imageframe.common;

import com.dseel.imageframe.ImageFrameMod;
import com.dseel.imageframe.entity.ImageFrameEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.core.registries.BuiltInRegistries;

public class ModEntityTypes {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, ImageFrameMod.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ImageFrameEntity>> IMAGE_FRAME =
            ENTITY_TYPES.register("image_frame", () ->
                    EntityType.Builder.<ImageFrameEntity>of(ImageFrameEntity::new, MobCategory.MISC)
                            .sized(0.5f, 0.5f)
                            .clientTrackingRange(10)
                            .noSummon()
                            .build("image_frame")
            );
}
