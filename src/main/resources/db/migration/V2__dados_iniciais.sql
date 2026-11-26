-- Configuração única de reserva (id fixo 1), com os valores padrão do Modelo de Dados.
INSERT INTO configuracao_reserva (id) VALUES (1);

-- Horário de funcionamento inicial, das 18:00 às 23:00 todos os dias.
-- 1 é segunda-feira e 7 é domingo. O administrador ajusta depois pela tela de configurações.
INSERT INTO horario_funcionamento (dia_semana, aberto, abertura, fechamento) VALUES
    (1, TRUE, '18:00:00', '23:00:00'),
    (2, TRUE, '18:00:00', '23:00:00'),
    (3, TRUE, '18:00:00', '23:00:00'),
    (4, TRUE, '18:00:00', '23:00:00'),
    (5, TRUE, '18:00:00', '23:00:00'),
    (6, TRUE, '18:00:00', '23:00:00'),
    (7, TRUE, '18:00:00', '23:00:00');
