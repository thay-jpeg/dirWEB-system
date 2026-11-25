package unioeste.casestudy.irpf.col;

import unioeste.casestudy.irpf.manager.IRPFException;
import unioeste.geral.pessoa.bo.PessoaJuridica;

//Validação de cadastro e consulta de fonte pagadora
public class PessoaJuridicaCol {

    private final PessoaCol pessoaCol = new PessoaCol();

    public void validarCadastro(PessoaJuridica pj) throws IRPFException {
        if (pj == null) {
            throw new IRPFException("Dados da Pessoa Jurídica não informados.");
        }
        if (pj.getNome() == null || pj.getNome().isBlank()) {
            throw new IRPFException("Nome/Razão Social da empresa é obrigatório.");
        }
        if (!pessoaCol.validarCNPJ(pj.getCnpj())) {
            throw new IRPFException("CNPJ inválido. Informe 14 dígitos numéricos.");
        }
        pessoaCol.validarEmails(pj.getEmails());
        pessoaCol.validarTelefones(pj.getTelefones());
    }

    public String validarELimparCnpj(String cnpj) throws IRPFException {
        if (!pessoaCol.validarCNPJ(cnpj)) {
            throw new IRPFException("CNPJ inválido.");
        }
        return pessoaCol.limparDigitos(cnpj);
    }
}