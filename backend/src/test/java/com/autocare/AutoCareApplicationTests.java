package com.autocare;

import com.autocare.entity.Customer;
import com.autocare.entity.ServiceRecord;
import com.autocare.entity.Vehicle;
import com.autocare.exception.BadRequestException;
import com.autocare.exception.ResourceNotFoundException;
import com.autocare.repository.CustomerRepository;
import com.autocare.repository.ServiceRecordRepository;
import com.autocare.repository.VehicleRepository;
import com.autocare.service.CustomerService;
import com.autocare.service.DashboardService;
import com.autocare.service.ServiceRecordService;
import com.autocare.service.VehicleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AutoCareApplicationTests {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private VehicleService vehicleService;

    @Autowired
    private ServiceRecordService serviceRecordService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private ServiceRecordRepository serviceRecordRepository;

    private String randomSuffix() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    private String randomDigits(int len) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return sb.toString();
    }

    @Test
    @DisplayName("1. Dashboard stats should load database counts correctly")
    void testDashboardStatsLoading() {
        Map<String, Object> stats = dashboardService.getStats();
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalCustomers"));
        assertTrue(stats.containsKey("totalVehicles"));
        assertTrue(stats.containsKey("totalServiceRecords"));
        assertTrue(stats.containsKey("serviceRecordsThisMonth"));
        assertTrue(stats.containsKey("recentServices"));
    }

    @Test
    @DisplayName("2. Vehicles list should load all vehicles")
    void testVehiclesListLoading() {
        List<Vehicle> list = vehicleService.getAllVehicles();
        assertNotNull(list);
    }

    @Test
    @DisplayName("3. Combined customer and vehicle creation in one transactional workflow")
    @Transactional
    void testCombinedCustomerAndVehicleCreation() {
        String suffix = randomSuffix();
        Customer newCustomer = new Customer();
        newCustomer.setFullName("Test User " + suffix);
        newCustomer.setPhone("9812" + randomDigits(6));
        newCustomer.setEmail("test" + suffix + "@autocare.com");
        newCustomer.setAddress("42 Technology Park, Bangalore");

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber("KA01TC" + suffix);
        vehicle.setBrand("Honda");
        vehicle.setModel("Elevate");
        vehicle.setVariant("ZX CVT");
        vehicle.setManufacturingYear(2023);
        vehicle.setFuelType("Petrol");
        vehicle.setCustomer(newCustomer);

        Vehicle created = vehicleService.createVehicle(vehicle);

        assertNotNull(created.getVehicleId());
        assertEquals("KA01TC" + suffix, created.getRegistrationNumber());
        assertNotNull(created.getCustomer());
        assertNotNull(created.getCustomer().getCustomerId());
        assertEquals("Test User " + suffix, created.getCustomer().getFullName());
    }

    @Test
    @DisplayName("4. Associate new vehicle with existing selected customer")
    @Transactional
    void testExistingCustomerSelection() {
        String suffix = randomSuffix();
        Customer customer = new Customer();
        customer.setFullName("Existing Owner " + suffix);
        customer.setPhone("9988" + randomDigits(6));
        Customer savedCustomer = customerService.createCustomer(customer);

        Vehicle vehicle = new Vehicle();
        vehicle.setRegistrationNumber("KA02EX" + suffix);
        vehicle.setBrand("Hyundai");
        vehicle.setModel("Ioniq 5");
        vehicle.setFuelType("Electric");
        vehicle.setManufacturingYear(2024);

        Customer ref = new Customer();
        ref.setCustomerId(savedCustomer.getCustomerId());
        vehicle.setCustomer(ref);

        Vehicle created = vehicleService.createVehicle(vehicle);

        assertNotNull(created.getVehicleId());
        assertEquals(savedCustomer.getCustomerId(), created.getCustomer().getCustomerId());
        assertEquals(savedCustomer.getFullName(), created.getCustomer().getFullName());
    }

    @Test
    @DisplayName("5. One customer can own multiple vehicles")
    @Transactional
    void testMultipleVehiclesBelongingToOneCustomer() {
        String suffix = randomSuffix();
        Customer customer = new Customer();
        customer.setFullName("Multi Fleet " + suffix);
        customer.setPhone("9123" + randomDigits(6));
        Customer savedCustomer = customerService.createCustomer(customer);

        Customer ref = new Customer();
        ref.setCustomerId(savedCustomer.getCustomerId());

        Vehicle v1 = new Vehicle();
        v1.setRegistrationNumber("KA10M1" + suffix);
        v1.setBrand("Maruti Suzuki");
        v1.setModel("Swift");
        v1.setFuelType("Petrol");
        v1.setCustomer(ref);
        vehicleService.createVehicle(v1);

        Vehicle v2 = new Vehicle();
        v2.setRegistrationNumber("KA10M2" + suffix);
        v2.setBrand("Tata");
        v2.setModel("Harrier");
        v2.setFuelType("Diesel");
        v2.setCustomer(ref);
        vehicleService.createVehicle(v2);

        List<Vehicle> customerVehicles = vehicleService.getVehiclesByCustomer(savedCustomer.getCustomerId());
        assertEquals(2, customerVehicles.size());
    }

    @Test
    @DisplayName("6. Search vehicle by registration number (case-insensitive)")
    @Transactional
    void testVehicleSearchByRegistration() {
        String suffix = randomSuffix();
        Customer customer = new Customer();
        customer.setFullName("Search Test " + suffix);
        customer.setPhone("9456" + randomDigits(6));
        Customer savedCustomer = customerService.createCustomer(customer);

        Customer ref = new Customer();
        ref.setCustomerId(savedCustomer.getCustomerId());

        Vehicle v = new Vehicle();
        v.setRegistrationNumber("KA03SR" + suffix);
        v.setBrand("Toyota");
        v.setModel("Fortuner");
        v.setFuelType("Diesel");
        v.setCustomer(ref);
        vehicleService.createVehicle(v);

        // Lowercase query search
        Vehicle found = vehicleService.getVehicleByRegistration(("ka03sr" + suffix).toLowerCase());
        assertNotNull(found);
        assertEquals("KA03SR" + suffix, found.getRegistrationNumber());

        // Non-existent search throws 404
        assertThrows(ResourceNotFoundException.class, () ->
                vehicleService.getVehicleByRegistration("NONEXIST999"));
    }

    @Test
    @DisplayName("7. Search vehicles by customer name and customer phone number")
    @Transactional
    void testVehicleSearchByCustomerNameAndPhone() {
        String suffix = randomSuffix();
        Customer customer = new Customer();
        customer.setFullName("SpecialName" + suffix);
        customer.setPhone("9777" + randomDigits(6));
        Customer savedCustomer = customerService.createCustomer(customer);

        Customer ref = new Customer();
        ref.setCustomerId(savedCustomer.getCustomerId());

        Vehicle v = new Vehicle();
        v.setRegistrationNumber("KA04SP" + suffix);
        v.setBrand("Skoda");
        v.setModel("Slavia");
        v.setFuelType("Petrol");
        v.setCustomer(ref);
        vehicleService.createVehicle(v);

        // Search by customer name
        List<Vehicle> resultsByName = vehicleService.searchVehicles("SpecialName" + suffix);
        assertFalse(resultsByName.isEmpty());
        assertTrue(resultsByName.stream().anyMatch(veh -> veh.getRegistrationNumber().equals("KA04SP" + suffix)));

        // Search by customer phone
        List<Vehicle> resultsByPhone = vehicleService.searchVehicles(customer.getPhone());
        assertFalse(resultsByPhone.isEmpty());
        assertTrue(resultsByPhone.stream().anyMatch(veh -> veh.getRegistrationNumber().equals("KA04SP" + suffix)));
    }

    @Test
    @DisplayName("8. Service history loading, chronological ordering, and preservation")
    @Transactional
    void testServiceHistoryLoadingAndPreservation() {
        String suffix = randomSuffix();
        Customer customer = new Customer();
        customer.setFullName("Service Client " + suffix);
        customer.setPhone("9666" + randomDigits(6));
        Customer savedCustomer = customerService.createCustomer(customer);

        Customer ref = new Customer();
        ref.setCustomerId(savedCustomer.getCustomerId());

        Vehicle v = new Vehicle();
        v.setRegistrationNumber("KA05SH" + suffix);
        v.setBrand("Kia");
        v.setModel("Seltos");
        v.setFuelType("Petrol");
        v.setCustomer(ref);
        Vehicle savedVehicle = vehicleService.createVehicle(v);

        // First visit: 10,000 km
        ServiceRecord sr1 = new ServiceRecord();
        Vehicle vehRef = new Vehicle();
        vehRef.setVehicleId(savedVehicle.getVehicleId());
        sr1.setVehicle(vehRef);
        sr1.setServiceDate(LocalDate.now().minusMonths(6));
        sr1.setOdometerReading(10000);
        sr1.setServiceType("General Service");
        sr1.setWorkPerformed("Oil replacement");
        serviceRecordService.createServiceRecord(sr1);

        // Second visit: 20,000 km
        ServiceRecord sr2 = new ServiceRecord();
        sr2.setVehicle(vehRef);
        sr2.setServiceDate(LocalDate.now().minusMonths(1));
        sr2.setOdometerReading(20000);
        sr2.setServiceType("Periodic Maintenance");
        sr2.setWorkPerformed("Filter change, 20k check");
        serviceRecordService.createServiceRecord(sr2);

        // Load service history
        List<ServiceRecord> history = serviceRecordService.getServiceHistory(savedVehicle.getVehicleId());
        assertEquals(2, history.size());
        // Verify newest first: sr2 (20,000 km) should be first, sr1 (10,000 km) should be second
        assertEquals(20000, history.get(0).getOdometerReading());
        assertEquals(10000, history.get(1).getOdometerReading());
    }

    @Test
    @DisplayName("9. Reject duplicate vehicle registration numbers")
    @Transactional
    void testDuplicateRegistrationRejection() {
        String suffix = randomSuffix();
        Customer customer = new Customer();
        customer.setFullName("Dup Test " + suffix);
        customer.setPhone("9555" + randomDigits(6));
        Customer savedCustomer = customerService.createCustomer(customer);

        Customer ref = new Customer();
        ref.setCustomerId(savedCustomer.getCustomerId());

        Vehicle v1 = new Vehicle();
        v1.setRegistrationNumber("KA06DP" + suffix);
        v1.setBrand("MG");
        v1.setModel("Hector");
        v1.setFuelType("Petrol");
        v1.setCustomer(ref);
        vehicleService.createVehicle(v1);

        // Attempt second vehicle with identical registration number
        Vehicle v2 = new Vehicle();
        v2.setRegistrationNumber("KA06DP" + suffix);
        v2.setBrand("Honda");
        v2.setModel("Amaze");
        v2.setFuelType("Diesel");
        v2.setCustomer(ref);

        assertThrows(BadRequestException.class, () -> vehicleService.createVehicle(v2));
    }

    @Test
    @DisplayName("10. Reject negative and lower-than-previous odometer readings")
    @Transactional
    void testOdometerValidation() {
        String suffix = randomSuffix();
        Customer customer = new Customer();
        customer.setFullName("Odo Test " + suffix);
        customer.setPhone("9444" + randomDigits(6));
        Customer savedCustomer = customerService.createCustomer(customer);

        Customer ref = new Customer();
        ref.setCustomerId(savedCustomer.getCustomerId());

        Vehicle v = new Vehicle();
        v.setRegistrationNumber("KA07OD" + suffix);
        v.setBrand("Nissan");
        v.setModel("Magnite");
        v.setFuelType("Petrol");
        v.setCustomer(ref);
        Vehicle savedVehicle = vehicleService.createVehicle(v);

        Vehicle vehRef = new Vehicle();
        vehRef.setVehicleId(savedVehicle.getVehicleId());

        // Negative odometer rejected
        ServiceRecord negRecord = new ServiceRecord();
        negRecord.setVehicle(vehRef);
        negRecord.setServiceDate(LocalDate.now());
        negRecord.setOdometerReading(-100);
        negRecord.setServiceType("Check");
        negRecord.setWorkPerformed("Inspect");
        assertThrows(BadRequestException.class, () -> serviceRecordService.createServiceRecord(negRecord));

        // Create initial record at 25,000 km
        ServiceRecord validRecord = new ServiceRecord();
        validRecord.setVehicle(vehRef);
        validRecord.setServiceDate(LocalDate.now().minusDays(10));
        validRecord.setOdometerReading(25000);
        validRecord.setServiceType("Major Service");
        validRecord.setWorkPerformed("Timing belt replaced");
        serviceRecordService.createServiceRecord(validRecord);

        // Attempt lower odometer at 20,000 km -> must be rejected
        ServiceRecord lowerRecord = new ServiceRecord();
        lowerRecord.setVehicle(vehRef);
        lowerRecord.setServiceDate(LocalDate.now());
        lowerRecord.setOdometerReading(20000);
        lowerRecord.setServiceType("Checkup");
        lowerRecord.setWorkPerformed("Routine inspection");
        assertThrows(BadRequestException.class, () -> serviceRecordService.createServiceRecord(lowerRecord));
    }

    @Test
    @DisplayName("11. Deletion restrictions: Prevent customer delete with vehicles, prevent vehicle delete with service records")
    @Transactional
    void testDeletionRestrictions() {
        String suffix = randomSuffix();
        Customer customer = new Customer();
        customer.setFullName("Delete Protect " + suffix);
        customer.setPhone("9333" + randomDigits(6));
        Customer savedCustomer = customerService.createCustomer(customer);

        Customer ref = new Customer();
        ref.setCustomerId(savedCustomer.getCustomerId());

        Vehicle v = new Vehicle();
        v.setRegistrationNumber("KA08DL" + suffix);
        v.setBrand("Renault");
        v.setModel("Kiger");
        v.setFuelType("Petrol");
        v.setCustomer(ref);
        Vehicle savedVehicle = vehicleService.createVehicle(v);

        // Cannot delete customer while owning vehicle
        assertThrows(BadRequestException.class, () ->
                customerService.deleteCustomer(savedCustomer.getCustomerId()));

        // Add service record to vehicle
        Vehicle vehRef = new Vehicle();
        vehRef.setVehicleId(savedVehicle.getVehicleId());
        ServiceRecord sr = new ServiceRecord();
        sr.setVehicle(vehRef);
        sr.setServiceDate(LocalDate.now());
        sr.setOdometerReading(5000);
        sr.setServiceType("First Service");
        sr.setWorkPerformed("Full vehicle inspection");
        serviceRecordService.createServiceRecord(sr);

        // Cannot delete vehicle while having service records
        assertThrows(BadRequestException.class, () ->
                vehicleService.deleteVehicle(savedVehicle.getVehicleId()));
    }
}
