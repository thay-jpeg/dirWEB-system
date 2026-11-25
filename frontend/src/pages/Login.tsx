import { useState } from 'react'
import AuthLayout from '@/components/auth/AuthLayout'
import StepCpf from '@/components/auth/StepCpf'
import StepCadastro from '@/components/auth/StepCadastro'
import ilustracaoGovBr from '@/assets/govbr-ilustracao.png'

type Step = 'identificar' | 'cadastro'

export default function Login() {
  const [step, setStep] = useState<Step>('identificar')
  const [identificador, setIdentificador] = useState('')

  return (
    <AuthLayout
      ilustracao={ilustracaoGovBr}
      ilustracaoAlt="Tela de identificação"
    >
      {step === 'identificar' && (
        <StepCpf
          initialCpf={identificador}
          onResult={(valor) => {
            setIdentificador(valor)
            setStep('cadastro')
          }}
        />
      )}
      {step === 'cadastro' && (
        <StepCadastro
          cpf={identificador}
          onCancel={() => setStep('identificar')}
        />
      )}
    </AuthLayout>
  )
}