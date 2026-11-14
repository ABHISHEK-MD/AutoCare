import { Component, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { ServiceRecordService } from '../../services/service-record.service';
import { ServiceRecord } from '../../models/service-record.model';

@Component({
  selector: 'app-service-record-detail',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './service-record-detail.html',
  styleUrl: './service-record-detail.css'
})
export class ServiceRecordDetail implements OnInit {
  record = signal<ServiceRecord | null>(null);
  serviceId = signal<number>(0);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor(
    private serviceRecordService: ServiceRecordService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.serviceId.set(+idParam);
      this.loadRecord(+idParam);
    }
  }

  loadRecord(id: number): void {
    this.loading.set(true);
    this.error.set(null);
    this.cdr.markForCheck();

    this.serviceRecordService.getById(id).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (data) => {
        this.record.set(data);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error.set('Service record not found.');
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  deleteRecord(): void {
    const rec = this.record();
    if (!rec || !rec.serviceId) return;
    const confirmed = window.confirm(
      `Are you sure you want to delete this service record from ${rec.serviceDate}?\n\nThis action cannot be undone.`
    );
    if (!confirmed) return;

    this.loading.set(true);
    this.cdr.markForCheck();

    this.serviceRecordService.delete(rec.serviceId).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: () => {
        if (rec.vehicle?.registrationNumber) {
          this.router.navigate(['/service-history'], { queryParams: { reg: rec.vehicle.registrationNumber } });
        } else {
          this.router.navigate(['/service-history']);
        }
      },
      error: (err) => {
        const msg = err.error?.error || 'Failed to delete service record.';
        this.error.set(msg);
        this.cdr.markForCheck();
      }
    });
  }

  goBack(): void {
    const rec = this.record();
    if (rec?.vehicle?.registrationNumber) {
      this.router.navigate(['/service-history'], { queryParams: { reg: rec.vehicle.registrationNumber } });
    } else {
      this.router.navigate(['/service-history']);
    }
  }
}
