"""
Gera os anúncios falados do protocolo de contingência (RF10 / #99) em
frontend/public/audio/contingencia/<CODIGO>.mp3, a partir de src/data/contingencia-falas.json.

Voz: Microsoft Antonio (neural, pt-BR) via edge-tts, mais grave e com efeito de alto-falante de
usina (graves reforçados, compressão e eco curto) via ffmpeg. Os .mp3 ficam versionados: o sistema
não precisa de internet nem deste script para tocar o alerta.

Rodar de novo só quando mudar alguma frase:
    pip install edge-tts      (e ter o ffmpeg no PATH)
    python frontend/scripts/gerar-audio-contingencia.py
"""
import asyncio
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

import edge_tts

FRONTEND = Path(__file__).resolve().parent.parent
FALAS = FRONTEND / "src" / "data" / "contingencia-falas.json"
DESTINO = FRONTEND / "public" / "audio" / "contingencia"

VOZ = "pt-BR-AntonioNeural"
VELOCIDADE = "-10%"
TOM = "-6Hz"
# Tom ~8% mais grave sem mudar a duração + alto-falante de pavilhão.
FILTRO = (
    "asetrate=24000*0.92,aresample=24000,atempo=1.0870,"
    "highpass=f=80,lowpass=f=7500,equalizer=f=140:t=q:w=1:g=4,"
    "acompressor=threshold=-20dB:ratio=3:attack=5:release=120,"
    "aecho=0.8:0.55:70|140:0.22|0.10,loudnorm=I=-15:TP=-1.5"
)


async def gerar(codigo: str, texto: str, pasta_temp: Path) -> None:
    bruto = pasta_temp / f"{codigo}-bruto.mp3"
    await edge_tts.Communicate(texto, VOZ, rate=VELOCIDADE, pitch=TOM).save(str(bruto))
    saida = DESTINO / f"{codigo}.mp3"
    resultado = subprocess.run(
        ["ffmpeg", "-y", "-loglevel", "error", "-i", str(bruto), "-af", FILTRO,
         "-ar", "44100", "-ac", "1", "-b:a", "96k", str(saida)],
        capture_output=True, text=True,
    )
    if resultado.returncode != 0:
        sys.exit(f"ffmpeg falhou em {codigo}: {resultado.stderr.strip()}")
    print(f"ok  {saida.relative_to(FRONTEND)}")


async def main() -> None:
    if shutil.which("ffmpeg") is None:
        sys.exit("ffmpeg não encontrado no PATH.")
    falas = json.loads(FALAS.read_text(encoding="utf-8"))
    DESTINO.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as temp:
        for codigo, texto in falas.items():
            await gerar(codigo, texto, Path(temp))


if __name__ == "__main__":
    asyncio.run(main())
