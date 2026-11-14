package com.autocare.repository;

import com.autocare.entity.ServiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceRecordRepository extends JpaRepository<ServiceRecord, Long> {

    List<ServiceRecord> findByVehicleVehicleIdOrderByServiceDateDesc(Long vehicleId);

    Optional<ServiceRecord> findTopByVehicleVehicleIdOrderByServiceDateDescServiceIdDesc(Long vehicleId);

    long countByServiceDateBetween(LocalDate start, LocalDate end);

    List<ServiceRecord> findTop10ByOrderByServiceDateDescServiceIdDesc();

    List<ServiceRecord> findTop10ByOrderByCreatedAtDesc();

    boolean existsByVehicleVehicleId(Long vehicleId);
}
