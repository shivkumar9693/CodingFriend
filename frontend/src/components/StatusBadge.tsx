import React from 'react';
import { SubmissionStatus } from '../types/submission';
import { Clock, Loader2, CheckCircle2, XCircle, Play } from 'lucide-react';

export interface StatusBadgeProps {
  status: SubmissionStatus;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, size = 'md' }) => {
  const getStatusConfig = () => {
    switch (status) {
      case 'QUEUED':
        return {
          label: 'Queued',
          className: 'status-badge-queued',
          icon: Clock,
        };
      case 'RUNNING':
        return {
          label: 'Running',
          className: 'status-badge-running',
          icon: Play,
        };
      case 'EVALUATING':
        return {
          label: 'Evaluating',
          className: 'status-badge-evaluating',
          icon: Loader2,
        };
      case 'COMPLETED':
        return {
          label: 'Completed',
          className: 'status-badge-completed',
          icon: CheckCircle2,
        };
      case 'FAILED':
        return {
          label: 'Failed',
          className: 'status-badge-failed',
          icon: XCircle,
        };
      default:
        return {
          label: status,
          className: 'status-badge-queued',
          icon: Clock,
        };
    }
  };

  const config = getStatusConfig();
  const Icon = config.icon;

  return (
    <span className={`status-badge ${config.className} status-badge-${size}`}>
      <Icon size={size === 'sm' ? 12 : 14} className={status === 'EVALUATING' ? 'spinner-icon' : ''} />
      <span>{config.label}</span>
    </span>
  );
};
