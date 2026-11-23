export type PerfilAcesso = 'contribuinte' | 'auditorFiscal'

// Formato devolvido pelo login da API. O identificador da sessão fica no cookie.
export interface SessaoUsuario {
  idPessoa: number
  loginCpf: string
  nome: string
  sobrenome: string
  perfilAcesso: PerfilAcesso
}
