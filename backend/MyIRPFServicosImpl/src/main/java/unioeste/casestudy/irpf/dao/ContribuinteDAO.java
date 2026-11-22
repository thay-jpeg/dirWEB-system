package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.bo.UsuarioAcesso;

public class ContribuinteDAO {
    private final Connection conexao;

    public ContribuinteDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public void inserirContribuinte(Contribuinte c) throws SQLException {
        String sql = "INSERT INTO Contribuinte (Pessoa_idPessoa, Sobrenome, Nome_Social, CPF, RG, Sexo) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, c.getIdPessoa());
            stmt.setString(2, c.getSobrenome());
            stmt.setString(3, c.getNomeSocial());
            stmt.setString(4, c.getCpf());
            stmt.setString(5, c.getRg());
            stmt.setString(6, c.getSexo());
            stmt.executeUpdate();
        }
    }

    public void atualizarContribuinte(Contribuinte c) throws SQLException {
        String sql = "UPDATE Contribuinte SET Sobrenome = ?, Nome_Social = ?, RG = ?, Sexo = ? " +
                     "WHERE Pessoa_idPessoa = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, c.getSobrenome());
            stmt.setString(2, c.getNomeSocial());
            stmt.setString(3, c.getRg());
            stmt.setString(4, c.getSexo());
            stmt.setInt(5, c.getIdPessoa());
            stmt.executeUpdate();
        }
    }

    public void inserirUsuarioAcesso(UsuarioAcesso usuario) throws SQLException {
        String sql = "INSERT INTO UsuarioAcesso (loginCpf, senhaHash, perfilAcesso, Contribuinte_Pessoa_idPessoa) " +
                     "VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, usuario.getLoginCpf());
            stmt.setString(2, usuario.getSenhaHash());
            stmt.setString(3, usuario.getPerfilAcesso());
            stmt.setInt(4, usuario.getContribuinte().getIdPessoa());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) usuario.setIdUsuarioAcesso(keys.getInt(1));
            }
        }
    }

    public Integer buscarIdPorCpf(String cpfLimpo) throws SQLException {
        String sql = "SELECT Pessoa_idPessoa FROM Contribuinte WHERE CPF = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cpfLimpo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("Pessoa_idPessoa") : null;
            }
        }
    }

    public boolean existePorId(int idPessoa) throws SQLException {
        String sql = "SELECT 1 FROM Contribuinte WHERE Pessoa_idPessoa = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idPessoa);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }
}