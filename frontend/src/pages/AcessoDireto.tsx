import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { authService } from '@/services/authService'
import { useAuthStore } from '@/store/useAuthStore'
import { homePorPerfil } from '@/utils/perfil'

export default function AcessoDireto() {
  const { identificador } = useParams<{ identificador: string }>()
  const navigate = useNavigate()
  const [erro, setErro] = useState<string | null>(null)

  useEffect(() => {
    if (!identificador) {
      navigate('/acesso', { replace: true })
      return
    }

    let ativo = true

    authService
      .identificar(identificador)
      .then((usuario) => {
        if (!ativo) return
        // Confira o nome do método no seu useAuthStore.ts (ex.: login ou setUsuario)
        useAuthStore.getState().login(usuario)
        navigate(homePorPerfil(usuario.perfilAcesso), { replace: true })
      })
      .catch((err) => {
        if (!ativo) return
        setErro(err instanceof Error ? err.message : 'CPF ou código de auditor não encontrado.')
      })

    return () => {
      ativo = false
    }
  }, [identificador, navigate])

  if (erro) {
    return (
      <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
        <p className="text-red-600 font-medium">{erro}</p>
        <Link to="/acesso" className="text-blue-700 underline">
          Voltar para a tela de identificação
        </Link>
      </div>
    )
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-6 text-gray-600">
      Identificando perfil...
    </div>
  )
}