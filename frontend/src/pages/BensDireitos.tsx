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
import { bemSchema } from '@/types'
import type { Bem, BemSchema } from '@/types'
import { mapearBem } from '@/services/declaracaoService'
import { somar, useDeclaracaoStore } from '@/store/useDeclaracaoStore'
import { formatBRL } from '@/utils/masks'

export default function BensDireitos() {
  const { bens: bensRascunho, declaracaoConsultada, enviando, enviada, carregado, setBens, showToast } = useDeclaracaoStore()
  const [aberto, setAberto] = useState(false)

  const { register, handleSubmit, reset, setError, formState: { errors } } = useForm<BemSchema>({
    resolver: zodResolver(bemSchema),
  })

  // O Resumo seleciona a declaração salva que esta aba exibe em modo de consulta.
  const bens = declaracaoConsultada ? declaracaoConsultada.bensDireitos.map(mapearBem) : bensRascunho
  const exercicio = declaracaoConsultada?.anoExercicio ?? 2026
  const totalAtual = somar(bens, (b) => b.Valor_31_12_2026)
  const fechar = () => { setAberto(false); reset() }

  const onSubmit = (data: BemSchema) => {
    if (declaracaoConsultada || enviando || enviada) return
    // Regra: a soma total dos bens (31/12/2026) deve ser > R$ 100,00
    if (totalAtual + data.Valor_31_12_2026 <= 100) {
      setError('Valor_31_12_2026', { message: 'A soma total dos bens deve ser maior que R$ 100,00' })
      return
    }
    // O bem é parte do rascunho. O banco gera o ID quando a declaração é enviada.
    setBens([...bensRascunho, { ...data, chaveRascunho: crypto.randomUUID() }])
    showToast('Bem incluído no rascunho.')
    fechar()
  }

  const remover = (chave?: string) => {
    if (!chave || declaracaoConsultada || enviando || enviada || !window.confirm('Remover este bem do rascunho?')) return
    setBens(bensRascunho.filter((b) => b.chaveRascunho !== chave))
  }

  const columns: Column<Bem>[] = [
    { header: 'Tipo de Bem', render: (b) => b.Tipo_Bem },
    { header: 'Nome do Bem', render: (b) => b.Nome_bem },
    { header: 'Valor ano anterior', align: 'right', render: (b) => formatBRL(b.Valor_ano_anterior) },
    { header: `Valor em 31/12/${exercicio}`, align: 'right', render: (b) => formatBRL(b.Valor_31_12_2026) },
  ]

  if (!declaracaoConsultada) {
    columns.push({
      header: 'Ações', align: 'right',
      render: (b) => !enviada && !enviando && (
        <button type="button" aria-label="Remover" onClick={() => remover(b.chaveRascunho)} className="rounded p-1.5 text-red-600 hover:bg-red-50">
          <Trash2 className="h-4 w-4" />
        </button>
      ),
    })
  }

  return (
    <div>
      <PageHeader
        title="Bens e Direitos"
        subtitle={declaracaoConsultada ? 'Bens gravados na declaração selecionada.' : enviada ? 'Bens incluídos na declaração enviada.' : 'Os bens do rascunho são gravados ao enviar a declaração.'}
        action={!declaracaoConsultada && <Button disabled={!carregado || enviando || enviada} icon={<Plus className="h-4 w-4" />} onClick={() => setAberto(true)}>Adicionar Bem</Button>}
      />

      <DataGrid
        columns={columns}
        rows={bens}
        rowKey={(b) => b.chaveRascunho ?? b.idBens_direitos ?? b.Nome_bem}
        loading={!carregado}
        footer={`Total em 31/12/${exercicio}: ${formatBRL(totalAtual)}`}
      />

      <Modal open={aberto && !declaracaoConsultada} title="Adicionar Bem" onClose={fechar}>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Input label="Tipo de Bem" placeholder="Ex.: Veículo" {...register('Tipo_Bem')} error={errors.Tipo_Bem?.message} />
          <Input label="Nome do Bem" {...register('Nome_bem')} error={errors.Nome_bem?.message} />
          <Input label="Valor ano anterior (R$)" type="number" step="0.01" {...register('Valor_ano_anterior')} error={errors.Valor_ano_anterior?.message} />
          <Input label="Valor em 31/12/2026 (R$)" type="number" step="0.01" {...register('Valor_31_12_2026')} error={errors.Valor_31_12_2026?.message} />
          <div className="mt-2 flex justify-end gap-2 sm:col-span-2">
            <Button variant="ghost" onClick={fechar}>Cancelar</Button>
            <Button type="submit" disabled={enviando || enviada}>Incluir no rascunho</Button>
          </div>
        </form>
      </Modal>
    </div>
  )
}
