package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import unioeste.casestudy.irpf.bo.TipoDependencia;
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

    public List<TipoDependencia> listarTiposDependencia() throws SQLException {
        String sql = "SELECT idTipo_dependente, Tipo_dependente "
                + "FROM Tipo_dependente ORDER BY idTipo_dependente";

        List<TipoDependencia> tipos = new ArrayList<>();

        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                TipoDependencia tipo = new TipoDependencia();
                tipo.setIdTipoDependencia(rs.getInt("idTipo_dependente"));
                tipo.setDescricao(rs.getString("Tipo_dependente"));
                tipos.add(tipo);
            }
        }

        return tipos;
    }

    public List<Dependente> listarPorContribuinte(int idPessoa) throws SQLException {
        String sql = """
            SELECT d.idDependente, d.Nome, d.CPF,
                   t.idTipo_dependente, t.Tipo_dependente
            FROM Dependente d
            JOIN Tipo_dependente t
                ON t.idTipo_dependente = d.Tipo_dependente_idTipo_dependente
            WHERE d.Contribuinte_Pessoa_idPessoa = ?
            ORDER BY d.idDependente
            """;

        List<Dependente> dependentes = new ArrayList<>();

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idPessoa);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TipoDependencia tipo = new TipoDependencia();
                    tipo.setIdTipoDependencia(rs.getInt("idTipo_dependente"));
                    tipo.setDescricao(rs.getString("Tipo_dependente"));

                    Dependente dependente = new Dependente();
                    dependente.setIdDependente(rs.getInt("idDependente"));
                    dependente.setNome(rs.getString("Nome"));
                    dependente.setCpf(rs.getString("CPF"));
                    dependente.setTipoDependencia(tipo);
                    dependentes.add(dependente);
                }
            }
        }

        return dependentes;
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