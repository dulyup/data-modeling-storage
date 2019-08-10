package com.neu.demo.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.JsonNodeReader;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.io.Resources;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lyupingdu
 * @date 2019-07-24.
 */
public class JsonMapUtil {

    public static JsonNode schemaNode = getSchemaNode();

    public static Map<String, Object> jsonToMap(String json) throws IOException {
        return new ObjectMapper().readValue(json, HashMap.class);
    }

    private static JsonNode getJsonNodeFromFile(String filePath) {
        JsonNode jsonNode = null;
        try {
            jsonNode = new JsonNodeReader().fromReader(new FileReader(filePath));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonNode;
    }

    public static boolean validateJson(String json) {
        JsonNode jsonNode = getJsonNodeFromString(json);
        ProcessingReport report = JsonSchemaFactory.byDefault().getValidator().validateUnchecked(schemaNode, jsonNode);
        return report.isSuccess();
    }

    private static JsonNode getSchemaNode() {
        return getJsonNodeFromFile(Resources.getResource("schema.json").getFile());
    }

    private static JsonNode getJsonNodeFromString(String jsonStr) {
        JsonNode jsonNode = null;
        try {
            jsonNode = JsonLoader.fromString(jsonStr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return jsonNode;
    }
}