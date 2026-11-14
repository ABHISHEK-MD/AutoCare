import { Component, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { finalize } from 'rxjs/operators';
import { VehicleService } from '../../services/vehicle.service';
import { Vehicle } from '../../models/vehicle.model';

@Component({
  selector: 'app-vehicle-list',
  standalone: true,
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './vehicle-list.html',
  styleUrl: './vehicle-list.css'
})
export class VehicleList implements OnInit {
  vehicles = signal<Vehicle[]>([]);
  searchQuery = signal<string>('');
  loading = signal<boolean>(true);
  error = signal<string | null>(null);
  successMessage = signal<string | null>(null);

  constructor(
    private vehicleService: VehicleService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadVehicles();
  }

  loadVehicles(): void {
    this.loading.set(true);
    this.error.set(null);
    this.cdr.markForCheck();

    this.vehicleService.getAll().pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (data) => {
        this.vehicles.set(data);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error.set('Failed to load vehicles list.');
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  onSearch(): void {
    const query = this.searchQuery().trim();
    if (!query) {
      this.loadVehicles();
      return;
    }

    this.loading.set(true);
    this.error.set(null);
    this.cdr.markForCheck();

    this.vehicleService.search(query).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (data) => {
        this.vehicles.set(data);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error.set('Vehicle search failed.');
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  clearSearch(): void {
    this.searchQuery.set('');
    this.loadVehicles();
  }

  getRegisterQueryParams(): { reg?: string } {
    const raw = this.searchQuery().trim();
    if (!raw) return {};
    const compact = raw.replace(/[\s-]/g, '');
    if (/^[A-Za-z0-9]{4,20}$/.test(compact) && /[A-Za-z]/.test(compact) && /\d/.test(compact)) {
      return { reg: compact.toUpperCase() };
    }
    return {};
  }

  deleteVehicle(vehicle: Vehicle): void {
    if (!vehicle.vehicleId) return;
    const confirmed = window.confirm(
      `Are you sure you want to delete vehicle "${vehicle.registrationNumber}" (${vehicle.brand} ${vehicle.model})?\n\nNote: Deletion will be rejected if this vehicle has any service history.`
    );
    if (!confirmed) return;

    this.loading.set(true);
    this.error.set(null);
    this.cdr.markForCheck();

    this.vehicleService.delete(vehicle.vehicleId).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        this.successMessage.set(`Vehicle "${vehicle.registrationNumber}" was successfully deleted.`);
        this.loadVehicles();
        setTimeout(() => {
          this.successMessage.set(null);
          this.cdr.markForCheck();
        }, 4000);
      },
      error: (err) => {
        const msg = err.error?.error || 'Cannot delete vehicle that has recorded service history.';
        this.error.set(msg);
        this.cdr.markForCheck();
        setTimeout(() => {
          this.error.set(null);
          this.cdr.markForCheck();
        }, 6000);
      }
    });
  }
}
