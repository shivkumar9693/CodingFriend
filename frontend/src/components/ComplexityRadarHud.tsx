import React, { useState, useRef, useEffect } from 'react';
import {
  Activity,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Sparkles,
  ChevronRight,
  X,
} from 'lucide-react';
import { ComplexityRadarResult } from '../services/complexityRadarService';

export interface ComplexityRadarHudProps {
  analysis: ComplexityRadarResult;
  onAskMentorAdvice?: (message: string) => void;
}

export const ComplexityRadarHud: React.FC<ComplexityRadarHudProps> = ({
  analysis,
  onAskMentorAdvice,
}) => {
  const [isOpen, setIsOpen] = useState<boolean>(false);
  const popoverRef = useRef<HTMLDivElement>(null);

  // Close on outside click
  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      if (popoverRef.current && !popoverRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    if (isOpen) {
      document.addEventListener('mousedown', handleOutsideClick);
    }
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
    };
  }, [isOpen]);

  const getStatusColorClass = () => {
    switch (analysis.status) {
      case 'OPTIMAL':
        return 'radar-chip-green';
      case 'SUB_OPTIMAL':
        return 'radar-chip-yellow';
      case 'DETOUR_ERROR':
        return 'radar-chip-red';
      default:
        return 'radar-chip-green';
    }
  };

  const getStatusIcon = () => {
    switch (analysis.status) {
      case 'OPTIMAL':
        return <CheckCircle2 size={12} className="text-emerald-400" />;
      case 'SUB_OPTIMAL':
        return <AlertTriangle size={12} className="text-amber-400" />;
      case 'DETOUR_ERROR':
        return <XCircle size={12} className="text-rose-400" />;
    }
  };

  return (
    <div className="complexity-radar-wrapper" ref={popoverRef}>
      {/* Sleek Compact Toolbar Pill */}
      <button
        type="button"
        className={`radar-toolbar-chip ${getStatusColorClass()} ${isOpen ? 'active-open' : ''}`}
        onClick={() => setIsOpen(!isOpen)}
        title="Real-Time Complexity & Cognitive Detour Radar (Click to inspect)"
      >
        <span className={`radar-beacon-dot dot-${analysis.status.toLowerCase()}`} />
        <span className="radar-time-pill">{analysis.timeComplexity}</span>
        <span className="radar-chip-status-text">
          {analysis.status === 'OPTIMAL' && 'Optimal'}
          {analysis.status === 'SUB_OPTIMAL' && 'Sub-Optimal'}
          {analysis.status === 'DETOUR_ERROR' && 'Detour'}
        </span>
      </button>

      {/* Floating Socratic Insight Popover */}
      {isOpen && (
        <div className={`radar-popover-card popover-${analysis.status.toLowerCase()}`}>
          <div className="radar-popover-header">
            <div className="radar-popover-title">
              <span className={`radar-beacon-dot dot-${analysis.status.toLowerCase()}`} />
              <Activity size={13} className="text-slate-400" />
              <span>Complexity & Detour Radar</span>
            </div>
            <button
              type="button"
              className="radar-popover-close"
              onClick={() => setIsOpen(false)}
              aria-label="Close"
            >
              <X size={12} />
            </button>
          </div>

          <div className="radar-popover-body">
            <div className="radar-metrics-row">
              <div className="radar-metric-item">
                <span className="metric-label">Time Complexity</span>
                <span className="metric-val">{analysis.timeComplexity}</span>
              </div>
              <div className="radar-metric-item">
                <span className="metric-label">Space Complexity</span>
                <span className="metric-val">{analysis.spaceComplexity}</span>
              </div>
              <div className="radar-metric-item">
                <span className="metric-label">Status</span>
                <span className="metric-val status-badge-inline">
                  {getStatusIcon()}
                  <span>{analysis.headline}</span>
                </span>
              </div>
            </div>

            <p className="radar-popover-desc">"{analysis.detailMessage}"</p>
          </div>

          <div className="radar-popover-footer">
            <div className="radar-footer-hint">
              <HelpCircle size={12} />
              <span>
                {analysis.status === 'OPTIMAL' && 'Clean logic — optimal memory and runtime.'}
                {analysis.status === 'SUB_OPTIMAL' && 'Optimize loop/table invariant to prevent TLE.'}
                {analysis.status === 'DETOUR_ERROR' && 'Unused code or block anomaly flagged.'}
              </span>
            </div>

            {onAskMentorAdvice && analysis.status !== 'OPTIMAL' && (
              <button
                type="button"
                className="btn-radar-ask-popover"
                onClick={() => {
                  setIsOpen(false);
                  onAskMentorAdvice(`Can you give me a conceptual hint about ${analysis.headline.toLowerCase()}?`);
                }}
              >
                <Sparkles size={11} className="text-yellow-400" />
                <span>Ask SocraBuddy</span>
                <ChevronRight size={11} />
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
