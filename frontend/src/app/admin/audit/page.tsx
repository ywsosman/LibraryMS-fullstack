'use client';

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { auditLogsApi } from '@/lib/api';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';
import { CaretLeft, CaretRight, Clock } from '@phosphor-icons/react';

export default function AdminAuditPage() {
  const [entityType, setEntityType] = useState('');
  const [operation, setOperation] = useState('');
  const [username, setUsername] = useState('');
  const [page, setPage] = useState(0);

  const { data: auditData, isLoading } = useQuery({
    queryKey: ['admin-audit-logs', entityType, operation, username, page],
    queryFn: () =>
      auditLogsApi.list({
        entityType: entityType || undefined,
        operation: operation || undefined,
        username: username || undefined,
        page,
        size: 15,
      }),
  });

  const logs = auditData?.content || [];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-rule">
        <div>
          <h1 className="font-serif text-4xl sm:text-5xl tracking-tight text-ink">
            Activity log
          </h1>
          <p className="text-sm text-muted mt-1">
            Every change made in the system, newest first.
          </p>
        </div>

      </div>

      {/* Filter Toolbar */}
      <div className="p-4 rounded-lg bg-surface border border-rule flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 w-full md:w-auto">
          <Input
            placeholder="Entity Type (e.g. Book)..."
            value={entityType}
            onChange={(e) => {
              setEntityType(e.target.value);
              setPage(0);
            }}
          />

          <Input
            placeholder="Operation (e.g. CREATE)..."
            value={operation}
            onChange={(e) => {
              setOperation(e.target.value);
              setPage(0);
            }}
          />

          <Input
            placeholder="Username..."
            value={username}
            onChange={(e) => {
              setUsername(e.target.value);
              setPage(0);
            }}
          />
        </div>

        <span className="text-sm text-muted shrink-0">
          {auditData?.totalElements || 0} entries
        </span>
      </div>

      {/* Table */}
      <div className="rounded-md bg-surface border border-rule overflow-hidden">
        {isLoading ? (
          <div className="p-12 text-center text-sm text-muted">
            Loading…
          </div>
        ) : logs.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-rule text-xs font-medium text-muted">
                  <th className="py-3.5 px-5">When</th>
                  <th className="py-3.5 px-5">By</th>
                  <th className="py-3.5 px-5">Operation</th>
                  <th className="py-3.5 px-5">Record</th>
                  <th className="py-3.5 px-5">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-rule text-sm">
                {logs.map((log) => (
                  <tr key={log.id} className="hover:bg-sunken/50 transition-colors">
                    <td className="py-4 px-5 text-muted whitespace-nowrap">
                      <div className="flex items-center gap-1.5">
                        <Clock className="w-3.5 h-3.5 text-faint" />
                        <span>{new Date(log.occurredAt).toLocaleString()}</span>
                      </div>
                    </td>
                    <td className="py-4 px-5 font-semibold text-ink">
                      {log.username || 'System'}
                    </td>
                    <td className="py-4 px-5">
                      <span className="capitalize">{log.operation.toLowerCase().replace(/_/g, ' ')}</span>
                    </td>
                    <td className="py-4 px-5 text-muted">
                      <span className="text-ink capitalize">{log.entityType.toLowerCase()}</span>
                      {log.entityId && (
                        <span className="text-xs text-faint ml-1">#{log.entityId}</span>
                      )}
                    </td>
                    <td className="py-4 px-5 text-muted max-w-sm truncate">
                      {log.details || ''}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-12 text-center text-sm text-muted">
            No activity matches these filters.
          </div>
        )}
      </div>

      {/* Pagination Bar */}
      {auditData && auditData.totalPages > 1 && (
        <div className="flex items-center justify-center gap-3 mt-8 text-sm">
          <Button
            variant="secondary"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            icon={<CaretLeft className="w-4 h-4" />}
          >
            Previous
          </Button>
          <span className="text-muted px-3">
            {page + 1} / {auditData.totalPages}
          </span>
          <Button
            variant="secondary"
            size="sm"
            disabled={page >= auditData.totalPages - 1}
            onClick={() => setPage((p) => p + 1)}
            icon={<CaretRight className="w-4 h-4" />}
          >
            Next
          </Button>
        </div>
      )}
    </div>
  );
}
