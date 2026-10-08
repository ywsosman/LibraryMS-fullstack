'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { booksApi, authorsApi } from '@/lib/api';
import { BookResponse, CreateBookRequest } from '@/lib/types';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Pagination } from '@/components/admin/Pagination';
import {
  Plus,
  PencilSimple,
  Trash,
  Barcode,
  MagnifyingGlass,
  WarningCircle,
} from '@phosphor-icons/react';

export default function AdminBooksPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);

  // Modals state
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingBook, setEditingBook] = useState<BookResponse | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  // Form state
  const [title, setTitle] = useState('');
  const [isbn, setIsbn] = useState('');
  const [publishedDate, setPublishedDate] = useState('2024-01-15');
  const [description, setDescription] = useState('');
  const [selectedAuthorIds, setSelectedAuthorIds] = useState<number[]>([]);
  const [genresInput, setGenresInput] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const { data: booksData, isLoading } = useQuery({
    queryKey: ['admin-books', searchTerm, page],
    queryFn: () => booksApi.list({ title: searchTerm || undefined, page, size: 10 }),
  });

  const { data: authorsData } = useQuery({
    queryKey: ['admin-authors-list'],
    queryFn: () => authorsApi.list({ size: 100 }),
  });

  const resetForm = () => {
    setTitle('');
    setIsbn('');
    setPublishedDate('2024-01-15');
    setDescription('');
    setSelectedAuthorIds([]);
    setGenresInput('');
    setFormError(null);
  };

  const openCreateModal = () => {
    resetForm();
    setIsAddOpen(true);
  };

  const openEditModal = (book: BookResponse) => {
    setTitle(book.title);
    setIsbn(book.isbn);
    setPublishedDate(book.publishedDate);
    setDescription(book.description || '');
    setSelectedAuthorIds(book.authors?.map((a) => a.id) || []);
    setGenresInput(book.genres?.join(', ') || '');
    setFormError(null);
    setEditingBook(book);
  };

  const createMutation = useMutation({
    mutationFn: (data: CreateBookRequest) => booksApi.create(data),
    onSuccess: () => {
      setIsAddOpen(false);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['admin-books'] });
    },
    onError: (err: { message?: string; details?: string[] }) => {
      const details = err.details?.join(', ') || '';
      setFormError(details || err.message || 'Failed to create volume.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: number; req: CreateBookRequest }) =>
      booksApi.update(data.id, data.req),
    onSuccess: () => {
      setEditingBook(null);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['admin-books'] });
    },
    onError: (err: { message?: string; details?: string[] }) => {
      const details = err.details?.join(', ') || '';
      setFormError(details || err.message || 'Failed to update volume.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => booksApi.delete(id),
    onSuccess: () => {
      setDeletingId(null);
      queryClient.invalidateQueries({ queryKey: ['admin-books'] });
    },
  });

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    if (selectedAuthorIds.length === 0) {
      setFormError('Please select at least one author.');
      return;
    }

    const genres = genresInput
      .split(',')
      .map((g) => g.trim())
      .filter(Boolean);

    const payload: CreateBookRequest = {
      title,
      isbn,
      publishedDate,
      description: description || undefined,
      authorIds: selectedAuthorIds,
      genres,
    };

    if (editingBook) {
      updateMutation.mutate({ id: editingBook.id, req: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const books = booksData?.content || [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-rule">
        <div>
          <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">
            Books
          </h1>
          <p className="text-sm text-muted mt-1">
            Titles in the catalog. Physical copies are managed under Copies.
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          icon={<Plus className="w-4 h-4" />}
          onClick={openCreateModal}
        >
          Add book
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="p-4 rounded-lg bg-surface border border-rule flex items-center justify-between gap-4">
        <div className="w-full max-w-sm">
          <Input
            placeholder="Filter books by title..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(0);
            }}
            leftIcon={<MagnifyingGlass className="w-4 h-4" />}
          />
        </div>

        <span className="text-sm text-muted shrink-0">
          {booksData?.totalElements || 0} total
        </span>
      </div>

      {/* Table */}
      <div className="rounded-md bg-surface border border-rule overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-muted">
            Loading…
          </div>
        ) : books.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-rule text-xs font-medium text-muted">
                  <th className="py-3.5 px-5">Title</th>
                  <th className="py-3.5 px-5">Authors</th>
                  <th className="py-3.5 px-5">Genres</th>
                  <th className="py-3.5 px-5">Inventory</th>
                  <th className="py-3.5 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rule text-sm">
                {books.map((b) => (
                  <tr key={b.id} className="hover:bg-sunken/50 transition-colors">
                    <td className="py-4 px-5">
                      <span className="font-semibold text-ink block text-sm">
                        {b.title}
                      </span>
                      <span className="text-xs text-muted">ISBN {b.isbn}</span>
                    </td>
                    <td className="py-4 px-5 text-muted">
                      {b.authors?.map((a) => a.name).join(', ') || 'None'}
                    </td>
                    <td className="py-4 px-5 text-muted">
                      {b.genres?.join(', ') || 'Not set'}
                    </td>
                    <td className="py-4 px-5">
                      <span className="text-success font-bold">{b.availableCopies}</span>
                      <span className="text-faint"> / {b.totalCopies} copies</span>
                    </td>
                    <td className="py-4 px-5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <Link href={`/admin/copies?bookId=${b.id}`}>
                          <button
                            title="Manage Physical Copies"
                            className="p-1.5 rounded-lg text-muted hover:text-accent hover:bg-sunken transition-colors cursor-pointer"
                          >
                            <Barcode className="w-4 h-4" />
                          </button>
                        </Link>
                        <button
                          title="Edit Book"
                          onClick={() => openEditModal(b)}
                          className="p-1.5 rounded-lg text-muted hover:text-ink hover:bg-sunken transition-colors cursor-pointer"
                        >
                          <PencilSimple className="w-4 h-4" />
                        </button>
                        <button
                          title="Delete Book"
                          onClick={() => setDeletingId(b.id)}
                          className="p-1.5 rounded-lg text-muted hover:text-danger hover:bg-sunken transition-colors cursor-pointer"
                        >
                          <Trash className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12 text-center text-sm text-muted">
            No catalog books registered. Click &quot;Add Catalog Title&quot; to create your first entry.
          </div>
        )}
      </div>

      <Pagination page={page} totalPages={booksData?.totalPages} totalElements={booksData?.totalElements} onChange={setPage} />

      {/* Add / Edit Modal */}
      <Modal
        isOpen={isAddOpen || Boolean(editingBook)}
        onClose={() => {
          setIsAddOpen(false);
          setEditingBook(null);
          resetForm();
        }}
        title={editingBook ? 'Edit book' : 'Add a book'}
      >
        <form onSubmit={handleSave} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-md bg-danger/10 text-sm text-danger flex items-center gap-2">
              <WarningCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <Input
            label="Title"
            placeholder="e.g. Master and Margarita"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="ISBN"
              placeholder="9780140449136"
              value={isbn}
              onChange={(e) => setIsbn(e.target.value)}
              required
            />

            <Input
              label="Published"
              type="date"
              value={publishedDate}
              onChange={(e) => setPublishedDate(e.target.value)}
              required
            />
          </div>

          <div>
            <label className="text-sm font-medium text-ink block mb-1.5">
              Select Author(s)
            </label>
            <select
              multiple
              value={selectedAuthorIds.map(String)}
              onChange={(e) => {
                const vals = Array.from(e.target.selectedOptions, (option) =>
                  Number(option.value)
                );
                setSelectedAuthorIds(vals);
              }}
              className="w-full rounded-md bg-surface border border-rule-strong text-sm text-ink p-2.5 outline-none focus:border-accent focus:ring-2 focus:ring-accent/20 h-28"
            >
              {authorsData?.content.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.name}
                </option>
              ))}
            </select>
            <span className="text-xs text-faint mt-1 block">
              Hold Ctrl/Cmd to select multiple authors
            </span>
          </div>

          <Input
            label="Subjects, separated by commas"
            placeholder="Fiction, Classic, Russian Literature"
            value={genresInput}
            onChange={(e) => setGenresInput(e.target.value)}
          />

          <div>
            <label className="text-sm font-medium text-ink block mb-1.5">
              Description
            </label>
            <textarea
              rows={3}
              placeholder="Archival summary or plot overview..."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full rounded-md bg-surface border border-rule-strong text-sm text-ink p-3 outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-rule">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => {
                setIsAddOpen(false);
                setEditingBook(null);
              }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={createMutation.isPending || updateMutation.isPending}
            >
              {editingBook ? 'Save Changes' : 'Create Book'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={Boolean(deletingId)}
        onClose={() => setDeletingId(null)}
        title="Delete this book?"
        maxWidth="sm"
      >
        <p className="text-sm text-muted leading-relaxed mb-6">
          Are you sure you want to permanently delete this catalog title? Any physical copies and past loans linked to this volume may be affected.
        </p>
        <div className="flex items-center justify-end gap-3">
          <Button variant="ghost" size="sm" onClick={() => setDeletingId(null)}>
            Cancel
          </Button>
          <Button
            variant="danger"
            size="sm"
            isLoading={deleteMutation.isPending}
            onClick={() => deletingId && deleteMutation.mutate(deletingId)}
          >
            Delete
          </Button>
        </div>
      </Modal>
    </div>
  );
}
