import { EQ_FREQS } from './settings'

// Wraps an <audio> element in a Web Audio graph:
//   source -> [EQ biquad filters] -> gain -> destination
// Gain is used for the volume slider and for fade in/out, so the element's own
// .volume stays at 1. Created lazily on first use (needs a running context).
export class AudioGraph {
  private ctx: AudioContext | null = null
  private gain: GainNode | null = null
  private filters: BiquadFilterNode[] = []
  private connected = false
  private baseVolume = 1
  private eqEnabled = false
  private eqGains: number[] = EQ_FREQS.map(() => 0)

  constructor(private el: HTMLAudioElement) {}

  private ensure() {
    if (this.connected) return
    try {
      const Ctx = window.AudioContext || (window as any).webkitAudioContext
      if (!Ctx) return
      this.ctx = new Ctx()
      const src = this.ctx.createMediaElementSource(this.el)
      this.filters = EQ_FREQS.map((f, i) => {
        const bq = this.ctx!.createBiquadFilter()
        bq.type = i === 0 ? 'lowshelf' : i === EQ_FREQS.length - 1 ? 'highshelf' : 'peaking'
        bq.frequency.value = f
        bq.Q.value = 1
        bq.gain.value = this.eqEnabled ? this.eqGains[i] : 0
        return bq
      })
      this.gain = this.ctx.createGain()
      this.gain.gain.value = this.baseVolume
      // chain: src -> f0 -> f1 -> ... -> gain -> dest
      let node: AudioNode = src
      for (const f of this.filters) {
        node.connect(f)
        node = f
      }
      node.connect(this.gain)
      this.gain.connect(this.ctx.destination)
      this.connected = true
    } catch {
      // MediaElementSource can only be created once / may be blocked — fall back
      // to controlling the element's volume directly.
      this.connected = false
    }
  }

  resume() {
    this.ensure()
    this.ctx?.resume().catch(() => {})
  }

  setVolume(v: number) {
    this.baseVolume = v
    if (this.gain && this.ctx) {
      this.gain.gain.cancelScheduledValues(this.ctx.currentTime)
      this.gain.gain.value = v
    } else {
      this.el.volume = v
    }
  }

  // Ramp gain to `target` over `sec`; resolves when done.
  fadeTo(target: number, sec: number): Promise<void> {
    return new Promise((resolve) => {
      if (this.gain && this.ctx) {
        const g = this.gain.gain
        const now = this.ctx.currentTime
        g.cancelScheduledValues(now)
        g.setValueAtTime(Math.max(0.0001, g.value), now)
        g.linearRampToValueAtTime(Math.max(0.0001, target), now + sec)
        setTimeout(resolve, sec * 1000)
      } else {
        this.el.volume = target
        resolve()
      }
    })
  }

  restoreVolume() {
    this.setVolume(this.baseVolume)
  }

  setEqEnabled(on: boolean) {
    this.eqEnabled = on
    this.applyEq()
  }

  setEqGains(gains: number[]) {
    this.eqGains = gains
    this.applyEq()
  }

  private applyEq() {
    this.filters.forEach((f, i) => {
      f.gain.value = this.eqEnabled ? this.eqGains[i] ?? 0 : 0
    })
  }
}
