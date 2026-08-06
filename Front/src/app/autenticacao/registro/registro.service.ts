import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RegistroRequest, RegistroResponse } from './registro.model';

@Injectable({
  providedIn: 'root'
})
export class RegistroService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/auth';

  register(data: RegistroRequest): Observable<RegistroResponse> {
    const payload = {
      nome: data.fullName,
      email: data.emailOrRegistration,
      senha: data.password,
      role: 'ESTUDANTE'
    };

    return this.http.post<RegistroResponse>(`${this.apiUrl}/cadastro`, payload);
  }
}