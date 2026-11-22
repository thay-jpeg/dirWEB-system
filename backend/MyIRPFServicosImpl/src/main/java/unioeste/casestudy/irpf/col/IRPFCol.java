package unioeste.casestudy.irpf.col;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

import unioeste.casestudy.irpf.bo.BemDireito;
import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.bo.Dependente;
import unioeste.casestudy.irpf.bo.RendimentoPJ;
import unioeste.casestudy.irpf.bo.UsuarioAcesso;
import unioeste.casestudy.irpf.manager.IRPFException;
import unioeste.geral.pessoa.bo.PessoaJuridica;

public class IRPFCol {

    public String limparDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("[^0-9]", "");
    }

    public boolean validarCPF(String cpf) {
        return limparDigitos(cpf).length() == 11;
    }

    public boolean validarCNPJ(String cnpj) {
        return limparDigitos(cnpj).length() == 14;
    }

    public String gerarHashSenha(String senhaPura) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(senhaPura.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar hash da senha.", e);
        }
    }

    public void validarCadastroUsuario(UsuarioAcesso usuario) throws IRPFException {
        if (usuario == null || usuario.getContribuinte() == null) {
            throw new IRPFException("Dados do usuário e contribuinte são obrigatórios.");
        }
        String cpf = usuario.getLoginCpf() != null ? usuario.getLoginCpf() : usuario.getContribuinte().getCpf();
        if (!validarCPF(cpf)) {
            throw new IRPFException("CPF inválido. Informe 11 dígitos numéricos.");
        }
        if (usuario.getContribuinte().getNome() == null || usuario.getContribuinte().getNome().isBlank()) {
            throw new IRPFException("Nome completo do contribuinte é obrigatório.");
        }
        if (usuario.getSenhaHash() == null || usuario.getSenhaHash().length() < 6) {
            throw new IRPFException("A senha deve conter no mínimo 6 caracteres.");
        }
    }

    public void validarAtualizacaoContribuinte(Contribuinte c) throws IRPFException {
        if (c == null) {
            throw new IRPFException("Objeto Contribuinte não pode ser nulo.");
        }
        boolean temId = c.getIdPessoa() != null && c.getIdPessoa() > 0;
        boolean temCpf = validarCPF(c.getCpf());
        if (!temId && !temCpf) {
            throw new IRPFException("Informe o idPessoa ou um CPF válido para atualizar o contribuinte.");
        }
        if (c.getNome() == null || c.getNome().isBlank()) {
            throw new IRPFException("O nome do contribuinte é obrigatório.");
        }
    }

    public void validarDependente(Dependente dep) throws IRPFException {
        if (dep == null) {
            throw new IRPFException("Dados do dependente não informados.");
        }
        if (dep.getNome() == null || dep.getNome().isBlank()) {
            throw new IRPFException("Nome do dependente é obrigatório.");
        }
        if (!validarCPF(dep.getCpf())) {
            throw new IRPFException("CPF do dependente inválido.");
        }
        if (dep.getTipoDependencia() == null || dep.getTipoDependencia().getIdTipoDependencia() == null) {
            throw new IRPFException("Tipo de dependência é obrigatório.");
        }
    }

    public void validarPessoaJuridica(PessoaJuridica pj) throws IRPFException {
        if (pj == null) {
            throw new IRPFException("Dados da Pessoa Jurídica não informados.");
        }
        if (pj.getNome() == null || pj.getNome().isBlank()) {
            throw new IRPFException("Nome/Razão Social da empresa é obrigatório.");
        }
        if (!validarCNPJ(pj.getCnpj())) {
            throw new IRPFException("CNPJ inválido. Informe 14 dígitos numéricos.");
        }
    }

    public void validarListaRendimentos(List<RendimentoPJ> rendimentos) throws IRPFException {
        if (rendimentos == null || rendimentos.isEmpty()) {
            return;
        }
        double somaTotal = 0.0;
        for (RendimentoPJ r : rendimentos) {
            if (r.getFontePagadora() == null || !validarCNPJ(r.getFontePagadora().getCnpj())) {
                throw new IRPFException("CNPJ válido da fonte pagadora é obrigatório em todos os rendimentos.");
            }
            if (r.getValorTotalRecebido() == null || r.getValor13Salario() == null
                    || r.getIrrfPago() == null || r.getInss() == null) {
                throw new IRPFException("Todos os valores do rendimento (Total, 13º, IRRF e INSS) devem ser informados.");
            }
            if (r.getValorTotalRecebido() < 0 || r.getValor13Salario() < 0
                    || r.getIrrfPago() < 0 || r.getInss() < 0) {
                throw new IRPFException("Rendimentos não aceitam valores negativos.");
            }
            somaTotal += r.getValorTotalRecebido();
        }
        if (somaTotal <= 100.00) {
            throw new IRPFException("O valor total de rendimentos deve ser maior que R$ 100,00.");
        }
    }

    public void validarListaBens(List<BemDireito> bens) throws IRPFException {
        if (bens == null || bens.isEmpty()) {
            throw new IRPFException("Informe pelo menos um bem para registro.");
        }
        double somaAtual = 0.0;
        for (BemDireito b : bens) {
            if (b.getNomeBem() == null || b.getNomeBem().isBlank()) {
                throw new IRPFException("Nome do bem é obrigatório.");
            }
            if (b.getTipoBem() == null || b.getTipoBem().getDescricao() == null
                    || b.getTipoBem().getDescricao().isBlank()) {
                throw new IRPFException("Tipo do bem (descrição) é obrigatório.");
            }
            if (b.getValorAnoAnterior() == null || b.getValorAtual() == null) {
                throw new IRPFException("Valores do ano anterior e de 31/12/2026 são obrigatórios.");
            }
            if (b.getValorAnoAnterior() < 0 || b.getValorAtual() < 0) {
                throw new IRPFException("Bens e Direitos não aceitam valores negativos.");
            }
            somaAtual += b.getValorAtual();
        }
        if (somaAtual <= 100.00) {
            throw new IRPFException("O valor total dos bens informados deve ser maior que R$ 100,00.");
        }
    }
}