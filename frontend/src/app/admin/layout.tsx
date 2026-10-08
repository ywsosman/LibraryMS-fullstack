'use client';

import React from 'react';
import Link from 'next/link';
import { useAuth } from '@/lib/auth-context';
import { AdminSidebar } from '@/components/admin/AdminSidebar';

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { isAdmin, isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <div className="min-h-[100dvh]" />;
  }

  if (!isAdmin) {
    return (
      <div className="min-h-[100dvh] flex items-center px-4 sm:px-6">
        <div className="max-w-md mx-auto">
          <Link href="/" className="font-serif text-2xl text-ink">LibraryMS</Link>
          <h1 className="mt-10 font-serif text-4xl tracking-tight text-ink">This area is for library staff</h1>
          <p className="mt-3 text-muted">
            {isAuthenticated
              ? 'Your account doesn’t have staff access. Ask an administrator if you think it should.'
              : 'Sign in with a staff account to manage the catalog and loans.'}
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            {!isAuthenticated && (
              <Link
                href="/login"
                className="inline-flex items-center h-10 px-4 rounded-md bg-accent text-on-accent text-sm font-medium hover:bg-accent-hover transition-colors"
              >
                Sign in
              </Link>
            )}
            <Link
              href="/catalog"
              className="inline-flex items-center h-10 px-4 rounded-md border border-rule-strong text-ink text-sm font-medium hover:bg-sunken transition-colors"
            >
              Go to the catalog
            </Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-[100dvh] flex flex-col md:flex-row">
      <AdminSidebar />
      <main className="flex-1 min-w-0 px-4 sm:px-8 lg:px-12 py-8 sm:py-10">
        <div className="max-w-6xl">{children}</div>
      </main>
    </div>
  );
}
