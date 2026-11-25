package unioeste.casestudy.irpf.col;

import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.manager.IRPFException;

//Cadastro, atualização, normalização de nome/sobrenome e consulta de contribuinte
public class ContribuinteCol {

    private final PessoaCol pessoaCol = new PessoaCol();

    public void validarCadastro(Contribuinte c) throws IRPFException {
        if (c == null) {
            throw new IRPFException("Dados do contribuinte são obrigatórios.");
        }
        if (!pessoaCol.validarCPF(c.getCpf())) {
            throw new IRPFException("CPF inválido. Informe 11 dígitos numéricos.");
        }
        if (c.getNome() == null || c.getNome().isBlank()) {
            throw new IRPFException("Nome do contribuinte é obrigatório.");
        }
        pessoaCol.validarEmails(c.getEmails());
        pessoaCol.validarTelefones(c.getTelefones());
    }

    public void normalizarCadastro(Contribuinte c) {
        c.setCpf(pessoaCol.limparDigitos(c.getCpf()));

        if ((c.getSobrenome() == null || c.getSobrenome().isBlank())
                && c.getNome().trim().contains(" ")) {
            String[] partes = c.getNome().trim().split("\\s+", 2);
            c.setNome(partes[0]);
            c.setSobrenome(partes[1]);
        }
    }

    public void validarAtualizacao(Contribuinte c) throws IRPFException {
        if (c == null) {
            throw new IRPFException("Objeto Contribuinte não pode ser nulo.");
        }
        boolean temId = c.getIdPessoa() != null && c.getIdPessoa() > 0;
        boolean temCpf = pessoaCol.validarCPF(c.getCpf());
        if (!temId && !temCpf) {
            throw new IRPFException("Informe o idPessoa ou um CPF válido para atualizar o contribuinte.");
        }
        if (c.getNome() == null || c.getNome().isBlank()) {
            throw new IRPFException("O nome do contribuinte é obrigatório.");
        }
        pessoaCol.validarEmails(c.getEmails());
        pessoaCol.validarTelefones(c.getTelefones());
    }

    public void validarId(int idPessoa) throws IRPFException {
        if (idPessoa <= 0) {
            throw new IRPFException("Informe um ID de contribuinte válido.");
        }
    }

    public String validarELimparCpfConsulta(String cpf) throws IRPFException {
        if (!pessoaCol.validarCPF(cpf)) {
            throw new IRPFException("CPF inválido.");
        }
        return pessoaCol.limparDigitos(cpf);
    }
}