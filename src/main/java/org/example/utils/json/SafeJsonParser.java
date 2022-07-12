package org.example.utils.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.victools.jsonschema.generator.*;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;


import java.util.Set;

public class SafeJsonParser {

    protected final ObjectMapper mapper;
    protected final JsonSchemaFactory factory;
    protected final SchemaGeneratorConfigBuilder configBuilder;
    protected final SchemaGeneratorConfig config;

    public SafeJsonParser() {
        this.mapper = new ObjectMapper();
        this.factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V201909);
        this.configBuilder = new SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09,
                OptionPreset.PLAIN_JSON)
                .with(Option.FORBIDDEN_ADDITIONAL_PROPERTIES_BY_DEFAULT);
        this.config = configBuilder.build();
    }

    public final ParsingResult parseJson(Class<?> structureClass,
                                             Object data,
                                                Gson gson) {
        final ParsingResult result = new ParsingResult();
        if (data instanceof String) {
            final Object parsedJson = parseRawJson((String) data, structureClass, gson);
            if (parsedJson == null) {
                result.setInvalidJson(true);
                return result;
            }
            if (!isValidSchema(data, structureClass)) {
                result.setInvalidSchema(true);
                return result;
            }
            result.setData(parsedJson);
            return result;
        }

        if (!isValidSchema(data, structureClass)) {
            result.setInvalidSchema(true);
            return result;
        }

        result.setData(gson.fromJson(gson.toJson(data), structureClass));
        return result;
    }

    protected static Object parseRawJson(String jsonString,
                                   Class<?> clazz,
                                    Gson gson) {
        try {
            return gson.fromJson(jsonString, clazz);
        } catch (JsonSyntaxException ex) {
            return null;
        }
    }

    public final boolean isValidSchema(Object dataObject, Class<?> toMatch) {
        SchemaGenerator generator = new SchemaGenerator(this.config);
        final JsonNode jsonSchemaConfig = generator.generateSchema(toMatch);
        JsonSchema schema = this.factory.getSchema(jsonSchemaConfig);
        Set<ValidationMessage> messages;

        if (dataObject instanceof String) {
            try {
                messages = schema.validate(this.mapper.readTree((String) dataObject));
            } catch (JsonProcessingException e) {
                return false;
            }
        } else messages = schema.validate(this.mapper.valueToTree(dataObject));

        return messages.size() <= 0;
    }

}
