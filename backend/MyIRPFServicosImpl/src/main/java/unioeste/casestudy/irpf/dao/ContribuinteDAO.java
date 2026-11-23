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

    public UsuarioAcesso buscarUsuarioPorCpf(String cpf) throws SQLException {
        String sql = """
            SELECT u.loginCpf, u.senhaHash, u.perfilAcesso,
                   p.idPessoa, p.Nome, c.Sobrenome
            FROM UsuarioAcesso u
            JOIN Contribuinte c ON c.Pessoa_idPessoa = u.Contribuinte_Pessoa_idPessoa
            JOIN Pessoa p ON p.idPessoa = c.Pessoa_idPessoa
            WHERE u.loginCpf = ?
            """;

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cpf);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Contribuinte contribuinte = new Contribuinte();
                contribuinte.setIdPessoa(rs.getInt("idPessoa"));
                contribuinte.setNome(rs.getString("Nome"));
                contribuinte.setSobrenome(rs.getString("Sobrenome"));

                UsuarioAcesso usuario = new UsuarioAcesso();
                usuario.setLoginCpf(rs.getString("loginCpf"));
                usuario.setSenhaHash(rs.getString("senhaHash"));
                usuario.setPerfilAcesso(rs.getString("perfilAcesso"));
                usuario.setContribuinte(contribuinte);
                return usuario;
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

    public Contribuinte buscarPorId(int idPessoa) throws SQLException {
        String sql = """
            SELECT p.idPessoa, p.Nome, c.Sobrenome, c.Nome_Social,
                   c.CPF, c.RG, c.Sexo
            FROM Contribuinte c
            JOIN Pessoa p ON p.idPessoa = c.Pessoa_idPessoa
            WHERE p.idPessoa = ?
            """;

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idPessoa);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Contribuinte contribuinte = new Contribuinte();
                contribuinte.setIdPessoa(rs.getInt("idPessoa"));
                contribuinte.setNome(rs.getString("Nome"));
                contribuinte.setSobrenome(rs.getString("Sobrenome"));
                contribuinte.setNomeSocial(rs.getString("Nome_Social"));
                contribuinte.setCpf(rs.getString("CPF"));
                contribuinte.setRg(rs.getString("RG"));
                contribuinte.setSexo(rs.getString("Sexo"));
                return contribuinte;
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