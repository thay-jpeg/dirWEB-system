import { useState } from 'react'
import AuthLayout from '@/components/auth/AuthLayout'
import StepCpf from '@/components/auth/StepCpf'
import StepSenha from '@/components/auth/StepSenha'
import StepCadastro from '@/components/auth/StepCadastro'
import ilustracaoGovBr from '@/assets/govbr-ilustracao.png'
import ilustracaoSenha from '@/assets/govbr-senha.png'

type Step = 'cpf' | 'senha' | 'cadastro'

export default function Login() {
  const [step, setStep] = useState<Step>('cpf')
  const [cpf, setCpf] = useState('')
  const [cadastroConcluido, setCadastroConcluido] = useState(false)

  const comSenha = step === 'senha'

  // Sem consulta de conta por CPF, a pessoa escolhe entre entrar e criar uma conta.
  return (
    <AuthLayout
      ilustracao={comSenha ? ilustracaoSenha : ilustracaoGovBr}
      ilustracaoAlt={comSenha ? 'Tela de login' : 'Tela de identificação'}
    >
      {step === 'cpf' && (
        <StepCpf initialCpf={cpf} onResult={(valor, destino) => {
          setCpf(valor)
          setCadastroConcluido(false)
          setStep(destino)
        }} />
      )}
      {step === 'senha' && (
        <StepSenha cpf={cpf} cadastroConcluido={cadastroConcluido} onCancel={() => setStep('cpf')} />
      )}
      {step === 'cadastro' && (
        <StepCadastro cpf={cpf} onCancel={() => setStep('cpf')} onDone={() => {
          setCadastroConcluido(true)
          setStep('senha')
        }} />
      )}
    </AuthLayout>
  )
}
