import { Rol } from './models/api.models';

// Misma matriz de permisos que el backend (@PreAuthorize). Se usa en el menú, las rutas y los botones.
const PERSONAL: Rol[] = ['TECNICO', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR'];

export const PERMISOS = {
  equipoEscribir: ['COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  equipoEliminar: ['ADMINISTRADOR'] as Rol[],
  equipoHistorial: PERSONAL,
  categoriasVer: ['COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  reservaCrear: ['ESTUDIANTE', 'DOCENTE', 'COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  reservaNoShow: PERSONAL,
  // Filtro de reservas por usuario: lo valida el backend (403 con otro rol) y necesita /usuarios/opciones
  reservaFiltrarPorUsuario: ['TECNICO', 'COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  prestamoVer: PERSONAL,
  prestamoOperar: ['TECNICO', 'COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  mantenimientoVer: PERSONAL,
  mantenimientoOperar: ['TECNICO', 'COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  incidenciaVer: PERSONAL,
  incidenciaGestionar: ['TECNICO', 'COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  reportesGestion: ['DOCENTE', 'COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  reportesOperativos: PERSONAL,
  recomendaciones: ['TECNICO', 'COORDINADOR', 'ADMINISTRADOR'] as Rol[],
  admin: ['ADMINISTRADOR'] as Rol[],
};
