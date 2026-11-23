import PageHeader from '@/components/ui/PageHeader'

export default function GestaoAcessos() {
  return (
    <div>
      <PageHeader title="Gestão de Acessos" />
      {/* A API ainda não oferece consulta ou alteração de perfis de acesso. */}
      <p className="text-sm text-brand">A gestão de acessos ainda não está disponível.</p>
    </div>
  )
}
