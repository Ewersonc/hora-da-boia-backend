"""US07 a US15: disponibilidade, reservas do cliente, balcão, agenda e status."""
from concurrent.futures import ThreadPoolExecutor
import uuid

from conftest import Cliente, cadastrar_cliente, escolher_data_e_horarios


def test_disponibilidade_e_publica_e_lista_horarios():
    dia, horarios = escolher_data_e_horarios(2)
    assert len(horarios) >= 1
    assert all(len(h) == 5 for h in horarios)


def test_grupo_maior_que_a_maior_mesa_retorna_mensagem():
    resposta = Cliente().get("/api/disponibilidade?data=2999-01-01&pessoas=500")
    assert resposta.status_code == 409


def test_pessoas_invalido_retorna_400():
    dia, _ = escolher_data_e_horarios(2)
    assert Cliente().get(f"/api/disponibilidade?data={dia}&pessoas=0").status_code == 400


def test_cliente_cria_reserva_e_ve_em_minhas_reservas(cliente):
    dia, horarios = escolher_data_e_horarios(2)
    resposta = cliente.post("/api/reservas", json={"data": dia, "hora": horarios[0], "pessoas": 2})
    assert resposta.status_code == 201, resposta.text
    reserva = resposta.json()
    assert reserva["status"] == "CONFIRMADA"
    assert len(reserva["codigo"]) == 6

    minhas = cliente.get("/api/reservas/minhas").json()
    assert [r["codigo"] for r in minhas] == [reserva["codigo"]]


def test_cliente_cancela_a_propria_reserva(cliente):
    dia, horarios = escolher_data_e_horarios(2)
    reserva = cliente.post("/api/reservas", json={"data": dia, "hora": horarios[0], "pessoas": 2}).json()
    cancelada = cliente.post(f"/api/reservas/{reserva['id']}/cancelamento")
    assert cancelada.status_code == 200
    assert cancelada.json()["status"] == "CANCELADA"


def test_cliente_nao_cancela_reserva_de_outro_cliente(cliente):
    dia, horarios = escolher_data_e_horarios(2)
    reserva = cliente.post("/api/reservas", json={"data": dia, "hora": horarios[0], "pessoas": 2}).json()
    outro, _ = cadastrar_cliente()
    assert outro.post(f"/api/reservas/{reserva['id']}/cancelamento").status_code == 404


def test_cliente_nao_tem_duas_reservas_sobrepostas(cliente):
    dia, horarios = escolher_data_e_horarios(2, minimo_de_horarios=2)
    assert cliente.post("/api/reservas", json={"data": dia, "hora": horarios[0], "pessoas": 2}).status_code == 201
    segunda = cliente.post("/api/reservas", json={"data": dia, "hora": horarios[1], "pessoas": 2})
    assert segunda.status_code == 409


def test_alterar_reserva_mantem_o_codigo(cliente):
    dia, horarios = escolher_data_e_horarios(2, minimo_de_horarios=1)
    reserva = cliente.post("/api/reservas", json={"data": dia, "hora": horarios[0], "pessoas": 2}).json()
    resposta = cliente.put(f"/api/reservas/{reserva['id']}", json={"data": dia, "hora": horarios[0], "pessoas": 3})
    assert resposta.status_code == 200, resposta.text
    assert resposta.json()["codigo"] == reserva["codigo"]
    assert resposta.json()["numeroPessoas"] == 3


def test_balcao_agenda_e_status(atendente):
    dia, horarios = escolher_data_e_horarios(2)
    resposta = atendente.post("/api/atendimento/reservas", json={
        "nome": "Cliente de Balcão", "telefone": "83988880000", "data": dia, "hora": horarios[0], "pessoas": 2})
    assert resposta.status_code == 201, resposta.text
    reserva = resposta.json()

    agenda = atendente.get(f"/api/agenda?data={dia}").json()
    assert reserva["codigo"] in [r["codigo"] for r in agenda]

    assert atendente.patch(f"/api/reservas/{reserva['id']}/status", json={"status": "COMPARECEU"}).status_code == 200
    # status final não pode ser alterado (RN09)
    assert atendente.patch(f"/api/reservas/{reserva['id']}/status", json={"status": "NAO_COMPARECEU"}).status_code == 409


def test_balcao_sem_nome_retorna_400(atendente):
    dia, horarios = escolher_data_e_horarios(2)
    resposta = atendente.post("/api/atendimento/reservas", json={
        "nome": "", "telefone": "83988880000", "data": dia, "hora": horarios[0], "pessoas": 2})
    assert resposta.status_code == 400


def test_historico_do_administrador(admin, cliente):
    dia, horarios = escolher_data_e_horarios(2)
    cliente.post("/api/reservas", json={"data": dia, "hora": horarios[0], "pessoas": 2})
    resposta = admin.get(f"/api/reservas?de={dia}&ate={dia}&status=CONFIRMADA")
    assert resposta.status_code == 200
    assert len(resposta.json()) >= 1


def test_concorrencia_apenas_uma_reserva_vence_a_ultima_mesa(admin, atendente):
    """TEC06: várias reservas simultâneas disputam a única mesa que comporta o grupo."""
    capacidade = 70
    mesa = admin.post("/api/mesas", json={"identificacao": f"Concorrencia-{uuid.uuid4().hex[:6]}", "capacidade": capacidade})
    assert mesa.status_code == 201, mesa.text
    mesa_id = mesa.json()["id"]

    dia, horarios = escolher_data_e_horarios(capacidade)
    tentativas = 8
    clientes = [cadastrar_cliente()[0] for _ in range(tentativas)]
    reservas_criadas = []
    try:
        def reservar(c):
            return c.post("/api/reservas", json={"data": dia, "hora": horarios[0], "pessoas": capacidade})

        with ThreadPoolExecutor(max_workers=tentativas) as pool:
            respostas = list(pool.map(reservar, clientes))

        codigos = [r.status_code for r in respostas]
        reservas_criadas = [r.json()["id"] for r in respostas if r.status_code == 201]
        assert codigos.count(201) == 1, codigos
        assert codigos.count(409) == tentativas - 1, codigos
    finally:
        for reserva_id in reservas_criadas:
            atendente.post(f"/api/reservas/{reserva_id}/cancelamento")
        admin.patch(f"/api/mesas/{mesa_id}/desativacao")
