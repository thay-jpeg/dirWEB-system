import axios from 'axios'
import { useAuthStore } from '@/store/useAuthStore'

export const api = axios.create({
  // A API Java cria a sessão no cookie JSESSIONID, enviado pelo próprio navegador.
  baseURL: '/api',
  timeout: 15000,
})

export class ErroApi extends Error {
  constructor(message: string, public readonly status?: number) {
    super(message)
  }
}

api.interceptors.response.use(
  (res) => res,
  (error) => {
    const status = error.response?.status
    if (status === 401 && error.config?.url !== '/irpf/login') {
      useAuthStore.getState().logout()
    }

    const message = error.response?.data?.mensagem ?? error.response?.data?.message
    return Promise.reject(new ErroApi(
      status && status < 500 && typeof message === 'string' ? message : 'Não foi possível comunicar com o servidor. Tente novamente.',
      status,
    ))
  },
)
