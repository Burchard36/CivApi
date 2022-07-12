package org.example.socket;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ServerSettings {

    public boolean logNewConnections;
    public boolean logDisconnections;
    public boolean logMessages;

    public boolean logInvalidSchema;

    public boolean logInvalidJson;

    public boolean logInvalidAction;


    public ServerSettings() {
        this.logNewConnections = true;
        this.logDisconnections = true;
        this.logMessages = true;
        this.logInvalidSchema = true;
        this.logInvalidJson = true;
        this.logInvalidAction = true;
    }
}
