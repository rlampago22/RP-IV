/**
 * RF10 / #99 — aviso sonoro do protocolo de contingência: sirene curta de dois tons (Web Audio) e
 * depois a voz do sistema (Web Speech, sem arquivo de áudio nem serviço externo).
 *
 * O navegador só deixa tocar som depois que a pessoa interagiu com a página (clique, tecla). Na
 * Central isso já aconteceu no login; se não tiver acontecido, o alerta aparece mudo.
 */

const TONS_HZ = [392, 294] // sol e ré graves, alternados como sirene de planta
const DURACAO_TOM_S = 0.42
const REPETICOES = 2 // 2 x (tom alto + tom baixo) ≈ 1,7 s

let contexto = null
let falaAtual = null

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

function escolherVoz() {
  const vozes = window.speechSynthesis.getVoices()
  const portugues = vozes.filter((v) => v.lang?.toLowerCase().startsWith('pt'))
  const brasil = portugues.filter((v) => v.lang.toLowerCase() === 'pt-br')
  const candidatas = brasil.length > 0 ? brasil : portugues
  // Voz masculina soa mais grave (Windows: "Microsoft Daniel"); senão a primeira em português.
  return candidatas.find((v) => /daniel|antonio|ricardo|male|masculin/i.test(v.name)) ?? candidatas[0] ?? null
}

function vozesProntas() {
  if (window.speechSynthesis.getVoices().length > 0) return Promise.resolve()
  return new Promise((resolve) => {
    const pronto = () => resolve()
    window.speechSynthesis.addEventListener('voiceschanged', pronto, { once: true })
    setTimeout(pronto, 800) // alguns navegadores nunca disparam o evento
  })
}

/** Anuncia o texto com voz grave e pausada. Resolve quando termina (ou se não houver voz). */
export async function falar(texto) {
  if (!('speechSynthesis' in window) || !texto) return
  await vozesProntas()
  return new Promise((resolve) => {
    const fala = new SpeechSynthesisUtterance(texto)
    fala.lang = 'pt-BR'
    const voz = escolherVoz()
    if (voz) fala.voice = voz
    fala.pitch = 0.45 // grave
    fala.rate = 0.88 // pausada, como aviso de sistema
    fala.volume = 1
    fala.onend = resolve
    fala.onerror = resolve
    falaAtual = fala
    window.speechSynthesis.cancel()
    window.speechSynthesis.speak(fala)
  })
}

/** Corta sirene e voz (alerta fechado pelo operador). */
export function silenciar() {
  if ('speechSynthesis' in window) window.speechSynthesis.cancel()
  falaAtual = null
  if (contexto && contexto.state === 'running') {
    contexto.close()
    contexto = null
  }
}
