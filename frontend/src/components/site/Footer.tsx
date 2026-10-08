import React from 'react';
import Link from 'next/link';

export const Footer: React.FC = () => {
  return (
    <footer className="border-t border-rule mt-auto">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-10 flex flex-col sm:flex-row sm:items-end justify-between gap-6">
        <div>
          <Link href="/" className="font-serif text-xl text-ink">
            LibraryMS
          </Link>
          <p className="text-sm text-muted mt-1 max-w-xs">
            Find a book, borrow a copy, bring it back in two weeks.
          </p>
        </div>
        <div className="flex flex-wrap gap-x-6 gap-y-2 text-sm text-muted">
          <Link href="/catalog" className="hover:text-ink transition-colors">Catalog</Link>
          <Link href="/me" className="hover:text-ink transition-colors">My loans</Link>
          <Link href="/login" className="hover:text-ink transition-colors">Sign in</Link>
          <span className="text-faint">© LibraryMS</span>
        </div>
      </div>
    </footer>
  );
};
