import { Routes } from '@angular/router';
import { RegistroComponent } from './autenticacao/registro/registro.component';
import { LogoutComponent } from './autenticacao/logout/logout.component';
import { LoginComponent } from './autenticacao/login/login.component';
import { authGuard } from './autenticacao/auth.guard';

export const routes: Routes = [
    { path: '', redirectTo: 'login', pathMatch: 'full' },
    { path: 'login', component: LoginComponent },
    { path: 'registro', component: RegistroComponent },
    { path: 'logout', component: LogoutComponent, canActivate: [authGuard] },
    { path: '**', redirectTo: 'login' }
];