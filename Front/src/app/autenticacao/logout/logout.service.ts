import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface LogoutResponse {
  message: string;
  success: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class LogoutService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/auth/logout';

  logout(): Observable<void> {
    return this.http.post<void>(this.apiUrl, {});
  }
}