export interface Product {
  id: string;
  name: string;
  price: number;
  stock: number;
  image: string;
}

export interface CartItem extends Product {
  quantity: number;
}