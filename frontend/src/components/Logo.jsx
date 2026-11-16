/**
 * Emblema da Central de Supervisão: símbolo de radiação (proporções do ISO 361: miolo 1,
 * pás de 1,5 a 5) em selo circular, nas cores escuras da interface. Decorativo — o nome da
 * unidade aparece em texto ao lado.
 */
export default function Logo({ size = 42 }) {
  return (
    <svg
      className="scada-logo"
      width={size}
      height={size}
      viewBox="0 0 48 48"
      aria-hidden="true"
      focusable="false"
    >
      <circle cx="24" cy="24" r="22" fill="#0f1c30" stroke="#22d3ee" strokeWidth="2.2" />
      <circle cx="24" cy="24" r="18.4" fill="none" stroke="#22d3ee" strokeOpacity="0.35" strokeWidth="1.2" />
      <g fill="#e8f4ff" transform="translate(24 24) scale(0.74) translate(-24 -24)">
        <path d="M21.6 19.84 L16 10.14 A16 16 0 0 1 32 10.14 L26.4 19.84 A4.8 4.8 0 0 0 21.6 19.84 Z" />
        <path d="M28.8 24 L40 24 A16 16 0 0 1 32 37.86 L26.4 28.16 A4.8 4.8 0 0 0 28.8 24 Z" />
        <path d="M21.6 28.16 L16 37.86 A16 16 0 0 1 8 24 L19.2 24 A4.8 4.8 0 0 0 21.6 28.16 Z" />
        <circle cx="24" cy="24" r="3.2" />
      </g>
    </svg>
  )
}
