export type RoleName = 'ROLE_USER' | 'ROLE_ADMIN';

export type CopyStatus = 'AVAILABLE' | 'ON_LOAN' | 'LOST';

export type LoanStatusFilter = 'ALL' | 'OPEN' | 'RETURNED' | 'OVERDUE';

export interface AuthorSummary {
  id: number;
  name: string;
}

export interface BookSummary {
  id: number;
  title: string;
  isbn: string;
}

export interface CopySummary {
  id: number;
  barcode: string;
  status: CopyStatus;
}

export interface MemberSummary {
  id: number;
  fullName: string;
}

export interface AuthorResponse {
  id: number;
  name: string;
  bio?: string;
  birthDate?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateAuthorRequest {
  name: string;
  bio?: string;
  birthDate?: string;
}

export interface UpdateAuthorRequest {
  name: string;
  bio?: string;
  birthDate?: string;
}

export interface BookResponse {
  id: number;
  title: string;
  isbn: string;
  publishedDate: string;
  description?: string;
  authors: AuthorSummary[];
  genres: string[];
  totalCopies: number;
  availableCopies: number;
  createdAt: string;
  updatedAt: string;
}

export interface CreateBookRequest {
  title: string;
  isbn: string;
  publishedDate: string;
  description?: string;
  authorIds: number[];
  genres: string[];
}

export interface UpdateBookRequest {
  title: string;
  isbn: string;
  publishedDate: string;
  description?: string;
  authorIds: number[];
  genres: string[];
}

export interface CopyResponse {
  id: number;
  book: BookSummary;
  barcode: string;
  status: CopyStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CreateCopyRequest {
  bookId: number;
  barcode: string;
}

export interface UpdateCopyStatusRequest {
  status: CopyStatus;
}

export interface MemberResponse {
  id: number;
  fullName: string;
  email: string;
  phone?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateMemberRequest {
  fullName: string;
  email: string;
  phone?: string;
}

export interface ActivateMembershipRequest {
  fullName: string;
  phone?: string;
}

export interface UpdateMemberRequest {
  fullName?: string;
  email?: string;
  phone?: string;
}

export interface LoanResponse {
  id: number;
  copy: CopySummary;
  book: BookSummary;
  member: MemberSummary;
  borrowedAt: string;
  dueDate: string;
  returnedAt?: string | null;
  fineAmount: number;
  open: boolean;
  overdue: boolean;
}

export interface CreateLoanRequest {
  copyId: number;
  memberId?: number;
}

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  enabled: boolean;
  memberId?: number | null;
  roles: string[];
  createdAt: string;
  updatedAt: string;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface AuditLogResponse {
  id: number;
  userId?: number | null;
  username?: string | null;
  entityType: string;
  entityId?: number | null;
  operation: string;
  occurredAt: string;
  details?: string | null;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ApiErrorResponse {
  code: string;
  message: string;
  details: string[];
  timestamp: string;
  path: string;
}

export interface Page<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
