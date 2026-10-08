const DAY = 24 * 60 * 60 * 1000;

/**
 * Whole days a loan is past its due date (0 if not late).
 * The backend only stores a fine when the copy is checked in, so open
 * overdue loans are described by how late they are, not by a fine amount.
 */
export function daysLate(dueDate: string, now = Date.now()): number {
  return Math.max(0, Math.ceil((now - new Date(dueDate).getTime()) / DAY));
}

export const lateLabel = (dueDate: string) => {
  const days = Math.max(1, daysLate(dueDate));
  return `${days} ${days === 1 ? 'day' : 'days'} late`;
};
