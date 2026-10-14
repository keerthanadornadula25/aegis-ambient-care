package com.aegis.repo;

import com.aegis.model.Incident;
import com.aegis.model.IncidentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {
    List<Incident> findByPatientIdOrderByDetectedAtDesc(Long patientId);
    List<Incident> findByPatientIdAndStatusIn(Long patientId, List<IncidentStatus> statuses);
    long countByPatientIdAndStatusIn(Long patientId, List<IncidentStatus> statuses);
}
