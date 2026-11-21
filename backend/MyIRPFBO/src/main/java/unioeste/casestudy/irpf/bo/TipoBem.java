package unioeste.casestudy.irpf.bo;

import java.io.Serializable;

public class TipoBem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idTipoBem;
    private String descricao;

    public TipoBem() {}

    public Integer getIdTipoBem() { return idTipoBem; }
    public void setIdTipoBem(Integer idTipoBem) { this.idTipoBem = idTipoBem; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}