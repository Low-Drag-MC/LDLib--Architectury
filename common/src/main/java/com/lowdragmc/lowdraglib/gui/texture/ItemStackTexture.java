package com.lowdragmc.lowdraglib.gui.texture;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

@LDLRegister(name = "item_stack_texture", group = "texture")
public class ItemStackTexture extends TransformTexture {
    @Configurable(name = "ldlib.gui.editor.name.items")
    public final ItemStack[] itemStack;
    private int index = 0;
    private int ticks = 0;
    @Configurable(name = "ldlib.gui.editor.name.color")
    private int color = -1;
    private long lastTick;

    public ItemStackTexture() {
        this(Items.APPLE.asItem());
    }


    public ItemStackTexture(ItemStack... itemStacks) {
        this.itemStack = itemStacks;
    }

    public ItemStackTexture(Item... items) {
        this.itemStack = new ItemStack[items.length];
        for(int i = 0; i < items.length; i++) {
            itemStack[i] = new ItemStack(items[i]);
        }
    }

    @Override
    public ItemStackTexture setColor(int color) {
        this.color = color;
        return this;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void updateTick() {
        if (Minecraft.getInstance().level != null) {
            long tick = Minecraft.getInstance().level.getGameTime();
            if (tick == lastTick) return;
            lastTick = tick;
        }
        if(itemStack.length > 1 && ++ticks % 20 == 0)
            if(++index == itemStack.length)
                index = 0;
    }

    @Environment(EnvType.CLIENT)
    @Override
    protected void drawInternal(GuiGraphics graphics, int mouseX, int mouseY, float x, float y, int width, int height) {
        if (itemStack.length == 0) return;
        updateTick();
        graphics.pose().pushPose();
        graphics.pose().scale(width / 16f, height / 16f, 1);
        graphics.pose().translate(x * 16 / width, y * 16 / height, -200);
        DrawerHelper.drawItemStack(graphics, itemStack[index], 0, 0, color, null);
        graphics.pose().popPose();
    }
}
