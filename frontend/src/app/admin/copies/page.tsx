'use client';

import React, { useState, Suspense } from 'react';
import { useSearchParams } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { copiesApi, booksApi } from '@/lib/api';
import { CopyResponse, CopyStatus, CreateCopyRequest } from '@/lib/types';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
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

function CopiesManagementContent() {
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();

  const initialBookId = searchParams.get('bookId') ? Number(searchParams.get('bookId')) : undefined;
  const [filterBookId, setFilterBookId] = useState<number | undefined>(initialBookId);
  const [searchBarcode, setSearchBarcode] = useState('');
  const [page, setPage] = useState(0);

  // Modal states
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingCopy, setEditingCopy] = useState<CopyResponse | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  // Form states
  const [selectedBookId, setSelectedBookId] = useState<number | undefined>(initialBookId);
  const [newBarcode, setNewBarcode] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<CopyStatus>('AVAILABLE');
  const [formError, setFormError] = useState<string | null>(null);

  const { data: copiesData, isLoading } = useQuery({
    queryKey: ['admin-copies', filterBookId, searchBarcode, page],
    queryFn: () =>
      copiesApi.list({
        bookId: filterBookId,
        barcode: searchBarcode || undefined,
        page,
        size: 15,
      }),
  });

  const { data: booksData } = useQuery({
    queryKey: ['admin-books-dropdown'],
    queryFn: () => booksApi.list({ size: 100 }),
  });

  const createMutation = useMutation({
    mutationFn: (data: CreateCopyRequest) => copiesApi.create(data),
    onSuccess: () => {
      setIsAddOpen(false);
      setNewBarcode('');
      setFormError(null);
      queryClient.invalidateQueries({ queryKey: ['admin-copies'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to create physical copy.');
    },
  });

  const statusMutation = useMutation({
    mutationFn: (data: { id: number; status: CopyStatus }) =>
      copiesApi.updateStatus(data.id, { status: data.status }),
    onSuccess: () => {
      setEditingCopy(null);
      setFormError(null);
      queryClient.invalidateQueries({ queryKey: ['admin-copies'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to update copy status.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => copiesApi.delete(id),
    onSuccess: () => {
      setDeletingId(null);
      queryClient.invalidateQueries({ queryKey: ['admin-copies'] });
    },
  });

  const handleCreate = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (!selectedBookId) {
      setFormError('Please select a catalog book.');
      return;
    }
    createMutation.mutate({
      bookId: selectedBookId,
      barcode: newBarcode,
    });
  };

  const handleStatusUpdate = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingCopy) return;
    statusMutation.mutate({
      id: editingCopy.id,
      status: selectedStatus,
    });
  };

  const copies = copiesData?.content || [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-rule">
        <div>
          <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">
            Copies
          </h1>
          <p className="text-sm text-muted mt-1">
            Each barcoded copy on the shelves, and whether it is in, out, or lost.
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => {
            setNewBarcode(`BC-${Math.floor(100000 + Math.random() * 900000)}`);
            setFormError(null);
            setIsAddOpen(true);
          }}
        >
          Add copy
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="p-4 rounded-lg bg-surface border border-rule flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-4 w-full sm:w-auto">
          <div className="w-64">
            <Input
              placeholder="Search barcode..."
              value={searchBarcode}
              onChange={(e) => {
                setSearchBarcode(e.target.value);
                setPage(0);
              }}
              leftIcon={<MagnifyingGlass className="w-4 h-4" />}
            />
          </div>

          <select
            value={filterBookId || ''}
            onChange={(e) => {
              setFilterBookId(e.target.value ? Number(e.target.value) : undefined);
              setPage(0);
            }}
            className="rounded-md bg-surface border border-rule-strong text-sm text-ink py-2.5 px-3.5 outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
          >
            <option value="">All Catalog Titles</option>
            {booksData?.content.map((b) => (
              <option key={b.id} value={b.id}>
                {b.title}
              </option>
            ))}
          </select>
        </div>

        <span className="text-sm text-muted shrink-0">
          {copiesData?.totalElements || 0} total
        </span>
      </div>

      {/* Table */}
      <div className="rounded-md bg-surface border border-rule overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-muted">
            Loading…
          </div>
        ) : copies.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-rule text-xs font-medium text-muted">
                  <th className="py-3.5 px-5">Barcode</th>
                  <th className="py-3.5 px-5">Book Title</th>
                  <th className="py-3.5 px-5">Status</th>
                  <th className="py-3.5 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rule text-sm">
                {copies.map((c) => (
                  <tr key={c.id} className="hover:bg-sunken/50 transition-colors">
                    <td className="py-4 px-5 font-semibold text-ink flex items-center gap-2">
                      <Barcode className="w-4 h-4 text-muted" />
                      <span>{c.barcode}</span>
                    </td>
                    <td className="py-4 px-5 text-muted">
                      {c.book?.title || 'Unknown Title'}
                    </td>
                    <td className="py-4 px-5">
                      <Badge
                        variant={
                          c.status === 'AVAILABLE'
                            ? 'available'
                            : c.status === 'ON_LOAN'
                            ? 'on_loan'
                            : 'lost'
                        }
                        size="sm"
                      >
                        {c.status.replace('_', ' ')}
                      </Badge>
                    </td>
                    <td className="py-4 px-5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          title="Change Status"
                          onClick={() => {
                            setSelectedStatus(c.status);
                            setEditingCopy(c);
                          }}
                          className="p-1.5 rounded-lg text-muted hover:text-accent hover:bg-sunken transition-colors cursor-pointer"
                        >
                          <PencilSimple className="w-4 h-4" />
                        </button>
                        <button
                          title="Delete Copy"
                          onClick={() => setDeletingId(c.id)}
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
            No physical copies found. Click &quot;Add Physical Copy&quot; to print/register barcode items.
          </div>
        )}
      </div>

      <Pagination page={page} totalPages={copiesData?.totalPages} totalElements={copiesData?.totalElements} onChange={setPage} />

      {/* Add Copy Modal */}
      <Modal
        isOpen={isAddOpen}
        onClose={() => setIsAddOpen(false)}
        title="Add a copy"
      >
        <form onSubmit={handleCreate} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-md bg-danger/10 text-sm text-danger flex items-center gap-2">
              <WarningCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div>
            <label className="text-sm font-medium text-ink block mb-1.5">
              Book
            </label>
            <select
              value={selectedBookId || ''}
              onChange={(e) => setSelectedBookId(Number(e.target.value))}
              required
              className="w-full rounded-md bg-surface border border-rule-strong text-sm text-ink p-2.5 outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
            >
              <option value="">Choose a book...</option>
              {booksData?.content.map((b) => (
                <option key={b.id} value={b.id}>
                  {b.title} (ISBN {b.isbn})
                </option>
              ))}
            </select>
          </div>

          <Input
            label="Barcode"
            placeholder="e.g. BC-104928"
            value={newBarcode}
            onChange={(e) => setNewBarcode(e.target.value)}
            required
            helperText="Must be unique across all physical copies"
          />

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-rule">
            <Button type="button" variant="ghost" size="sm" onClick={() => setIsAddOpen(false)}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={createMutation.isPending}
            >
              Add copy
            </Button>
          </div>
        </form>
      </Modal>

      {/* Update Status Modal */}
      <Modal
        isOpen={Boolean(editingCopy)}
        onClose={() => setEditingCopy(null)}
        title="Change status"
        maxWidth="sm"
      >
        <form onSubmit={handleStatusUpdate} className="space-y-4">
          <p className="text-xs text-muted">
            Barcode: <span className="text-ink font-mono">{editingCopy?.barcode}</span>
          </p>

          <div>
            <label className="text-sm font-medium text-ink block mb-1.5">
              Status
            </label>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value as CopyStatus)}
              className="w-full rounded-md bg-surface border border-rule-strong text-sm text-ink p-2.5 outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
            >
              <option value="AVAILABLE">AVAILABLE (On Shelf)</option>
              <option value="ON_LOAN">ON_LOAN (Checked Out)</option>
              <option value="LOST">LOST (Archival Write-Off)</option>
            </select>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-rule">
            <Button type="button" variant="ghost" size="sm" onClick={() => setEditingCopy(null)}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={statusMutation.isPending}
            >
              Save status
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Modal */}
      <Modal
        isOpen={Boolean(deletingId)}
        onClose={() => setDeletingId(null)}
        title="Delete Copy"
        maxWidth="sm"
      >
        <p className="text-xs text-muted mb-6">
          Delete this copy? It will be removed from the shelf list.
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

export default function AdminCopiesPage() {
  return (
    <Suspense
      fallback={
        <div className="text-sm text-muted">Loading copy inventory...</div>
      }
    >
      <CopiesManagementContent />
    </Suspense>
  );
}
