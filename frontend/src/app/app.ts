import { Component, HostListener, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { marked } from 'marked';
import { Api } from './api';

interface ItemLeccion {
  id: number;
  nombre: string;
  objetivo: string;
}

interface Leccion extends ItemLeccion {
  markdown: string;
}

interface Registro {
  id: number;
  leccionNombre: string;
  fechaInicio: string;
  fechaFin: string | null;
  completada: boolean;
}

interface Usuario {
  id: number;
  nombreUsuario: string;
  creadoEn: string;
}

interface Estadisticas {
  desde: string;
  hasta: string;
  visitasTotales: number;
  visitantesUnicos: number;
  porDia: { fecha: string; visitas: number }[];
}

const OBJETIVOS: Record<string, string> = {
  COMPILAR_CON_ERROR: 'Compilar con error',
  COMPILAR_EXITOSO: 'Compilar sin errores',
  EJECUTAR_CON_ERROR: 'Ejecutar con error',
  EJECUTAR_EXITOSO: 'Ejecutar con éxito'
};

@Component({
  selector: 'app-root',
  imports: [FormsModule, DatePipe],
  templateUrl: './workspace.html',
  styleUrl: './workspace.css'
})
export class App {
  private api = inject(Api);
  private sesionId = this.idLocal('sesionId');
  private historialId: number | null = null;
  private temporizador: ReturnType<typeof setTimeout> | undefined;

  lecciones = signal<ItemLeccion[]>([]);
  leccion = signal<Leccion | null>(null);
  completada = signal(false);
  compilado = signal(false);
  codigo = signal('');
  consola = signal('Abre una lección para comenzar tu práctica.');
  ocupado = signal(false);
  menu = signal<string | null>(null);
  dialogo = signal<string | null>(null);
  tema = signal<'light' | 'dark'>('light');
  fuente = signal(15);
  aviso = signal('');

  admin = signal<string | null>(null);
  loginUsuario = signal('');
  loginClave = signal('');

  registros = signal<Registro[]>([]);

  usuarios = signal<Usuario[]>([]);
  editandoId = signal<number | null>(null);
  formUsuario = signal('');
  formClave = signal('');

  estadisticas = signal<Estadisticas | null>(null);
  desde = signal('');
  hasta = signal('');
  maxVisitas = computed(() => Math.max(1, ...(this.estadisticas()?.porDia.map((d) => d.visitas) ?? [1])));

  html = computed(() => {
    const l = this.leccion();
    if (!l) {
      return '';
    }
    const texto = l.markdown.replace(
      /(!\[[^\]]*\]\()(?!https?:|\/|data:)([^)\s]+)/g,
      (_, inicio: string, nombre: string) => `${inicio}/api/lecciones/${l.id}/imagenes/${encodeURIComponent(nombre)}`
    );
    return marked.parse(texto, { async: false }) as string;
  });

  constructor() {
    this.api.post('/api/visitas', { visitanteId: this.idLocal('visitanteId') }).catch(() => {});
    if (this.api.autorizacion) {
      this.api
        .get<{ usuario: string }>('/api/admin/yo', true)
        .then((r) => this.admin.set(r.usuario))
        .catch(() => this.api.salir());
    }
  }

  private idLocal(clave: string): string {
    try {
      let valor = localStorage.getItem(clave);
      if (!valor) {
        valor = crypto.randomUUID();
        localStorage.setItem(clave, valor);
      }
      return valor;
    } catch {
      return crypto.randomUUID();
    }
  }

  @HostListener('window:pagehide')
  alSalir(): void {
    if (this.historialId !== null) {
      const cuerpo = new Blob([JSON.stringify({ sesionId: this.sesionId })], { type: 'application/json' });
      navigator.sendBeacon(`/api/historial/${this.historialId}/cerrar`, cuerpo);
    }
  }

  objetivoTexto(objetivo: string): string {
    return OBJETIVOS[objetivo] ?? objetivo;
  }

  alternarMenu(nombre: string): void {
    this.menu.set(this.menu() === nombre ? null : nombre);
  }

  cambiarFuente(delta: number): void {
    this.fuente.set(Math.max(13, Math.min(21, this.fuente() + delta)));
  }

  avisar(texto: string): void {
    this.aviso.set(texto);
    clearTimeout(this.temporizador);
    this.temporizador = setTimeout(() => this.aviso.set(''), 7000);
  }

  private mensaje(e: any): string {
    if (e?.status === 0) {
      return 'No se pudo conectar con el servidor.';
    }
    return e?.error?.message || 'Ocurrió un error inesperado.';
  }

  private fallo(e: any): void {
    if (e?.status === 401 && this.admin()) {
      this.cerrarSesionAdmin();
    }
    this.avisar(this.mensaje(e));
  }

  cerrarDialogo(): void {
    this.dialogo.set(null);
  }

  async abrirDialogo(nombre: string): Promise<void> {
    this.menu.set(null);
    this.dialogo.set(nombre);
    try {
      if (nombre === 'catalogo' || nombre === 'eliminar') {
        this.lecciones.set(await this.api.get<ItemLeccion[]>('/api/lecciones'));
      } else if (nombre === 'historial') {
        this.registros.set(await this.api.get<Registro[]>(`/api/historial?sesionId=${this.sesionId}`));
      } else if (nombre === 'usuarios') {
        this.cancelarEdicion();
        this.usuarios.set(await this.api.get<Usuario[]>('/api/admin/usuarios', true));
      } else if (nombre === 'estadisticas') {
        this.desde.set('');
        this.hasta.set('');
        await this.consultarEstadisticas();
      }
    } catch (e) {
      this.fallo(e);
    }
  }

  async abrirLeccion(item: ItemLeccion): Promise<void> {
    try {
      const leccion = await this.api.get<Leccion>(`/api/lecciones/${item.id}`);
      const registro = await this.api.post<{ id: number }>('/api/historial', {
        sesionId: this.sesionId,
        leccionId: leccion.id
      });
      this.historialId = registro.id;
      this.leccion.set(leccion);
      this.completada.set(false);
      this.compilado.set(false);
      this.codigo.set('');
      this.consola.set(`Meta de aprendizaje: ${this.objetivoTexto(leccion.objetivo)}`);
      this.dialogo.set(null);
    } catch (e) {
      this.fallo(e);
    }
  }

  async cerrarLeccion(): Promise<void> {
    this.menu.set(null);
    const id = this.historialId;
    this.historialId = null;
    this.leccion.set(null);
    this.codigo.set('');
    this.completada.set(false);
    this.compilado.set(false);
    this.consola.set('Abre una lección para comenzar tu práctica.');
    if (id !== null) {
      try {
        await this.api.post(`/api/historial/${id}/cerrar`, { sesionId: this.sesionId });
      } catch {}
    }
  }

  async compilar(): Promise<void> {
    if (!this.codigo().trim()) {
      this.consola.set('Escribe código antes de compilar.');
      return;
    }
    this.ocupado.set(true);
    this.compilado.set(false);
    this.consola.set('Compilando...');
    try {
      const r = await this.api.post<any>('/api/compilar', this.cuerpoPractica());
      this.compilado.set(!!r.exitoso);
      let texto = r.exitoso ? 'Compilación exitosa.' : 'La compilación falló.';
      if (r.salida) {
        texto += '\n' + r.salida;
      }
      this.consola.set(texto + this.resultadoObjetivo(r.objetivoCumplido));
    } catch (e: any) {
      this.consola.set(this.mensaje(e));
    } finally {
      this.ocupado.set(false);
    }
  }

  async ejecutar(): Promise<void> {
    if (!this.compilado()) {
      this.consola.set('Compila el código sin errores antes de ejecutar.');
      return;
    }
    if (!this.codigo().trim()) {
      this.consola.set('Escribe código antes de ejecutar.');
      return;
    }
    this.ocupado.set(true);
    this.consola.set('Ejecutando...');
    try {
      const r = await this.api.post<any>('/api/ejecutar', this.cuerpoPractica());
      let texto: string;
      if (!r.compilacionExitosa) {
        texto = 'La compilación falló.\n' + r.salidaCompilacion;
      } else {
        texto = r.salida;
        if (r.errores) {
          texto += (texto ? '\n' : '') + r.errores;
        }
        if (r.timeout) {
          texto += '\nLa ejecución se detuvo por exceder el tiempo máximo.';
        }
        if (!texto) {
          texto = r.ejecucionExitosa ? '(El programa terminó sin mostrar nada.)' : 'La ejecución falló.';
        }
      }
      this.consola.set(texto + this.resultadoObjetivo(r.objetivoCumplido));
    } catch (e: any) {
      this.consola.set(this.mensaje(e));
    } finally {
      this.ocupado.set(false);
    }
  }

  private cuerpoPractica() {
    return { codigo: this.codigo(), sesionId: this.sesionId, historialId: this.historialId };
  }

  private resultadoObjetivo(cumplido: boolean): string {
    if (cumplido) {
      this.completada.set(true);
      return '\n\n✔ ¡Objetivo de la lección cumplido!';
    }
    return '';
  }

  async iniciarSesionAdmin(): Promise<void> {
    const usuario = this.loginUsuario().trim();
    if (!usuario || !this.loginClave()) {
      this.avisar('Escribe usuario y contraseña.');
      return;
    }
    this.api.entrar(usuario, this.loginClave());
    try {
      const r = await this.api.get<{ usuario: string }>('/api/admin/yo', true);
      this.admin.set(r.usuario);
      this.loginClave.set('');
      this.dialogo.set(null);
      this.avisar(`Sesión iniciada como ${r.usuario}.`);
    } catch (e) {
      this.api.salir();
      this.avisar(this.mensaje(e));
    }
  }

  cerrarSesionAdmin(): void {
    this.api.salir();
    this.admin.set(null);
    this.menu.set(null);
    this.dialogo.set(null);
  }

  async exportar(): Promise<void> {
    this.menu.set(null);
    try {
      const blob = await this.api.descargar('/api/admin/lecciones/exportar');
      const url = URL.createObjectURL(blob);
      const enlace = document.createElement('a');
      enlace.href = url;
      enlace.download = 'lecciones.zip';
      enlace.click();
      URL.revokeObjectURL(url);
    } catch (e: any) {
      if (e?.status === 404) {
        this.avisar('No hay lecciones para exportar.');
      } else {
        this.avisar('No se pudo exportar. Revisa tu sesión.');
        if (e?.status === 401) {
          this.cerrarSesionAdmin();
        }
      }
    }
  }

  async importar(evento: Event): Promise<void> {
    const entrada = evento.target as HTMLInputElement;
    const archivo = entrada.files?.[0];
    entrada.value = '';
    if (!archivo) {
      return;
    }
    const datos = new FormData();
    datos.append('archivo', archivo);
    try {
      const r = await this.api.post<{ importadas: string[]; errores: string[] }>('/api/admin/lecciones/importar', datos, true);
      let texto = `Lecciones importadas: ${r.importadas.length}.`;
      if (r.errores.length) {
        texto += ' Con errores: ' + r.errores.join(' | ');
      }
      this.avisar(texto);
    } catch (e) {
      this.fallo(e);
    }
  }

  async eliminarLeccion(item: ItemLeccion): Promise<void> {
    if (!confirm(`¿Eliminar la lección "${item.nombre}"? También se borrará su historial.`)) {
      return;
    }
    try {
      await this.api.borrar(`/api/admin/lecciones/${item.id}`, true);
      this.lecciones.set(this.lecciones().filter((l) => l.id !== item.id));
      if (this.leccion()?.id === item.id) {
        this.historialId = null;
        this.leccion.set(null);
        this.compilado.set(false);
        this.codigo.set('');
        this.consola.set('Abre una lección para comenzar tu práctica.');
      }
      this.avisar('Lección eliminada.');
    } catch (e) {
      this.fallo(e);
    }
  }

  editarUsuario(u: Usuario): void {
    this.editandoId.set(u.id);
    this.formUsuario.set(u.nombreUsuario);
    this.formClave.set('');
  }

  cancelarEdicion(): void {
    this.editandoId.set(null);
    this.formUsuario.set('');
    this.formClave.set('');
  }

  async guardarUsuario(): Promise<void> {
    const cuerpo = { usuario: this.formUsuario().trim(), clave: this.formClave() };
    try {
      const id = this.editandoId();
      if (id === null) {
        await this.api.post('/api/admin/usuarios', cuerpo, true);
      } else {
        await this.api.put(`/api/admin/usuarios/${id}`, cuerpo, true);
      }
      this.cancelarEdicion();
      this.usuarios.set(await this.api.get<Usuario[]>('/api/admin/usuarios', true));
      this.avisar('Usuario guardado.');
    } catch (e) {
      this.fallo(e);
    }
  }

  async eliminarUsuario(u: Usuario): Promise<void> {
    if (!confirm(`¿Eliminar al administrador "${u.nombreUsuario}"?`)) {
      return;
    }
    try {
      await this.api.borrar(`/api/admin/usuarios/${u.id}`, true);
      this.usuarios.set(await this.api.get<Usuario[]>('/api/admin/usuarios', true));
      this.avisar('Usuario eliminado.');
    } catch (e) {
      this.fallo(e);
    }
  }

  async consultarEstadisticas(): Promise<void> {
    const parametros = new URLSearchParams();
    if (this.desde()) {
      parametros.set('desde', this.desde());
    }
    if (this.hasta()) {
      parametros.set('hasta', this.hasta());
    }
    try {
      const r = await this.api.get<Estadisticas>(`/api/admin/estadisticas?${parametros}`, true);
      this.estadisticas.set(r);
      this.desde.set(r.desde);
      this.hasta.set(r.hasta);
    } catch (e) {
      this.fallo(e);
    }
  }
}
