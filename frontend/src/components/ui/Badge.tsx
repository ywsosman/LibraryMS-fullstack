'use client';

import React from 'react';

export interface BadgeProps {
  children: React.ReactNode;
  variant?: 'available' | 'on_loan' | 'lost' | 'overdue' | 'returned' | 'neutral' | 'accent';
  size?: 'sm' | 'md';
  className?: string;
}

export const Badge: React.FC<BadgeProps> = ({
  children,
  variant = 'neutral',
  size = 'md',
  className = '',
}) => {
  const sizeStyles = size === 'sm' ? 'px-1.5 py-0.5 text-xs' : 'px-2 py-0.5 text-sm';

  const variantStyles = {
    available: 'bg-success/10 text-success',
    on_loan: 'bg-warning/10 text-warning',
    lost: 'bg-danger/10 text-danger',
    overdue: 'bg-danger text-paper',
    returned: 'bg-sunken text-muted',
    neutral: 'bg-sunken text-muted',
    accent: 'bg-accent/10 text-accent',
  }[variant];

  return (
    <span
      className={`inline-flex items-center rounded-sm font-medium capitalize whitespace-nowrap ${sizeStyles} ${variantStyles} ${className}`}
    >
      {/* Status enums arrive as ON_LOAN-style strings; normalise so `capitalize` reads well */}
      {typeof children === 'string' ? children.replace(/_/g, ' ').toLowerCase() : children}
    </span>
  );
};
