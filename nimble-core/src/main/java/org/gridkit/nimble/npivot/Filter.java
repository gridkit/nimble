package org.gridkit.nimble.npivot;

public interface Filter extends Function {
    Boolean apply(Sample sample);
}
