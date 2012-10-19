package org.gridkit.nimble.btrace.ext;

import java.io.Serializable;
import java.util.List;

import org.gridkit.nimble.btrace.ext.model.ScalarSample;

public class PollSamplesCmdResult implements Serializable {
    private static final long serialVersionUID = 8988203060254970517L;
    
    private List<Element> elements;

    public List<Element> getElements() {
        return elements;
    }

    public void setElements(List<Element> elements) {
        this.elements = elements;
    }

    public static class Element implements Serializable {
        private static final long serialVersionUID = -7906160410947353935L;
        
        private String scriptClass;
        private String sampleStore;
        private List<ScalarSample> samples;
        
        public String getScriptClass() {
            return scriptClass;
        }
        
        public void setScriptClass(String clazz) {
            this.scriptClass = clazz;
        }
        
        public String getSampleStore() {
            return sampleStore;
        }
        
        public void setSampleStore(String sampleStore) {
            this.sampleStore = sampleStore;
        }
        
        public List<ScalarSample> getSamples() {
            return samples;
        }
        
        public void setSamples(List<ScalarSample> samples) {
            this.samples = samples;
        }
    }
}
