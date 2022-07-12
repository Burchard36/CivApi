package org.example.socket.action;

import lombok.Getter;
import org.example.socket.user.ConnectedClient;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Getter
public class MethodHolder {

    protected final Method method;
    protected final Class<?> parsingClass;
    protected final Class<?> methodCallingClass;
    protected final Object methodCallingObject;

    public MethodHolder(final Method method,
                        final Class<?> parsingObject,
                        final Class<?> methodCallingClass) {
        this.method = method;
        this.parsingClass = parsingObject;
        this.methodCallingClass = methodCallingClass;

        try {
            methodCallingObject = this.methodCallingClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public void invokeMethod(Object object,
                             ConnectedClient client) {
        try {
            this.method.invoke(this.methodCallingObject, client, object);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException ex) {
            ex.printStackTrace();
        }
    }

}
