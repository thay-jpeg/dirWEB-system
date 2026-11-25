package unioeste.casestudy.irpf.manager;

import java.sql.Connection;
import java.sql.SQLException;

import unioeste.apoio.bd.ConexaoBD;
import unioeste.casestudy.irpf.col.PessoaJuridicaCol;
import unioeste.casestudy.irpf.dao.PessoaDAO;
import unioeste.casestudy.irpf.dao.PessoaJuridicaDAO;
import unioeste.geral.pessoa.bo.PessoaJuridica;

//Cadastrar e consultar fonte pagadora
public class UCPessoaJuridicaServicos {

    private final PessoaJuridicaCol pessoaJuridicaCol = new PessoaJuridicaCol();
    private final IRPFServicoApoio apoio = new IRPFServicoApoio();

    public PessoaJuridica cadastrarPessoaJuridica(PessoaJuridica pj) throws IRPFException, Exception {
        pessoaJuridicaCol.validarCadastro(pj);
        String cnpjLimpo = pessoaJuridicaCol.validarELimparCnpj(pj.getCnpj());
        pj.setCnpj(cnpjLimpo);

        if (pj.getEndereco() != null) {
            pj.setEndereco(apoio.resolverEndereco(pj.getEndereco()));
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                PessoaJuridicaDAO pjDAO = new PessoaJuridicaDAO(conn);
                if (pjDAO.buscarIdPorCnpj(cnpjLimpo) != null) {
                    throw new IRPFException("Já existe uma Pessoa Jurídica cadastrada com este CNPJ.");
                }

                PessoaDAO pessoaDAO = new PessoaDAO(conn);
                apoio.resolverTelefonesPorCodigo(pj.getTelefones(), pessoaDAO);

                int idPessoa = pessoaDAO.inserirPessoa(pj);
                pjDAO.inserir(pj);
                pessoaDAO.substituirEmails(idPessoa, pj.getEmails());
                pessoaDAO.substituirTelefones(idPessoa, pj.getTelefones());

                conn.commit();
                return pj;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao cadastrar Pessoa Jurídica: " + e.getMessage(), e);
            }
        }
    }

    public PessoaJuridica consultarFontePagadora(String cnpj) throws IRPFException, SQLException {
        String cnpjLimpo = pessoaJuridicaCol.validarELimparCnpj(cnpj);

        try (Connection conn = ConexaoBD.getConexao()) {
            PessoaJuridicaDAO dao = new PessoaJuridicaDAO(conn);
            PessoaJuridica empresa = dao.buscarPorCnpj(cnpjLimpo);

            if (empresa == null) {
                throw new IRPFException("Fonte pagadora não encontrada.");
            }

            PessoaDAO pessoaDAO = new PessoaDAO(conn);
            pessoaDAO.carregarComplementos(empresa);
            return empresa;
        }
    }
}