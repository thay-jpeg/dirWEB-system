package unioeste.geral.pessoa.bo;

import java.io.Serializable;

public class Telefone implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idTelefone;
    private String fone;
    private DDD ddd;
    private DDI ddi;

    public Telefone() {}

    public Integer getIdTelefone() { return idTelefone; }
    public void setIdTelefone(Integer idTelefone) { this.idTelefone = idTelefone; }

    public String getFone() { return fone; }
    public void setFone(String fone) { this.fone = fone; }

    public DDD getDdd() { return ddd; }
    public void setDdd(DDD ddd) { this.ddd = ddd; }

    public DDI getDdi() { return ddi; }
    public void setDdi(DDI ddi) { this.ddi = ddi; }
}