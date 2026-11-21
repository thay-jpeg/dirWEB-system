package unioeste.geral.pessoa.bo;

import java.io.Serializable;

public class Email implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer idEmail;
    private String email;

    public Email() {}

    public Integer getIdEmail() { return idEmail; }
    public void setIdEmail(Integer idEmail) { this.idEmail = idEmail; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}