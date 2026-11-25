package unioeste.casestudy.irpf.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import unioeste.apoio.bd.ConexaoBD;
import unioeste.casestudy.irpf.bo.BemDireito;
import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.bo.DeclaracaoRenda;
import unioeste.casestudy.irpf.bo.Dependente;
import unioeste.casestudy.irpf.bo.RendimentoPJ;
import unioeste.casestudy.irpf.col.ContribuinteCol;
import unioeste.casestudy.irpf.col.DeclaracaoCol;
import unioeste.casestudy.irpf.col.DependenteCol;
import unioeste.casestudy.irpf.col.PessoaCol;
import unioeste.casestudy.irpf.dao.ContribuinteDAO;
import unioeste.casestudy.irpf.dao.DeclaracaoDAO;
import unioeste.casestudy.irpf.dao.DependenteDAO;
import unioeste.casestudy.irpf.dao.PessoaJuridicaDAO;

//Registrar bens, listar bens, enviar declaração completa e consultar declaração
public class UCDeclaracaoServicos {

    private final PessoaCol pessoaCol = new PessoaCol();
    private final ContribuinteCol contribuinteCol = new ContribuinteCol();
    private final DependenteCol dependenteCol = new DependenteCol();
    private final DeclaracaoCol declaracaoCol = new DeclaracaoCol();
    private final IRPFServicoApoio apoio = new IRPFServicoApoio();

    public List<BemDireito> registrarBens(List<BemDireito> bens) throws IRPFException, Exception {
        Integer idContribuinte = declaracaoCol.validarRegistroBensAvulso(bens);

        for (BemDireito bem : bens) {
            if (bem.getEndereco() != null) {
                bem.setEndereco(apoio.resolverEndereco(bem.getEndereco()));
            }
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                if (!contribDAO.existePorId(idContribuinte)) {
                    throw new IRPFException("Contribuinte informado não encontrado.");
                }

                DeclaracaoDAO decDAO = new DeclaracaoDAO(conn);
                Integer idDeclaracao = bens.get(0).getIdDeclaracao();
                if (idDeclaracao == null || idDeclaracao <= 0) {
                    idDeclaracao = decDAO.buscarIdDeclaracaoAtiva(idContribuinte, 2026);
                    if (idDeclaracao == null) {
                        DeclaracaoRenda rascunho = new DeclaracaoRenda();
                        Contribuinte c = new Contribuinte();
                        c.setIdPessoa(idContribuinte);
                        rascunho.setContribuinte(c);
                        rascunho.setAnoExercicio(2026);
                        rascunho.setStatus("Em Preenchimento");
                        idDeclaracao = decDAO.inserirDeclaracao(rascunho);
                    }
                }

                for (BemDireito bem : bens) {
                    decDAO.inserirBemComTipo(bem, idContribuinte, idDeclaracao);
                }

                conn.commit();
                return bens;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao registrar bens: " + e.getMessage(), e);
            }
        }
    }

    public List<BemDireito> listarBens(int idPessoa, int idDeclaracao) throws IRPFException, SQLException {
        try (Connection conn = ConexaoBD.getConexao()) {
            DeclaracaoDAO dao = new DeclaracaoDAO(conn);
            carregarCabecalho(dao, idPessoa, idDeclaracao);
            return dao.listarBens(idPessoa, idDeclaracao);
        }
    }

    public DeclaracaoRenda consultarDeclaracao(int idPessoa, int idDeclaracao) throws IRPFException, SQLException {
        try (Connection conn = ConexaoBD.getConexao()) {
            DeclaracaoDAO declaracaoDAO = new DeclaracaoDAO(conn);
            DependenteDAO dependenteDAO = new DependenteDAO(conn);

            DeclaracaoRenda declaracao = carregarCabecalho(declaracaoDAO, idPessoa, idDeclaracao);
            declaracao.setContribuinte(apoio.carregarContribuinte(conn, idPessoa));
            declaracao.setDependentes(dependenteDAO.listarPorContribuinte(idPessoa));
            declaracao.setBensDireitos(declaracaoDAO.listarBens(idPessoa, idDeclaracao));
            declaracao.setRendimentos(declaracaoDAO.listarRendimentos(idDeclaracao));

            return declaracao;
        }
    }

    public DeclaracaoRenda consultarUltimaDeclaracao(int idPessoa, int anoExercicio)
            throws IRPFException, SQLException {
        contribuinteCol.validarId(idPessoa);

        try (Connection conn = ConexaoBD.getConexao()) {
            ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
            if (!contribDAO.existePorId(idPessoa)) {
                throw new IRPFException("Contribuinte não encontrado.");
            }

            DeclaracaoDAO decDAO = new DeclaracaoDAO(conn);
            Integer idDeclaracao = decDAO.buscarIdDeclaracaoAtiva(idPessoa, anoExercicio);
            if (idDeclaracao == null) {
                return null;
            }

            DependenteDAO dependenteDAO = new DependenteDAO(conn);
            DeclaracaoRenda declaracao = carregarCabecalho(decDAO, idPessoa, idDeclaracao);
            declaracao.setContribuinte(apoio.carregarContribuinte(conn, idPessoa));
            declaracao.setDependentes(dependenteDAO.listarPorContribuinte(idPessoa));
            declaracao.setBensDireitos(decDAO.listarBens(idPessoa, idDeclaracao));
            declaracao.setRendimentos(decDAO.listarRendimentos(idDeclaracao));
            return declaracao;
        }
    }

    public DeclaracaoRenda cadastrarDeclaracaoRenda(DeclaracaoRenda declaracao) throws IRPFException, Exception {
        declaracaoCol.validarDeclaracaoCompleta(declaracao);

        if (declaracao.getBensDireitos() != null) {
            for (BemDireito bem : declaracao.getBensDireitos()) {
                if (bem.getEndereco() != null) {
                    bem.setEndereco(apoio.resolverEndereco(bem.getEndereco()));
                }
            }
        }
        if (declaracao.getDependentes() != null) {
            for (Dependente dep : declaracao.getDependentes()) {
                dependenteCol.validarDadosBasicos(dep);
                dependenteCol.normalizarCpf(dep);
            }
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                Integer idContribuinte = apoio.resolverIdContribuinte(contribDAO, declaracao.getContribuinte());
                declaracao.getContribuinte().setIdPessoa(idContribuinte);

                DeclaracaoDAO decDAO = new DeclaracaoDAO(conn);
                int idDeclaracao = decDAO.inserirDeclaracao(declaracao);

                if (declaracao.getDependentes() != null && !declaracao.getDependentes().isEmpty()) {
                    DependenteDAO depDAO = new DependenteDAO(conn);
                    for (Dependente dep : declaracao.getDependentes()) {
                        if (dep.getIdDependente() == null) {
                            if (!depDAO.existeTipoDependencia(dep.getTipoDependencia().getIdTipoDependencia())) {
                                throw new IRPFException("Tipo de dependência inválido no dependente: " + dep.getNome());
                            }
                            depDAO.inserir(dep, idContribuinte);
                        }
                    }
                }

                if (declaracao.getRendimentos() != null && !declaracao.getRendimentos().isEmpty()) {
                    PessoaJuridicaDAO pjDAO = new PessoaJuridicaDAO(conn);
                    for (RendimentoPJ rend : declaracao.getRendimentos()) {
                        String cnpj = pessoaCol.limparDigitos(rend.getFontePagadora().getCnpj());
                        Integer idPj = pjDAO.buscarIdPorCnpj(cnpj);
                        if (idPj == null) {
                            throw new IRPFException("Fonte pagadora com CNPJ " + cnpj + " não está cadastrada no sistema.");
                        }
                        rend.getFontePagadora().setIdPessoa(idPj);
                        decDAO.inserirRendimento(rend, idDeclaracao, idPj);
                    }
                }

                if (declaracao.getBensDireitos() != null && !declaracao.getBensDireitos().isEmpty()) {
                    for (BemDireito bem : declaracao.getBensDireitos()) {
                        decDAO.inserirBemComTipo(bem, idContribuinte, idDeclaracao);
                    }
                }

                conn.commit();
                return declaracao;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao cadastrar declaração: " + e.getMessage(), e);
            }
        }
    }

    private DeclaracaoRenda carregarCabecalho(DeclaracaoDAO dao, int idPessoa, int idDeclaracao)
            throws IRPFException, SQLException {
        DeclaracaoRenda declaracao = dao.buscarCabecalho(idPessoa, idDeclaracao);
        if (declaracao == null) {
            throw new IRPFException("Declaração não encontrada para esse contribuinte.");
        }
        return declaracao;
    }
}