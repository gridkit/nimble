package org.gridkit.nimble.btrace.ext;

import java.io.Serializable;
import java.util.List;

import org.gridkit.nimble.btrace.ext.model.SampleStoreState;

public class PollStateCmdResult implements Serializable {
    private static final long serialVersionUID = 7841147230710651218L;
    
    private List<SampleStoreState> data;

    public List<SampleStoreState> getData() {
        return data;
    }

    public void setData(List<SampleStoreState> data) {
        this.data = data;
    }
}
