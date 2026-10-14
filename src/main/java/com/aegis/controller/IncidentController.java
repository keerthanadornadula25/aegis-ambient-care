package com.aegis.controller;

import com.aegis.dto.IncidentDto;
import com.aegis.service.IncidentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/incidents")
@CrossOrigin(origins = "*")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<List<IncidentDto>> getIncidents(@RequestParam(value = "patientId", defaultValue = "1") Long patientId) {
        return ResponseEntity.ok(incidentService.getIncidents(patientId));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<Map<String, Object>> resolveIncident(@PathVariable Long id) {
        boolean resolved = incidentService.resolveIncident(id);
        return ResponseEntity.ok(Map.of(
                "success", resolved,
                "message", resolved ? "Incident #" + id + " has been marked as resolved." : "Incident not found or already resolved."
        ));
    }
}
