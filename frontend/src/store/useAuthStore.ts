import { create } from 'zustand'
import type { SessaoUsuario } from '@/types'
import { useDeclaracaoStore } from './useDeclaracaoStore'

interface AuthState {
  usuario: SessaoUsuario | null
  login: (usuario: SessaoUsuario) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  // Sem uma rota de consulta de sessão, recarregar a página pede novo login.
  usuario: null,
  login: (usuario) => {
    useDeclaracaoStore.getState().reset()
    set({ usuario })
  },
  logout: () => {
    // A API ainda não tem logout. Limpa a interface e os dados em memória.
    useDeclaracaoStore.getState().reset()
    set({ usuario: null })
  },
}))
