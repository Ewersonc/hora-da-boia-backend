# Hora da Boia — Backend

Sistema web de reservas de mesas para restaurante. Trabalho de Engenharia de Software de Ewerson Costa e Darlison Valdivino.

O cliente escolhe data, número de pessoas e horário e recebe um código de reserva. A equipe acompanha a agenda do dia, registra reservas de balcão e controla o comparecimento. O administrador configura mesas, horários e regras. O sistema impede duas reservas na mesma mesa e no mesmo horário, inclusive com requisições simultâneas.


## Funcionalidades

- **Cliente:** cadastro, login, consulta de horários livres, reserva, alteração, cancelamento e lista de reservas.
- **Atendente:** agenda do dia, reserva de balcão (cliente sem cadastro), status de comparecimento e cancelamento.
- **Administrador:** tudo do atendente, mais mesas, horário de funcionamento, parâmetros de reserva, atendentes e histórico.
- **Automático:** a cada minuto, reservas confirmadas que passaram da tolerância de atraso viram "não compareceu".

Regras principais: reserva de 2 horas por padrão, mesa escolhida automaticamente (a menor que comporta o grupo), cancelamento e alteração até 2 horas antes, antecedência de 1 hora a 30 dias. Todos os valores são configuráveis. Veja a Especificação de Requisitos em `docs/fase-1`.

## Tecnologias

Java 21, Spring Boot 3, Spring Data JPA, Spring Security com JWT, MySQL 8, Flyway, Docker, JUnit 5, Testcontainers e pytest. O frontend (HTML, CSS e JavaScript) está em um projeto separado, `hora-da-boia-frontend`, e consome esta API.

## Instalação

Requisitos: Java 21, Maven 3.9 ou superior e Docker.

```
git clone https://github.com/[usuario]/hora-da-boia.git
cd hora-da-boia
```

No IntelliJ, abra a pasta do projeto (File > Open) e selecione o `pom.xml`, importando como projeto Maven.

## Configuração

O perfil `dev` já vem configurado para o MySQL do Docker Compose e não exige nenhuma variável. Para produção, veja `docs/deploy.md`.

| Variável | Uso | Padrão no perfil dev |
|---|---|---|
| `DB_HOST`, `DB_PORT`, `DB_NAME` | Endereço do banco | `localhost`, `3306`, `horadaboia` |
| `DB_USER`, `DB_PASSWORD` | Credenciais do banco | `horadaboia`, `horadaboia` |
| `DB_URL` | URL JDBC completa (perfil `prod`) | sem padrão |
| `JWT_SECRET` | Chave de assinatura dos tokens | chave de desenvolvimento (sem padrão em `prod`) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Administrador inicial | `admin@horadaboia.local` e `admin12345` (somente dev) |
| `PORT` | Porta HTTP (perfil `prod`) | `8080` |
| `CORS_ORIGENS` | Endereço(s) do frontend autorizados a chamar a API, separados por vírgula | `http://localhost:5500`, `http://127.0.0.1:5500`, `http://localhost:3000` |

Nunca faça commit do arquivo `.env`.

## Execução

1. Banco de dados: `docker compose up -d db`
2. Aplicação: `mvn spring-boot:run`
3. Acesse:
   - Documentação da API (Swagger): http://localhost:8080/swagger-ui.html
   - Saúde: http://localhost:8080/actuator/health
4. Para usar a interface, suba o projeto `hora-da-boia-frontend` (veja o README dele). Ele abre em http://localhost:5500 e chama esta API em http://localhost:8080.

No perfil `dev` existem 4 mesas de exemplo e o administrador `admin@horadaboia.local` (senha `admin12345`). Crie atendentes pela tela de Atendentes e clientes pela tela de cadastro (no frontend).

Para subir a API e o banco em contêineres: `docker compose --profile app up --build` (exige o `JWT_SECRET` no arquivo `.env`). O frontend continua sendo servido à parte.

Se preferir o wrapper do Maven (`./mvnw`), gere-o uma vez com `mvn wrapper:wrapper` e faça o commit dos arquivos criados.

## Testes

```
mvn test
cd testes-api && pip install -r requirements.txt && pytest -v
```

O `mvn test` usa o Testcontainers, que sobe um MySQL temporário e exige o Docker em execução. O `pytest` precisa da aplicação rodando. O plano completo está em `TestCasePlan.md` e os registros de execução em `docs/evidencias-de-testes.md`.

## Manutenção

- **Logs:** `docker compose logs -f app` (em contêiner) ou a saída do `mvn spring-boot:run`.
- **Banco de dados:** o esquema muda apenas por novas migrações em `src/main/resources/db/migration`. Uma migração já aplicada nunca é editada.
- **Backup:** `docker compose exec db mysqldump -u root -p horadaboia > backup.sql`.
- **Atualização:** `git pull` e `docker compose --profile app up -d --build`.
- **Parâmetros do restaurante:** horários, duração, antecedências, prazo de cancelamento e tolerância se ajustam pela tela de Configurações, sem alterar o código.

## Organização

```
src/main/java/br/com/horadaboia/
  config/       relógio, OpenAPI e administrador inicial
  security/     filtro JWT e regras de acesso
  controller/   rotas REST
  service/      regras de negócio e transações
  repository/   Spring Data JPA
  model/        entidades e enumerados
  dto/          objetos de entrada e saída da API
  exception/    exceções de negócio e tratamento de erros
  scheduler/    agendador de não comparecimento
src/main/resources/
  db/migration/ migrações Flyway
testes-api/     testes de API em pytest
docs/           documentos das fases, registro de IA e implantação
```

## Documentação

- Fases 1 e 2: `docs/fase-1` e `docs/fase-2`
- Plano de testes: `TestCasePlan.md`
- Implantação: `docs/deploy.md`
- Manual do usuário: `docs/Manual_do_Usuário.pdf`
- Uso de IA: `docs/Registro_de_Utilização_da_IA.pdf` e `docs/Relatório_de_Utilização_de_IA.pdf`

## Commits

Padrão Conventional Commits em português, com a história no final:

```
feat(reserva): impede sobreposição de horários na mesma mesa (US08)
```
