'use client';

import React, { useState } from 'react';
import { clothColor, coverUrl } from '@/lib/book-cover';

export interface BookCoverProps {
  title: string;
  author?: string;
  isbn?: string;
  /** Large covers request the high-resolution image */
  large?: boolean;
  className?: string;
}

/**
 * Fluid 2:3 book cover. Width comes from the parent (or className).
 * Shows the real cover from Open Library when one exists, otherwise a typeset cloth cover.
 */
export const BookCover: React.FC<BookCoverProps> = ({
  title,
  author,
  isbn,
  large = false,
  className = '',
}) => {
  const src = coverUrl(isbn, large ? 'L' : 'M');
  const [failed, setFailed] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const showImage = src && !failed;

  return (
    <div
      className={`relative aspect-[2/3] w-full overflow-hidden rounded-[3px] shadow-cover select-none ${className}`}
      style={{ backgroundColor: clothColor(title, isbn) }}
    >
      {/* Typeset fallback, also visible while the image loads */}
      <div
        className={`absolute inset-0 flex flex-col justify-between p-[9%] text-[#f3f1ea] transition-opacity duration-300 ${
          showImage && loaded ? 'opacity-0' : 'opacity-100'
        }`}
        aria-hidden={Boolean(showImage && loaded)}
      >
        <div className="h-px w-1/3 bg-current opacity-50" />
        <p
          className={`font-serif leading-[1.1] ${large ? 'text-3xl' : 'text-[clamp(0.85rem,1.6vw,1.15rem)]'} line-clamp-5`}
        >
          {title}
        </p>
        {author ? (
          <p className={`${large ? 'text-sm' : 'text-[0.65rem]'} uppercase tracking-[0.12em] opacity-75 line-clamp-2`}>
            {author}
          </p>
        ) : (
          <span />
        )}
      </div>

      {showImage && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          src={src}
          alt={`Cover of ${title}`}
          loading="lazy"
          onLoad={() => setLoaded(true)}
          onError={() => setFailed(true)}
          className={`absolute inset-0 h-full w-full object-cover transition-opacity duration-300 ${
            loaded ? 'opacity-100' : 'opacity-0'
          }`}
        />
      )}

      {/* Subtle spine crease */}
      <div className="pointer-events-none absolute inset-y-0 left-0 w-[6%] bg-gradient-to-r from-black/25 to-transparent" />
    </div>
  );
};
