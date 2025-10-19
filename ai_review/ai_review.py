import subprocess
import requests
import os
import time

OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434")
MODEL_NAME = os.getenv("OLLAMA_MODEL", "phi3")

def ensure_model_downloaded():
    """Baixa automaticamente o modelo se ele ainda não existir"""
    print(f"🔍 Verificando se o modelo '{MODEL_NAME}' está disponível...")

    try:
        resp = requests.get(f"{OLLAMA_URL}/api/tags")
        resp.raise_for_status()
        models = resp.json().get("models", [])

        if any(m.get("name") == MODEL_NAME for m in models):
            print(f"✅ Modelo '{MODEL_NAME}' já disponível.")
            return True
        else:
            print(f"⬇️ Modelo '{MODEL_NAME}' não encontrado. Baixando...")
            pull = requests.post(f"{OLLAMA_URL}/api/pull", json={"name": MODEL_NAME}, stream=True)
            for line in pull.iter_lines():
                if line:
                    print(line.decode("utf-8"))
            print("✅ Download concluído.")
            return True

    except requests.exceptions.RequestException as e:
        print(f"❌ Erro ao verificar/baixar modelo: {e}")
        return False

def get_diff():
    """Coleta o diff do último commit"""
    diff = subprocess.getoutput("git diff HEAD~1 HEAD")
    return diff if diff.strip() else None

def analyze_with_ai(diff):
    """Envia o diff para o modelo do Ollama"""
    prompt = f"""
Você é um revisor de código especializado em Java Spring Boot.
Analise o diff abaixo e aponte:
- possíveis bugs
- violações de boas práticas
- melhorias de performance ou segurança
- clareza e legibilidade

Responda em formato de lista.

Código:
{diff}
"""
    response = requests.post(
        f"{OLLAMA_URL}/api/generate",
        json={"model": MODEL_NAME, "prompt": prompt},
        stream=True
    )
    print("🤖 Feedback da IA:\n")
    for line in response.iter_lines():
        if line:
            print(line.decode("utf-8"))

if __name__ == "__main__":
    if ensure_model_downloaded():
        time.sleep(2)  # pequena espera para o modelo ser carregado
        diff = get_diff()
        if diff:
            analyze_with_ai(diff)
        else:
            print("Nenhuma modificação detectada no commit atual.")
    else:
        print("❌ Não foi possível preparar o modelo. Encerrando.")
