package unioeste.casestudy.irpf.col;

import java.util.List;

import unioeste.casestudy.irpf.manager.IRPFException;
import unioeste.geral.pessoa.bo.DDD;
import unioeste.geral.pessoa.bo.DDI;
import unioeste.geral.pessoa.bo.Email;
import unioeste.geral.pessoa.bo.Telefone;

//Limpeza de dígitos, validação de CPF/CNPJ, e-mails e telefones
public class PessoaCol {

    public String limparDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
    }

    public boolean validarCPF(String cpf) {
        return limparDigitos(cpf).length() == 11;
    }

    public boolean validarCNPJ(String cnpj) {
        return limparDigitos(cnpj).length() == 14;
    }

    public void validarEmails(List<Email> emails) throws IRPFException {
        if (emails == null) {
            return;
        }
        for (Email email : emails) {
            if (email != null && email.getEmail() != null && !email.getEmail().isBlank()) {
                if (!email.getEmail().trim().contains("@")) {
                    throw new IRPFException("E-mail inválido: " + email.getEmail());
                }
            }
        }
    }

    public void validarTelefones(List<Telefone> telefones) throws IRPFException {
        if (telefones == null) {
            return;
        }
        for (Telefone telefone : telefones) {
            if (telefone == null || telefone.getFone() == null
                    || telefone.getFone().isBlank() || telefone.getDdd() == null) {
                throw new IRPFException("Informe o número e o DDD do telefone.");
            }

            DDD ddd = telefone.getDdd();
            DDI ddi = telefone.getDdi();
            DDI ddiDoDdd = ddd.getDdi();

            Integer idDdi = ddi == null ? null : ddi.getIdDDI();
            if (idDdi == null && ddiDoDdd != null) {
                idDdi = ddiDoDdd.getIdDDI();
            }

            boolean possuiIds = ddd.getIdDDD() != null && ddd.getIdDDD() > 0
                    && idDdi != null && idDdi > 0;
            if (possuiIds) {
                continue;
            }

            String codigoDdd = ddd.getDdd();
            String codigoDdi = ddi == null ? null : ddi.getDdi();
            if ((codigoDdi == null || codigoDdi.isBlank()) && ddiDoDdd != null) {
                codigoDdi = ddiDoDdd.getDdi();
            }

            if (codigoDdd == null || codigoDdd.isBlank()
                    || codigoDdi == null || codigoDdi.isBlank()) {
                throw new IRPFException("Informe os códigos de DDD e DDI.");
            }
        }
    }
}