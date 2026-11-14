import { Vehicle } from './vehicle.model';

export interface ServiceRecord {
  serviceId?: number;
  vehicle?: Vehicle;
  serviceDate: string;
  odometerReading: number;
  serviceType: string;
  customerComplaint?: string;
  workPerformed: string;
  partsReplaced?: string;
  remarks?: string;
  createdAt?: string;
  updatedAt?: string;
}
