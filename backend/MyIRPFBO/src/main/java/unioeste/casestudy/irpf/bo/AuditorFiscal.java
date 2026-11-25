package unioeste.casestudy.irpf.bo;

import unioeste.geral.pessoa.bo.PessoaFisica;

public class AuditorFiscal extends PessoaFisica {
    private static final long serialVersionUID = 1L;

    private String codAuditor;

    public AuditorFiscal() {}

    public String getCodAuditor() { return codAuditor; }
    public void setCodAuditor(String codAuditor) { this.codAuditor = codAuditor; }
}