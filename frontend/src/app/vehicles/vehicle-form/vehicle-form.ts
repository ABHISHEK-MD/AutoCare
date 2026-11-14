import { Component, OnInit, OnDestroy, ChangeDetectorRef, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, FormsModule } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { VehicleService } from '../../services/vehicle.service';
import { CustomerService } from '../../services/customer.service';
import { Customer } from '../../models/customer.model';

@Component({
  selector: 'app-vehicle-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './vehicle-form.html',
  styleUrl: './vehicle-form.css'
})
export class VehicleForm implements OnInit, OnDestroy {
  vehicleForm!: FormGroup;
  isEditMode = signal(false);
  vehicleId = signal<number | null>(null);

  // Existing customer association state
  selectedCustomerId = signal<number | null>(null);
  selectedCustomer = signal<Customer | null>(null);
  allCustomers = signal<Customer[]>([]);
  suggestedCustomer = signal<Customer | null>(null);
  showCustomerSearch = signal(false);
  customerSearchInput = signal('');
  filteredCustomers = signal<Customer[]>([]);

  loading = signal(false);
  submitting = signal(false);
  errorMessage = signal<string | null>(null);

  fuelTypes = ['Petrol', 'Diesel', 'Electric', 'Hybrid', 'CNG', 'LPG'];
  currentYear = new Date().getFullYear();

  private queryParamsSub?: Subscription;

  constructor(
    private fb: FormBuilder,
    private vehicleService: VehicleService,
    private customerService: CustomerService,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.initForm();
    this.loadAllCustomers();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.isEditMode.set(true);
      this.vehicleId.set(+idParam);
      this.loadVehicle(+idParam);
    } else {
      this.queryParamsSub = this.route.queryParamMap.subscribe(params => {
        const regParam = params.get('reg');
        if (regParam) {
          const cleanReg = regParam.trim().toUpperCase();
          this.vehicleForm.patchValue({
            registrationNumber: cleanReg
          });
        } else if (!this.isEditMode()) {
          this.vehicleForm.patchValue({
            registrationNumber: ''
          });
        }

        const queryCustomerId = params.get('customerId');
        if (queryCustomerId) {
          this.selectExistingCustomerById(+queryCustomerId);
        }
        this.cdr.markForCheck();
      });
    }
  }

  ngOnDestroy(): void {
    if (this.queryParamsSub) {
      this.queryParamsSub.unsubscribe();
    }
    this.vehicleForm?.reset();
  }

  onCancel(): void {
    this.vehicleForm.reset();
    if (this.isEditMode() && this.vehicleId()) {
      this.router.navigate(['/vehicles', this.vehicleId()]);
    } else {
      this.router.navigate(['/vehicles']);
    }
  }

  private initForm(): void {
    this.vehicleForm = this.fb.group({
      // Customer Information
      customerFullName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(150)]],
      customerPhone: ['', [Validators.required, Validators.pattern('^[0-9]{10,15}$')]],
      customerEmail: ['', [Validators.email, Validators.maxLength(150)]],
      customerAddress: ['', [Validators.maxLength(500)]],

      // Vehicle Information
      registrationNumber: ['', [Validators.required, Validators.minLength(4), Validators.maxLength(20)]],
      brand: ['', [Validators.required, Validators.maxLength(50)]],
      model: ['', [Validators.required, Validators.maxLength(50)]],
      variant: ['', [Validators.maxLength(50)]],
      manufacturingYear: [null, [Validators.min(1900), Validators.max(this.currentYear + 1)]],
      fuelType: ['', [Validators.required]]
    });
  }

  private loadAllCustomers(): void {
    this.customerService.getAll().pipe(
      finalize(() => this.cdr.markForCheck())
    ).subscribe({
      next: (list) => {
        this.allCustomers.set(list);
        this.cdr.markForCheck();
      },
      error: (err) => console.error('Failed to load existing customers:', err)
    });
  }

  private selectExistingCustomerById(customerId: number): void {
    this.customerService.getById(customerId).pipe(
      finalize(() => this.cdr.markForCheck())
    ).subscribe({
      next: (cust) => {
        this.selectCustomer(cust);
      },
      error: (err) => console.error(err)
    });
  }

  private loadVehicle(id: number): void {
    this.loading.set(true);
    this.cdr.markForCheck();

    this.vehicleService.getById(id).pipe(
      finalize(() => {
        this.loading.set(false);
        this.cdr.markForCheck();
      })
    ).subscribe({
      next: (veh) => {
        if (veh.customer) {
          this.selectedCustomerId.set(veh.customer.customerId || null);
          this.selectedCustomer.set(veh.customer);
        }

        this.vehicleForm.patchValue({
          customerFullName: veh.customer?.fullName || '',
          customerPhone: veh.customer?.phone || '',
          customerEmail: veh.customer?.email || '',
          customerAddress: veh.customer?.address || '',

          registrationNumber: veh.registrationNumber,
          brand: veh.brand,
          model: veh.model,
          variant: veh.variant || '',
          manufacturingYear: veh.manufacturingYear || null,
          fuelType: veh.fuelType
        });
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.errorMessage.set('Vehicle not found or failed to load.');
        console.error(err);
        this.cdr.markForCheck();
      }
    });
  }

  onPhoneBlur(): void {
    const enteredPhone = this.vehicleForm.get('customerPhone')?.value?.trim();
    if (!enteredPhone || this.selectedCustomerId()) return;

    // Check if an existing customer has this phone number
    const match = this.allCustomers().find(c => c.phone === enteredPhone);
    if (match) {
      this.suggestedCustomer.set(match);
      this.cdr.markForCheck();
    }
  }

  useSuggestedCustomer(): void {
    const cust = this.suggestedCustomer();
    if (cust) {
      this.selectCustomer(cust);
      this.suggestedCustomer.set(null);
    }
  }

  dismissSuggestedCustomer(): void {
    this.suggestedCustomer.set(null);
  }

  toggleCustomerSearchModal(): void {
    this.showCustomerSearch.update(v => !v);
    this.filteredCustomers.set(this.allCustomers());
    this.cdr.markForCheck();
  }

  onSearchCustomers(query: string): void {
    const q = query.trim().toLowerCase();
    if (!q) {
      this.filteredCustomers.set(this.allCustomers());
    } else {
      const filtered = this.allCustomers().filter(c =>
        c.fullName.toLowerCase().includes(q) || c.phone.includes(q)
      );
      this.filteredCustomers.set(filtered);
    }
    this.cdr.markForCheck();
  }

  selectCustomer(cust: Customer): void {
    this.selectedCustomerId.set(cust.customerId || null);
    this.selectedCustomer.set(cust);
    this.suggestedCustomer.set(null);
    this.showCustomerSearch.set(false);

    this.vehicleForm.patchValue({
      customerFullName: cust.fullName,
      customerPhone: cust.phone,
      customerEmail: cust.email || '',
      customerAddress: cust.address || ''
    });
    this.cdr.markForCheck();
  }

  unlinkCustomer(): void {
    this.selectedCustomerId.set(null);
    this.selectedCustomer.set(null);
    this.cdr.markForCheck();
  }

  onSubmit(): void {
    if (this.vehicleForm.invalid || this.submitting()) {
      this.vehicleForm.markAllAsTouched();
      this.cdr.markForCheck();
      return;
    }

    this.submitting.set(true);
    this.errorMessage.set(null);
    this.cdr.markForCheck();

    const formVal = this.vehicleForm.value;

    // Build customer object:
    // If selectedCustomerId is set, reference existing customer.
    // If not set, create a new customer in the same transaction.
    const customerPayload: any = {
      fullName: formVal.customerFullName.trim(),
      phone: formVal.customerPhone.trim(),
      email: formVal.customerEmail ? formVal.customerEmail.trim() : null,
      address: formVal.customerAddress ? formVal.customerAddress.trim() : null
    };

    if (this.selectedCustomerId()) {
      customerPayload.customerId = this.selectedCustomerId();
    }

    const payload: any = {
      registrationNumber: formVal.registrationNumber.trim().toUpperCase(),
      brand: formVal.brand.trim(),
      model: formVal.model.trim(),
      variant: formVal.variant ? formVal.variant.trim() : null,
      manufacturingYear: formVal.manufacturingYear ? +formVal.manufacturingYear : null,
      fuelType: formVal.fuelType,
      customer: customerPayload
    };

    if (this.isEditMode() && this.vehicleId()) {
      this.vehicleService.update(this.vehicleId()!, payload).pipe(
        finalize(() => {
          this.submitting.set(false);
          this.cdr.markForCheck();
        })
      ).subscribe({
        next: () => {
          this.router.navigate(['/vehicles', this.vehicleId()]);
        },
        error: (err) => {
          const msg = err.error?.error || 'Failed to update vehicle. Please verify input.';
          this.errorMessage.set(msg);
          this.cdr.markForCheck();
        }
      });
    } else {
      this.vehicleService.create(payload).pipe(
        finalize(() => {
          this.submitting.set(false);
          this.cdr.markForCheck();
        })
      ).subscribe({
        next: (created) => {
          this.router.navigate(['/vehicles', created.vehicleId]);
        },
        error: (err) => {
          const msg = err.error?.error || 'Failed to register vehicle and customer. Please ensure registration number is unique.';
          this.errorMessage.set(msg);
          this.cdr.markForCheck();
        }
      });
    }
  }
}
