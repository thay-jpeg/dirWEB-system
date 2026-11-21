package unioeste.geral.pessoa.bo;

public class PessoaFisica extends Pessoa {
    private static final long serialVersionUID = 1L;

    private String sobrenome;
    private String nomeSocial;
    private String cpf;
    private String rg;
    private String sexo;

    public PessoaFisica() {}

    public String getSobrenome() { return sobrenome; }
    public void setSobrenome(String sobrenome) { this.sobrenome = sobrenome; }

    public String getNomeSocial() { return nomeSocial; }
    public void setNomeSocial(String nomeSocial) { this.nomeSocial = nomeSocial; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }

    public String getSexo() { return sexo; }
    public void setSexo(String sexo) { this.sexo = sexo; }

    public String getNomeCompleto() {
        if (getNome() == null) return sobrenome;
        if (sobrenome == null || sobrenome.isBlank()) return getNome();
        return getNome() + " " + sobrenome;
    }
}