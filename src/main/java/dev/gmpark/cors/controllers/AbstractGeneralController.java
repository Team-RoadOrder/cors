package dev.gmpark.cors.controllers;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractGeneralController {
    protected AbstractGeneralController() {
        super();
    }
    protected Map<String , Object>  prepareJsonResponse (Enum<?> result) {
        Map<String , Object> response = new HashMap<>();
        response.put("result", result.name());
        return response;
    }

}
