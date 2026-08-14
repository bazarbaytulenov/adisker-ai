import { clsx } from 'clsx'

// ─── Button ──────────────────────────────────────────────────────────────────
interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  loading?: boolean
}

export function Button({
  variant = 'primary',
  size = 'md',
  loading,
  children,
  className,
  disabled,
  ...props
}: ButtonProps) {
  const base = 'inline-flex items-center justify-center font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50'
  const variants = {
    primary:   'bg-primary-600 text-white hover:bg-primary-700 focus:ring-primary-500',
    secondary: 'border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 focus:ring-primary-500',
    danger:    'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
    ghost:     'text-gray-600 hover:bg-gray-100 focus:ring-primary-500',
  }
  const sizes = {
    sm: 'px-3 py-1.5 text-xs gap-1.5',
    md: 'px-4 py-2 text-sm gap-2',
    lg: 'px-5 py-2.5 text-base gap-2',
  }
  return (
    <button
      disabled={disabled || loading}
      className={clsx(base, variants[variant], sizes[size], className)}
      {...props}
    >
      {loading && (
        <svg className="h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
        </svg>
      )}
      {children}
    </button>
  )
}

// ─── Input ───────────────────────────────────────────────────────────────────
interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
}

export function Input({ label, error, className, id, ...props }: InputProps) {
  const inputId = id || label?.toLowerCase().replace(/\s+/g, '-')
  return (
    <div className="space-y-1">
      {label && <label htmlFor={inputId} className="label">{label}</label>}
      <input
        id={inputId}
        className={clsx('input', error && 'border-red-400 focus:border-red-500 focus:ring-red-400', className)}
        {...props}
      />
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  )
}

// ─── Select ──────────────────────────────────────────────────────────────────
interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  options: { value: string; label: string }[]
  placeholder?: string
}

export function Select({ label, error, options, placeholder, className, id, ...props }: SelectProps) {
  const selectId = id || label?.toLowerCase().replace(/\s+/g, '-')
  return (
    <div className="space-y-1">
      {label && <label htmlFor={selectId} className="label">{label}</label>}
      <select
        id={selectId}
        className={clsx('input', error && 'border-red-400 focus:border-red-500 focus:ring-red-400', className)}
        {...props}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((o) => (
          <option key={o.value} value={o.value}>{o.label}</option>
        ))}
      </select>
      {error && <p className="text-xs text-red-600">{error}</p>}
    </div>
  )
}

// ─── Card ────────────────────────────────────────────────────────────────────
export function Card({ children, className }: { children: React.ReactNode; className?: string }) {
  return <div className={clsx('card', className)}>{children}</div>
}

// ─── Modal ───────────────────────────────────────────────────────────────────
interface ModalProps {
  open: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
  width?: string
}

export function Modal({ open, onClose, title, children, width = 'max-w-lg' }: ModalProps) {
  if (!open) return null
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      <div className="absolute inset-0 bg-black/50" onClick={onClose} />
      <div className={clsx('relative bg-white rounded-xl shadow-xl w-full mx-4', width)}>
        <div className="flex items-center justify-between p-6 border-b border-gray-100">
          <h2 className="text-lg font-semibold text-gray-900">{title}</h2>
          <button onClick={onClose} className="text-gray-400 hover:text-gray-600 transition-colors">
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
        <div className="p-6">{children}</div>
      </div>
    </div>
  )
}

// ─── Table ───────────────────────────────────────────────────────────────────
export function Table({ children }: { children: React.ReactNode }) {
  return (
    <div className="overflow-x-auto rounded-xl border border-gray-100">
      <table className="min-w-full divide-y divide-gray-100 bg-white text-sm">{children}</table>
    </div>
  )
}

export function Th({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <th className={clsx('px-4 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wide bg-gray-50', className)}>
      {children}
    </th>
  )
}

export function Td({ children, className }: { children: React.ReactNode; className?: string }) {
  return <td className={clsx('px-4 py-3 text-gray-700', className)}>{children}</td>
}

// ─── Spinner ─────────────────────────────────────────────────────────────────
export function Spinner({ className }: { className?: string }) {
  return (
    <div className={clsx('flex items-center justify-center p-8', className)}>
      <svg className="h-8 w-8 animate-spin text-primary-600" viewBox="0 0 24 24" fill="none">
        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
      </svg>
    </div>
  )
}

// ─── Empty state ─────────────────────────────────────────────────────────────
export function Empty({ message = 'Нет данных' }: { message?: string }) {
  return (
    <div className="flex flex-col items-center justify-center py-12 text-gray-400">
      <svg className="h-12 w-12 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
      </svg>
      <p className="text-sm">{message}</p>
    </div>
  )
}

// ─── Pagination ──────────────────────────────────────────────────────────────
interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

export function Pagination({ page, totalPages, totalElements, onPageChange }: PaginationProps) {
  if (totalPages <= 1) return null
  return (
    <div className="flex items-center justify-between mt-4 text-sm text-gray-600">
      <span>Всего: {totalElements}</span>
      <div className="flex items-center gap-1">
        <Button variant="secondary" size="sm" disabled={page === 0} onClick={() => onPageChange(page - 1)}>←</Button>
        <span className="px-3 py-1.5">{page + 1} / {totalPages}</span>
        <Button variant="secondary" size="sm" disabled={page >= totalPages - 1} onClick={() => onPageChange(page + 1)}>→</Button>
      </div>
    </div>
  )
}
