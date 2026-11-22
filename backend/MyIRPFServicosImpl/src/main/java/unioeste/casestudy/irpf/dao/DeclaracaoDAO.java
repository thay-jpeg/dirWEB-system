package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;

import unioeste.casestudy.irpf.bo.BemDireito;
import unioeste.casestudy.irpf.bo.DeclaracaoRenda;
import unioeste.casestudy.irpf.bo.RendimentoPJ;

public class DeclaracaoDAO {
    private final Connection conexao;

    public DeclaracaoDAO(Connection conexao) {
        this.conexao = conexao;
    }

    public int inserirDeclaracao(DeclaracaoRenda dec) throws SQLException {
        String sql = "INSERT INTO Declaracao_Renda (Contribuinte_Pessoa_idPessoa, anoExercicio, status) " +
                     "VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, dec.getContribuinte().getIdPessoa());
            stmt.setInt(2, dec.getAnoExercicio());
            stmt.setString(3, dec.getStatus());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    dec.setIdDeclaracao(id);
                    return id;
                }
            }
        }
        throw new SQLException("Falha ao obter idDeclaracao_Renda gerado.");
    }

    public Integer buscarIdDeclaracaoAtiva(int idContribuinte, int anoExercicio) throws SQLException {
        String sql = "SELECT idDeclaracao_Renda FROM Declaracao_Renda " +
                     "WHERE Contribuinte_Pessoa_idPessoa = ? AND anoExercicio = ? " +
                     "ORDER BY idDeclaracao_Renda DESC LIMIT 1";
        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idContribuinte);
            stmt.setInt(2, anoExercicio);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("idDeclaracao_Renda") : null;
            }
        }
    }

    public void inserirRendimento(RendimentoPJ rend, int idDeclaracao, int idPessoaJuridica) throws SQLException {
        String sql = "INSERT INTO Rendimentos_trib_PJ " +
                     "(Valor_Total_Recebido, Valor_13_salario, IRRF_pago, INSS, " +
                     " Declaracao_Renda_idDeclaracao_Renda, Pessoa_Juridica_Pessoa_idPessoa) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conexao.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setDouble(1, rend.getValorTotalRecebido());
            stmt.setDouble(2, rend.getValor13Salario());
            stmt.setDouble(3, rend.getIrrfPago());
            stmt.setDouble(4, rend.getInss());
            stmt.setInt(5, idDeclaracao);
            stmt.setInt(6, idPessoaJuridica);
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) rend.setIdRendimento(keys.getInt(1));
            }
            rend.setIdDeclaracao(idDeclaracao);
        }
    }

    public void inserirBemComTipo(BemDireito bem, int idContribuinte, int idDeclaracao) throws SQLException {
        String sqlBem = "INSERT INTO Bens_direitos " +
                        "(Nome_bem, Valor_ano_anterior, Valor_31_12_2026, " +
                        " Contribuinte_Pessoa_idPessoa, Declaracao_Renda_idDeclaracao_Renda, Endereco_idEndereco) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
        int idBemGerado;
        try (PreparedStatement stmt = conexao.prepareStatement(sqlBem, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, bem.getNomeBem());
            stmt.setDouble(2, bem.getValorAnoAnterior());
            stmt.setDouble(3, bem.getValorAtual());
            stmt.setInt(4, idContribuinte);
            stmt.setInt(5, idDeclaracao);
            if (bem.getEndereco() != null) {
                stmt.setInt(6, bem.getEndereco().getIdEndereco());
            } else {
                stmt.setNull(6, Types.INTEGER);
            }
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Falha ao gerar idBens_direitos.");
                idBemGerado = keys.getInt(1);
                bem.setIdBem(idBemGerado);
                bem.setIdContribuinte(idContribuinte);
                bem.setIdDeclaracao(idDeclaracao);
            }
        }

        String sqlTipo = "INSERT INTO Tipo_Bem (descricao_tipo, Bens_direitos_idBens_direitos) VALUES (?, ?)";
        try (PreparedStatement stmtTipo = conexao.prepareStatement(sqlTipo, Statement.RETURN_GENERATED_KEYS)) {
            stmtTipo.setString(1, bem.getTipoBem().getDescricao());
            stmtTipo.setInt(2, idBemGerado);
            stmtTipo.executeUpdate();
            try (ResultSet keys = stmtTipo.getGeneratedKeys()) {
                if (keys.next()) bem.getTipoBem().setIdTipoBem(keys.getInt(1));
            }
        }
    }
}