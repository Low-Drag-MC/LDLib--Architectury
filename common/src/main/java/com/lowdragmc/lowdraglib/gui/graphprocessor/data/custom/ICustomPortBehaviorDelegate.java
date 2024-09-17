package com.lowdragmc.lowdraglib.gui.graphprocessor.data.custom;

import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortData;
import com.lowdragmc.lowdraglib.gui.graphprocessor.data.PortEdge;

import java.util.List;

public interface ICustomPortBehaviorDelegate {
    /**
     * Return the new list of port data by the current port edges
     */
    List<PortData> handle(List<PortEdge> edges);
}