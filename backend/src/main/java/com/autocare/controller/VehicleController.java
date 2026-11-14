package com.autocare.controller;

import com.autocare.entity.ServiceRecord;
import com.autocare.entity.Vehicle;
import com.autocare.service.ServiceRecordService;
import com.autocare.service.VehicleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;
    private final ServiceRecordService serviceRecordService;

    public VehicleController(VehicleService vehicleService, ServiceRecordService serviceRecordService) {
        this.vehicleService = vehicleService;
        this.serviceRecordService = serviceRecordService;
    }

    @GetMapping
    public ResponseEntity<List<Vehicle>> getAllVehicles(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "customerId", required = false) Long customerId) {
        List<Vehicle> vehicles;
        if (customerId != null) {
            vehicles = vehicleService.getVehiclesByCustomer(customerId);
        } else if (search != null && !search.trim().isEmpty()) {
            vehicles = vehicleService.searchVehicles(search);
        } else {
            vehicles = vehicleService.getAllVehicles();
        }
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vehicle> getVehicleById(@PathVariable Long id) {
        return ResponseEntity.ok(vehicleService.getVehicleById(id));
    }

    @GetMapping("/registration/{registrationNumber}")
    public ResponseEntity<Vehicle> getVehicleByRegistration(@PathVariable String registrationNumber) {
        return ResponseEntity.ok(vehicleService.getVehicleByRegistration(registrationNumber));
    }

    @PostMapping
    public ResponseEntity<Vehicle> createVehicle(@RequestBody Vehicle vehicle) {
        Vehicle created = vehicleService.createVehicle(vehicle);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Vehicle> updateVehicle(@PathVariable Long id, @RequestBody Vehicle vehicle) {
        return ResponseEntity.ok(vehicleService.updateVehicle(id, vehicle));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Long id) {
        vehicleService.deleteVehicle(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{vehicleId}/service-history")
    public ResponseEntity<List<ServiceRecord>> getServiceHistory(@PathVariable Long vehicleId) {
        return ResponseEntity.ok(serviceRecordService.getServiceHistory(vehicleId));
    }
}
