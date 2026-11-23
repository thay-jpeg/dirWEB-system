import type { SessaoUsuario } from '@/types'
import type { CadastroForm } from '@/types/authSchemas'
import { api } from './api'

export const authService = {
  async autenticar(cpf: string, senha: string): Promise<SessaoUsuario> {
    const { data } = await api.post<SessaoUsuario>('/irpf/login', {
      cpf: cpf.replace(/\D/g, ''),
      senha,
    })
    if (data.perfilAcesso !== 'contribuinte' && data.perfilAcesso !== 'auditorFiscal') {
      throw new Error('A conta está com um perfil de acesso inválido.')
    }
    return data
  },

  async cadastrarContribuinte({ cpf, nome, senha }: CadastroForm) {
    const loginCpf = cpf.replace(/\D/g, '')
    // O backend recebe a senha no campo senhaHash e calcula o hash no cadastro.
    await api.post('/irpf/cadastrarContribuinte', {
      loginCpf,
      senhaHash: senha,
      contribuinte: { nome, cpf: loginCpf },
    })
  },
}
