import { useEffect, useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@/utils/zodResolver'
import { UserRound } from 'lucide-react'
import Input from '@/components/ui/Input'
import Select from '@/components/ui/Select'
import Button from '@/components/ui/Button'
import PageHeader from '@/components/ui/PageHeader'
import { SEXOS, UFS, contribuinteSchema } from '@/types'
import type { ContribuinteSchema } from '@/types'
import { declaracaoService } from '@/services/declaracaoService'
import { obterEnderecoExterno } from '@/services/enderecoservice'
import { useDeclaracaoStore } from '@/store/useDeclaracaoStore'
import { useAuthStore } from '@/store/useAuthStore'
import { maskCEP, maskCPF, maskTelefone } from '@/utils/masks'

export default function DadosContribuinte() {
  const contribuinte = useDeclaracaoStore((s) => s.contribuinte)
  const carregado = useDeclaracaoStore((s) => s.carregado)
  const erroContribuinte = useDeclaracaoStore((s) => s.erroContribuinte)
  const setContribuinte = useDeclaracaoStore((s) => s.setContribuinte)
  const showToast = useDeclaracaoStore((s) => s.showToast)
  const [salvando, setSalvando] = useState(false)
  const [buscandoCep, setBuscandoCep] = useState(false)
  const consultaCep = useRef<AbortController | null>(null)

  const { register, handleSubmit, reset, getValues, setValue, setError, clearErrors, formState: { errors } } = useForm<ContribuinteSchema>({
    resolver: zodResolver(contribuinteSchema),
  })

  const usuario = useAuthStore((s) => s.usuario)
  useEffect(() => {
    if (contribuinte) reset(contribuinte)
    else if (usuario) reset({ Nome: usuario.nome, Sobrenome: usuario.sobrenome, CPF: usuario.loginCpf })
  }, [contribuinte, usuario, reset])

  useEffect(() => () => consultaCep.current?.abort(), [])

  const campoCep = register('CEP')

  const buscarCep = async () => {
    if (consultaCep.current || salvando) return
    const cep = getValues('CEP').replace(/\D/g, '')
    if (cep.length !== 8) {
      setError('CEP', { message: 'Informe os 8 dígitos do CEP.' })
      return
    }

    clearErrors('CEP')
    const consulta = new AbortController()
    consultaCep.current = consulta
    setBuscandoCep(true)

    try {
      // O UC de endereço consulta o ViaCEP. Aqui só preenchemos os campos do formulário.
      const endereco = await obterEnderecoExterno(cep, consulta.signal)
      if (consulta.signal.aborted || getValues('CEP').replace(/\D/g, '') !== cep) return
      if (!endereco) {
        setError('CEP', { message: 'O serviço não retornou um endereço para este CEP.' })
        return
      }

      const campos = [
        ['Rua', endereco.Logradouro],
        ['Bairro', endereco.Bairro],
        ['Cidade', endereco.Cidade],
        ['UF', endereco.UF],
      ] as const
      for (const [campo, valor] of campos) {
        setValue(campo, valor, { shouldDirty: true })
        if (valor) clearErrors(campo)
      }
    } catch (error) {
      if (!consulta.signal.aborted) {
        setError('CEP', { message: error instanceof Error ? error.message : 'Não foi possível buscar o CEP.' })
      }
    } finally {
      if (consultaCep.current === consulta) {
        consultaCep.current = null
        setBuscandoCep(false)
      }
    }
  }

  const onSubmit = async (data: ContribuinteSchema) => {
    if (!usuario || erroContribuinte || consultaCep.current) return
    try {
      setSalvando(true)
      // Atualiza a pessoa associada à conta autenticada.
      const salvo = await declaracaoService.atualizarContribuinte(usuario, data)
      if (useAuthStore.getState().usuario !== usuario) return
      setContribuinte(salvo)
      showToast('Dados do contribuinte salvos com sucesso!')
    } catch (error) {
      if (useAuthStore.getState().usuario === usuario) {
        showToast(error instanceof Error ? error.message : 'Erro ao salvar os dados.', 'error')
      }
    } finally {
      setSalvando(false)
    }
  }

  if (!carregado) return <p className="text-brand">Carregando...</p>

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate>
      <PageHeader title="Dados do Contribuinte" subtitle="Confira seus dados pessoais e os contatos cadastrados." />

      <div className="flex gap-5">
        <div className="hidden h-24 w-24 shrink-0 items-center justify-center rounded-xl bg-brand text-white sm:flex">
          <UserRound className="h-14 w-14" fill="currentColor" />
        </div>
        <div className="grid flex-1 grid-cols-1 gap-4 md:grid-cols-3">
          <Input label="Nome" {...register('Nome')} error={errors.Nome?.message} />
          <Input label="Sobrenome" {...register('Sobrenome')} error={errors.Sobrenome?.message} />
          <Input label="Nome Social" {...register('Nome_Social')} error={errors.Nome_Social?.message} />
        </div>
      </div>

      <div className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-3">
        <Input readOnly label="CPF" mask={maskCPF} placeholder="000.000.000-00" {...register('CPF')} error={errors.CPF?.message} />
        <Input label="RG" {...register('RG')} error={errors.RG?.message} />
        <Select label="Sexo" options={SEXOS.map((v) => ({ value: v, label: v }))} {...register('Sexo')} error={errors.Sexo?.message} />
      </div>

      <h2 className="mb-2 mt-7 text-base font-bold text-brand">Endereço</h2>
      <p className="mb-3 text-sm text-neutral-600">Use Enter ou Buscar CEP para preencher o endereço. Para salvar, o endereço precisa de referências já cadastradas no sistema.</p>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-6">
        <div className="flex items-start gap-2 md:col-span-2">
          <Input
            className="min-w-0 flex-1"
            label="CEP"
            mask={maskCEP}
            placeholder="00000-000"
            inputMode="numeric"
            {...campoCep}
            error={errors.CEP?.message}
            onChange={(event) => {
              void campoCep.onChange(event)
              if (consultaCep.current) {
                consultaCep.current.abort()
                consultaCep.current = null
                setBuscandoCep(false)
              }
              clearErrors('CEP')
            }}
            onKeyDown={(event) => {
              // Enter no CEP faz a consulta, sem enviar o formulário.
              if (event.key === 'Enter' && !event.nativeEvent.isComposing) {
                event.preventDefault()
                void buscarCep()
              }
            }}
          />
          <Button className="mt-6 h-10 shrink-0 px-3" onClick={buscarCep} disabled={buscandoCep || salvando}>
            {buscandoCep ? 'Buscando...' : 'Buscar CEP'}
          </Button>
        </div>
        <Input className="md:col-span-3" label="Rua" {...register('Rua')} error={errors.Rua?.message} />
        <Input className="md:col-span-1" label="Número" {...register('Numero')} error={errors.Numero?.message} />
        <Input className="md:col-span-2" label="Complemento" {...register('Complemento')} error={errors.Complemento?.message} />
        <Input className="md:col-span-2" label="Bairro" {...register('Bairro')} error={errors.Bairro?.message} />
        <Input className="md:col-span-1" label="Cidade" {...register('Cidade')} error={errors.Cidade?.message} />
        <Select className="md:col-span-1" label="UF" placeholder="UF" options={UFS.map((v) => ({ value: v, label: v }))} {...register('UF')} error={errors.UF?.message} />
      </div>

      <h2 className="mb-3 mt-7 text-base font-bold text-brand">Contato</h2>
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Input label="E-mail" type="email" {...register('Email')} error={errors.Email?.message} />
        <Input readOnly label="Telefone" mask={maskTelefone} placeholder="(00) 00000-0000" {...register('Telefone')} error={errors.Telefone?.message} />
      </div>

      {/* O telefone exige IDs de DDD e DDI que a API atual ainda não lista. */}
      <p className="mt-2 text-sm text-neutral-600">O telefone cadastrado está disponível para consulta.</p>

      <div className="mt-8 flex justify-end">
        <Button type="submit" disabled={salvando || buscandoCep || erroContribuinte} className="px-8">{salvando ? 'Salvando...' : 'Salvar dados'}</Button>
      </div>
    </form>
  )
}
