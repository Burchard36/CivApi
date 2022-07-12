package org.example.utils.auth.google;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.TokenRequest;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.auth.oauth2.TokenResponseException;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleOAuthConstants;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.example.utils.auth.google.json.AuthenticationResult;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GoogleHandler {

    protected final HttpTransport transport;
    protected final GsonFactory factory;
    protected final List<String> scopes = Arrays.asList("https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile");
    protected static String CLIENT_ID = "318189328718-pvq2lvac0r9nm85jct6f7vkpjkca404h.apps.googleusercontent.com";
    protected static String CLIENT_SECRET = "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";
    protected final GoogleAuthorizationCodeFlow authorizationCodeFlow;

    public GoogleHandler() {
        this.transport = new NetHttpTransport();
        this.factory = new GsonFactory();

        this.authorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
                this.transport,
                this.factory,
                CLIENT_ID,
                CLIENT_SECRET,
                this.scopes)
                .build();
    }



    public CompletableFuture<AuthenticationResult> requestAuthorizationCode(final String serverAuthCode) {
        AuthorizationCodeRequestUrl authorizationUrl =
                new AuthorizationCodeRequestUrl(GoogleOAuthConstants.TOKEN_SERVER_URL, serverAuthCode);
        TokenRequest tokenRequest = new TokenRequest(
                this.transport,
                this.factory,
                authorizationUrl,
                "authorization_code")
                .set("code", URLDecoder.decode(serverAuthCode, StandardCharsets.UTF_8))
                .set("client_id", CLIENT_ID)
                .set("client_secret", CLIENT_SECRET)
                .setScopes(this.scopes);


        TokenResponse tokenResponse;
        try {
            tokenResponse = tokenRequest.execute();
        } catch (IOException e) {
            if (e instanceof TokenResponseException tokExc) {
                tokExc.printStackTrace();
                System.out.println(tokExc.getMessage());
                System.out.println(tokExc.getDetails() + "");
            } else e.printStackTrace();

            return CompletableFuture.supplyAsync(() -> null);
        }

        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        return CompletableFuture.supplyAsync(() -> new AuthenticationResult(accessToken, refreshToken));
    }


}

