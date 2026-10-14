package com.aegis.controller;

import com.aegis.dto.DemoResetResponse;
import com.aegis.engine.IncidentStateMachine;
import com.aegis.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DemoController {

    private final IncidentService incidentService;
    private final IncidentStateMachine stateMachine;

    public DemoController(IncidentService incidentService, IncidentStateMachine stateMachine) {
        this.incidentService = incidentService;
        this.stateMachine = stateMachine;
    }

    @PostMapping("/demo/reset")
    public ResponseEntity<DemoResetResponse> resetDemo() {
        incidentService.resetDemo();
        return ResponseEntity.ok(new DemoResetResponse(true, "Demo state reset successfully. All safety rules and verification timers reset to NORMAL."));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "app", "Aegis Ambient",
                "timestamp", Instant.now().toString(),
                "patientState", stateMachine.getCurrentStatus().name()
        ));
    }
}
