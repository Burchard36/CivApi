package org.example.socket.action;

import java.util.HashMap;
import java.util.Set;

public class ActionRunner {

    protected HashMap<String, MethodHolder> actionMap;

    public ActionRunner() {
        this.actionMap = new HashMap<>();
    }

    public void addAction(final String actionName,
                          final MethodHolder methodHolder) {
        actionMap.put(actionName, methodHolder);
    }

    public MethodHolder getAction(String actionName) {
        return actionMap.get(actionName);
    }

}
