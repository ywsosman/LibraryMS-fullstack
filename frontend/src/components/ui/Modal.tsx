'use client';

import React, { useEffect } from 'react';
import { X } from '@phosphor-icons/react';

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl';
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
  footer,
  maxWidth = 'md',
}) => {
  useEffect(() => {
    if (!isOpen) return;

    // Prevent background page scrolling while modal is open
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);

    return () => {
      document.body.style.overflow = originalOverflow;
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const maxWidthStyles = {
    sm: 'max-w-md',
    md: 'max-w-lg',
    lg: 'max-w-2xl',
    xl: 'max-w-4xl',
  }[maxWidth];

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      {/* Backdrop overlay */}
      <div
        className="fixed inset-0 bg-ink/40 transition-opacity"
        onClick={onClose}
        aria-hidden="true"
      />

      {/* Centering container with min-h-full to prevent negative scroll cutoff */}
      <div
        className="min-h-full flex items-center justify-center p-3 sm:p-6"
        onClick={(e) => {
          if (e.target === e.currentTarget) {
            onClose();
          }
        }}
      >
        <div
          className={`relative w-full ${maxWidthStyles} max-h-[calc(100dvh-1.5rem)] sm:max-h-[calc(100dvh-3rem)] flex flex-col bg-surface border border-rule rounded-lg shadow-xl z-10 my-auto`}
          role="dialog"
          aria-modal="true"
          aria-label={title}
          onClick={(e) => e.stopPropagation()}
        >
          {/* Pinned header with title and close button */}
          <div className="flex items-center justify-between gap-4 px-6 sm:px-8 py-4 sm:py-5 border-b border-rule shrink-0">
            <h2 className="font-serif text-xl sm:text-2xl text-ink tracking-tight">{title}</h2>
            <button
              onClick={onClose}
              className="w-8 h-8 -mr-2 rounded-md flex items-center justify-center text-muted hover:text-ink hover:bg-sunken transition-colors cursor-pointer"
              aria-label="Close"
            >
              <X className="w-5 h-5" />
            </button>
          </div>

          {/* Scrollable content body */}
          <div className="p-6 sm:p-8 pt-5 sm:pt-6 overflow-y-auto overscroll-contain flex-1 min-h-0">
            {children}
          </div>

          {/* Optional sticky footer */}
          {footer && (
            <div className="flex items-center justify-end gap-3 px-6 sm:px-8 py-4 border-t border-rule bg-surface shrink-0">
              {footer}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
