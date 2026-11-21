package unioeste.casestudy.irpf.bo;

import java.io.Serializable;
import unioeste.geral.endereco.bo.Endereco;

public class BemDireito implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idBem;
    private String nomeBem;
    private Double valorAnoAnterior;
    private Double valorAtual;
    private TipoBem tipoBem;
    private Endereco endereco;
    private Integer idDeclaracao;
    private Integer idContribuinte;

    public BemDireito() {}

    public Integer getIdBem() { return idBem; }
    public void setIdBem(Integer idBem) { this.idBem = idBem; }

    public String getNomeBem() { return nomeBem; }
    public void setNomeBem(String nomeBem) { this.nomeBem = nomeBem; }

    public Double getValorAnoAnterior() { return valorAnoAnterior; }
    public void setValorAnoAnterior(Double valorAnoAnterior) { this.valorAnoAnterior = valorAnoAnterior; }

    public Double getValorAtual() { return valorAtual; }
    public void setValorAtual(Double valorAtual) { this.valorAtual = valorAtual; }

    public TipoBem getTipoBem() { return tipoBem; }
    public void setTipoBem(TipoBem tipoBem) { this.tipoBem = tipoBem; }

    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }

    public Integer getIdDeclaracao() { return idDeclaracao; }
    public void setIdDeclaracao(Integer idDeclaracao) { this.idDeclaracao = idDeclaracao; }

    public Integer getIdContribuinte() { return idContribuinte; }
    public void setIdContribuinte(Integer idContribuinte) { this.idContribuinte = idContribuinte; }
}