package com.autocare.service;

import com.autocare.entity.Customer;
import com.autocare.exception.BadRequestException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.repository.CustomerRepository;
import com.autocare.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;

    public CustomerService(CustomerRepository customerRepository, VehicleRepository vehicleRepository) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomers(String query) {
        if (query == null || query.trim().isEmpty()) {
            return customerRepository.findAll();
        }
        return customerRepository.findByFullNameContainingIgnoreCaseOrPhoneContaining(query, query);
    }

    public Customer createCustomer(Customer customer) {
        validateCustomer(customer);
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer customer) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        validateCustomer(customer);

        existing.setFullName(customer.getFullName());
        existing.setPhone(customer.getPhone());
        existing.setEmail(customer.getEmail());
        existing.setAddress(customer.getAddress());

        return customerRepository.save(existing);
    }

    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + id));

        if (vehicleRepository.existsByCustomerCustomerId(id)) {
            throw new BadRequestException("Cannot delete customer who has registered vehicles");
        }

        customerRepository.delete(customer);
    }

    private void validateCustomer(Customer customer) {
        if (customer.getFullName() == null || customer.getFullName().trim().isEmpty()) {
            throw new BadRequestException("Full name is required");
        }

        if (customer.getPhone() == null || customer.getPhone().trim().isEmpty()) {
            throw new BadRequestException("Phone number is required");
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
}
