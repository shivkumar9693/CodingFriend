import React from 'react';
import { ProblemDifficulty } from '../types/problem';

interface DifficultyBadgeProps {
  difficulty: ProblemDifficulty;
}

export const DifficultyBadge: React.FC<DifficultyBadgeProps> = ({ difficulty }) => {
  const diffClass = difficulty?.toLowerCase() || 'easy';
  const label = difficulty ? difficulty.charAt(0) + difficulty.slice(1).toLowerCase() : 'Easy';
  return <span className={`difficulty-badge badge-${diffClass}`}>{label}</span>;
};
