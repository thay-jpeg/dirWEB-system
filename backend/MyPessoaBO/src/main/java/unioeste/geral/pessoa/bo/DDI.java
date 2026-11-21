package unioeste.geral.pessoa.bo;

import java.io.Serializable;

public class DDI implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idDDI;
    private String ddi;

    public DDI() {}

    public Integer getIdDDI() { return idDDI; }
    public void setIdDDI(Integer idDDI) { this.idDDI = idDDI; }

    public String getDdi() { return ddi; }
    public void setDdi(String ddi) { this.ddi = ddi; }
}