import React from 'react';

export interface BrandLogoProps {
  size?: number;
  className?: string;
  showText?: boolean;
  textClassName?: string;
  badgeText?: string;
}

export const BrandLogo: React.FC<BrandLogoProps> = ({
  size = 32,
  className = '',
  showText = false,
  textClassName = '',
  badgeText,
}) => {
  return (
    <div className={`brand-logo-cluster ${className}`} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.65rem' }}>
      <svg
        width={size}
        height={size}
        viewBox="0 0 100 100"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        className="brand-logo-svg"
        style={{ flexShrink: 0, overflow: 'visible' }}
      >
        <defs>
          {/* Background Glow Filter */}
          <filter id="cf-glow" x="-20%" y="-20%" width="140%" height="140%" filterUnits="userSpaceOnUse">
            <feGaussianBlur stdDeviation="6" result="blur" />
            <feComposite in="SourceGraphic" in2="blur" operator="over" />
          </filter>

          {/* Core Spark Glow Filter */}
          <filter id="cf-spark-glow" x="-50%" y="-50%" width="200%" height="200%">
            <feGaussianBlur in="SourceGraphic" stdDeviation="4" result="sparkBlur" />
            <feMerge>
              <feMergeNode in="sparkBlur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>

          {/* Left Wing Gradient (Cyan to Purple) */}
          <linearGradient id="cf-left-wing" x1="10" y1="20" x2="60" y2="85" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#06b6d4" />
            <stop offset="50%" stopColor="#6366f1" />
            <stop offset="100%" stopColor="#a855f7" />
          </linearGradient>

          {/* Right Wing Gradient (Amber to Neon Crimson) */}
          <linearGradient id="cf-right-wing" x1="90" y1="20" x2="40" y2="85" gradientUnits="userSpaceOnUse">
            <stop offset="0%" stopColor="#f59e0b" />
            <stop offset="50%" stopColor="#ef4444" />
            <stop offset="100%" stopColor="#ec4899" />
          </linearGradient>

          {/* Center Intersection Gradient */}
          <linearGradient id="cf-spark-grad" x1="0%" y1="0%" x2="100%" y2="100%">
            <stop offset="0%" stopColor="#ffffff" />
            <stop offset="40%" stopColor="#fde047" />
            <stop offset="100%" stopColor="#38bdf8" />
          </linearGradient>
        </defs>

        {/* Ambient Backlight Glow */}
        <path
          d="M32 26 C18 26, 12 36, 18 50 C12 64, 18 74, 32 74 C44 74, 52 58, 68 58 C82 58, 88 64, 82 50 C88 36, 82 26, 68 26 C52 26, 44 42, 32 42 Z"
          fill="url(#cf-left-wing)"
          opacity="0.25"
          filter="url(#cf-glow)"
        />

        {/* Left Infinity Code Bracket Wing */}
        <path
          d="M 36 22
             C 22 22, 14 32, 18 42
             C 12 45, 8 48, 8 50
             C 8 52, 12 55, 18 58
             C 14 68, 22 78, 36 78
             C 48 78, 54 62, 64 50
             C 54 38, 48 22, 36 22 Z"
          fill="url(#cf-left-wing)"
          stroke="rgba(255, 255, 255, 0.2)"
          strokeWidth="1.5"
        />

        {/* Right Infinity Code Bracket Wing (Interlocking) */}
        <path
          d="M 64 22
             C 78 22, 86 32, 82 42
             C 88 45, 92 48, 92 50
             C 92 52, 88 55, 82 58
             C 86 68, 78 78, 64 78
             C 52 78, 46 62, 36 50
             C 46 38, 52 22, 64 22 Z"
          fill="url(#cf-right-wing)"
          stroke="rgba(255, 255, 255, 0.2)"
          strokeWidth="1.5"
          style={{ mixBlendMode: 'screen' }}
        />

        {/* Inner Bracket Highlights */}
        <path
          d="M 32 30 C 24 30, 20 38, 22 45 C 18 47, 16 49, 16 50 C 16 51, 18 53, 22 55 C 20 62, 24 70, 32 70"
          stroke="#38bdf8"
          strokeWidth="3.5"
          strokeLinecap="round"
          fill="none"
          opacity="0.9"
        />

        <path
          d="M 68 30 C 76 30, 80 38, 78 45 C 82 47, 84 49, 84 50 C 84 51, 82 53, 78 55 C 80 62, 76 70, 68 70"
          stroke="#fde047"
          strokeWidth="3.5"
          strokeLinecap="round"
          fill="none"
          opacity="0.9"
        />

        {/* Central Socratic AI Star Spark (The Mentor Nucleus) */}
        <g filter="url(#cf-spark-glow)">
          {/* Outer Halo */}
          <circle cx="50" cy="50" r="10" fill="#06b6d4" opacity="0.3" />
          <circle cx="50" cy="50" r="6" fill="#fde047" opacity="0.5" />
          
          {/* 8-Point Radiant Star */}
          <path
            d="M 50 34
               Q 50 50, 66 50
               Q 50 50, 50 66
               Q 50 50, 34 50
               Q 50 50, 50 34 Z"
            fill="url(#cf-spark-grad)"
          />
          <path
            d="M 50 39
               Q 50 50, 61 50
               Q 50 50, 50 61
               Q 50 50, 39 50
               Q 50 50, 50 39 Z"
            fill="#ffffff"
          />

          {/* Diagonal Micro Rays */}
          <path
            d="M 42 42 L 58 58 M 58 42 L 42 58"
            stroke="#ffffff"
            strokeWidth="1.5"
            strokeLinecap="round"
            opacity="0.75"
          />
        </g>
      </svg>

      {showText && (
        <div className={`brand-logo-text-group ${textClassName}`} style={{ display: 'flex', flexDirection: 'column' }}>
          <span className="brand-title" style={{ fontSize: '1.1rem', fontWeight: 800, letterSpacing: '-0.02em', lineHeight: 1.15 }}>
            CodingFriend
          </span>
          {badgeText && (
            <span className="brand-badge" style={{ fontSize: '0.6875rem', fontWeight: 700, color: '#eab308', display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
              ✦ {badgeText}
            </span>
          )}
        </div>
      )}
    </div>
  );
};
