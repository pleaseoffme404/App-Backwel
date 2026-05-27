import React from 'react';

interface SkeletonProps extends React.HTMLAttributes<HTMLDivElement> {}

export function Skeleton({ className = '', ...props }: SkeletonProps) {
  return (
    <div
      className={`relative overflow-hidden bg-bg-secondary rounded-md ${className}`}
      {...props}
    >
      <div className="absolute inset-0 -translate-x-full animate-[shimmer_1.5s_infinite] bg-gradient-to-r from-transparent via-text-primary/10 to-transparent" />
    </div>
  );
}