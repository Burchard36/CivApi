package org.example.utils;

import com.google.gson.JsonObject;

public class Errors {

    public static JsonObject malformedJsonObject() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("response_action", "ERROR");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject.addProperty("error", "Malformed JSON object was received by the client, please contact a developer!");

        jsonObject.add("response", jsonObject2);
        return jsonObject;
    }

    public static JsonObject invalidGoogleLogin() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("response_action", "ERROR");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("error", "Invalid google authentication, please try again.");

        jsonObject.add("response", jsonObject2);
        return jsonObject;
    }

    public static JsonObject internalServerError() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("response_action", "ERROR");

        JsonObject jsonObject2 = new JsonObject();
        jsonObject2.addProperty("error", "Internal server error! If this keeps persisting contact a developer with steps to reproduce!");

        jsonObject.add("response", jsonObject2);
        return jsonObject;
    }

}