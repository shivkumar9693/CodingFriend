/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: ['class', '[data-theme="dark"]'],
  corePlugins: {
    // Preserve custom CSS resets and prevent resetting Monaco editor or form styles
    preflight: false,
  },
  theme: {
    extend: {
      colors: {
        brand: {
          red: '#ef4444',
          'red-dark': '#dc2626',
          yellow: '#eab308',
          'yellow-light': '#fef08a',
          teal: '#00b8a3',
          amber: '#f59e0b',
        },
        surface: {
          primary: 'var(--bg-primary)',
          secondary: 'var(--bg-secondary)',
          card: 'var(--bg-card)',
          elevated: 'var(--bg-elevated)',
        },
      },
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'Inter', 'sans-serif'],
        mono: ['JetBrains Mono', 'Fira Code', 'monospace'],
      },
      borderRadius: {
        sm: 'var(--radius-sm, 6px)',
        md: 'var(--radius-md, 10px)',
        lg: 'var(--radius-lg, 16px)',
        xl: 'var(--radius-xl, 22px)',
      },
    },
  },
  plugins: [],
};
