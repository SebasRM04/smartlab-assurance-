// Tipos que reflejan los DTO del backend Spring Boot (paquete com.utp.smartlab.dto)

export type Rol = 'ESTUDIANTE' | 'TECNICO' | 'DOCENTE' | 'COORDINADOR' | 'ADMINISTRADOR';
export type EstadoEquipo = 'DISPONIBLE' | 'EN_MANTENIMIENTO';
export type EstadoReserva = 'CONFIRMADA' | 'ATENDIDA' | 'NO_SHOW' | 'CANCELADA';
export type EstadoPrestamo = 'ACTIVO' | 'DEVUELTO';
export type EstadoMantenimiento = 'PROGRAMADO' | 'CERRADO';
export type TipoMantenimiento = 'PREVENTIVO' | 'CORRECTIVO';
export type EstadoIncidencia = 'ABIERTA' | 'EN_REVISION' | 'RESUELTA';
export type ClasificacionUtilizacion = 'SUBUTILIZADO' | 'NORMAL' | 'SOBREUTILIZADO';

export const ROLES: Rol[] = ['ESTUDIANTE', 'TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR'];
export const ESTADOS_EQUIPO: EstadoEquipo[] = ['DISPONIBLE', 'EN_MANTENIMIENTO'];
export const ESTADOS_RESERVA: EstadoReserva[] = ['CONFIRMADA', 'ATENDIDA', 'NO_SHOW', 'CANCELADA'];
export const TIPOS_MANTENIMIENTO: TipoMantenimiento[] = ['PREVENTIVO', 'CORRECTIVO'];

// ---- Autenticación
export interface LoginRequest { email: string; password: string; }
export interface RegistroRequest { nombre: string; email: string; password: string; }
export interface JwtResponse {
  token: string;
  tipo: string;
  usuarioId: number;
  nombre: string;
  email: string;
  rol: Rol;
}
export interface Sesion {
  token: string;
  usuarioId: number;
  nombre: string;
  email: string;
  rol: Rol;
}

// ---- Usuarios y categorías
export interface Usuario { id: number; nombre: string; email: string; rol: Rol; }
/** Dato mínimo de un usuario (id y nombre) para el selector del filtro de reservas. */
export interface UsuarioOpcion { id: number; nombre: string; }
export interface UsuarioRequest { nombre: string; email: string; password: string; rol: Rol; }
export interface UsuarioUpdate { nombre: string; email: string; rol: Rol; password?: string; }
export interface Categoria { id: number; nombre: string; }
export interface CategoriaRequest { nombre: string; }

// ---- Equipos
export interface Equipo {
  id: number;
  nombre: string;
  categoriaId: number;
  categoriaNombre: string;
  ubicacion: string | null;
  estado: EstadoEquipo;
  contadorUsoHoras: number;
}
export interface EquipoRequest { nombre: string; categoriaId: number; ubicacion: string; }
export interface HistorialEstado {
  estadoAnterior: EstadoEquipo | null;
  estadoNuevo: EstadoEquipo;
  usuarioId: number | null;
  usuarioNombre: string | null;
  motivo: string | null;
  fechaCambio: string;
}
export interface UtilizacionEquipo {
  prestamoId: number;
  reservaId: number;
  usuarioNombre: string;
  fechaEntrega: string;
  fechaLimite: string;
  fechaDevolucionReal: string | null;
  horasUso: number | null;
  estado: EstadoPrestamo;
}

// ---- Listados por página (PaginaDTO del backend)
export interface PaginaApi<T> {
  contenido: T[];
  pagina: number; // empieza en 0
  tamanio: number;
  totalElementos: number;
  totalPaginas: number;
  primera: boolean;
  ultima: boolean;
}

// ---- Reservas
export interface Reserva {
  id: number;
  equipoId: number;
  usuarioId: number;
  usuarioNombre: string;
  fechaInicio: string;
  fechaFin: string;
  estado: EstadoReserva;
}
export interface ReservaRequest { equipoId: number; fechaInicio: string; fechaFin: string; }

// ---- Préstamos
export interface Prestamo {
  id: number;
  reservaId: number;
  fechaEntrega: string;
  fechaLimite: string;
  fechaDevolucionReal: string | null;
  estado: EstadoPrestamo;
  moroso: boolean;
  vencido: boolean;
}
export interface PrestamoCheckoutRequest { reservaId: number; fechaLimite: string; }

// ---- Mantenimientos
export interface Mantenimiento {
  id: number;
  equipoId: number;
  usuarioId: number;
  usuarioNombre: string;
  tipo: TipoMantenimiento;
  fechaProgramada: string;
  fechaEjecucion: string | null;
  estado: EstadoMantenimiento;
  tardio: boolean;
}
export interface MantenimientoRequest { equipoId: number; tipo: TipoMantenimiento; fechaProgramada: string; }

// ---- Incidencias
export interface Incidencia {
  id: number;
  equipoId: number;
  usuarioId: number;
  usuarioNombre: string;
  descripcion: string;
  evidenciaUrl: string | null;
  estado: EstadoIncidencia;
  fechaRegistro: string;
}
export interface IncidenciaRequest { equipoId: number; descripcion: string; evidenciaUrl?: string | null; }

// ---- Auditoría
export interface Auditoria {
  id: number;
  usuarioId: number | null;
  usuarioNombre: string | null;
  accion: string;
  entidadAfectada: string;
  fechaHora: string;
  detalle: string | null;
}

// ---- Reportes / indicadores
export interface ReservaMetricas {
  total: number; confirmadas: number; atendidas: number; noShow: number; canceladas: number;
  totalConfirmadas: number; tasaNoShow: number;
}
export interface ConflictoMetricas { totalSolicitudes: number; rechazadasPorConflicto: number; tasaConflicto: number; }
export interface PrestamoMetricas {
  total: number; activos: number; devueltos: number; conMora: number; vencidos: number; tasaMora: number;
}
export interface MantenimientoMetricas { total: number; cerrados: number; tardios: number; tasaTardio: number; }
export interface DisponibilidadMetricas {
  totalEquiposEnMantenimiento: number; equiposCorrectamenteBloqueados: number; exactitudDisponibilidad: number;
}
export interface EquipoRecomendacion {
  equipoId: number; nombre: string; contadorUsoHoras: number; fechaProgramada: string | null; motivo: string;
}
export interface UtilizacionEquipoResumen {
  equipoId: number; nombre: string; horasUso: number; frecuenciaPrestamos: number;
  clasificacion: ClasificacionUtilizacion;
}
export interface UtilizacionDashboard {
  umbralSobreutilizacionHoras: number; totalEquipos: number; subutilizados: number; normales: number;
  sobreutilizados: number; equipos: UtilizacionEquipoResumen[];
}

// ---- Error estándar del backend (ErrorResponse)
export interface ErrorApi {
  timestamp?: string;
  status?: number;
  error?: string;
  message?: string;
  detalles?: string[] | null;
}
