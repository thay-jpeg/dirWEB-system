package unioeste.casestudy.irpf.manager;

import java.sql.Connection;
import java.sql.SQLException;

import unioeste.apoio.bd.ConexaoBD;
import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.col.ContribuinteCol;
import unioeste.casestudy.irpf.dao.ContribuinteDAO;
import unioeste.casestudy.irpf.dao.PessoaDAO;

//Cadastrar, atualizar dados pessoais e consultar contribuinte por CPF
public class UCContribuinteServicos {

    private final ContribuinteCol contribuinteCol = new ContribuinteCol();
    private final IRPFServicoApoio apoio = new IRPFServicoApoio();

    public Contribuinte cadastrarContribuinte(Contribuinte contrib) throws IRPFException, Exception {
        contribuinteCol.validarCadastro(contrib);
        contribuinteCol.normalizarCadastro(contrib);

        if (contrib.getEndereco() != null) {
            contrib.setEndereco(apoio.resolverEndereco(contrib.getEndereco()));
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                if (contribDAO.buscarIdPorCpf(contrib.getCpf()) != null) {
                    throw new IRPFException("Já existe um contribuinte cadastrado com este CPF.");
                }

                PessoaDAO pessoaDAO = new PessoaDAO(conn);
                apoio.resolverTelefonesPorCodigo(contrib.getTelefones(), pessoaDAO);

                Integer idPessoaExistente = contribDAO.buscarIdPessoaFisicaPorCpf(contrib.getCpf());
                if (idPessoaExistente != null) {
                    contrib.setIdPessoa(idPessoaExistente);
                } else {
                    pessoaDAO.inserirPessoa(contrib);
                    contribDAO.inserirPessoaFisica(contrib);
                }

                contribDAO.inserirContribuinte(contrib.getIdPessoa());
                pessoaDAO.substituirEmails(contrib.getIdPessoa(), contrib.getEmails());
                pessoaDAO.substituirTelefones(contrib.getIdPessoa(), contrib.getTelefones());

                conn.commit();
                return contrib;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao cadastrar contribuinte: " + e.getMessage(), e);
            }
        }
    }

    public Contribuinte atualizarDadosContribuinte(Contribuinte contrib) throws IRPFException, Exception {
        contribuinteCol.validarAtualizacao(contrib);

        if (contrib.getEndereco() != null) {
            contrib.setEndereco(apoio.resolverEndereco(contrib.getEndereco()));
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                Integer idPessoa = apoio.resolverIdContribuinte(contribDAO, contrib);
                contrib.setIdPessoa(idPessoa);

                PessoaDAO pessoaDAO = new PessoaDAO(conn);
                apoio.resolverTelefonesPorCodigo(contrib.getTelefones(), pessoaDAO);

                pessoaDAO.atualizarPessoa(contrib);
                contribDAO.atualizarPessoaFisica(contrib);
                pessoaDAO.substituirEmails(idPessoa, contrib.getEmails());
                pessoaDAO.substituirTelefones(idPessoa, contrib.getTelefones());

                conn.commit();
                return contrib;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao atualizar dados do contribuinte: " + e.getMessage(), e);
            }
        }
    }

    public Contribuinte consultarContribuinte(String cpf) throws IRPFException, SQLException {
        String cpfLimpo = contribuinteCol.validarELimparCpfConsulta(cpf);

        try (Connection conn = ConexaoBD.getConexao()) {
            ContribuinteDAO dao = new ContribuinteDAO(conn);
            Integer idPessoa = dao.buscarIdPorCpf(cpfLimpo);

            if (idPessoa == null) {
                throw new IRPFException("Contribuinte não encontrado.");
            }

            return apoio.carregarContribuinte(conn, idPessoa);
        }
    }
}