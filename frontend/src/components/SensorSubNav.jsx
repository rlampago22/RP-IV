import { NavLink, useLocation } from 'react-router-dom'
import { TIPOS_SENSOR } from './SensorCard.jsx'
import { buscaDoReator } from '../state/reatorSelecionado.js'

/** Sub-navegação "uma aba por sensor" (pedido do professor, 13/11), dentro da
 * seção Sensores — reaproveita o visual das abas principais (`.tab2`).
 * Repassa o `?reator=` atual (se houver) para a aba continuar filtrada. */
export default function SensorSubNav() {
  const { search } = useLocation()
  const busca = buscaDoReator(search)

  return (
    <nav className="scada-tabs scada-subtabs" aria-label="Sensores individuais">
      <NavLink to={`/sensores${busca}`} end className={({ isActive }) => `tab2${isActive ? ' active' : ''}`}>
        Visão geral
      </NavLink>
      {TIPOS_SENSOR.map((s) => (
        <NavLink
          key={s.slug}
          to={`/sensores/${s.slug}${busca}`}
          className={({ isActive }) => `tab2${isActive ? ' active' : ''}`}
        >
          {s.nome}
        </NavLink>
      ))}
    </nav>
  )
}
