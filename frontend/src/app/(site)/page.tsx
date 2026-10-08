'use client';

import React, { useMemo, useRef } from 'react';
import Link from 'next/link';
import { useQuery } from '@tanstack/react-query';
import { ArrowRight } from '@phosphor-icons/react';
import { booksApi } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { gsap, useGSAP } from '@/lib/gsap';
import type { BookResponse } from '@/lib/types';
import { Navbar } from '@/components/site/Navbar';
import { Footer } from '@/components/site/Footer';
import { BookCover } from '@/components/ui/BookCover';

export default function HomePage() {
  const { isAuthenticated } = useAuth();
  const { data, isLoading, isError } = useQuery({
    queryKey: ['home-books'],
    queryFn: () => booksApi.list({ size: 24, sort: 'createdAt,desc' }),
  });

  const books = useMemo(() => data?.content ?? [], [data]);
  const genres = useMemo(
    () => Array.from(new Set(books.flatMap((b) => b.genres ?? []))).sort(),
    [books]
  );

  return (
    <div className="min-h-[100dvh] flex flex-col">
      <Navbar />
      <main className="flex-1">
        <Hero books={books.slice(0, 3)} isAuthenticated={isAuthenticated} />
        <Shelf books={books.slice(0, 6)} total={data?.totalElements} isLoading={isLoading} isError={isError} />
        {genres.length > 0 && <GenreIndex genres={genres} />}
        <LendingRules />
      </main>
      <Footer />
    </div>
  );
}

function Hero({ books, isAuthenticated }: { books: BookResponse[]; isAuthenticated: boolean }) {
  const scope = useRef<HTMLElement>(null);

  // Text settles in first, covers follow: the eye lands on the headline, then the books.
  useGSAP(
    () => {
      const mm = gsap.matchMedia();
      mm.add('(prefers-reduced-motion: no-preference)', () => {
        gsap.from('[data-hero-text]', { y: 18, opacity: 0, duration: 0.7, ease: 'power3.out', stagger: 0.08 });
        gsap.from('[data-hero-cover]', { y: 40, opacity: 0, duration: 0.9, ease: 'power3.out', stagger: 0.12, delay: 0.2 });
      });
    },
    { scope, dependencies: [books.length], revertOnUpdate: true }
  );

  return (
    <section ref={scope} className="max-w-6xl mx-auto px-4 sm:px-6 pt-12 sm:pt-20 pb-16 grid grid-cols-1 lg:grid-cols-12 gap-12 lg:gap-8 items-center">
      <div className="lg:col-span-6">
        <h1 data-hero-text className="font-serif text-5xl sm:text-6xl lg:text-7xl leading-[1.04] tracking-tight text-ink">
          Find a book. <em className="text-accent pb-1">Borrow</em> the copy on the shelf.
        </h1>
        <p data-hero-text className="mt-6 text-lg text-muted leading-relaxed max-w-md">
          Search the whole collection, see which copies are in right now, and check one out in a click.
        </p>
        <div data-hero-text className="mt-8 flex flex-wrap items-center gap-3">
          <Link
            href="/catalog"
            className="inline-flex items-center gap-2 h-12 px-6 rounded-md bg-accent text-on-accent font-medium hover:bg-accent-hover transition-colors active:translate-y-px"
          >
            Browse the catalog
            <ArrowRight className="w-4 h-4" />
          </Link>
          {!isAuthenticated && (
            <Link
              href="/register"
              className="inline-flex items-center h-12 px-6 rounded-md border border-rule-strong text-ink font-medium hover:bg-sunken transition-colors"
            >
              Get a library card
            </Link>
          )}
        </div>
      </div>

      <div className="lg:col-span-6">
        {books.length >= 3 ? (
          <div className="relative mx-auto max-w-md h-[340px] sm:h-[420px]">
            {books.map((book, i) => (
              // GSAP owns the inner element's transform; position and tilt live on the wrapper.
              <Link
                key={book.id}
                href={`/books/${book.id}`}
                aria-label={book.title}
                className={`group absolute w-[44%] ${
                  ['left-0 top-10 -rotate-6', 'left-[28%] top-0 z-10', 'right-0 top-12 rotate-6'][i]
                }`}
              >
                <div data-hero-cover>
                  <div className="transition-transform duration-300 group-hover:-translate-y-2">
                    <BookCover title={book.title} author={book.authors?.[0]?.name} isbn={book.isbn} large />
                  </div>
                </div>
              </Link>
            ))}
          </div>
        ) : (
          // eslint-disable-next-line @next/next/no-img-element
          <img
            data-hero-cover
            src="/hero-library.jpg"
            alt="A reading room lined with wooden bookshelves"
            className="w-full aspect-[4/3] object-cover rounded-md"
          />
        )}
      </div>
    </section>
  );
}

function Shelf({
  books,
  total,
  isLoading,
  isError,
}: {
  books: BookResponse[];
  total?: number;
  isLoading: boolean;
  isError: boolean;
}) {
  const scope = useRef<HTMLElement>(null);

  // Reveal the shelf as it scrolls in, once.
  useGSAP(
    () => {
      if (!books.length) return;
      const mm = gsap.matchMedia();
      mm.add('(prefers-reduced-motion: no-preference)', () => {
        gsap.from('[data-shelf-item]', {
          y: 24,
          opacity: 0,
          duration: 0.6,
          ease: 'power2.out',
          stagger: 0.06,
          scrollTrigger: { trigger: scope.current, start: 'top 80%', once: true },
        });
      });
    },
    { scope, dependencies: [books.length], revertOnUpdate: true }
  );

  return (
    <section ref={scope} className="border-t border-rule">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-16 sm:py-20">
        <div className="flex items-baseline justify-between gap-4 mb-10">
          <h2 className="font-serif text-3xl sm:text-4xl tracking-tight text-ink">New on the shelves</h2>
          <Link href="/catalog" className="text-sm text-accent hover:underline underline-offset-4 whitespace-nowrap">
            {total ? `See all ${total} books` : 'See all books'}
          </Link>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-x-5 gap-y-10">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="animate-pulse">
                <div className="aspect-[2/3] rounded-[3px] bg-sunken" />
                <div className="h-4 w-4/5 bg-sunken rounded mt-3" />
                <div className="h-3 w-1/2 bg-sunken rounded mt-2" />
              </div>
            ))}
          </div>
        ) : isError ? (
          <p className="text-muted max-w-md">
            The catalog isn&apos;t reachable right now, so the shelf is empty. Try again in a minute.
          </p>
        ) : books.length === 0 ? (
          <p className="text-muted max-w-md">No books have been added yet. Staff can add the first titles from the staff area.</p>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-x-5 gap-y-10">
            {books.map((book) => (
              <Link key={book.id} href={`/books/${book.id}`} data-shelf-item className="group">
                <div className="transition-transform duration-300 group-hover:-translate-y-1">
                  <BookCover title={book.title} author={book.authors?.[0]?.name} isbn={book.isbn} />
                </div>
                <h3 className="mt-3 text-[15px] font-medium leading-snug text-ink group-hover:text-accent transition-colors line-clamp-2">
                  {book.title}
                </h3>
                <p className="text-sm text-muted line-clamp-1">
                  {book.authors?.map((a) => a.name).join(', ')}
                </p>
                <p className={`text-sm mt-1 ${book.availableCopies > 0 ? 'text-success' : 'text-faint'}`}>
                  {book.availableCopies > 0 ? `${book.availableCopies} on the shelf` : 'All copies out'}
                </p>
              </Link>
            ))}
          </div>
        )}
      </div>
    </section>
  );
}

function GenreIndex({ genres }: { genres: string[] }) {
  return (
    <section className="bg-sunken">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-16 sm:py-20 grid grid-cols-1 md:grid-cols-12 gap-8">
        <h2 className="md:col-span-4 font-serif text-3xl sm:text-4xl tracking-tight text-ink">Browse by subject</h2>
        <ul className="md:col-span-8 flex flex-wrap gap-x-8 gap-y-3">
          {genres.map((genre) => (
            <li key={genre}>
              <Link
                href={`/catalog?genre=${encodeURIComponent(genre)}`}
                className="font-serif text-2xl sm:text-3xl text-muted hover:text-accent transition-colors"
              >
                {genre}
              </Link>
            </li>
          ))}
        </ul>
      </div>
    </section>
  );
}

// Mirrors the backend defaults in application.properties (app.loans.*).
const RULES = [
  { term: 'Loan period', detail: '14 days from the day you borrow' },
  { term: 'Books at once', detail: 'Up to 5 open loans per card' },
  { term: 'Late returns', detail: '$0.50 per day after the due date' },
];

function LendingRules() {
  return (
    <section className="border-t border-rule">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 py-16 sm:py-24 grid grid-cols-1 lg:grid-cols-12 gap-10 lg:gap-16 items-center">
        {/* eslint-disable-next-line @next/next/no-img-element */}
        <img
          src="/hero-library.jpg"
          alt="A reading room lined with wooden bookshelves"
          loading="lazy"
          className="lg:col-span-7 w-full aspect-[16/10] object-cover rounded-md"
        />
        <div className="lg:col-span-5">
          <h2 className="font-serif text-3xl sm:text-4xl tracking-tight text-ink">How borrowing works</h2>
          <dl className="mt-8 divide-y divide-rule border-y border-rule">
            {RULES.map((rule) => (
              <div key={rule.term} className="py-4 grid grid-cols-[8rem_1fr] gap-4">
                <dt className="text-sm text-muted">{rule.term}</dt>
                <dd className="text-ink">{rule.detail}</dd>
              </div>
            ))}
          </dl>
          <p className="mt-6 text-muted">
            Bring the book back to the desk and the copy goes straight back on the shelf for the next reader.
          </p>
        </div>
      </div>
    </section>
  );
}
