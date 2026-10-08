'use client';

import React from 'react';
import { CircleNotch } from '@phosphor-icons/react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'outline' | 'ghost' | 'danger';
  size?: 'sm' | 'md' | 'lg';
  isLoading?: boolean;
  icon?: React.ReactNode;
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    {
      children,
      variant = 'primary',
      size = 'md',
      isLoading = false,
      icon,
      className = '',
      disabled,
      ...props
    },
    ref
  ) => {
    const baseStyles =
      'inline-flex items-center justify-center whitespace-nowrap font-medium rounded-md transition-colors duration-150 cursor-pointer select-none active:translate-y-px disabled:opacity-50 disabled:pointer-events-none gap-2';

    const sizeStyles = {
      sm: 'text-sm px-3 h-8',
      md: 'text-sm px-4 h-10',
      lg: 'text-base px-6 h-12',
    }[size];

    const variantStyles = {
      primary: 'bg-accent text-on-accent hover:bg-accent-hover',
      secondary: 'bg-surface text-ink border border-rule-strong hover:bg-sunken',
      outline: 'bg-transparent text-ink border border-ink hover:bg-ink hover:text-paper',
      ghost: 'bg-transparent text-muted hover:text-ink hover:bg-sunken',
      danger: 'bg-transparent text-danger border border-danger/40 hover:bg-danger/10',
    }[variant];

    return (
      <button
        ref={ref}
        disabled={disabled || isLoading}
        className={`${baseStyles} ${sizeStyles} ${variantStyles} ${className}`}
        {...props}
      >
        {isLoading ? (
          <CircleNotch className="w-4 h-4 animate-spin text-current" />
        ) : (
          icon && <span className="inline-flex shrink-0">{icon}</span>
        )}
        <span>{children}</span>
      </button>
    );
  }
);

Button.displayName = 'Button';
