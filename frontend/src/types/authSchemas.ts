import { z } from 'zod'
import { validarCPF } from '@/utils/validators'

export const cpfStepSchema = z.object({
  cpf: z.string().refine((v) => v.replace(/\D/g, '').length === 11, 'Informe os 11 dígitos do CPF'),
})
export const senhaStepSchema = z.object({ senha: z.string().min(1, 'Informe a senha') })

const senhaForte = z.string().min(8, 'A senha deve ter no mínimo 8 caracteres')

export const cadastroSchema = z.object({
  nome: z.string().trim().refine((v) => v.split(/\s+/).filter(Boolean).length >= 2, 'Informe o nome completo (nome e sobrenome)'),
  cpf: z.string().refine(validarCPF, 'CPF inválido. Volte e informe um CPF válido para se cadastrar'),
  senha: senhaForte,
})

export type CpfStepForm = z.infer<typeof cpfStepSchema>
export type SenhaStepForm = z.infer<typeof senhaStepSchema>
export type CadastroForm = z.infer<typeof cadastroSchema>
