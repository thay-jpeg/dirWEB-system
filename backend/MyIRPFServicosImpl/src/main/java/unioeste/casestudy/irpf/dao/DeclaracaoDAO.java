package unioeste.casestudy.irpf.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import unioeste.casestudy.irpf.bo.BemDireito;
import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.bo.DeclaracaoRenda;
import unioeste.casestudy.irpf.bo.RendimentoPJ;
import unioeste.casestudy.irpf.bo.TipoBem;
import unioeste.geral.pessoa.bo.PessoaJuridica;

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

    // Cabeçalho = (dec, pessoa)
    public DeclaracaoRenda buscarCabecalho(int idPessoa, int idDeclaracao)
            throws SQLException {
        String sql = """
            SELECT idDeclaracao_Renda, anoExercicio, status
            FROM Declaracao_Renda
            WHERE idDeclaracao_Renda = ?
              AND Contribuinte_Pessoa_idPessoa = ?
            """;

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idDeclaracao);
            stmt.setInt(2, idPessoa);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                DeclaracaoRenda declaracao = new DeclaracaoRenda();
                declaracao.setIdDeclaracao(rs.getInt("idDeclaracao_Renda"));
                declaracao.setAnoExercicio(rs.getInt("anoExercicio"));
                declaracao.setStatus(rs.getString("status"));
                return declaracao;
            }
        }
    }

    public List<DeclaracaoRenda> listarDeclaracoesParaAuditoria() throws SQLException {
        String sql = """
            SELECT d.idDeclaracao_Renda, d.anoExercicio, d.status,
                   p.idPessoa, p.Nome, pf.Sobrenome, pf.CPF
            FROM Declaracao_Renda d
            JOIN Contribuinte c ON c.Pessoa_idPessoa = d.Contribuinte_Pessoa_idPessoa
            JOIN Pessoa_Fisica pf ON pf.Pessoa_idPessoa = c.Pessoa_idPessoa
            JOIN Pessoa p ON p.idPessoa = pf.Pessoa_idPessoa
            WHERE d.status IN ('Enviada', 'Processada', 'Retida', 'Retida em Malha Fina')
            ORDER BY d.idDeclaracao_Renda DESC
            """;

        List<DeclaracaoRenda> declaracoes = new ArrayList<>();

        try (PreparedStatement stmt = conexao.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Contribuinte titular = new Contribuinte();
                titular.setIdPessoa(rs.getInt("idPessoa"));
                titular.setNome(rs.getString("Nome"));
                titular.setSobrenome(rs.getString("Sobrenome"));
                titular.setCpf(rs.getString("CPF"));

                DeclaracaoRenda declaracao = new DeclaracaoRenda();
                declaracao.setIdDeclaracao(rs.getInt("idDeclaracao_Renda"));
                declaracao.setAnoExercicio(rs.getInt("anoExercicio"));
                declaracao.setStatus(rs.getString("status"));
                declaracao.setContribuinte(titular);
                declaracoes.add(declaracao);
            }
        }

        return declaracoes;
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

    // Lista dos rendimentos
    public List<RendimentoPJ> listarRendimentos(int idDeclaracao) throws SQLException {
        String sql = """
            SELECT r.idRendimentos, r.Valor_Total_Recebido, r.Valor_13_salario,
                   r.IRRF_pago, r.INSS, p.idPessoa, p.Nome, pj.CNPJ
            FROM Rendimentos_trib_PJ r
            JOIN Pessoa_Juridica pj ON pj.Pessoa_idPessoa = r.Pessoa_Juridica_Pessoa_idPessoa
            JOIN Pessoa p ON p.idPessoa = pj.Pessoa_idPessoa
            WHERE r.Declaracao_Renda_idDeclaracao_Renda = ?
            ORDER BY r.idRendimentos
            """;

        List<RendimentoPJ> rendimentos = new ArrayList<>();

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idDeclaracao);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PessoaJuridica fonte = new PessoaJuridica();
                    fonte.setIdPessoa(rs.getInt("idPessoa"));
                    fonte.setNome(rs.getString("Nome"));
                    fonte.setCnpj(rs.getString("CNPJ"));

                    RendimentoPJ rendimento = new RendimentoPJ();
                    rendimento.setIdRendimento(rs.getInt("idRendimentos"));
                    rendimento.setIdDeclaracao(idDeclaracao);
                    rendimento.setValorTotalRecebido(rs.getObject("Valor_Total_Recebido", Double.class));
                    rendimento.setValor13Salario(rs.getObject("Valor_13_salario", Double.class));
                    rendimento.setIrrfPago(rs.getObject("IRRF_pago", Double.class));
                    rendimento.setInss(rs.getObject("INSS", Double.class));
                    rendimento.setFontePagadora(fonte);
                    rendimentos.add(rendimento);
                }
            }
        }

        return rendimentos;
    }

    // Lista dos bens
    public List<BemDireito> listarBens(int idPessoa, int idDeclaracao)
            throws SQLException {
        String sql = """
            SELECT b.idBens_direitos, b.Nome_bem, b.Valor_ano_anterior,
                   b.Valor_31_12_2026, t.idTipo_Bem, t.descricao_tipo
            FROM Bens_direitos b
            LEFT JOIN Tipo_Bem t
                ON t.Bens_direitos_idBens_direitos = b.idBens_direitos
            WHERE b.Declaracao_Renda_idDeclaracao_Renda = ?
              AND b.Contribuinte_Pessoa_idPessoa = ?
            ORDER BY b.idBens_direitos
            """;

        List<BemDireito> bens = new ArrayList<>();

        try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
            stmt.setInt(1, idDeclaracao);
            stmt.setInt(2, idPessoa);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BemDireito bem = new BemDireito();
                    bem.setIdBem(rs.getInt("idBens_direitos"));
                    bem.setNomeBem(rs.getString("Nome_bem"));
                    bem.setValorAnoAnterior(rs.getObject("Valor_ano_anterior", Double.class));
                    bem.setValorAtual(rs.getObject("Valor_31_12_2026", Double.class));
                    bem.setIdContribuinte(idPessoa);
                    bem.setIdDeclaracao(idDeclaracao);

                    Integer idTipo = rs.getObject("idTipo_Bem", Integer.class);
                    if (idTipo != null) {
                        TipoBem tipo = new TipoBem();
                        tipo.setIdTipoBem(idTipo);
                        tipo.setDescricao(rs.getString("descricao_tipo"));
                        bem.setTipoBem(tipo);
                    }

                    bens.add(bem);
                }
            }
        }

        return bens;
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
            if (bem.getEndereco() != null && bem.getEndereco().getIdEndereco() > 0) {
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

    public DeclaracaoRenda buscarCabecalhoPorId(int idDeclaracao) throws SQLException {
    String sql = """
        SELECT idDeclaracao_Renda, Contribuinte_Pessoa_idPessoa, anoExercicio, status
        FROM Declaracao_Renda
        WHERE idDeclaracao_Renda = ?
        """;

    try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
        stmt.setInt(1, idDeclaracao);
        try (ResultSet rs = stmt.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            Contribuinte titular = new Contribuinte();
            titular.setIdPessoa(rs.getInt("Contribuinte_Pessoa_idPessoa"));

            DeclaracaoRenda declaracao = new DeclaracaoRenda();
            declaracao.setIdDeclaracao(rs.getInt("idDeclaracao_Renda"));
            declaracao.setAnoExercicio(rs.getInt("anoExercicio"));
            declaracao.setStatus(rs.getString("status"));
            declaracao.setContribuinte(titular);
            return declaracao;
        }
    }
}

public boolean atualizarStatusAuditoria(int idDeclaracao, String novoStatus, int idAuditor) throws SQLException {
    String sql = """
        UPDATE Declaracao_Renda
        SET status = ?, Auditor_Fiscal_Pessoa_idPessoa = ?
        WHERE idDeclaracao_Renda = ?
        """;

    try (PreparedStatement stmt = conexao.prepareStatement(sql)) {
        stmt.setString(1, novoStatus);
        stmt.setInt(2, idAuditor);
        stmt.setInt(3, idDeclaracao);
        return stmt.executeUpdate() > 0;
    }
}
}