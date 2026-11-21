# DIRweb — Declaração de Imposto de Renda (IRPF 2026)

Sistema web para gerenciamento e auditoria da Declaração de Imposto de Renda Retido na Fonte (IRPF 2026), desenvolvido para a disciplina de **Engenharia de Software I** (Ciência da Computação — UNIOESTE).

## Tecnologias

* **Back-end:** Java 17, Spring Boot 3.4.5, Maven (Multi-módulo), JDBC puro (sem JPA/Hibernate) e HikariCP (Pool de Conexões).
* **Banco de Dados:** MySQL 8+.
* **Front-end:** React + Vite + Tailwind CSS.
* **Testes:** JUnit 5 (unitários) e Bruno (rotas REST).

---

## Arquitetura do Back-end (Multi-módulo Maven)

Todo o back-end fica em `backend/` e é orquestrado pelo POM pai (`dirweb-parent`):

| Módulo | Responsabilidade | Pacote Base |
| :--- | :--- | :--- |
| `MyInfraAPI` | Pool de conexões (`ConexaoBD`), `database.properties` e `db.sql` | `unioeste.apoio.bd` |
| `MyEnderecoBO` | Entidades de Endereço (`Serializable`) | `unioeste.geral.endereco.bo` |
| `MyEnderecoServicosImpl` | Regras de negócio, COLs, DAOs e testes de Endereço | `unioeste.geral.endereco.{col,dao,manager}` |
| `MyEnderecoServicos` | Controller REST de Endereço (`EnderecoController`) | `unioeste.geral.endereco.api` |
| `MyPessoaBO` | Entidades genéricas (`Pessoa`, `PessoaFisica`, `PessoaJuridica`, contatos) | `unioeste.geral.pessoa.bo` |
| `MyIRPFBO` | Entidades do estudo de caso IRPF (`Contribuinte`, `DeclaracaoRenda`, etc.) | `unioeste.casestudy.irpf.bo` |
| `MyIRPFServicosImpl` | Regras de negócio, COLs, DAOs e testes do IRPF | `unioeste.casestudy.irpf.{col,dao,manager}` |
| `MyIRPFServicos` | Aplicação Spring Boot executável (`DirwebApplication`) e Controllers do IRPF | `unioeste` / `unioeste.casestudy.irpf.api` |

---

## Pré-requisitos

* **JDK 17+** (compilação configurada com `--release 17`)
* **Apache Maven 3.8+**
* **MySQL Server 8+** (com serviço ativo)
* **Node.js 18+** (para o front-end)

---

## 1. Configuração do Banco de Dados (MySQL)

1. Abra o **MySQL Workbench** e execute o script:
   ```text
   backend/MyInfraAPI/src/main/resources/sql/db.sql
   ```
   *(O script recria automaticamente o schema `mydb` com todas as tabelas.)*

2. Popule os registros mínimos auxiliares para validar os testes de Endereço:
   ```sql
   USE mydb;

   INSERT INTO UF (idUF, UF) VALUES (1, 'PR');
   INSERT INTO Cidade (idCidade, Cidade, UF_idUF) VALUES (1, 'Foz do Iguaçu', 1);
   INSERT INTO Bairro (idBairro, Bairro) VALUES (1, 'Centro');
   INSERT INTO Tipo_logradouro (idTipo_logradouro, Tipo_logradouro) VALUES (1, 'Avenida');
   INSERT INTO Logradouro (idLogradouro, Logradouro, Tipo_logradouro_idTipo_logradouro) VALUES (1, 'Brasil', 1);
   ```

3. Verifique as credenciais locais (usuário, senha e porta do seu MySQL) em:
   ```text
   backend/MyInfraAPI/src/main/resources/database.properties
   ```

---

## 2. Como Abrir o Projeto na IDE

### IntelliJ IDEA
1. Clique em **File → Open** e selecione diretamente o arquivo **`backend/pom.xml`**.
2. Quando perguntado, escolha **Open as Project** (não abra apenas a pasta raiz sem importar o Maven).
3. Aguarde o IntelliJ baixar as dependências e reconhecer os 8 módulos na aba lateral **Maven**.
4. Em **File → Project Structure → Project**, confirme que o **SDK** está em Java 17 (ou superior) e o **Language level** em `17`.
5. Caso algum módulo apareça com imports em vermelho após um `git pull`, clique no ícone de **Reload All Maven Projects** na aba lateral **Maven**.

### VS Code
1. Abra a pasta do repositório no VS Code (com o *Extension Pack for Java* instalado).
2. Caso apareçam erros fantasmas de importação após atualizar o projeto, pressione `Ctrl + Shift + P` → **`Java: Clean Java Language Server Workspace`** → **Reload and delete**.

---

## 3. Compilar e Executar o Back-end

Todos os comandos abaixo devem ser executados dentro da pasta **`backend/`**:

### Instalar todos os módulos localmente (obrigatório na primeira vez e após alterar BOs)
```bash
mvn clean install -DskipTests
```

### Rodar os testes unitários de Endereço
```bash
mvn test -pl MyEnderecoServicosImpl
```

### Subir a API REST (porta `8080`)
```bash
mvn -pl MyIRPFServicos spring-boot:run
```
*(No IntelliJ, você também pode rodar clicando no botão **Play** direto na classe `unioeste.DirwebApplication` dentro de `MyIRPFServicos`.)*

---

## 4. Endpoints REST Disponíveis (Módulo Endereço)

Base URL: `http://localhost:8080`

| Serviço | Método | Endpoint |
| :--- | :--- | :--- |
| Cadastrar Endereço | `POST` | `/api/endereco/cadastrarEndereco` |
| Obter Endereço por CEP | `GET` | `/api/endereco/obterEnderecoPorCEP/{cep}` |
| Obter Endereço por ID | `GET` | `/api/endereco/obterEnderecoPorID/{id}` |
| Obter Endereço Externo (ViaCEP) | `GET` | `/api/endereco/obterEnderecoExterno/{cep}` |
| Obter Cidade | `GET` | `/api/endereco/obterCidade/{idCidade}` |

### Exemplo de Body JSON (`POST /api/endereco/cadastrarEndereco`)
```json
{
  "cep": "85851-000",
  "tipoEndereco": 1,
  "numero": "1500",
  "complemento": "Apto 42",
  "bairro": { "idBairro": 1 },
  "logradouro": {
    "idLogradouro": 1,
    "tipoLogradouro": { "idTipoLogradouro": 1 }
  },
  "cidade": {
    "idCidade": 1,
    "uf": { "idUF": 1 }
  }
}
```

---

## 5. Executar o Front-end

Dentro da pasta do front-end (`frontend/dirweb-front/`):

```bash
npm install
npm run dev
```

---

## Boas Práticas no Repositório

* **Não subir arquivos de IDE ou build:** `.idea/`, `*.iml`, `.vscode/`, `.classpath`, `target/` e `node_modules/` já estão ignorados no `.gitignore`.
* **Novas classes de negócio (BO):** todas as classes em `MyEnderecoBO`, `MyPessoaBO` e `MyIRPFBO` devem implementar `Serializable` e possuir construtor padrão vazio.
* **Ao adicionar dependências:** declare apenas no `pom.xml` do módulo correspondente e rode `mvn clean install -DskipTests` na pasta `backend/`.