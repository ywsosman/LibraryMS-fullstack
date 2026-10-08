import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  BookResponse,
  CreateBookRequest,
  UpdateBookRequest,
  AuthorResponse,
  CreateAuthorRequest,
  UpdateAuthorRequest,
  CopyResponse,
  CreateCopyRequest,
  UpdateCopyStatusRequest,
  MemberResponse,
  CreateMemberRequest,
  ActivateMembershipRequest,
  UpdateMemberRequest,
  LoanResponse,
  CreateLoanRequest,
  LoanStatusFilter,
  UserResponse,
  UpdateUserRequest,
  ChangePasswordRequest,
  AuditLogResponse,
  Page,
  ApiErrorResponse,
} from './types';

const API_BASE = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080';

export class ApiError extends Error {
  constructor(
    public status: number,
    public code: string,
    message: string,
    public details: string[] = [],
    public path?: string
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

// Token storage helpers
let memoryToken: string | null = null;

export const setAccessToken = (token: string | null) => {
  memoryToken = token;
};

export const getAccessToken = (): string | null => {
  return memoryToken;
};

export const getRefreshToken = (): string | null => {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('libraryms_refresh_token');
};

export const setRefreshToken = (token: string | null) => {
  if (typeof window === 'undefined') return;
  if (token) {
    localStorage.setItem('libraryms_refresh_token', token);
  } else {
    localStorage.removeItem('libraryms_refresh_token');
  }
};

let refreshPromise: Promise<string | null> | null = null;

async function refreshAccessToken(): Promise<string | null> {
  const currentRefresh = getRefreshToken();
  if (!currentRefresh) return null;

  if (refreshPromise) return refreshPromise;

  refreshPromise = (async () => {
    try {
      const res = await fetch(`${API_BASE}/api/v1/auth/refresh`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: currentRefresh }),
      });

      if (!res.ok) {
        setAccessToken(null);
        setRefreshToken(null);
        return null;
      }

      const data: AuthResponse = await res.json();
      setAccessToken(data.accessToken);
      setRefreshToken(data.refreshToken);
      return data.accessToken;
    } catch {
      setAccessToken(null);
      setRefreshToken(null);
      return null;
    } finally {
      refreshPromise = null;
    }
  })();

  return refreshPromise;
}

interface RequestOptions extends RequestInit {
  requiresAuth?: boolean;
}

export async function apiFetch<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { requiresAuth = false, headers: customHeaders, ...rest } = options;
  const headers = new Headers(customHeaders || {});

  if (!headers.has('Content-Type') && !(rest.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }

  const token = getAccessToken();
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  } else if (requiresAuth) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      headers.set('Authorization', `Bearer ${refreshed}`);
    }
  }

  let response = await fetch(`${API_BASE}${endpoint}`, {
    ...rest,
    headers,
  });

  // Handle 401 token expiration and retry once
  if (response.status === 401 && getRefreshToken()) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      headers.set('Authorization', `Bearer ${refreshed}`);
      response = await fetch(`${API_BASE}${endpoint}`, {
        ...rest,
        headers,
      });
    }
  }

  if (!response.ok) {
    let errorData: ApiErrorResponse | null = null;
    try {
      errorData = await response.json();
    } catch {
      // not JSON
    }

    if (errorData) {
      throw new ApiError(
        response.status,
        errorData.code || 'UNKNOWN_ERROR',
        errorData.message || 'Request failed',
        errorData.details || [],
        errorData.path
      );
    }

    throw new ApiError(
      response.status,
      'HTTP_ERROR',
      `HTTP error ${response.status}: ${response.statusText}`
    );
  }

  if (response.status === 204) {
    return {} as T;
  }

  return response.json();
}

// -------------------------------------------------------------
// Domain API Services
// -------------------------------------------------------------

export const authApi = {
  login: (data: LoginRequest) =>
    apiFetch<AuthResponse>('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  register: (data: RegisterRequest) =>
    apiFetch<AuthResponse>('/api/v1/auth/register', {
      method: 'POST',
      body: JSON.stringify(data),
    }),
  logout: (refreshToken: string) =>
    apiFetch<void>('/api/v1/auth/logout', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),
  refresh: refreshAccessToken,
};

export const booksApi = {
  list: (params?: { title?: string; author?: string; isbn?: string; genre?: string; page?: number; size?: number; sort?: string }) => {
    const q = new URLSearchParams();
    if (params?.title) q.set('title', params.title);
    if (params?.author) q.set('author', params.author);
    if (params?.isbn) q.set('isbn', params.isbn);
    if (params?.genre) q.set('genre', params.genre);
    if (params?.page !== undefined) q.set('page', params.page.toString());
    if (params?.size !== undefined) q.set('size', params.size.toString());
    if (params?.sort) q.set('sort', params.sort);
    return apiFetch<Page<BookResponse>>(`/api/v1/books?${q.toString()}`);
  },
  get: (id: number) => apiFetch<BookResponse>(`/api/v1/books/${id}`),
  getCopies: (id: number, page = 0, size = 20) =>
    apiFetch<Page<CopyResponse>>(`/api/v1/books/${id}/copies?page=${page}&size=${size}`),
  create: (data: CreateBookRequest) =>
    apiFetch<BookResponse>('/api/v1/books', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  update: (id: number, data: UpdateBookRequest) =>
    apiFetch<BookResponse>(`/api/v1/books/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  delete: (id: number) =>
    apiFetch<void>(`/api/v1/books/${id}`, {
      method: 'DELETE',
      requiresAuth: true,
    }),
};

export const authorsApi = {
  list: (params?: { name?: string; page?: number; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.name) q.set('name', params.name);
    if (params?.page !== undefined) q.set('page', params.page.toString());
    if (params?.size !== undefined) q.set('size', params.size.toString());
    return apiFetch<Page<AuthorResponse>>(`/api/v1/authors?${q.toString()}`);
  },
  get: (id: number) => apiFetch<AuthorResponse>(`/api/v1/authors/${id}`),
  create: (data: CreateAuthorRequest) =>
    apiFetch<AuthorResponse>('/api/v1/authors', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  update: (id: number, data: UpdateAuthorRequest) =>
    apiFetch<AuthorResponse>(`/api/v1/authors/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  delete: (id: number) =>
    apiFetch<void>(`/api/v1/authors/${id}`, {
      method: 'DELETE',
      requiresAuth: true,
    }),
};

export const copiesApi = {
  list: (params?: { bookId?: number; barcode?: string; status?: string; page?: number; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.bookId) q.set('bookId', params.bookId.toString());
    if (params?.barcode) q.set('barcode', params.barcode);
    if (params?.status) q.set('status', params.status);
    if (params?.page !== undefined) q.set('page', params.page.toString());
    if (params?.size !== undefined) q.set('size', params.size.toString());
    return apiFetch<Page<CopyResponse>>(`/api/v1/copies?${q.toString()}`);
  },
  get: (id: number) => apiFetch<CopyResponse>(`/api/v1/copies/${id}`),
  create: (data: CreateCopyRequest) =>
    apiFetch<CopyResponse>('/api/v1/copies', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  updateStatus: (id: number, data: UpdateCopyStatusRequest) =>
    apiFetch<CopyResponse>(`/api/v1/copies/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  delete: (id: number) =>
    apiFetch<void>(`/api/v1/copies/${id}`, {
      method: 'DELETE',
      requiresAuth: true,
    }),
};

export const membersApi = {
  list: (params?: { name?: string; email?: string; page?: number; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.name) q.set('name', params.name);
    if (params?.email) q.set('email', params.email);
    if (params?.page !== undefined) q.set('page', params.page.toString());
    if (params?.size !== undefined) q.set('size', params.size.toString());
    return apiFetch<Page<MemberResponse>>(`/api/v1/members?${q.toString()}`, { requiresAuth: true });
  },
  getMy: () => apiFetch<MemberResponse>('/api/v1/members/me', { requiresAuth: true }),
  /** Creates a library card for the signed-in account (email comes from the account). */
  activateMine: (data: ActivateMembershipRequest) =>
    apiFetch<MemberResponse>('/api/v1/members/me', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  get: (id: number) => apiFetch<MemberResponse>(`/api/v1/members/${id}`, { requiresAuth: true }),
  create: (data: CreateMemberRequest) =>
    apiFetch<MemberResponse>('/api/v1/members', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  update: (id: number, data: UpdateMemberRequest) =>
    apiFetch<MemberResponse>(`/api/v1/members/${id}`, {
      method: 'PUT',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  delete: (id: number) =>
    apiFetch<void>(`/api/v1/members/${id}`, {
      method: 'DELETE',
      requiresAuth: true,
    }),
};

export const loansApi = {
  list: (params?: { memberId?: number; copyId?: number; status?: LoanStatusFilter; page?: number; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.memberId) q.set('memberId', params.memberId.toString());
    if (params?.copyId) q.set('copyId', params.copyId.toString());
    if (params?.status && params.status !== 'ALL') q.set('status', params.status);
    if (params?.page !== undefined) q.set('page', params.page.toString());
    if (params?.size !== undefined) q.set('size', params.size.toString());
    return apiFetch<Page<LoanResponse>>(`/api/v1/loans?${q.toString()}`, { requiresAuth: true });
  },
  listMy: (page = 0, size = 20) =>
    apiFetch<Page<LoanResponse>>(`/api/v1/loans/me?page=${page}&size=${size}`, { requiresAuth: true }),
  get: (id: number) => apiFetch<LoanResponse>(`/api/v1/loans/${id}`, { requiresAuth: true }),
  create: (data: CreateLoanRequest) =>
    apiFetch<LoanResponse>('/api/v1/loans', {
      method: 'POST',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  returnLoan: (id: number) =>
    apiFetch<LoanResponse>(`/api/v1/loans/${id}/return`, {
      method: 'POST',
      requiresAuth: true,
    }),
};

export const usersApi = {
  getMe: () => apiFetch<UserResponse>('/api/v1/users/me', { requiresAuth: true }),
  updateMe: (data: UpdateUserRequest) =>
    apiFetch<UserResponse>('/api/v1/users/me', {
      method: 'PATCH',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  changePassword: (data: ChangePasswordRequest) =>
    apiFetch<void>('/api/v1/users/me/password', {
      method: 'PUT',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  deleteMe: () =>
    apiFetch<void>('/api/v1/users/me', {
      method: 'DELETE',
      requiresAuth: true,
    }),
  list: (page = 0, size = 20) =>
    apiFetch<Page<UserResponse>>(`/api/v1/users?page=${page}&size=${size}`, { requiresAuth: true }),
  get: (id: number) => apiFetch<UserResponse>(`/api/v1/users/${id}`, { requiresAuth: true }),
  update: (id: number, data: UpdateUserRequest) =>
    apiFetch<UserResponse>(`/api/v1/users/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
      requiresAuth: true,
    }),
  delete: (id: number) =>
    apiFetch<void>(`/api/v1/users/${id}`, {
      method: 'DELETE',
      requiresAuth: true,
    }),
  linkMember: (userId: number, memberId: number) =>
    apiFetch<UserResponse>(`/api/v1/users/${userId}/member/${memberId}`, {
      method: 'PUT',
      requiresAuth: true,
    }),
  unlinkMember: (userId: number) =>
    apiFetch<UserResponse>(`/api/v1/users/${userId}/member`, {
      method: 'DELETE',
      requiresAuth: true,
    }),
};

export const auditLogsApi = {
  list: (params?: {
    entityType?: string;
    entityId?: number;
    username?: string;
    operation?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }) => {
    const q = new URLSearchParams();
    if (params?.entityType) q.set('entityType', params.entityType);
    if (params?.entityId) q.set('entityId', params.entityId.toString());
    if (params?.username) q.set('username', params.username);
    if (params?.operation) q.set('operation', params.operation);
    if (params?.from) q.set('from', params.from);
    if (params?.to) q.set('to', params.to);
    if (params?.page !== undefined) q.set('page', params.page.toString());
    if (params?.size !== undefined) q.set('size', params.size.toString());
    return apiFetch<Page<AuditLogResponse>>(`/api/v1/audit-logs?${q.toString()}`, { requiresAuth: true });
  },
};
