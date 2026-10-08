'use client';

import React from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helperText?: string;
  leftIcon?: React.ReactNode;
  rightIcon?: React.ReactNode;
}

export const fieldStyles =
  'w-full rounded-md bg-surface border border-rule-strong text-sm text-ink placeholder:text-faint transition-colors outline-none focus:border-accent focus:ring-2 focus:ring-accent/20 disabled:opacity-60 disabled:bg-sunken';

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  (
    {
      label,
      error,
      helperText,
      leftIcon,
      rightIcon,
      className = '',
      id,
      disabled,
      ...props
    },
    ref
  ) => {
    const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

    return (
      <div className="w-full flex flex-col gap-1.5">
        {label && (
          <label htmlFor={inputId} className="text-sm font-medium text-ink">
            {label}
          </label>
        )}
        <div className="relative flex items-center">
          {leftIcon && (
            <span className="absolute left-3 text-faint pointer-events-none flex items-center">
              {leftIcon}
            </span>
          )}
          <input
            ref={ref}
            id={inputId}
            disabled={disabled}
            aria-invalid={Boolean(error)}
            className={`${fieldStyles} h-10 ${leftIcon ? 'pl-9' : 'pl-3'} ${rightIcon ? 'pr-9' : 'pr-3'} ${
              error ? 'border-danger focus:border-danger focus:ring-danger/20' : ''
            } ${className}`}
            {...props}
          />
          {rightIcon && (
            <span className="absolute right-3 text-faint flex items-center">{rightIcon}</span>
          )}
        </div>
        {error ? (
          <p className="text-sm text-danger">{error}</p>
        ) : helperText ? (
          <p className="text-sm text-muted">{helperText}</p>
        ) : null}
      </div>
    );
  }
);

Input.displayName = 'Input';
