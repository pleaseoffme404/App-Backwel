import React, { useState, useEffect } from 'react';

interface ItemVariant {
  itemAttributes: Record<string, string>;
  baseSalePrice: number;
  initialStock: number;
  redundancyStock: number;
  logicalLimit: number;
  images: string[];
}

export default function ProductosAdmin() {
  const [activeTab, setActiveTab] = useState('productos');
  const [categories, setCategories] = useState<any[]>([]);
  const [isCreating, setIsCreating] = useState(false);
  const [name, setName] = useState('');
  const [brand, setBrand] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [description, setDescription] = useState('');
  const [attributes, setAttributes] = useState<string[]>([]);
  const [newAttr, setNewAttr] = useState('');
const [items, setItems] = useState<ItemVariant[]>([]);

  const [isCreatingCategory, setIsCreatingCategory] = useState(false);
  const [catName, setCatName] = useState('');
  const [catDescription, setCatDescription] = useState('');
  const [catParentId, setCatParentId] = useState('');

  const fetchCategories = async () => {
    try {
      const res = await fetch('/api/v1/categories/');
      if (res.ok) {
        const data = await res.json();
        setCategories(data);
      }
    } catch (error) {}
  };

  useEffect(() => {
    fetchCategories();
  }, []);

const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('/api/v1/categories/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          categoryName: catName,
          parentId: catParentId || null
        })
      });
      if (res.ok) {
        setIsCreatingCategory(false);
        setCatName('');
        setCatDescription('');
        setCatParentId('');
        fetchCategories();
      } else {
        alert('Error al crear la categoría. Revisa los datos.');
      }
    } catch (err) {
      alert('Error de red al conectar con el servidor.');
    }
  };

  const handleAddAttribute = (e: React.KeyboardEvent | React.MouseEvent) => {
    if (('key' in e && e.key === 'Enter') || e.type === 'click') {
      e.preventDefault();
      if (newAttr.trim() && !attributes.includes(newAttr.trim())) {
        setAttributes([...attributes, newAttr.trim()]);
        setNewAttr('');
      }
    }
  };

  const handleAddItem = () => {
    const defaultAttrs: Record<string, string> = {};
    attributes.forEach(attr => defaultAttrs[attr] = '');
    setItems([...items, {
      itemAttributes: defaultAttrs,
      baseSalePrice: 0.01,
      initialStock: 0,
      redundancyStock: 0,
      logicalLimit: 1,
      images: ['']
    }]);
  };

  const updateItem = (index: number, field: keyof ItemVariant, value: any) => {
    const updated = [...items];
    updated[index] = { ...updated[index], [field]: value };
    setItems(updated);
  };

  const updateItemAttribute = (itemIndex: number, attrName: string, value: string) => {
    const updated = [...items];
    updated[itemIndex].itemAttributes = { ...updated[itemIndex].itemAttributes, [attrName]: value };
    setItems(updated);
  };

  const updateItemImage = (itemIndex: number, imgIndex: number, url: string) => {
    const updated = [...items];
    const newImages = [...updated[itemIndex].images];
    newImages[imgIndex] = url;
    updated[itemIndex].images = newImages;
    setItems(updated);
  };

  const addImageField = (itemIndex: number) => {
    const updated = [...items];
    updated[itemIndex].images.push('');
    setItems(updated);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const payload = { name, description, categoryId, brand, attributes, items };
    try {
      const res = await fetch('/api/v1/products/', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (res.ok) {
        setIsCreating(false);
        setItems([]);
        setAttributes([]);
        setName('');
        setBrand('');
        setDescription('');
        setCategoryId('');
      } else {
        const error = await res.json();
        alert('Error al crear producto: ' + JSON.stringify(error));
      }
    } catch (err) {
      alert('Error de red al conectar con el servidor.');
    }
  };

const renderCategorias = () => {
    if (isCreatingCategory) {
      return (
        <div className="w-full max-w-3xl mx-auto flex flex-col gap-6 pb-12">
          <div className="flex items-center justify-between border-b border-brand-primary/10 pb-4">
            <h1 className="text-3xl font-black text-brand-primary tracking-tight">Nueva Categoría</h1>
            <button onClick={() => setIsCreatingCategory(false)} className="text-text-primary/60 hover:text-accent font-medium">Cancelar</button>
          </div>
          <form onSubmit={handleCreateCategory} className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-4 shadow-sm">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase">Nombre</label>
              <input type="text" required value={catName} onChange={e => setCatName(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Ej: Laptops, Ropa, Herramientas..." />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase">Categoría Padre (Opcional)</label>
              <select value={catParentId} onChange={e => setCatParentId(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary">
                <option value="">Ninguna (Categoría Raíz)</option>
                {categories.map((cat: any) => (
                  <option key={cat.categoryId || cat.id} value={cat.categoryId || cat.id}>{cat.name}</option>
                ))}
              </select>
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-bold text-text-primary/70 uppercase">Descripción</label>
              <textarea required value={catDescription} onChange={e => setCatDescription(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary min-h-[100px] resize-y" placeholder="Descripción de la categoría..."></textarea>
            </div>
            <button type="submit" className="mt-4 py-4 bg-brand-primary text-bg-primary font-black text-lg rounded-xl hover:opacity-90 transition-all shadow-xl shadow-brand-primary/20">
              Guardar Categoría
            </button>
          </form>
        </div>
      );
    }

    return (
      <div className="w-full flex flex-col gap-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-black text-brand-primary tracking-tight">Categorías</h1>
            <p className="text-text-primary/70">Organiza tu catálogo creando un árbol de categorías.</p>
          </div>
          <button onClick={() => setIsCreatingCategory(true)} className="bg-brand-primary text-bg-primary px-6 py-3 rounded-lg font-bold hover:opacity-90 transition-opacity flex items-center gap-2">
            + Nueva Categoría
          </button>
        </div>
        
        <div className="flex flex-col gap-3">
          {categories.length === 0 ? (
            <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl p-8 text-center flex flex-col items-center justify-center min-h-[200px]">
              <h3 className="text-xl font-bold text-text-primary mb-2">Aún no hay categorías</h3>
              <p className="text-text-primary/60">Crea tu primera categoría raíz para empezar a ordenar tus productos.</p>
            </div>
          ) : (
            categories.map((cat: any) => (
              <div key={cat.categoryId || cat.id} className="bg-bg-secondary p-4 rounded-xl border border-brand-primary/10 flex items-center justify-between hover:border-brand-primary/30 transition-colors">
                <div className="flex flex-col">
                  <span className="font-bold text-lg text-brand-primary">{cat.name}</span>
                  <span className="text-sm text-text-primary/60 line-clamp-1">{cat.description}</span>
                </div>
                <span className="text-xs font-mono bg-bg-primary px-2 py-1 rounded text-text-primary/40 border border-brand-primary/10 select-all">
                  {cat.categoryId || cat.id}
                </span>
              </div>
            ))
          )}
        </div>
      </div>
    );
  };

  const renderDescuentos = () => (
    <div className="flex flex-col items-center justify-center h-full text-text-primary/50">
      <h2 className="text-2xl font-bold mb-2 text-brand-primary">Gestión de Descuentos</h2>
      <p>Módulo en construcción.</p>
    </div>
  );

  const renderInventario = () => (
    <div className="flex flex-col items-center justify-center h-full text-text-primary/50">
      <h2 className="text-2xl font-bold mb-2 text-brand-primary">Gestión de Inventario</h2>
      <p>Módulo en construcción.</p>
    </div>
  );

  const renderProductos = () => {
    if (!isCreating) {
      return (
        <div className="w-full flex flex-col gap-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-black text-brand-primary tracking-tight">Catálogo de Productos</h1>
              <p className="text-text-primary/70">Gestiona los productos base y sus variantes.</p>
            </div>
            <button 
              onClick={() => setIsCreating(true)}
              className="bg-brand-primary text-bg-primary px-6 py-3 rounded-lg font-bold hover:opacity-90 transition-opacity flex items-center gap-2"
            >
              + Nuevo Producto
            </button>
          </div>
          <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl p-8 text-center flex flex-col items-center justify-center min-h-[400px]">
            <h3 className="text-xl font-bold text-text-primary mb-2">Aún no hay productos</h3>
            <p className="text-text-primary/60 max-w-md">Crea tu primer producto para empezar a gestionar el inventario.</p>
          </div>
        </div>
      );
    }

    return (
      <div className="w-full max-w-5xl mx-auto flex flex-col gap-6 pb-12">
        <div className="flex items-center justify-between border-b border-brand-primary/10 pb-4">
          <h1 className="text-3xl font-black text-brand-primary tracking-tight">Crear Nuevo Producto</h1>
          <button onClick={() => setIsCreating(false)} className="text-text-primary/60 hover:text-accent font-medium">Cancelar</button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-8">
          <section className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-4 shadow-sm">
            <h2 className="text-xl font-bold text-brand-primary flex items-center gap-2">
              <span className="w-6 h-px bg-brand-primary/30"></span> Información Base
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Nombre del Producto</label>
                <input type="text" required value={name} onChange={e => setName(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Ej: Laptop ThinkPad L14" />
              </div>
              <div className="flex flex-col gap-1">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Marca</label>
                <input type="text" required value={brand} onChange={e => setBrand(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Ej: Lenovo" />
              </div>
              <div className="flex flex-col gap-1 md:col-span-2">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Categoría</label>
                <select required value={categoryId} onChange={e => setCategoryId(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary">
                  <option value="" disabled>Selecciona una categoría...</option>
                  {categories.map((cat: any) => (
                    <option key={cat.categoryId || cat.id} value={cat.categoryId || cat.id}>{cat.name}</option>
                  ))}
                </select>
              </div>
              <div className="flex flex-col gap-1 md:col-span-2">
                <label className="text-xs font-bold text-text-primary/70 uppercase">Descripción</label>
                <textarea required value={description} onChange={e => setDescription(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary min-h-[100px] resize-y" placeholder="Descripción detallada del producto..."></textarea>
              </div>
            </div>
          </section>

          <section className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-4 shadow-sm">
            <h2 className="text-xl font-bold text-brand-primary flex items-center gap-2">
              <span className="w-6 h-px bg-brand-primary/30"></span> Estructura de Atributos
            </h2>
            <div className="flex gap-2">
              <input type="text" value={newAttr} onKeyDown={handleAddAttribute} onChange={e => setNewAttr(e.target.value)} className="flex-1 p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="Nuevo atributo (Ej: Talla, Color)..." />
              <button type="button" onClick={handleAddAttribute} className="bg-brand-secondary text-bg-primary px-6 rounded-lg font-bold hover:opacity-90">Agregar</button>
            </div>
            {attributes.length > 0 && (
              <div className="flex flex-wrap gap-2 mt-2">
                {attributes.map(attr => (
                  <span key={attr} className="bg-brand-primary/10 text-brand-primary px-3 py-1 rounded-full text-sm font-bold flex items-center gap-2 border border-brand-primary/20">
                    {attr}
                    <button type="button" onClick={() => setAttributes(attributes.filter(a => a !== attr))} className="hover:text-accent">&times;</button>
                  </span>
                ))}
              </div>
            )}
          </section>

          <section className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 flex flex-col gap-6 shadow-sm">
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-bold text-brand-primary flex items-center gap-2">
                <span className="w-6 h-px bg-brand-primary/30"></span> Variantes (Items)
              </h2>
              <button type="button" onClick={handleAddItem} disabled={attributes.length === 0} className="text-sm bg-accent text-white px-4 py-2 rounded-lg font-bold disabled:opacity-50 hover:opacity-90 transition-opacity">
                + Añadir Variante
              </button>
            </div>

            {items.map((item, idx) => (
              <div key={idx} className="bg-bg-primary p-5 rounded-lg border border-brand-primary/10 flex flex-col gap-4 relative">
                <button type="button" onClick={() => setItems(items.filter((_, i) => i !== idx))} className="absolute top-4 right-4 text-text-primary/40 hover:text-accent font-bold">&times; Eliminar</button>
                
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  {attributes.map(attr => (
                    <div key={attr} className="flex flex-col gap-1">
                      <label className="text-xs font-bold text-brand-secondary uppercase">{attr}</label>
                      <input type="text" required value={item.itemAttributes[attr] || ''} onChange={e => updateItemAttribute(idx, attr, e.target.value)} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" placeholder="Valor..." />
                    </div>
                  ))}
                </div>

                <div className="grid grid-cols-2 md:grid-cols-4 gap-4 border-t border-brand-primary/10 pt-4">
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Precio Base ($)</label>
                    <input type="number" step="0.01" min="0.01" required value={item.baseSalePrice} onChange={e => updateItem(idx, 'baseSalePrice', parseFloat(e.target.value))} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Stock Inicial</label>
                    <input type="number" min="0" required value={item.initialStock} onChange={e => updateItem(idx, 'initialStock', parseInt(e.target.value, 10))} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Stock Redundancia</label>
                    <input type="number" min="0" required value={item.redundancyStock} onChange={e => updateItem(idx, 'redundancyStock', parseInt(e.target.value, 10))} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                  <div className="flex flex-col gap-1">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">Límite Lógico</label>
                    <input type="number" min="1" required value={item.logicalLimit} onChange={e => updateItem(idx, 'logicalLimit', parseInt(e.target.value, 10))} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary" />
                  </div>
                </div>

                <div className="border-t border-brand-primary/10 pt-4 flex flex-col gap-2">
                  <div className="flex items-center justify-between">
                    <label className="text-xs font-bold text-text-primary/70 uppercase">URLs de Imágenes</label>
                    <button type="button" onClick={() => addImageField(idx)} className="text-xs text-brand-primary font-bold hover:underline">+ Añadir Imagen</button>
                  </div>
                  {item.images.map((img, imgIdx) => (
                    <input key={imgIdx} type="url" required value={img} onChange={e => updateItemImage(idx, imgIdx, e.target.value)} className="p-2 bg-bg-secondary border border-brand-primary/20 rounded text-sm text-text-primary outline-none focus:border-brand-primary w-full" placeholder="https://..." />
                  ))}
                </div>
              </div>
            ))}
            {items.length === 0 && <p className="text-sm text-text-primary/50 text-center py-4">Define primero los atributos arriba y luego añade variantes.</p>}
          </section>

          <button type="submit" disabled={items.length === 0} className="w-full py-4 bg-brand-primary text-bg-primary font-black text-lg rounded-xl hover:opacity-90 transition-all disabled:opacity-50 shadow-xl shadow-brand-primary/20">
            Guardar Producto en Base de Datos
          </button>
        </form>
      </div>
    );
  };

  const navClasses = (tabName: string) => 
    `w-full text-left px-4 py-3 rounded-lg font-bold transition-all ${activeTab === tabName ? 'bg-brand-primary text-bg-primary shadow-md' : 'text-text-primary/70 hover:bg-bg-primary hover:text-brand-primary'}`;

  return (
    <div className="flex h-[calc(100vh-10rem)] gap-6">
      <aside className="w-64 shrink-0 flex flex-col gap-2 bg-bg-secondary p-4 rounded-xl border border-brand-primary/20">
        <h2 className="text-sm font-black text-text-primary/40 uppercase tracking-widest mb-2 px-2">Operaciones</h2>
        <button onClick={() => setActiveTab('productos')} className={navClasses('productos')}>
          Productos
        </button>
        <button onClick={() => setActiveTab('categorias')} className={navClasses('categorias')}>
          Categorías
        </button>
        <button onClick={() => setActiveTab('descuentos')} className={navClasses('descuentos')}>
          Descuentos
        </button>
        <button onClick={() => setActiveTab('inventario')} className={navClasses('inventario')}>
          Inventario
        </button>
      </aside>

      <main className="flex-1 bg-bg-primary rounded-xl overflow-y-auto pr-2">
        {activeTab === 'productos' && renderProductos()}
        {activeTab === 'categorias' && renderCategorias()}
        {activeTab === 'descuentos' && renderDescuentos()}
        {activeTab === 'inventario' && renderInventario()}
      </main>
    </div>
  );
}