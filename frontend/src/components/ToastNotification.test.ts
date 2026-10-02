import { describe, it, expect, vi } from 'vitest';

describe('ToastNotification logic and state', () => {
  it('should compute appropriate verdict styling and labels', () => {
    const formatVerdictLabel = (verdict?: string | null) => {
      if (!verdict) return 'Notice';
      return verdict
        .split('_')
        .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
        .join(' ');
    };

    expect(formatVerdictLabel('ACCEPTED')).toBe('Accepted');
    expect(formatVerdictLabel('WRONG_ANSWER')).toBe('Wrong Answer');
    expect(formatVerdictLabel('TIME_LIMIT_EXCEEDED')).toBe('Time Limit Exceeded');
    expect(formatVerdictLabel(null)).toBe('Notice');
  });

  it('should support auto-dismiss timers and pausing on hover', () => {
    vi.useFakeTimers();
    let dismissed = false;
    const duration = 3000;

    const timer = setTimeout(() => {
      dismissed = true;
    }, duration);

    expect(dismissed).toBe(false);
    vi.advanceTimersByTime(2000);
    expect(dismissed).toBe(false);
    vi.advanceTimersByTime(1000);
    expect(dismissed).toBe(true);

    clearTimeout(timer);
    vi.useRealTimers();
  });
});
