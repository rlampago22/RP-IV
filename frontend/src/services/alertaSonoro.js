/**
 * RF10 / #99 — aviso sonoro do protocolo de contingência: sirene curta de dois tons (Web Audio) e
 * depois o anúncio gravado do protocolo (public/audio/contingencia/*.mp3, voz neural grave com
 * efeito de alto-falante — ver frontend/scripts/gerar-audio-contingencia.py).
 *
 * O navegador só deixa tocar som depois que a pessoa interagiu com a página (clique, tecla). Na
 * Central isso já aconteceu no login; se não tiver acontecido, o alerta aparece mudo.
 */

const TONS_HZ = [392, 294] // sol e ré graves, alternados como sirene de planta
const DURACAO_TOM_S = 0.42
const REPETICOES = 2 // 2 x (tom alto + tom baixo) ≈ 1,7 s

let contexto = null
let anuncio = null

/** Toca a sirene e resolve quando ela termina. */
export function tocarSirene() {
  const Contexto = window.AudioContext || window.webkitAudioContext
  if (!Contexto) return Promise.resolve()
  try {
    contexto = contexto ?? new Contexto()
    if (contexto.state === 'suspended') contexto.resume()
    const inicio = contexto.currentTime + 0.05
    const filtro = contexto.createBiquadFilter()
    filtro.type = 'lowpass'
    filtro.frequency.value = 1400 // tira o chiado da onda dente-de-serra
    const volume = contexto.createGain()
    volume.gain.value = 0.16
    filtro.connect(volume).connect(contexto.destination)

    let t = inicio
    for (let r = 0; r < REPETICOES; r++) {
      for (const hz of TONS_HZ) {
        const osc = contexto.createOscillator()
        osc.type = 'sawtooth'
        osc.frequency.value = hz
        osc.connect(filtro)
        osc.start(t)
        osc.stop(t + DURACAO_TOM_S)
        t += DURACAO_TOM_S
      }
    }
    return new Promise((resolve) => setTimeout(resolve, (t - contexto.currentTime) * 1000 + 150))
  } catch {
    return Promise.resolve()
  }
}

/** Toca o anúncio gravado. Resolve quando termina — ou na hora, se o navegador bloquear o som. */
export function tocarAnuncio(url) {
  if (!url) return Promise.resolve()
  return new Promise((resolve) => {
    anuncio = new Audio(url)
    anuncio.volume = 1
    anuncio.onended = resolve
    anuncio.onerror = resolve
    anuncio.play().catch(resolve)
  })
}

/** Corta sirene e anúncio (alerta fechado pelo operador). */
export function silenciar() {
  if (anuncio) {
    anuncio.pause()
    anuncio = null
  }
  if (contexto && contexto.state === 'running') {
    contexto.close()
    contexto = null
  }
}
