package com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.logic;

import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.graphprocessor.nodes.ListMergeNode;

@LDLRegister(name = "and", group = "graph_processor.node.logic")
public class AndNode extends ListMergeNode<Boolean> {

    @Override
    public Class<Boolean> type() {
        return Boolean.class;
    }

    @Override
    public Boolean defaultValue() {
        return false;
    }

    @Override
    public Boolean merge(Boolean a, Boolean b) {
        return a && b;
    }
}
