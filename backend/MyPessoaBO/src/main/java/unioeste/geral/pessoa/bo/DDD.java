package unioeste.geral.pessoa.bo;

import java.io.Serializable;

public class DDD implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idDDD;
    private String ddd;
    private DDI ddi;

    public DDD() {}

    public Integer getIdDDD() { return idDDD; }
    public void setIdDDD(Integer idDDD) { this.idDDD = idDDD; }

    public String getDdd() { return ddd; }
    public void setDdd(String ddd) { this.ddd = ddd; }

    public DDI getDdi() { return ddi; }
    public void setDdi(DDI ddi) { this.ddi = ddi; }
}