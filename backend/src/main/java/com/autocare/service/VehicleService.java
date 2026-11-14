package com.autocare.service;

import com.autocare.entity.Customer;
import com.autocare.entity.Vehicle;
import com.autocare.exception.BadRequestException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.repository.CustomerRepository;
import com.autocare.repository.ServiceRecordRepository;
import com.autocare.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final CustomerRepository customerRepository;
    private final ServiceRecordRepository serviceRecordRepository;

    public VehicleService(VehicleRepository vehicleRepository,
                          CustomerRepository customerRepository,
                          ServiceRecordRepository serviceRecordRepository) {
        this.vehicleRepository = vehicleRepository;
        this.customerRepository = customerRepository;
        this.serviceRecordRepository = serviceRecordRepository;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        List<Vehicle> vehicles = vehicleRepository.findAll();
        // Force initialization of customer within the transaction
        vehicles.forEach(v -> {
            if (v.getCustomer() != null) {
                v.getCustomer().getFullName();
            }
        });
        return vehicles;
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));
        // Force initialization of customer
        if (vehicle.getCustomer() != null) {
            vehicle.getCustomer().getFullName();
        }
        return vehicle;
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicleByRegistration(String regNum) {
        if (regNum == null || regNum.trim().isEmpty()) {
            throw new BadRequestException("Registration number is required");
        }
        String normalized = regNum.trim().toUpperCase();
        return vehicleRepository.findByRegistrationNumberIgnoreCase(normalized)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with registration number: " + normalized));
    }

    @Transactional(readOnly = true)
    public List<Vehicle> searchVehicles(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllVehicles();
        }
        List<Vehicle> vehicles = vehicleRepository.searchVehicles(query.trim());
        vehicles.forEach(v -> {
            if (v.getCustomer() != null) {
                v.getCustomer().getFullName();
            }
        });
        return vehicles;
    }

    @Transactional(readOnly = true)
    public List<Vehicle> getVehiclesByCustomer(Long customerId) {
        List<Vehicle> vehicles = vehicleRepository.findByCustomerCustomerId(customerId);
        vehicles.forEach(v -> {
            if (v.getCustomer() != null) {
                v.getCustomer().getFullName();
            }
        });
        return vehicles;
    }

    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicle.getCustomer() == null) {
            throw new BadRequestException("Customer information is required");
        }

        Customer customer;
        Long customerId = extractCustomerId(vehicle);

        if (customerId != null && customerId > 0) {
            // Existing customer selected
            customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        } else {
            // Create new customer and vehicle together in one transaction
            Customer newCustomer = vehicle.getCustomer();
            validateCustomerInfo(newCustomer);
            customer = customerRepository.save(newCustomer);
        }

        validateVehicle(vehicle);

        String regNum = vehicle.getRegistrationNumber().trim().toUpperCase();
        if (vehicleRepository.existsByRegistrationNumber(regNum)) {
            throw new BadRequestException("A vehicle with registration number " + regNum + " already exists");
        }

        vehicle.setRegistrationNumber(regNum);
        vehicle.setCustomer(customer);
        return vehicleRepository.save(vehicle);
    }

    public Vehicle updateVehicle(Long id, Vehicle vehicle) {
        Vehicle existing = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        validateVehicle(vehicle);

        if (vehicle.getCustomer() != null) {
            Long customerId = extractCustomerId(vehicle);
            if (customerId != null && customerId > 0) {
                Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));

                // If customer details were edited alongside vehicle, update them
                if (vehicle.getCustomer().getFullName() != null && !vehicle.getCustomer().getFullName().trim().isEmpty()) {
                    customer.setFullName(vehicle.getCustomer().getFullName().trim());
                }
                if (vehicle.getCustomer().getPhone() != null && !vehicle.getCustomer().getPhone().trim().isEmpty()) {
                    String phone = vehicle.getCustomer().getPhone().trim();
                    if (!phone.matches("\\d{10,15}")) {
                        throw new BadRequestException("Phone number must be between 10 and 15 digits");
                    }
                    customer.setPhone(phone);
                }
                if (vehicle.getCustomer().getEmail() != null) {
                    String email = vehicle.getCustomer().getEmail().trim();
                    if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                        throw new BadRequestException("Invalid email format");
                    }
                    customer.setEmail(email.isEmpty() ? null : email);
                }
                if (vehicle.getCustomer().getAddress() != null) {
                    customer.setAddress(vehicle.getCustomer().getAddress().trim());
                }
                customerRepository.save(customer);
                existing.setCustomer(customer);
            }
        }

        String regNum = vehicle.getRegistrationNumber().trim().toUpperCase();
        Optional<Vehicle> vehicleWithSameReg = vehicleRepository.findByRegistrationNumber(regNum);
        if (vehicleWithSameReg.isPresent() && !vehicleWithSameReg.get().getVehicleId().equals(id)) {
            throw new BadRequestException("A vehicle with registration number " + regNum + " already exists");
        }

        existing.setRegistrationNumber(regNum);
        existing.setBrand(vehicle.getBrand());
        existing.setModel(vehicle.getModel());
        existing.setVariant(vehicle.getVariant());
        existing.setManufacturingYear(vehicle.getManufacturingYear());
        existing.setFuelType(vehicle.getFuelType());

        Vehicle saved = vehicleRepository.save(existing);
        saved.getCustomer().getFullName();
        return saved;
    }

    private void validateCustomerInfo(Customer customer) {
        if (customer.getFullName() == null || customer.getFullName().trim().isEmpty()) {
            throw new BadRequestException("Customer full name is required");
        }

        if (customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
            throw new BadRequestException("Customer phone number is required");
        }

        String phone = customer.getPhone().trim();
        if (!phone.matches("\\d{10,15}")) {
            throw new BadRequestException("Phone number must be between 10 and 15 digits");
        }

        if (customer.getEmail() != null && !customer.getEmail().trim().isEmpty()) {
            String email = customer.getEmail().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                throw new BadRequestException("Invalid email format");
            }
        }
    }

    public void deleteVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with id: " + id));

        if (serviceRecordRepository.existsByVehicleVehicleId(id)) {
            throw new BadRequestException("Cannot delete vehicle that has service records");
        }

        vehicleRepository.delete(vehicle);
    }

    private void validateVehicle(Vehicle vehicle) {
        if (vehicle.getRegistrationNumber() == null || vehicle.getRegistrationNumber().trim().isEmpty()) {
            throw new BadRequestException("Registration number is required");
        }

        if (vehicle.getCustomer() == null) {
            throw new BadRequestException("Customer is required");
        }

        if (vehicle.getBrand() == null || vehicle.getBrand().trim().isEmpty()) {
            throw new BadRequestException("Brand is required");
        }

        if (vehicle.getModel() == null || vehicle.getModel().trim().isEmpty()) {
            throw new BadRequestException("Model is required");
        }

        if (vehicle.getFuelType() == null || vehicle.getFuelType().trim().isEmpty()) {
            throw new BadRequestException("Fuel type is required");
        }

        if (vehicle.getManufacturingYear() != null) {
            int currentYear = java.time.Year.now().getValue();
            if (vehicle.getManufacturingYear() < 1900 || vehicle.getManufacturingYear() > currentYear + 1) {
                throw new BadRequestException("Manufacturing year must be between 1900 and " + (currentYear + 1));
            }
        }
    }

    private Long extractCustomerId(Vehicle vehicle) {
        if (vehicle.getCustomer() != null && vehicle.getCustomer().getCustomerId() != null) {
            return vehicle.getCustomer().getCustomerId();
        }
        return null;
    }
}
