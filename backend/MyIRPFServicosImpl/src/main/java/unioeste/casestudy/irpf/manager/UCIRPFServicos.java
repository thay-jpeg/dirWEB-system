package unioeste.casestudy.irpf.manager;

import java.sql.Connection;
import java.util.List;

import unioeste.apoio.bd.ConexaoBD;
import unioeste.casestudy.irpf.bo.BemDireito;
import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.bo.DeclaracaoRenda;
import unioeste.casestudy.irpf.bo.Dependente;
import unioeste.casestudy.irpf.bo.RendimentoPJ;
import unioeste.casestudy.irpf.bo.UsuarioAcesso;
import unioeste.casestudy.irpf.col.IRPFCol;
import unioeste.casestudy.irpf.dao.ContribuinteDAO;
import unioeste.casestudy.irpf.dao.DeclaracaoDAO;
import unioeste.casestudy.irpf.dao.DependenteDAO;
import unioeste.casestudy.irpf.dao.PessoaDAO;
import unioeste.casestudy.irpf.dao.PessoaJuridicaDAO;
import unioeste.geral.endereco.bo.Endereco;
import unioeste.geral.endereco.manager.EnderecoException;
import unioeste.geral.endereco.manager.UCEnderecoGeralServicos;
import unioeste.geral.pessoa.bo.PessoaJuridica;

public class UCIRPFServicos {

    private final IRPFCol irpfCol = new IRPFCol();
    private final UCEnderecoGeralServicos enderecoServicos = new UCEnderecoGeralServicos();

    // Cadastro inicial no gov.br
    public UsuarioAcesso cadastrarContribuinte(UsuarioAcesso usuario) throws IRPFException, Exception {
        irpfCol.validarCadastroUsuario(usuario);

        Contribuinte contrib = usuario.getContribuinte();
        String cpfLimpo = irpfCol.limparDigitos(
                usuario.getLoginCpf() != null ? usuario.getLoginCpf() : contrib.getCpf()
        );
        contrib.setCpf(cpfLimpo);
        usuario.setLoginCpf(cpfLimpo);

        if ((contrib.getSobrenome() == null || contrib.getSobrenome().isBlank())
                && contrib.getNome().trim().contains(" ")) {
            String[] partes = contrib.getNome().trim().split("\\s+", 2);
            contrib.setNome(partes[0]);
            contrib.setSobrenome(partes[1]);
        }

        // Perfil c senha protegida SHA-256
        usuario.setPerfilAcesso("contribuinte");
        usuario.setSenhaHash(irpfCol.gerarHashSenha(usuario.getSenhaHash()));

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                if (contribDAO.buscarIdPorCpf(cpfLimpo) != null) {
                    throw new IRPFException("Já existe um contribuinte cadastrado com este CPF.");
                }

                PessoaDAO pessoaDAO = new PessoaDAO(conn);
                pessoaDAO.inserirPessoa(contrib);
                contribDAO.inserirContribuinte(contrib);
                contribDAO.inserirUsuarioAcesso(usuario);

                conn.commit();
                usuario.setSenhaHash(null); // Nunca retornar o hash na resposta
                return usuario;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao cadastrar contribuinte: " + e.getMessage(), e);
            }
        }
    }

    // Atualização de dados pessoais do contribuinte
    public Contribuinte atualizarDadosContribuinte(Contribuinte contrib) throws IRPFException, Exception {
        irpfCol.validarAtualizacaoContribuinte(contrib);

        if (contrib.getEndereco() != null) {
            contrib.setEndereco(resolverEndereco(contrib.getEndereco()));
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                Integer idPessoa = resolverIdContribuinte(contribDAO, contrib);
                contrib.setIdPessoa(idPessoa);

                PessoaDAO pessoaDAO = new PessoaDAO(conn);
                pessoaDAO.atualizarPessoa(contrib);
                contribDAO.atualizarContribuinte(contrib);
                pessoaDAO.substituirEmails(idPessoa, contrib.getEmails());
                pessoaDAO.substituirTelefones(idPessoa, contrib.getTelefones());

                conn.commit();
                return contrib;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao atualizar dados do contribuinte: " + e.getMessage(), e);
            }
        }
    }

    // Cadastro de dependente
    public Dependente cadastrarDependente(Dependente dependente) throws IRPFException, Exception {
        irpfCol.validarDependente(dependente);
        if (dependente.getContribuinte() == null) {
            throw new IRPFException("Contribuinte titular do dependente é obrigatório.");
        }
        dependente.setCpf(irpfCol.limparDigitos(dependente.getCpf()));

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                Integer idContribuinte = resolverIdContribuinte(contribDAO, dependente.getContribuinte());

                DependenteDAO depDAO = new DependenteDAO(conn);
                if (!depDAO.existeTipoDependencia(dependente.getTipoDependencia().getIdTipoDependencia())) {
                    throw new IRPFException("Tipo de dependência informado não existe no banco.");
                }

                depDAO.inserir(dependente, idContribuinte);
                dependente.getContribuinte().setIdPessoa(idContribuinte);

                conn.commit();
                return dependente;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao cadastrar dependente: " + e.getMessage(), e);
            }
        }
    }

    // Cadastro de Pessoa Juridica/Fonte Pagadora
    public PessoaJuridica cadastrarPessoaJuridica(PessoaJuridica pj) throws IRPFException, Exception {
        irpfCol.validarPessoaJuridica(pj);
        String cnpjLimpo = irpfCol.limparDigitos(pj.getCnpj());
        pj.setCnpj(cnpjLimpo);

        if (pj.getEndereco() != null) {
            pj.setEndereco(resolverEndereco(pj.getEndereco()));
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                PessoaJuridicaDAO pjDAO = new PessoaJuridicaDAO(conn);
                if (pjDAO.buscarIdPorCnpj(cnpjLimpo) != null) {
                    throw new IRPFException("Já existe uma Pessoa Jurídica cadastrada com este CNPJ.");
                }

                PessoaDAO pessoaDAO = new PessoaDAO(conn);
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

    // Registro de Bens (> R$ 100,00)
    public List<BemDireito> registrarBens(List<BemDireito> bens) throws IRPFException, Exception {
        irpfCol.validarListaBens(bens);

        Integer idContribuinte = bens.get(0).getIdContribuinte();
        if (idContribuinte == null || idContribuinte <= 0) {
            throw new IRPFException("idContribuinte é obrigatório para registrar bens.");
        }

        for (BemDireito bem : bens) {
            if (bem.getEndereco() != null) {
                bem.setEndereco(resolverEndereco(bem.getEndereco()));
            }
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                if (!contribDAO.existePorId(idContribuinte)) {
                    throw new IRPFException("Contribuinte informado não encontrado.");
                }

                DeclaracaoDAO decDAO = new DeclaracaoDAO(conn);
                Integer idDeclaracao = bens.get(0).getIdDeclaracao();
                if (idDeclaracao == null || idDeclaracao <= 0) {
                    idDeclaracao = decDAO.buscarIdDeclaracaoAtiva(idContribuinte, 2026);
                    if (idDeclaracao == null) {
                        DeclaracaoRenda rascunho = new DeclaracaoRenda();
                        Contribuinte c = new Contribuinte();
                        c.setIdPessoa(idContribuinte);
                        rascunho.setContribuinte(c);
                        rascunho.setAnoExercicio(2026);
                        rascunho.setStatus("Em Preenchimento");
                        idDeclaracao = decDAO.inserirDeclaracao(rascunho);
                    }
                }

                for (BemDireito bem : bens) {
                    decDAO.inserirBemComTipo(bem, idContribuinte, idDeclaracao);
                }

                conn.commit();
                return bens;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao registrar bens: " + e.getMessage(), e);
            }
        }
    }

    // Cadastro completo da DIRPF
    public DeclaracaoRenda cadastrarDeclaracaoRenda(DeclaracaoRenda declaracao) throws IRPFException, Exception {
        if (declaracao == null || declaracao.getContribuinte() == null) {
            throw new IRPFException("Dados da declaração e contribuinte são obrigatórios.");
        }
        if (declaracao.getAnoExercicio() == null) {
            declaracao.setAnoExercicio(2026);
        }
        if (declaracao.getStatus() == null || declaracao.getStatus().isBlank()) {
            declaracao.setStatus("Enviada");
        }

        // validações de regras de negócio antes da transação
        if (declaracao.getRendimentos() != null && !declaracao.getRendimentos().isEmpty()) {
            irpfCol.validarListaRendimentos(declaracao.getRendimentos());
        }
        if (declaracao.getBensDireitos() != null && !declaracao.getBensDireitos().isEmpty()) {
            irpfCol.validarListaBens(declaracao.getBensDireitos());
            for (BemDireito bem : declaracao.getBensDireitos()) {
                if (bem.getEndereco() != null) {
                    bem.setEndereco(resolverEndereco(bem.getEndereco()));
                }
            }
        }
        if (declaracao.getDependentes() != null) {
            for (Dependente dep : declaracao.getDependentes()) {
                irpfCol.validarDependente(dep);
                dep.setCpf(irpfCol.limparDigitos(dep.getCpf()));
            }
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            conn.setAutoCommit(false);
            try {
                ContribuinteDAO contribDAO = new ContribuinteDAO(conn);
                Integer idContribuinte = resolverIdContribuinte(contribDAO, declaracao.getContribuinte());
                declaracao.getContribuinte().setIdPessoa(idContribuinte);

                DeclaracaoDAO decDAO = new DeclaracaoDAO(conn);
                int idDeclaracao = decDAO.inserirDeclaracao(declaracao);

                // Dependentes (apenas os novos sem ID)
                if (declaracao.getDependentes() != null && !declaracao.getDependentes().isEmpty()) {
                    DependenteDAO depDAO = new DependenteDAO(conn);
                    for (Dependente dep : declaracao.getDependentes()) {
                        if (dep.getIdDependente() == null) {
                            if (!depDAO.existeTipoDependencia(dep.getTipoDependencia().getIdTipoDependencia())) {
                                throw new IRPFException("Tipo de dependência inválido no dependente: " + dep.getNome());
                            }
                            depDAO.inserir(dep, idContribuinte);
                        }
                    }
                }

                // rendimentos de PJ (valida CNPJ previamente cadastrado)
                if (declaracao.getRendimentos() != null && !declaracao.getRendimentos().isEmpty()) {
                    PessoaJuridicaDAO pjDAO = new PessoaJuridicaDAO(conn);
                    for (RendimentoPJ rend : declaracao.getRendimentos()) {
                        String cnpj = irpfCol.limparDigitos(rend.getFontePagadora().getCnpj());
                        Integer idPj = pjDAO.buscarIdPorCnpj(cnpj);
                        if (idPj == null) {
                            throw new IRPFException("Fonte pagadora com CNPJ " + cnpj + " não está cadastrada no sistema.");
                        }
                        rend.getFontePagadora().setIdPessoa(idPj);
                        decDAO.inserirRendimento(rend, idDeclaracao, idPj);
                    }
                }

                // bens + tipo_bem
                if (declaracao.getBensDireitos() != null && !declaracao.getBensDireitos().isEmpty()) {
                    for (BemDireito bem : declaracao.getBensDireitos()) {
                        decDAO.inserirBemComTipo(bem, idContribuinte, idDeclaracao);
                    }
                }

                conn.commit();
                return declaracao;
            } catch (IRPFException e) {
                conn.rollback();
                throw e;
            } catch (Exception e) {
                conn.rollback();
                throw new Exception("Erro de infraestrutura ao cadastrar declaração: " + e.getMessage(), e);
            }
        }
    }

    // auxiliares

    private Integer resolverIdContribuinte(ContribuinteDAO dao, Contribuinte c) throws Exception {
        if (c.getIdPessoa() != null && c.getIdPessoa() > 0) {
            if (!dao.existePorId(c.getIdPessoa())) {
                throw new IRPFException("Contribuinte não encontrado para o idPessoa informado.");
            }
            return c.getIdPessoa();
        }
        String cpf = irpfCol.limparDigitos(c.getCpf());
        Integer id = dao.buscarIdPorCpf(cpf);
        if (id == null) {
            throw new IRPFException("Contribuinte não encontrado para o CPF informado.");
        }
        return id;
    }

    private Endereco resolverEndereco(Endereco endereco) throws IRPFException, Exception {
        try {
            if (endereco.getIdEndereco() > 0) {
                return enderecoServicos.obterEnderecoPorID(endereco);
            }
            return enderecoServicos.cadastrarEndereco(endereco);
        } catch (EnderecoException e) {
            throw new IRPFException("Erro no serviço de endereço: " + e.getMessage());
        }
    }
}