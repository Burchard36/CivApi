package org.example.utils.auth;

import org.example.utils.auth.Request;
import org.example.utils.auth.google.RequestType;

public abstract class Requester {

    protected final Request initializePostRequest(String url) {
        return new Request(url, RequestType.POST);
    }

}
