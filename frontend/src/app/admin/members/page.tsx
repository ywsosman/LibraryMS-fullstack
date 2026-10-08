'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { membersApi } from '@/lib/api';
import { MemberResponse, CreateMemberRequest } from '@/lib/types';
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

export default function AdminMembersPage() {
  const queryClient = useQueryClient();
  const [searchName, setSearchName] = useState('');
  const [searchEmail, setSearchEmail] = useState('');
  const [page, setPage] = useState(0);

  // Modal states
  const [isAddOpen, setIsAddOpen] = useState(false);
  const [editingMember, setEditingMember] = useState<MemberResponse | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  // Form states
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [phone, setPhone] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const { data: membersData, isLoading } = useQuery({
    queryKey: ['admin-members', searchName, searchEmail, page],
    queryFn: () =>
      membersApi.list({
        name: searchName || undefined,
        email: searchEmail || undefined,
        page,
        size: 10,
      }),
  });

  const resetForm = () => {
    setFullName('');
    setEmail('');
    setPhone('');
    setFormError(null);
  };

  const openEditModal = (m: MemberResponse) => {
    setFullName(m.fullName);
    setEmail(m.email);
    setPhone(m.phone || '');
    setFormError(null);
    setEditingMember(m);
  };

  const createMutation = useMutation({
    mutationFn: (data: CreateMemberRequest) => membersApi.create(data),
    onSuccess: () => {
      setIsAddOpen(false);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['admin-members'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to create member record.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: number; req: CreateMemberRequest }) =>
      membersApi.update(data.id, data.req),
    onSuccess: () => {
      setEditingMember(null);
      resetForm();
      queryClient.invalidateQueries({ queryKey: ['admin-members'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to update member record.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => membersApi.delete(id),
    onSuccess: () => {
      setDeletingId(null);
      queryClient.invalidateQueries({ queryKey: ['admin-members'] });
    },
    onError: (err: { message?: string }) => {
      alert(err.message || 'Cannot delete member with active open loans.');
    },
  });

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    const payload: CreateMemberRequest = {
      fullName,
      email,
      phone: phone || undefined,
    };

    if (editingMember) {
      updateMutation.mutate({ id: editingMember.id, req: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  const members = membersData?.content || [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-rule">
        <div>
          <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">
            Members
          </h1>
          <p className="text-sm text-muted mt-1">
            Library card holders and their contact details.
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
          Add member
        </Button>
      </div>

      {/* Filter Toolbar */}
      <div className="p-4 rounded-lg bg-surface border border-rule flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex flex-wrap items-center gap-4 w-full sm:w-auto">
          <div className="w-60">
            <Input
              placeholder="Search by name..."
              value={searchName}
              onChange={(e) => {
                setSearchName(e.target.value);
                setPage(0);
              }}
              leftIcon={<MagnifyingGlass className="w-4 h-4" />}
            />
          </div>

          <div className="w-60">
            <Input
              placeholder="Search by email..."
              value={searchEmail}
              onChange={(e) => {
                setSearchEmail(e.target.value);
                setPage(0);
              }}
            />
          </div>
        </div>

        <span className="text-sm text-muted shrink-0">
          {membersData?.totalElements || 0} total
        </span>
      </div>

      {/* Table */}
      <div className="rounded-md bg-surface border border-rule overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-muted">
            Loading…
          </div>
        ) : members.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-rule text-xs font-medium text-muted">
                  <th className="py-3.5 px-5">Member</th>
                  <th className="py-3.5 px-5">Email</th>
                  <th className="py-3.5 px-5">Phone</th>
                  <th className="py-3.5 px-5">Registered</th>
                  <th className="py-3.5 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rule text-sm">
                {members.map((m) => (
                  <tr key={m.id} className="hover:bg-sunken/50 transition-colors">
                    <td className="py-4 px-5">
                      <span className="font-semibold text-ink block text-sm">
                        {m.fullName}
                      </span>
                      <span className="text-xs text-muted font-mono">
                        Card {String(m.id).padStart(6, '0')}
                      </span>
                    </td>
                    <td className="py-4 px-5 text-muted">{m.email}</td>
                    <td className="py-4 px-5 text-muted">{m.phone || 'Not set'}</td>
                    <td className="py-4 px-5 text-muted">
                      {new Date(m.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-4 px-5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          title="Edit Member"
                          onClick={() => openEditModal(m)}
                          className="p-1.5 rounded-lg text-muted hover:text-ink hover:bg-sunken transition-colors cursor-pointer"
                        >
                          <PencilSimple className="w-4 h-4" />
                        </button>
                        <button
                          title="Delete Member"
                          onClick={() => setDeletingId(m.id)}
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
            No patron records found. Click &quot;Add Patron Record&quot; to issue a new membership.
          </div>
        )}
      </div>

      <Pagination page={page} totalPages={membersData?.totalPages} totalElements={membersData?.totalElements} onChange={setPage} />

      {/* Add / Edit Modal */}
      <Modal
        isOpen={isAddOpen || Boolean(editingMember)}
        onClose={() => {
          setIsAddOpen(false);
          setEditingMember(null);
          resetForm();
        }}
        title={editingMember ? 'Edit member' : 'Add a member'}
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
            placeholder="e.g. John Doe"
            value={fullName}
            onChange={(e) => setFullName(e.target.value)}
            required
          />

          <Input
            label="Email"
            type="email"
            placeholder="john@example.com"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required
          />

          <Input
            label="Phone"
            placeholder="+1 555-019-2834"
            value={phone}
            onChange={(e) => setPhone(e.target.value)}
          />

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-rule">
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => {
                setIsAddOpen(false);
                setEditingMember(null);
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
              {editingMember ? 'Save Changes' : 'Issue Membership'}
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={Boolean(deletingId)}
        onClose={() => setDeletingId(null)}
        title="Delete this record?"
        maxWidth="sm"
      >
        <p className="text-sm text-muted leading-relaxed mb-6">
          Are you sure you want to soft-delete this patron record? The system ensures no active open loans exist before allowing deletion.
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
