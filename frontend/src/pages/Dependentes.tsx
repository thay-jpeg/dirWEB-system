import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@/utils/zodResolver'
import { Plus, Trash2 } from 'lucide-react'
import Button from '@/components/ui/Button'
import DataGrid from '@/components/ui/DataGrid'
import type { Column } from '@/components/ui/DataGrid'
import Input from '@/components/ui/Input'
import Modal from '@/components/ui/Modal'
import PageHeader from '@/components/ui/PageHeader'
import Select from '@/components/ui/Select'
import { dependenteSchema } from '@/types'
import type { Dependente, DependenteSchema, TipoDependente } from '@/types'
import { declaracaoService } from '@/services/declaracaoService'
import { useDeclaracaoStore } from '@/store/useDeclaracaoStore'
import { useAuthStore } from '@/store/useAuthStore'
import { maskCPF } from '@/utils/masks'

export default function Dependentes() {
  const { dependentes, semDependentes, carregado, erroDependentes, setDependentes, setSemDependentes, showToast } =
    useDeclaracaoStore()
  const usuario = useAuthStore((s) => s.usuario)
  const [tiposDependente, setTiposDependente] = useState<TipoDependente[]>([])
  const [carregandoTipos, setCarregandoTipos] = useState(false)
  const [aberto, setAberto] = useState(false)
  const [salvando, setSalvando] = useState(false)
  const [excluindoId, setExcluindoId] = useState<number | null>(null)

  const { register, handleSubmit, reset, formState: { errors } } = useForm<DependenteSchema>({
    resolver: zodResolver(dependenteSchema),
  })

  const fechar = () => { setAberto(false); reset({ Nome: '', CPF: '' }) }

  const abrirFormulario = async () => {
    if (!usuario) return
    setCarregandoTipos(true)
    try {
      const tipos = await declaracaoService.listarTiposDependencia()
      if (useAuthStore.getState().usuario !== usuario) return
      if (tipos.length === 0) {
        showToast('Nenhum tipo de dependência cadastrado no banco.', 'error')
        return
      }
      setTiposDependente(tipos)
      setAberto(true)
    } catch (error) {
      if (useAuthStore.getState().usuario === usuario) {
        showToast(error instanceof Error ? error.message : 'Erro ao carregar os tipos.', 'error')
      }
    } finally {
      setCarregandoTipos(false)
    }
  }

  const onSubmit = async (data: DependenteSchema) => {
    if (!usuario) return
    try {
      setSalvando(true)
      const tipo = tiposDependente.find((t) => t.idTipo_dependente === data.Tipo_dependente_idTipo_dependente)
      if (!tipo) throw new Error('Selecione um tipo de dependência.')
      const novo = await declaracaoService.cadastrarDependente(usuario.idPessoa, data, tipo.Tipo_dependente)
      if (useAuthStore.getState().usuario !== usuario) return
      setDependentes([...useDeclaracaoStore.getState().dependentes, novo])
      setSemDependentes(false)
      showToast('Dependente adicionado com sucesso!')
      fechar()
    } catch (error) {
      if (useAuthStore.getState().usuario === usuario) {
        showToast(error instanceof Error ? error.message : 'Erro ao salvar o dependente.', 'error')
      }
    } finally {
      setSalvando(false)
    }
  }

  const excluir = async (dep: Dependente) => {
    if (!usuario || !dep.idDependente || excluindoId !== null) return
    if (!window.confirm(`Remover o dependente "${dep.Nome}"?`)) return

    try {
      setExcluindoId(dep.idDependente)
      await declaracaoService.excluirDependente(usuario.idPessoa, dep.idDependente)
      if (useAuthStore.getState().usuario !== usuario) return
      setDependentes(
        useDeclaracaoStore.getState().dependentes.filter((d) => d.idDependente !== dep.idDependente),
      )
      showToast('Dependente excluído com sucesso!')
    } catch (error) {
      if (useAuthStore.getState().usuario === usuario) {
        showToast(error instanceof Error ? error.message : 'Erro ao excluir o dependente.', 'error')
      }
    } finally {
      setExcluindoId(null)
    }
  }

  const columns: Column<Dependente>[] = [
    { header: 'Nome', render: (d) => d.Nome },
    { header: 'CPF', render: (d) => d.CPF },
    { header: 'Tipo de Dependência', render: (d) => d.Tipo_dependente },
    {
      header: 'Ações',
      align: 'right',
      render: (d) =>
        d.idDependente && (
          <button
            type="button"
            aria-label="Excluir dependente"
            disabled={excluindoId === d.idDependente}
            onClick={() => excluir(d)}
            className="rounded p-1.5 text-red-600 hover:bg-red-50 disabled:opacity-40"
          >
            <Trash2 className="h-4 w-4" />
          </button>
        ),
    },
  ]

  return (
    <div>
      <PageHeader
        title="Dependentes"
        subtitle="O contribuinte pode não ter ou ter vários dependentes."
        action={
          <Button icon={<Plus className="h-4 w-4" />} disabled={!carregado || erroDependentes || carregandoTipos} onClick={abrirFormulario}>
            {carregandoTipos ? 'Carregando tipos...' : 'Adicionar Dependente'}
          </Button>
        }
      />

      <DataGrid columns={columns} rows={dependentes} rowKey={(d) => d.idDependente ?? d.CPF} loading={!carregado}
        emptyMessage={erroDependentes ? 'Não foi possível consultar os dependentes.' : undefined} />

      <label className={`mt-4 flex items-center gap-2 text-sm text-brand ${dependentes.length ? 'opacity-50' : ''}`}>
        <input
          type="checkbox"
          checked={semDependentes}
          disabled={!carregado || erroDependentes || dependentes.length > 0}
          onChange={(e) => setSemDependentes(e.target.checked)}
          className="h-4 w-4 accent-[#1a3f86]"
        />
        Não possuo dependentes
      </label>

      <Modal open={aberto} title="Adicionar Dependente" onClose={fechar}>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
          <Input label="Nome" {...register('Nome')} error={errors.Nome?.message} />
          <Input label="CPF" mask={maskCPF} placeholder="000.000.000-00" {...register('CPF')} error={errors.CPF?.message} />
          <Select
            label="Tipo de Dependência"
            options={tiposDependente.map((t) => ({ value: t.idTipo_dependente, label: t.Tipo_dependente }))}
            {...register('Tipo_dependente_idTipo_dependente')}
            error={errors.Tipo_dependente_idTipo_dependente?.message}
          />
          <div className="mt-2 flex justify-end gap-2">
            <Button variant="ghost" disabled={salvando} onClick={fechar}>Cancelar</Button>
            <Button type="submit" disabled={salvando}>{salvando ? 'Salvando...' : 'Salvar'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}