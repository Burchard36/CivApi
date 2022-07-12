package org.example;


import lombok.Getter;

import java.util.concurrent.CompletableFuture;

public abstract class Instance extends Thread {

    @Getter
    protected final String instanceName;
    protected Instance(String instanceName) {
        this.instanceName = instanceName;

        this.runSaveTask();
        this.start();
    }

    @Override
    public void run() {
        this.onStart();
    }

    public int saveDelay = 5;


    protected abstract void onStart();

    protected abstract void onStop();
    protected abstract void onSave();

    protected void runSaveTask() {
        new Thread(() -> {
            try {
                System.out.println("Running save task");
                Thread.sleep(saveDelay * 10000L);
                CompletableFuture.runAsync(this::onSave);
                this.runSaveTask();
            } catch (InterruptedException ex) {
                // Ignore
            }
        }).start();
    }

}
