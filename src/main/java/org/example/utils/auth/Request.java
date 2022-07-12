package org.example.utils.auth;

import com.google.gson.JsonObject;
import org.example.utils.auth.google.RequestType;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class Request {

    HttpsURLConnection urlConnection;
    protected JsonObject data;
    protected String contentType = "application/json";

    public Request(String url, RequestType type) {
        try {
            this.urlConnection = (HttpsURLConnection) new URL(url).openConnection();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        this.setRequestType(type);
        this.urlConnection.setDoOutput(true);
        this.urlConnection.setDoInput(true);

    }

    public Request setRequestType(RequestType requestType) {
        try {
            this.urlConnection.setRequestMethod(requestType.name());
        } catch (ProtocolException ex) {
            ex.printStackTrace();
        }
        return this;
    }

    public Request setContentType(String contentType) {
        this.contentType = contentType;
        this.urlConnection.setRequestProperty("Content-Type", contentType);
        this.urlConnection.setRequestProperty("Accept", contentType);
        return this;
    }

    public Request setData(JsonObject data) {
        this.data = data;
        return this;
    }

    /**
     * Executes the request and returns the response as a string.
     * @return CompletableFuture resolves to a string of the response.
     */
    public CompletableFuture<String> executeRequest() {

        if (this.urlConnection.getRequestMethod().equals("POST")) {
            try {
                this.urlConnection.getOutputStream().write(this.data.toString().getBytes());


                try (BufferedReader reader = new BufferedReader(
                        new java.io.InputStreamReader(this.urlConnection.getInputStream(),
                                StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    // TODO: Convert string to json
                    return CompletableFuture.completedFuture(sb.toString());
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        return null;
    }



}
