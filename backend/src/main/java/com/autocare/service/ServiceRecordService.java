package com.autocare.service;

import com.autocare.entity.ServiceRecord;
import com.autocare.entity.Vehicle;
import com.autocare.exception.BadRequestException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.repository.ServiceRecordRepository;
import com.autocare.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ServiceRecordService {

    private final ServiceRecordRepository serviceRecordRepository;
    private final VehicleRepository vehicleRepository;

    public ServiceRecordService(ServiceRecordRepository serviceRecordRepository,
                                VehicleRepository vehicleRepository) {
        this.serviceRecordRepository = serviceRecordRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<ServiceRecord> getAllServiceRecords() {
        List<ServiceRecord> records = serviceRecordRepository.findAll();
        initializeAssociations(records);
        return records;
    }

    @Transactional(readOnly = true)
    public ServiceRecord getServiceRecordById(Long id) {
        ServiceRecord record = serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service record not found with id: " + id));
        initializeAssociation(record);
        return record;
    }

    @Transactional(readOnly = true)
    public List<ServiceRecord> getServiceHistory(Long vehicleId) {
        List<ServiceRecord> records = serviceRecordRepository
                .findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);
        initializeAssociations(records);
        return records;
    }

    public ServiceRecord createServiceRecord(ServiceRecord record) {
        Long vehicleId = extractVehicleId(record);
        if (vehicleId == null) {
            throw new BadRequestException("Vehicle is required");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        validateServiceRecord(record);
        validateOdometerForCreate(vehicleId, record.getOdometerReading());

        record.setVehicle(vehicle);
        ServiceRecord saved = serviceRecordRepository.save(record);
        initializeAssociation(saved);
        return saved;
    }

    public ServiceRecord updateServiceRecord(Long id, ServiceRecord record) {
        ServiceRecord existing = serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service record not found with id: " + id));

        Long vehicleId = extractVehicleId(record);
        if (vehicleId == null) {
            throw new BadRequestException("Vehicle is required");
        }

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));

        validateServiceRecord(record);
        validateOdometerForUpdate(vehicleId, record.getOdometerReading(), id);

        existing.setVehicle(vehicle);
        existing.setServiceDate(record.getServiceDate());
        existing.setOdometerReading(record.getOdometerReading());
        existing.setServiceType(record.getServiceType());
        existing.setCustomerComplaint(record.getCustomerComplaint());
        existing.setWorkPerformed(record.getWorkPerformed());
        existing.setPartsReplaced(record.getPartsReplaced());
        existing.setRemarks(record.getRemarks());

        ServiceRecord saved = serviceRecordRepository.save(existing);
        initializeAssociation(saved);
        return saved;
    }

    public void deleteServiceRecord(Long id) {
        ServiceRecord record = serviceRecordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Service record not found with id: " + id));
        serviceRecordRepository.delete(record);
    }

    private void validateServiceRecord(ServiceRecord record) {
        if (record.getServiceDate() == null) {
            throw new BadRequestException("Service date is required");
        }

        if (record.getServiceDate().isAfter(LocalDate.now().plusDays(1))) {
            throw new BadRequestException("Service date cannot be in the future");
        }

        if (record.getOdometerReading() == null) {
            throw new BadRequestException("Odometer reading is required");
        }

        if (record.getOdometerReading() < 0) {
            throw new BadRequestException("Odometer reading cannot be negative");
        }

        if (record.getServiceType() == null || record.getServiceType().trim().isEmpty()) {
            throw new BadRequestException("Service type is required");
        }

        if (record.getWorkPerformed() == null || record.getWorkPerformed().trim().isEmpty()) {
            throw new BadRequestException("Work performed is required");
        }
    }

    private void validateOdometerForCreate(Long vehicleId, Integer odometerReading) {
        if (odometerReading == null) {
            return;
        }

        Optional<ServiceRecord> latestRecord = serviceRecordRepository
                .findTopByVehicleVehicleIdOrderByServiceDateDescServiceIdDesc(vehicleId);

        if (latestRecord.isPresent() && latestRecord.get().getOdometerReading() != null) {
            if (odometerReading < latestRecord.get().getOdometerReading()) {
                throw new BadRequestException(
                        "Odometer reading (" + odometerReading + ") cannot be less than the previous reading ("
                                + latestRecord.get().getOdometerReading() + ")");
            }
        }
    }

    private void validateOdometerForUpdate(Long vehicleId, Integer odometerReading, Long currentRecordId) {
        if (odometerReading == null) {
            return;
        }

        // Get all records for this vehicle, excluding the current one being updated
        List<ServiceRecord> records = serviceRecordRepository
                .findByVehicleVehicleIdOrderByServiceDateDesc(vehicleId);

        for (ServiceRecord rec : records) {
            if (rec.getServiceId().equals(currentRecordId)) {
                continue;
            }
            if (rec.getOdometerReading() != null && odometerReading < rec.getOdometerReading()) {
                throw new BadRequestException(
                        "Odometer reading (" + odometerReading + ") cannot be less than an existing reading ("
                                + rec.getOdometerReading() + ")");
            }
        }
    }

    private Long extractVehicleId(ServiceRecord record) {
        if (record.getVehicle() != null && record.getVehicle().getVehicleId() != null) {
            return record.getVehicle().getVehicleId();
        }
        return null;
    }

    private void initializeAssociations(List<ServiceRecord> records) {
        records.forEach(this::initializeAssociation);
    }

    private void initializeAssociation(ServiceRecord record) {
        if (record.getVehicle() != null) {
            record.getVehicle().getRegistrationNumber(); // force init vehicle
            if (record.getVehicle().getCustomer() != null) {
                record.getVehicle().getCustomer().getFullName(); // force init customer
            }
        }
    }
}
