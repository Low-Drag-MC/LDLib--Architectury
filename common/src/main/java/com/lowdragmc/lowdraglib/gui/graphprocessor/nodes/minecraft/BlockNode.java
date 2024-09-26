package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.minecraft;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

@LDLRegister(name = "block", group = "graph_processor.node.minecraft")
public class BlockNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public Block out = null;
    @Configurable(showName = false)
    public Block internalValue = Blocks.AIR;

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof Block block) {
            out = block;
        } else if (in instanceof BlockState blockState) {
            out = blockState.getBlock();
        } else {
            var name = in.toString();
            if (ResourceLocation.isValidResourceLocation(name)) {
                out = BuiltInRegistries.BLOCK.get(new ResourceLocation(name));
            } else {
                out = null;
            }
        }
        internalValue = out;
    }
}
