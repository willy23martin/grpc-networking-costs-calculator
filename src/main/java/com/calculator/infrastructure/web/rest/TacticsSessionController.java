package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.TacticsConfigDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session/tactics")
public class TacticsSessionController {

    static final String SESSION_KEY = "tacticsConfig";

    @PostMapping
    public ResponseEntity<Void> saveTactics(
            @RequestBody TacticsConfigDTO dto,
            HttpSession session) {

        session.setAttribute(SESSION_KEY, dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<TacticsConfigDTO> getTactics(HttpSession session) {
        TacticsConfigDTO dto = (TacticsConfigDTO) session.getAttribute(SESSION_KEY);
        return ResponseEntity.ok(dto != null ? dto : TacticsConfigDTO.empty());
    }

    @DeleteMapping
    public ResponseEntity<Void> clearTactics(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return ResponseEntity.noContent().build();
    }
}