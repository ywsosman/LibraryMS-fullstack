'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { usersApi, membersApi } from '@/lib/api';
import { UserResponse } from '@/lib/types';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Modal } from '@/components/ui/Modal';
import {
  Link as LinkIcon,
  LinkBreak,
  PencilSimple,
  Trash,
  WarningCircle,
} from '@phosphor-icons/react';

export default function AdminUsersPage() {
  const queryClient = useQueryClient();
  const page = 0;

  // Link member modal
  const [linkingUser, setLinkingUser] = useState<UserResponse | null>(null);
  const [selectedMemberId, setSelectedMemberId] = useState<number | undefined>();

  // Edit user modal
  const [editingUser, setEditingUser] = useState<UserResponse | null>(null);
  const [editUsername, setEditUsername] = useState('');
  const [editEmail, setEditEmail] = useState('');

  // Delete modal
  const [deletingId, setDeletingId] = useState<number | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  const { data: usersData, isLoading } = useQuery({
    queryKey: ['admin-users', page],
    queryFn: () => usersApi.list(page, 15),
  });

  const { data: membersData } = useQuery({
    queryKey: ['admin-members-dropdown-users'],
    queryFn: () => membersApi.list({ size: 100 }),
    enabled: Boolean(linkingUser),
  });

  const linkMutation = useMutation({
    mutationFn: (data: { userId: number; memberId: number }) =>
      usersApi.linkMember(data.userId, data.memberId),
    onSuccess: () => {
      setLinkingUser(null);
      setSelectedMemberId(undefined);
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to link patron card.');
    },
  });

  const unlinkMutation = useMutation({
    mutationFn: (userId: number) => usersApi.unlinkMember(userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (err: { message?: string }) => {
      alert(err.message || 'Failed to unlink patron card.');
    },
  });

  const updateMutation = useMutation({
    mutationFn: (data: { id: number; username?: string; email?: string }) =>
      usersApi.update(data.id, { username: data.username, email: data.email }),
    onSuccess: () => {
      setEditingUser(null);
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to update user profile.');
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => usersApi.delete(id),
    onSuccess: () => {
      setDeletingId(null);
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
    },
  });

  const users = usersData?.content || [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-rule">
        <div>
          <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">
            Accounts
          </h1>
          <p className="text-sm text-muted mt-1">
            Sign-in accounts, staff access, and which card each account uses.
          </p>
        </div>

        <span className="text-sm text-muted">
          {usersData?.totalElements || 0} accounts
        </span>
      </div>

      {/* Table */}
      <div className="rounded-md bg-surface border border-rule overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-muted">
            Loading…
          </div>
        ) : users.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-rule text-xs font-medium text-muted">
                  <th className="py-3.5 px-5">Account</th>
                  <th className="py-3.5 px-5">Roles</th>
                  <th className="py-3.5 px-5">Library card</th>
                  <th className="py-3.5 px-5">Registered</th>
                  <th className="py-3.5 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rule text-sm">
                {users.map((u) => (
                  <tr key={u.id} className="hover:bg-sunken/50 transition-colors">
                    <td className="py-4 px-5">
                      <span className="font-semibold text-ink block text-sm">
                        {u.username}
                      </span>
                      <span className="text-xs text-muted">{u.email}</span>
                    </td>
                    <td className="py-4 px-5">
                      <div className="flex items-center gap-1.5 flex-wrap">
                        {u.roles?.map((r) => (
                          <span
                            key={r}
                            className={`px-1.5 py-0.5 rounded-sm text-xs font-medium ${
                              r === 'ROLE_ADMIN'
                                ? 'bg-accent/10 text-accent'
                                : 'bg-sunken text-muted'
                            }`}
                          >
                            {r === 'ROLE_ADMIN' ? 'Staff' : 'Reader'}
                          </span>
                        ))}
                      </div>
                    </td>
                    <td className="py-4 px-5">
                      {u.memberId ? (
                        <div className="flex items-center gap-2">
                          <span className="text-ink font-mono">{String(u.memberId).padStart(6, '0')}</span>
                          <button
                            title="Unlink Member"
                            onClick={() => unlinkMutation.mutate(u.id)}
                            className="p-1 rounded text-muted hover:text-danger transition-colors cursor-pointer"
                          >
                            <LinkBreak className="w-3.5 h-3.5" />
                          </button>
                        </div>
                      ) : (
                        <button
                          onClick={() => {
                            setLinkingUser(u);
                            setSelectedMemberId(undefined);
                            setFormError(null);
                          }}
                          className="inline-flex items-center gap-1 text-xs text-accent hover:underline cursor-pointer"
                        >
                          <LinkIcon className="w-3 h-3" />
                          <span>Link to Patron</span>
                        </button>
                      )}
                    </td>
                    <td className="py-4 px-5 text-muted">
                      {new Date(u.createdAt).toLocaleDateString()}
                    </td>
                    <td className="py-4 px-5 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          title="Edit Profile"
                          onClick={() => {
                            setEditUsername(u.username);
                            setEditEmail(u.email);
                            setEditingUser(u);
                            setFormError(null);
                          }}
                          className="p-1.5 rounded-lg text-muted hover:text-ink hover:bg-sunken transition-colors cursor-pointer"
                        >
                          <PencilSimple className="w-4 h-4" />
                        </button>
                        <button
                          title="Delete User"
                          onClick={() => setDeletingId(u.id)}
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
          <div className="p-12 text-center text-sm text-muted">No user records found.</div>
        )}
      </div>

      {/* Link Member Modal */}
      <Modal
        isOpen={Boolean(linkingUser)}
        onClose={() => setLinkingUser(null)}
        title="Link a library card"
        maxWidth="sm"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (linkingUser && selectedMemberId) {
              linkMutation.mutate({ userId: linkingUser.id, memberId: selectedMemberId });
            }
          }}
          className="space-y-4"
        >
          {formError && (
            <div className="p-3 rounded-md bg-danger/10 text-sm text-danger flex items-center gap-2">
              <WarningCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <p className="text-xs text-muted">
            Select patron record for account: <strong className="text-ink">{linkingUser?.username}</strong>
          </p>

          <div>
            <label className="text-sm font-medium text-ink block mb-1.5">
              Member
            </label>
            <select
              value={selectedMemberId || ''}
              onChange={(e) => setSelectedMemberId(Number(e.target.value))}
              required
              className="w-full rounded-md bg-surface border border-rule-strong text-sm text-ink p-2.5 outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
            >
              <option value="">Select a member...</option>
              {membersData?.content.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName} ({m.email})
                </option>
              ))}
            </select>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-rule">
            <Button variant="ghost" size="sm" onClick={() => setLinkingUser(null)}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={linkMutation.isPending}
            >
              Link card
            </Button>
          </div>
        </form>
      </Modal>

      {/* Edit User Modal */}
      <Modal
        isOpen={Boolean(editingUser)}
        onClose={() => setEditingUser(null)}
        title="Edit account"
        maxWidth="sm"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            if (editingUser) {
              updateMutation.mutate({
                id: editingUser.id,
                username: editUsername || undefined,
                email: editEmail || undefined,
              });
            }
          }}
          className="space-y-4"
        >
          {formError && (
            <div className="p-3 rounded-md bg-danger/10 text-sm text-danger flex items-center gap-2">
              <WarningCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <Input
            label="Username"
            value={editUsername}
            onChange={(e) => setEditUsername(e.target.value)}
            required
          />

          <Input
            label="Email"
            type="email"
            value={editEmail}
            onChange={(e) => setEditEmail(e.target.value)}
            required
          />

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-rule">
            <Button variant="ghost" size="sm" onClick={() => setEditingUser(null)}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={updateMutation.isPending}
            >
              Save
            </Button>
          </div>
        </form>
      </Modal>

      {/* Delete Confirmation Modal */}
      <Modal
        isOpen={Boolean(deletingId)}
        onClose={() => setDeletingId(null)}
        title="Delete this account?"
        maxWidth="sm"
      >
        <p className="text-sm text-muted leading-relaxed mb-6">
          Are you sure you want to delete this user account? The user audit history will be preserved with ON DELETE SET NULL.
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
