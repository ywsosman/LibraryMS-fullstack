import React from 'react';
import Link from 'next/link';

/** Form on the left, reading-room photo on the right (hidden on small screens). */
export function AuthShell({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <div className="min-h-[100dvh] grid grid-cols-1 lg:grid-cols-2">
      <div className="flex flex-col px-4 sm:px-10 py-8">
        <Link href="/" className="font-serif text-2xl tracking-tight text-ink">
          LibraryMS
        </Link>
        <main className="flex-1 flex items-center">
          <div className="w-full max-w-sm mx-auto lg:mx-0 lg:ml-[12%] py-12">
            <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">{title}</h1>
            <p className="mt-3 text-muted">{subtitle}</p>
            <div className="mt-10">{children}</div>
          </div>
        </main>
      </div>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src="/hero-library.jpg"
        alt=""
        className="hidden lg:block h-[100dvh] w-full object-cover sticky top-0"
      />
    </div>
  );
}

export function FormError({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <p role="alert" className="mb-6 p-3 rounded-md bg-danger/10 text-sm text-danger">
      {message}
    </p>
  );
}
