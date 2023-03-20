package io.sapl.functions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import io.sapl.api.functions.Function;
import io.sapl.api.functions.FunctionLibrary;
import io.sapl.api.interpreter.Val;
import io.sapl.api.validation.JsonObject;
import io.sapl.api.validation.Text;

@FunctionLibrary(name = SchemaValidationLibrary.NAME, description = SchemaValidationLibrary.DESCRIPTION)
public class SchemaValidationLibrary {

    public static final String NAME = "schema";

    public static final String DESCRIPTION = "This library contains the mandatory functions for testing the compliance of a JSON object with a JSON schema.";

    private static final String ISCOMPLIANTWITHSCHEMA_DOC = "isCompliantWithSchema(jsonSchema, jsonVar):" +
            "Assumes that jsonSchema is a String that can be converted into a valid JSON schema, and jsonVar is a valid JSON Object or a String that can be converted to a JSON node." +
            "If jsonSchema cannot be converted to a JSON schema, or jsonVar cannot be converted to a JSON node, returns an error." +
            "If jsonVar is compliant with jsonSchema, returns TRUE, else returns FALSE.";

    private static final String INVALID_JSON_OBJECT = "Illegal parameter for JSON object. jsonObject parameter must be a valid JSON object or a String that can be converted to a JSON object.";

    private static final String INVALID_JSON_SCHEMA = "Illegal parameter for JSON schema. jsonSchema parameter must be a String that can be converted to a valid JSON schema.";

    private static final SpecVersion.VersionFlag SPEC_VERSION_JSON_SCHEMA = SpecVersion.VersionFlag.V4;


    @Function(docs = ISCOMPLIANTWITHSCHEMA_DOC)
    public static Val isCompliantWithSchema(@JsonObject @Text Val jsonObject, @Text Val jsonSchema){
        JsonNode jsonNode = null;
        String jsonSchemaAsString = jsonSchema.get().asText();
        var schema = jsonSchemaFromString(jsonSchemaAsString);

        if(jsonObject.isTextual()){
            jsonNode = jsonNodeFromString(jsonObject.get().asText());
        } else if (jsonObject.isObject()){
            jsonNode = jsonObject.getJsonNode();
        }

        var errors = schema.validate(jsonNode);
        return Val.of(errors.isEmpty());
    }

    private static JsonNode jsonNodeFromString(String node){
        JsonNode jsonNode;
        try {
            jsonNode = new ObjectMapper().readTree(node);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(INVALID_JSON_OBJECT);
        }
        return jsonNode;
    }

    private static JsonSchema jsonSchemaFromString(String schema) {
        JsonSchema jsonSchema;
        var jsonSchemaFactory = JsonSchemaFactory.getInstance(SPEC_VERSION_JSON_SCHEMA);
        try {
            jsonSchema = jsonSchemaFactory.getSchema(schema);
        } catch (Exception e){
            throw new IllegalArgumentException(INVALID_JSON_SCHEMA);
        }
        return jsonSchema;
    }

}
