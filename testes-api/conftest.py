"""Fixtures dos testes de API (TEC05).

Os testes chamam a aplicação já em execução, pelo lado de fora, como um cliente real.
Variáveis de ambiente:
  API_URL         endereço da aplicação (padrão http://localhost:8080)
  ADMIN_EMAIL     e-mail do administrador (padrão: o administrador do perfil dev)
  ADMIN_PASSWORD  senha do administrador
"""
import os
import random
import uuid
from datetime import date, timedelta

import pytest
import requests

API_URL = os.environ.get("API_URL", "http://localhost:8080").rstrip("/")
ADMIN_EMAIL = os.environ.get("ADMIN_EMAIL", "admin@horadaboia.local")
ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD", "admin12345")


class Cliente:
    """Pequeno cliente HTTP com token opcional."""

    def __init__(self, token=None):
        self.token = token

    def _cabecalhos(self):
        return {"Authorization": f"Bearer {self.token}"} if self.token else {}

    def get(self, caminho, **kwargs):
        return requests.get(API_URL + caminho, headers=self._cabecalhos(), timeout=30, **kwargs)

    def post(self, caminho, json=None):
        return requests.post(API_URL + caminho, json=json, headers=self._cabecalhos(), timeout=30)

    def put(self, caminho, json=None):
        return requests.put(API_URL + caminho, json=json, headers=self._cabecalhos(), timeout=30)

    def patch(self, caminho, json=None):
        return requests.patch(API_URL + caminho, json=json, headers=self._cabecalhos(), timeout=30)


def email_unico(prefixo="cliente"):
    return f"{prefixo}-{uuid.uuid4().hex[:10]}@teste.local"


def cadastrar_cliente(email=None):
    email = email or email_unico()
    resposta = requests.post(API_URL + "/api/auth/cadastro", timeout=30, json={
        "nome": "Cliente Pytest", "email": email, "telefone": "83999990000", "senha": "senha12345"})
    assert resposta.status_code == 201, resposta.text
    return Cliente(resposta.json()["token"]), email


def escolher_data_e_horarios(pessoas, minimo_de_horarios=1):
    """Procura uma data futura com horários livres, para os testes não dependerem do dia em que rodam."""
    publico = Cliente()
    datas = list(range(3, 28))
    random.shuffle(datas)
    for dias in datas:
        dia = (date.today() + timedelta(days=dias)).isoformat()
        resposta = publico.get(f"/api/disponibilidade?data={dia}&pessoas={pessoas}")
        if resposta.status_code == 200 and len(resposta.json()["horarios"]) >= minimo_de_horarios:
            return dia, [h[:5] for h in resposta.json()["horarios"]]
    pytest.skip("Nenhuma data com horários livres. Verifique mesas e horário de funcionamento.")


@pytest.fixture(scope="session")
def admin():
    resposta = requests.post(API_URL + "/api/auth/login", timeout=30,
                             json={"email": ADMIN_EMAIL, "senha": ADMIN_PASSWORD})
    assert resposta.status_code == 200, (
        "Não foi possível entrar como administrador. Confira ADMIN_EMAIL e ADMIN_PASSWORD. " + resposta.text)
    return Cliente(resposta.json()["token"])


@pytest.fixture()
def atendente(admin):
    email = email_unico("atendente")
    resposta = admin.post("/api/atendentes", json={"nome": "Atendente Pytest", "email": email, "senha": "senha12345"})
    assert resposta.status_code == 201, resposta.text
    login = requests.post(API_URL + "/api/auth/login", timeout=30, json={"email": email, "senha": "senha12345"})
    assert login.status_code == 200, login.text
    return Cliente(login.json()["token"])


@pytest.fixture()
def cliente():
    return cadastrar_cliente()[0]
