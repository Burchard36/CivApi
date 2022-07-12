package org.example;

import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class InstanceRunner{

    public HashMap<String, Instance> runningInstances;
    public InstanceRunner(Class<?> mainClass) {
        this.runningInstances = new HashMap<>();
        Reflections reflections = new Reflections(mainClass.getPackage().getName());
        reflections.getSubTypesOf(Instance.class).forEach(instanceClass -> {
            try {
                Constructor<? extends Instance> instanceConstructor = instanceClass.getDeclaredConstructor();
                Instance instance = instanceConstructor.newInstance();
                String instanceKey = instance.instanceName;

                if (this.runningInstances.get(instanceKey) != null) {
                    throw new IllegalStateException("Instance with name " + instanceKey + " is already running!");
                }

                this.runningInstances.putIfAbsent(instanceKey, instance);

            } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                     IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public final Instance getInstance(String instanceKey) {
        return this.runningInstances.get(instanceKey);
    }

}
