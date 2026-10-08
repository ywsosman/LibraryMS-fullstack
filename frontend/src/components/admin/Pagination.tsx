'use client';

import React from 'react';
import { CaretLeft, CaretRight } from '@phosphor-icons/react';

export interface PaginationProps {
  page: number;
  totalPages?: number;
  totalElements?: number;
  onChange: (page: number) => void;
}

/** Previous / next controls for a Spring `Page` response. Renders nothing for a single page. */
export const Pagination: React.FC<PaginationProps> = ({ page, totalPages = 0, totalElements, onChange }) => {
  if (totalPages <= 1) return null;

  const button =
    'inline-flex items-center gap-1.5 h-9 px-3 rounded-md text-sm text-ink border border-rule-strong bg-surface hover:bg-sunken transition-colors disabled:opacity-40 disabled:pointer-events-none cursor-pointer';

  return (
    <nav className="mt-4 flex items-center justify-between gap-4" aria-label="Pagination">
      <span className="text-sm text-muted">
        Page {page + 1} of {totalPages}
        {totalElements !== undefined && <span className="hidden sm:inline"> ({totalElements} total)</span>}
      </span>
      <div className="flex items-center gap-2">
        <button type="button" className={button} disabled={page === 0} onClick={() => onChange(page - 1)}>
          <CaretLeft className="w-4 h-4" />
          Previous
        </button>
        <button type="button" className={button} disabled={page >= totalPages - 1} onClick={() => onChange(page + 1)}>
          Next
          <CaretRight className="w-4 h-4" />
        </button>
      </div>
    </nav>
  );
};
