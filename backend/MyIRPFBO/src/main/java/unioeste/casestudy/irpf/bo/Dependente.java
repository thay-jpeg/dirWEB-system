package unioeste.casestudy.irpf.bo;

import java.io.Serializable;

public class Dependente implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idDependente;
    private String nome;
    private String cpf;
    private TipoDependencia tipoDependencia;
    private Contribuinte contribuinte;

    public Dependente() {}

    public Integer getIdDependente() { return idDependente; }
    public void setIdDependente(Integer idDependente) { this.idDependente = idDependente; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }

    public TipoDependencia getTipoDependencia() { return tipoDependencia; }
    public void setTipoDependencia(TipoDependencia tipoDependencia) { this.tipoDependencia = tipoDependencia; }

    public Contribuinte getContribuinte() { return contribuinte; }
    public void setContribuinte(Contribuinte contribuinte) { this.contribuinte = contribuinte; }
}