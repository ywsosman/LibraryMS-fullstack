'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/lib/auth-context';
import { AuthShell, FormError } from '@/components/site/AuthShell';
import { Input } from '@/components/ui/Input';
import { Button } from '@/components/ui/Button';

export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();

  const [username, setUsername] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [passwordError, setPasswordError] = useState<string | undefined>();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setPasswordError(undefined);

    if (password.length < 10) {
      setPasswordError('Use at least 10 characters.');
      return;
    }

    setIsSubmitting(true);

    try {
      await register({ username, email, password });
      router.push('/me');
    } catch (err: unknown) {
      const e = err as { message?: string; details?: string[] };
      const detailStr = e.details && e.details.length > 0 ? e.details.join(', ') : '';
      setError(detailStr || e.message || 'We couldn’t create your account.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthShell
      title="Get a library card"
      subtitle="Create an account, then activate your card to start borrowing."
    >
      <FormError message={error} />
      <form onSubmit={handleSubmit} className="space-y-5">
        <Input
          label="Username"
          autoComplete="username"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          required
          minLength={3}
          maxLength={50}
          helperText="3 to 50 characters."
        />
        <Input
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          required
        />
        <Input
          label="Password"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          required
          error={passwordError}
          helperText="At least 10 characters."
        />
        <Button type="submit" size="lg" className="w-full" isLoading={isSubmitting}>
          Create account
        </Button>
      </form>
      <p className="mt-8 text-sm text-muted">
        Already have one?{' '}
        <Link href="/login" className="text-accent hover:underline underline-offset-4">
          Sign in
        </Link>
      </p>
    </AuthShell>
  );
}
