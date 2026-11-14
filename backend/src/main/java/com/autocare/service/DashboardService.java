package com.autocare.service;

import com.autocare.entity.ServiceRecord;
import com.autocare.repository.CustomerRepository;
import com.autocare.repository.ServiceRecordRepository;
import com.autocare.repository.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final CustomerRepository customerRepository;
    private final VehicleRepository vehicleRepository;
    private final ServiceRecordRepository serviceRecordRepository;

    public DashboardService(CustomerRepository customerRepository,
                            VehicleRepository vehicleRepository,
                            ServiceRecordRepository serviceRecordRepository) {
        this.customerRepository = customerRepository;
        this.vehicleRepository = vehicleRepository;
        this.serviceRecordRepository = serviceRecordRepository;
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalCustomers", customerRepository.count());
        stats.put("totalVehicles", vehicleRepository.count());
        stats.put("totalServiceRecords", serviceRecordRepository.count());

        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());
        stats.put("serviceRecordsThisMonth",
                serviceRecordRepository.countByServiceDateBetween(startOfMonth, endOfMonth));

        List<ServiceRecord> recentServices = serviceRecordRepository.findTop10ByOrderByServiceDateDescServiceIdDesc();
        // Force initialization of lazy associations
        recentServices.forEach(record -> {
            if (record.getVehicle() != null) {
                record.getVehicle().getRegistrationNumber();
                if (record.getVehicle().getCustomer() != null) {
                    record.getVehicle().getCustomer().getFullName();
                }
            }
        });
        stats.put("recentServices", recentServices);

        return stats;
    }
}
