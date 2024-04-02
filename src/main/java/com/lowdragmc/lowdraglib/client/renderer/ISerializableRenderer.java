package com.lowdragmc.lowdraglib.client.renderer;

import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlock;
import com.lowdragmc.lowdraglib.client.renderer.block.RendererBlockEntity;
import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurable;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.lowdragmc.lowdraglib.syncdata.IAutoPersistedSerializable;
import com.lowdragmc.lowdraglib.utils.BlockInfo;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.Optional;

public interface ISerializableRenderer extends IConfigurable, IRenderer, IAutoPersistedSerializable {

    static CompoundTag serializeWrapper(ISerializableRenderer renderer) {
        return renderer.serializeNBT();
    }

    static ISerializableRenderer deserializeWrapper(CompoundTag tag) {
        var type = tag.getString("_type");
        var wrapper = AnnotationDetector.REGISTER_RENDERERS.get(type);
        if (wrapper != null) {
            var renderer = wrapper.creator().get();
            renderer.deserializeNBT(tag);
            renderer.initRenderer();
            return renderer;
        }
        return null;
    }

    // should be called after deserialization and only once.
    default void initRenderer() {

    }

    default void createPreview(ConfiguratorGroup father) {
        var level = new TrackedDummyWorld();
        level.addBlock(BlockPos.ZERO, BlockInfo.fromBlock(RendererBlock.BLOCK));
        Optional.ofNullable(level.getBlockEntity(BlockPos.ZERO)).ifPresent(blockEntity -> {
            if (blockEntity instanceof RendererBlockEntity holder) {
                holder.setRenderer(this);
            }
        });

        var sceneWidget = new SceneWidget(0, 0, 100, 100, level);
        sceneWidget.setRenderFacing(false);
        sceneWidget.setRenderSelect(false);
        sceneWidget.createScene(level);
        sceneWidget.getRenderer().setOnLookingAt(null); // better performance
        sceneWidget.setRenderedCore(Collections.singleton(BlockPos.ZERO), null);
        sceneWidget.setBackground(new ColorBorderTexture(2, ColorPattern.T_WHITE.color));

        father.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", sceneWidget));
    }

    @Override
    default void buildConfigurator(ConfiguratorGroup father) {
        createPreview(father);
        IConfigurable.super.buildConfigurator(father);
    }
}
