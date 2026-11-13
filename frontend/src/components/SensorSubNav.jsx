import { NavLink } from 'react-router-dom'
import { TIPOS_SENSOR } from './SensorCard.jsx'

/** Sub-navegação "uma aba por sensor" (pedido do professor, 13/11), dentro da
 * seção Sensores — reaproveita o visual das abas principais (`.tab2`). */
export default function SensorSubNav() {
  return (
    <nav className="scada-tabs scada-subtabs" aria-label="Sensores individuais">
      <NavLink to="/sensores" end className={({ isActive }) => `tab2${isActive ? ' active' : ''}`}>
        Visão geral
      </NavLink>
      {TIPOS_SENSOR.map((s) => (
        <NavLink
          key={s.slug}
          to={`/sensores/${s.slug}`}
          className={({ isActive }) => `tab2${isActive ? ' active' : ''}`}
        >
          {s.nome}
        </NavLink>
      ))}
    </nav>
  )
}
