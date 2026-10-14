package com.aegis.repo;

import com.aegis.model.SensorEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorEventRepository extends JpaRepository<SensorEvent, Long> {
    List<SensorEvent> findTop20ByPatientIdOrderByTimestampDesc(Long patientId);
}
