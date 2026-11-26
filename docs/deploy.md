# Implantação do Hora da Boia

Este projeto é só o backend (API). O frontend fica em outro projeto (`hora-da-boia-frontend`), que é estático e pode ser publicado separadamente; depois de publicá-lo, informe o endereço dele em `CORS_ORIGENS` e o endereço da API em `js/config.js` do front.

A API é empacotada em Docker. Qualquer lugar que rode contêineres e ofereça um MySQL 8 (8.0.16 ou superior) serve. Os planos gratuitos e as regras de cada hospedagem mudam com frequência, então confirmem as condições atuais antes de escolher.

## Variáveis de ambiente (perfil `prod`)

| Variável | Descrição |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DB_URL` | URL JDBC, por exemplo `jdbc:mysql://HOST:3306/horadaboia?serverTimezone=America/Sao_Paulo` |
| `DB_USER`, `DB_PASSWORD` | Credenciais do banco |
| `JWT_SECRET` | Chave aleatória com pelo menos 32 caracteres |
| `CORS_ORIGENS` | Endereço público do frontend, por exemplo `https://hora-da-boia.exemplo.com` (vários, separados por vírgula, sem barra no final) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Criam o administrador na primeira execução (se ainda não existir um) |
| `PORT` | Porta HTTP, quando a plataforma define uma (padrão 8080) |

Gere uma chave aleatória com `openssl rand -base64 48`. Nunca faça commit desses valores.

## Opção A: servidor com Docker Compose

Funciona em qualquer máquina virtual com Docker (nuvem ou servidor da faculdade).

1. Copie o projeto para o servidor (`git clone`).
2. Crie o arquivo `.env` a partir do `.env.example` e troque todos os valores, principalmente `DB_PASSWORD`, `DB_ROOT_PASSWORD`, `JWT_SECRET`, `ADMIN_PASSWORD` e `CORS_ORIGENS`.
3. Suba tudo: `docker compose --profile app up -d --build`.
4. Confira `http://SERVIDOR:8080/actuator/health`. A resposta deve conter `"status":"UP"`.
5. Para usar HTTPS e um domínio, coloque na frente um proxy reverso (por exemplo Caddy ou Nginx) apontando para a porta 8080.

Observação: o `docker-compose.yml` publica a porta 3306 do MySQL para facilitar o desenvolvimento. Em um servidor público, remova a linha `ports` do serviço `db` ou bloqueie a porta no firewall.

## Opção B: plataforma de hospedagem de contêineres

1. Crie um banco MySQL na plataforma (ou use um MySQL gerenciado) e anote host, porta, usuário e senha.
2. Crie um serviço a partir do repositório do GitHub, usando o `Dockerfile` da raiz.
3. Defina as variáveis da tabela acima no painel da plataforma.
4. Faça o deploy e confira `/actuator/health`.

## Depois do deploy

1. Abra o endereço público do frontend, entre com `ADMIN_EMAIL` e `ADMIN_PASSWORD` e cadastre as mesas e o horário de funcionamento em Mesas e Configurações.
2. Rode os testes de API contra a aplicação publicada:

   ```
   cd testes-api
   API_URL=https://seu-endereco ADMIN_EMAIL=... ADMIN_PASSWORD=... pytest -v
   ```

   Esses testes criam clientes, mesas e reservas de teste. Rode-os antes de cadastrar dados reais, ou apenas em um ambiente de avaliação.
3. Guarde o endereço público e inclua-o no README e na apresentação.

## Manutenção

- **Logs:** `docker compose logs -f app`.
- **Atualização:** `git pull` e `docker compose --profile app up -d --build`. As migrações do banco rodam sozinhas na inicialização.
- **Backup:** `docker compose exec db mysqldump -u root -p horadaboia > backup.sql`.
- **Esquema do banco:** só muda por novas migrações em `src/main/resources/db/migration`. Nunca edite uma migração já aplicada.
