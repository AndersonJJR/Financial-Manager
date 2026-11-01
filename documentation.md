# FinancialManager — Documentação

API REST de gerenciamento financeiro pessoal construída com **Spring Boot 4.1.2** (Java 21, Maven, MySQL). Permite registrar receitas e despesas vinculadas a contas e categorias, tratar compras parceladas e recorrentes, consultar resumos mensais, calcular saldos consolidados e exportar extratos formatados.

---
## 1. Visão geral

| Item | Valor |
|---|---|
| Base URL | `http://localhost:8080/api/v1` |
| Autenticação | JWT stateless (Bearer token, HS256, expiração padrão de 24h) |
| Formato de dados | JSON |
| Banco de dados | MySQL (`localhost:3306/financialmanager`) |
| Idioma da API | Português (mensagens de erro, enums em MAIÚSCULO) |
| CORS | Habilitado para todos os verbos HTTP e origens padrão |

**Multiusuário por design:** todos os recursos (contas, categorias, transações) pertencem ao usuário autenticado — nenhum endpoint expõe dados de outros usuários, e a propriedade é verificada nas camadas de serviço para cada operação.

---
## 2. Autenticação e Usuários — `/auth`

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/auth/registrar` | Registra novo usuário e devolve token JWT (HTTP 201) |
| `POST` | `/auth/login` | Autentica com e-mail + senha e devolve token JWT (HTTP 200) |
| `GET` | `/auth/me` | Dados cadastrais do usuário autenticado (requer token) |
| `PUT` | `/auth/me` | Atualiza o nome do usuário autenticado (requer token) |
| `PUT` | `/auth/alterar-senha` | Altera a senha exigindo validação da senha atual (requer token) |

### 2.1 Registro e Login
**Request de registro:**
```json
{
  "nome": "Anderson Silva",
  "email": "anderson@example.com",
  "senha": "senhaSecreta123"
}
```
- `nome`: 2–120 caracteres (obrigatório).
- `email`: formato válido, ≤ 150 caracteres (obrigatório, case-insensitive).
- `senha`: 6–100 caracteres (armazenada com hash **BCrypt**).

**Resposta:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "expiraEm": "2026-11-02T20:30:00.000Z",
  "usuario": {
    "id": "37518ef5-8c7c-403f-9244-54a93971c09e",
    "nome": "Anderson Silva",
    "email": "anderson@example.com",
    "role": "ROLE_USER",
    "ativo": true,
    "criadoEm": "2026-11-01T17:00:00"
  }
}
```

### 2.2 Atualização de Perfil e Senha
- **`PUT /auth/me`**: Atualiza o nome:
  ```json
  { "nome": "Anderson Silva Junior" }
  ```
- **`PUT /auth/alterar-senha`**: Altera a senha do usuário com confirmação prévia:
  ```json
  {
    "senhaAtual": "senhaSecreta123",
    "novaSenha": "novaSenhaSegura456"
  }
  ```
  *Validações:* Rejeita caso a senha atual informada não confira com o hash gravado ou caso a nova senha seja idêntica à anterior (HTTP 422).

**Uso do token nas demais rotas:**
```http
Authorization: Bearer <token>
```

---
## 3. Contas financeiras — `/contas`

Representam contas bancárias, carteiras, cartões de crédito e investimentos.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/contas` | Cria conta ou reativa conta existente com o mesmo nome (HTTP 201 + `Location`) |
| `GET` | `/contas?ativa=true` | Lista contas com cálculo dinâmico de `saldoAtual` (filtro opcional `ativa`) |
| `GET` | `/contas/{id}` | Busca conta específica por ID com `saldoAtual` calculado |
| `PUT` | `/contas/{id}` | Atualiza dados cadastrais da conta |
| `DELETE` | `/contas/{id}` | Inativação lógica (`ativa=false`), preservando histórico contábil |

**Request de criação/atualização:**
```json
{
  "nome": "Nubank",
  "instituicao": "Banco Nu S.A.",
  "tipo": "CONTA_CORRENTE",
  "saldoInicial": 1000.00,
  "ativa": true
}
```
- `tipo` (obrigatório): `CONTA_CORRENTE`, `POUPANCA`, `CARTEIRA`, `CORRETORA`, `CARTAO_CREDITO`.
- `saldoInicial`: padrão `0.00` caso omitido.

**Resposta com Saldo Atual Consolidado:**
```json
{
  "id": "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d",
  "nome": "Nubank",
  "instituicao": "Banco Nu S.A.",
  "tipo": "CONTA_CORRENTE",
  "saldoInicial": 1000.00,
  "saldoAtual": 1450.50,
  "ativa": true,
  "criadoEm": "2026-11-01T10:00:00",
  "atualizadoEm": "2026-11-01T12:30:00"
}
```
- **Cálculo de `saldoAtual`:** Calculado dinamicamente: `saldoInicial + soma(receitas EFETIVADAS) - soma(despesas EFETIVADAS)`.
- **Reativação transparente:** Caso o usuário tente criar uma conta com nome idêntico a uma conta inativada anteriormente, o sistema reativa a conta existente e atualiza seus dados, preservando a chave estrangeira em todas as transações passadas.

---
## 4. Categorias — `/categorias`

Classificação orçamentária para despesas e receitas.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/categorias` | Cria categoria ou reativa inativa com mesmo nome/tipo (HTTP 201 + `Location`) |
| `GET` | `/categorias?tipo=DESPESA&ativa=true` | Lista categorias com filtros opcionais por tipo e status |
| `GET` | `/categorias/{id}` | Busca categoria por ID |
| `PUT` | `/categorias/{id}` | Atualiza dados da categoria |
| `DELETE` | `/categorias/{id}` | Inativação lógica (`ativa=false`) |

**Request:**
```json
{
  "nome": "Alimentação",
  "descricao": "Supermercado, restaurantes e delivery",
  "corHex": "#FF5733",
  "tipo": "DESPESA",
  "ativa": true
}
```
- `corHex`: formato `#RRGGBB` ou `#RGB` (opcional).
- `tipo`: `RECEITA` ou `DESPESA`.
- **Constraint de unicidade:** Impede categorias ativas duplicadas com a mesma combinação de `(nome, tipo)` para o mesmo usuário. Se houver uma inativa, ela é reativada.

---
## 5. Transações — `/transacoes`

Núcleo de movimentações financeiras.

| Método | Endpoint | Descrição |
|---|---|---|
| `POST` | `/transacoes` | Lançamento avulso (`UNICA`), HTTP 201 + `Location` |
| `POST` | `/transacoes/parceladas` | Gera e persiste em lote parcelas de compras parceladas, HTTP 201 |
| `POST` | `/transacoes/recorrentes` | Projeta ocorrências de receitas/despesas recorrentes, HTTP 201 |
| `GET` | `/transacoes` | Lista transações com filtros avançados |
| `GET` | `/transacoes/{id}` | Detalhes de uma transação |
| `GET` | `/transacoes/grupos/{grupoId}` | Lista parcelas/ocorrências pertencentes a um mesmo grupo |
| `DELETE` | `/transacoes/grupos/{grupoId}/pendentes` | Cancela todas as parcelas pendentes do grupo |
| `PUT` | `/transacoes/{id}` | Atualiza lançamento |
| `PATCH` | `/transacoes/{id}/efetivar?dataPagamento=YYYY-MM-DD` | Baixa/efetiva lançamento pendente |
| `DELETE` | `/transacoes/{id}` | Exclui transação |
| `GET` | `/transacoes/resumo-mensal?ano=2026&mes=10` | Resumo analítico com detalhamento por categoria |
| `GET` | `/transacoes/exportar?dataInicio=...&dataFim=...` | Exporta extrato em CSV ou JSON formatado |

### 5.1 Lançamento avulso
```json
{
  "descricao": "Supermercado Semanal",
  "valor": 350.75,
  "dataCompetencia": "2026-11-01",
  "dataPagamento": null,
  "tipo": "DESPESA",
  "status": "PENDENTE",
  "contaId": "uuid-da-conta",
  "categoriaId": "uuid-da-categoria",
  "observacao": "Alimentos do mês"
}
```

### 5.2 Regras de Consistência de Status
- Se `status == PENDENTE`, `dataPagamento` é obrigatoriamente `null`.
- Se `status == EFETIVADA`, `dataPagamento` deve estar presente (caso omitida na criação/baixa, assume `dataCompetencia` ou data atual).
- Se `status == CANCELADA`, não pode haver `dataPagamento`.
- **Efetivação de canceladas:** Uma transação cancelada não pode ser diretamente efetivada via `PATCH /transacoes/{id}/efetivar` (HTTP 422).

### 5.3 Compra Parcelada (`/transacoes/parceladas`)
- `modoValor`:
  - `VALOR_TOTAL`: Divide o valor entre as parcelas, ajustando os centavos de arredondamento na 1ª parcela.
  - `VALOR_PARCELA`: O valor informado será aplicado em cada parcela individualmente.
- `periodicidade`: `SEMANAL`, `QUINZENAL` (+15 dias), `MENSAL`, `BIMESTRAL`, `TRIMESTRAL`, `SEMESTRAL`, `ANUAL`.
- `primeiraParcelaPaga`: Permite que a 1ª parcela nasça imediatamente `EFETIVADA` e as demais `PENDENTE`.

### 5.4 Resumo Mensal (`GET /transacoes/resumo-mensal`)
Retorna os consolidados de entradas e saídas e a distribuição percentual de despesas por categoria:
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
      "categoriaId": "uuid",
      "categoriaNome": "Alimentação",
      "corHex": "#FF5733",
      "valorNominal": 1200.00,
      "percentual": 40.68,
      "quantidadeTransacoes": 8
    }
  ]
}
```

---
## 6. Exportação de Extrato (`GET /transacoes/exportar`)

Exporta arquivo para download via header `Content-Disposition: attachment; filename="extrato_...csv|json"`.

- **Parâmetros:**
  - `dataInicio` e `dataFim` (obrigatórios, formato ISO `YYYY-MM-DD`).
  - `contasIds` (opcional, repetível: `?contasIds=id1&contasIds=id2`).
  - `formato`: `CSV` (padrão) ou `JSON`.
- **Proteção contra CSV Injection (Formula Injection - CWE-1236):**
  Campos de texto que iniciam com `=`, `+`, `-`, `@` ou caracteres de tabulação são automaticamente prefixados com `'` (apóstrofo) para neutralizar execução indevida de fórmulas ou macros no Microsoft Excel e LibreOffice Calc.
- **JSON Pretty-Printed:**
  Estruturado com totalizadores do período (`totalReceitas`, `totalDespesas`, `saldoPeriodo`) e listagem individualizada.

---
## 7. Configuração e Variáveis de Ambiente (`.env`)

Todas as credenciais sensíveis e parâmetros de conexão foram isolados no arquivo `.env` na raiz do projeto (ignorado pelo git).

O arquivo [`application.properties`](file:///var/home/anderson/IdeaProjects/Financial-Manager/src/main/resources/application.properties) importa automaticamente o `.env` via:
```properties
spring.config.import=optional:file:.env[.properties]
```

### Arquivo `.env` (exemplo baseado em `.env.example`):
```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/financialmanager
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=sua_senha_aqui

JWT_SECRET=coloque_aqui_uma_chave_base64_ou_string_com_no_minimo_32_bytes
JWT_EXPIRATION_MS=86400000
JWT_ISSUER=financial-manager-api
```

> [!IMPORTANT]
> A chave `JWT_SECRET` deve conter no mínimo **256 bits (32 bytes)** de entropia. Caso contrário, a aplicação rejeita a inicialização no boot por segurança.

---
## 8. Tratamento de Erros e Segurança

Todos os erros retornam o padrão `problem-detail`:
```json
{
  "type": "https://financialmanager.anderson.com/errors/422",
  "title": "Violação de regra de negócio",
  "status": 422,
  "detail": "Uma transação cancelada não pode ser diretamente efetivada.",
  "instance": "/api/v1/transacoes/123/efetivar",
  "timestamp": "2026-11-01T17:40:00.000Z"
}
```

- **HTTP 400:** Erros de validação nos campos (`errors: [{ campo, mensagem }]`).
- **HTTP 401:** Token ausente, expirado ou credenciais inválidas.
- **HTTP 403:** Tentativa de acessar ou vincular recurso de outro usuário.
- **HTTP 404:** Recurso não localizado para o usuário autenticado.
- **HTTP 409:** Conflito de integridade relacional ou unicidade de chave.
- **HTTP 422:** Violação de regra de negócio (conta inativa, categoria incompatível, etc.).
- **HTTP 500:** Erro interno sanitizado ("Ocorreu um erro interno inesperado no processamento da solicitação.") sem vazamento de detalhes internos de queries, tabelas ou stacktrace.

---
## 9. Como Executar

```bash
# 1. Definir o JDK 21+ no terminal
export JAVA_HOME=$HOME/.jdks/openjdk-27.0.1.0.3 PATH=$JAVA_HOME/bin:$PATH

# 2. Configurar o arquivo .env a partir do template
cp .env.example .env
# edite o .env com suas credenciais do MySQL e JWT_SECRET

# 3. Executar todos os testes automatizados
./mvnw test

# 4. Iniciar a API
./mvnw spring-boot:run
```
