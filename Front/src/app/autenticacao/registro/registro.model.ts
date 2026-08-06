export interface RegistroRequest {
  fullName: string;
  emailOrRegistration: string;
  password: string;
}

export interface RegistroResponse {
  message: string;
  success: boolean;
}