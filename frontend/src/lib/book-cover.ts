// Cloth colours for books without a published cover image.
const CLOTHS = ['#2e3e5c', '#2f4a3a', '#7a3522', '#2b2b2e', '#4a2f45', '#1f4e52', '#5a4a1f', '#3d3550'];

function hashString(str: string): number {
  let hash = 0;
  for (let i = 0; i < str.length; i++) {
    hash = (hash << 5) - hash + str.charCodeAt(i);
    hash |= 0;
  }
  return Math.abs(hash);
}

export function clothColor(title: string, isbn?: string): string {
  return CLOTHS[hashString(`${isbn || ''}${title}`) % CLOTHS.length];
}

/**
 * Open Library serves covers by ISBN. `default=false` makes it 404 when it has no
 * cover, so the <img> onError can fall back to the typeset cloth cover.
 */
export function coverUrl(isbn: string | undefined, size: 'S' | 'M' | 'L' = 'M'): string | null {
  const clean = isbn?.replace(/[^0-9Xx]/g, '');
  if (!clean || (clean.length !== 10 && clean.length !== 13)) return null;
  return `https://covers.openlibrary.org/b/isbn/${clean}-${size}.jpg?default=false`;
}
