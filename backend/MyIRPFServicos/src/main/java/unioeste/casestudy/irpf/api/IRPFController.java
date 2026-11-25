package unioeste.casestudy.irpf.api;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import unioeste.casestudy.irpf.bo.AuditorFiscal;
import unioeste.casestudy.irpf.bo.BemDireito;
import unioeste.casestudy.irpf.bo.Contribuinte;
import unioeste.casestudy.irpf.bo.DeclaracaoRenda;
import unioeste.casestudy.irpf.bo.Dependente;
import unioeste.casestudy.irpf.bo.TipoDependencia;
import unioeste.casestudy.irpf.manager.IRPFException;
import unioeste.casestudy.irpf.manager.UCAuditorFiscalServicos;
import unioeste.casestudy.irpf.manager.UCContribuinteServicos;
import unioeste.casestudy.irpf.manager.UCDeclaracaoServicos;
import unioeste.casestudy.irpf.manager.UCDependenteServicos;
import unioeste.casestudy.irpf.manager.UCPessoaJuridicaServicos;
import unioeste.geral.pessoa.bo.PessoaJuridica;

@RestController
@RequestMapping("/api/irpf")
@CrossOrigin(origins = "*")
public class IRPFController {

    private final UCContribuinteServicos contribuinteServicos = new UCContribuinteServicos();
    private final UCDependenteServicos dependenteServicos = new UCDependenteServicos();
    private final UCPessoaJuridicaServicos pessoaJuridicaServicos = new UCPessoaJuridicaServicos();
    private final UCDeclaracaoServicos declaracaoServicos = new UCDeclaracaoServicos();
    private final UCAuditorFiscalServicos auditorServicos = new UCAuditorFiscalServicos();

    public record PerfilSaida(
            int idPessoa, String cpf, String codAuditor,
            String nome, String sobrenome, String perfilAcesso) {}

    public record DecisaoAuditoriaEntrada(String status, int idAuditor) {}

    // Identifica perfil c codAuditor ou CPF
    @GetMapping("/identificar/{identificador}")
    public ResponseEntity<?> identificarPerfil(@PathVariable("identificador") String identificador)
            throws SQLException {
        AuditorFiscal auditor = auditorServicos.buscarAuditorPorCodigoOuCpf(identificador);
        if (auditor != null) {
            return ResponseEntity.ok(new PerfilSaida(
                    auditor.getIdPessoa(),
                    auditor.getCpf(),
                    auditor.getCodAuditor(),
                    auditor.getNome(),
                    auditor.getSobrenome(),
                    "auditorFiscal"));
        }

        try {
            Contribuinte contribuinte = contribuinteServicos.consultarContribuinte(identificador);
            return ResponseEntity.ok(new PerfilSaida(
                    contribuinte.getIdPessoa(),
                    contribuinte.getCpf(),
                    null,
                    contribuinte.getNome(),
                    contribuinte.getSobrenome(),
                    "contribuinte"));
        } catch (IRPFException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensagem", "CPF ou código de auditor não encontrado."));
        }
    }

    // Contribuinte

    @PostMapping("/cadastrarContribuinte")
    public ResponseEntity<?> cadastrarContribuinte(@RequestBody Contribuinte contribuinte) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contribuinteServicos.cadastrarContribuinte(contribuinte));
    }

    @PutMapping("/atualizarDadosContribuinte")
    public ResponseEntity<?> atualizarDadosContribuinte(@RequestBody Contribuinte contribuinte) throws Exception {
        return ResponseEntity.ok(contribuinteServicos.atualizarDadosContribuinte(contribuinte));
    }

    @GetMapping("/contribuintes/cpf/{cpf}")
    public ResponseEntity<Contribuinte> consultarContribuinte(
            @PathVariable("cpf") String cpf) throws IRPFException, SQLException {
        return ResponseEntity.ok(contribuinteServicos.consultarContribuinte(cpf));
    }

    // Dependentes

    @PostMapping("/cadastrarDependente")
    public ResponseEntity<?> cadastrarDependente(@RequestBody Dependente dependente) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dependenteServicos.cadastrarDependente(dependente));
    }

    @DeleteMapping("/contribuintes/{idPessoa}/dependentes/{idDependente}")
    public ResponseEntity<Void> excluirDependente(
            @PathVariable("idPessoa") int idPessoa,
            @PathVariable("idDependente") int idDependente) throws IRPFException, SQLException {
        dependenteServicos.excluirDependente(idPessoa, idDependente);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/listarTiposDependencia")
    public ResponseEntity<List<TipoDependencia>> listarTiposDependencia() throws SQLException {
        return ResponseEntity.ok(dependenteServicos.listarTiposDependencia());
    }

    @GetMapping("/contribuintes/{idPessoa}/dependentes")
    public ResponseEntity<List<Dependente>> listarDependentes(
            @PathVariable("idPessoa") int idPessoa) throws IRPFException, SQLException {
        return ResponseEntity.ok(dependenteServicos.listarDependentes(idPessoa));
    }

    // Pessoa Jurídica/Fonte Pagadora

    @PostMapping("/cadastrarPessoaJuridica")
    public ResponseEntity<?> cadastrarPessoaJuridica(@RequestBody PessoaJuridica pj) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pessoaJuridicaServicos.cadastrarPessoaJuridica(pj));
    }

    @GetMapping("/fontes-pagadoras/{cnpj}")
    public ResponseEntity<PessoaJuridica> consultarFontePagadora(
            @PathVariable("cnpj") String cnpj) throws IRPFException, SQLException {
        return ResponseEntity.ok(pessoaJuridicaServicos.consultarFontePagadora(cnpj));
    }

    // Declaração e Bens

    @PostMapping("/registrarBens")
    public ResponseEntity<?> registrarBens(@RequestBody List<BemDireito> bens) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(declaracaoServicos.registrarBens(bens));
    }

    @GetMapping("/contribuintes/{idPessoa}/declaracoes/{idDeclaracao}/bens")
    public ResponseEntity<List<BemDireito>> listarBens(
            @PathVariable("idPessoa") int idPessoa,
            @PathVariable("idDeclaracao") int idDeclaracao)
            throws IRPFException, SQLException {
        return ResponseEntity.ok(declaracaoServicos.listarBens(idPessoa, idDeclaracao));
    }

    @GetMapping("/contribuintes/{idPessoa}/declaracoes/{idDeclaracao}")
    public ResponseEntity<DeclaracaoRenda> consultarDeclaracao(
            @PathVariable("idPessoa") int idPessoa,
            @PathVariable("idDeclaracao") int idDeclaracao)
            throws IRPFException, SQLException {
        return ResponseEntity.ok(declaracaoServicos.consultarDeclaracao(idPessoa, idDeclaracao));
    }

    @GetMapping("/contribuintes/{idPessoa}/declaracao-atual")
    public ResponseEntity<DeclaracaoRenda> consultarDeclaracaoAtual(
            @PathVariable("idPessoa") int idPessoa) throws IRPFException, SQLException {
        DeclaracaoRenda declaracao = declaracaoServicos.consultarUltimaDeclaracao(idPessoa, 2026);
        if (declaracao == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(declaracao);
    }

    @PostMapping("/cadastrarDeclaracaoRenda")
    public ResponseEntity<?> cadastrarDeclaracaoRenda(@RequestBody DeclaracaoRenda declaracao) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(declaracaoServicos.cadastrarDeclaracaoRenda(declaracao));
    }

    // Auditoria Fiscal

    @GetMapping("/auditoria/declaracoes")
    public ResponseEntity<List<DeclaracaoRenda>> listarDeclaracoesParaAuditoria() throws SQLException {
        return ResponseEntity.ok(auditorServicos.listarDeclaracoesParaAuditoria());
    }

    @PutMapping("/auditoria/declaracoes/{idDeclaracao}/status")
    public ResponseEntity<DeclaracaoRenda> atualizarStatusAuditoria(
            @PathVariable("idDeclaracao") int idDeclaracao,
            @RequestBody DecisaoAuditoriaEntrada entrada) throws IRPFException, SQLException {
        return ResponseEntity.ok(
                auditorServicos.atualizarStatusAuditoria(idDeclaracao, entrada.status(), entrada.idAuditor()));
    }

    // Exceções

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