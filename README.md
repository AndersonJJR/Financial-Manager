# Notas do Autor

Este é um projeto Java com Spring Boot, para fins de Estudo.
Por meio deste projeto pude testar, aprimorar e rever alguns conceitos conhecidos.

## Como conduzi esse projeto?

Primeiramente separei as minhas responsabilidades e as responsabilidades da IA.

### O que foram as minhas responsabilidades?

- Pensar o que seria o sistema criado.
- Realizar a coleta de requisitos deste sistema, o que ele deveria cumprir.
- Seguindo a modelagem de software, separar entre requisitos funcionais e não funcionais.
- Detalhar para a IA em seu processo operacional de geração de código, que deva seguir padrões de qualidade de software e arquitetura de pastas. Como testes, DRY (Don't repeat yourself) e SOLID.
- Após a geração, realizar uma verificação rápida no código gerado.
- Acompanhar variáveis de ambiente e não deixar chaves sensíveis expostas.
- Realizar testes de interface, validando se tudo está atuando conforme requisitado, tanto as funcionalidades básicas quanto as regras de negócio.

### O que ficou responsável pela IA?

Gerar código, corrigir seus bugs e documentar tanto as funcionalidades quanto os bugs. Os bugs servem para entender o erro e evitá-lo em aplicações futuras.

## Ferramentas de IA que utilizei nesse projeto

- **Orca** → Uma ADE (Agent Development Enviroment), no qual auxilia na orquestração de múltiplas IAs.
- **Antigravity CLI** → Devido a ter o plano PRO para estudantes, possuo uma gama maior de ferramentas de IA e modelos. Mas consome tokens que só serão atualizados semanalmente.
- **Opencode CLI** → A fim de economizar nos tokens, separei tarefas muitos simples para os modelos chineses de IA, como DeepSeek 4.1 Flash Free e GLM 5.3 Free. Ambos modelos gratuitos, mas com contexto máximo de 200k tokens no momento que realizei esse código.
- **Google Stitch** → Ferramenta do ecossistema da Google para design de interfaces, podendo exportar para o Figma. Gosto bastante dessa ferramenta para design.

## Meu fluxo de trabalho

1. Realizar a modelagem deste sistema
2. Gerar um back-end com atributos sólidos e de qualidade
3. Validar
4. Gerar as telas no Stitch
5. Importar os arquivos das telas no código
6. Gerar e implementar as telas e suas funcionalidades, seguindo o back-end como base
7. Testes e Validações
8. Correções e Finalização da documentação
9. Gerar o dockerfile dos projetos e o docker-compose
10. Testes finais
11. Fim

## Notas finais

Com o passar do tempo pude me abrir mais com a IA e deixa-la tomar mais espaço como ferramenta operacional, enquanto pude me dedicar mais à parte de arquitetura e estudo. Mas para isso solidifiquei minha base, fiz muitos projetos que ficaram inacabados, repetição e compreensão de código para que possa entender o fluxo de desenvolvimento dela, isso em questões de Java com Spring Boot.

---

# Financial Manager — API REST

![Status](https://img.shields.io/badge/Status-Conclu%C3%ADdo-success?style=for-the-badge)
![Finalidade](https://img.shields.io/badge/Finalidade-Estudo-blue?style=for-the-badge)

API RESTful completa e robusta para gestão e controle financeiro pessoal, desenvolvida para fornecer segurança em ambiente multiusuário, cálculos financeiros consistentes e consolidação orçamentária analítica.

### Tecnologias Utilizadas

![Java 21](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot 4.1.2](https://img.shields.io/badge/Spring_Boot-4.1.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Security 6](https://img.shields.io/badge/Spring_Security-6-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-HS256_Stateless-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Hibernate / JPA](https://img.shields.io/badge/Hibernate-Data_JPA-59666C?style=for-the-badge&logo=hibernate&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Docker Compose](https://img.shields.io/badge/Docker_Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)
![Lombok](https://img.shields.io/badge/Lombok-BC002D?style=for-the-badge&logo=projectlombok&logoColor=white)

---

## Funcionalidades Principais

### 1. Autenticação e Segurança Multiusuário
- **Cadastro e Login com JWT:** Autenticação stateless com tokens assinados (HS256) e senhas criptografadas com **BCrypt**.
- **Gestão de Perfil e Senha:** Endpoints para consulta de dados, alteração de nome e troca de senha exigindo a validação da senha atual.
- **Isolamento Estrito de Recursos:** Todas as contas, categorias e movimentações são estritamente atreladas ao usuário autenticado, impedindo qualquer acesso cruzado indevido entre usuários.

### 2. Gestão Inteligente de Contas Financeiras
- **Múltiplos Tipos de Contas:** Suporte para Conta Corrente, Poupança, Carteira Física, Corretora/Investimento e Cartão de Crédito.
- **Cálculo Dinâmico de Saldo:** Saldo atual calculado em tempo real a partir do saldo inicial somado às receitas efetivadas e subtraído das despesas efetivadas.
- **Reativação Transparente:** Ao tentar criar uma conta com nome idêntico a uma inativada anteriormente, o sistema reativa o registro existente mantendo o histórico de lançamentos anteriores.
- **Exclusão Lógica:** Inativação segura preservando a integridade das transações passadas.

### 3. Categorização Orçamentária
- **Classificação por Tipo:** Categorias segregadas entre `RECEITA` e `DESPESA`, com suporte a identificação visual por código hexadecimal de cor.
- **Constraint de Integridade:** Prevenção de duplicidade por usuário e reativação automática de categorias arquivadas.

### 4. Gestão Avançada de Transações
- **Lançamentos Avulsos:** Registro de receitas e despesas com definição de competência, pagamento, conta e categoria vinculadas.
- **Compras Parceladas com Rateio Preciso:**
  - Modo `VALOR_TOTAL`: Divide o montante entre as parcelas e ajusta centavos de arredondamento automaticamente na 1ª parcela.
  - Modo `VALOR_PARCELA`: Multiplica o valor informado pela quantidade de parcelas.
  - Periodicidades variadas (`SEMANAL`, `QUINZENAL`, `MENSAL`, `BIMESTRAL`, `TRIMESTRAL`, `SEMESTRAL`, `ANUAL`).
  - Opção de iniciar com a primeira parcela já paga/efetivada.
- **Transações Recorrentes:** Projeção em lote de despesas ou receitas fixas periódicas (aluguel, assinaturas, salários) de 1 a 60 ocorrências.
- **Cancelamento em Massa de Parcelas Pendentes:** Cancela todas as parcelas ou ocorrências futuras ainda não pagas de um mesmo grupo de parcelamento/recorrência com um único comando, mantendo intacto o histórico do que já foi quitado.
- **Baixa Contábil Direta:** Endpoint específico para efetivação (`PATCH /efetivar`), registrando a data de pagamento e recalculando saldos instantaneamente.

### 5. Resumos Mensais e Relatórios Analíticos
- **Consolidação em Tempo Real:** Totalizadores mensais de entradas e saídas separadas por status (realizadas vs. previstas) e saldo líquido apurado.
- **Detalhamento por Categoria:** Distribuição percentual e nominal de gastos por categoria para análise visual de orçamento.

### 6. Exportação de Extratos com Proteção de Dados
- **Formatos CSV e JSON:** Geração de extratos filtrados por período e contas selecionadas.
- **Mitigação contra CSV Formula Injection (CWE-1236):** Sanitização automática de caracteres que iniciam fórmulas executáveis (`=`, `+`, `-`, `@`, `\t`), neutralizando riscos em planilhas eletrônicas (Excel e LibreOffice).

### 7. Pipeline de Validações e Qualidade de Código
- **Chain of Responsibility:** Validadores desacoplados para integridade de status contábil, valor positivo, compatibilidade de tipo entre transação e categoria, propriedade do recurso e regras de parcelamento.
- **Respostas de Erro Padronizadas (RFC 7807/9457):** Retorno padronizado de detalhes de erro (`Problem Details`) para códigos 400, 401, 403, 404, 409, 422 e 500.

---

## Como Executar

### Pré-requisitos
- **Java 21** instalado
- **MySQL 8.4** em execução (ou Docker)

### Opção 1: Executando com Docker Compose (Recomendado)
```bash
# Sobe o banco MySQL e a API compilada em contêiner
docker compose up -d --build

# Acompanhar logs da aplicação
docker compose logs -f backend
```

### Opção 2: Executando Localmente via Maven
```bash
# 1. Configurar variáveis de ambiente (.env a partir do exemplo)
cp .env.example .env

# 2. Executar os testes unitários
./mvnw test -Dtest=TransacaoServiceTest,ContaServiceTest,CategoriaServiceTest,UsuarioServiceTest

# 3. Iniciar a API
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080/api/v1`.

> Para documentação técnica completa de todas as rotas e payloads, consulte o arquivo [`documentation.md`](file:///var/home/anderson/IdeaProjects/Financial-Manager/documentation.md).

---

## Autor

<p align="center">
  <img src="https://avatars.githubusercontent.com/u/206952189?v=4" width="130" height="130" style="border-radius: 50%;" alt="Anderson Chaves Junior" />
</p>

<p align="center">
  <strong>Anderson Chaves Junior</strong>
</p>

<p align="center">
  <a href="https://www.linkedin.com/in/andersonchavesjunior/" target="_blank">
    <img src="https://img.shields.io/badge/LinkedIn-Anderson%20Chaves%20Junior-0077B5?style=for-the-badge&logo=linkedin&logoColor=white" alt="LinkedIn" />
  </a>
</p>
