package com.calculator.infrastructure.web.rest;

import com.calculator.domain.dto.ArchitecturalDecisionsDTO;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session/tactics")
public class TacticsSessionController {

    static final String SESSION_KEY = "architecturalDecisions";

    @PostMapping
    public ResponseEntity<Void> saveTactics(
            @RequestBody ArchitecturalDecisionsDTO dto,
            HttpSession session) {

        session.setAttribute(SESSION_KEY, dto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<ArchitecturalDecisionsDTO> getTactics(HttpSession session) {
        ArchitecturalDecisionsDTO architecturalDecisionsDTO = (ArchitecturalDecisionsDTO) session.getAttribute(SESSION_KEY);
        return ResponseEntity.ok(architecturalDecisionsDTO != null ? architecturalDecisionsDTO : ArchitecturalDecisionsDTO.empty());
    }

    @DeleteMapping
    public ResponseEntity<Void> clearTactics(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
        return ResponseEntity.noContent().build();
    }
}