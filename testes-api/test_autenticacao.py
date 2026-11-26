"""US01 e US02: cadastro, login e perfis."""
import requests

from conftest import API_URL, Cliente, cadastrar_cliente, email_unico


def test_cadastro_valido_permite_login():
    _, email = cadastrar_cliente()
    resposta = requests.post(API_URL + "/api/auth/login", json={"email": email, "senha": "senha12345"}, timeout=30)
    assert resposta.status_code == 200
    assert resposta.json()["perfil"] == "CLIENTE"


def test_email_duplicado_retorna_409():
    _, email = cadastrar_cliente()
    resposta = requests.post(API_URL + "/api/auth/cadastro", timeout=30, json={
        "nome": "Outro", "email": email, "telefone": "83999990000", "senha": "senha12345"})
    assert resposta.status_code == 409


def test_cadastro_invalido_retorna_400_com_campos():
    resposta = requests.post(API_URL + "/api/auth/cadastro", timeout=30, json={
        "nome": "Fulano", "email": email_unico(), "telefone": "", "senha": "123"})
    assert resposta.status_code == 400
    assert "senha" in resposta.json()["campos"]
    assert "telefone" in resposta.json()["campos"]


def test_login_com_senha_errada_retorna_401_generico():
    _, email = cadastrar_cliente()
    resposta = requests.post(API_URL + "/api/auth/login", json={"email": email, "senha": "errada123"}, timeout=30)
    assert resposta.status_code == 401
    assert resposta.json()["mensagem"] == "E-mail ou senha inválidos."


def test_rota_protegida_sem_token_retorna_401():
    assert Cliente().get("/api/reservas/minhas").status_code == 401


def test_token_invalido_retorna_401():
    assert Cliente("token.invalido.qualquer").get("/api/reservas/minhas").status_code == 401


def test_cliente_nao_acessa_areas_da_equipe(cliente):
    assert cliente.get("/api/mesas").status_code == 403
    assert cliente.get("/api/atendentes").status_code == 403
    assert cliente.get("/api/agenda").status_code == 403


def test_atendente_nao_gerencia_mesas(atendente):
    assert atendente.get("/api/mesas").status_code == 403
    assert atendente.get("/api/atendentes").status_code == 403
