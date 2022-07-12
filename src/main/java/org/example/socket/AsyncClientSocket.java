package org.example.socket;

import org.example.utils.URIParser;

import java.net.URI;
import java.net.URISyntaxException;

public class AsyncClientSocket {

    protected URI connectionUri;

    public AsyncClientSocket(final String host) {
        final URI uri = URIParser.getUri(host);
        if (uri == null) {
            throw new IllegalArgumentException("Invalid host: " + host);
        }

        this.connectionUri = uri;


    }


}
