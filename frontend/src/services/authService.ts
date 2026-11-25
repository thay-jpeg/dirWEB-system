import type { SessaoUsuario } from '@/types'
import { api } from './api'

interface PerfilApi {
  idPessoa: number
  cpf: string
  codAuditor: string | null
  nome: string
  sobrenome: string | null
  perfilAcesso: string
}

interface ContribuinteCriadoApi {
  idPessoa: number
  cpf: string
  nome: string
  sobrenome?: string | null
}

export const authService = {
  async identificar(identificador: string): Promise<SessaoUsuario> {
    const valor = identificador.trim()
    const { data } = await api.get<PerfilApi>(
      `/irpf/identificar/${encodeURIComponent(valor)}`,
    )

    if (data.perfilAcesso !== 'contribuinte' && data.perfilAcesso !== 'auditorFiscal') {
      throw new Error('Perfil de acesso inválido.')
    }

    return {
      idPessoa: data.idPessoa,
      loginCpf: data.cpf,
      nome: data.nome,
      sobrenome: data.sobrenome ?? '',
      perfilAcesso: data.perfilAcesso,
    }
  },

  async cadastrarContribuinte({ cpf, nome }: { cpf: string; nome: string }): Promise<SessaoUsuario> {
    const cpfLimpo = cpf.replace(/\D/g, '')
    const { data } = await api.post<ContribuinteCriadoApi>('/irpf/cadastrarContribuinte', {
      nome: nome.trim(),
      cpf: cpfLimpo,
    })

    return {
      idPessoa: data.idPessoa,
      loginCpf: data.cpf,
      nome: data.nome,
      sobrenome: data.sobrenome ?? '',
      perfilAcesso: 'contribuinte',
    }
  },
}