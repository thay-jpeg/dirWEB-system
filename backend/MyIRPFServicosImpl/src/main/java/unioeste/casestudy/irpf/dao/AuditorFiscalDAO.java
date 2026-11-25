package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import unioeste.casestudy.irpf.bo.AuditorFiscal;

public class AuditorFiscalDAO {
    private final Connection conexao;

    public AuditorFiscalDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public AuditorFiscal buscarPorCodigoOuCpf(String codigoOriginal, String cpfLimpo) throws SQLException {
        String sql = "SELECT p.idPessoa, p.Nome, pf.Sobrenome, pf.Nome_Social, pf.CPF, pf.RG, pf.Sexo, a.codAuditor " +
                     "FROM Auditor_Fiscal a " +
                     "INNER JOIN Pessoa_Fisica pf ON a.Pessoa_idPessoa = pf.Pessoa_idPessoa " +
                     "INNER JOIN Pessoa p ON pf.Pessoa_idPessoa = p.idPessoa " +
                     "WHERE UPPER(a.codAuditor) = UPPER(?) OR pf.CPF = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, codigoOriginal.trim());
            stmt.setString(2, cpfLimpo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    AuditorFiscal auditor = new AuditorFiscal();
                    auditor.setIdPessoa(rs.getInt("idPessoa"));
                    auditor.setNome(rs.getString("Nome"));
                    auditor.setSobrenome(rs.getString("Sobrenome"));
                    auditor.setNomeSocial(rs.getString("Nome_Social"));
                    auditor.setCpf(rs.getString("CPF"));
                    auditor.setRg(rs.getString("RG"));
                    auditor.setSexo(rs.getString("Sexo"));
                    auditor.setCodAuditor(rs.getString("codAuditor"));
                    return auditor;
                }
            }
        }
        return null;
    }

    public boolean existePorId(int idPessoa) throws SQLException {
    String sql = "SELECT 1 FROM Auditor_Fiscal WHERE Pessoa_idPessoa = ?";
    try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
        stmt.setInt(1, idPessoa);
        try (ResultSet rs = stmt.executeQuery()) {
            return rs.next();
        }
    }
}
}