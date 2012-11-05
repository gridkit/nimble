package org.gridkit.nimble.orchestration;

import java.lang.reflect.Method;
import java.util.Collection;

import org.gridkit.util.concurrent.Box;
import org.gridkit.vicluster.ViNode;

public interface ScenarioVisitor {
    void visitDeploy(DeployAction action, Box<Void> box);
    
    void visitCall(CallAction action, Box<Void> box);
    
    void visitCheckpoint(CheckpointAction action, Box<Void> box);

    public interface BeanRef {
        int getId();
    }

    public interface ScenarioAction {
        int getActionId();
        
        Collection<ScenarioAction> getDependencies();
        
        Collection<ViNode> getNodes();
    }
    
    public interface DeployAction extends ScenarioAction {
        Object getBean();
        
        BeanRef getBeanRef();
    }
    
    public interface CallAction extends ScenarioAction {
        BeanRef getBeanRef();
        
        Method getMethod();
        
        Object[] getArgs();
        
        BeanRef getResultRef();
    }
    
    public interface CheckpointAction extends ScenarioAction {        
        String getName();
        
        long getSleepTimeMs();
    }
}
