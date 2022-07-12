package org.test;

import org.example.InstanceRunner;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;

public class MyMainExample {


    public MyMainExample() {}

    public static void main(String[] args) {
        new InstanceRunner(MyMainExample.class);
    }
}
