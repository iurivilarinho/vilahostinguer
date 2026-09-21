# -*- coding: utf-8 -*-
"""Torna um Markdown AUTOCONTIDO: embute as imagens locais como data URIs base64.

Uso:  python embed_images.py entrada.md saida.md
Converte  ![alt](imagens/x.png)  em  ![alt](data:image/png;base64,...)  -> um .md unico.
Ignora URLs http(s):// e imagens que ja sejam data:. Caminhos inexistentes ficam intactos.
"""
import base64
import os
import re
import sys

MIME = {
    ".png": "image/png", ".jpg": "image/jpeg", ".jpeg": "image/jpeg",
    ".gif": "image/gif", ".svg": "image/svg+xml", ".webp": "image/webp",
}

IMG_RE = re.compile(r"!\[([^\]]*)\]\(([^)\s]+)(\s+\"[^\"]*\")?\)")


def embed(md_in, md_out):
    base = os.path.dirname(os.path.abspath(md_in))
    text = open(md_in, encoding="utf-8").read()
    n = [0]

    def repl(m):
        alt, path = m.group(1), m.group(2).strip()
        if path.startswith(("http://", "https://", "data:")):
            return m.group(0)
        full = os.path.normpath(os.path.join(base, path.strip("<>\"")))
        ext = os.path.splitext(full)[1].lower()
        if not os.path.exists(full) or ext not in MIME:
            return m.group(0)
        data = base64.b64encode(open(full, "rb").read()).decode("ascii")
        n[0] += 1
        return f"![{alt}](data:{MIME[ext]};base64,{data})"

    out = IMG_RE.sub(repl, text)
    open(md_out, "w", encoding="utf-8").write(out)
    print(f"OK -> {md_out}  ({n[0]} imagem(ns) embutida(s))")


if __name__ == "__main__":
    embed(sys.argv[1], sys.argv[2])
