package org.example.utils.auth.google.json;

public class AuthenticationResult {

    public String access_token;
    public String refresh_token;

    public AuthenticationResult(String access_token, String refresh_token) {
        this.access_token = access_token;
        this.refresh_token = refresh_token;
    }
}