import { useState } from 'react'
import type { Product } from '../api/types'
import { Modal } from './Modal'
import './VisualDetails.css'

export function ProductPhoto({ product, size = 64 }: { product: Product; size?: number }) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const [expanded, setExpanded] = useState(false)
  const available = Boolean(product.imageUrl && product.imageUrl !== failedUrl)
  return <>
    <button type="button" className="product-photo" style={{ width: size, height: size }} onClick={() => setExpanded(true)} aria-label={`Ver imagen y detalles de ${product.name}`}>
      {available ? <img src={product.imageUrl!} alt={product.name} loading="lazy" decoding="async" onError={() => setFailedUrl(product.imageUrl)} /> : <span className="product-photo-fallback"><svg viewBox="0 0 64 64" fill="none" aria-hidden="true"><path d="m32 8 23 13v25L32 59 9 46V21L32 8Z" fill="currentColor" opacity=".1"/><path d="m9 21 23 13 23-13M32 34v25M21 14l23 13v10M32 8 9 21v25l23 13 23-13V21L32 8Z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round"/></svg><span>{product.categoryName || 'Producto'}</span></span>}
      <span className="photo-zoom" aria-hidden="true">↗</span>
    </button>
    {expanded && <Modal title={product.name} onClose={() => setExpanded(false)}><div className="product-photo-preview">{available ? <img src={product.imageUrl!} alt={product.name} onError={() => setFailedUrl(product.imageUrl)} /> : <p>No hay una fotografía disponible para este producto.</p>}</div><p className="photo-caption">{product.sku} · {product.categoryName}</p><p className="photo-caption">Existencias: {product.stock} {product.unit}</p></Modal>}
  </>
}
