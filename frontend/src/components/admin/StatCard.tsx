import React from 'react';
import Link from 'next/link';

export interface StatCardProps {
  label: string;
  value?: number;
  href?: string;
  tone?: 'default' | 'danger';
}

export const StatCard: React.FC<StatCardProps> = ({ label, value, href, tone = 'default' }) => {
  const body = (
    <>
      <span className="text-sm text-muted">{label}</span>
      <span
        className={`mt-2 block font-serif text-5xl tracking-tight tabular-nums ${
          tone === 'danger' && value ? 'text-danger' : 'text-ink'
        }`}
      >
        {value ?? <span className="inline-block h-10 w-12 rounded bg-sunken animate-pulse align-middle" />}
      </span>
    </>
  );

  return href ? (
    <Link href={href} className="block py-5 sm:px-5 first:sm:pl-0 hover:bg-sunken/50 transition-colors">
      {body}
    </Link>
  ) : (
    <div className="py-5 sm:px-5 first:sm:pl-0">{body}</div>
  );
};
