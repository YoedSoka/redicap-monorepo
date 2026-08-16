import type { EstadoActa } from '../lib/api'

const ETIQUETAS: Record<EstadoActa, string> = {
  RECIBIDA: 'Recibida',
  EN_CAPTURA_1: 'En captura 1',
  EN_CAPTURA_2: 'En captura 2',
  EN_CAPTURA_3: 'En captura 3',
  MESA_DELIBERACION: 'Mesa de deliberación',
  VALIDADA: 'Validada',
  VALIDADA_VERIFICADOR: 'Validada por verificador',
  ILEGIBLE: 'Ilegible',
  PUBLICADA: 'Publicada',
}

export default function EstadoBadge({ estado }: { estado: EstadoActa }) {
  return (
    <span className="inline-block rounded-full bg-impepac-magenta-50 px-3 py-1 text-xs font-medium text-impepac-magenta-700">
      {ETIQUETAS[estado] ?? estado}
    </span>
  )
}
