package com.autocare.controller;

import com.autocare.entity.ServiceRecord;
import com.autocare.service.ServiceRecordService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/service-records")
public class ServiceRecordController {

    private final ServiceRecordService serviceRecordService;

    public ServiceRecordController(ServiceRecordService serviceRecordService) {
        this.serviceRecordService = serviceRecordService;
    }

    @GetMapping
    public ResponseEntity<List<ServiceRecord>> getAllServiceRecords() {
        return ResponseEntity.ok(serviceRecordService.getAllServiceRecords());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServiceRecord> getServiceRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceRecordService.getServiceRecordById(id));
    }

    @PostMapping
    public ResponseEntity<ServiceRecord> createServiceRecord(@RequestBody ServiceRecord record) {
        ServiceRecord created = serviceRecordService.createServiceRecord(record);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ServiceRecord> updateServiceRecord(@PathVariable Long id,
                                                              @RequestBody ServiceRecord record) {
        return ResponseEntity.ok(serviceRecordService.updateServiceRecord(id, record));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteServiceRecord(@PathVariable Long id) {
        serviceRecordService.deleteServiceRecord(id);
        return ResponseEntity.noContent().build();
    }
}
