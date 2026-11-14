import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { path: 'dashboard', loadComponent: () => import('./dashboard/dashboard').then(m => m.Dashboard) },
  { path: 'vehicles', loadComponent: () => import('./vehicles/vehicle-list/vehicle-list').then(m => m.VehicleList) },
  { path: 'vehicles/add', loadComponent: () => import('./vehicles/vehicle-form/vehicle-form').then(m => m.VehicleForm) },
  { path: 'vehicles/:id', loadComponent: () => import('./vehicles/vehicle-detail/vehicle-detail').then(m => m.VehicleDetail) },
  { path: 'vehicles/:id/edit', loadComponent: () => import('./vehicles/vehicle-form/vehicle-form').then(m => m.VehicleForm) },
  { path: 'service-history', loadComponent: () => import('./service-history/service-search/service-search').then(m => m.ServiceSearch) },
  { path: 'service-history/add/:vehicleId', loadComponent: () => import('./service-history/service-record-form/service-record-form').then(m => m.ServiceRecordForm) },
  { path: 'service-history/:id', loadComponent: () => import('./service-history/service-record-detail/service-record-detail').then(m => m.ServiceRecordDetail) },
  { path: 'service-history/:id/edit', loadComponent: () => import('./service-history/service-record-form/service-record-form').then(m => m.ServiceRecordForm) },
  // Backward compatibility redirect for old customer routes
  { path: 'customers', redirectTo: 'vehicles', pathMatch: 'full' },
  { path: 'customers/**', redirectTo: 'vehicles' },
  { path: '**', redirectTo: 'dashboard' }
];
