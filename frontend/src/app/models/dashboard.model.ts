import { ServiceRecord } from './service-record.model';

export interface DashboardStats {
  totalCustomers: number;
  totalVehicles: number;
  totalServiceRecords: number;
  serviceRecordsThisMonth: number;
  recentServices: ServiceRecord[];
}
