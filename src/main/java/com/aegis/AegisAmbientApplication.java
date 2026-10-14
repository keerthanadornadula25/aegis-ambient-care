package com.aegis;

import com.aegis.model.Patient;
import com.aegis.repo.PatientRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class AegisAmbientApplication {

    private static final Logger log = LoggerFactory.getLogger(AegisAmbientApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AegisAmbientApplication.class, args);
    }

    @Bean
    public CommandLineRunner initDatabase(PatientRepository patientRepository) {
        return args -> {
            if (patientRepository.count() == 0) {
                Patient demoPatient = new Patient(
                        1L,
                        "John Doe",
                        78,
                        "O+",
                        "Penicillin",
                        "Type 2 Diabetes, Mild Cognitive Impairment",
                        "123 Main St, Apt 4B, Springfield",
                        "Jane Doe (Daughter)",
                        "(555) 234-5678"
                );
                patientRepository.save(demoPatient);
                log.info("Initialized demo patient profile for [{}], ID: 1 (Demo Data — Not Real Patient Information)", demoPatient.getName());
            }
        };
    }
}
