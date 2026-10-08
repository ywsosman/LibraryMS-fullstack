'use client';

import React, { useState, Suspense } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, CheckCircle, WarningCircle } from '@phosphor-icons/react';
import { booksApi, loansApi } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { Navbar } from '@/components/site/Navbar';
import { Footer } from '@/components/site/Footer';
import { BookCover } from '@/components/ui/BookCover';
import { Button } from '@/components/ui/Button';
import { Badge } from '@/components/ui/Badge';

function publishedYear(date?: string) {
  const year = Number(date?.slice(0, 4));
  return Number.isFinite(year) && year > 0 ? String(year) : null;
}

function BookDetailContent() {
  const params = useParams();
  const bookId = Number(params.id);
  const { isAuthenticated, isLoading: isAuthLoading } = useAuth();
  const queryClient = useQueryClient();

  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const { data: book, isLoading: isBookLoading, isError } = useQuery({
    queryKey: ['book-detail', bookId],
    queryFn: () => booksApi.get(bookId),
    enabled: Boolean(bookId),
  });

  const { data: copiesData, isLoading: isCopiesLoading } = useQuery({
    queryKey: ['book-copies', bookId],
    queryFn: () => booksApi.getCopies(bookId),
    enabled: Boolean(bookId),
  });

  const borrowMutation = useMutation({
    mutationFn: (copyId: number) => loansApi.create({ copyId }),
    onSuccess: (loan) => {
      setFeedback({
        type: 'success',
        message: `It's yours. Please return it by ${new Date(loan.dueDate).toLocaleDateString(undefined, {
          month: 'long',
          day: 'numeric',
        })}.`,
      });
      queryClient.invalidateQueries({ queryKey: ['book-detail', bookId] });
      queryClient.invalidateQueries({ queryKey: ['book-copies', bookId] });
      queryClient.invalidateQueries({ queryKey: ['my-loans'] });
      // Staff views cached in this session (an admin borrowing for themselves)
      queryClient.invalidateQueries({ queryKey: ['admin-loans'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats-open-loans'] });
    },
    onError: (err: { message?: string }) => {
      setFeedback({ type: 'error', message: err.message || 'We couldn’t complete that loan.' });
    },
  });

  if (isBookLoading) {
    return (
      <Shell>
        <div className="grid grid-cols-1 md:grid-cols-12 gap-10 md:gap-16 animate-pulse">
          <div className="md:col-span-4 aspect-[2/3] max-w-xs rounded-[3px] bg-sunken" />
          <div className="md:col-span-8 space-y-4">
            <div className="h-12 w-3/4 bg-sunken rounded" />
            <div className="h-6 w-1/3 bg-sunken rounded" />
            <div className="h-24 w-full bg-sunken rounded mt-8" />
          </div>
        </div>
      </Shell>
    );
  }

  if (!book) {
    return (
      <Shell>
        <div className="py-16 max-w-md">
          <h1 className="font-serif text-4xl text-ink">
            {isError ? 'We couldn’t find that book' : 'Book not found'}
          </h1>
          <p className="mt-3 text-muted">It may have been removed from the catalog, or the server isn&apos;t responding.</p>
          <Link href="/catalog" className="inline-block mt-6 text-accent hover:underline underline-offset-4">
            Back to the catalog
          </Link>
        </div>
      </Shell>
    );
  }

  const copies = copiesData?.content ?? [];
  const availableCopies = copies.filter((c) => c.status === 'AVAILABLE');
  const year = publishedYear(book.publishedDate);

  return (
    <Shell>
      <div className="grid grid-cols-1 md:grid-cols-12 gap-10 md:gap-16">
        <div className="md:col-span-4">
          <div className="max-w-[280px] md:max-w-none md:sticky md:top-24">
            <BookCover title={book.title} author={book.authors?.[0]?.name} isbn={book.isbn} large />
          </div>
        </div>

        <div className="md:col-span-8">
          {book.genres?.length > 0 && (
            <p className="text-sm text-muted">
              {book.genres.map((g, i) => (
                <React.Fragment key={g}>
                  {i > 0 && ', '}
                  <Link href={`/catalog?genre=${encodeURIComponent(g)}`} className="hover:text-accent transition-colors">
                    {g}
                  </Link>
                </React.Fragment>
              ))}
            </p>
          )}

          <h1 className="mt-2 font-serif text-4xl sm:text-5xl leading-[1.08] tracking-tight text-ink">{book.title}</h1>
          <p className="mt-3 font-serif text-xl sm:text-2xl italic text-muted pb-1">
            {book.authors?.map((a) => a.name).join(', ') || 'Unknown author'}
          </p>

          {/* Borrowing */}
          <div className="mt-8 p-5 rounded-md bg-surface border border-rule flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <p className="text-ink font-medium">
                {book.availableCopies > 0
                  ? `${book.availableCopies} of ${book.totalCopies} ${book.totalCopies === 1 ? 'copy' : 'copies'} on the shelf`
                  : book.totalCopies > 0
                  ? 'All copies are out right now'
                  : 'The library has no copies of this yet'}
              </p>
              <p className="text-sm text-muted mt-0.5">Loans last 14 days.</p>
            </div>
            {availableCopies.length > 0 &&
              (isAuthenticated ? (
                <Button
                  size="lg"
                  isLoading={borrowMutation.isPending}
                  onClick={() => borrowMutation.mutate(availableCopies[0].id)}
                >
                  Borrow this book
                </Button>
              ) : (
                <Link
                  href="/login"
                  className={`inline-flex items-center justify-center h-12 px-6 rounded-md bg-accent text-on-accent font-medium hover:bg-accent-hover transition-colors whitespace-nowrap ${
                    isAuthLoading ? 'invisible' : ''
                  }`}
                >
                  Sign in to borrow
                </Link>
              ))}
          </div>

          {feedback && (
            <div
              role="status"
              className={`mt-4 flex items-start gap-2.5 text-sm ${feedback.type === 'success' ? 'text-success' : 'text-danger'}`}
            >
              {feedback.type === 'success' ? (
                <CheckCircle weight="fill" className="w-5 h-5 shrink-0" />
              ) : (
                <WarningCircle weight="fill" className="w-5 h-5 shrink-0" />
              )}
              <span>
                {feedback.message}
                {feedback.type === 'success' && (
                  <>
                    {' '}
                    <Link href="/me" className="underline underline-offset-4">See your loans</Link>
                  </>
                )}
              </span>
            </div>
          )}

          {book.description && (
            <p className="mt-10 font-serif text-lg sm:text-xl leading-relaxed text-ink max-w-[65ch]">{book.description}</p>
          )}

          <dl className="mt-10 grid grid-cols-2 sm:grid-cols-3 gap-6 border-t border-rule pt-6">
            <div>
              <dt className="text-sm text-muted">ISBN</dt>
              <dd className="mt-1 font-mono text-sm text-ink">{book.isbn}</dd>
            </div>
            {year && (
              <div>
                <dt className="text-sm text-muted">First published</dt>
                <dd className="mt-1 text-ink">{year}</dd>
              </div>
            )}
            <div>
              <dt className="text-sm text-muted">Copies held</dt>
              <dd className="mt-1 text-ink">{book.totalCopies}</dd>
            </div>
          </dl>

          {/* Copies */}
          <section className="mt-14">
            <h2 className="font-serif text-2xl text-ink">Copies</h2>
            {isCopiesLoading ? (
              <p className="mt-4 text-sm text-muted">Checking the shelves…</p>
            ) : copies.length > 0 ? (
              <ul className="mt-4 divide-y divide-rule border-y border-rule">
                {copies.map((copy) => (
                  <li key={copy.id} className="py-3 flex items-center justify-between gap-4">
                    <span className="font-mono text-sm text-ink">{copy.barcode}</span>
                    <div className="flex items-center gap-4">
                      <Badge
                        size="sm"
                        variant={copy.status === 'AVAILABLE' ? 'available' : copy.status === 'ON_LOAN' ? 'on_loan' : 'lost'}
                      >
                        {copy.status === 'AVAILABLE' ? 'On the shelf' : copy.status}
                      </Badge>
                      {copy.status === 'AVAILABLE' && isAuthenticated && (
                        <button
                          disabled={borrowMutation.isPending}
                          onClick={() => borrowMutation.mutate(copy.id)}
                          className="text-sm text-accent hover:underline underline-offset-4 disabled:opacity-50 cursor-pointer"
                        >
                          Borrow
                        </button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-4 text-sm text-muted">No copies have been added for this title yet.</p>
            )}
          </section>
        </div>
      </div>
    </Shell>
  );
}

function Shell({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-[100dvh] flex flex-col">
      <Navbar />
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 pt-8 pb-20">
        <Link
          href="/catalog"
          className="inline-flex items-center gap-1.5 text-sm text-muted hover:text-ink transition-colors mb-8"
        >
          <ArrowLeft className="w-4 h-4" />
          Catalog
        </Link>
        {children}
      </main>
      <Footer />
    </div>
  );
}

export default function BookDetailPage() {
  return (
    <Suspense fallback={<div className="min-h-[100dvh]" />}>
      <BookDetailContent />
    </Suspense>
  );
}
