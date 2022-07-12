package org.example.utils.notifier;

public class Notifier {
    public NotifyEvent notifyEvent;

    public Notifier() {

    }

    public void notifySync() {
        this.notifyEvent.onNotified();
    }

    public Notifier onNotified(NotifyEvent event) {
        this.notifyEvent = event;
        return this;
    }

}
