export interface LoginRequest {
  usuario: string;
  senha: string;
}

export interface LoginResponse {
  token: string;
  usuario?: {
    id: number;
    nome: string;
    email: string;
    perfil: string;
  };
}