package unioeste.casestudy.irpf.col;

import unioeste.casestudy.irpf.manager.IRPFException;

//Validação de codAuditor ou CPF
public class AuditorFiscalCol {

    private final PessoaCol pessoaCol = new PessoaCol();

    public boolean identificadorValido(String identificador) {
        return identificador != null && !identificador.isBlank();
    }

    public String extrairCpfLimpo(String identificador) {
        return pessoaCol.limparDigitos(identificador);
    }

    public void validarIdAuditor(int idAuditor) throws IRPFException {
        if (idAuditor <= 0) {
            throw new IRPFException("Identificação do auditor fiscal é obrigatória.");
        }
    }
}