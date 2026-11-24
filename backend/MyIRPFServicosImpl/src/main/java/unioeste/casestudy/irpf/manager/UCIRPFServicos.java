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
import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.geral.pessoa.bo.DDD;
import unioeste.geral.pessoa.bo.DDI;
import unioeste.geral.pessoa.bo.Telefone;
import unioeste.geral.endereco.manager.EnderecoException;
import unioeste.geral.endereco.manager.UCEnderecoGeralServicos;
import unioeste.geral.pessoa.bo.PessoaJuridica;
import java.sql.SQLException;
import unioeste.casestudy.irpf.bo.TipoDependencia;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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

    public UsuarioAcesso autenticar(String cpf, String senha) throws SQLException {
        if (!irpfCol.validarCPF(cpf) || senha == null || senha.isEmpty()) {
            return null;
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            ContribuinteDAO dao = new ContribuinteDAO(conn);
            UsuarioAcesso usuario = dao.buscarUsuarioPorCpf(irpfCol.limparDigitos(cpf));

            if (usuario == null || usuario.getSenhaHash() == null) {
                return null;
            }

            String hashInformado = irpfCol.gerarHashSenha(senha);
            boolean senhaCorreta = MessageDigest.isEqual(
                    hashInformado.getBytes(StandardCharsets.UTF_8),
                    usuario.getSenhaHash().getBytes(StandardCharsets.UTF_8));

            if (!senhaCorreta) {
                return null;
            }

            usuario.setSenhaHash(null);
            return usuario;
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
                resolverTelefonesPorCodigo(contrib.getTelefones(), pessoaDAO);
                pessoaDAO.atualizarPessoa(contrib);
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
                //commit do fluxo "final feliz"
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

    //Lista os tipos de dependência cadastrados no banco.(filhos, pais, etc.)
    public List<TipoDependencia> listarTiposDependencia() throws SQLException {
        try (Connection conn = ConexaoBD.getConexao()) {
            DependenteDAO dependenteDAO = new DependenteDAO(conn);
            return dependenteDAO.listarTiposDependencia();
        }
    }
    //Lista os dependetes de um contribuinte
    public List<Dependente> listarDependentes(int idPessoa)
            throws IRPFException, SQLException {
        if (idPessoa <= 0) {
            throw new IRPFException("Informe um ID de contribuinte válido.");
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            ContribuinteDAO contribuinteDAO = new ContribuinteDAO(conn);

            if (!contribuinteDAO.existePorId(idPessoa)) {
                throw new IRPFException("Contribuinte não encontrado.");
            }

            DependenteDAO dependenteDAO = new DependenteDAO(conn);
            return dependenteDAO.listarPorContribuinte(idPessoa);
        }
    }
    //Consulta do Contribuinte
    public Contribuinte consultarContribuinte(String cpf)
            throws IRPFException, SQLException {
        if (!irpfCol.validarCPF(cpf)) {
            throw new IRPFException("CPF inválido.");
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            ContribuinteDAO dao = new ContribuinteDAO(conn);
            Integer idPessoa = dao.buscarIdPorCpf(irpfCol.limparDigitos(cpf));

            if (idPessoa == null) {
                throw new IRPFException("Contribuinte não encontrado.");
            }

            return carregarContribuinte(conn, idPessoa);
        }
    }

    private Contribuinte carregarContribuinte(Connection conn, int idPessoa)
            throws IRPFException, SQLException {
        ContribuinteDAO dao = new ContribuinteDAO(conn);
        Contribuinte contribuinte = dao.buscarPorId(idPessoa);

        if (contribuinte == null) {
            throw new IRPFException("Contribuinte não encontrado.");
        }

        PessoaDAO pessoaDAO = new PessoaDAO(conn);
        pessoaDAO.carregarComplementos(contribuinte);
        return contribuinte;
    }
    //Consulta Fonte Pagadora
    public PessoaJuridica consultarFontePagadora(String cnpj)
            throws IRPFException, SQLException {
        if (!irpfCol.validarCNPJ(cnpj)) {
            throw new IRPFException("CNPJ inválido.");
        }

        try (Connection conn = ConexaoBD.getConexao()) {
            PessoaJuridicaDAO dao = new PessoaJuridicaDAO(conn);
            PessoaJuridica empresa = dao.buscarPorCnpj(irpfCol.limparDigitos(cnpj));

            if (empresa == null) {
                throw new IRPFException("Fonte pagadora não encontrada.");
            }

            PessoaDAO pessoaDAO = new PessoaDAO(conn);
            pessoaDAO.carregarComplementos(empresa);
            return empresa;
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

    public List<BemDireito> listarBens(int idPessoa, int idDeclaracao)
            throws IRPFException, SQLException {
        try (Connection conn = ConexaoBD.getConexao()) {
            DeclaracaoDAO dao = new DeclaracaoDAO(conn);
            carregarCabecalho(dao, idPessoa, idDeclaracao);
            return dao.listarBens(idPessoa, idDeclaracao);
        }
    }
    //Consulta Geral Dec
    public DeclaracaoRenda consultarDeclaracao(int idPessoa, int idDeclaracao)
            throws IRPFException, SQLException {
        try (Connection conn = ConexaoBD.getConexao()) {
            DeclaracaoDAO declaracaoDAO = new DeclaracaoDAO(conn);
            DependenteDAO dependenteDAO = new DependenteDAO(conn);

            DeclaracaoRenda declaracao =
                    carregarCabecalho(declaracaoDAO, idPessoa, idDeclaracao);

            declaracao.setContribuinte(carregarContribuinte(conn, idPessoa));
            declaracao.setDependentes(dependenteDAO.listarPorContribuinte(idPessoa));
            declaracao.setBensDireitos(declaracaoDAO.listarBens(idPessoa, idDeclaracao));
            declaracao.setRendimentos(declaracaoDAO.listarRendimentos(idDeclaracao));

            return declaracao;
        }
    }

    public List<DeclaracaoRenda> listarDeclaracoesParaAuditoria() throws SQLException {
        try (Connection conn = ConexaoBD.getConexao()) {
            DeclaracaoDAO dao = new DeclaracaoDAO(conn);
            return dao.listarDeclaracoesParaAuditoria();
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

    private DeclaracaoRenda carregarCabecalho(
            DeclaracaoDAO dao, int idPessoa, int idDeclaracao)
            throws IRPFException, SQLException {
        DeclaracaoRenda declaracao = dao.buscarCabecalho(idPessoa, idDeclaracao);

        if (declaracao == null) {
            throw new IRPFException("Declaração não encontrada para esse contribuinte.");
        }

        return declaracao;
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

    private void resolverTelefonesPorCodigo(List<Telefone> telefones, PessoaDAO pessoaDAO)
            throws IRPFException, SQLException {
        if (telefones == null) {
            return;
        }

        for (Telefone telefone : telefones) {
            if (telefone == null || telefone.getFone() == null
                    || telefone.getFone().isBlank() || telefone.getDdd() == null) {
                throw new IRPFException("Informe o número e o DDD do telefone.");
            }

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

            if (codigoDdd == null || codigoDdd.isBlank()
                    || codigoDdi == null || codigoDdi.isBlank()) {
                throw new IRPFException("Informe os códigos de DDD e DDI.");
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