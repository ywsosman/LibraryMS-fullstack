'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { List, SignOut, X } from '@phosphor-icons/react';

const linkBase = 'text-sm whitespace-nowrap transition-colors hover:text-ink';

export const Navbar: React.FC = () => {
  const pathname = usePathname();
  const { user, isAdmin, isAuthenticated, isLoading, logout } = useAuth();
  const [menuOpen, setMenuOpen] = useState(false);

  const navLinks = [
    { href: '/catalog', label: 'Catalog' },
    ...(isAuthenticated ? [{ href: '/me', label: 'My loans' }] : []),
    ...(isAdmin ? [{ href: '/admin', label: 'Staff' }] : []),
  ];
  const isActive = (href: string) => pathname === href || pathname.startsWith(`${href}/`);
  const closeMenu = () => setMenuOpen(false);

  useEffect(() => {
    if (!menuOpen) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setMenuOpen(false);
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [menuOpen]);

  return (
    <header className="sticky top-0 z-40 bg-paper/95 backdrop-blur-sm border-b border-rule">
      <nav className="max-w-6xl mx-auto h-16 px-4 sm:px-6 flex items-center justify-between gap-6" aria-label="Main">
        <div className="flex items-center gap-8 min-w-0">
          <Link href="/" onClick={closeMenu} className="font-serif text-2xl tracking-tight text-ink">
            LibraryMS
          </Link>
          <div className="hidden md:flex items-center gap-5">
            {navLinks.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                aria-current={isActive(link.href) ? 'page' : undefined}
                className={`${linkBase} ${
                  isActive(link.href) ? 'text-ink underline underline-offset-[6px] decoration-accent decoration-2' : 'text-muted'
                }`}
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>

        {/* Desktop auth controls. Held back until the session check finishes so they don't flicker. */}
        <div className={`hidden md:flex items-center gap-4 transition-opacity ${isLoading ? 'opacity-0' : 'opacity-100'}`}>
          {isAuthenticated ? (
            <>
              <span className="text-sm text-muted truncate max-w-[160px]">{user?.username}</span>
              <button
                onClick={logout}
                className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink transition-colors cursor-pointer"
              >
                <SignOut className="w-4 h-4" />
                Sign out
              </button>
            </>
          ) : (
            <>
              <Link href="/login" className={`${linkBase} text-muted`}>
                Sign in
              </Link>
              <Link
                href="/register"
                className="inline-flex items-center h-9 px-3.5 rounded-md bg-accent text-on-accent text-sm font-medium hover:bg-accent-hover transition-colors whitespace-nowrap"
              >
                Get a library card
              </Link>
            </>
          )}
        </div>

        {/* Mobile menu button */}
        <button
          type="button"
          onClick={() => setMenuOpen((open) => !open)}
          aria-expanded={menuOpen}
          aria-controls="mobile-menu"
          aria-label={menuOpen ? 'Close menu' : 'Open menu'}
          className="md:hidden -mr-2 w-10 h-10 inline-flex items-center justify-center rounded-md text-ink hover:bg-sunken transition-colors cursor-pointer"
        >
          {menuOpen ? <X className="w-6 h-6" /> : <List className="w-6 h-6" />}
        </button>
      </nav>

      {/* Mobile menu panel */}
      <div id="mobile-menu" hidden={!menuOpen} className="md:hidden border-t border-rule bg-paper shadow-lg">
        <div className="px-4 py-4 flex flex-col">
          {navLinks.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              onClick={closeMenu}
              aria-current={isActive(link.href) ? 'page' : undefined}
              className={`py-3 font-serif text-2xl border-b border-rule ${isActive(link.href) ? 'text-accent' : 'text-ink'}`}
            >
              {link.label}
            </Link>
          ))}

          <div className="pt-5">
            {isLoading ? null : isAuthenticated ? (
              <div className="flex items-center justify-between gap-4">
                <span className="text-sm text-muted truncate">Signed in as {user?.username}</span>
                <button
                  onClick={() => {
                    closeMenu();
                    logout();
                  }}
                  className="inline-flex items-center gap-1.5 h-10 px-3 rounded-md border border-rule-strong text-sm text-ink hover:bg-sunken transition-colors cursor-pointer"
                >
                  <SignOut className="w-4 h-4" />
                  Sign out
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-3">
                <Link
                  href="/login"
                  onClick={closeMenu}
                  className="inline-flex items-center justify-center h-11 rounded-md border border-rule-strong text-sm font-medium text-ink hover:bg-sunken transition-colors"
                >
                  Sign in
                </Link>
                <Link
                  href="/register"
                  onClick={closeMenu}
                  className="inline-flex items-center justify-center h-11 rounded-md bg-accent text-on-accent text-sm font-medium hover:bg-accent-hover transition-colors"
                >
                  Get a library card
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};
