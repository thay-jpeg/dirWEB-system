package unioeste.casestudy.irpf.bo;

import java.io.Serializable;

public class UsuarioAcesso implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idUsuarioAcesso;
    private String loginCpf;
    private String senhaHash;
    private String perfilAcesso; // "contribuinte" ou "auditorFiscal"
    private Contribuinte contribuinte;

    public UsuarioAcesso() {}

    public Integer getIdUsuarioAcesso() { return idUsuarioAcesso; }
    public void setIdUsuarioAcesso(Integer idUsuarioAcesso) { this.idUsuarioAcesso = idUsuarioAcesso; }

    public String getLoginCpf() { return loginCpf; }
    public void setLoginCpf(String loginCpf) { this.loginCpf = loginCpf; }

    public String getSenhaHash() { return senhaHash; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }

    public String getPerfilAcesso() { return perfilAcesso; }
    public void setPerfilAcesso(String perfilAcesso) { this.perfilAcesso = perfilAcesso; }

    public Contribuinte getContribuinte() { return contribuinte; }
    public void setContribuinte(Contribuinte contribuinte) { this.contribuinte = contribuinte; }
}