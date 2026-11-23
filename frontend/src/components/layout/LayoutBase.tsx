import { useEffect, useState } from 'react'
import { Outlet } from 'react-router-dom'
import Header from './Header'
import Sidebar from './Sidebar'
import Footer from './Footer'
import Toast from '@/components/ui/Toast'
import Button from '@/components/ui/Button'
import { declaracaoService } from '@/services/declaracaoService'
import { useDeclaracaoStore } from '@/store/useDeclaracaoStore'
import { useAuthStore } from '@/store/useAuthStore'

export default function LayoutBase() {
  const usuario = useAuthStore((s) => s.usuario)
  const erroContribuinte = useDeclaracaoStore((s) => s.erroContribuinte)
  const erroDependentes = useDeclaracaoStore((s) => s.erroDependentes)
  const declaracaoConsultada = useDeclaracaoStore((s) => s.declaracaoConsultada)
  const selecionarDeclaracao = useDeclaracaoStore((s) => s.selecionarDeclaracao)
  const enviada = useDeclaracaoStore((s) => s.enviada)
  const [tentativa, setTentativa] = useState(0)
  const [recarregando, setRecarregando] = useState(false)

  useEffect(() => {
    if (!usuario) return

    let ativo = true
    setRecarregando(true)
    // Uma falha de consulta fica visível e preserva a navegação entre as abas.
    Promise.allSettled([
      declaracaoService.carregarContribuinte(usuario.loginCpf),
      declaracaoService.listarDependentes(usuario.idPessoa),
    ]).then(([perfil, dependentes]) => {
      if (!ativo) return
      useDeclaracaoStore.getState().hydrate({
        ...(perfil.status === 'fulfilled' ? { contribuinte: perfil.value } : {}),
        ...(dependentes.status === 'fulfilled' ? { dependentes: dependentes.value } : {}),
        erroContribuinte: perfil.status === 'rejected',
        erroDependentes: dependentes.status === 'rejected',
      })
      setRecarregando(false)
    })
    return () => { ativo = false }
  }, [usuario, tentativa])

  return (
    <div className="flex min-h-screen flex-col bg-white">
      <Header />
      <div className="flex w-full flex-1 gap-5 px-6 py-5">
        <Sidebar />
        <main className="min-w-0 flex-1">
          {(erroContribuinte || erroDependentes) && (
            <div role="alert" className="mb-4 flex flex-wrap items-center gap-3 rounded-lg bg-amber-50 p-3 text-sm text-amber-800">
              {erroContribuinte && <span>Falha ao consultar os dados do contribuinte.</span>}
              {erroDependentes && <span>Falha ao consultar os dependentes.</span>}
              <Button disabled={recarregando} onClick={() => setTentativa((n) => n + 1)}>Tentar novamente</Button>
            </div>
          )}
          {declaracaoConsultada && (
            <div className="mb-4 flex flex-wrap items-center justify-between gap-3 rounded-lg bg-brand-soft p-3 text-sm text-brand">
              <p role="status">
                <strong>Consulta da declaração #{declaracaoConsultada.idDeclaracao}</strong>
                {' · '}{declaracaoConsultada.anoExercicio}{' · '}{declaracaoConsultada.status}
              </p>
              <Button variant="ghost" onClick={() => selecionarDeclaracao(null)}>
                {enviada ? 'Voltar à declaração enviada' : 'Voltar ao rascunho'}
              </Button>
            </div>
          )}
          <Outlet />
        </main>
      </div>
      <Footer />
      <Toast />
    </div>
  )
}
