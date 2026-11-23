package unioeste.geral.endereco.dao;

import unioeste.geral.endereco.bo.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class CidadeDAO {
    private final Connection conexao;

    public CidadeDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public Cidade selecionarPorId(int idCidade) throws SQLException {
        String sql = "SELECT c.idCidade, c.Cidade, u.idUF, u.UF " +
                "FROM Cidade c " +
                "INNER JOIN UF u ON c.UF_idUF = u.idUF " +
                "WHERE c.idCidade = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idCidade);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UF uf = new UF(rs.getInt("idUF"), rs.getString("UF"));
                    return new Cidade(rs.getInt("idCidade"), rs.getString("Cidade"), uf);
                }
            }
        }
        return null;
    }


    public Cidade selecionarPorNomeEUf(String nomeCidade, int idUF) throws SQLException {
        String sql = """
            SELECT c.idCidade, c.Cidade, u.idUF, u.UF
            FROM Cidade c
            INNER JOIN UF u ON c.UF_idUF = u.idUF
            WHERE c.Cidade = ? AND c.UF_idUF = ?
            """;

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, nomeCidade);
            stmt.setInt(2, idUF);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    UF uf = new UF(rs.getInt("idUF"), rs.getString("UF"));
                    return new Cidade(rs.getInt("idCidade"), rs.getString("Cidade"), uf);
                }
            }
        }

        return null;
    }

    public Cidade inserir(String nomeCidade, UF uf) throws SQLException {
        String sql = "INSERT INTO Cidade (Cidade, UF_idUF) VALUES (?, ?)";

        try (PreparedStatement stmt =
                     conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, nomeCidade);
            stmt.setInt(2, uf.getIdUF());
            stmt.executeUpdate();

            try (ResultSet chaves = stmt.getGeneratedKeys()) {
                if (chaves.next()) {
                    return new Cidade(chaves.getInt(1), nomeCidade, uf);
                }
            }
        }

        throw new SQLException("O banco não retornou o ID da cidade cadastrada.");
    }
}