'use client';

import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { loansApi, copiesApi, membersApi } from '@/lib/api';
import { LoanStatusFilter, CreateLoanRequest, LoanResponse } from '@/lib/types';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Badge } from '@/components/ui/Badge';
import { Modal } from '@/components/ui/Modal';
import { Pagination } from '@/components/admin/Pagination';
import { lateLabel } from '@/lib/loans';
import {
  ArrowClockwise,
  Plus,
  WarningCircle,
  CheckCircle,
} from '@phosphor-icons/react';

export default function AdminLoansPage() {
  const queryClient = useQueryClient();
  const [selectedStatus, setSelectedStatus] = useState<LoanStatusFilter>('ALL');
  const [filterMemberId, setFilterMemberId] = useState<string>('');
  const [filterCopyId, setFilterCopyId] = useState<string>('');
  const [page, setPage] = useState(0);

  // New loan checkout modal
  const [isCheckoutOpen, setIsCheckoutOpen] = useState(false);
  const [selectedCopyId, setSelectedCopyId] = useState<number | undefined>();
  const [selectedMemberId, setSelectedMemberId] = useState<number | undefined>();
  const [formError, setFormError] = useState<string | null>(null);

  // Return loan confirmation modal
  const [returningLoan, setReturningLoan] = useState<LoanResponse | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  const { data: loansData, isLoading } = useQuery({
    queryKey: ['admin-loans', selectedStatus, filterMemberId, filterCopyId, page],
    queryFn: () =>
      loansApi.list({
        status: selectedStatus,
        memberId: filterMemberId ? Number(filterMemberId) : undefined,
        copyId: filterCopyId ? Number(filterCopyId) : undefined,
        page,
        size: 10,
      }),
  });

  const { data: availableCopies } = useQuery({
    queryKey: ['available-copies-dropdown'],
    queryFn: () => copiesApi.list({ status: 'AVAILABLE', size: 100 }),
    enabled: isCheckoutOpen,
  });

  const { data: membersDropdown } = useQuery({
    queryKey: ['members-dropdown-checkout'],
    queryFn: () => membersApi.list({ size: 100 }),
    enabled: isCheckoutOpen,
  });

  const checkoutMutation = useMutation({
    mutationFn: (data: CreateLoanRequest) => loansApi.create(data),
    onSuccess: () => {
      setIsCheckoutOpen(false);
      setSelectedCopyId(undefined);
      setSelectedMemberId(undefined);
      setFormError(null);
      setSuccessMsg('Loan checked out successfully! 14-day borrowing window started.');
      queryClient.invalidateQueries({ queryKey: ['admin-loans'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats-open-loans'] });
    },
    onError: (err: { message?: string }) => {
      setFormError(err.message || 'Failed to issue loan.');
    },
  });

  const returnMutation = useMutation({
    mutationFn: (id: number) => loansApi.returnLoan(id),
    onSuccess: (returnedLoan) => {
      setReturningLoan(null);
      setSuccessMsg(
        returnedLoan.fineAmount > 0
          ? `Checked in. It was late, so a fine of ${returnedLoan.fineAmount.toFixed(2)} was added.`
          : `Checked in. ${returnedLoan.book.title} is back on the shelf.`
      );
      queryClient.invalidateQueries({ queryKey: ['admin-loans'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats-open-loans'] });
      queryClient.invalidateQueries({ queryKey: ['admin-stats-overdue-loans'] });
    },
    onError: (err: { message?: string }) => {
      alert(err.message || 'Failed to process return.');
    },
  });

  const handleCheckout = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (!selectedCopyId) {
      setFormError('Please select a physical book copy.');
      return;
    }
    if (!selectedMemberId) {
      setFormError('Please select a library patron.');
      return;
    }
    checkoutMutation.mutate({
      copyId: selectedCopyId,
      memberId: selectedMemberId,
    });
  };

  const loans = loansData?.content || [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-rule">
        <div>
          <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">
            Loans
          </h1>
          <p className="text-sm text-muted mt-1">
            Check books out, check them back in, and keep an eye on what is late.
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          icon={<Plus className="w-4 h-4" />}
          onClick={() => {
            setFormError(null);
            setIsCheckoutOpen(true);
          }}
        >
          Check out a book
        </Button>
      </div>

      {/* Success banner */}
      {successMsg && (
        <div className="p-4 rounded-md bg-success/10 text-sm text-success flex items-center justify-between">
          <div className="flex items-center gap-2">
            <CheckCircle className="w-4 h-4 shrink-0" />
            <span>{successMsg}</span>
          </div>
          <button
            onClick={() => setSuccessMsg(null)}
            className="text-xs underline hover:opacity-80"
          >
            Dismiss
          </button>
        </div>
      )}

      {/* Filter Toolbar */}
      <div className="p-4 rounded-lg bg-surface border border-rule flex flex-col md:flex-row items-center justify-between gap-4">
        {/* Status Pill Tabs */}
        <div className="flex items-center gap-1.5 p-1 rounded-md bg-sunken border border-rule">
          {(['ALL', 'OPEN', 'RETURNED', 'OVERDUE'] as LoanStatusFilter[]).map((status) => (
            <button
              key={status}
              onClick={() => {
                setSelectedStatus(status);
                setPage(0);
              }}
              className={`px-3 py-1.5 rounded text-sm font-medium transition-colors cursor-pointer ${
                selectedStatus === status
                  ? 'bg-surface text-ink shadow-sm'
                  : 'text-muted hover:text-ink'
              }`}
            >
              {{ ALL: 'All', OPEN: 'On loan', RETURNED: 'Returned', OVERDUE: 'Overdue' }[status]}
            </button>
          ))}
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto">
          <Input
            placeholder="Member ID..."
            value={filterMemberId}
            onChange={(e) => {
              setFilterMemberId(e.target.value);
              setPage(0);
            }}
            className="w-32"
          />
          <Input
            placeholder="Copy ID..."
            value={filterCopyId}
            onChange={(e) => {
              setFilterCopyId(e.target.value);
              setPage(0);
            }}
            className="w-32"
          />

          <span className="text-sm text-muted shrink-0">
            {loansData?.totalElements || 0} total
          </span>
        </div>
      </div>

      {/* Table */}
      <div className="rounded-md bg-surface border border-rule overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-muted">
            Loading…
          </div>
        ) : loans.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-rule text-xs font-medium text-muted">
                  <th className="py-3.5 px-5">Book</th>
                  <th className="py-3.5 px-5">Patron</th>
                  <th className="py-3.5 px-5">Borrowed</th>
                  <th className="py-3.5 px-5">Due</th>
                  <th className="py-3.5 px-5">Status</th>
                  <th className="py-3.5 px-5 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rule text-sm">
                {loans.map((loan) => (
                  <tr key={loan.id} className="hover:bg-sunken/50 transition-colors">
                    <td className="py-4 px-5">
                      <span className="font-semibold text-ink block text-sm">
                        {loan.book.title}
                      </span>
                      <span className="text-xs text-muted">
                        <span className="font-mono">{loan.copy.barcode}</span>
                      </span>
                    </td>
                    <td className="py-4 px-5">
                      <span className="text-ink font-medium block">
                        {loan.member.fullName}
                      </span>
                      <span className="text-xs text-faint font-mono">Card {String(loan.member.id).padStart(6, '0')}</span>
                    </td>
                    <td className="py-4 px-5 text-muted">
                      {new Date(loan.borrowedAt).toLocaleDateString()}
                    </td>
                    <td className="py-4 px-5 text-muted">
                      {new Date(loan.dueDate).toLocaleDateString()}
                    </td>
                    <td className="py-4 px-5">
                      {loan.open ? (
                        loan.overdue ? (
                          <Badge variant="overdue" size="sm">
                            Overdue · {lateLabel(loan.dueDate)}
                          </Badge>
                        ) : (
                          <Badge variant="on_loan" size="sm">
                            On loan
                          </Badge>
                        )
                      ) : (
                        <Badge variant="returned" size="sm">
                          Returned
                        </Badge>
                      )}
                    </td>
                    <td className="py-4 px-5 text-right">
                      {loan.open && (
                        <Button
                          variant="outline"
                          size="sm"
                          icon={<ArrowClockwise className="w-3.5 h-3.5" />}
                          onClick={() => setReturningLoan(loan)}
                        >
                          Check in
                        </Button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12 text-center text-sm text-muted">
            No loans match this filter.
          </div>
        )}
      </div>

      <Pagination page={page} totalPages={loansData?.totalPages} totalElements={loansData?.totalElements} onChange={setPage} />

      {/* Checkout Modal */}
      <Modal
        isOpen={isCheckoutOpen}
        onClose={() => setIsCheckoutOpen(false)}
        title="Check out a book"
      >
        <form onSubmit={handleCheckout} className="space-y-4">
          {formError && (
            <div className="p-3 rounded-md bg-danger/10 text-sm text-danger flex items-center gap-2">
              <WarningCircle className="w-4 h-4 shrink-0" />
              <span>{formError}</span>
            </div>
          )}

          <div>
            <label className="text-sm font-medium text-ink block mb-1.5">
              Available Physical Copy (Barcode)
            </label>
            <select
              value={selectedCopyId || ''}
              onChange={(e) => setSelectedCopyId(Number(e.target.value))}
              required
              className="w-full rounded-md bg-surface border border-rule-strong text-sm text-ink p-2.5 outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
            >
              <option value="">Select available barcode...</option>
              {availableCopies?.content.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.barcode} · {c.book.title}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="text-sm font-medium text-ink block mb-1.5">
              Library Patron (Member)
            </label>
            <select
              value={selectedMemberId || ''}
              onChange={(e) => setSelectedMemberId(Number(e.target.value))}
              required
              className="w-full rounded-md bg-surface border border-rule-strong text-sm text-ink p-2.5 outline-none focus:border-accent focus:ring-2 focus:ring-accent/20"
            >
              <option value="">Select patron...</option>
              {membersDropdown?.content.map((m) => (
                <option key={m.id} value={m.id}>
                  {m.fullName} ({m.email})
                </option>
              ))}
            </select>
          </div>

          <div className="p-3.5 rounded-md bg-sunken border border-rule text-sm text-muted space-y-1">
            <p>Due back in 14 days. Late returns are charged $0.50 a day.</p>
          </div>

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-rule">
            <Button type="button" variant="ghost" size="sm" onClick={() => setIsCheckoutOpen(false)}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              size="sm"
              isLoading={checkoutMutation.isPending}
            >
              Check out
            </Button>
          </div>
        </form>
      </Modal>

      {/* Return Loan Confirmation Modal */}
      <Modal
        isOpen={Boolean(returningLoan)}
        onClose={() => setReturningLoan(null)}
        title="Check in"
        maxWidth="sm"
      >
        <div className="space-y-4">
          <p className="text-sm text-muted leading-relaxed">
            Confirm check-in for <strong className="text-ink">{returningLoan?.book.title}</strong>{' '}
            (<span className="font-mono">{returningLoan?.copy.barcode}</span>)?
          </p>

          {returningLoan && returningLoan.overdue && (
            <div className="p-3 rounded-md bg-danger/10 text-sm text-danger">
              This copy is {lateLabel(returningLoan.dueDate)}. The fine is worked out when you check it in.
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-4 border-t border-rule">
            <Button variant="ghost" size="sm" onClick={() => setReturningLoan(null)}>
              Cancel
            </Button>
            <Button
              variant="primary"
              size="sm"
              isLoading={returnMutation.isPending}
              onClick={() => returningLoan && returnMutation.mutate(returningLoan.id)}
            >
              Check in
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
}
