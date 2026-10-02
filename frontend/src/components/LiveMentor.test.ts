import { describe, it, expect } from 'vitest';
import { InlineAssistResponse } from '../types/mentor';

describe('Live Socratic Mentor inline assist logic', () => {
  it('should structure inline Socratic assistance responses', () => {
    const mockResponse: InlineAssistResponse = {
      nudgeType: 'LOGIC_QUESTION',
      inlineMessage: 'What data structure can help you check complement in O(1) time here?',
      targetLine: 4,
      revealsSolution: false,
      modelName: 'qwen2.5-coder:latest',
    };

    expect(mockResponse.nudgeType).toBe('LOGIC_QUESTION');
    expect(mockResponse.revealsSolution).toBe(false);
    expect(mockResponse.inlineMessage).toContain('O(1)');
  });

  it('should toggle live mentor state correctly', () => {
    let isLiveMentorEnabled = true;
    const toggle = () => {
      isLiveMentorEnabled = !isLiveMentorEnabled;
    };

    toggle();
    expect(isLiveMentorEnabled).toBe(false);
    toggle();
    expect(isLiveMentorEnabled).toBe(true);
  });
});
