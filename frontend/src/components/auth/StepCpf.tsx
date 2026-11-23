import { useForm } from 'react-hook-form'
import { zodResolver } from '@/utils/zodResolver'
import { Contact } from 'lucide-react'
import GovInput from './GovInput'
import GovButton from './GovButton'
import { cpfStepSchema } from '@/types/authSchemas'
import type { CpfStepForm } from '@/types/authSchemas'
import { maskCPF } from '@/utils/masks'

interface Props { initialCpf: string; onResult: (cpf: string, destino: 'senha' | 'cadastro') => void }

export default function StepCpf({ initialCpf, onResult }: Props) {
  const { register, handleSubmit, formState: { errors } } = useForm<CpfStepForm>({
    resolver: zodResolver(cpfStepSchema), defaultValues: { cpf: initialCpf },
  })

  // Valida o formato aqui. A conta só é verificada nas rotas de login e cadastro.
  return (
    <form onSubmit={handleSubmit(({ cpf }) => onResult(cpf, 'senha'))} noValidate className="flex flex-col gap-5">
      <h1 className="text-xl font-bold text-black">Identifique-se no DIRweb</h1>
      <div className="flex items-center gap-3 text-lg text-black">
        <Contact className="h-6 w-6 text-brand-action" /> Número do CPF
      </div>
      <GovInput label="CPF" mask={maskCPF} placeholder="Digite seu CPF" inputMode="numeric" autoFocus
        {...register('cpf')} error={errors.cpf?.message} />
      <div className="flex flex-wrap justify-end gap-3">
        <GovButton variant="outline" onClick={handleSubmit(({ cpf }) => onResult(cpf, 'cadastro'))}>Criar conta</GovButton>
        <GovButton type="submit">Continuar</GovButton>
      </div>
    </form>
  )
}
