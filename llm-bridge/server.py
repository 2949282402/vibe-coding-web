import json
import logging
import os
import urllib.error
import urllib.request
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://127.0.0.1:11434").rstrip("/")
DEFAULT_MODEL = os.getenv("OLLAMA_MODEL", "qwen3.5:4b").strip() or "qwen3.5:4b"
DEFAULT_TIMEOUT_SECONDS = int(os.getenv("OLLAMA_TIMEOUT_SECONDS", "600"))
SERVER_PORT = int(os.getenv("PORT", "8090"))
LOG_FILE = os.getenv("BRIDGE_LOG_FILE", "/logs/llm-bridge.txt")


logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s",
    handlers=[
        logging.StreamHandler(),
        logging.FileHandler(LOG_FILE, encoding="utf-8"),
    ],
)
LOGGER = logging.getLogger("llm-bridge")


def json_response(handler: BaseHTTPRequestHandler, status: int, payload: dict) -> None:
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    handler.send_response(status)
    handler.send_header("Content-Type", "application/json; charset=utf-8")
    handler.send_header("Content-Length", str(len(body)))
    handler.end_headers()
    handler.wfile.write(body)


def stream_response_headers(handler: BaseHTTPRequestHandler) -> None:
    handler.send_response(HTTPStatus.OK)
    handler.send_header("Content-Type", "application/x-ndjson; charset=utf-8")
    handler.send_header("Cache-Control", "no-cache")
    handler.send_header("Connection", "close")
    handler.end_headers()


def stream_json_line(handler: BaseHTTPRequestHandler, payload: dict) -> None:
    line = (json.dumps(payload, ensure_ascii=False) + "\n").encode("utf-8")
    handler.wfile.write(line)
    handler.wfile.flush()


def read_json(handler: BaseHTTPRequestHandler) -> dict:
    transfer_encoding = str(handler.headers.get("Transfer-Encoding", "") or "").lower()
    if "chunked" in transfer_encoding:
        chunks = []
        while True:
            line = handler.rfile.readline().strip()
            if not line:
                continue
            chunk_size = int(line.split(b";", 1)[0], 16)
            if chunk_size == 0:
                handler.rfile.readline()
                break
            chunks.append(handler.rfile.read(chunk_size))
            handler.rfile.read(2)
        raw = b"".join(chunks)
        return json.loads(raw.decode("utf-8")) if raw else {}

    length = int(handler.headers.get("Content-Length", "0") or "0")
    if length <= 0:
        return {}
    raw = handler.rfile.read(length)
    if not raw:
        return {}
    return json.loads(raw.decode("utf-8"))


def build_prompt(payload: dict) -> str:
    prompt = str(payload.get("prompt", "") or "").strip()
    if prompt:
        return prompt

    system_prompt = str(payload.get("systemPrompt", "") or "").strip()
    user_prompt = str(payload.get("userPrompt", "") or "").strip()
    question = str(payload.get("question", "") or "").strip()
    context = str(payload.get("context", "") or "").strip()

    sections = []
    if system_prompt:
        sections.append(system_prompt)
    if question:
        sections.append(f"Question:\n{question}")
    if context:
        sections.append(f"Context:\n{context}")
    if user_prompt:
        sections.append(user_prompt)
    return "\n\n".join(section for section in sections if section).strip()


def call_ollama_generate(payload: dict) -> dict:
    model = str(payload.get("model", "") or DEFAULT_MODEL).strip() or DEFAULT_MODEL
    prompt = build_prompt(payload)
    if not prompt:
        raise ValueError("prompt is required")

    timeout = int(payload.get("timeoutSeconds", DEFAULT_TIMEOUT_SECONDS) or DEFAULT_TIMEOUT_SECONDS)
    temperature = float(payload.get("temperature", 0.2) or 0.2)

    request_body = json.dumps(
        {
            "model": model,
            "prompt": prompt,
            "stream": False,
            "options": {
                "temperature": temperature,
            },
        },
        ensure_ascii=False,
    ).encode("utf-8")

    request = urllib.request.Request(
        url=f"{OLLAMA_BASE_URL}/api/generate",
        data=request_body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response_body = response.read().decode("utf-8")
            data = json.loads(response_body or "{}")
            LOGGER.info("generate ok model=%s done=%s", model, bool(data.get("done", False)))
            return {
                "model": model,
                "content": str(data.get("response", "") or "").strip(),
                "done": bool(data.get("done", False)),
                "doneReason": str(data.get("done_reason", "") or "").strip(),
                "source": "ollama-native-generate",
            }
    except urllib.error.HTTPError as exc:
        error_body = exc.read().decode("utf-8", errors="replace")
        LOGGER.exception("generate http error model=%s code=%s", model, exc.code)
        raise RuntimeError(f"Ollama HTTP {exc.code}: {error_body}") from exc
    except urllib.error.URLError as exc:
        LOGGER.exception("generate connection error model=%s", model)
        raise RuntimeError(f"Ollama connection failed: {exc.reason}") from exc


def stream_ollama_generate(handler: BaseHTTPRequestHandler, payload: dict) -> None:
    model = str(payload.get("model", "") or DEFAULT_MODEL).strip() or DEFAULT_MODEL
    prompt = build_prompt(payload)
    if not prompt:
        raise ValueError("prompt is required")

    timeout = int(payload.get("timeoutSeconds", DEFAULT_TIMEOUT_SECONDS) or DEFAULT_TIMEOUT_SECONDS)
    temperature = float(payload.get("temperature", 0.2) or 0.2)

    request_body = json.dumps(
        {
            "model": model,
            "prompt": prompt,
            "stream": True,
            "options": {
                "temperature": temperature,
            },
        },
        ensure_ascii=False,
    ).encode("utf-8")

    request = urllib.request.Request(
        url=f"{OLLAMA_BASE_URL}/api/generate",
        data=request_body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    accumulated = []
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            stream_response_headers(handler)
            while True:
                raw_line = response.readline()
                if not raw_line:
                    break
                line = raw_line.decode("utf-8").strip()
                if not line:
                    continue

                data = json.loads(line)
                delta = str(data.get("response", "") or "")
                if delta:
                    accumulated.append(delta)
                    stream_json_line(
                        handler,
                        {
                            "model": model,
                            "delta": delta,
                            "done": False,
                            "source": "ollama-native-generate",
                        },
                    )

                if data.get("done"):
                    LOGGER.info("stream ok model=%s done=true", model)
                    stream_json_line(
                        handler,
                        {
                            "model": model,
                            "content": "".join(accumulated).strip(),
                            "done": True,
                            "doneReason": str(data.get("done_reason", "") or "").strip(),
                            "source": "ollama-native-generate",
                        },
                    )
                    return

            stream_json_line(
                handler,
                {
                    "model": model,
                    "content": "".join(accumulated).strip(),
                    "done": True,
                    "doneReason": "stop",
                    "source": "ollama-native-generate",
                },
            )
    except urllib.error.HTTPError as exc:
        error_body = exc.read().decode("utf-8", errors="replace")
        LOGGER.exception("stream http error model=%s code=%s", model, exc.code)
        raise RuntimeError(f"Ollama HTTP {exc.code}: {error_body}") from exc
    except urllib.error.URLError as exc:
        LOGGER.exception("stream connection error model=%s", model)
        raise RuntimeError(f"Ollama connection failed: {exc.reason}") from exc


class BridgeHandler(BaseHTTPRequestHandler):
    server_version = "hejulian-llm-bridge/1.0"

    def do_GET(self) -> None:  # noqa: N802
        if self.path == "/health":
            LOGGER.info("health check")
            json_response(
                self,
                HTTPStatus.OK,
                {
                    "status": "UP",
                    "ollamaBaseUrl": OLLAMA_BASE_URL,
                    "defaultModel": DEFAULT_MODEL,
                },
            )
            return
        json_response(self, HTTPStatus.NOT_FOUND, {"error": "not found"})

    def do_POST(self) -> None:  # noqa: N802
        if self.path not in {"/generate", "/generate/stream"}:
            json_response(self, HTTPStatus.NOT_FOUND, {"error": "not found"})
            return

        try:
            payload = read_json(self)
            LOGGER.info("request path=%s model=%s", self.path, str(payload.get("model", "") or DEFAULT_MODEL).strip() or DEFAULT_MODEL)
            if self.path == "/generate/stream":
                stream_ollama_generate(self, payload)
                return
            result = call_ollama_generate(payload)
            if not result.get("content"):
                json_response(
                    self,
                    HTTPStatus.BAD_GATEWAY,
                    {
                        "error": "empty response from ollama",
                        "source": result.get("source", "ollama-native-generate"),
                    },
                )
                return
            json_response(self, HTTPStatus.OK, result)
        except ValueError as exc:
            LOGGER.warning("bad request path=%s error=%s", self.path, exc)
            json_response(self, HTTPStatus.BAD_REQUEST, {"error": str(exc)})
        except Exception as exc:  # noqa: BLE001
            LOGGER.exception("request failed path=%s", self.path)
            json_response(self, HTTPStatus.BAD_GATEWAY, {"error": str(exc), "source": "ollama-native-generate"})

    def log_message(self, format: str, *args) -> None:
        return


if __name__ == "__main__":
    server = ThreadingHTTPServer(("0.0.0.0", SERVER_PORT), BridgeHandler)
    LOGGER.info("LLM bridge listening on 0.0.0.0:%s, Ollama=%s, model=%s", SERVER_PORT, OLLAMA_BASE_URL, DEFAULT_MODEL)
    server.serve_forever()
