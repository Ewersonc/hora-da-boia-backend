"""US03 a US06: mesas, horário de funcionamento, parâmetros e atendentes."""
import uuid

import requests

from conftest import API_URL, Cliente, email_unico


def test_horario_de_funcionamento_tem_sete_dias(admin):
    horarios = admin.get("/api/configuracoes/horarios").json()
    assert [h["diaSemana"] for h in horarios] == [1, 2, 3, 4, 5, 6, 7]


def test_parametros_de_reserva_tem_os_cinco_campos(admin):
    parametros = admin.get("/api/configuracoes/reservas").json()
    assert set(parametros) == {"duracaoMinutos", "antecedenciaMinMinutos", "antecedenciaMaxDias",
                               "prazoCancelamentoMinutos", "toleranciaAtrasoMinutos"}


def test_parametro_invalido_retorna_400(admin):
    parametros = admin.get("/api/configuracoes/reservas").json()
    parametros["duracaoMinutos"] = 0
    assert admin.put("/api/configuracoes/reservas", json=parametros).status_code == 400


def test_mesa_duplicada_retorna_409(admin):
    corpo = {"identificacao": f"Dup-{uuid.uuid4().hex[:6]}", "capacidade": 4}
    primeira = admin.post("/api/mesas", json=corpo)
    assert primeira.status_code == 201
    assert admin.post("/api/mesas", json=corpo).status_code == 409
    admin.patch(f"/api/mesas/{primeira.json()['id']}/desativacao")


def test_mesa_com_capacidade_zero_retorna_400(admin):
    assert admin.post("/api/mesas", json={"identificacao": "Invalida", "capacidade": 0}).status_code == 400


def test_atendente_criado_entra_e_desativado_nao_entra_mais(admin):
    email = email_unico("atendente")
    criado = admin.post("/api/atendentes", json={"nome": "Atendente", "email": email, "senha": "senha12345"})
    assert criado.status_code == 201
    login = {"email": email, "senha": "senha12345"}
    assert requests.post(API_URL + "/api/auth/login", json=login, timeout=30).status_code == 200

    assert admin.patch(f"/api/atendentes/{criado.json()['id']}/desativacao").status_code == 200
    assert requests.post(API_URL + "/api/auth/login", json=login, timeout=30).status_code == 401
