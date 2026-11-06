/** Páginas necesarias para `total` elementos (0 si no hay ninguno). */
export function totalPaginasDe(total: number, tamanio: number): number {
  return tamanio > 0 ? Math.ceil(total / tamanio) : 0;
}

/** Ajusta una página (base 0) al rango válido para `total` elementos. */
export function paginaValida(pagina: number, total: number, tamanio: number): number {
  const ultima = Math.max(totalPaginasDe(total, tamanio) - 1, 0);
  return Math.min(Math.max(pagina, 0), ultima);
}

/** Recorta una lista ya cargada para mostrar solo la página pedida (base 0). */
export function rebanar<T>(lista: readonly T[], pagina: number, tamanio: number): T[] {
  const actual = paginaValida(pagina, lista.length, tamanio);
  return lista.slice(actual * tamanio, (actual + 1) * tamanio);
}
