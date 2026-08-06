import { Component, inject, signal } from '@angular/core';
import { CommonModule, Location } from '@angular/common';
import { Router } from '@angular/router';
import { LogoutService } from './logout.service';
import { AutenticacaoService } from '../autenticacao.service';

@Component({
  selector: 'app-logout',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './logout.component.html'
})
export class LogoutComponent {
  private readonly logoutService = inject(LogoutService);
  private readonly authService = inject(AutenticacaoService);
  private readonly router = inject(Router);
  private readonly location = inject(Location);

  isLoading = signal<boolean>(false);

  onConfirmLogout(): void {
    this.isLoading.set(true);

    this.logoutService.logout().subscribe({
      next: () => {
        this.clearSessionAndRedirect();
      },
      error: () => {
        // Garante a saída do usuário mesmo em caso de falha na comunicação de rede
        this.clearSessionAndRedirect();
      }
    });
  }

  onCancel(): void {
    this.location.back();
  }

  private clearSessionAndRedirect(): void {
    this.authService.clearToken();
    sessionStorage.clear();
    this.isLoading.set(false);
    this.router.navigate(['/login']);
  }
}