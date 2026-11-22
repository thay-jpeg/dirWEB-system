package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import unioeste.geral.pessoa.bo.PessoaJuridica;

public class PessoaJuridicaDAO {
    private final Connection conexao;

    public PessoaJuridicaDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public void inserir(PessoaJuridica pj) throws SQLException {
        String sql = "INSERT INTO Pessoa_Juridica (CNPJ, Pessoa_idPessoa) VALUES (?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, pj.getCnpj());
            stmt.setInt(2, pj.getIdPessoa());
            stmt.executeUpdate();
        }
    }

    public Integer buscarIdPorCnpj(String cnpjLimpo) throws SQLException {
        String sql = "SELECT Pessoa_idPessoa FROM Pessoa_Juridica WHERE CNPJ = ?";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setString(1, cnpjLimpo);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("Pessoa_idPessoa") : null;
            }
        }
    }
}