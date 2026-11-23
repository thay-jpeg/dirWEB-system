import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { AlertTriangle, Send } from 'lucide-react'
import Button from '@/components/ui/Button'
import Input from '@/components/ui/Input'
import PageHeader from '@/components/ui/PageHeader'
import { somar, useDeclaracaoStore, validarDeclaracao } from '@/store/useDeclaracaoStore'
import { useEnviarDeclaracao } from '@/hooks'
import { useAuthStore } from '@/store/useAuthStore'
import { declaracaoService, mapearBem, mapearContribuinte, mapearRendimento } from '@/services/declaracaoService'
import { formatBRL } from '@/utils/masks'

function Stat({ label, value, hint }: { label: string; value: string; hint?: string }) {
  return (
    <div className="rounded-xl bg-brand-soft p-5 text-brand">
      <p className="text-sm font-medium opacity-80">{label}</p>
      <p className="mt-1 text-2xl font-bold">{value}</p>
      {hint && <p className="mt-1 text-xs opacity-70">{hint}</p>}
    </div>
  )
}

function ConsultaDeclaracao() {
  const usuario = useAuthStore((s) => s.usuario)
  const idEnviado = useDeclaracaoStore((s) => s.idDeclaracao)
  const resultado = useDeclaracaoStore((s) => s.declaracaoConsultada)
  const selecionarDeclaracao = useDeclaracaoStore((s) => s.selecionarDeclaracao)
  const enviando = useDeclaracaoStore((s) => s.enviando)
  const [id, setId] = useState<string | null>(null)
  const [erro, setErro] = useState('')
  const [consultando, setConsultando] = useState(false)
  const telaAtiva = useRef(false)

  useEffect(() => {
    telaAtiva.current = true
    return () => { telaAtiva.current = false }
  }, [])

  const consultar = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const numero = Number(id ?? resultado?.idDeclaracao ?? idEnviado)
    if (!usuario || !Number.isSafeInteger(numero) || numero <= 0 || consultando || enviando) return
    setConsultando(true)
    setErro('')
    try {
      // A consulta seleciona a mesma declaração para o Resumo, Bens e Rendimentos.
      const declaracao = await declaracaoService.consultarDeclaracao(usuario.idPessoa, numero)
      if (telaAtiva.current && useAuthStore.getState().usuario === usuario) {
        selecionarDeclaracao(declaracao)
        setId(null)
      }
    } catch (error) {
      if (telaAtiva.current) setErro(error instanceof Error ? error.message : 'Erro ao consultar a declaração.')
    } finally {
      if (telaAtiva.current) setConsultando(false)
    }
  }

  return (
    <section className="mt-8 rounded-xl border border-brand/20 p-4 text-sm text-brand">
      <h2 className="mb-3 font-bold">Consultar declaração salva</h2>
      <form onSubmit={consultar} className="flex flex-wrap items-end gap-3">
        <Input label="ID da declaração" type="number" min="1" step="1"
          value={id ?? resultado?.idDeclaracao ?? idEnviado ?? ''} onChange={(event) => setId(event.target.value)} required />
        <Button type="submit" disabled={consultando || enviando}>{consultando ? 'Consultando...' : 'Consultar'}</Button>
      </form>
      {erro && <p role="alert" className="mt-3 text-red-700">{erro}</p>}
      {resultado && (
        <div className="mt-3">
          <p>{resultado.rendimentos.length} rendimento(s) · {resultado.bensDireitos.length} bem(ns)</p>
          {resultado.bensDireitos.length > 0 && (
            <ul className="mt-2 list-disc pl-5">
              {resultado.bensDireitos.map((bem) => (
                <li key={bem.idBem}>{bem.nomeBem} · {formatBRL(bem.valorAtual)}</li>
              ))}
            </ul>
          )}
        </div>
      )}
    </section>
  )
}

export default function Resumo() {
  const s = useDeclaracaoStore()
  const { enviar, enviando } = useEnviarDeclaracao()

  if (!s.carregado) return <p className="text-brand">Carregando...</p>

  const declaracao = s.declaracaoConsultada
  const exercicio = declaracao?.anoExercicio ?? 2026
  const contribuinte = declaracao ? mapearContribuinte(declaracao.contribuinte) : s.contribuinte
  const dependentes = declaracao?.dependentes ?? s.dependentes
  const rendimentos = declaracao ? declaracao.rendimentos.map(mapearRendimento) : s.rendimentos
  const bens = declaracao ? declaracao.bensDireitos.map(mapearBem) : s.bens
  const totalRend = somar(rendimentos, (r) => r.Valor_Total_Recebido)
  const total13 = somar(rendimentos, (r) => r.valor13Salario)
  const totalInss = somar(rendimentos, (r) => r.INSS)
  const totalIrrf = somar(rendimentos, (r) => r.IRRF_pago)
  const bensAnt = somar(bens, (b) => b.Valor_ano_anterior)
  const bensAtual = somar(bens, (b) => b.Valor_31_12_2026)
  const evolucao = bensAtual - bensAnt
  const evolucaoPct = bensAnt > 0 ? (evolucao / bensAnt) * 100 : 0
  const pendencias = declaracao || s.enviada ? [] : validarDeclaracao(s)

  return (
    <div>
      <PageHeader title="Resumo da Declaração" subtitle={declaracao ? 'Valores da declaração salva.' : 'Confira os dados da nova declaração antes de enviar.'} />

      {!declaracao && s.enviada && (
        <p className="mb-4 rounded-lg bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-700">
          Declaração enviada com sucesso. ID {s.idDeclaracao}.
        </p>
      )}
      {pendencias.length > 0 && (
        <div className="mb-4 rounded-lg border border-amber-300 bg-amber-50 px-4 py-3 text-sm text-amber-800">
          <p className="mb-1 flex items-center gap-2 font-bold"><AlertTriangle className="h-4 w-4" /> Pendências</p>
          <ul className="list-disc pl-6">{pendencias.map((p) => <li key={p}>{p}</li>)}</ul>
        </div>
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        <Stat label="Contribuinte" value={contribuinte ? `${contribuinte.Nome} ${contribuinte.Sobrenome}` : '—'} hint={contribuinte ? `CPF ${contribuinte.CPF}` : undefined} />
        <Stat label="Dependentes" value={String(dependentes.length)} hint={!declaracao && s.semDependentes ? 'Declarado: sem dependentes' : undefined} />
        <Stat label="Empresas pagadoras" value={String(rendimentos.length)} />
        <Stat label="Total de Rendimentos" value={formatBRL(totalRend)} hint={`13º salário: ${formatBRL(total13)}`} />
        <Stat label="INSS retido" value={formatBRL(totalInss)} />
        <Stat label="IRRF pago" value={formatBRL(totalIrrf)} />
        <Stat label={`Bens em 31/12/${exercicio - 1}`} value={formatBRL(bensAnt)} hint="Valor do ano anterior" />
        <Stat label={`Bens em 31/12/${exercicio}`} value={formatBRL(bensAtual)} />
        <Stat
          label="Evolução Patrimonial"
          value={`${evolucao >= 0 ? '+' : ''}${formatBRL(evolucao)}`}
          hint={`${evolucaoPct >= 0 ? '+' : ''}${evolucaoPct.toFixed(1)}% em relação ao ano anterior`}
        />
      </div>

      {!declaracao && <Button
        variant="action"
        disabled={enviando || s.enviada}
        icon={<Send className="h-5 w-5" fill="currentColor" />}
        className="mt-8 w-full rounded-xl py-4 text-base"
        onClick={enviar}
      >
        {s.enviada ? 'DECLARAÇÃO ENVIADA' : enviando ? 'ENVIANDO...' : 'ENVIAR DECLARAÇÃO'}
      </Button>}
      <ConsultaDeclaracao />
    </div>
  )
}
