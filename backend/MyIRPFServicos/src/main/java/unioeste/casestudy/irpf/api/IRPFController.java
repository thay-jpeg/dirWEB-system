package unioeste.casestudy.irpf.api;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import unioeste.casestudy.irpf.bo.BemDireito;
import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.bo.DeclaracaoRenda;
import unioeste.casestudy.irpf.bo.Dependente;
import unioeste.casestudy.irpf.bo.UsuarioAcesso;
import unioeste.casestudy.irpf.manager.IRPFException;
import unioeste.casestudy.irpf.manager.UCIRPFServicos;
import unioeste.geral.pessoa.bo.PessoaJuridica;
import java.sql.SQLException;
import org.springframework.web.bind.annotation.GetMapping;
import unioeste.casestudy.irpf.bo.TipoDependencia;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/irpf")
@CrossOrigin(origins = "*")
public class IRPFController {

    private final UCIRPFServicos servicos = new UCIRPFServicos();

    public record LoginEntrada(String cpf, String senha) {}

    public record LoginSaida(
            int idPessoa, String loginCpf, String nome,
            String sobrenome, String perfilAcesso) {}

    // Cadastro inicial do contribuinte (gov.br)
    @PostMapping("/cadastrarContribuinte")
    public ResponseEntity<?> cadastrarContribuinte(@RequestBody UsuarioAcesso usuario) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicos.cadastrarContribuinte(usuario));
    }
    //Login do usuário
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginEntrada entrada, HttpServletRequest request)
            throws SQLException {
        UsuarioAcesso usuario = servicos.autenticar(entrada.cpf(), entrada.senha());

        if (usuario == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "CPF ou senha inválidos."));
        }

        HttpSession sessaoAnterior = request.getSession(false);
        if (sessaoAnterior != null) {
            sessaoAnterior.invalidate();
        }

        Contribuinte pessoa = usuario.getContribuinte();
        HttpSession sessao = request.getSession();
        sessao.setAttribute("idPessoa", pessoa.getIdPessoa());
        sessao.setAttribute("perfilAcesso", usuario.getPerfilAcesso());

        return ResponseEntity.ok(new LoginSaida(
                pessoa.getIdPessoa(),
                usuario.getLoginCpf(),
                pessoa.getNome(),
                pessoa.getSobrenome(),
                usuario.getPerfilAcesso()));
    }

    // Att de dados pessoais
    @PutMapping("/atualizarDadosContribuinte")
    public ResponseEntity<?> atualizarDadosContribuinte(@RequestBody Contribuinte contribuinte) throws Exception {
        return ResponseEntity.ok(servicos.atualizarDadosContribuinte(contribuinte));
    }

    // Cadastro de dependente
    @PostMapping("/cadastrarDependente")
    public ResponseEntity<?> cadastrarDependente(@RequestBody Dependente dependente) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicos.cadastrarDependente(dependente));
    }
    //Lista de dependentes
    @GetMapping("/listarTiposDependencia")
    public ResponseEntity<List<TipoDependencia>> listarTiposDependencia() throws SQLException {
        List<TipoDependencia> tipos = servicos.listarTiposDependencia();
        return ResponseEntity.ok(tipos);
    }
    //Lista os dependentes de um contribuinte
    @GetMapping("/contribuintes/{idPessoa}/dependentes")
    public ResponseEntity<List<Dependente>> listarDependentes(
            @PathVariable("idPessoa") int idPessoa) throws IRPFException, SQLException {
        return ResponseEntity.ok(servicos.listarDependentes(idPessoa));
    }
    //Consulta contribuinte por CPF
    @GetMapping("/contribuintes/cpf/{cpf}")
    public ResponseEntity<Contribuinte> consultarContribuinte(
            @PathVariable("cpf") String cpf) throws IRPFException, SQLException {
        return ResponseEntity.ok(servicos.consultarContribuinte(cpf));
    }
    //Consulta fonte pagadora
    @GetMapping("/fontes-pagadoras/{cnpj}")
    public ResponseEntity<PessoaJuridica> consultarFontePagadora(
            @PathVariable("cnpj") String cnpj) throws IRPFException, SQLException {
        return ResponseEntity.ok(servicos.consultarFontePagadora(cnpj));
    }
    //Consulta Dec
    @GetMapping("/contribuintes/{idPessoa}/declaracoes/{idDeclaracao}/bens")
    public ResponseEntity<List<BemDireito>> listarBens(
            @PathVariable("idPessoa") int idPessoa,
            @PathVariable("idDeclaracao") int idDeclaracao)
            throws IRPFException, SQLException {
        List<BemDireito> bens = servicos.listarBens(idPessoa, idDeclaracao);
        return ResponseEntity.ok(bens);
    }
    //Consulta Geral Dec
    @GetMapping("/contribuintes/{idPessoa}/declaracoes/{idDeclaracao}")
    public ResponseEntity<DeclaracaoRenda> consultarDeclaracao(
            @PathVariable("idPessoa") int idPessoa,
            @PathVariable("idDeclaracao") int idDeclaracao)
            throws IRPFException, SQLException {
        return ResponseEntity.ok(servicos.consultarDeclaracao(idPessoa, idDeclaracao));
    }

    // Cadastro de Pessoa Juridica/Fonte Pagadora
    @PostMapping("/cadastrarPessoaJuridica")
    public ResponseEntity<?> cadastrarPessoaJuridica(@RequestBody PessoaJuridica pj) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicos.cadastrarPessoaJuridica(pj));
    }

    // Registro de Bens
    @PostMapping("/registrarBens")
    public ResponseEntity<?> registrarBens(@RequestBody List<BemDireito> bens) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicos.registrarBens(bens));
    }

    // Cadastro completo da DIRPF
    @PostMapping("/cadastrarDeclaracaoRenda")
    public ResponseEntity<?> cadastrarDeclaracaoRenda(@RequestBody DeclaracaoRenda declaracao) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicos.cadastrarDeclaracaoRenda(declaracao));
    }

    @GetMapping("/auditoria/declaracoes")
    public ResponseEntity<?> listarDeclaracoesParaAuditoria(HttpServletRequest request)
            throws SQLException {
        HttpSession sessao = request.getSession(false);

        if (sessao == null || sessao.getAttribute("idPessoa") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "Faça login para acessar a auditoria."));
        }

        if (!"auditorFiscal".equals(sessao.getAttribute("perfilAcesso"))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensagem", "Acesso restrito ao auditor fiscal."));
        }

        return ResponseEntity.ok(servicos.listarDeclaracoesParaAuditoria());
    }

    @ExceptionHandler(IRPFException.class)
    public ResponseEntity<?> handleValidacao(IRPFException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", "Validação", "mensagem", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleInfra(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("erro", "Infraestrutura", "mensagem", e.getMessage()));
    }
}