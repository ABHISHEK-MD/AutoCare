import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  sidebarOpen = signal(false);
  pageTitle = signal('Dashboard');

  constructor(private router: Router) {
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      this.updateTitle(event.urlAfterRedirects || event.url);
      this.sidebarOpen.set(false);
    });
  }

  toggleSidebar() {
    this.sidebarOpen.update(open => !open);
  }

  closeSidebar() {
    this.sidebarOpen.set(false);
  }

  private updateTitle(url: string) {
    if (url.includes('/vehicles')) {
      this.pageTitle.set('Vehicles & Customers');
    } else if (url.includes('/service-history')) {
      this.pageTitle.set('Service History');
    } else {
      this.pageTitle.set('Dashboard');
    }
  }
}
