package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import unioeste.casestudy.irpf.bo.Dependente;

public class DependenteDAO {
    private final Connection conexao;

    public DependenteDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public boolean existeTipoDependencia(int idTipo) throws SQLException {
        String sql = "SELECT 1 FROM Tipo_dependente WHERE idTipo_dependente = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idTipo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public Dependente inserir(Dependente dep, int idContribuinte) throws SQLException {
        String sql = "INSERT INTO Dependente (Nome, CPF, Contribuinte_Pessoa_idPessoa, Tipo_dependente_idTipo_dependente) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, dep.getNome());
            stmt.setString(2, dep.getCpf());
            stmt.setInt(3, idContribuinte);
            stmt.setInt(4, dep.getTipoDependencia().getIdTipoDependencia());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) dep.setIdDependente(keys.getInt(1));
            }
        }
        return dep;
    }
}