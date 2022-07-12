package org.test;

import org.example.Instance;

public class TestInstance extends Instance {


    public TestInstance() {
        super("TEST_INSTANCE");
        System.out.println("init test instance");
    }

    @Override
    protected void onStart() {
    }

    @Override
    protected void onStop() {

    }

    @Override
    protected void onSave() {
        System.out.println("Calling onSave");
    }
}
