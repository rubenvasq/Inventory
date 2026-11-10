import { stockLevel } from './stockLevel'

export function StockGauge({ stock, minStock, active, unit }: { stock: number; minStock: number; active: boolean; unit: string }) {
  const level = stockLevel(stock, minStock, active)
  const description = `${level.status}: ${level.quantity} ${unit}. ${level.minimum > 0 ? `Mínimo: ${level.minimum}. La mitad del arco representa el mínimo; el arco completo representa ${level.target} ${unit} o más.` : 'Sin mínimo configurado; el arco indica si hay existencias.'}`
  return <span className={`stock-indicator stock-indicator-${level.tone}`} tabIndex={0}>
    <svg viewBox="0 0 52 30" role="img" aria-label={description}>
      <title>{description}</title>
      <path className="stock-indicator-track" d="M 5 26 A 21 21 0 0 1 47 26" pathLength="100" />
      <path className="stock-indicator-fill" d="M 5 26 A 21 21 0 0 1 47 26" pathLength="100" strokeDasharray={`${level.percent} 100`} />
      {level.minimum > 0 && <path className="stock-indicator-minimum" d="M 26 1 L 26 9" />}
    </svg>
    <span className="stock-indicator-tooltip" role="tooltip">{description}</span>
  </span>
}
