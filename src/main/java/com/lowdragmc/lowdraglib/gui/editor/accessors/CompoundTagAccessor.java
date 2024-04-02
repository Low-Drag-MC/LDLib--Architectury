package com.lowdragmc.lowdraglib.gui.editor.accessors;

import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.gui.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.StringConfigurator;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ConfigAccessor
public class CompoundTagAccessor extends TypesAccessor<CompoundTag> {

    public CompoundTagAccessor() {
        super(CompoundTag.class);
    }

    @Override
    public CompoundTag defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            try {
                return NbtUtils.snbtToStructure(field.getAnnotation(DefaultValue.class).stringValue()[0]);
            } catch (Exception e) {
                return new CompoundTag();
            }
        }
        return new CompoundTag();
    }

    @Override
    public Configurator create(String name, Supplier<CompoundTag> supplier, Consumer<CompoundTag> consumer, boolean forceUpdate, Field field) {
        var configurator = new StringConfigurator(name,
                () -> NbtUtils.structureToSnbt(supplier.get()).replaceAll("\t", "").replaceAll("\\n", "").replaceAll(" ", ""),
                text -> {
                    try {
                        consumer.accept(NbtUtils.snbtToStructure(text));
                    } catch (CommandSyntaxException ignored) {}
                }, NbtUtils.structureToSnbt(defaultValue(field, String.class)).replaceAll("\t", "").replaceAll("\\n", "").replaceAll(" ", ""), forceUpdate);
        configurator.setCompoundTag(true);
        return configurator;
    }
}
