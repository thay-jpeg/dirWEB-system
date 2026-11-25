package unioeste.casestudy.irpf.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import unioeste.apoio.bd.ConexaoBD;
import unioeste.casestudy.irpf.bo.Dependente;
import unioeste.casestudy.irpf.bo.TipoDependencia;
import unioeste.casestudy.irpf.col.ContribuinteCol;
import unioeste.casestudy.irpf.col.DependenteCol;
import unioeste.casestudy.irpf.dao.ContribuinteDAO;
import unioeste.casestudy.irpf.dao.DependenteDAO;

//Cadastrar, excluir, listar dependentes e listar tipos de dependência
public class UCDependenteServicos {

    private final DependenteCol dependenteCol = new DependenteCol();
    private final ContribuinteCol contribuinteCol = new ContribuinteCol();
    private final IRPFServicoApoio apoio = new IRPFServicoApoio();

    public Dependente cadastrarDependente(Dependente dependente) throws IRPFException, Exception {
        dependenteCol.validarCadastro(dependente);
        dependenteCol.normalizarCpf(dependente);

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                Integer idContribuinte = apoio.resolverIdContribuinte(contribDAO, dependente.getContribuinte());

                DependenteDAO depDAO = new DependenteDAO(conn);
                if (!depDAO.existeTipoDependencia(dependente.getTipoDependencia().getIdTipoDependencia())) {
                    throw new IRPFException("Tipo de dependência informado não existe no banco.");
                }

                depDAO.inserir(dependente, idContribuinte);
                dependente.getContribuinte().setIdPessoa(idContribuinte);

                conn.commit();
                return dependente;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao cadastrar dependente: " + e.getMessage(), e);
            }
        }
    }

    public void excluirDependente(int idContribuinte, int idDependente) throws IRPFException, SQLException {
        dependenteCol.validarExclusao(idContribuinte, idDependente);

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                if (!contribDAO.existePorId(idContribuinte)) {
                    throw new IRPFException("Contribuinte não encontrado.");
                }

                DependenteDAO depDAO = new DependenteDAO(conn);
                if (!depDAO.excluir(idDependente, idContribuinte)) {
                    throw new IRPFException("Dependente não encontrado para este contribuinte.");
                }

                conn.commit();
            } catch (IRPFException | SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    public List<TipoDependencia> listarTiposDependencia() throws SQLException {
        try (Connection conn = ConexaoBD.getConexao()) {
            DependenteDAO dependenteDAO = new DependenteDAO(conn);
            return dependenteDAO.listarTiposDependencia();
        }
    }

    public List<Dependente> listarDependentes(int idPessoa) throws IRPFException, SQLException {
        contribuinteCol.validarId(idPessoa);

        try (Connection conn = ConexaoBD.getConexao()) {
            ContribuinteDAO contribuinteDAO = new ContribuinteDAO(conn);
            if (!contribuinteDAO.existePorId(idPessoa)) {
                throw new IRPFException("Contribuinte não encontrado.");
            }

            DependenteDAO dependenteDAO = new DependenteDAO(conn);
            return dependenteDAO.listarPorContribuinte(idPessoa);
        }
    }
}