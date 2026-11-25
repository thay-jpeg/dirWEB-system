package unioeste.casestudy.irpf.col;

import unioeste.casestudy.irpf.bo.Dependente;
import unioeste.casestudy.irpf.manager.IRPFException;

//Validação de cadastro, listagem e exclusão de dependentes
public class DependenteCol {

    private final PessoaCol pessoaCol = new PessoaCol();

    public void validarCadastro(Dependente dep) throws IRPFException {
        validarDadosBasicos(dep);
        if (dep.getContribuinte() == null) {
            throw new IRPFException("Contribuinte titular do dependente é obrigatório.");
        }
    }

    public void validarDadosBasicos(Dependente dep) throws IRPFException {
        if (dep == null) {
            throw new IRPFException("Dados do dependente não informados.");
        }
        if (dep.getNome() == null || dep.getNome().isBlank()) {
            throw new IRPFException("Nome do dependente é obrigatório.");
        }
        if (!pessoaCol.validarCPF(dep.getCpf())) {
            throw new IRPFException("CPF do dependente inválido.");
        }
        if (dep.getTipoDependencia() == null || dep.getTipoDependencia().getIdTipoDependencia() == null) {
            throw new IRPFException("Tipo de dependência é obrigatório.");
        }
    }

    public void normalizarCpf(Dependente dep) {
        dep.setCpf(pessoaCol.limparDigitos(dep.getCpf()));
    }

    public void validarExclusao(int idContribuinte, int idDependente) throws IRPFException {
        if (idContribuinte <= 0 || idDependente <= 0) {
            throw new IRPFException("Informe IDs válidos para contribuinte e dependente.");
        }
    }
}