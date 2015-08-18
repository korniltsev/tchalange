package ru.korniltsev.telegram.core.flow.pathview;

import java.util.ArrayList;
import java.util.List;

public class TraversalAwareHelper  {
    final List<Runnable> actions = new ArrayList<>();
    private boolean traversalCompleted = false;

    public void setTraversalCompleted(){
        traversalCompleted = true;
        for (int i = 0, actionsSize = actions.size(); i < actionsSize; i++) {
            Runnable action = actions.get(i);
            action.run();
        }
        actions.clear();
    }


    public void runWhenTraversalCompleted(Runnable r){
        if (traversalCompleted){
            r.run();
        } else {
            actions.add(r);
        }
    }
}
