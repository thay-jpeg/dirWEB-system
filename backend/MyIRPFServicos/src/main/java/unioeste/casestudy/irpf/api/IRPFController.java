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

@RestController
@RequestMapping("/api/irpf")
@CrossOrigin(origins = "*")
public class IRPFController {

    private final UCIRPFServicos servicos = new UCIRPFServicos();

    // Cadastro inicial do contribuinte (gov.br)
    @PostMapping("/cadastrarContribuinte")
    public ResponseEntity<?> cadastrarContribuinte(@RequestBody UsuarioAcesso usuario) throws Exception {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicos.cadastrarContribuinte(usuario));
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