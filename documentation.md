# FinancialManager — Documentação Técnica da API

API RESTful de alta performance para gerenciamento financeiro pessoal desenvolvida em **Spring Boot 4.1.2** com **Java 21**, Maven e persistência relacional em **MySQL 8.4**. A aplicação oferece controle rigoroso de fluxo de caixa multiusuário, suporte a movimentações avulsas, compras parceladas e transações recorrentes, consolidação dinâmica de saldos, relatórios mensais analíticos e exportação de extratos em CSV ou JSON com proteção contra injeções.

---

## 1. Visão Geral e Arquitetura

### 1.1 Stack Tecnológica
- **Linguagem & Plataforma:** Java 21 (LTS)
- **Framework Principal:** [Spring Boot 4.1.2](file:///var/home/anderson/IdeaProjects/Financial-Manager/pom.xml)
- **Acesso a Dados:** Spring Data JPA / Hibernate ORM
- **Segurança:** Spring Security 6 + JJWT 0.12.6 (JSON Web Tokens stateless HS256)
- **Banco de Dados:** MySQL 8.4 (Driver `mysql-connector-j`)
- **Serialização & Validação:** Jackson 3.x (`tools.jackson.databind`), Bean Validation (Hibernate Validator)
- **Produtividade & Boilerplate:** Project Lombok
- **Contêineres:** Dockerfile multi-estágio e Docker Compose

### 1.2 Parâmetros Principais
| Item | Especificação |
|---|---|
| **Base URL** | `http://localhost:8080/api/v1` |
| **Autenticação** | JWT Bearer Token stateless (`Authorization: Bearer <token>`) |
| **Criptografia de Senhas** | BCrypt Password Encoder |
| **Padrão de Erro** | Problem Details (RFC 7807 / RFC 9457) |
| **Formato de Dados** | JSON (`application/json;charset=UTF-8`) |
| **CORS** | Habilitado para todos os métodos (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`) |
| **Fuso Horário & Moeda** | UTC / Formatação de extratos em `pt-BR` (R$) |

### 1.3 Organização do Código por Domínio
O projeto segue uma arquitetura orientada a domínios de negócio organizada sob o pacote base `com.anderson.financialmanager`:
```
src/main/java/com/anderson/financialmanager/
├── config/                  # Segurança, filtro JWT, SecurityContextHelper
├── controller/              # Controladores REST expostos em /api/v1/*
├── exception/               # GlobalExceptionHandler e ApiErrorResponse (Problem Details)
└── model/                   # Domínios de negócio
    ├── usuario/             # Entidade Usuario, UsuarioRepository, UsuarioService, DTOs
    ├── conta/               # Entidade Conta, TipoConta, ContaRepository, ContaService, DTOs
    ├── categoria/           # Entidade Categoria, CategoriaRepository, CategoriaService, DTOs
    └── transacao/           # Entidade Transacao, TransacaoService, ExportacaoExtratoService,
                             # Enums, DTOs e pipeline de validadores em transacao/validacao/
```

### 1.4 Modelo de Isolamento Multiusuário
A segurança do sistema adota segregação rígida por usuário autenticado:
- Todos os registros (`Conta`, `Categoria`, `Transacao`) possuem vínculo obrigatório com o `Usuario` logado via chave estrangeira.
- O usuário ativo é extraído do token JWT via [`SecurityContextHelper`](file:///var/home/anderson/IdeaProjects/Financial-Manager/src/main/java/com/anderson/financialmanager/config/SecurityContextHelper.java).
- Nenhum usuário pode consultar, alterar, efetivar ou vincular dados de terceiros. Tentativas de acesso indevido disparam exceções imediatas (`HTTP 403 - Acesso negado` ou `HTTP 404 - Não encontrado`).

---

## 2. Autenticação e Gestão de Usuários — `/api/v1/auth`

Gerencia o ciclo de vida da conta do usuário e emissão de tokens de acesso.

| Método | Endpoint | Protegido | Descrição |
|---|---|:---:|---|
| `POST` | `/api/v1/auth/registrar` | Não | Registra novo usuário e retorna o token JWT gerado (HTTP 201) |
| `POST` | `/api/v1/auth/login` | Não | Autentica e-mail e senha, retornando o token JWT (HTTP 200) |
| `GET` | `/api/v1/auth/me` | Sim | Retorna os dados cadastrais do usuário autenticado |
| `PUT` | `/api/v1/auth/me` | Sim | Atualiza o nome do usuário autenticado |
| `PUT` | `/api/v1/auth/alterar-senha` | Sim | Altera a senha com validação prévia da senha atual (HTTP 204) |

### 2.1 Registro de Usuário (`POST /api/v1/auth/registrar`)
**Request:**
```json
{
  "nome": "Anderson Silva",
  "email": "anderson@example.com",
  "senha": "senhaSegura123"
}
```
*Regras de Validação:*
- `nome`: Obrigatório, entre 2 e 120 caracteres.
- `email`: Obrigatório, formato de e-mail válido, até 150 caracteres (normalizado em minúsculas, constraint única no banco).
- `senha`: Obrigatória, entre 6 e 100 caracteres.

**Response (`HTTP 201 Created`):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhbmRlcnNvbkBleGFtcGxlLmNvbSIsImlkIjoiMzc1MThlZjUtOGM3Yy00MDNmLTkyNDQtNTRhOTM5NzFjMDllIiwiaWF0IjoxNzMwNDkxMDAwLCJleHAiOjE3MzA1Nzc0MDB9...",
  "tipo": "Bearer",
  "expiraEm": "2026-11-03T18:00:00.000Z",
  "usuario": {
    "id": "37518ef5-8c7c-403f-9244-54a93971c09e",
    "nome": "Anderson Silva",
    "email": "anderson@example.com",
    "role": "ROLE_USER",
    "ativo": true,
    "criadoEm": "2026-11-02T18:00:00",
    "atualizadoEm": "2026-11-02T18:00:00"
  }
}
```

### 2.2 Autenticação (`POST /api/v1/auth/login`)
**Request:**
```json
{
  "email": "anderson@example.com",
  "senha": "senhaSegura123"
}
```
**Response (`HTTP 200 OK`):** Estrutura idêntica ao `TokenResponse` exibido no registro.

### 2.3 Atualização de Perfil (`PUT /api/v1/auth/me`)
Permite alterar o nome de exibição do usuário:
```json
{
  "nome": "Anderson Silva Junior"
}
```

### 2.4 Alteração de Senha (`PUT /api/v1/auth/alterar-senha`)
**Request:**
```json
{
  "senhaAtual": "senhaSegura123",
  "novaSenha": "novaSenhaSegura456"
}
```
*Regras de Negócio:*
- A `senhaAtual` deve corresponder ao hash BCrypt armazenado.
- A `novaSenha` deve ter entre 6 e 100 caracteres e ser estritamente diferente da `senhaAtual` (HTTP 422 em caso de repetição).
- Retorna `HTTP 204 No Content` em caso de sucesso.

---

## 3. Contas Financeiras — `/api/v1/contas`

Representam carteiras, contas correntes, poupanças, cartões ou contas de corretora/investimento.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/contas` | Cria uma nova conta ou reativa conta existente com mesmo nome (HTTP 201 + `Location`) |
| `GET` | `/api/v1/contas` | Lista contas com filtro opcional `?ativa=true\|false` e cálculo em lote de `saldoAtual` |
| `GET` | `/api/v1/contas/{id}` | Busca conta específica por UUID com `saldoAtual` consolidado |
| `PUT` | `/api/v1/contas/{id}` | Atualiza nome, instituição, tipo, saldo inicial e status |
| `DELETE` | `/api/v1/contas/{id}` | Inativação lógica (`ativa=false`), preservando integridade referencial histórica (HTTP 204) |

### 3.1 Tipos de Conta Suportados (`TipoConta`)
- `CONTA_CORRENTE`
- `POUPANCA`
- `CARTEIRA`
- `CORRETORA`
- `CARTAO_CREDITO`

### 3.2 Request de Criação / Edição (`ContaRequest`)
```json
{
  "nome": "Nubank Principal",
  "instituicao": "Nu Pagamentos S.A.",
  "tipo": "CONTA_CORRENTE",
  "saldoInicial": 1500.00,
  "ativa": true
}
```
- `nome`: 2 a 100 caracteres (obrigatório).
- `instituicao`: Até 100 caracteres (opcional).
- `tipo`: Valor do enum `TipoConta` (obrigatório).
- `saldoInicial`: Padrão `0.00` caso omitido.

### 3.3 Response de Conta (`ContaResponse`)
```json
{
  "id": "5170d182-d04b-4a25-9dfa-01c0cf05c6d3",
  "nome": "Nubank Principal",
  "instituicao": "Nu Pagamentos S.A.",
  "tipo": "CONTA_CORRENTE",
  "saldoInicial": 1500.00,
  "saldoAtual": 2840.50,
  "ativa": true,
  "criadoEm": "2026-11-01T10:00:00",
  "atualizadoEm": "2026-11-02T12:00:00"
}
```

### 3.4 Regras Importantes de Negócio:
1. **Cálculo Dinâmico de Saldo Atual:**
   $$\text{saldoAtual} = \text{saldoInicial} + \sum(\text{receitas EFETIVADAS}) - \sum(\text{despesas EFETIVADAS})$$
   - Movimentações com status `PENDENTE` ou `CANCELADA` **não** afetam o `saldoAtual`.
   - Na listagem geral (`GET /api/v1/contas`), o saldo é calculado via query agregada de alta performance no repositório, evitando consultas N+1.
2. **Reativação Transparente:**
   - Se o usuário tentar cadastrar uma conta com nome idêntico ao de uma conta que foi inativada anteriormente, o sistema reativa o registro inativo e atualiza seus campos, garantindo que o histórico contábil de transações antigas permaneça íntegro.

---

## 4. Categorias — `/api/v1/categorias`

Categorização de receitas e despesas orçamentárias.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/categorias` | Cria categoria ou reativa inativa com mesmo par `(nome, tipo)` (HTTP 201 + `Location`) |
| `GET` | `/api/v1/categorias` | Lista categorias com filtros `?tipo=RECEITA\|DESPESA` e `?ativa=true\|false` |
| `GET` | `/api/v1/categorias/{id}` | Busca categoria específica por UUID |
| `PUT` | `/api/v1/categorias/{id}` | Atualiza nome, descrição, cor e status |
| `DELETE` | `/api/v1/categorias/{id}` | Inativação lógica (`ativa=false`), HTTP 204 |

### 4.1 Request de Criação / Edição (`CategoriaRequest`)
```json
{
  "nome": "Alimentação & Mercado",
  "descricao": "Supermercados, restaurantes, lanchonetes e entregas",
  "corHex": "#FF5733",
  "tipo": "DESPESA",
  "ativa": true
}
```
- `nome`: 2 a 100 caracteres (obrigatório).
- `descricao`: Até 250 caracteres (opcional).
- `corHex`: Padrão hexadecimal `#RRGGBB` ou `#RGB` (opcional).
- `tipo`: `RECEITA` ou `DESPESA` (obrigatório).

### 4.2 Response de Categoria (`CategoriaResponse`)
```json
{
  "id": "18f9720b-f3eb-4df6-8806-9351a02e6ce9",
  "nome": "Alimentação & Mercado",
  "descricao": "Supermercados, restaurantes, lanchonetes e entregas",
  "corHex": "#FF5733",
  "tipo": "DESPESA",
  "ativa": true,
  "criadoEm": "2026-11-01T10:00:00",
  "atualizadoEm": "2026-11-02T12:00:00"
}
```

### 4.3 Regras de Unicidade e Reativação:
- Existe uma constraint de unicidade no banco: `uk_categorias_usuario_nome_tipo (usuario_id, nome, tipo)`.
- Se uma categoria com o mesmo par `(nome, tipo)` já estiver ativa, a criação é recusada com erro HTTP 422.
- Se existir uma categoria com o mesmo par `(nome, tipo)` inativada (`ativa=false`), ela é automaticamente reativada com os novos atributos, mantendo os vínculos históricos.

---

## 5. Transações Financeiras — `/api/v1/transacoes`

Módulo central que suporta lançamentos únicos, parcelamentos com rateio centesimal, projeção de recorrências e consolidação orçamentária.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/api/v1/transacoes` | Lança transação avulsa (`UNICA`), HTTP 201 + `Location` |
| `POST` | `/api/v1/transacoes/parceladas` | Gera e persiste em lote parcelas de compra parcelada, HTTP 201 |
| `POST` | `/api/v1/transacoes/recorrentes` | Projeta lote de ocorrências periódicas fixas, HTTP 201 |
| `GET` | `/api/v1/transacoes` | Lista transações com filtros de período, tipo, status, conta e categoria |
| `GET` | `/api/v1/transacoes/{id}` | Busca transação específica por ID |
| `GET` | `/api/v1/transacoes/grupos/{grupoId}` | Lista todas as parcelas ou ocorrências de um grupo de parcelamento/recorrência |
| `DELETE` | `/api/v1/transacoes/grupos/{grupoId}/pendentes` | Cancela todas as parcelas pendentes do grupo e encerra a recorrência |
| `PUT` | `/api/v1/transacoes/{id}` | Atualiza dados cadastrais da transação |
| `PATCH` | `/api/v1/transacoes/{id}/efetivar` | Baixa/efetiva lançamento com query param opcional `?dataPagamento=YYYY-MM-DD` |
| `DELETE` | `/api/v1/transacoes/{id}` | Exclusão física permanente do lançamento (HTTP 204) |
| `GET` | `/api/v1/transacoes/resumo-mensal` | Relatório consolidado mensal com agregação por categoria (`?ano=YYYY&mes=MM`) |
| `GET` | `/api/v1/transacoes/exportar` | Exporta extrato em anexo CSV ou JSON formatado |

---

### 5.1 Enums e Tipos de Domínio
- **`TipoTransacao`:** `RECEITA`, `DESPESA`
- **`StatusTransacao`:**
  - `PENDENTE`: Não afeta o saldo atual da conta. Não possui `dataPagamento`.
  - `EFETIVADA`: Movimentação concretizada. Altera o saldo atual da conta. Exige `dataPagamento`.
  - `CANCELADA`: Lançamento anulado. Não possui `dataPagamento` e não afeta o saldo.
- **`ModalidadeTransacao`:** `UNICA`, `PARCELADA`, `RECORRENTE`
- **`Periodicidade`:** `SEMANAL`, `QUINZENAL` (+15 dias), `MENSAL`, `BIMESTRAL`, `TRIMESTRAL`, `SEMESTRAL`, `ANUAL`
- **`ModoValorParcelamento`:** `VALOR_TOTAL`, `VALOR_PARCELA`

---

### 5.2 Lançamento Avulso (`POST /api/v1/transacoes`)

**Request (`TransacaoRequest`):**
```json
{
  "descricao": "Consulta Odontológica",
  "valor": 250.00,
  "dataCompetencia": "2026-11-10",
  "dataPagamento": null,
  "tipo": "DESPESA",
  "status": "PENDENTE",
  "contaId": "5170d182-d04b-4a25-9dfa-01c0cf05c6d3",
  "categoriaId": "18f9720b-f3eb-4df6-8806-9351a02e6ce9",
  "observacao": "Limpeza preventiva"
}
```

**Response (`TransacaoResponse`):**
```json
{
  "id": "e9b28392-1234-4567-89ab-cdef01234567",
  "descricao": "Consulta Odontológica",
  "valor": 250.00,
  "dataCompetencia": "2026-11-10",
  "dataPagamento": null,
  "tipo": "DESPESA",
  "status": "PENDENTE",
  "modalidade": "UNICA",
  "grupoParcelamentoId": null,
  "parcelaAtual": null,
  "totalParcelas": null,
  "referenciaParcela": null,
  "periodicidade": null,
  "recorrenciaAtiva": false,
  "observacao": "Limpeza preventiva",
  "contaId": "5170d182-d04b-4a25-9dfa-01c0cf05c6d3",
  "contaNome": "Nubank Principal",
  "categoriaId": "18f9720b-f3eb-4df6-8806-9351a02e6ce9",
  "categoriaNome": "Saúde",
  "criadoEm": "2026-11-02T14:00:00",
  "atualizadoEm": "2026-11-02T14:00:00"
}
```

---

### 5.3 Compra Parcelada (`POST /api/v1/transacoes/parceladas`)

Permite desdobrar uma compra em parcelas com distribuição de datas conforme a periodicidade e ajuste automático de arredondamento.

**Request (`DespesaParceladaRequest`):**
```json
{
  "descricao": "Notebook Dell XPS",
  "valorInformado": 1000.00,
  "modoValor": "VALOR_TOTAL",
  "totalParcelas": 3,
  "periodicidade": "MENSAL",
  "dataPrimeiraParcela": "2026-11-05",
  "primeiraParcelaPaga": true,
  "contaId": "5170d182-d04b-4a25-9dfa-01c0cf05c6d3",
  "categoriaId": "18f9720b-f3eb-4df6-8806-9351a02e6ce9",
  "observacao": "Compra corporativa"
}
```

**Mecanismo de Cálculo:**
- **`VALOR_TOTAL`:** O valor é dividido pelo total de parcelas com arredondamento para baixo (`DOWN`) em duas casas decimais. Os centavos restantes são somados à **1ª parcela** para garantir que a soma das parcelas seja exatamente igual ao total informado.
  *Exemplo ($R\$\ 1.000,00$ em 3x):*
  - Parcela 1: $R\$\ 333,34$ (efetivada se `primeiraParcelaPaga=true`)
  - Parcela 2: $R\$\ 333,33$ (pendente)
  - Parcela 3: $R\$\ 333,33$ (pendente)
- **`VALOR_PARCELA`:** O valor informado é multiplicado pelo número de parcelas. Cada parcela recebe o valor exato informado.
- Cada parcela é nomeada com o sufixo ordinal (ex: `Notebook Dell XPS (1/3)`).
- Todas compartilham um identificador comum `grupoParcelamentoId`.

**Response (`ParcelamentoResponse`):**
```json
{
  "grupoId": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "descricaoBase": "Notebook Dell XPS",
  "modalidade": "PARCELADA",
  "periodicidade": "MENSAL",
  "totalParcelas": 3,
  "valorTotalConsolidado": 1000.00,
  "parcelas": [
    {
      "id": "f1...",
      "descricao": "Notebook Dell XPS (1/3)",
      "valor": 333.34,
      "dataCompetencia": "2026-11-05",
      "dataPagamento": "2026-11-05",
      "status": "EFETIVADA",
      "parcelaAtual": 1,
      "totalParcelas": 3,
      ...
    },
    ...
  ]
}
```

---

### 5.4 Transações Recorrentes (`POST /api/v1/transacoes/recorrentes`)

Gera ocorrências periódicas projetadas para despesas ou receitas repetitivas (assinaturas, aluguel, salários, planos).

**Request (`TransacaoRecorrenteRequest`):**
```json
{
  "descricao": "Assinatura Streaming",
  "valor": 55.90,
  "tipo": "DESPESA",
  "periodicidade": "MENSAL",
  "dataInicio": "2026-11-15",
  "quantidadeOcorrencias": 12,
  "primeiraOcorrenciaEfetivada": true,
  "contaId": "5170d182-d04b-4a25-9dfa-01c0cf05c6d3",
  "categoriaId": "18f9720b-f3eb-4df6-8806-9351a02e6ce9",
  "observacao": "Cobrança mensal no cartão"
}
```
- `quantidadeOcorrencias`: Entre 1 e 60 ocorrências por solicitação.
- `recorrenciaAtiva`: Criada com valor booleano `true`.
- Se `primeiraOcorrenciaEfetivada=true`, a 1ª ocorrência nasce `EFETIVADA` na `dataInicio` e as subsequentes com status `PENDENTE`.

---

### 5.5 Gestão e Cancelamento de Grupos

- **`GET /api/v1/transacoes/grupos/{grupoId}`:** Lista todas as parcelas ou ocorrências vinculadas ao grupo.
- **`DELETE /api/v1/transacoes/grupos/{grupoId}/pendentes`:**
  - Desativa o sinalizador `recorrenciaAtiva=false` em todas as transações do lote.
  - Para cada parcela que ainda estiver com status `PENDENTE`, transiciona seu status para `CANCELADA` e anula `dataPagamento`.
  - Parcelas já `EFETIVADA` permanecem inalteradas, preservando o histórico de pagamentos anteriores.
  - Retorna a lista atualizada de todas as transações pertencentes ao grupo.

---

### 5.6 Efetivação de Lançamento (`PATCH /api/v1/transacoes/{id}/efetivar`)

Realiza a baixa contábil de um lançamento pendente.
- **Query Param opcional:** `?dataPagamento=YYYY-MM-DD`. Se omitida, assume a data atual do servidor (`LocalDate.now()`).
- **Restrição de Negócio:** Se a transação já estiver `CANCELADA`, o sistema rejeita a operação com `HTTP 422 Unprocessable Entity` ("Não é permitido efetivar uma transação que se encontra cancelada.").

---

### 5.7 Filtros de Listagem (`GET /api/v1/transacoes`)

Suporta consulta customizada através dos seguintes parâmetros opcionais:
```http
GET /api/v1/transacoes?dataInicio=2026-11-01&dataFim=2026-11-30&tipo=DESPESA&status=EFETIVADA&contaId=...&categoriaId=...
```
- Validação: `dataInicio` não pode ser posterior a `dataFim`.

---

### 5.8 Resumo Mensal Analítico (`GET /api/v1/transacoes/resumo-mensal`)

Fornece visão contábil consolidada para o mês/ano informado:
```http
GET /api/v1/transacoes/resumo-mensal?ano=2026&mes=11
```

**Response (`ResumoMensalResponse`):**
```json
{
  "ano": 2026,
  "mes": 11,
  "totalEntradasRealizadas": 6500.00,
  "totalEntradasPrevistas": 6500.00,
  "totalSaidasPagas": 2100.00,
  "totalSaidasPrevistas": 850.00,
  "totalSaidasConsolidado": 2950.00,
  "saldoLiquidoRealizado": 4400.00,
  "saldoLiquidoPrevisto": 3550.00,
  "gastosPorCategoria": [
    {
      "categoriaId": "18f9720b-f3eb-4df6-8806-9351a02e6ce9",
      "categoriaNome": "Alimentação & Mercado",
      "corHex": "#FF5733",
      "valorNominal": 1200.00,
      "percentual": 40.68,
      "quantidadeTransacoes": 8
    },
    {
      "categoriaId": "a901f443-4b6e-44e2-9bca-c7366113b2e5",
      "categoriaNome": "Moradia",
      "corHex": "#3380FF",
      "valorNominal": 900.00,
      "percentual": 30.51,
      "quantidadeTransacoes": 2
    }
  ]
}
```

---

## 6. Exportação de Extratos — `/api/v1/transacoes/exportar`

Gera arquivos para download com cabeçalho HTTP:
`Content-Disposition: attachment; filename="extrato_{dataInicio}_a_{dataFim}.csv|json"`

### 6.1 Parâmetros de Requisição
- `dataInicio` (obrigatório, ISO `YYYY-MM-DD`)
- `dataFim` (obrigatório, ISO `YYYY-MM-DD`)
- `contasIds` (opcional, lista de UUIDs repetíveis: `?contasIds=uuid1&contasIds=uuid2`)
- `formato` (opcional, padrão `CSV`. Valores: `CSV` ou `JSON`)

### 6.2 Proteção contra CSV Formula Injection (CWE-1236)
Para proteger os usuários contra execução indevida de macros em softwares como Excel e LibreOffice Calc:
- Qualquer campo textual cujo conteúdo se inicie com `=`, `+`, `-`, `@` ou caracteres de tabulação (`\t`) é automaticamente prefixado com o caractere `'` (apóstrofo de escape).
- Delimitador padrão: `;` (ponto e vírgula), compatível com a formatação monetária e decimal brasileira.
- Codificação: `UTF-8`.

### 6.3 Estrutura do JSON de Exportação
Quando requisitado com `formato=JSON`:
```json
{
  "dataInicio": "2026-11-01",
  "dataFim": "2026-11-30",
  "geradoEm": "2026-11-02T16:30:00",
  "totalRegistros": 42,
  "totalReceitas": 7500.00,
  "totalDespesas": 3200.00,
  "saldoPeriodo": 4300.00,
  "transacoes": [
    {
      "id": "e9b28392-1234-4567-89ab-cdef01234567",
      "dataCompetencia": "2026-11-05",
      "dataPagamento": "2026-11-05",
      "descricao": "Supermercado",
      "tipo": "DESPESA",
      "status": "EFETIVADA",
      "modalidade": "UNICA",
      "parcela": "-",
      "valor": 350.00,
      "valorFormatado": "R$ 350,00",
      "conta": "Nubank Principal",
      "instituicaoConta": "Nu Pagamentos S.A.",
      "categoria": "Alimentação & Mercado",
      "observacao": "Compras da semana"
    }
  ]
}
```

---

## 7. Pipeline de Validação (Chain of Responsibility)

As validações de integridade e negócio são isoladas em classes desacopladas sob o pacote `model/transacao/validacao/`:

1. **[`ValidadorPropriedadeEStatusContaCategoria`](file:///var/home/anderson/IdeaProjects/Financial-Manager/src/main/java/com/anderson/financialmanager/model/transacao/validacao/ValidadorPropriedadeEStatusContaCategoria.java):**
   - Garante que a `Conta` e a `Categoria` informadas pertencem ao usuário autenticado (se não, lança `AcessoNegadoException` -> HTTP 403).
   - Bloqueia lançamentos em contas inativas (`RegraDeNegocioException` -> HTTP 422).
   - Bloqueia lançamentos em categorias inativas (`RegraDeNegocioException` -> HTTP 422).
2. **[`ValidadorCompatibilidadeTipoCategoria`](file:///var/home/anderson/IdeaProjects/Financial-Manager/src/main/java/com/anderson/financialmanager/model/transacao/validacao/ValidadorCompatibilidadeTipoCategoria.java):**
   - Impede que uma transação de `DESPESA` utilize uma categoria do tipo `RECEITA` (e vice-versa).
3. **[`ValidadorConsistenciaStatusPagamento`](file:///var/home/anderson/IdeaProjects/Financial-Manager/src/main/java/com/anderson/financialmanager/model/transacao/validacao/ValidadorConsistenciaStatusPagamento.java):**
   - `valor` deve ser estritamente maior que zero.
   - Status `PENDENTE`: `dataPagamento` deve ser `null`.
   - Status `CANCELADA`: `dataPagamento` deve ser `null`.
   - Status `EFETIVADA`: `dataPagamento` é obrigatória.
   - Modalidade `PARCELADA`: valida se $1 \leq \text{parcelaAtual} \leq \text{totalParcelas}$.
4. **[`ValidadorValorMinimoParcelamento`](file:///var/home/anderson/IdeaProjects/Financial-Manager/src/main/java/com/anderson/financialmanager/model/transacao/validacao/ValidadorValorMinimoParcelamento.java):**
   - Exige no mínimo 2 parcelas.
   - Em `VALOR_TOTAL`, a divisão por parcelas não pode resultar em parcelas inferiores a R$ 0,01.

---

## 8. Tratamento Centralizado de Erros

A API adota o padrão **Problem Details** gerenciado pelo [`GlobalExceptionHandler`](file:///var/home/anderson/IdeaProjects/Financial-Manager/src/main/java/com/anderson/financialmanager/exception/GlobalExceptionHandler.java):

```json
{
  "type": "https://financialmanager.anderson.com/errors/422",
  "title": "Violação de regra de negócio",
  "status": 422,
  "detail": "Não é permitido registrar transações em uma conta inativa.",
  "instance": "/api/v1/transacoes",
  "timestamp": "2026-11-02T16:45:10.123Z"
}
```

### 8.1 Mapeamento de Códigos HTTP:
- **HTTP 400 (Bad Request):** JSON malformado, tipos incompatíveis ou campos inválidos. O payload inclui o array detalhado `errors: [{ "campo": "nome", "mensagem": "..." }]`.
- **HTTP 401 (Unauthorized):** Token JWT ausente, expirado ou inválido; credenciais incorretas no login.
- **HTTP 403 (Forbidden):** Tentativa de acessar ou operar recursos pertencentes a outro usuário.
- **HTTP 404 (Not Found):** Identificador não localizado para o usuário logado.
- **HTTP 409 (Conflict):** Violação de integridade referencial ou restrição de unicidade no banco.
- **HTTP 422 (Unprocessable Entity):** Regras de negócio violadas (conta inativa, inconsistência de datas/valores, reativações).
- **HTTP 500 (Internal Server Error):** Erros de sistema não tratados; mensagem sanitizada para prevenir vazamento de dados internos e stacktraces.

---

## 9. Configuração e Variáveis de Ambiente

As configurações são definidas no [`application.properties`](file:///var/home/anderson/IdeaProjects/Financial-Manager/src/main/resources/application.properties) e aceitam substituição via arquivo `.env` local ou variáveis de ambiente do sistema.

### 9.1 Variáveis Suportadas
| Variável | Descrição | Exemplo Padrão |
|---|---|---|
| `SPRING_DATASOURCE_URL` | URL de conexão JDBC ao MySQL | `jdbc:mysql://localhost:3306/financialmanager` |
| `SPRING_DATASOURCE_USERNAME` | Usuário do banco | `root` |
| `SPRING_DATASOURCE_PASSWORD` | Senha do banco | `root` ou vazia |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | Estratégia de DDL do Hibernate | `update` (na 1ª execução) / `none` / `validate` |
| `JWT_SECRET` | Chave secreta HS256 (mínimo 32 bytes / 256 bits) | *Chave segura de alta entropia* |
| `JWT_EXPIRATION_MS` | Tempo de vida do token em milissegundos | `86400000` (24 horas) |
| `JWT_ISSUER` | Emissor gravado no claim do token | `financial-manager-api` |

> [!IMPORTANT]
> - O banco de dados **não** possui scripts DDL manuais obrigatórios no boot; na primeira execução em um banco novo, defina `SPRING_JPA_HIBERNATE_DDL_AUTO=update` para que as tabelas e índices sejam gerados automaticamente pelo Hibernate.
> - A chave `JWT_SECRET` deve conter **no mínimo 32 bytes (256 bits)**. Chaves menores impedirão a subida da aplicação pelo validador do JJWT.

---

## 10. Execução e Testes

### 10.1 Execução Local com Maven
```bash
# 1. Configurar JDK 21+ no PATH (exemplo local)
export JAVA_HOME=$HOME/.jdks/openjdk-27.0.1.0.3 PATH=$JAVA_HOME/bin:$PATH

# 2. Criar e configurar o .env a partir do template
cp .env.example .env
# Configure suas credenciais de banco e defina SPRING_JPA_HIBERNATE_DDL_AUTO=update

# 3. Executar a suíte de testes unitários isolados
./mvnw test -Dtest=TransacaoServiceTest,ContaServiceTest,CategoriaServiceTest,UsuarioServiceTest

# 4. Iniciar a API Spring Boot
./mvnw spring-boot:run
```

### 10.2 Execução com Docker e Docker Compose
O projeto inclui suporte completo a contêineres:
- **[`Dockerfile`](file:///var/home/anderson/IdeaProjects/Financial-Manager/Dockerfile):** Compilação otimizada em multi-stage build usando Eclipse Temurin 21 Jammy, executando a aplicação sob usuário seguro e sem privilégios (`spring:spring`) com flags de JVM para contêiner (`-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0`).
- **[`docker-compose.yml`](file:///var/home/anderson/IdeaProjects/Financial-Manager/docker-compose.yml):** Orquestra o banco MySQL 8.4 (com healthcheck ativo na porta 3306), o backend na porta 8080 e o frontend na porta 80 dentro da rede isolada `financial-network`.

```bash
# Subir todo o ambiente com build automático dos contêineres
docker compose up -d --build

# Verificar logs da API
docker compose logs -f backend

# Parar os serviços
docker compose down
```
