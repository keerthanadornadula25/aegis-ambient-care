package com.aegis.service;

import com.aegis.dto.IncidentDto;
import com.aegis.engine.IncidentStateMachine;
import com.aegis.engine.RuleEngine;
import com.aegis.model.Incident;
import com.aegis.model.IncidentStatus;
import com.aegis.repo.IncidentRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final IncidentStateMachine stateMachine;
    private final RuleEngine ruleEngine;

    public IncidentService(IncidentRepository incidentRepository,
                           IncidentStateMachine stateMachine,
                           RuleEngine ruleEngine) {
        this.incidentRepository = incidentRepository;
        this.stateMachine = stateMachine;
        this.ruleEngine = ruleEngine;
    }

    public List<IncidentDto> getIncidents(Long patientId) {
        List<Incident> list = incidentRepository.findByPatientIdOrderByDetectedAtDesc(patientId != null ? patientId : 1L);
        return list.stream().map(IncidentDto::new).collect(Collectors.toList());
    }

    public boolean resolveIncident(Long incidentId) {
        return stateMachine.resolveIncident(1L, incidentId);
    }

    public void resetDemo() {
        stateMachine.resetDemo(1L);
        ruleEngine.reset();
    }

    public long countActiveIncidents(Long patientId) {
        return incidentRepository.countByPatientIdAndStatusIn(
                patientId != null ? patientId : 1L,
                List.of(IncidentStatus.CHECK_IN, IncidentStatus.COUNTDOWN, IncidentStatus.ALERT)
        );
    }
}
