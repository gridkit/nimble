package org.gridkit.nimble.btrace.ext;

import java.io.Serializable;
import java.util.List;

import org.gridkit.nimble.btrace.ext.model.SampleStoreContents;

public class PollSamplesCmdResult implements Serializable {
    private static final long serialVersionUID = 8988203060254970517L;
    
    private List<SampleStoreContents> data;

    public List<SampleStoreContents> getData() {
        return data;
    }

    public void setData(List<SampleStoreContents> data) {
        this.data = data;
    }
}
