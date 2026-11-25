package unioeste.casestudy.irpf.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import unioeste.apoio.bd.ConexaoBD;
import unioeste.casestudy.irpf.bo.AuditorFiscal;
import unioeste.casestudy.irpf.bo.DeclaracaoRenda;
import unioeste.casestudy.irpf.col.AuditorFiscalCol;
import unioeste.casestudy.irpf.col.DeclaracaoCol;
import unioeste.casestudy.irpf.dao.AuditorFiscalDAO;
import unioeste.casestudy.irpf.dao.DeclaracaoDAO;

//Identificar auditor, listar triagem e aprovar/reter declaração
public class UCAuditorFiscalServicos {

    private final AuditorFiscalCol auditorFiscalCol = new AuditorFiscalCol();
    private final DeclaracaoCol declaracaoCol = new DeclaracaoCol();
    private final UCDeclaracaoServicos declaracaoServicos = new UCDeclaracaoServicos();

    public AuditorFiscal buscarAuditorPorCodigoOuCpf(String identificador) throws SQLException {
        if (!auditorFiscalCol.identificadorValido(identificador)) {
            return null;
        }
        String cpfLimpo = auditorFiscalCol.extrairCpfLimpo(identificador);
        try (Connection conn = ConexaoBD.getConexao()) {
            AuditorFiscalDAO dao = new AuditorFiscalDAO(conn);
            return dao.buscarPorCodigoOuCpf(identificador, cpfLimpo);
        }
    }

    public List<DeclaracaoRenda> listarDeclaracoesParaAuditoria() throws SQLException {
        try (Connection conn = ConexaoBD.getConexao()) {
            DeclaracaoDAO dao = new DeclaracaoDAO(conn);
            return dao.listarDeclaracoesParaAuditoria();
        }
    }

    public DeclaracaoRenda atualizarStatusAuditoria(int idDeclaracao, String novoStatus, int idAuditor)
            throws IRPFException, SQLException {
        declaracaoCol.validarParametrosAuditoria(idDeclaracao, novoStatus);
        auditorFiscalCol.validarIdAuditor(idAuditor);

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                AuditorFiscalDAO auditorDAO = new AuditorFiscalDAO(conn);
                if (!auditorDAO.existePorId(idAuditor)) {
                    throw new IRPFException("Auditor fiscal informado não está cadastrado.");
                }

                DeclaracaoDAO decDAO = new DeclaracaoDAO(conn);
                DeclaracaoRenda atual = decDAO.buscarCabecalhoPorId(idDeclaracao);
                declaracaoCol.validarTransicaoStatus(atual, novoStatus);

                decDAO.atualizarStatusAuditoria(idDeclaracao, novoStatus, idAuditor);
                conn.commit();

                return declaracaoServicos.consultarDeclaracao(atual.getContribuinte().getIdPessoa(), idDeclaracao);
            } catch (IRPFException | SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}