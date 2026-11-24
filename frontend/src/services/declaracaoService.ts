import type { Bem, ContribuinteForm, Dependente, Rendimento, SessaoUsuario, TipoDependente } from '@/types'
import { api, ErroApi } from './api'
import { mapearBackendParaFrontend, mapearFrontendParaBackend } from './enderecoservice'
import type { EnderecoBackendDTO } from './enderecoservice'


type TipoDependenciaApi = { idTipoDependencia: number; descricao: string | null }
type FontePagadoraApi = { cnpj: string; nome: string }
export type ContribuinteApi = {
  idPessoa: number
  cpf: string
  nome: string
  sobrenome: string | null
  nomeSocial: string | null
  rg: string | null
  sexo: string | null
  endereco: EnderecoBackendDTO | null
  emails: { email: string }[]
  telefones: { fone: string; ddd: { ddd: string } | null; ddi?: { ddi: string } | null }[]
}
type DependenteApi = { idDependente: number; nome: string; cpf: string; tipoDependencia: TipoDependenciaApi }
type RendimentoApi = {
  idRendimento: number
  fontePagadora: FontePagadoraApi
  valorTotalRecebido: number
  inss: number
  irrfPago: number
  valor13Salario: number
}
type BemApi = {
  idBem: number
  nomeBem: string
  tipoBem: { idTipoBem: number; descricao: string } | null
  valorAnoAnterior: number
  valorAtual: number
}
export type DeclaracaoApi = {
  idDeclaracao: number
  anoExercicio: number
  status: string
  contribuinte: ContribuinteApi
  dependentes: DependenteApi[]
  rendimentos: RendimentoApi[]
  bensDireitos: BemApi[]
}

const digitos = (valor: string) => valor.replace(/\D/g, '')
const normalizar = (valor: string) =>
  valor.trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()

// Traduz os objetos Java para os campos já usados pelos formulários e tabelas.
export function mapearContribuinte(pessoa: ContribuinteApi): ContribuinteForm {
  const endereco = pessoa.endereco ? mapearBackendParaFrontend(pessoa.endereco) : null
  const telefone = pessoa.telefones[0]

  return {
    Nome: pessoa.nome,
    Sobrenome: pessoa.sobrenome ?? '',
    Nome_Social: pessoa.nomeSocial ?? '',
    CPF: pessoa.cpf,
    RG: pessoa.rg ?? '',
    Sexo: pessoa.sexo ?? '',
    Email: pessoa.emails[0]?.email ?? '',
    DDITelefone: digitos(telefone?.ddi?.ddi ?? '55'),
    Telefone: telefone ? [telefone.ddd?.ddd, telefone.fone].filter(Boolean).join(' ') : '',
    Rua: endereco?.Logradouro ?? '',
    Numero: endereco?.Numero ?? '',
    Complemento: endereco?.Complemento ?? '',
    Bairro: endereco?.Bairro ?? '',
    Cidade: endereco?.Cidade ?? '',
    UF: endereco?.UF ?? '',
    CEP: endereco?.CEP ?? '',
  }
}

export function mapearDependente(dep: DependenteApi): Dependente {
  return {
    idDependente: dep.idDependente,
    Nome: dep.nome,
    CPF: dep.cpf,
    Tipo_dependente_idTipo_dependente: dep.tipoDependencia.idTipoDependencia,
    Tipo_dependente: dep.tipoDependencia.descricao ?? undefined,
  }
}

export function mapearRendimento(rendimento: RendimentoApi): Rendimento {
  return {
    idRendimentos: rendimento.idRendimento,
    CNPJ: rendimento.fontePagadora.cnpj,
    nomeEmpresa: rendimento.fontePagadora.nome,
    Valor_Total_Recebido: rendimento.valorTotalRecebido,
    INSS: rendimento.inss,
    IRRF_pago: rendimento.irrfPago,
    valor13Salario: rendimento.valor13Salario,
  }
}

export function mapearBem(bem: BemApi): Bem {
  return {
    idBens_direitos: bem.idBem,
    Tipo_Bem: bem.tipoBem?.descricao ?? '',
    Nome_bem: bem.nomeBem,
    Valor_ano_anterior: bem.valorAnoAnterior,
    Valor_31_12_2026: bem.valorAtual,
  }
}

async function consultarContribuinte(cpf: string) {
  const { data } = await api.get<ContribuinteApi>(`/irpf/contribuintes/cpf/${digitos(cpf)}`)
  return data
}

async function resolverEndereco(form: ContribuinteForm, atual: EnderecoBackendDTO | null) {
  if (!form.CEP.trim()) return atual
  const cep = digitos(form.CEP)

  // A API precisa dos IDs de cidade, bairro, logradouro e UF. Reusa referências de um CEP cadastrado.
  const corresponde = (dto: EnderecoBackendDTO) => {
    const endereco = mapearBackendParaFrontend(dto)
    return digitos(endereco.CEP) === cep &&
      normalizar(endereco.Logradouro) === normalizar(form.Rua) &&
      normalizar(endereco.Bairro) === normalizar(form.Bairro) &&
      normalizar(endereco.Cidade) === normalizar(form.Cidade) &&
      normalizar(endereco.UF) === normalizar(form.UF)
  }

  const mesmoNumero = (dto: EnderecoBackendDTO) =>
    (dto.numero ?? '') === form.Numero.trim() && (dto.complemento ?? '') === (form.Complemento ?? '').trim()

  if (atual && corresponde(atual) && mesmoNumero(atual)) return atual

  const { data: candidatos } = await api.get<EnderecoBackendDTO[]>(`/endereco/obterEnderecoPorCEP/${cep}`)
  const referencias = atual ? [atual, ...candidatos] : candidatos
  const validos = referencias.filter(corresponde)
  const existente = validos.find(mesmoNumero)
  if (existente) return existente

  const modelo = validos[0]
  if (!modelo) {
    return mapearFrontendParaBackend({
      CEP: cep,
      Numero: form.Numero.trim(),
      Complemento: form.Complemento?.trim(),
      Bairro: form.Bairro,
      Logradouro: form.Rua,
      Cidade: form.Cidade,
      UF: form.UF,
    })
  }

  return {
    ...modelo,
    idEndereco: 0,
    numero: form.Numero.trim(),
    complemento: form.Complemento?.trim(),
  }
}

export const declaracaoService = {
  async carregarContribuinte(cpf: string) {
    return mapearContribuinte(await consultarContribuinte(cpf))
  },

  async atualizarContribuinte(usuario: SessaoUsuario, form: ContribuinteForm) {
    // O PUT substitui os contatos. Preserva os telefones e os outros e-mails já cadastrados.
    const atual = await consultarContribuinte(usuario.loginCpf)
    const endereco = await resolverEndereco(form, atual.endereco)
    const emails = atual.emails.slice(1)
    if (form.Email.trim()) emails.unshift({ email: form.Email.trim() })
      const numero = digitos(form.Telefone)
const ddiDigitado = form.DDITelefone.trim()

if (numero && !/^\+?[1-9]\d{0,2}$/.test(ddiDigitado)) {
  throw new Error('Informe um DDI com 1 a 3 dígitos, como +55.')
}

const codigoDdi = digitos(ddiDigitado)
const telefoneAtual = atual.telefones[0]
const numeroAtual = telefoneAtual
  ? digitos((telefoneAtual.ddd?.ddd ?? '') + telefoneAtual.fone)
  : ''
const ddiAtual = digitos(telefoneAtual?.ddi?.ddi ?? '55')
const alterouTelefone = numero !== '' &&
  (numero !== numeroAtual || codigoDdi !== ddiAtual)

if (alterouTelefone && !/^\d{10,11}$/.test(numero)) {
  throw new Error('Informe o DDD e um telefone com 8 ou 9 dígitos.')
}

const telefones = alterouTelefone
  ? [{
      fone: numero.slice(2),
      ddd: { ddd: numero.slice(0, 2) },
      ddi: { ddi: codigoDdi },
    }, ...atual.telefones.slice(1)]
  : atual.telefones

    const { data } = await api.put<ContribuinteApi>('/irpf/atualizarDadosContribuinte', {
      idPessoa: usuario.idPessoa,
      cpf: usuario.loginCpf,
      nome: form.Nome.trim(),
      sobrenome: form.Sobrenome.trim(),
      nomeSocial: form.Nome_Social.trim(),
      rg: form.RG.trim(),
      sexo: form.Sexo,
      endereco,
      emails,
      telefones,
    })
    return mapearContribuinte(data)
  },

  async listarTiposDependencia(): Promise<TipoDependente[]> {
    const { data } = await api.get<TipoDependenciaApi[]>('/irpf/listarTiposDependencia')
    return data.map((tipo) => ({
      idTipo_dependente: tipo.idTipoDependencia,
      Tipo_dependente: tipo.descricao ?? '',
    }))
  },

  async listarDependentes(idPessoa: number) {
    const { data } = await api.get<DependenteApi[]>(`/irpf/contribuintes/${idPessoa}/dependentes`)
    return data.map(mapearDependente)
  },

  async cadastrarDependente(idPessoa: number, formulario: Dependente, descricao: string) {
    const { data } = await api.post<DependenteApi>('/irpf/cadastrarDependente', {
      nome: formulario.Nome.trim(),
      cpf: digitos(formulario.CPF),
      tipoDependencia: { idTipoDependencia: formulario.Tipo_dependente_idTipo_dependente },
      contribuinte: { idPessoa },
    })
    // O POST devolve o ID do tipo. A descrição já veio do catálogo do banco.
    return { ...mapearDependente(data), Tipo_dependente: data.tipoDependencia.descricao ?? descricao }
  },

  async consultarFontePagadora(cnpj: string) {
    try {
      const { data } = await api.get<FontePagadoraApi>(`/irpf/fontes-pagadoras/${digitos(cnpj)}`)
      return data
    } catch (error) {
      if (error instanceof ErroApi && error.status === 400 && error.message === 'Fonte pagadora não encontrada.') {
        return null
      }
      throw error
    }
  },

  async cadastrarFontePagadora(cnpj: string, nome: string) {
    const { data } = await api.post<FontePagadoraApi>('/irpf/cadastrarPessoaJuridica', {
      cnpj: digitos(cnpj), nome: nome.trim(),
    })
    return data
  },

  async consultarDeclaracao(idPessoa: number, idDeclaracao: number) {
    const { data } = await api.get<DeclaracaoApi>(`/irpf/contribuintes/${idPessoa}/declaracoes/${idDeclaracao}`)
    return data
  },

  async enviarDeclaracao(idPessoa: number, rendimentos: Rendimento[], bens: Bem[]) {
    // Registra bens e rendimentos juntos. O endpoint cria a declaração e devolve o ID do banco.
    const { data } = await api.post<DeclaracaoApi>('/irpf/cadastrarDeclaracaoRenda', {
      contribuinte: { idPessoa },
      anoExercicio: 2026,
      status: 'Enviada',
      rendimentos: rendimentos.map((r) => ({
        fontePagadora: { cnpj: digitos(r.CNPJ) },
        valorTotalRecebido: r.Valor_Total_Recebido,
        inss: r.INSS,
        irrfPago: r.IRRF_pago,
        valor13Salario: r.valor13Salario,
      })),
      bensDireitos: bens.map((b) => ({
        nomeBem: b.Nome_bem,
        tipoBem: { descricao: b.Tipo_Bem },
        valorAnoAnterior: b.Valor_ano_anterior,
        valorAtual: b.Valor_31_12_2026,
      })),
    })
    return data.idDeclaracao
  },
}
