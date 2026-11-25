import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@/utils/zodResolver'
import GovInput from './GovInput'
import GovButton from './GovButton'
import { cadastroSchema } from '@/types/authSchemas'
import type { CadastroForm } from '@/types/authSchemas'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/store/useAuthStore'
import { homePorPerfil } from '@/utils/perfil'
import { maskCPF } from '@/utils/masks'

interface Props {
  cpf: string
  onCancel: () => void
}

export default function StepCadastro({ cpf, onCancel }: Props) {
  const navigate = useNavigate()
  const [erro, setErro] = useState('')
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<CadastroForm>({
    resolver: zodResolver(cadastroSchema),
    defaultValues: { cpf },
  })

  const onSubmit = async (d: CadastroForm) => {
    try {
      setErro('')
      const usuario = await authService.cadastrarContribuinte({
        nome: d.nome,
        cpf: d.cpf,
      })
      useAuthStore.getState().login(usuario)
      navigate(homePorPerfil(usuario.perfilAcesso), { replace: true })
    } catch (e) {
      setErro(e instanceof Error ? e.message : 'Erro ao cadastrar')
    }
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <h1 className="text-xl font-bold text-black">Cadastro de novo contribuinte</h1>
      <p className="text-[0.95rem] text-neutral-800">
        Preencha os dados para iniciar sua declaração.
      </p>
      <GovInput
        label="Nome Completo"
        placeholder="Digite seu nome completo"
        autoFocus
        {...register('nome')}
        error={errors.nome?.message}
      />
      <GovInput
        label="CPF"
        mask={maskCPF}
        placeholder="Digite seu CPF"
        inputMode="numeric"
        {...register('cpf')}
        error={errors.cpf?.message}
      />
      {erro && <p className="text-sm text-red-600">{erro}</p>}
      <div className="flex justify-center gap-4">
        <GovButton type="button" variant="outline" disabled={isSubmitting} onClick={onCancel}>
          Cancelar
        </GovButton>
        <GovButton type="submit" disabled={isSubmitting}>
          {isSubmitting ? 'Cadastrando...' : 'Cadastrar'}
        </GovButton>
      </div>
    </form>
  )
}