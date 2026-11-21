package unioeste.geral.pessoa.bo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import unioeste.geral.endereco.bo.Endereco;

public abstract class Pessoa implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idPessoa;
    private String nome;
    private Endereco endereco;
    private List<Email> emails = new ArrayList<>();
    private List<Telefone> telefones = new ArrayList<>();

    public Pessoa() {}

    public Integer getIdPessoa() { return idPessoa; }
    public void setIdPessoa(Integer idPessoa) { this.idPessoa = idPessoa; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }

    public List<Email> getEmails() { return emails; }
    public void setEmails(List<Email> emails) { this.emails = emails; }

    public List<Telefone> getTelefones() { return telefones; }
    public void setTelefones(List<Telefone> telefones) { this.telefones = telefones; }
}