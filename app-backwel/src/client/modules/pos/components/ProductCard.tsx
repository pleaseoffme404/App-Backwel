import React from 'react';
import { Product } from '../types';

interface ProductCardProps {
  product: Product;
  onClick: (product: Product) => void;
}

export function ProductCard({ product, onClick }: ProductCardProps) {
  const isOutOfStock = product.stock <= 0;

  return (
    <button
      onClick={() => !isOutOfStock && onClick(product)}
      disabled={isOutOfStock}
      className={`bg-bg-secondary border border-brand-primary/20 rounded-xl p-4 flex flex-col gap-4 h-[240px] text-left transition-all duration-200 ${
        isOutOfStock 
          ? 'opacity-50 cursor-not-allowed' 
          : 'hover:-translate-y-1 hover:shadow-lg hover:border-brand-primary/50 active:scale-[0.98]'
      }`}
    >
      <div className="w-full h-32 bg-bg-primary rounded-lg overflow-hidden flex items-center justify-center">
        {product.image ? (
          <img src={product.image} alt={product.name} className="w-full h-full object-cover" />
        ) : (
          <span className="text-brand-primary/50 text-4xl font-bold">{product.name.charAt(0)}</span>
        )}
      </div>
      <div className="flex flex-col flex-1 justify-between">
        <h3 className="text-text-primary font-medium line-clamp-2 leading-tight">{product.name}</h3>
        <div className="flex justify-between items-center mt-2">
          <span className="text-brand-secondary font-black text-lg">${product.price.toFixed(2)}</span>
          <span className={`text-xs font-bold px-2 py-1 rounded ${
            isOutOfStock ? 'bg-red-500/20 text-red-500' : 'bg-brand-primary/20 text-brand-primary'
          }`}>
            {isOutOfStock ? 'AGOTADO' : `${product.stock} un.`}
          </span>
        </div>
      </div>
    </button>
  );
}