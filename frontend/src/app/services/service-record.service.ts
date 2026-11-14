import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ServiceRecord } from '../models/service-record.model';

@Injectable({ providedIn: 'root' })
export class ServiceRecordService {
  private apiUrl = 'http://localhost:8080/api/service-records';
  private vehicleApiUrl = 'http://localhost:8080/api/vehicles';

  constructor(private http: HttpClient) {}

  getAll(): Observable<ServiceRecord[]> {
    return this.http.get<ServiceRecord[]>(this.apiUrl);
  }

  getById(id: number): Observable<ServiceRecord> {
    return this.http.get<ServiceRecord>(`${this.apiUrl}/${id}`);
  }

  getByVehicle(vehicleId: number): Observable<ServiceRecord[]> {
    return this.http.get<ServiceRecord[]>(`${this.vehicleApiUrl}/${vehicleId}/service-history`);
  }

  create(record: ServiceRecord): Observable<ServiceRecord> {
    return this.http.post<ServiceRecord>(this.apiUrl, record);
  }

  update(id: number, record: ServiceRecord): Observable<ServiceRecord> {
    return this.http.put<ServiceRecord>(`${this.apiUrl}/${id}`, record);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}
