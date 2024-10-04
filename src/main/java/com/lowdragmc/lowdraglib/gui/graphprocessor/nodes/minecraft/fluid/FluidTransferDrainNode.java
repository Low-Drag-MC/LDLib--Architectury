package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.minecraft.fluid;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.runtime.ConfiguratorParser;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.InputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.annotation.OutputPort;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.trigger.LinearTriggerNode;
import com.lowdragmc.lowdraglib.side.fluid.IFluidHandlerModifiable;
import lombok.SneakyThrows;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.lang.reflect.Method;
import java.util.HashMap;

@LDLRegister(name = "fluid drain", group = "graph_processor.node.minecraft.fluid")
public class FluidTransferDrainNode extends LinearTriggerNode {
    @InputPort(name = "fluid transfer")
    public IFluidHandlerModifiable fluidTransfer;
    @InputPort
    public FluidStack fluidstack;
    @InputPort
    public Boolean simulate;
    @OutputPort
    public FluidStack drained;
    @Configurable(name = "simulate")
    public boolean internalSimulate;

    @Override
    public void process() {
        drained = null;
        if (fluidTransfer != null && fluidstack != null) {
            drained = fluidTransfer.drain(fluidstack, simulate == null ? internalSimulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE : simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @Override
    @SneakyThrows
    public void buildConfigurator(ConfiguratorGroup father) {
        var setter = new HashMap<String, Method>();
        var clazz = getClass();
        for (var port : getInputPorts()) {
            if (port.fieldName.equals("simulate")) {
                if (port.getEdges().isEmpty()) {
                    ConfiguratorParser.createFieldConfigurator(clazz.getField("internalSimulate"), father, clazz, setter, this);
                }
            }
        }
    }
}