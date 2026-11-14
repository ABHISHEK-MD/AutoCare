import { Component, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { VehicleService } from '../../services/vehicle.service';
import { ServiceRecordService } from '../../services/service-record.service';
import { Vehicle } from '../../models/vehicle.model';
import { ServiceRecord } from '../../models/service-record.model';

@Component({
  selector: 'app-vehicle-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './vehicle-detail.html',
  styleUrl: './vehicle-detail.css'
})
export class VehicleDetail implements OnInit {
  vehicle = signal<Vehicle | null>(null);
  serviceRecords = signal<ServiceRecord[]>([]);
  vehicleId = signal<number>(0);
  loading = signal(true);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  constructor(
    private vehicleService: VehicleService,
    private serviceRecordService: ServiceRecordService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.vehicleId.set(+idParam);
      this.loadVehicleDetails(+idParam);
    }
  }

  loadVehicleDetails(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.cdr.markForCheck();

    this.vehicleService.getById(id).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (veh) => {
        this.vehicle.set(veh);
        this.loadServiceHistory(id);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error.set('Vehicle not found.');
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  private loadServiceHistory(vehicleId: number): void {
    this.serviceRecordService.getByVehicle(vehicleId).pipe(
      finalize(() => this.cdr.markForCheck())
    ).subscribe({
      next: (records) => {
        this.serviceRecords.set(records);
        this.cdr.markForCheck();
      },
      error: (err) => {
        console.error('Failed to load service records:', err);
      }
    });
  }

  deleteVehicle(): void {
    const veh = this.vehicle();
    if (!veh || !veh.vehicleId) return;
    const confirmed = window.confirm(
      `Are you sure you want to delete vehicle "${veh.registrationNumber}"?\n\nNote: Deletion will be rejected if this vehicle has any service history.`
    );
    if (!confirmed) return;

    this.loading.set(true);
    this.error.set(null);
    this.cdr.markForCheck();

    this.vehicleService.delete(veh.vehicleId).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.router.navigate(['/vehicles']);
      },
      error: (err) => {
        const msg = err.error?.error || 'Cannot delete vehicle that has recorded service history.';
        this.error.set(msg);
        this.cdr.markForCheck();
      }
    });
  }
}
