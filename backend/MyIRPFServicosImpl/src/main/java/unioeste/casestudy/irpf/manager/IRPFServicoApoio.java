package unioeste.casestudy.irpf.manager;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.col.PessoaCol;
import unioeste.casestudy.irpf.dao.ContribuinteDAO;
import unioeste.casestudy.irpf.dao.PessoaDAO;
import unioeste.geral.endereco.bo.Endereco;
import unioeste.geral.endereco.manager.EnderecoException;
import unioeste.geral.endereco.manager.UCEnderecoGeralServicos;
import unioeste.geral.pessoa.bo.DDD;
import unioeste.geral.pessoa.bo.DDI;
import unioeste.geral.pessoa.bo.Telefone;

//Métodos auxiliares para resolver IDs, validar e normalizar dados de contribuintes, endereços e telefones
class IRPFServicoApoio {

    private final PessoaCol pessoaCol = new PessoaCol();
    private final UCEnderecoGeralServicos enderecoServicos = new UCEnderecoGeralServicos();

    Integer resolverIdContribuinte(ContribuinteDAO dao, Contribuinte c) throws IRPFException, SQLException {
        if (c.getIdPessoa() != null && c.getIdPessoa() > 0) {
            if (!dao.existePorId(c.getIdPessoa())) {
                throw new IRPFException("Contribuinte não encontrado para o idPessoa informado.");
            }
            return c.getIdPessoa();
        }
        String cpf = pessoaCol.limparDigitos(c.getCpf());
        Integer id = dao.buscarIdPorCpf(cpf);
        if (id == null) {
            throw new IRPFException("Contribuinte não encontrado para o CPF informado.");
        }
        return id;
    }

    Contribuinte carregarContribuinte(Connection conn, int idPessoa) throws IRPFException, SQLException {
        ContribuinteDAO dao = new ContribuinteDAO(conn);
        Contribuinte contribuinte = dao.buscarPorId(idPessoa);
        if (contribuinte == null) {
            throw new IRPFException("Contribuinte não encontrado.");
        }
        PessoaDAO pessoaDAO = new PessoaDAO(conn);
        pessoaDAO.carregarComplementos(contribuinte);
        return contribuinte;
    }

    Endereco resolverEndereco(Endereco endereco) throws IRPFException, Exception {
        try {
            if (endereco.getIdEndereco() > 0) {
                return enderecoServicos.obterEnderecoPorID(endereco);
            }
            return enderecoServicos.cadastrarEndereco(endereco);
        } catch (EnderecoException e) {
            throw new IRPFException("Erro no serviço de endereço: " + e.getMessage());
        }
    }

    void resolverTelefonesPorCodigo(List<Telefone> telefones, PessoaDAO pessoaDAO)
            throws IRPFException, SQLException {
        if (telefones == null) {
            return;
        }
        pessoaCol.validarTelefones(telefones);

        for (Telefone telefone : telefones) {
            DDD ddd = telefone.getDdd();
            DDI ddi = telefone.getDdi();
            DDI ddiDoDdd = ddd.getDdi();

            Integer idDdi = ddi == null ? null : ddi.getIdDDI();
            if (idDdi == null && ddiDoDdd != null) {
                idDdi = ddiDoDdd.getIdDDI();
            }

            if (ddd.getIdDDD() != null && ddd.getIdDDD() > 0 && idDdi != null && idDdi > 0) {
                continue;
            }

            String codigoDdd = ddd.getDdd();
            String codigoDdi = ddi == null ? null : ddi.getDdi();
            if ((codigoDdi == null || codigoDdi.isBlank()) && ddiDoDdd != null) {
                codigoDdi = ddiDoDdd.getDdi();
            }

            DDD encontrado = pessoaDAO.selecionarDddPorCodigoEDdi(codigoDdd, codigoDdi);
            if (encontrado == null) {
                throw new IRPFException("O par de DDD e DDI não está cadastrado no banco.");
            }

            telefone.setDdd(encontrado);
            telefone.setDdi(encontrado.getDdi());
        }
    }
}