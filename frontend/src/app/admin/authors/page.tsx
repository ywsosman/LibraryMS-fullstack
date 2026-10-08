'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { authorsApi } from '@/lib/api';
import { AuthorResponse, CreateAuthorRequest } from '@/lib/types';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import { Pagination } from '@/components/admin/Pagination';
import {
  Plus,
  PencilSimple,
  Trash,
  MagnifyingGlass,
  WarningCircle,
} from '@phosphor-icons/react';

export default function AdminAuthorsPage() {
  const queryClient = useQueryClient();
  const [searchTerm, setSearchTerm] = useState('');
  const [page, setPage] = useState(0);

  // Modal states
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingAuthor, setEditingAuthor] = useState<AuthorResponse | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  // Form states
  const [name, setName] = useState('');
  const [bio, setBio] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const { data: authorsData, isLoading } = useQuery({
    queryKey: ['admin-authors', searchTerm, page],
    queryFn: () => authorsApi.list({ name: searchTerm || undefined, page, size: 10 }),
  });

  const resetForm = () => {
    setName('');
    setBio('');
    setBirthDate('');
    setFormError(null);
  };

  const openEditModal = (author: AuthorResponse) => {
    setName(author.name);
    setBio(author.bio || '');
    setBirthDate(author.birthDate || '');
    setFormError(null);
    setEditingAuthor(author);
  };

  const createMutation = useMutation({
    mutationFn: (data: CreateAuthorRequest) => authorsApi.create(data),
    onSuccess: () => {
      setIsAddOpen(false);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['admin-authors'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to create author.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: number; req: CreateAuthorRequest }) =>
      authorsApi.update(data.id, data.req),
    onSuccess: () => {
      setEditingAuthor(null);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['admin-authors'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to update author.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => authorsApi.delete(id),
    onSuccess: () => {
      setDeletingId(null);
      queryClient.invalidateQueries({ queryKey: ['admin-authors'] });
    },
  });

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const payload: CreateAuthorRequest = {
      name,
      bio: bio || undefined,
      birthDate: birthDate || undefined,
    };

    if (editingAuthor) {
      updateMutation.mutate({ id: editingAuthor.id, req: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const authors = authorsData?.content || [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-rule">
        <div>
          <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">
            Authors
          </h1>
          <p className="text-sm text-muted mt-1">
            The people who wrote the books in the catalog.
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => {
            resetForm();
            setIsAddOpen(true);
          }}
        >
          Add author
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="p-4 rounded-lg bg-surface border border-rule flex items-center justify-between gap-4">
        <div className="w-full max-w-sm">
          <Input
            placeholder="Search author by name..."
            value={searchTerm}
            onChange={(e) => {
              setSearchTerm(e.target.value);
              setPage(0);
            }}
            leftIcon={<MagnifyingGlass className="w-4 h-4" />}
          />
        </div>

        <span className="text-sm text-muted shrink-0">
          {authorsData?.totalElements || 0} total
        </span>
      </div>

      {/* Table */}
      <div className="rounded-md bg-surface border border-rule overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-muted">
            Loading…
          </div>
        ) : authors.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-rule text-xs font-medium text-muted">
                  <th className="py-3.5 px-5">Name</th>
                  <th className="py-3.5 px-5">Birth Date</th>
                  <th className="py-3.5 px-5">Biography</th>
                  <th className="py-3.5 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rule text-sm">
                {authors.map((a) => (
                  <tr key={a.id} className="hover:bg-sunken/50 transition-colors">
                    <td className="py-4 px-5 font-semibold text-ink">{a.name}</td>
                    <td className="py-4 px-5 text-muted">{a.birthDate || 'Not set'}</td>
                    <td className="py-4 px-5 text-muted max-w-xs truncate">
                      {a.bio || 'Not set'}
                    </td>
                    <td className="py-4 px-5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          title="Edit Author"
                          onClick={() => openEditModal(a)}
                          className="p-1.5 rounded-lg text-muted hover:text-ink hover:bg-sunken transition-colors cursor-pointer"
                        >
                          <PencilSimple className="w-4 h-4" />
                        </button>
                        <button
                          title="Delete Author"
                          onClick={() => setDeletingId(a.id)}
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
            No authors found. Click &quot;Add Author&quot; to register a new writer.
          </div>
        )}
      </div>

      <Pagination page={page} totalPages={authorsData?.totalPages} totalElements={authorsData?.totalElements} onChange={setPage} />

      {/* Add / Edit Modal */}
      <Modal
        isOpen={isAddOpen || Boolean(editingAuthor)}
        onClose={() => {
          setIsAddOpen(false);
          setEditingAuthor(null);
          resetForm();
        }}
        title={editingAuthor ? 'Edit author' : 'Add an author'}
      >
        <form onSubmit={handleSave} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-md bg-danger/10 text-sm text-danger flex items-center gap-2">
              <WarningCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <Input
            label="Full name"
            placeholder="e.g. Leo Tolstoy"
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
          />

          <Input
            label="Date of birth"
            type="date"
            value={birthDate}
            onChange={(e) => setBirthDate(e.target.value)}
          />

          <div>
            <label className="text-sm font-medium text-ink block mb-1.5">
              Biography
            </label>
            <textarea
              rows={3}
              placeholder="Biographical highlights, literary style, or historical context..."
              value={bio}
              onChange={(e) => setBio(e.target.value)}
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
                setEditingAuthor(null);
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
              {editingAuthor ? 'Save Changes' : 'Create Author'}
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
          Are you sure you want to delete this author? Any books associated with this author will remain in catalog.
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
