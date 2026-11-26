# Plano e casos de teste: Hora da Boia

Este documento define os cenários de teste do Hora da Boia, os resultados esperados e como cada caso é executado. Ele deriva dos critérios de aceitação das Histórias de Usuário e das regras de negócio (RN01 a RN12) da Especificação de Requisitos.

## 1. Estratégia

| Nível | Ferramenta | O que valida | Onde fica |
|---|---|---|---|
| Integração da API com banco real | JUnit 5, Spring Boot Test, MockMvc e Testcontainers (MySQL 8.4) | Regras de negócio, permissões, validações, migrações Flyway e concorrência | `src/test/java` |
| API pelo lado de fora | pytest e requests | A aplicação em execução, como um cliente real | `testes-api/` |
| Interface | Testes manuais roteirizados | Fluxos das telas, mensagens e responsividade | Seção 4 deste documento |

Nos testes JUnit, o relógio da aplicação é substituído por um relógio controlável (`RelogioMutavel`), fixado em segunda-feira, 16/11/2026, às 17:00, no fuso de Brasília. Assim, prazos, antecedências e tolerâncias são verificados sem depender da hora real. Antes de cada teste, o banco volta a um estado conhecido: reservas apagadas, mesas desativadas e parâmetros restaurados.

## 2. Como executar

Testes JUnit (precisam do Docker em execução):

```
mvn test
```

Testes de API (com a aplicação rodando em `http://localhost:8080` no perfil `dev`):

```
cd testes-api
pip install -r requirements.txt
pytest -v
```

Para testar uma aplicação publicada: `API_URL=https://seu-endereco ADMIN_EMAIL=... ADMIN_PASSWORD=... pytest -v`.

## 3. Casos de teste automatizados

Status possíveis: A executar, Aprovado, Reprovado (com o defeito registrado em `docs/evidencias-de-testes.md`).

| ID | História | Cenário | Resultado esperado | Automação (JUnit) | Status |
|---|---|---|---|---|---|
| TC-001 | US01 | Cadastro com dados válidos, depois login | Conta criada com perfil CLIENTE e login com sucesso | `AutenticacaoIntegracaoTests.cadastroValidoPermiteLogin` | A executar |
| TC-002 | US01 | Cadastro com e-mail já existente | 409 e mensagem de e-mail já utilizado | `emailDuplicadoRetorna409` | A executar |
| TC-003 | US01 | Cadastro com telefone em branco e senha de 3 caracteres | 400 com os campos `senha` e `telefone` | `cadastroInvalidoRetorna400` | A executar |
| TC-004 | US02 | Login com senha errada | 401 com mensagem genérica | `senhaErradaRetorna401` | A executar |
| TC-005 | US02 | Rota protegida sem token | 401 | `rotaProtegidaSemTokenRetorna401` | A executar |
| TC-006 | US02 | Cliente acessa mesas, atendentes e agenda | 403 em todas | `clienteNaoAcessaAreasDaEquipe` | A executar |
| TC-007 | US07 | Consulta de disponibilidade com uma mesa livre | 7 horários, de 30 em 30 minutos, a partir das 18:00 | `ReservaIntegracaoTests.disponibilidadeListaHorariosComMesaLivre` | A executar |
| TC-008 | US07 | Grupo maior que a maior mesa | Mensagem de que o restaurante não comporta o grupo | `grupoMaiorQueAMaiorMesa` | A executar |
| TC-009 | US07 | Data além de 30 dias | Mensagem sobre a janela de reservas | `dataForaDaJanela` | A executar |
| TC-010 | US08, US09 | Cliente cria reserva | 201, status CONFIRMADA, código e presença em Minhas reservas | `clienteCriaReservaConfirmada` | A executar |
| TC-011 | US08 | Segundo cliente reserva a última mesa no mesmo horário | 409 de horário indisponível | `ultimaMesaOcupadaGeraConflito` | A executar |
| TC-012 | US08 | Reservas que apenas se encostam (fim igual ao início) | Ambas aceitas (RN03) | `reservasQueEncostamSaoPermitidas` | A executar |
| TC-013 | US08 | Mesmo cliente com duas reservas sobrepostas | 409 (RN08) | `clienteNaoTemDuasReservasSobrepostas` | A executar |
| TC-014 | US08 | Reserva às 12:00, fora do funcionamento | 409 (RN04) | `foraDoHorarioDeFuncionamento` | A executar |
| TC-015 | US08 | Reserva sem a antecedência mínima | 409 (RN05) | `semAntecedenciaMinima` | A executar |
| TC-016 | US11 | Cliente cancela com mais de 2 horas de antecedência | CANCELADA e horário volta a ficar livre | `clienteCancelaDentroDoPrazo` | A executar |
| TC-017 | US11 | Cancelamento fora do prazo pelo cliente e pelo atendente | Cliente recebe 409, atendente cancela (RN06) | `cancelamentoForaDoPrazo` | A executar |
| TC-018 | US11 | Cliente cancela reserva de outro cliente | 404 | `clienteNaoMexeEmReservaAlheia` | A executar |
| TC-019 | US10 | Alteração válida de horário e número de pessoas | 200 e o código da reserva se mantém | `alteracaoMantemOCodigo` | A executar |
| TC-020 | US10 | Alteração para horário indisponível | 409 e a reserva original é mantida (RN10) | `alteracaoIndisponivelMantemOriginal` | A executar |
| TC-021 | US10 | Alteração fora do prazo | 409 (RN06) | `alteracaoForaDoPrazo` | A executar |
| TC-022 | US12, US13 | Atendente registra reserva de balcão | 201 e a reserva aparece na agenda com nome e telefone | `reservaDeBalcaoApareceNaAgenda` | A executar |
| TC-023 | US12 | Reserva de balcão sem nome | 400 | `reservaDeBalcaoSemNome` | A executar |
| TC-024 | US14 | Atendente marca COMPARECEU e depois tenta mudar | Primeira 200, segunda 409 (RN09) | `statusFinalNaoMuda` | A executar |
| TC-025 | US14 | Reserva marcada como não compareceu | A mesa volta a ficar livre | `naoCompareceuLiberaAMesa` | A executar |
| TC-026 | US15 | Agendador após a tolerância de 15 minutos | Antes: nada muda. Depois: NAO_COMPARECEU (RN07) | `naoComparecimentoAutomatico` | A executar |
| TC-027 | US15 | Agendador e reserva que já compareceu | Reserva não é alterada | `agendadorNaoAlteraReservaQueCompareceu` | A executar |
| TC-028 | US08 | 8 reservas simultâneas para a última mesa (TEC06, RNF06) | Exatamente 1 aceita (201) e 7 recusadas (409) | `apenasUmaReservaSimultaneaVenceAUltimaMesa` | A executar |
| TC-029 | US04 | Mesa com identificação repetida | 409 | `AdministracaoIntegracaoTests.mesaDuplicada` | A executar |
| TC-030 | US04 | Mesa com capacidade zero | 400 | `mesaComCapacidadeInvalida` | A executar |
| TC-031 | US04 | Desativar mesa com reserva futura confirmada | 409 (RN11) | `mesaComReservaFuturaNaoDesativa` | A executar |
| TC-032 | US04 | Mesa desativada na disponibilidade | Mesa não é considerada | `mesaDesativadaNaoEntraNaDisponibilidade` | A executar |
| TC-033 | US05 | Dia marcado como fechado | Disponibilidade sem horários | `diaFechadoNaoOfereceHorarios` | A executar |
| TC-034 | US05 | Fechamento antes da abertura | 409 | `fechamentoAntesDaAbertura` | A executar |
| TC-035 | US06 | Duração alterada para 60 minutos e valor inválido | 9 horários na consulta e 400 para duração zero | `parametrosDeReserva` | A executar |
| TC-036 | US03 | Atendente criado entra e, desativado, não entra mais | Login 200, depois 401 | `atendenteCriadoEDesativado` | A executar |
| TC-037 | US03 | Atendente acessa mesas e atendentes | 403 | `atendenteNaoGerenciaMesasNemAtendentes` | A executar |
| TC-038 | US16 | Histórico por período e status | Apenas as reservas que atendem ao filtro | `historicoDoAdministrador` | A executar |
| TC-039 | Migrações | Contexto sobe e Flyway roda no MySQL | 7 horários, 1 configuração e 1 administrador | `MigracoesIntegracaoTests` | A executar |

Os mesmos cenários de autenticação, reservas, balcão, concorrência e administração têm testes equivalentes em pytest, nos arquivos `testes-api/test_autenticacao.py`, `test_reservas.py` e `test_administracao.py`.

## 4. Casos de teste manuais de interface

| ID | História | Passos | Resultado esperado | Status |
|---|---|---|---|---|
| TM-01 | US01, US02 | Abrir a página inicial, cadastrar um cliente e sair. Entrar de novo | Cadastro leva à tela de nova reserva. O login volta à mesma tela. Erros aparecem na tela | A executar |
| TM-02 | US02 | Entrar como cliente, atendente e administrador | Cada perfil vê apenas o seu menu. Abrir `/mesas.html` como cliente volta ao login | A executar |
| TM-03 | US07, US08 | Como cliente, escolher data e pessoas, ver horários, escolher um e confirmar | Aparece o código da reserva e o resumo | A executar |
| TM-04 | US08 | Tentar reservar um horário que acabou de ser ocupado por outro cliente | Mensagem de indisponibilidade e lista de horários atualizada | A executar |
| TM-05 | US09 | Abrir Minhas reservas | Abas Futuras e Passadas, com código, data, horário e status. Botões desativados fora do prazo | A executar |
| TM-06 | US10 | Alterar data, horário e número de pessoas de uma reserva | A reserva muda e o código se mantém | A executar |
| TM-07 | US11 | Cancelar uma reserva pela janela de confirmação | Status Cancelada e mesa liberada | A executar |
| TM-08 | US13, US14 | Como atendente, abrir a agenda, selecionar uma reserva e marcar Compareceu | Status atualizado na lista. Reservas com status final não têm ações | A executar |
| TM-09 | US12 | Registrar uma reserva de balcão | Código exibido para informar ao cliente e reserva na agenda | A executar |
| TM-10 | US04 | Como administrador, criar, editar e desativar uma mesa | Lista atualizada. Mesa com reserva futura não desativa | A executar |
| TM-11 | US05, US06 | Alterar horários e parâmetros e consultar a disponibilidade como cliente | Novos valores refletidos nas consultas | A executar |
| TM-12 | US03 | Criar, editar e desativar um atendente | Atendente desativado não consegue entrar | A executar |
| TM-13 | RNF07 | Abrir as telas em um celular (ou com a janela estreita) e em Chrome, Firefox e Edge | Telas legíveis e utilizáveis, sem rolagem horizontal da página | A executar |
| TM-14 | RNF05 | Cadastrar um cliente com o nome `<b>teste</b>` e ver a agenda | O texto aparece literalmente, sem formatação nem script | A executar |

## 5. Critérios de aprovação

- Todos os casos Must e Should passam, e os que reprovarem têm defeito registrado e corrigido.
- O teste de concorrência (TC-028) passa em pelo menos 5 execuções seguidas.
- Nenhum defeito de severidade alta fica aberto na entrega.

## 6. Evidências

As execuções, os defeitos encontrados e as correções são registrados em `docs/evidencias-de-testes.md`. Guardem também os relatórios do Maven (`target/surefire-reports`), a saída do pytest e as capturas de tela dos testes manuais.
