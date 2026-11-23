package unioeste.geral.endereco.dao;

import unioeste.geral.endereco.bo.UF;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class UFDAO {
    private final Connection conexao;

    public UFDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public UF selecionarPorSigla(String sigla) throws SQLException {
        if (sigla == null || sigla.isBlank()) {
            return null;
        }

        String sql = "SELECT idUF, UF FROM UF WHERE UF = ?";

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, sigla.trim().toUpperCase(Locale.ROOT));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new UF(rs.getInt("idUF"), rs.getString("UF"));
                }
            }
        }

        return null;
    }
}