package unioeste.geral.endereco.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import unioeste.geral.endereco.bo.*;
import unioeste.geral.endereco.manager.EnderecoException;
import unioeste.geral.endereco.manager.UCEnderecoGeralServicos;

import java.util.Map;

@RestController
@RequestMapping("/api/endereco")
@CrossOrigin(origins = "*")
public class EnderecoController {

    private final UCEnderecoGeralServicos servicos = new UCEnderecoGeralServicos();

    // 1. cadastrarEndereco
    @PostMapping("/cadastrarEndereco")
    public ResponseEntity<?> cadastrarEndereco(@RequestBody Endereco entrada) throws Exception {
        Endereco salvo = servicos.cadastrarEndereco(entrada);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    // 2. obterEnderecoPorCEP
    @GetMapping("/obterEnderecoPorCEP/{cep}")
    public ResponseEntity<?> obterEnderecoPorCEP(@PathVariable String cep) throws Exception {
        return ResponseEntity.ok(servicos.obterEnderecoPorCEP(cep));
    }

    // 3. obterEnderecoPorID
    @GetMapping("/obterEnderecoPorID/{id}")
    public ResponseEntity<?> obterEnderecoPorID(@PathVariable int id) throws Exception {
        Endereco filtro = new Endereco();
        filtro.setIdEndereco(id);
        return ResponseEntity.ok(servicos.obterEnderecoPorID(filtro));
    }

    // 4. obterEnderecoExterno
    @GetMapping("/obterEnderecoExterno/{cep}")
    public ResponseEntity<?> obterEnderecoExterno(@PathVariable String cep) throws Exception {
        return ResponseEntity.ok(servicos.obterEnderecoExterno(cep));
    }

    // 5. obterCidade
    @GetMapping("/obterCidade/{idCidade}")
    public ResponseEntity<?> obterCidade(@PathVariable int idCidade) throws Exception {
        Cidade filtro = new Cidade();
        filtro.setIdCidade(idCidade);
        return ResponseEntity.ok(servicos.obterCidade(filtro));
    }

    @ExceptionHandler(EnderecoException.class)
    public ResponseEntity<?> handleValidacao(EnderecoException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("erro", "Validação", "mensagem", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleInfra(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("erro", "Infraestrutura", "mensagem", e.getMessage()));
    }
}