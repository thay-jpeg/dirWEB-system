import { z } from 'zod'
import { validarCPF } from '@/utils/validators'

export const cpfStepSchema = z.object({
  identificador: z
    .string()
    .trim()
    .min(3, 'Informe o CPF ou código de auditor'),
})

export const cadastroSchema = z.object({
  nome: z
    .string()
    .trim()
    .refine(
      (v) => v.split(/\s+/).filter(Boolean).length >= 2,
      'Informe o nome completo (nome e sobrenome)',
    ),
  cpf: z
    .string()
    .refine(validarCPF, 'Informe um CPF válido para se cadastrar'),
})

export type CpfStepForm = z.infer<typeof cpfStepSchema>
export type CadastroForm = z.infer<typeof cadastroSchema>