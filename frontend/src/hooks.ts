import { useNavigate } from 'react-router-dom'
import { declaracaoService } from '@/services/declaracaoService'
import { useAuthStore } from '@/store/useAuthStore'
import { useDeclaracaoStore, validarDeclaracao } from '@/store/useDeclaracaoStore'

export function useSair() {
  const navigate = useNavigate()
  return () => {
    useAuthStore.getState().logout()
    navigate('/login', { replace: true })
  }
}

export function useEnviarDeclaracao() {
  const enviando = useDeclaracaoStore((s) => s.enviando)

  const enviar = async () => {
    const s = useDeclaracaoStore.getState()
    const usuario = useAuthStore.getState().usuario
    if (!usuario || !s.carregado || s.enviando || s.enviada || s.declaracaoConsultada) return

    const pendencias = validarDeclaracao(s)
    if (pendencias.length > 0) {
      window.alert('Não foi possível enviar a declaração:\n\n• ' + pendencias.join('\n• '))
      return
    }
    if (!window.confirm('Confirmar o envio da Declaração de Imposto de Renda 2026?')) return

    try {
      // O início e o resumo compartilham o bloqueio de envio para evitar chamadas simultâneas.
      s.setEnviando(true)
      const id = await declaracaoService.enviarDeclaracao(usuario.idPessoa, s.rendimentos, s.bens)
      if (useAuthStore.getState().usuario !== usuario) return
      s.registrarEnvio(id)
      s.showToast(`Declaração enviada com sucesso. ID ${id}.`)
    } catch (error) {
      if (useAuthStore.getState().usuario === usuario) {
        s.showToast(error instanceof Error ? error.message : 'Erro ao enviar a declaração.', 'error')
      }
    } finally {
      if (useAuthStore.getState().usuario === usuario) s.setEnviando(false)
    }
  }
  return { enviar, enviando }
}
