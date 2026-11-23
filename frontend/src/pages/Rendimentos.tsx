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
import { rendimentoSchema } from '@/types'
import type { Rendimento, RendimentoSchema } from '@/types'
import { declaracaoService, mapearRendimento } from '@/services/declaracaoService'
import { somar, useDeclaracaoStore } from '@/store/useDeclaracaoStore'
import { useAuthStore } from '@/store/useAuthStore'
import { formatBRL, maskCNPJ } from '@/utils/masks'

export default function Rendimentos() {
  const { rendimentos: rendimentosRascunho, declaracaoConsultada, carregado, enviando, enviada, setRendimentos, showToast } = useDeclaracaoStore()
  const [aberto, setAberto] = useState(false)
  const [salvando, setSalvando] = useState(false)

  const { register, handleSubmit, reset, formState: { errors } } = useForm<RendimentoSchema>({
    resolver: zodResolver(rendimentoSchema),
  })

  // Usa a mesma declaração selecionada no Resumo para mostrar rendimentos gravados.
  const rendimentos = declaracaoConsultada ? declaracaoConsultada.rendimentos.map(mapearRendimento) : rendimentosRascunho
  const fechar = () => { setAberto(false); reset() }

  const onSubmit = async (data: RendimentoSchema) => {
    const usuario = useAuthStore.getState().usuario
    if (!usuario || declaracaoConsultada || enviando || enviada) return
    try {
      setSalvando(true)
      // A fonte pagadora existe no banco. Os valores ficam no rascunho até o envio.
      let fonte = await declaracaoService.consultarFontePagadora(data.CNPJ)
      if (useAuthStore.getState().usuario !== usuario) return
      if (!fonte) {
        if (!data.nomeEmpresa.trim()) throw new Error('Informe o nome para cadastrar esta fonte pagadora.')
        if (!window.confirm(`Cadastrar a fonte pagadora ${data.nomeEmpresa.trim()}?`)) return
        fonte = await declaracaoService.cadastrarFontePagadora(data.CNPJ, data.nomeEmpresa)
      }

      const estado = useDeclaracaoStore.getState()
      if (useAuthStore.getState().usuario !== usuario || estado.declaracaoConsultada || estado.enviando || estado.enviada) return
      setRendimentos([...estado.rendimentos, {
        ...data,
        chaveRascunho: crypto.randomUUID(),
        CNPJ: fonte.cnpj,
        nomeEmpresa: fonte.nome,
      }])
      showToast('Rendimento incluído no rascunho.')
      fechar()
    } catch (error) {
      if (useAuthStore.getState().usuario === usuario) {
        showToast(error instanceof Error ? error.message : 'Erro ao consultar a fonte pagadora.', 'error')
      }
    } finally {
      setSalvando(false)
    }
  }

  const remover = (chave?: string) => {
    if (!chave || declaracaoConsultada || enviando || enviada || !window.confirm('Remover este rendimento do rascunho?')) return
    setRendimentos(rendimentosRascunho.filter((r) => r.chaveRascunho !== chave))
  }

  const columns: Column<Rendimento>[] = [
    { header: 'CNPJ Empresa', render: (r) => r.CNPJ },
    { header: 'Nome Empresa', render: (r) => r.nomeEmpresa },
    { header: 'Valor Total Recebido', align: 'right', render: (r) => formatBRL(r.Valor_Total_Recebido) },
    { header: 'INSS', align: 'right', render: (r) => formatBRL(r.INSS) },
    { header: 'IRRF Pago', align: 'right', render: (r) => formatBRL(r.IRRF_pago) },
    { header: '13º Salário', align: 'right', render: (r) => formatBRL(r.valor13Salario) },
  ]

  if (!declaracaoConsultada) {
    columns.push({
      header: 'Ações', align: 'right',
      render: (r) => !enviada && !enviando && (
        <button type="button" aria-label="Remover" onClick={() => remover(r.chaveRascunho)} className="rounded p-1.5 text-red-600 hover:bg-red-50">
          <Trash2 className="h-4 w-4" />
        </button>
      ),
    })
  }

  return (
    <div>
      <PageHeader
        title="Rendimentos Tributáveis Recebidos de Pessoa Jurídica"
        subtitle={declaracaoConsultada ? 'Rendimentos gravados na declaração selecionada.' : enviada ? 'Rendimentos incluídos na declaração enviada.' : 'Os rendimentos do rascunho são gravados ao enviar a declaração.'}
        action={!declaracaoConsultada && <Button disabled={!carregado || enviando || enviada} icon={<Plus className="h-4 w-4" />} onClick={() => setAberto(true)}>Adicionar Rendimento</Button>}
      />

      <DataGrid
        columns={columns}
        rows={rendimentos}
        rowKey={(r) => r.chaveRascunho ?? r.idRendimentos ?? r.CNPJ}
        loading={!carregado}
        footer={`Total recebido: ${formatBRL(somar(rendimentos, (r) => r.Valor_Total_Recebido))}`}
      />

      <Modal open={aberto && !declaracaoConsultada} title="Adicionar Rendimento" onClose={fechar}>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input label="CNPJ da Empresa" mask={maskCNPJ} placeholder="00.000.000/0000-00" {...register('CNPJ')} error={errors.CNPJ?.message} />
          <Input label="Nome da Empresa (para novo CNPJ)" {...register('nomeEmpresa')} error={errors.nomeEmpresa?.message} />
          <Input className="sm:col-span-2" label="Valor Total Recebido (R$)" type="number" step="0.01" {...register('Valor_Total_Recebido')} error={errors.Valor_Total_Recebido?.message} />
          <Input label="INSS (R$)" type="number" step="0.01" {...register('INSS')} error={errors.INSS?.message} />
          <Input label="IRRF Pago (R$)" type="number" step="0.01" {...register('IRRF_pago')} error={errors.IRRF_pago?.message} />
          <Input className="sm:col-span-2" label="Valor 13º Salário (R$)" type="number" step="0.01" {...register('valor13Salario')} error={errors.valor13Salario?.message} />
          <div className="mt-2 flex justify-end gap-2 sm:col-span-2">
            <Button variant="ghost" disabled={salvando} onClick={fechar}>Cancelar</Button>
            <Button type="submit" disabled={salvando || enviando || enviada}>{salvando ? 'Consultando...' : 'Incluir no rascunho'}</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
