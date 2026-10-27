/**
 * Ícones inline SVG do painel SCADA (Opção A v2).
 * Sem dependência externa/CDN — só paths à mão, mesmo espírito "zero libs" do mvp/.
 */

function Svg({ size = 16, children, ...rest }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...rest}
    >
      {children}
    </svg>
  )
}

export function AtomLogo({ size = 26, accent = '#06B6D4' }) {
  return (
    <svg width={size} height={size} viewBox="0 0 32 32" fill="none" role="img" aria-label="Usina — logo">
      <g stroke={accent === '#06B6D4' ? '#ECFEFF' : '#ECFEFF'} strokeWidth="1.6" opacity="0.92">
        <ellipse cx="16" cy="16" rx="13" ry="5.4" />
        <ellipse cx="16" cy="16" rx="13" ry="5.4" transform="rotate(60 16 16)" />
        <ellipse cx="16" cy="16" rx="13" ry="5.4" transform="rotate(120 16 16)" />
      </g>
      <circle cx="16" cy="16" r="3.1" fill="#F8FAFC" />
      <circle cx="29" cy="16" r="1.6" fill="#F8FAFC" />
    </svg>
  )
}

export function IconThermometer(props) {
  return (
    <Svg {...props}>
      <path d="M14 14.76V3.5a2.5 2.5 0 00-5 0v11.26a4.5 4.5 0 105 0z" />
    </Svg>
  )
}

export function IconGauge(props) {
  return (
    <Svg {...props}>
      <circle cx="12" cy="13" r="8" />
      <path d="M12 13l4-4" />
      <path d="M12 5v1" />
    </Svg>
  )
}

export function IconRadiation(props) {
  return (
    <Svg {...props} strokeWidth="0">
      <circle cx="12" cy="12" r="2.1" fill="currentColor" />
      <circle cx="12" cy="6.3" r="2.1" fill="currentColor" opacity="0.85" />
      <circle cx="7" cy="15.2" r="2.1" fill="currentColor" opacity="0.85" />
      <circle cx="17" cy="15.2" r="2.1" fill="currentColor" opacity="0.85" />
    </Svg>
  )
}

export function IconDroplet(props) {
  return (
    <Svg {...props}>
      <path d="M12 2.69l5.66 5.66a8 8 0 11-11.31 0z" />
    </Svg>
  )
}

export function IconBell(props) {
  return (
    <Svg {...props}>
      <path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 01-3.46 0" />
    </Svg>
  )
}

export function IconShield(props) {
  return (
    <Svg {...props}>
      <path d="M12 2l8 4v6c0 5-3.5 8.5-8 10-4.5-1.5-8-5-8-10V6l8-4z" />
    </Svg>
  )
}

export function IconUser(props) {
  return (
    <Svg {...props}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21c0-4 4-7 8-7s8 3 8 7" />
    </Svg>
  )
}

export function IconPower(props) {
  return (
    <Svg {...props}>
      <path d="M12 3v9" />
      <path d="M6.5 6.5a8 8 0 1011 0" />
    </Svg>
  )
}

export function IconGrid(props) {
  return (
    <Svg {...props}>
      <rect x="3" y="3" width="8" height="8" rx="1.5" />
      <rect x="13" y="3" width="8" height="8" rx="1.5" />
      <rect x="3" y="13" width="8" height="8" rx="1.5" />
      <rect x="13" y="13" width="8" height="8" rx="1.5" />
    </Svg>
  )
}

export function IconPlay(props) {
  return (
    <Svg {...props}>
      <path d="M5 3l15 9-15 9V3z" />
    </Svg>
  )
}

export function IconCheck(props) {
  return (
    <Svg {...props}>
      <path d="M20 6L9 17l-5-5" />
    </Svg>
  )
}

export function IconClock(props) {
  return (
    <Svg {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 3" />
    </Svg>
  )
}

export function IconActivity(props) {
  return (
    <Svg {...props}>
      <path d="M3 12h4l3 8 4-16 3 8h4" />
    </Svg>
  )
}

export function IconAlertTriangle(props) {
  return (
    <Svg {...props}>
      <path d="M12 9v4" />
      <path d="M12 17h.01" />
      <path d="M10.3 3.9L1.8 18a2 2 0 001.7 3h17a2 2 0 001.7-3L13.7 3.9a2 2 0 00-3.4 0z" />
    </Svg>
  )
}

export function IconLock(props) {
  return (
    <Svg {...props}>
      <rect x="5" y="11" width="14" height="9" rx="2" />
      <path d="M8 11V7a4 4 0 118 0v4" />
    </Svg>
  )
}

export function IconCore(props) {
  return (
    <Svg {...props}>
      <circle cx="12" cy="12" r="9" />
      <circle cx="12" cy="12" r="3" />
    </Svg>
  )
}
