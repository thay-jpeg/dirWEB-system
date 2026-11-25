import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@/utils/zodResolver'
import { Contact } from 'lucide-react'
import GovInput from './GovInput'
import GovButton from './GovButton'
import { cpfStepSchema } from '@/types/authSchemas'
import type { CpfStepForm } from '@/types/authSchemas'
import { maskCPF } from '@/utils/masks'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/store/useAuthStore'
import { homePorPerfil } from '@/utils/perfil'

interface Props {
  initialCpf: string
  onResult: (cpf: string) => void
}

const maskIdentificador = (valor: string) =>
  /[a-zA-Z]/.test(valor) ? valor.toUpperCase() : maskCPF(valor)

export default function StepCpf({ initialCpf, onResult }: Props) {
  const navigate = useNavigate()
  const [erro, setErro] = useState('')
  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<CpfStepForm>({
    resolver: zodResolver(cpfStepSchema),
    defaultValues: { identificador: initialCpf },
  })

  const onSubmit = async ({ identificador }: CpfStepForm) => {
    try {
      setErro('')
      const usuario = await authService.identificar(identificador)
      useAuthStore.getState().login(usuario)
      navigate(homePorPerfil(usuario.perfilAcesso), { replace: true })
    } catch (e) {
      setErro(
        e instanceof Error
          ? e.message
          : 'CPF ou código de auditor não encontrado.',
      )
    }
  }

  const handleIrParaCadastro = () => {
    const atual = getValues('identificador').trim()
    onResult(/[a-zA-Z]/.test(atual) ? '' : atual)
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-5">
      <h1 className="text-xl font-bold text-black">Identifique-se no DIRweb</h1>
      <div className="flex items-center gap-3 text-lg text-black">
        <Contact className="h-6 w-6 text-brand-action" /> CPF ou Código de Auditor
      </div>
      <GovInput
        label="CPF ou Código de Auditor"
        mask={maskIdentificador}
        placeholder="Digite seu CPF ou código de auditor"
        autoFocus
        {...register('identificador')}
        error={errors.identificador?.message}
      />
      {erro && <p className="text-sm text-red-600">{erro}</p>}
      <div className="flex flex-wrap justify-end gap-3">
        <GovButton
          type="button"
          variant="outline"
          disabled={isSubmitting}
          onClick={handleIrParaCadastro}
        >
          Cadastrar contribuinte
        </GovButton>
        <GovButton type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Verificando...' : 'Continuar'}
        </GovButton>
      </div>
    </form>
  )
}