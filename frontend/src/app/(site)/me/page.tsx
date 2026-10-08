'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useAuth } from '@/lib/auth-context';
import { loansApi, usersApi, membersApi } from '@/lib/api';
import type { LoanResponse } from '@/lib/types';
import { Navbar } from '@/components/site/Navbar';
import { Footer } from '@/components/site/Footer';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { BookCover } from '@/components/ui/BookCover';

const MAX_OPEN_LOANS = 5; // app.loans.max-open-loans
const DAY = 24 * 60 * 60 * 1000;

type Feedback = { type: 'success' | 'error'; message: string } | null;

function dueLabel(loan: LoanResponse) {
  const days = Math.ceil((new Date(loan.dueDate).getTime() - Date.now()) / DAY);
  if (loan.overdue || days < 0) {
    const late = Math.max(1, -days);
    return { text: `Overdue by ${late} ${late === 1 ? 'day' : 'days'}`, late: true };
  }
  if (days === 0) return { text: 'Due today', late: false };
  if (days === 1) return { text: 'Due tomorrow', late: false };
  return { text: `Due in ${days} days`, late: false };
}

const formatDate = (d: string) =>
  new Date(d).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });

const money = (n: number) => `$${n.toFixed(2)}`;

function FeedbackLine({ feedback }: { feedback: Feedback }) {
  if (!feedback) return null;
  return (
    <p role="status" className={`text-sm ${feedback.type === 'success' ? 'text-success' : 'text-danger'}`}>
      {feedback.message}
    </p>
  );
}

export default function MyLoansPage() {
  const router = useRouter();
  const { user, member, isAuthenticated, isLoading: isAuthLoading, refreshProfile } = useAuth();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [pwdFeedback, setPwdFeedback] = useState<Feedback>(null);

  const [memberFullName, setMemberFullName] = useState('');
  const [memberPhone, setMemberPhone] = useState('');
  const [memberFeedback, setMemberFeedback] = useState<Feedback>(null);

  // Only redirect once the session check has finished, otherwise a refresh bounces signed-in readers
  useEffect(() => {
    if (!isAuthLoading && !isAuthenticated) router.replace('/login');
  }, [isAuthLoading, isAuthenticated, router]);

  const { data: loansData, isLoading: isLoansLoading, isError: isLoansError } = useQuery({
    queryKey: ['my-loans'],
    queryFn: () => loansApi.listMy(0, 50),
    enabled: isAuthenticated && Boolean(member),
  });

  const passwordMutation = useMutation({
    mutationFn: () => usersApi.changePassword({ currentPassword, newPassword }),
    onSuccess: () => {
      setPwdFeedback({ type: 'success', message: 'Password changed.' });
      setCurrentPassword('');
      setNewPassword('');
    },
    onError: (err: { message?: string }) => {
      setPwdFeedback({ type: 'error', message: err.message || 'We couldn’t change your password.' });
    },
  });

  const createMemberMutation = useMutation({
    mutationFn: async () => {
      await membersApi.activateMine({ fullName: memberFullName, phone: memberPhone || undefined });
      await refreshProfile();
    },
    onSuccess: () => {
      setMemberFeedback({ type: 'success', message: 'Your card is active. You can borrow now.' });
      setMemberFullName('');
      setMemberPhone('');
    },
    onError: (err: { message?: string }) => {
      setMemberFeedback({ type: 'error', message: err.message || 'We couldn’t activate your card.' });
    },
  });

  if (isAuthLoading || !isAuthenticated) {
    return <div className="min-h-[100dvh]" />;
  }

  const loans = loansData?.content ?? [];
  const openLoans = loans.filter((l) => l.open);
  const pastLoans = loans.filter((l) => !l.open);
  const totalFines = loans.reduce((acc, l) => acc + (Number(l.fineAmount) || 0), 0);

  return (
    <div className="min-h-[100dvh] flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 pt-12 pb-20">
        <p className="text-muted">Hello, {member?.fullName?.split(' ')[0] || user?.username}.</p>
        <h1 className="mt-1 font-serif text-5xl sm:text-6xl tracking-tight text-ink">Your loans</h1>

        <div className="mt-12 grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-16">
          {/* Loans */}
          <div className="lg:col-span-8">
            {!member ? (
              <div className="py-10 border-y border-rule">
                <h2 className="font-serif text-2xl text-ink">Activate your card to start borrowing</h2>
                <p className="mt-2 text-muted max-w-md">
                  Your account isn&apos;t linked to a library card yet. Fill in the form on this page and you can borrow straight away.
                </p>
              </div>
            ) : isLoansLoading ? (
              <div className="space-y-6 animate-pulse">
                {Array.from({ length: 2 }).map((_, i) => (
                  <div key={i} className="flex gap-5">
                    <div className="w-16 aspect-[2/3] bg-sunken rounded-[3px]" />
                    <div className="flex-1 space-y-2 pt-1">
                      <div className="h-5 w-1/2 bg-sunken rounded" />
                      <div className="h-4 w-1/4 bg-sunken rounded" />
                    </div>
                  </div>
                ))}
              </div>
            ) : isLoansError ? (
              <p className="text-danger">We couldn&apos;t load your loans. Refresh the page to try again.</p>
            ) : (
              <>
                <div className="flex items-baseline justify-between gap-4 pb-4 border-b border-rule">
                  <h2 className="font-serif text-2xl text-ink">Checked out</h2>
                  <span className="text-sm text-muted">
                    {openLoans.length} of {MAX_OPEN_LOANS}
                  </span>
                </div>

                {openLoans.length > 0 ? (
                  <ul className="divide-y divide-rule">
                    {openLoans.map((loan) => {
                      const due = dueLabel(loan);
                      return (
                        <li key={loan.id} className="py-5 flex gap-5">
                          <Link href={`/books/${loan.book.id}`} className="w-16 shrink-0">
                            <BookCover title={loan.book.title} isbn={loan.book.isbn} />
                          </Link>
                          <div className="flex-1 min-w-0 flex flex-col sm:flex-row sm:items-start justify-between gap-2">
                            <div className="min-w-0">
                              <Link
                                href={`/books/${loan.book.id}`}
                                className="font-medium text-ink hover:text-accent transition-colors"
                              >
                                {loan.book.title}
                              </Link>
                              <p className="mt-0.5 text-sm text-muted">
                                Borrowed {formatDate(loan.borrowedAt)}
                                <span className="font-mono text-faint"> · {loan.copy.barcode}</span>
                              </p>
                            </div>
                            <div className="sm:text-right shrink-0">
                              <p className={`text-sm font-medium ${due.late ? 'text-danger' : 'text-ink'}`}>{due.text}</p>
                              <p className="text-sm text-muted">
                                {due.late ? `Was due ${formatDate(loan.dueDate)}` : formatDate(loan.dueDate)}
                              </p>
                            </div>
                          </div>
                        </li>
                      );
                    })}
                  </ul>
                ) : (
                  <div className="py-10">
                    <p className="text-muted">Nothing checked out right now.</p>
                    <Link href="/catalog" className="inline-block mt-3 text-accent hover:underline underline-offset-4">
                      Find something to read
                    </Link>
                  </div>
                )}

                {pastLoans.length > 0 && (
                  <section className="mt-14">
                    <h2 className="font-serif text-2xl text-ink pb-4 border-b border-rule">Returned</h2>
                    <ul className="divide-y divide-rule">
                      {pastLoans.slice(0, 10).map((loan) => (
                        <li key={loan.id} className="py-3 flex items-center justify-between gap-4 text-sm">
                          <Link href={`/books/${loan.book.id}`} className="text-ink hover:text-accent transition-colors truncate">
                            {loan.book.title}
                          </Link>
                          <span className="text-muted shrink-0">
                            {loan.returnedAt ? `Returned ${formatDate(loan.returnedAt)}` : 'Returned'}
                            {loan.fineAmount > 0 && <span className="text-danger"> · {money(loan.fineAmount)}</span>}
                          </span>
                        </li>
                      ))}
                    </ul>
                  </section>
                )}
              </>
            )}
          </div>

          {/* Card and account */}
          <aside className="lg:col-span-4 space-y-12">
            <section>
              <h2 className="font-serif text-2xl text-ink">Library card</h2>
              {member ? (
                <dl className="mt-4 space-y-3 text-sm">
                  <div className="flex justify-between gap-4">
                    <dt className="text-muted">Name</dt>
                    <dd className="text-ink text-right">{member.fullName}</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-muted">Card number</dt>
                    <dd className="text-ink font-mono">{String(member.id).padStart(6, '0')}</dd>
                  </div>
                  <div className="flex justify-between gap-4">
                    <dt className="text-muted">Email</dt>
                    <dd className="text-ink text-right truncate">{member.email}</dd>
                  </div>
                  {member.phone && (
                    <div className="flex justify-between gap-4">
                      <dt className="text-muted">Phone</dt>
                      <dd className="text-ink">{member.phone}</dd>
                    </div>
                  )}
                  {totalFines > 0 && (
                    <div className="flex justify-between gap-4 pt-3 border-t border-rule">
                      <dt className="text-danger">Fines</dt>
                      <dd className="text-danger font-medium">{money(totalFines)}</dd>
                    </div>
                  )}
                </dl>
              ) : (
                <form
                  className="mt-4 space-y-4"
                  onSubmit={(e) => {
                    e.preventDefault();
                    createMemberMutation.mutate();
                  }}
                >
                  <Input
                    label="Full name"
                    autoComplete="name"
                    value={memberFullName}
                    onChange={(e) => setMemberFullName(e.target.value)}
                    required
                  />
                  <Input
                    label="Phone (optional)"
                    type="tel"
                    autoComplete="tel"
                    value={memberPhone}
                    onChange={(e) => setMemberPhone(e.target.value)}
                  />
                  <p className="text-sm text-muted">Your card uses your account email, {user?.email}.</p>
                  <FeedbackLine feedback={memberFeedback} />
                  <Button
                    type="submit"
                    className="w-full"
                    disabled={!memberFullName.trim()}
                    isLoading={createMemberMutation.isPending}
                  >
                    Activate card
                  </Button>
                </form>
              )}
            </section>

            <section>
              <h2 className="font-serif text-2xl text-ink">Password</h2>
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  setPwdFeedback(null);
                  passwordMutation.mutate();
                }}
                className="mt-4 space-y-4"
              >
                <Input
                  label="Current password"
                  type="password"
                  autoComplete="current-password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  required
                />
                <Input
                  label="New password"
                  type="password"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  required
                  minLength={10}
                  helperText="At least 10 characters."
                />
                <FeedbackLine feedback={pwdFeedback} />
                <Button type="submit" variant="secondary" className="w-full" isLoading={passwordMutation.isPending}>
                  Change password
                </Button>
              </form>
            </section>
          </aside>
        </div>
      </main>

      <Footer />
    </div>
  );
}
