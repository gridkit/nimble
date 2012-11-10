package org.gridkit.nimble.orchestration;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.gridkit.util.concurrent.Box;
import org.gridkit.vicluster.ViNode;

public interface ScenarioProposal {
    public static class Scenario {
        private Map<Integer, ScenarioAction> actions;
        
        public Scenario(Collection<ScenarioAction> actions) {
            // init actions var
        }
        
        void play(ScenarioPlayer player) {
            Map<Integer, ScenarioAction> nextActions = new HashMap<Integer, ScenarioAction>(actions);
            
            final BlockingQueue<ActionBox> queue = new LinkedBlockingQueue<ActionBox>();
            
            while (!nextActions.isEmpty()) {
                for (ScenarioAction action : nextStep(actions)) {
                    ActionBox box = new ActionBox(action, queue);
                    action.play(player, box);
                }

                ActionBox doneBox;
                
                try {
                    doneBox = queue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                
                if (doneBox.isError()) {
                    throw new RuntimeException(doneBox.getError());
                } else {
                    nextActions.remove(doneBox.getAction().getId());
                }
            }
        }
        
        void visit(ScenarioVisitor visitor) {
            play(new VisitingPlayer(visitor));
        }
        
        private static Collection<ScenarioAction> nextStep(Map<Integer, ScenarioAction> nextActions) {
            Collection<ScenarioAction> result = new ArrayList<ScenarioAction>();
            
            for (ScenarioAction action : nextActions.values()) {
                boolean ready = true;
                
                for (ScenarioAction depAction : action.getDependencies()) {
                    if (nextActions.containsKey(depAction.getId())) {
                        ready = false;
                        break;
                    }
                }
                
                if (ready) {
                    result.add(action);
                }
            }
            
            return result;
        }
        
        private static class ActionBox implements Box<Void> {
            private final ScenarioAction action;
            private final BlockingQueue<ActionBox> queue;

            private volatile Throwable error = null;
            
            public ActionBox(ScenarioAction action, BlockingQueue<ActionBox> queue) {
                this.action = action;
                this.queue = queue;
            }

            @Override
            public void setData(Void data) {
                queue.add(this);
            }

            @Override
            public void setError(Throwable error) {
                this.error = error;
                queue.add(this);
            }
            
            public ScenarioAction getAction() {
                return action;
            }

            public boolean isError() {
                return error != null;
            }
            
            public Throwable getError() {
                return error;
            }
        }
        
        private static class VisitingPlayer implements ScenarioPlayer {
            private final ScenarioVisitor visitor;
            
            public VisitingPlayer(ScenarioVisitor visitor) {
                this.visitor = visitor;
            }

            @Override
            public void playDeploy(DeployAction action, Box<Void> box) {
                visitor.visitDeploy(action);
                box.setData(null);
            }

            @Override
            public void playCall(CallAction action, Box<Void> box) {
                visitor.visitCall(action);
                box.setData(null);
            }

            @Override
            public void playCheckpoint(CheckpointAction action, Box<Void> box) {
                visitor.visitCheckpoint(action);
                box.setData(null);
            }
        }
    }

    public static interface ScenarioPlayer {
        void playDeploy(DeployAction action, Box<Void> box);
        
        void playCall(CallAction action, Box<Void> box);
        
        void playCheckpoint(CheckpointAction action, Box<Void> box);
    }
    
    public static interface ScenarioVisitor {
        void visitDeploy(DeployAction action);
        
        void visitCall(CallAction action);
        
        void visitCheckpoint(CheckpointAction action);
    }
    
    public static interface ScenarioListener {
        void beforeDeploy(DeployAction action);
        void afterDeploy(DeployAction action);
        
        // and so on
        
        void onError(ScenarioAction action, Throwable t);
    }
    
    public static class ListeningScenarioPlayer implements ScenarioPlayer {
        private ScenarioPlayer executor;
        private ScenarioListener listener;
        
        @Override
        public void playDeploy(final DeployAction action, final Box<Void> box) {
            listener.beforeDeploy(action);
            
            executor.playDeploy(action, new Box<Void>() {
                @Override
                public void setData(Void data) {
                    listener.afterDeploy(action);
                    box.setData(data);
                }

                @Override
                public void setError(Throwable e) {
                    listener.onError(action, e);
                    box.setError(e);
                }
            });
        }

        @Override
        public void playCall(CallAction action, Box<Void> box) {}
        @Override
        public void playCheckpoint(CheckpointAction action, Box<Void> box) {}
    }
    
    public static class LoggingListener {
        
    }
    
    public static class ViScenarioPlayer implements ScenarioPlayer {
        @Override
        public void playDeploy(DeployAction action, Box<Void> box) {
            if (action.getBean() instanceof DeployableBean) {
                // do deploy
            } else if (action.getBean() instanceof SplittableBean) {
                // do deploy
            } else {
                
            }
        }

        @Override
        public void playCall(CallAction action, Box<Void> box) {}

        @Override
        public void playCheckpoint(CheckpointAction action, Box<Void> box) {}
    }
    
    // this is class
    public interface BeanRef {
        int getId();
    }

    // this is class
    public interface ScenarioAction {
        int getId();
        
        Collection<ScenarioAction> getDependencies();
        
        void play(ScenarioPlayer player, Box<Void> box);
    }
    
    // this is class
    public interface DeployAction extends ScenarioAction {
        Collection<ViNode> getNodes();
        
        Object getBean();
        
        BeanRef getBeanRef();
    }
    
    // this is class
    public interface CallAction extends ScenarioAction {
        Collection<ViNode> getNodes();
        
        BeanRef getBeanRef();
        
        Method getMethod();
        
        Object[] getArgs();
        
        BeanRef getResultRef();
    }
    
    // this is class
    public interface CheckpointAction extends ScenarioAction {        
        String getName();
        
        long getSleepTimeMs();
    }
}
