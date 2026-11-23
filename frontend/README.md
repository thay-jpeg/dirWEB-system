# DIRweb – Frontend (IRPF 2026)

Inicie o backend Java e o MySQL antes de rodar o frontend.

```bash
npm install
npm run dev
```

O Vite encaminha `/api` para o backend em `http://localhost:8080`. Para usar outro endereço, copie `.env.example` para `.env` e ajuste `VITE_API_URL`. Uma hospedagem do build também precisa encaminhar `/api` para o backend na mesma origem.

## Fluxos integrados

- Em `/login`, informe o CPF e escolha **Continuar** ou **Criar conta**. O Java cria a sessão no login. O perfil vindo do banco abre a área do contribuinte ou do auditor. Atualizar a página pede um novo login.
- Os dados do contribuinte e os dependentes são consultados no banco. O cadastro de dependente grava imediatamente. O formulário busca os tipos de dependência cadastrados no banco.
- Os rendimentos e bens permanecem em memória enquanto a declaração é preenchida. **Enviar declaração** grava os dois em uma nova declaração. O Resumo mostra o ID retornado e permite consultar uma declaração salva pelo ID.
- A triagem mostra as declarações retornadas pela API do auditor. Cada linha abre os detalhes.
- Recuperação de senha, gestão de acessos, exclusão de dependentes e decisão de auditoria aguardam as respectivas rotas. A saída da interface limpa os dados locais. Encerrar a sessão Java exige uma rota de logout.
