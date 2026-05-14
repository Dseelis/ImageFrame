package com.dseel.imageframe.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

public class ImageFrameEntity extends Entity {

    private static final EntityDataAccessor<String> IMAGE_URL =
            SynchedEntityData.defineId(ImageFrameEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> WIDTH =
            SynchedEntityData.defineId(ImageFrameEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HEIGHT =
            SynchedEntityData.defineId(ImageFrameEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FACING_INT =
            SynchedEntityData.defineId(ImageFrameEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> START_TIME =
            SynchedEntityData.defineId(ImageFrameEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> MIRRORED =
            SynchedEntityData.defineId(ImageFrameEntity.class, EntityDataSerializers.BOOLEAN);

    public ImageFrameEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setInvisible(true); // Entity itself invisible; renderer draws the image
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(IMAGE_URL, "");
        builder.define(WIDTH, 1);
        builder.define(HEIGHT, 1);
        builder.define(FACING_INT, Direction.NORTH.get3DDataValue());
        builder.define(START_TIME, 0);
        builder.define(MIRRORED, false);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        this.refreshDimensions(); // 💥 ВАЖНО
    }

    @Override
    public net.minecraft.world.entity.EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        return net.minecraft.world.entity.EntityDimensions.scalable(
                this.getWidth(),
                this.getHeight()
        );
    }

    @Override
    public void refreshDimensions() {
        super.refreshDimensions();
        this.setBoundingBox(this.makeBoundingBox());
    }

    @Override
    protected AABB makeBoundingBox() {
        Direction dir = getFacingDirection();
        double w = getWidth();
        double h = getHeight();
        double hw = w / 2.0;
        double hh = h / 2.0;
        double thickness = 0.03125; // 1/32 block

        double x = getX();
        double y = getY();
        double z = getZ();

        switch (dir) {
            case NORTH:
            case SOUTH:
                return new AABB(x - hw, y - hh, z - thickness, x + hw, y + hh, z + thickness);
            case EAST:
            case WEST:
                return new AABB(x - thickness, y - hh, z - hw, x + thickness, y + hh, z + hw);
            case UP:
            case DOWN:
                return new AABB(x - hw, y - thickness, z - hh, x + hw, y + thickness, z + hh);
            default:
                return super.makeBoundingBox();
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.setImageUrl(tag.getString("ImageUrl"));
        this.setWidth(tag.getInt("FrameWidth"));
        this.setHeight(tag.getInt("FrameHeight"));
        this.setFacingDirection(Direction.from3DDataValue(tag.getInt("FacingDir")));
        this.setStartTime(tag.getInt("StartTime"));
        this.setMirrored(tag.getBoolean("Mirrored"));
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            this.refreshDimensions();
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("ImageUrl", this.getImageUrl());
        tag.putInt("FrameWidth", this.getWidth());
        tag.putInt("FrameHeight", this.getHeight());
        tag.putInt("FacingDir", this.getFacingDirection().get3DDataValue());
        tag.putInt("StartTime", this.getStartTime());
        tag.putBoolean("Mirrored", this.isMirrored());
    }

    public String getImageUrl() {
        return this.entityData.get(IMAGE_URL);
    }

    public void setImageUrl(String url) {
        this.entityData.set(IMAGE_URL, url);
    }

    public int getWidth() {
        return this.entityData.get(WIDTH);
    }

    public int getStartTime() {
        return this.entityData.get(START_TIME);
    }

    public void setStartTime(int t) {
        this.entityData.set(START_TIME, t);
    }

    public void setWidth(int w) {
        this.entityData.set(WIDTH, Math.max(1, Math.min(w, 8)));
        this.refreshDimensions();
    }

    public int getHeight() {
        return this.entityData.get(HEIGHT);
    }

    public void setHeight(int h) {
        this.entityData.set(HEIGHT, Math.max(1, Math.min(h, 8)));
        this.refreshDimensions();
    }

    public Direction getFacingDirection() {
        return Direction.from3DDataValue(this.entityData.get(FACING_INT));
    }

    public void setFacingDirection(Direction dir) {
        this.entityData.set(FACING_INT, dir.get3DDataValue());
        this.refreshDimensions();
    }

    public boolean isMirrored() {
        return this.entityData.get(MIRRORED);
    }

    public void setMirrored(boolean mirrored) {
        this.entityData.set(MIRRORED, mirrored);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distSq) {
        return distSq < 65536.0; // 256 blocks
    }
}
