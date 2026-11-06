import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class Api {
  private http = inject(HttpClient);
  autorizacion: string | null = this.leer();

  private leer(): string | null {
    try {
      return sessionStorage.getItem('autorizacion');
    } catch {
      return null;
    }
  }

  entrar(usuario: string, clave: string): void {
    const bytes = new TextEncoder().encode(`${usuario}:${clave}`);
    this.autorizacion = 'Basic ' + btoa(String.fromCharCode(...bytes));
    try {
      sessionStorage.setItem('autorizacion', this.autorizacion);
    } catch {}
  }

  salir(): void {
    this.autorizacion = null;
    try {
      sessionStorage.removeItem('autorizacion');
    } catch {}
  }

  private opciones(admin: boolean): { headers?: HttpHeaders } {
    if (admin && this.autorizacion) {
      return { headers: new HttpHeaders({ Authorization: this.autorizacion }) };
    }
    return {};
  }

  get<T>(url: string, admin = false): Promise<T> {
    return firstValueFrom(this.http.get<T>(url, this.opciones(admin)));
  }

  post<T>(url: string, cuerpo: unknown, admin = false): Promise<T> {
    return firstValueFrom(this.http.post<T>(url, cuerpo, this.opciones(admin)));
  }

  put<T>(url: string, cuerpo: unknown, admin = false): Promise<T> {
    return firstValueFrom(this.http.put<T>(url, cuerpo, this.opciones(admin)));
  }

  borrar(url: string, admin = false): Promise<unknown> {
    return firstValueFrom(this.http.delete(url, this.opciones(admin)));
  }

  descargar(url: string): Promise<Blob> {
    return firstValueFrom(this.http.get(url, { ...this.opciones(true), responseType: 'blob' }));
  }
}
