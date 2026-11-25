package unioeste.casestudy.irpf.bo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class DeclaracaoRenda implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idDeclaracao;
    private Integer anoExercicio;
    private String status; // "Em Preenchimento", "Enviada", "Processada", "Retida em Malha Fina"
    private Contribuinte contribuinte;
    private AuditorFiscal auditorFiscal;
    private List<Dependente> dependentes = new ArrayList<>();
    private List<RendimentoPJ> rendimentos = new ArrayList<>();
    private List<BemDireito> bensDireitos = new ArrayList<>();

    public DeclaracaoRenda() {}

    public Integer getIdDeclaracao() { return idDeclaracao; }
    public void setIdDeclaracao(Integer idDeclaracao) { this.idDeclaracao = idDeclaracao; }

    public Integer getAnoExercicio() { return anoExercicio; }
    public void setAnoExercicio(Integer anoExercicio) { this.anoExercicio = anoExercicio; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Contribuinte getContribuinte() { return contribuinte; }
    public void setContribuinte(Contribuinte contribuinte) { this.contribuinte = contribuinte; }

    public AuditorFiscal getAuditorFiscal() { return auditorFiscal; }
    public void setAuditorFiscal(AuditorFiscal auditorFiscal) { this.auditorFiscal = auditorFiscal; }

    public List<Dependente> getDependentes() { return dependentes; }
    public void setDependentes(List<Dependente> dependentes) { this.dependentes = dependentes; }

    public List<RendimentoPJ> getRendimentos() { return rendimentos; }
    public void setRendimentos(List<RendimentoPJ> rendimentos) { this.rendimentos = rendimentos; }

    public List<BemDireito> getBensDireitos() { return bensDireitos; }
    public void setBensDireitos(List<BemDireito> bensDireitos) { this.bensDireitos = bensDireitos; }
}