import axios from 'axios'

export const api = axios.create({
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
    const message = error.response?.data?.mensagem ?? error.response?.data?.message
    return Promise.reject(new ErroApi(
      status && status < 500 && typeof message === 'string'
        ? message
        : 'Não foi possível comunicar com o servidor. Tente novamente.',
      status,
    ))
  },
)