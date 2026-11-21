package unioeste.casestudy.irpf.bo;

import java.io.Serializable;

public class TipoDependencia implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idTipoDependencia;
    private String descricao;

    public TipoDependencia() {}

    public Integer getIdTipoDependencia() { return idTipoDependencia; }
    public void setIdTipoDependencia(Integer idTipoDependencia) { this.idTipoDependencia = idTipoDependencia; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}