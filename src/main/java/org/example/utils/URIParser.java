package org.example.utils;

import java.net.URI;
import java.net.URISyntaxException;

public class URIParser {
    public static URI getUri(String uri) {
        URI returnUri;
        try {
            returnUri = new URI(uri);
        } catch (URISyntaxException ex) {
            return null;
        }
        return returnUri;
    }
}
