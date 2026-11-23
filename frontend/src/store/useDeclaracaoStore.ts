import { create } from 'zustand'
import type { DeclaracaoApi } from '@/services/declaracaoService'
import type { Bem, ContribuinteForm, Dependente, Rendimento } from '@/types'

export type EtapaId = 'dados' | 'dependentes' | 'rendimentos' | 'bens' | 'resumo'

export const ETAPAS: { id: EtapaId; label: string; peso: number }[] = [
  { id: 'dados', label: 'Dados do Contribuinte', peso: 10 },
  { id: 'dependentes', label: 'Dependentes', peso: 15 },
  { id: 'rendimentos', label: 'Rendimentos', peso: 30 },
  { id: 'bens', label: 'Bens e Direitos', peso: 30 },
  { id: 'resumo', label: 'Resumo da Declaração', peso: 15 },
]

export type ToastType = 'success' | 'error'

interface DeclaracaoState {
  carregado: boolean
  contribuinte: ContribuinteForm | null
  dependentes: Dependente[]
  semDependentes: boolean
  rendimentos: Rendimento[]
  bens: Bem[]
  // Erros de leitura são distintos de uma lista vazia no banco.
  erroContribuinte: boolean
  erroDependentes: boolean
  // A declaração consultada fica disponível ao navegar, separada dos dados em edição.
  declaracaoConsultada: DeclaracaoApi | null
  idDeclaracao: number | null
  enviando: boolean
  enviada: boolean
  toast: { id: number; msg: string; type: ToastType } | null

  hydrate: (d: Partial<DeclaracaoState>) => void
  setContribuinte: (c: ContribuinteForm) => void
  setDependentes: (d: Dependente[]) => void
  setSemDependentes: (v: boolean) => void
  setRendimentos: (r: Rendimento[]) => void
  setBens: (b: Bem[]) => void
  selecionarDeclaracao: (declaracao: DeclaracaoApi | null) => void
  registrarEnvio: (id: number) => void
  setEnviando: (v: boolean) => void
  showToast: (msg: string, type?: ToastType) => void
  clearToast: () => void
  reset: () => void
}

const dadosIniciais = {
  carregado: false,
  contribuinte: null,
  dependentes: [],
  semDependentes: false,
  rendimentos: [],
  bens: [],
  erroContribuinte: false,
  erroDependentes: false,
  declaracaoConsultada: null,
  idDeclaracao: null,
  enviando: false,
  enviada: false,
  toast: null,
} satisfies Partial<DeclaracaoState>

export const useDeclaracaoStore = create<DeclaracaoState>((set) => ({
  ...dadosIniciais,
  hydrate: (d) => set({ ...d, carregado: true }),
  setContribuinte: (contribuinte) => set({ contribuinte }),
  setDependentes: (dependentes) => set({ dependentes }),
  setSemDependentes: (semDependentes) => set({ semDependentes }),
  setRendimentos: (rendimentos) => set({ rendimentos }),
  setBens: (bens) => set({ bens }),
  selecionarDeclaracao: (declaracaoConsultada) => set({ declaracaoConsultada }),
  registrarEnvio: (idDeclaracao) => set({ idDeclaracao, enviada: true }),
  setEnviando: (enviando) => set({ enviando }),
  showToast: (msg, type = 'success') => set({ toast: { id: Date.now(), msg, type } }),
  clearToast: () => set({ toast: null }),
  reset: () => set(dadosIniciais),
}))

type Snapshot = Pick<DeclaracaoState, 'contribuinte' | 'dependentes' | 'semDependentes' | 'rendimentos' | 'bens' | 'enviada' | 'erroContribuinte' | 'erroDependentes'>

export const somar = <T,>(lista: T[], pick: (i: T) => number) => lista.reduce((a, i) => a + pick(i), 0)

const dadosPreenchidos = (c: ContribuinteForm | null) => c && c.Nome && c.Sobrenome && c.RG && c.Sexo

export function calcEtapas(s: Snapshot): EtapaId[] {
  const ok: EtapaId[] = []
  if (dadosPreenchidos(s.contribuinte) && !s.erroContribuinte) ok.push('dados')
  if (!s.erroDependentes && (s.dependentes.length > 0 || s.semDependentes)) ok.push('dependentes')
  if (s.rendimentos.length > 0) ok.push('rendimentos')
  if (s.bens.length > 0 && somar(s.bens, (b) => b.Valor_31_12_2026) > 100) ok.push('bens')
  if (s.enviada) ok.push('resumo')
  return ok
}

export function calcProgresso(s: Snapshot): number {
  const ok = calcEtapas(s)
  return ETAPAS.filter((e) => ok.includes(e.id)).reduce((a, e) => a + e.peso, 0)
}

/** Pendências que impedem o envio (o resumo/envio não depende de "resumo" estar concluído). */
export function validarDeclaracao(s: Snapshot): string[] {
  const p: string[] = []
  if (!dadosPreenchidos(s.contribuinte) || s.erroContribuinte) p.push('Confira os Dados do Contribuinte.')
  if (s.erroDependentes) p.push('Carregue os dependentes antes do envio.')
  if (s.dependentes.length === 0 && !s.semDependentes) p.push('Informe os dependentes ou marque "Não possuo dependentes".')
  if (s.rendimentos.length === 0) p.push('Cadastre ao menos um rendimento tributável.')
  else if (s.rendimentos.some((r) => r.Valor_Total_Recebido <= 100)) p.push('Há rendimento com valor total menor ou igual a R$ 100,00.')
  if (s.bens.length === 0 || somar(s.bens, (b) => b.Valor_31_12_2026) <= 100) p.push('A soma dos bens e direitos deve ser maior que R$ 100,00.')
  return p
}
