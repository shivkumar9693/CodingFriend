import React, { useMemo } from 'react';
import { ActivityHeatmapData } from '../types/duel';
import { Flame, Trophy, Calendar, Award } from 'lucide-react';

interface ActivityHeatmapProps {
  data: ActivityHeatmapData | null;
  isLoading?: boolean;
}

export const ActivityHeatmap: React.FC<ActivityHeatmapProps> = ({ data, isLoading = false }) => {
  // Generate 52 weeks (364 days) of grid cells ending today
  const { weeks, totalCount } = useMemo(() => {
    const today = new Date();
    const map = new Map<string, number>();

    const formatLocalDate = (date: Date): string => {
      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, '0');
      const day = String(date.getDate()).padStart(2, '0');
      return `${year}-${month}-${day}`;
    };

    if (data?.dailyActivities) {
      for (const act of data.dailyActivities) {
        map.set(String(act.date), act.count);
      }
    }

    const daysList: { dateStr: string; count: number; dayOfWeek: number }[] = [];
    let runningTotal = 0;

    for (let i = 363; i >= 0; i--) {
      const d = new Date(today);
      d.setDate(d.getDate() - i);
      const dateStr = formatLocalDate(d);
      const count = map.get(dateStr) || 0;
      runningTotal += count;
      daysList.push({
        dateStr,
        count,
        dayOfWeek: d.getDay(), // 0 = Sun, 6 = Sat
      });
    }

    // Group into 52 columns of 7 days
    const groupedWeeks: { dateStr: string; count: number }[][] = [];
    let currentWeek: { dateStr: string; count: number }[] = [];

    for (let i = 0; i < daysList.length; i++) {
      currentWeek.push(daysList[i]);
      if (currentWeek.length === 7 || i === daysList.length - 1) {
        groupedWeeks.push(currentWeek);
        currentWeek = [];
      }
    }

    return { weeks: groupedWeeks, totalCount: runningTotal };
  }, [data]);

  const getCellColor = (count: number) => {
    if (count === 0) return 'var(--heatmap-bg-empty, rgba(255, 255, 255, 0.04))';
    if (count <= 2) return 'rgba(34, 197, 94, 0.35)';
    if (count <= 5) return 'rgba(34, 197, 94, 0.65)';
    if (count <= 9) return 'rgba(34, 197, 94, 0.85)';
    return '#22c55e';
  };

  if (isLoading) {
    return (
      <div className="heatmap-container loading">
        <div className="skeleton" style={{ height: '140px', width: '100%', borderRadius: '12px' }} />
      </div>
    );
  }

  return (
    <div className="activity-heatmap-card">
      <div className="heatmap-header">
        <div className="heatmap-title-group">
          <Calendar size={18} className="text-yellow" />
          <h3>Activity Heatmap & Problem Solving Streak</h3>
        </div>
        <div className="heatmap-metrics-summary">
          <span className="summary-pill">
            <strong>{data?.totalSubmissions || totalCount}</strong> submissions in the last year
          </span>
        </div>
      </div>

      {/* Streak Telemetry Row */}
      <div className="heatmap-stats-row">
        <div className="stat-badge">
          <Flame size={16} className="text-orange" />
          <div className="stat-badge-text">
            <span className="stat-badge-val">{data?.currentStreak || 0} Days</span>
            <span className="stat-badge-lbl">Current Streak</span>
          </div>
        </div>

        <div className="stat-badge">
          <Trophy size={16} className="text-yellow" />
          <div className="stat-badge-text">
            <span className="stat-badge-val">{data?.maxStreak || 0} Days</span>
            <span className="stat-badge-lbl">Longest Streak</span>
          </div>
        </div>

        <div className="stat-badge">
          <Award size={16} className="text-green" />
          <div className="stat-badge-text">
            <span className="stat-badge-val">{data?.totalActiveDays || 0} Days</span>
            <span className="stat-badge-lbl">Active Coding Days</span>
          </div>
        </div>
      </div>

      {/* Heatmap Grid */}
      <div className="heatmap-grid-wrapper">
        <div className="heatmap-grid">
          {weeks.map((week, wIdx) => (
            <div key={wIdx} className="heatmap-week-col">
              {week.map((day, dIdx) => (
                <div
                  key={dIdx}
                  className="heatmap-day-cell"
                  style={{ backgroundColor: getCellColor(day.count) }}
                  title={`${day.count} submissions on ${day.dateStr}`}
                />
              ))}
            </div>
          ))}
        </div>

        {/* Heatmap Legend */}
        <div className="heatmap-legend">
          <span className="legend-label">Less</span>
          <div className="legend-cell" style={{ backgroundColor: getCellColor(0) }} />
          <div className="legend-cell" style={{ backgroundColor: getCellColor(2) }} />
          <div className="legend-cell" style={{ backgroundColor: getCellColor(5) }} />
          <div className="legend-cell" style={{ backgroundColor: getCellColor(9) }} />
          <div className="legend-cell" style={{ backgroundColor: getCellColor(12) }} />
          <span className="legend-label">More</span>
        </div>
      </div>
    </div>
  );
};
