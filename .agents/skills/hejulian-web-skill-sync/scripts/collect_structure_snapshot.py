from __future__ import annotations

import argparse
from pathlib import Path
from typing import Iterable


def list_names(path: Path, *, dirs_only: bool = False, files_only: bool = False) -> list[str]:
    if not path.exists():
        return []
    items = []
    for item in sorted(path.iterdir(), key=lambda p: (not p.is_dir(), p.name.lower())):
        if item.name in {".git", "node_modules", "target", "__pycache__"}:
            continue
        if dirs_only and not item.is_dir():
            continue
        if files_only and not item.is_file():
            continue
        items.append(item.name + ("/" if item.is_dir() else ""))
    return items


def list_relative_files(path: Path, base: Path) -> list[str]:
    if not path.exists():
        return []
    files = []
    for item in sorted(path.rglob("*"), key=lambda p: str(p).lower()):
        if any(part in {".git", "node_modules", "target", "__pycache__"} for part in item.parts):
            continue
        if item.is_file():
            files.append(str(item.relative_to(base)).replace("\\", "/"))
    return files


def section(title: str, items: Iterable[str]) -> str:
    lines = [f"## {title}"]
    collected = list(items)
    if not collected:
        lines.append("- <无>")
    else:
        lines.extend(f"- `{item}`" for item in collected)
    return "\n".join(lines)


def build_snapshot(repo_root: Path) -> str:
    frontend_src = repo_root / "frontend" / "src"
    backend_java = repo_root / "backend" / "src" / "main" / "java" / "com" / "hejulian" / "blog"
    mapper_root = repo_root / "backend" / "src" / "main" / "resources" / "mapper"

    parts = [
        "# 当前仓库结构快照",
        "",
        f"- 仓库根目录：`{repo_root}`",
        section("根目录条目", list_names(repo_root)),
        section("frontend/src 子目录", list_names(frontend_src, dirs_only=True)),
        section("frontend/src/api 文件", list_names(frontend_src / "api", files_only=True)),
        section("frontend/src/stores 文件", list_names(frontend_src / "stores", files_only=True)),
        section("frontend/src/views 页面", list_relative_files(frontend_src / "views", frontend_src)),
        section("backend 主包子目录", list_names(backend_java, dirs_only=True)),
        section("backend/controller 文件", list_names(backend_java / "controller", files_only=True)),
        section("backend/controller/admin 文件", list_names(backend_java / "controller" / "admin", files_only=True)),
        section("backend/service 文件", list_names(backend_java / "service", files_only=True)),
        section("backend/agent 文件", list_relative_files(backend_java / "agent", backend_java)),
        section("backend/rag 文件", list_relative_files(backend_java / "rag", backend_java)),
        section("mapper XML", list_names(mapper_root, files_only=True)),
        section("deploy 目录", list_relative_files(repo_root / "deploy", repo_root)),
        section("llm-bridge 文件", list_relative_files(repo_root / "llm-bridge", repo_root)),
        section("todo 文档", list_relative_files(repo_root / "todo", repo_root)),
    ]
    return "\n\n".join(parts) + "\n"


def main() -> None:
    default_root = Path(__file__).resolve().parents[4]
    parser = argparse.ArgumentParser(description="采集 hejulian-web 当前目录结构快照")
    parser.add_argument("--repo-root", type=Path, default=default_root, help="仓库根目录")
    parser.add_argument("--output", type=Path, help="可选：将快照写入指定 Markdown 文件")
    args = parser.parse_args()

    repo_root = args.repo_root.resolve()
    content = build_snapshot(repo_root)

    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(content, encoding="utf-8")
    else:
        print(content, end="")


if __name__ == "__main__":
    main()
