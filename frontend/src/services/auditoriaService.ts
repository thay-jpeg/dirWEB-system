import type { DeclaracaoAuditoria, StatusDeclaracao } from '@/types'
import { api } from './api'
import { declaracaoService, mapearBem, mapearContribuinte, mapearDependente, mapearRendimento } from './declaracaoService'
import type { DeclaracaoApi } from './declaracaoService'

function statusAuditoria(status: string): status is StatusDeclaracao {
  return status === 'Enviada' || status === 'Processada' || status === 'Retida'
}

function mapearDeclaracao(declaracao: DeclaracaoApi): DeclaracaoAuditoria {
  if (!statusAuditoria(declaracao.status)) {
    throw new Error('Esta declaração ainda não está disponível na triagem.')
  }

  const dados = mapearContribuinte(declaracao.contribuinte)
  return {
    idDeclaracao: declaracao.idDeclaracao,
    anoExercicio: declaracao.anoExercicio,
    status: declaracao.status,
    contribuinte: {
      cpf: dados.CPF,
      nome: dados.Nome,
      sobrenome: dados.Sobrenome,
      nomeSocial: dados.Nome_Social,
      rg: dados.RG,
      sexo: dados.Sexo,
      email: dados.Email,
      telefone: dados.Telefone,
      endereco: [dados.Rua, dados.Numero, dados.Complemento, dados.Bairro, dados.Cidade, dados.UF, dados.CEP]
        .filter(Boolean).join(', '),
    },
    dependentes: declaracao.dependentes.map(mapearDependente),
    rendimentos: declaracao.rendimentos.map(mapearRendimento),
    bens: declaracao.bensDireitos.map(mapearBem),
  }
}

async function consultarTriagem() {
  const { data } = await api.get<DeclaracaoApi[]>('/irpf/auditoria/declaracoes')
  return data
}

export const auditoriaService = {
  async listarDeclaracoes() {
    return (await consultarTriagem()).map(mapearDeclaracao)
  },

  async obterDeclaracao(idDeclaracao: number) {
    // Valida a sessão na triagem e obtém o titular antes de carregar o detalhe.
    const item = (await consultarTriagem()).find((d) => d.idDeclaracao === idDeclaracao)
    if (!item) throw new Error('Declaração não encontrada na triagem.')
    return mapearDeclaracao(await declaracaoService.consultarDeclaracao(item.contribuinte.idPessoa, idDeclaracao))
  },
}
