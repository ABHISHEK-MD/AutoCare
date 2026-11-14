import { Component, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { VehicleService } from '../../services/vehicle.service';
import { ServiceRecordService } from '../../services/service-record.service';
import { Vehicle } from '../../models/vehicle.model';
import { ServiceRecord } from '../../models/service-record.model';

@Component({
  selector: 'app-service-search',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './service-search.html',
  styleUrl: './service-search.css'
})
export class ServiceSearch implements OnInit {
  regQuery = signal<string>('');
  searchedReg = signal<string>('');
  hasSearched = signal<boolean>(false);
  loading = signal<boolean>(false);
  notFound = signal<boolean>(false);
  errorMessage = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  vehicle = signal<Vehicle | null>(null);
  serviceRecords = signal<ServiceRecord[]>([]);

  // Search counter to prevent stale asynchronous responses
  private activeSearchId = 0;

  constructor(
    private vehicleService: VehicleService,
    private serviceRecordService: ServiceRecordService,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.queryParamMap.subscribe(params => {
      const reg = params.get('reg');
      if (reg) {
        this.regQuery.set(reg);
        this.performSearch();
      }
    });
  }

  onSearchSubmit(): void {
    if (!this.regQuery().trim()) return;
    this.performSearch();
  }

  performSearch(): void {
    const cleanReg = this.regQuery().trim().toUpperCase();
    if (!cleanReg) return;

    const currentSearchId = ++this.activeSearchId;

    this.loading.set(true);
    this.hasSearched.set(true);
    this.searchedReg.set(cleanReg);
    this.notFound.set(false);
    this.errorMessage.set(null);
    this.vehicle.set(null);
    this.serviceRecords.set([]);
    this.cdr.markForCheck();

    this.vehicleService.getByRegistration(cleanReg).pipe(
      finalize(() => {
        // Only clear loading if this is the latest search
        if (this.activeSearchId === currentSearchId) {
          // If vehicle was found, loading continues while loading history
          // Otherwise loading stops
          if (this.notFound() || this.errorMessage()) {
            this.loading.set(false);
            this.cdr.markForCheck();
          }
        }
      })
    ).subscribe({
      next: (veh) => {
        if (this.activeSearchId !== currentSearchId) return;
        this.vehicle.set(veh);
        this.loadHistory(veh.vehicleId!, currentSearchId);
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (this.activeSearchId !== currentSearchId) return;
        this.loading.set(false);
        if (err.status === 404) {
          this.notFound.set(true);
        } else {
          this.errorMessage.set('An error occurred while searching for the vehicle.');
          console.error(err);
        }
        this.cdr.markForCheck();
      }
    });
  }

  private loadHistory(vehicleId: number, searchId: number): void {
    this.serviceRecordService.getByVehicle(vehicleId).pipe(
      finalize(() => {
        if (this.activeSearchId === searchId) {
          this.loading.set(false);
          this.cdr.markForCheck();
        }
      })
    ).subscribe({
      next: (records) => {
        if (this.activeSearchId !== searchId) return;
        this.serviceRecords.set(records);
        this.cdr.markForCheck();
      },
      error: (err) => {
        if (this.activeSearchId !== searchId) return;
        this.errorMessage.set('Failed to load vehicle service records.');
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  deleteRecord(record: ServiceRecord): void {
    if (!record.serviceId) return;
    const confirmed = window.confirm(
      `Are you sure you want to delete the service record from ${record.serviceDate} (${record.odometerReading} km)?\n\nThis action cannot be undone.`
    );
    if (!confirmed) return;

    this.loading.set(true);
    this.cdr.markForCheck();

    this.serviceRecordService.delete(record.serviceId).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.successMessage.set('Service record deleted successfully.');
        const veh = this.vehicle();
        if (veh?.vehicleId) {
          this.loadHistory(veh.vehicleId, this.activeSearchId);
        }
        setTimeout(() => {
          this.successMessage.set(null);
          this.cdr.markForCheck();
        }, 4000);
      },
      error: (err) => {
        const msg = err.error?.error || 'Failed to delete service record.';
        this.errorMessage.set(msg);
        this.cdr.markForCheck();
        setTimeout(() => {
          this.errorMessage.set(null);
          this.cdr.markForCheck();
        }, 5000);
      }
    });
  }
}
