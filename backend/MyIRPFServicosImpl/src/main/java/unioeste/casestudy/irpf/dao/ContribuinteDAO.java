package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.geral.endereco.bo.Endereco;
import unioeste.geral.pessoa.bo.PessoaFisica;

public class ContribuinteDAO {
    private final Connection conexao;

    public ContribuinteDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public void inserirPessoaFisica(PessoaFisica pf) throws SQLException {
        String sql = "INSERT INTO Pessoa_Fisica (Pessoa_idPessoa, Sobrenome, Nome_Social, CPF, RG, Sexo) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, pf.getIdPessoa());
            stmt.setString(2, pf.getSobrenome());
            stmt.setString(3, pf.getNomeSocial());
            stmt.setString(4, pf.getCpf());
            stmt.setString(5, pf.getRg());
            stmt.setString(6, pf.getSexo());
            stmt.executeUpdate();
        }
    }

    public void inserirContribuinte(int idPessoa) throws SQLException {
        String sql = "INSERT INTO Contribuinte (Pessoa_idPessoa) VALUES (?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idPessoa);
            stmt.executeUpdate();
        }
    }

    public void atualizarPessoaFisica(PessoaFisica pf) throws SQLException {
        String sql = "UPDATE Pessoa_Fisica SET Sobrenome = ?, Nome_Social = ?, RG = ?, Sexo = ? " +
                     "WHERE Pessoa_idPessoa = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, pf.getSobrenome());
            stmt.setString(2, pf.getNomeSocial());
            stmt.setString(3, pf.getRg());
            stmt.setString(4, pf.getSexo());
            stmt.setInt(5, pf.getIdPessoa());
            stmt.executeUpdate();
        }
    }

    public Contribuinte buscarPorId(int idPessoa) throws SQLException {
        String sql = """
            SELECT p.idPessoa, p.Nome, p.Endereco_idEndereco,
                   pf.Sobrenome, pf.Nome_Social, pf.CPF, pf.RG, pf.Sexo
            FROM Contribuinte c
            INNER JOIN Pessoa_Fisica pf ON pf.Pessoa_idPessoa = c.Pessoa_idPessoa
            INNER JOIN Pessoa p ON p.idPessoa = pf.Pessoa_idPessoa
            WHERE c.Pessoa_idPessoa = ?
            """;

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idPessoa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                Contribuinte c = new Contribuinte();
                c.setIdPessoa(rs.getInt("idPessoa"));
                c.setNome(rs.getString("Nome"));
                c.setSobrenome(rs.getString("Sobrenome"));
                c.setNomeSocial(rs.getString("Nome_Social"));
                c.setCpf(rs.getString("CPF"));
                c.setRg(rs.getString("RG"));
                c.setSexo(rs.getString("Sexo"));

                Integer idEndereco = rs.getObject("Endereco_idEndereco", Integer.class);
                if (idEndereco != null) {
                    Endereco endereco = new Endereco();
                    endereco.setIdEndereco(idEndereco);
                    c.setEndereco(endereco);
                }

                return c;
            }
        }
    }

    public Integer buscarIdPessoaFisicaPorCpf(String cpfLimpo) throws SQLException {
        String sql = "SELECT Pessoa_idPessoa FROM Pessoa_Fisica WHERE CPF = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cpfLimpo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("Pessoa_idPessoa") : null;
            }
        }
    }

    public Integer buscarIdPorCpf(String cpfLimpo) throws SQLException {
        String sql = "SELECT c.Pessoa_idPessoa " +
                     "FROM Contribuinte c " +
                     "INNER JOIN Pessoa_Fisica pf ON c.Pessoa_idPessoa = pf.Pessoa_idPessoa " +
                     "WHERE pf.CPF = ?";
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