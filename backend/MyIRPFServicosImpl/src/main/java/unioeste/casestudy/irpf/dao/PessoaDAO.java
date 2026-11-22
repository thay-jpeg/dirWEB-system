package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import unioeste.geral.pessoa.bo.Email;
import unioeste.geral.pessoa.bo.Pessoa;
import unioeste.geral.pessoa.bo.Telefone;

public class PessoaDAO {
    private final Connection conexao;

    public PessoaDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public int inserirPessoa(Pessoa pessoa) throws SQLException {
        String sql = "INSERT INTO Pessoa (Nome, Endereco_idEndereco) VALUES (?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, pessoa.getNome());
            if (pessoa.getEndereco() != null) {
                stmt.setInt(2, pessoa.getEndereco().getIdEndereco());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    pessoa.setIdPessoa(id);
                    return id;
                }
            }
        }
        throw new SQLException("Falha ao obter idPessoa gerado.");
    }

    public void atualizarPessoa(Pessoa pessoa) throws SQLException {
        String sql = "UPDATE Pessoa SET Nome = ?, Endereco_idEndereco = COALESCE(?, Endereco_idEndereco) WHERE idPessoa = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, pessoa.getNome());
            if (pessoa.getEndereco() !=     null) {
                stmt.setInt(2, pessoa.getEndereco().getIdEndereco());
            } else {
                stmt.setNull(2, Types.INTEGER);
            }
            stmt.setInt(3, pessoa.getIdPessoa());
            stmt.executeUpdate();
        }
    }

    public void substituirEmails(int idPessoa, List<Email> emails) throws SQLException {
        if (emails == null || emails.isEmpty()) return;

        try (PreparedStatement del = conexao.prepareStatement("DELETE FROM Email WHERE Pessoa_idPessoa = ?")) {
            del.setInt(1, idPessoa);
            del.executeUpdate();
        }

        String sql = "INSERT INTO Email (Email, Pessoa_idPessoa) VALUES (?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Email email : emails) {
                if (email.getEmail() == null || email.getEmail().isBlank()) continue;
                stmt.setString(1, email.getEmail().trim());
                stmt.setInt(2, idPessoa);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) email.setIdEmail(keys.getInt(1));
                }
            }
        }
    }

    public void substituirTelefones(int idPessoa, List<Telefone> telefones) throws SQLException {
        if (telefones == null || telefones.isEmpty()) return;

        try (PreparedStatement del = conexao.prepareStatement("DELETE FROM Telefone WHERE Pessoa_idPessoa = ?")) {
            del.setInt(1, idPessoa);
            del.executeUpdate();
        }

        String sql = "INSERT INTO Telefone (fone, DDD_idDDD, DDD_DDI_idDDI, Pessoa_idPessoa) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (Telefone tel : telefones) {
                if (tel.getFone() == null || tel.getFone().isBlank() || tel.getDdd() == null) continue;
                int idDdd = tel.getDdd().getIdDDD();
                int idDdi = (tel.getDdi() != null && tel.getDdi().getIdDDI() != null)
                        ? tel.getDdi().getIdDDI()
                        : tel.getDdd().getDdi().getIdDDI();

                stmt.setString(1, tel.getFone().trim());
                stmt.setInt(2, idDdd);
                stmt.setInt(3, idDdi);
                stmt.setInt(4, idPessoa);
                stmt.executeUpdate();
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) tel.setIdTelefone(keys.getInt(1));
                }
            }
        }
    }
}