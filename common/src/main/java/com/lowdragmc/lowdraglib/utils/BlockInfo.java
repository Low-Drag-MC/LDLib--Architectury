package com.lowdragmc.lowdraglib.utils;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.syncdata.IPersistedSerializable;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

/**
 * Author: KilaBash
 * Date: 2022/04/26
 * Description:
 */
@NoArgsConstructor
public class BlockInfo implements IPersistedSerializable, IConfigurable {
    public static final BlockInfo EMPTY = new BlockInfo(Blocks.AIR);

    @Setter
    @Configurable
    private BlockState blockState;
    @Setter
    private boolean hasBlockEntity;
    @Setter
    @Configurable
    private CompoundTag tag;
    @Setter
    @Configurable
    private ItemStack itemStack;
    @Setter
    private Consumer<BlockEntity> postCreate;
    // runtime
    private BlockEntity lastEntity;

    public BlockInfo(Block block) {
        this(block.defaultBlockState());
    }

    public BlockInfo(BlockState blockState) {
        this(blockState, false);
    }

    public BlockInfo(BlockState blockState, boolean hasBlockEntity) {
        this(blockState, hasBlockEntity, null, null);
    }
    public BlockInfo(BlockState blockState, Consumer<BlockEntity> postCreate) {
        this(blockState, true, null, postCreate);
    }

    public BlockInfo(BlockState blockState, boolean hasBlockEntity, ItemStack itemStack, Consumer<BlockEntity> postCreate) {
        this.blockState = blockState;
        this.hasBlockEntity = hasBlockEntity;
        this.itemStack = itemStack;
        this.postCreate = postCreate;
    }

    public static BlockInfo fromBlockState(BlockState state) {
        try {
            if (state.getBlock() instanceof EntityBlock) {
                BlockEntity blockEntity = ((EntityBlock) state.getBlock()).newBlockEntity(BlockPos.ZERO, state);
                if (blockEntity != null) {
                    return new BlockInfo(state, true);
                }
            }
        } catch (Exception ignored){ }
        return new BlockInfo(state);
    }

    public static BlockInfo fromBlock(Block block) {
        return BlockInfo.fromBlockState(block.defaultBlockState());
    }

    public BlockState getBlockState() {
        return blockState;
    }

    public boolean hasBlockEntity() {
        return hasBlockEntity;
    }

    public BlockEntity getBlockEntity(BlockPos pos) {
        if (hasBlockEntity && blockState.getBlock() instanceof EntityBlock entityBlock) {
            if (lastEntity != null && lastEntity.getBlockPos().equals(pos)) {
                return lastEntity;
            }
            lastEntity = entityBlock.newBlockEntity(pos, blockState);
            if (tag != null && lastEntity != null) {
                var compoundTag2 = lastEntity.saveWithoutMetadata();
                var compoundTag3 = compoundTag2.copy();
                compoundTag2.merge(tag);
                if (!compoundTag2.equals(compoundTag3)) {
                    lastEntity.load(compoundTag2);
                }
            }
            if (postCreate != null) {
                postCreate.accept(lastEntity);
            }
            return lastEntity;
        }
        return null;
    }

    public BlockEntity getBlockEntity(Level level, BlockPos pos) {
        BlockEntity entity = getBlockEntity(pos);
        if (entity != null) {
            entity.setLevel(level);
        }
        return entity;
    }

    public ItemStack getItemStackForm() {
        return itemStack == null ? new ItemStack(blockState.getBlock()) : itemStack;
    }

    public ItemStack getItemStackForm(BlockAndTintGetter level, BlockPos pos) {
        if (itemStack != null) return itemStack;
        return blockState.getBlock().getCloneItemStack(new FacadeBlockAndTintGetter(level, pos, blockState, null), pos, blockState);
    }

    public void apply(Level world, BlockPos pos) {
        world.setBlockAndUpdate(pos, blockState);
        BlockEntity blockEntity = getBlockEntity(pos);
        if (blockEntity != null) {
            world.setBlockEntity(blockEntity);
        }
    }

    public void clearBlockEntityCache() {
        lastEntity = null;
    }
}
