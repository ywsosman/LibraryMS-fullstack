'use client';

import React from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { Plus, ArrowsLeftRight } from '@phosphor-icons/react';
import { booksApi, copiesApi, loansApi, membersApi, auditLogsApi } from '@/lib/api';
import { lateLabel } from '@/lib/loans';
import { StatCard } from '@/components/admin/StatCard';
import { Button } from '@/components/ui/Button';

const VERBS: Record<string, string> = {
  CREATE: 'added',
  UPDATE: 'updated',
  DELETE: 'deleted',
  RETURN: 'checked in',
  LOGIN: 'signed in as',
};

const timeAgo = (iso: string) => {
  const mins = Math.round((Date.now() - new Date(iso).getTime()) / 60000);
  if (mins < 1) return 'just now';
  if (mins < 60) return `${mins} min ago`;
  const hours = Math.round(mins / 60);
  if (hours < 24) return `${hours} h ago`;
  return new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
};

export default function AdminDashboardPage() {
  const { data: booksData } = useQuery({
    queryKey: ['admin-stats-books'],
    queryFn: () => booksApi.list({ size: 1 }),
  });

  const { data: copiesData } = useQuery({
    queryKey: ['admin-stats-copies'],
    queryFn: () => copiesApi.list({ size: 1 }),
  });

  const { data: openLoansData } = useQuery({
    queryKey: ['admin-stats-open-loans'],
    queryFn: () => loansApi.list({ status: 'OPEN', size: 1 }),
  });

  const { data: overdueLoansData, isLoading: isOverdueLoading } = useQuery({
    queryKey: ['admin-stats-overdue-loans'],
    queryFn: () => loansApi.list({ status: 'OVERDUE', size: 6 }),
  });

  const { data: membersData } = useQuery({
    queryKey: ['admin-stats-members'],
    queryFn: () => membersApi.list({ size: 1 }),
  });

  const { data: recentAudit, isLoading: isAuditLoading } = useQuery({
    queryKey: ['admin-recent-audit'],
    queryFn: () => auditLogsApi.list({ size: 8 }),
  });

  const overdueList = overdueLoansData?.content ?? [];
  const auditEntries = recentAudit?.content ?? [];

  return (
    <div>
      <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4">
        <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">Overview</h1>
        <div className="flex items-center gap-2">
          <Link href="/admin/loans">
            <Button size="sm" icon={<ArrowsLeftRight className="w-4 h-4" />}>
              Check out a book
            </Button>
          </Link>
          <Link href="/admin/books">
            <Button variant="secondary" size="sm" icon={<Plus className="w-4 h-4" />}>
              Add a book
            </Button>
          </Link>
        </div>
      </div>

      <div className="mt-10 grid grid-cols-2 lg:grid-cols-5 border-y border-rule sm:divide-x divide-rule">
        <StatCard label="Titles" value={booksData?.totalElements} href="/admin/books" />
        <StatCard label="Copies" value={copiesData?.totalElements} href="/admin/copies" />
        <StatCard label="On loan" value={openLoansData?.totalElements} href="/admin/loans" />
        <StatCard label="Overdue" value={overdueLoansData?.totalElements} href="/admin/loans" tone="danger" />
        <StatCard label="Members" value={membersData?.totalElements} href="/admin/members" />
      </div>

      <div className="mt-14 grid grid-cols-1 lg:grid-cols-12 gap-12">
        <section className="lg:col-span-7">
          <div className="flex items-baseline justify-between gap-4 pb-3 border-b border-rule">
            <h2 className="font-serif text-2xl text-ink">Overdue</h2>
            <Link href="/admin/loans" className="text-sm text-accent hover:underline underline-offset-4">
              All loans
            </Link>
          </div>
          {isOverdueLoading ? (
            <p className="py-6 text-sm text-muted">Loading…</p>
          ) : overdueList.length > 0 ? (
            <ul className="divide-y divide-rule">
              {overdueList.map((loan) => (
                <li key={loan.id} className="py-3.5 flex items-start justify-between gap-4">
                  <div className="min-w-0">
                    <p className="text-ink truncate">{loan.book.title}</p>
                    <p className="text-sm text-muted">
                      {loan.member.fullName} <span className="font-mono text-faint">· {loan.copy.barcode}</span>
                    </p>
                  </div>
                  <div className="text-right shrink-0">
                    <p className="text-sm font-medium text-danger">{lateLabel(loan.dueDate)}</p>
                    <p className="text-sm text-muted">
                      Due {new Date(loan.dueDate).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}
                    </p>
                  </div>
                </li>
              ))}
            </ul>
          ) : (
            <p className="py-6 text-muted">Nothing is overdue. Every book out is still within its loan period.</p>
          )}
        </section>

        <section className="lg:col-span-5">
          <div className="flex items-baseline justify-between gap-4 pb-3 border-b border-rule">
            <h2 className="font-serif text-2xl text-ink">Recent activity</h2>
            <Link href="/admin/audit" className="text-sm text-accent hover:underline underline-offset-4">
              Full log
            </Link>
          </div>
          {isAuditLoading ? (
            <p className="py-6 text-sm text-muted">Loading…</p>
          ) : auditEntries.length > 0 ? (
            <ul className="divide-y divide-rule">
              {auditEntries.map((log) => (
                <li key={log.id} className="py-3 flex items-start justify-between gap-4 text-sm">
                  <p className="text-ink">
                    <span className="font-medium">{log.username || 'System'}</span>{' '}
                    <span className="text-muted">{VERBS[log.operation] ?? log.operation.toLowerCase().replace(/_/g, ' ')}</span>{' '}
                    {log.entityType.toLowerCase()}
                    {log.entityId ? ` #${log.entityId}` : ''}
                  </p>
                  <span className="text-faint shrink-0">{timeAgo(log.occurredAt)}</span>
                </li>
              ))}
            </ul>
          ) : (
            <p className="py-6 text-muted">No activity recorded yet.</p>
          )}
        </section>
      </div>
    </div>
  );
}
