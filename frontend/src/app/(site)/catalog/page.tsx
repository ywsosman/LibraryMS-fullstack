'use client';

import React, { useDeferredValue, useMemo, useState, Suspense } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { MagnifyingGlass, CaretLeft, CaretRight } from '@phosphor-icons/react';
import { booksApi, authorsApi } from '@/lib/api';
import { Navbar } from '@/components/site/Navbar';
import { Footer } from '@/components/site/Footer';
import { BookCover } from '@/components/ui/BookCover';
import { Button } from '@/components/ui/Button';
import { fieldStyles } from '@/components/ui/Input';

const PAGE_SIZE = 15;

function CatalogContent() {
  const searchParams = useSearchParams();

  const [searchTitle, setSearchTitle] = useState('');
  const [selectedGenre, setSelectedGenre] = useState(searchParams.get('genre') || '');
  const [selectedAuthor, setSelectedAuthor] = useState('');
  const [searchIsbn, setSearchIsbn] = useState('');
  const [page, setPage] = useState(0);

  // Typing shouldn't fire a request per keystroke ahead of rendering
  const title = useDeferredValue(searchTitle);
  const isbn = useDeferredValue(searchIsbn);

  const { data: booksData, isLoading, isError, refetch, isFetching } = useQuery({
    queryKey: ['catalog-books', title, selectedAuthor, isbn, selectedGenre, page],
    queryFn: () =>
      booksApi.list({
        title: title || undefined,
        author: selectedAuthor || undefined,
        isbn: isbn || undefined,
        genre: selectedGenre || undefined,
        page,
        size: PAGE_SIZE,
      }),
    placeholderData: keepPreviousData,
  });

  const { data: authorsData } = useQuery({
    queryKey: ['all-authors-dropdown'],
    queryFn: () => authorsApi.list({ size: 100 }),
  });

  // There is no genres endpoint, so collect the subjects that actually exist in the catalog
  const { data: genreSource } = useQuery({
    queryKey: ['catalog-genre-source'],
    queryFn: () => booksApi.list({ size: 200 }),
    staleTime: 5 * 60 * 1000,
  });
  const genres = useMemo(() => {
    const set = new Set((genreSource?.content ?? []).flatMap((b) => b.genres ?? []));
    if (selectedGenre) set.add(selectedGenre);
    return Array.from(set).sort();
  }, [genreSource, selectedGenre]);

  const clearFilters = () => {
    setSearchTitle('');
    setSelectedGenre('');
    setSelectedAuthor('');
    setSearchIsbn('');
    setPage(0);
  };

  const hasActiveFilters = Boolean(searchTitle || selectedGenre || selectedAuthor || searchIsbn);
  const books = booksData?.content ?? [];

  return (
    <div className="min-h-[100dvh] flex flex-col">
      <Navbar />

      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 pt-12 pb-20">
        <h1 className="font-serif text-5xl sm:text-6xl tracking-tight text-ink">Catalog</h1>

        {/* Search */}
        <div className="mt-8 relative">
          <MagnifyingGlass className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-faint pointer-events-none" />
          <input
            type="search"
            aria-label="Search by title"
            placeholder="Search by title"
            value={searchTitle}
            onChange={(e) => {
              setSearchTitle(e.target.value);
              setPage(0);
            }}
            className={`${fieldStyles} h-14 pl-12 pr-4 text-lg`}
          />
        </div>

        {/* Filters */}
        <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-3">
          <div className="flex flex-col gap-1.5">
            <label htmlFor="filter-author" className="text-sm font-medium text-ink">Author</label>
            <select
              id="filter-author"
              value={selectedAuthor}
              onChange={(e) => {
                setSelectedAuthor(e.target.value);
                setPage(0);
              }}
              className={`${fieldStyles} h-10 px-3`}
            >
              <option value="">Any author</option>
              {authorsData?.content.map((author) => (
                <option key={author.id} value={author.name}>
                  {author.name}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="filter-genre" className="text-sm font-medium text-ink">Subject</label>
            <select
              id="filter-genre"
              value={selectedGenre}
              onChange={(e) => {
                setSelectedGenre(e.target.value);
                setPage(0);
              }}
              className={`${fieldStyles} h-10 px-3`}
            >
              <option value="">Any subject</option>
              {genres.map((g) => (
                <option key={g} value={g}>
                  {g}
                </option>
              ))}
            </select>
          </div>

          <div className="flex flex-col gap-1.5">
            <label htmlFor="filter-isbn" className="text-sm font-medium text-ink">ISBN</label>
            <input
              id="filter-isbn"
              inputMode="numeric"
              placeholder="978…"
              value={searchIsbn}
              onChange={(e) => {
                setSearchIsbn(e.target.value);
                setPage(0);
              }}
              className={`${fieldStyles} h-10 px-3 font-mono`}
            />
          </div>
        </div>

        {/* Result summary */}
        <div className="mt-10 mb-8 pb-4 border-b border-rule flex items-center justify-between gap-4 text-sm text-muted min-h-9">
          <span aria-live="polite">
            {isLoading
              ? 'Searching…'
              : isError
              ? ''
              : `${booksData?.totalElements ?? 0} ${booksData?.totalElements === 1 ? 'book' : 'books'}`}
          </span>
          {hasActiveFilters && (
            <button onClick={clearFilters} className="text-accent hover:underline underline-offset-4 cursor-pointer">
              Clear filters
            </button>
          )}
        </div>

        {/* Results */}
        {isLoading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-x-6 gap-y-12">
            {Array.from({ length: 10 }).map((_, i) => (
              <div key={i} className="animate-pulse">
                <div className="aspect-[2/3] rounded-[3px] bg-sunken" />
                <div className="h-4 w-4/5 bg-sunken rounded mt-3" />
                <div className="h-3 w-1/2 bg-sunken rounded mt-2" />
              </div>
            ))}
          </div>
        ) : isError ? (
          <div className="py-16 max-w-md">
            <h2 className="font-serif text-2xl text-ink">We couldn&apos;t load the catalog</h2>
            <p className="mt-2 text-muted">The library server didn&apos;t respond. Check that it&apos;s running, then try again.</p>
            <Button variant="secondary" className="mt-6" onClick={() => refetch()}>
              Try again
            </Button>
          </div>
        ) : books.length > 0 ? (
          <div
            className={`grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-x-6 gap-y-12 transition-opacity ${
              isFetching ? 'opacity-60' : 'opacity-100'
            }`}
          >
            {books.map((book) => (
              <Link key={book.id} href={`/books/${book.id}`} className="group">
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
                  {book.availableCopies > 0
                    ? `${book.availableCopies} of ${book.totalCopies} on the shelf`
                    : book.totalCopies > 0
                    ? 'All copies out'
                    : 'No copies yet'}
                </p>
              </Link>
            ))}
          </div>
        ) : (
          <div className="py-16 max-w-md">
            <h2 className="font-serif text-2xl text-ink">Nothing matches that search</h2>
            <p className="mt-2 text-muted">Try a shorter title, or clear the filters to see everything.</p>
            {hasActiveFilters && (
              <Button variant="secondary" className="mt-6" onClick={clearFilters}>
                Clear filters
              </Button>
            )}
          </div>
        )}

        {/* Pagination */}
        {booksData && booksData.totalPages > 1 && (
          <nav className="mt-16 flex items-center justify-between border-t border-rule pt-6" aria-label="Pagination">
            <Button
              variant="ghost"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              icon={<CaretLeft className="w-4 h-4" />}
            >
              Previous
            </Button>
            <span className="text-sm text-muted">
              Page {page + 1} of {booksData.totalPages}
            </span>
            <Button
              variant="ghost"
              size="sm"
              disabled={page >= booksData.totalPages - 1}
              onClick={() => setPage((p) => p + 1)}
              icon={<CaretRight className="w-4 h-4" />}
              className="flex-row-reverse"
            >
              Next
            </Button>
          </nav>
        )}
      </main>

      <Footer />
    </div>
  );
}

export default function CatalogPage() {
  return (
    <Suspense fallback={<div className="min-h-[100dvh]" />}>
      <CatalogContent />
    </Suspense>
  );
}
