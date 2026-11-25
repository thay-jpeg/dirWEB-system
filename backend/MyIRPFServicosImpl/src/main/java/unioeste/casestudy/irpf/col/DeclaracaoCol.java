package unioeste.casestudy.irpf.col;

import java.util.List;

import unioeste.casestudy.irpf.bo.BemDireito;
import unioeste.casestudy.irpf.bo.DeclaracaoRenda;
import unioeste.casestudy.irpf.bo.RendimentoPJ;
import unioeste.casestudy.irpf.manager.IRPFException;

//Validação de declaração, rendimentos, bens e transição de status da auditoria
public class DeclaracaoCol {

    private final PessoaCol pessoaCol = new PessoaCol();

    public void validarDeclaracaoCompleta(DeclaracaoRenda declaracao) throws IRPFException {
        if (declaracao == null || declaracao.getContribuinte() == null) {
            throw new IRPFException("Dados da declaração e contribuinte são obrigatórios.");
        }
        if (declaracao.getAnoExercicio() == null) {
            declaracao.setAnoExercicio(2026);
        }
        if (declaracao.getStatus() == null || declaracao.getStatus().isBlank()) {
            declaracao.setStatus("Enviada");
        }
        if (declaracao.getRendimentos() != null && !declaracao.getRendimentos().isEmpty()) {
            validarListaRendimentos(declaracao.getRendimentos());
        }
        if (declaracao.getBensDireitos() != null && !declaracao.getBensDireitos().isEmpty()) {
            validarListaBens(declaracao.getBensDireitos());
        }
    }

    public void validarListaRendimentos(List<RendimentoPJ> rendimentos) throws IRPFException {
        if (rendimentos == null || rendimentos.isEmpty()) {
            return;
        }
        double somaTotal = 0.0;
        for (RendimentoPJ r : rendimentos) {
            if (r.getFontePagadora() == null || !pessoaCol.validarCNPJ(r.getFontePagadora().getCnpj())) {
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

    public Integer validarRegistroBensAvulso(List<BemDireito> bens) throws IRPFException {
        validarListaBens(bens);
        Integer idContribuinte = bens.get(0).getIdContribuinte();
        if (idContribuinte == null || idContribuinte <= 0) {
            throw new IRPFException("idContribuinte é obrigatório para registrar bens.");
        }
        return idContribuinte;
    }

    public void validarParametrosAuditoria(int idDeclaracao, String novoStatus) throws IRPFException {
        if (idDeclaracao <= 0) {
            throw new IRPFException("ID da declaração inválido.");
        }
        if (!"Processada".equals(novoStatus) && !"Retida".equals(novoStatus)) {
            throw new IRPFException("Status inválido. Informe 'Processada' (aprovar) ou 'Retida' (malha fina).");
        }
    }

    public void validarTransicaoStatus(DeclaracaoRenda atual, String novoStatus) throws IRPFException {
        if (atual == null) {
            throw new IRPFException("Declaração não encontrada.");
        }
        if ("Em Preenchimento".equals(atual.getStatus())) {
            throw new IRPFException("Não é possível auditar uma declaração ainda em preenchimento.");
        }
        if ("Processada".equals(atual.getStatus())) {
            throw new IRPFException("Esta declaração já foi processada e não pode mais ter o status alterado.");
        }
        if (novoStatus.equals(atual.getStatus())) {
            throw new IRPFException("A declaração já se encontra com o status '" + novoStatus + "'.");
        }
    }
}