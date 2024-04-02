package com.lowdragmc.lowdraglib.gui.editor.accessors;

import com.lowdragmc.lowdraglib.client.renderer.IRenderer;
import com.lowdragmc.lowdraglib.client.renderer.impl.IModelRenderer;
import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigAccessor;
import com.lowdragmc.lowdraglib.gui.editor.annotation.DefaultValue;
import com.lowdragmc.lowdraglib.gui.editor.configurator.Configurator;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IRendererConfigurator;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.function.Consumer;
import java.util.function.Supplier;

@ConfigAccessor
public class IRendererAccessor extends TypesAccessor<IRenderer> {

    public IRendererAccessor() {
        super(IRenderer.class);
    }

    @Override
    public IRenderer defaultValue(Field field, Class<?> type) {
        if (field.isAnnotationPresent(DefaultValue.class)) {
            return new IModelRenderer(new ResourceLocation(field.getAnnotation(DefaultValue.class).stringValue()[0]));
        }
        return IRenderer.EMPTY;
    }

    @Override
    public Configurator create(String name, Supplier<IRenderer> supplier, Consumer<IRenderer> consumer, boolean forceUpdate, Field field) {
        return new IRendererConfigurator(name, supplier, consumer, defaultValue(field, ReflectionUtils.getRawType(field.getGenericType())), forceUpdate);
    }
}
