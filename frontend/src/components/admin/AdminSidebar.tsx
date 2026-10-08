'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import {
  SquaresFour,
  BookOpen,
  PenNib,
  Barcode,
  UsersThree,
  ArrowsLeftRight,
  UserGear,
  ClockCounterClockwise,
  SignOut,
  ArrowLeft,
  List,
  X,
} from '@phosphor-icons/react';

const links = [
  { href: '/admin', label: 'Overview', icon: SquaresFour },
  { href: '/admin/loans', label: 'Loans', icon: ArrowsLeftRight },
  { href: '/admin/books', label: 'Books', icon: BookOpen },
  { href: '/admin/copies', label: 'Copies', icon: Barcode },
  { href: '/admin/authors', label: 'Authors', icon: PenNib },
  { href: '/admin/members', label: 'Members', icon: UsersThree },
  { href: '/admin/users', label: 'Accounts', icon: UserGear },
  { href: '/admin/audit', label: 'Activity log', icon: ClockCounterClockwise },
];

const itemBase = 'flex items-center gap-3 px-3 rounded-md text-sm whitespace-nowrap transition-colors';

/** Nav list + footer, shared by the desktop sidebar and the mobile drawer. */
function SidebarBody({ onNavigate }: { onNavigate?: () => void }) {
  const pathname = usePathname();
  const { user, logout } = useAuth();

  return (
    <>
      <nav className="px-3 flex flex-col gap-1" aria-label="Staff">
        {links.map(({ href, label, icon: Icon }) => {
          const isActive = href === '/admin' ? pathname === '/admin' : pathname.startsWith(href);
          return (
            <Link
              key={href}
              href={href}
              onClick={onNavigate}
              aria-current={isActive ? 'page' : undefined}
              className={`${itemBase} h-10 md:h-9 ${
                isActive ? 'bg-surface text-ink font-medium shadow-sm' : 'text-muted hover:text-ink hover:bg-surface/60'
              }`}
            >
              <Icon weight={isActive ? 'fill' : 'regular'} className={`w-4 h-4 ${isActive ? 'text-accent' : ''}`} />
              {label}
            </Link>
          );
        })}
      </nav>

      <div className="mt-auto p-3 border-t border-rule space-y-1">
        <Link
          href="/"
          onClick={onNavigate}
          className={`${itemBase} h-10 md:h-9 text-muted hover:text-ink hover:bg-surface/60`}
        >
          <ArrowLeft className="w-4 h-4" />
          Public site
        </Link>
        <button
          onClick={() => {
            onNavigate?.();
            logout();
          }}
          className={`${itemBase} h-10 md:h-9 w-full text-muted hover:text-ink hover:bg-surface/60 cursor-pointer`}
        >
          <SignOut className="w-4 h-4" />
          <span className="truncate">Sign out {user?.username}</span>
        </button>
      </div>
    </>
  );
}

export const AdminSidebar: React.FC = () => {
  const [drawerOpen, setDrawerOpen] = useState(false);
  const closeDrawer = () => setDrawerOpen(false);

  // Escape closes the drawer; the page behind it doesn't scroll while it's open
  useEffect(() => {
    if (!drawerOpen) return;
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && setDrawerOpen(false);
    window.addEventListener('keydown', onKey);
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      window.removeEventListener('keydown', onKey);
      document.body.style.overflow = previousOverflow;
    };
  }, [drawerOpen]);

  return (
    <>
      {/* Desktop sidebar */}
      <aside className="hidden md:flex w-60 h-[100dvh] sticky top-0 shrink-0 bg-sunken border-r border-rule flex-col">
        <div className="px-5 py-6">
          <Link href="/admin" className="font-serif text-2xl tracking-tight text-ink">
            LibraryMS
          </Link>
          <p className="text-sm text-muted">Staff</p>
        </div>
        <SidebarBody />
      </aside>

      {/* Mobile top bar */}
      <header className="md:hidden sticky top-0 z-40 h-14 px-4 flex items-center justify-between bg-sunken/95 backdrop-blur-sm border-b border-rule">
        <Link href="/admin" className="flex items-baseline gap-2">
          <span className="font-serif text-xl tracking-tight text-ink">LibraryMS</span>
          <span className="text-sm text-muted">Staff</span>
        </Link>
        <button
          type="button"
          onClick={() => setDrawerOpen(true)}
          aria-expanded={drawerOpen}
          aria-controls="staff-drawer"
          aria-label="Open menu"
          className="-mr-2 w-10 h-10 inline-flex items-center justify-center rounded-md text-ink hover:bg-surface transition-colors cursor-pointer"
        >
          <List className="w-6 h-6" />
        </button>
      </header>

      {/* Mobile drawer */}
      <div className={`md:hidden fixed inset-0 z-50 ${drawerOpen ? '' : 'pointer-events-none'}`} aria-hidden={!drawerOpen}>
        <div
          onClick={closeDrawer}
          className={`absolute inset-0 bg-ink/40 transition-opacity duration-200 ${drawerOpen ? 'opacity-100' : 'opacity-0'}`}
        />
        <aside
          id="staff-drawer"
          role="dialog"
          aria-modal="true"
          aria-label="Staff menu"
          inert={!drawerOpen}
          className={`absolute inset-y-0 left-0 w-72 max-w-[85vw] bg-sunken border-r border-rule flex flex-col transition-transform duration-200 ease-out ${
            drawerOpen ? 'translate-x-0' : '-translate-x-full'
          }`}
        >
          <div className="h-14 px-4 flex items-center justify-between">
            <span className="font-serif text-xl tracking-tight text-ink">Staff</span>
            <button
              type="button"
              onClick={closeDrawer}
              aria-label="Close menu"
              className="-mr-2 w-10 h-10 inline-flex items-center justify-center rounded-md text-ink hover:bg-surface transition-colors cursor-pointer"
            >
              <X className="w-6 h-6" />
            </button>
          </div>
          <SidebarBody onNavigate={closeDrawer} />
        </aside>
      </div>
    </>
  );
};
