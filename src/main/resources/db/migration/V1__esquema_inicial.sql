-- Esquema inicial do Hora da Boia (MySQL 8.0.16 ou superior).
-- Veja o documento Modelo de Dados para a descrição de cada tabela.

CREATE TABLE usuario (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    telefone    VARCHAR(20),
    senha_hash  VARCHAR(100) NOT NULL,
    perfil      VARCHAR(20)  NOT NULL,
    ativo       BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_usuario_perfil   CHECK (perfil IN ('CLIENTE', 'ATENDENTE', 'ADMINISTRADOR')),
    CONSTRAINT chk_usuario_telefone CHECK (perfil <> 'CLIENTE' OR telefone IS NOT NULL)
) ENGINE=InnoDB;

CREATE TABLE mesa (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    identificacao  VARCHAR(30) NOT NULL UNIQUE,
    capacidade     INT         NOT NULL,
    ativa          BOOLEAN     NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_mesa_capacidade CHECK (capacidade > 0)
) ENGINE=InnoDB;

CREATE TABLE horario_funcionamento (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    dia_semana  SMALLINT NOT NULL UNIQUE,
    aberto      BOOLEAN  NOT NULL DEFAULT TRUE,
    abertura    TIME,
    fechamento  TIME,
    CONSTRAINT chk_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horario_intervalo CHECK (aberto = FALSE OR (abertura IS NOT NULL AND fechamento IS NOT NULL AND abertura < fechamento))
) ENGINE=InnoDB;

CREATE TABLE configuracao_reserva (
    id                          SMALLINT PRIMARY KEY,
    duracao_minutos             INT NOT NULL DEFAULT 120,
    antecedencia_min_minutos    INT NOT NULL DEFAULT 60,
    antecedencia_max_dias       INT NOT NULL DEFAULT 30,
    prazo_cancelamento_minutos  INT NOT NULL DEFAULT 120,
    tolerancia_atraso_minutos   INT NOT NULL DEFAULT 15,
    CONSTRAINT chk_config_id         CHECK (id = 1),
    CONSTRAINT chk_config_duracao    CHECK (duracao_minutos > 0),
    CONSTRAINT chk_config_ant_min    CHECK (antecedencia_min_minutos >= 0),
    CONSTRAINT chk_config_ant_max    CHECK (antecedencia_max_dias > 0),
    CONSTRAINT chk_config_prazo      CHECK (prazo_cancelamento_minutos >= 0),
    CONSTRAINT chk_config_tolerancia CHECK (tolerancia_atraso_minutos >= 0)
) ENGINE=InnoDB;

CREATE TABLE reserva (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo              VARCHAR(10)  NOT NULL UNIQUE,
    usuario_id          BIGINT,
    nome_convidado      VARCHAR(100),
    telefone_convidado  VARCHAR(20),
    mesa_id             BIGINT       NOT NULL,
    inicio              DATETIME     NOT NULL,
    fim                 DATETIME     NOT NULL,
    numero_pessoas      INT          NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'CONFIRMADA',
    criada_por_id       BIGINT       NOT NULL,
    criado_em           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reserva_usuario    FOREIGN KEY (usuario_id)    REFERENCES usuario (id),
    CONSTRAINT fk_reserva_mesa       FOREIGN KEY (mesa_id)       REFERENCES mesa (id),
    CONSTRAINT fk_reserva_criada_por FOREIGN KEY (criada_por_id) REFERENCES usuario (id),
    CONSTRAINT chk_reserva_periodo   CHECK (fim > inicio),
    CONSTRAINT chk_reserva_pessoas   CHECK (numero_pessoas > 0),
    CONSTRAINT chk_reserva_status    CHECK (status IN ('CONFIRMADA', 'COMPARECEU', 'NAO_COMPARECEU', 'CANCELADA')),
    CONSTRAINT chk_reserva_cliente   CHECK (usuario_id IS NOT NULL OR (nome_convidado IS NOT NULL AND telefone_convidado IS NOT NULL))
) ENGINE=InnoDB;

-- usuario_id e criada_por_id já recebem índice automático pela chave estrangeira.
CREATE INDEX idx_reserva_mesa_periodo  ON reserva (mesa_id, inicio, fim);
CREATE INDEX idx_reserva_inicio_status ON reserva (inicio, status);
