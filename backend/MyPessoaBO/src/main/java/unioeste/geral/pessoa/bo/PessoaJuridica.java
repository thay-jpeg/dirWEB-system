package unioeste.geral.pessoa.bo;

public class PessoaJuridica extends Pessoa {
    private static final long serialVersionUID = 1L;

    private String cnpj;

    public PessoaJuridica() {}

    public String getCnpj() { return cnpj; }
    public void setCnpj(String cnpj) { this.cnpj = cnpj; }
}