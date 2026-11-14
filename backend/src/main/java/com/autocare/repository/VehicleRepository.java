package com.autocare.repository;

import com.autocare.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByRegistrationNumberIgnoreCase(String registrationNumber);
    Optional<Vehicle> findByRegistrationNumber(String registrationNumber);

    List<Vehicle> findByCustomerCustomerId(Long customerId);

    @Query("SELECT v FROM Vehicle v LEFT JOIN v.customer c WHERE " +
           "LOWER(v.registrationNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(v.brand) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(v.model) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "(v.variant IS NOT NULL AND LOWER(v.variant) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(c.fullName IS NOT NULL AND LOWER(c.fullName) LIKE LOWER(CONCAT('%', :query, '%'))) OR " +
           "(c.phone IS NOT NULL AND c.phone LIKE CONCAT('%', :query, '%'))")
    List<Vehicle> searchVehicles(@Param("query") String query);

    List<Vehicle> findByRegistrationNumberContainingIgnoreCaseOrBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
            String registrationNumber, String brand, String model);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByCustomerCustomerId(Long customerId);
}
