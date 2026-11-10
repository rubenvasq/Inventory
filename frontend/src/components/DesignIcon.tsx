export function DesignIcon({ name }: { name: string }) {
  return <img className="design-icon" src={`/design/${name}.svg`} alt="" aria-hidden="true" />
}
