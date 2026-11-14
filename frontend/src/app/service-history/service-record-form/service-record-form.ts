import { Component, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { ServiceRecordService } from '../../services/service-record.service';
import { VehicleService } from '../../services/vehicle.service';
import { Vehicle } from '../../models/vehicle.model';

@Component({
  selector: 'app-service-record-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './service-record-form.html',
  styleUrl: './service-record-form.css'
})
export class ServiceRecordForm implements OnInit {
  serviceForm!: FormGroup;
  isEditMode = signal(false);
  serviceId = signal<number | null>(null);
  vehicleId = signal<number | null>(null);
  vehicle = signal<Vehicle | null>(null);

  loading = signal(false);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  serviceTypes = [
    'Periodic Maintenance',
    'General Service',
    'Oil & Filter Change',
    'Brake System Service',
    'Tire Rotation & Alignment',
    'Battery Inspection / Replacement',
    'AC & Cooling Service',
    'Engine Diagnostics & Repair',
    'Suspension & Steering',
    'Electrical & Electronics',
    'Body Work & Detailing',
    'Pre-Purchase Inspection',
    'Other Service'
  ];

  today = new Date().toISOString().split('T')[0];

  constructor(
    private fb: FormBuilder,
    private serviceRecordService: ServiceRecordService,
    private vehicleService: VehicleService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();

    const idParam = this.route.snapshot.paramMap.get('id');
    const vehicleIdParam = this.route.snapshot.paramMap.get('vehicleId');

    if (idParam) {
      // Edit mode
      this.isEditMode.set(true);
      this.serviceId.set(+idParam);
      this.loadServiceRecord(+idParam);
    } else if (vehicleIdParam) {
      // Add mode for specific vehicle
      this.vehicleId.set(+vehicleIdParam);
      this.loadVehicle(+vehicleIdParam);
    }
  }

  private initForm(): void {
    this.serviceForm = this.fb.group({
      serviceDate: [this.today, [Validators.required]],
      odometerReading: [null, [Validators.required, Validators.min(0)]],
      serviceType: ['', [Validators.required]],
      customerComplaint: ['', [Validators.maxLength(500)]],
      workPerformed: ['', [Validators.required, Validators.maxLength(1000)]],
      partsReplaced: ['', [Validators.maxLength(500)]],
      remarks: ['', [Validators.maxLength(500)]]
    });
  }

  private loadVehicle(id: number): void {
    this.loading.set(true);
    this.cdr.markForCheck();

    this.vehicleService.getById(id).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (veh) => {
        this.vehicle.set(veh);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage.set('Vehicle not found.');
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  private loadServiceRecord(id: number): void {
    this.loading.set(true);
    this.cdr.markForCheck();

    this.serviceRecordService.getById(id).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (record) => {
        this.vehicle.set(record.vehicle || null);
        if (record.vehicle?.vehicleId) {
          this.vehicleId.set(record.vehicle.vehicleId);
        }

        this.serviceForm.patchValue({
          serviceDate: record.serviceDate,
          odometerReading: record.odometerReading,
          serviceType: record.serviceType,
          customerComplaint: record.customerComplaint || '',
          workPerformed: record.workPerformed,
          partsReplaced: record.partsReplaced || '',
          remarks: record.remarks || ''
        });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage.set('Service record not found.');
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  onSubmit(): void {
    if (this.serviceForm.invalid || this.submitting() || !this.vehicleId()) {
      this.serviceForm.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.cdr.markForCheck();

    const formVal = this.serviceForm.value;

    const payload = {
      serviceDate: formVal.serviceDate,
      odometerReading: +formVal.odometerReading,
      serviceType: formVal.serviceType,
      customerComplaint: formVal.customerComplaint ? formVal.customerComplaint.trim() : null,
      workPerformed: formVal.workPerformed.trim(),
      partsReplaced: formVal.partsReplaced ? formVal.partsReplaced.trim() : null,
      remarks: formVal.remarks ? formVal.remarks.trim() : null,
      vehicle: { vehicleId: this.vehicleId()! }
    };

    if (this.isEditMode() && this.serviceId()) {
      this.serviceRecordService.update(this.serviceId()!, payload as any).pipe(
        finalize(() => {
          this.submitting.set(false);
          this.cdr.markForCheck();
        })
      ).subscribe({
        next: () => {
          this.navigateBackToVehicle();
        },
        error: (err) => {
          const msg = err.error?.error || 'Failed to update service record.';
          this.errorMessage.set(msg);
          this.cdr.markForCheck();
        }
      });
    } else {
      this.serviceRecordService.create(payload as any).pipe(
        finalize(() => {
          this.submitting.set(false);
          this.cdr.markForCheck();
        })
      ).subscribe({
        next: () => {
          this.navigateBackToVehicle();
        },
        error: (err) => {
          const msg = err.error?.error || 'Failed to log service record. Please check odometer and date values.';
          this.errorMessage.set(msg);
          this.cdr.markForCheck();
        }
      });
    }
  }

  navigateBackToVehicle(): void {
    const veh = this.vehicle();
    if (veh?.registrationNumber) {
      this.router.navigate(['/service-history'], { queryParams: { reg: veh.registrationNumber } });
    } else if (this.vehicleId()) {
      this.router.navigate(['/vehicles', this.vehicleId()]);
    } else {
      this.router.navigate(['/service-history']);
    }
  }
}
