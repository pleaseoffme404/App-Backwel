import React from 'react';
import { Skeleton } from '../../../shared/ui/Skeleton';

export function ProductCardSkeleton() {
  return (
    <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl p-4 flex flex-col gap-4 h-[240px]">
      <Skeleton className="w-full h-32 rounded-lg" />
      <div className="flex flex-col gap-2">
        <Skeleton className="w-3/4 h-5" />
        <div className="flex justify-between items-center mt-2">
          <Skeleton className="w-1/3 h-6" />
          <Skeleton className="w-1/4 h-4" />
        </div>
      </div>
    </div>
  );
}