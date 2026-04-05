package com.calculator.application.services.mapper.tactics;

import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class TacticsMapperService {

    public Map<String, String> tacticEntry(String name, String value, String description,
                                            String rpsImpact) {
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("name",        name);
        entry.put("value",       value != null ? value : "—");
        entry.put("description", description);
        entry.put("rpsImpact",   rpsImpact != null ? rpsImpact : "");
        return entry;
    }

}
