import { Customer } from './customer.model';

export interface Vehicle {
  vehicleId?: number;
  customer?: Customer;
  registrationNumber: string;
  brand: string;
  model: string;
  variant?: string;
  manufacturingYear?: number;
  fuelType: string;
  createdAt?: string;
  updatedAt?: string;
}
