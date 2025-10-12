import subprocess
import requests
import os

OLLAMA_URL = os.getenv("OLLAMA_URL", "http://localhost:11434")

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
        json={"model": "phi3", "prompt": prompt},
        stream=True
    )
    print("🤖 Feedback da IA:\n")
    for line in response.iter_lines():
        if line:
            data = line.decode("utf-8")
            print(data)

if __name__ == "__main__":
    diff = get_diff()
    if diff:
        analyze_with_ai(diff)
    else:
        print("Nenhuma modificação detectada no commit atual.")
