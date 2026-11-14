import { Component, OnInit, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { DashboardService } from '../services/dashboard.service';
import { DashboardStats } from '../models/dashboard.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {
  stats = signal<DashboardStats | null>(null);
  loading = signal<boolean>(true);
  error = signal<string | null>(null);

  constructor(
    private dashboardService: DashboardService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    this.loading.set(true);
    this.error.set(null);
    this.cdr.markForCheck();

    this.dashboardService.getStats().pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (data) => {
        this.stats.set(data);
        this.error.set(null);
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error.set('Failed to load dashboard data. Please verify the backend is running.');
        console.error('Error fetching dashboard stats:', err);
        this.cdr.markForCheck();
      }
    });
  }
}
