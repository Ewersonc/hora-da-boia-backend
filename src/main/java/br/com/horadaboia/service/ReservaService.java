package br.com.horadaboia.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import br.com.horadaboia.dto.ReservaBalcaoRequest;
import br.com.horadaboia.dto.ReservaRequest;
import br.com.horadaboia.dto.ReservaResponse;
import br.com.horadaboia.exception.RecursoNaoEncontradoException;
import br.com.horadaboia.exception.RegraNegocioException;
import br.com.horadaboia.model.ConfiguracaoReserva;
import br.com.horadaboia.model.HorarioFuncionamento;
import br.com.horadaboia.model.Mesa;
import br.com.horadaboia.model.Perfil;
import br.com.horadaboia.model.Reserva;
import br.com.horadaboia.model.StatusReserva;
import br.com.horadaboia.model.Usuario;
import br.com.horadaboia.repository.MesaRepository;
import br.com.horadaboia.repository.ReservaRepository;
import br.com.horadaboia.repository.UsuarioRepository;
import br.com.horadaboia.security.UsuarioAutenticado;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de reserva: criação sem conflito, alteração, cancelamento e status (RF09 a RF17, RN01 a RN10).
 *
 * A criação e a alteração rodam em transação com isolamento READ COMMITTED. Primeiro as mesas candidatas
 * são bloqueadas (SELECT ... FOR UPDATE), da menor para a maior, e só então o conflito de horário é checado.
 * Assim, duas requisições disputando a mesma mesa são serializadas (RNF06).
 */
@Service
public class ReservaService {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_CODIGO = 6;

    private final ReservaRepository reservaRepository;
    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ConfiguracaoService configuracaoService;
    private final Clock clock;
    private final SecureRandom aleatorio = new SecureRandom();

    public ReservaService(ReservaRepository reservaRepository, MesaRepository mesaRepository,
                          UsuarioRepository usuarioRepository, ConfiguracaoService configuracaoService, Clock clock) {
        this.reservaRepository = reservaRepository;
        this.mesaRepository = mesaRepository;
        this.usuarioRepository = usuarioRepository;
        this.configuracaoService = configuracaoService;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ criação

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservaResponse criarParaCliente(UsuarioAutenticado autor, ReservaRequest r) {
        Usuario cliente = buscarUsuario(autor.id());
        ConfiguracaoReserva cfg = configuracaoService.configuracao();
        Reserva reserva = criar(cfg, cliente, null, null, cliente, r.data(), r.hora(), r.pessoas());
        return resposta(reserva, cfg);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservaResponse criarNoBalcao(UsuarioAutenticado autor, ReservaBalcaoRequest r) {
        Usuario atendente = buscarUsuario(autor.id());
        ConfiguracaoReserva cfg = configuracaoService.configuracao();
        Reserva reserva = criar(cfg, null, r.nome().trim(), r.telefone().trim(), atendente,
                r.data(), r.hora(), r.pessoas());
        return resposta(reserva, cfg);
    }

    private Reserva criar(ConfiguracaoReserva cfg, Usuario cliente, String nomeConvidado, String telefoneConvidado,
                          Usuario criadaPor, LocalDate data, LocalTime hora, int pessoas) {
        LocalDateTime inicio = data.atTime(hora);
        LocalDateTime fim = inicio.plusMinutes(cfg.getDuracaoMinutos());
        validarPeriodo(cfg, inicio, fim);
        if (cliente != null
                && reservaRepository.existeConflitoDoCliente(cliente.getId(), inicio, fim, ReservaRepository.SEM_RESERVA)) {
            throw new RegraNegocioException("Você já tem uma reserva confirmada que se sobrepõe a este horário.");
        }
        Mesa mesa = escolherMesa(pessoas, inicio, fim, ReservaRepository.SEM_RESERVA);

        LocalDateTime agora = LocalDateTime.now(clock);
        Reserva reserva = new Reserva();
        reserva.setCodigo(gerarCodigo());
        reserva.setUsuario(cliente);
        reserva.setNomeConvidado(nomeConvidado);
        reserva.setTelefoneConvidado(telefoneConvidado);
        reserva.setMesa(mesa);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        reserva.setNumeroPessoas(pessoas);
        reserva.setStatus(StatusReserva.CONFIRMADA);
        reserva.setCriadaPor(criadaPor);
        reserva.setCriadoEm(agora);
        reserva.setAtualizadoEm(agora);
        return reservaRepository.save(reserva);
    }

    // ------------------------------------------------------------------ alteração

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservaResponse alterar(UsuarioAutenticado autor, Long id, ReservaRequest r) {
        Reserva reserva = buscar(id);
        exigirDono(autor, reserva);
        exigirConfirmada(reserva, "Apenas reservas confirmadas podem ser alteradas.");
        ConfiguracaoReserva cfg = configuracaoService.configuracao();
        if (!dentroDoPrazo(reserva, cfg)) {
            throw new RegraNegocioException("A alteração só é permitida até " + descreverPrazo(cfg)
                    + " antes do horário marcado.");
        }

        LocalDateTime inicio = r.data().atTime(r.hora());
        LocalDateTime fim = inicio.plusMinutes(cfg.getDuracaoMinutos());
        validarPeriodo(cfg, inicio, fim);
        if (reservaRepository.existeConflitoDoCliente(autor.id(), inicio, fim, reserva.getId())) {
            throw new RegraNegocioException("Você já tem uma reserva confirmada que se sobrepõe a este horário.");
        }
        Mesa mesa = escolherMesa(r.pessoas(), inicio, fim, reserva.getId());

        reserva.setMesa(mesa);
        reserva.setInicio(inicio);
        reserva.setFim(fim);
        reserva.setNumeroPessoas(r.pessoas());
        reserva.setAtualizadoEm(LocalDateTime.now(clock));
        return resposta(reserva, cfg);
    }

    // ------------------------------------------------------------------ cancelamento e status

    @Transactional
    public ReservaResponse cancelar(UsuarioAutenticado autor, Long id) {
        Reserva reserva = buscar(id);
        exigirConfirmada(reserva, "Apenas reservas confirmadas podem ser canceladas.");
        ConfiguracaoReserva cfg = configuracaoService.configuracao();
        if (autor.perfil() == Perfil.CLIENTE) {
            exigirDono(autor, reserva);
            if (!dentroDoPrazo(reserva, cfg)) {
                throw new RegraNegocioException("O cancelamento só é permitido até " + descreverPrazo(cfg)
                        + " antes do horário marcado.");
            }
        }
        reserva.setStatus(StatusReserva.CANCELADA);
        reserva.setAtualizadoEm(LocalDateTime.now(clock));
        return resposta(reserva, cfg);
    }

    @Transactional
    public ReservaResponse atualizarStatus(UsuarioAutenticado autor, Long id, StatusReserva novoStatus) {
        if (novoStatus == StatusReserva.CONFIRMADA) {
            throw new RegraNegocioException("Uma reserva não volta ao status confirmada.");
        }
        if (novoStatus == StatusReserva.CANCELADA) {
            return cancelar(autor, id);
        }
        Reserva reserva = buscar(id);
        exigirConfirmada(reserva, "A reserva já tem um status final e não pode ser alterada.");
        reserva.setStatus(novoStatus);
        reserva.setAtualizadoEm(LocalDateTime.now(clock));
        return resposta(reserva, configuracaoService.configuracao());
    }

    /** Marca como não compareceu as reservas confirmadas que passaram da tolerância de atraso (RF17, RN07). */
    @Transactional
    public int marcarNaoComparecimento() {
        ConfiguracaoReserva cfg = configuracaoService.configuracao();
        LocalDateTime agora = LocalDateTime.now(clock);
        return reservaRepository.marcarNaoComparecimento(agora.minusMinutes(cfg.getToleranciaAtrasoMinutos()), agora);
    }

    // ------------------------------------------------------------------ consultas

    @Transactional(readOnly = true)
    public List<ReservaResponse> minhas(UsuarioAutenticado autor) {
        ConfiguracaoReserva cfg = configuracaoService.configuracao();
        return reservaRepository.minhasReservas(autor.id()).stream().map(r -> resposta(r, cfg)).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> agenda(LocalDate data) {
        return noPeriodo(data, data, null);
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> historico(LocalDate de, LocalDate ate, StatusReserva status) {
        return noPeriodo(de, ate, status);
    }

    private List<ReservaResponse> noPeriodo(LocalDate de, LocalDate ate, StatusReserva status) {
        ConfiguracaoReserva cfg = configuracaoService.configuracao();
        LocalDateTime inicio = de.atStartOfDay();
        LocalDateTime fim = ate.plusDays(1).atStartOfDay();
        List<Reserva> reservas = status == null
                ? reservaRepository.noPeriodo(inicio, fim)
                : reservaRepository.noPeriodoComStatus(inicio, fim, status);
        return reservas.stream().map(r -> resposta(r, cfg)).toList();
    }

    // ------------------------------------------------------------------ regras internas

    private void validarPeriodo(ConfiguracaoReserva cfg, LocalDateTime inicio, LocalDateTime fim) {
        if (inicio.getMinute() % DisponibilidadeService.PASSO_MINUTOS != 0 || inicio.getSecond() != 0) {
            throw new RegraNegocioException("Horário inválido: use intervalos de "
                    + DisponibilidadeService.PASSO_MINUTOS + " minutos.");
        }
        HorarioFuncionamento horario = configuracaoService.horarioDoDia(inicio.getDayOfWeek().getValue())
                .filter(HorarioFuncionamento::isAberto)
                .orElseThrow(() -> new RegraNegocioException("O restaurante não abre nesta data."));
        boolean mesmoDia = fim.toLocalDate().equals(inicio.toLocalDate());
        if (!mesmoDia || inicio.toLocalTime().isBefore(horario.getAbertura())
                || fim.toLocalTime().isAfter(horario.getFechamento())) {
            throw new RegraNegocioException("O horário escolhido está fora do horário de funcionamento.");
        }
        LocalDateTime agora = LocalDateTime.now(clock);
        if (inicio.isBefore(agora.plusMinutes(cfg.getAntecedenciaMinMinutos()))) {
            throw new RegraNegocioException("A reserva exige antecedência mínima de "
                    + cfg.getAntecedenciaMinMinutos() + " minutos.");
        }
        if (inicio.toLocalDate().isAfter(agora.toLocalDate().plusDays(cfg.getAntecedenciaMaxDias()))) {
            throw new RegraNegocioException("A reserva só pode ser feita com até "
                    + cfg.getAntecedenciaMaxDias() + " dias de antecedência.");
        }
    }

    /** Bloqueia as mesas candidatas e devolve a menor que esteja livre no período (RN02, RN03). */
    private Mesa escolherMesa(int pessoas, LocalDateTime inicio, LocalDateTime fim, long ignorarReservaId) {
        List<Mesa> candidatas = mesaRepository.bloquearCandidatas(pessoas);
        if (candidatas.isEmpty()) {
            throw new RegraNegocioException("O restaurante não comporta grupos de " + pessoas + " pessoas.");
        }
        return candidatas.stream()
                .filter(mesa -> !reservaRepository.existeConflitoNaMesa(mesa.getId(), inicio, fim, ignorarReservaId))
                .findFirst()
                .orElseThrow(() -> new RegraNegocioException("Horário indisponível. Escolha outro horário."));
    }

    private boolean dentroDoPrazo(Reserva reserva, ConfiguracaoReserva cfg) {
        LocalDateTime limite = reserva.getInicio().minusMinutes(cfg.getPrazoCancelamentoMinutos());
        return !LocalDateTime.now(clock).isAfter(limite);
    }

    private String descreverPrazo(ConfiguracaoReserva cfg) {
        int minutos = cfg.getPrazoCancelamentoMinutos();
        return minutos % 60 == 0 ? (minutos / 60) + " hora(s)" : minutos + " minutos";
    }

    private void exigirDono(UsuarioAutenticado autor, Reserva reserva) {
        boolean dono = reserva.getUsuario() != null && reserva.getUsuario().getId().equals(autor.id());
        if (autor.perfil() == Perfil.CLIENTE && !dono) {
            throw new RecursoNaoEncontradoException("Reserva não encontrada.");
        }
    }

    private void exigirConfirmada(Reserva reserva, String mensagem) {
        if (reserva.getStatus() != StatusReserva.CONFIRMADA) {
            throw new RegraNegocioException(mensagem);
        }
    }

    private Reserva buscar(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private String gerarCodigo() {
        for (int tentativa = 0; tentativa < 10; tentativa++) {
            StringBuilder codigo = new StringBuilder(TAMANHO_CODIGO);
            for (int i = 0; i < TAMANHO_CODIGO; i++) {
                codigo.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            }
            if (!reservaRepository.existsByCodigo(codigo.toString())) {
                return codigo.toString();
            }
        }
        throw new IllegalStateException("Não foi possível gerar um código único para a reserva.");
    }

    private ReservaResponse resposta(Reserva reserva, ConfiguracaoReserva cfg) {
        Usuario cliente = reserva.getUsuario();
        String nome = cliente != null ? cliente.getNome() : reserva.getNomeConvidado();
        String telefone = cliente != null ? cliente.getTelefone() : reserva.getTelefoneConvidado();
        boolean noPrazo = reserva.getStatus() == StatusReserva.CONFIRMADA && dentroDoPrazo(reserva, cfg);
        return new ReservaResponse(reserva.getId(), reserva.getCodigo(), reserva.getInicio(), reserva.getFim(),
                reserva.getNumeroPessoas(), reserva.getStatus(), reserva.getMesa().getIdentificacao(),
                nome, telefone, noPrazo, noPrazo);
    }
}
