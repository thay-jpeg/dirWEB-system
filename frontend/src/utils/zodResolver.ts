import { toNestErrors } from '@hookform/resolvers'
import type { FieldError, FieldValues, Resolver } from 'react-hook-form'
import type { ZodType } from 'zod'

// A versão instalada do resolver espera erros do Zod 3. O Zod 4 devolve issues.
export function zodResolver<T extends FieldValues>(schema: ZodType<T>): Resolver<T> {
  return async (valores, _contexto, opcoes) => {
    const resultado = await schema.safeParseAsync(valores)
    if (resultado.success) return { values: resultado.data, errors: {} }

    const erros: Record<string, FieldError> = {}
    for (const problema of resultado.error.issues) {
      const campo = problema.path.map(String).join('.') || 'root'
      erros[campo] ??= { type: problema.code, message: problema.message }
    }
    return { values: {}, errors: toNestErrors(erros, opcoes) }
  }
}
