import type { Config } from 'tailwindcss';

export default {
  content: [
    "./index.html",
    "./src/client/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        'bg-primary': 'var(--bg-primary)',
        'bg-secondary': 'var(--bg-secondary)',
        'brand-primary': 'var(--brand-primary)',
        'brand-secondary': 'var(--brand-secondary)',
        'accent': 'var(--accent)',
        'text-primary': 'var(--text-primary)',
      }
    },
  },
  plugins: [],
} satisfies Config;