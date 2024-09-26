package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.minecraft;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.BaseNode;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LDLRegister(name = "item", group = "graph_processor.node.minecraft")
public class ItemNode extends BaseNode {
    @InputPort
    public Object in = null;
    @OutputPort
    public Item out = null;
    @Configurable(showName = false)
    public Item internalValue = Items.AIR;

    @Override
    public int getMinWidth() {
        return 100;
    }

    @Override
    public void process() {
        if (in == null) {
            out = internalValue;
            return;
        } else if (in instanceof Item item) {
            out = item;
        } else if (in instanceof ItemStack itemStack) {
            out = itemStack.getItem();
        } else {
            var name = in.toString();
            if (ResourceLocation.isValidResourceLocation(name)) {
                out = BuiltInRegistries.ITEM.get(new ResourceLocation(name));
            } else {
                out = null;
            }
        }
        internalValue = out;
    }
}
