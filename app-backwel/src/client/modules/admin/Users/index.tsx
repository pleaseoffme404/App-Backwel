import React, { useState } from 'react';

interface UserRow {
  id: string;
  email: string;
  role: string;
}

export default function UsuariosAdmin() {
  const [users, setUsers] = useState<UserRow[]>([
    { id: '1', email: 'admin@backwel.com', role: 'OWNER' }
  ]);
  const [activeTab, setActiveTab] = useState<'list' | 'create'>('list');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [roleName, setRoleName] = useState('USER');
  const [status, setStatus] = useState<{ type: 'success' | 'error', msg: string } | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedUser, setSelectedUser] = useState<string | null>(null);

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setStatus(null);

    try {
      const res = await fetch('/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password })
      });

      if (res.ok) {
        setUsers([{ id: Date.now().toString(), email, role: roleName }, ...users]);
        setStatus({ type: 'success', msg: `Cuenta de ${email} creada exitosamente.` });
        
        if (roleName !== 'USER') {
          await fetch('/auth/api/v1/roles/grant', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, roleName })
          });
        }
        
        setEmail('');
        setPassword('');
        setTimeout(() => setActiveTab('list'), 1500);
      } else {
        const errorData = await res.json().catch(() => null);
        setStatus({ type: 'error', msg: errorData?.message || 'Error de validación. Revisa la contraseña.' });
      }
    } catch (error) {
      setStatus({ type: 'error', msg: 'Error de red al intentar conectar con el servidor.' });
    } finally {
      setIsLoading(false);
    }
  };

  const handleRoleAction = async (targetEmail: string, action: 'grant' | 'revoke', role: string) => {
    setIsLoading(true);
    try {
      const res = await fetch(`/auth/api/v1/roles/${action}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: targetEmail, roleName: role })
      });
      if (res.ok) {
        setUsers(users.map(u => u.email === targetEmail ? { ...u, role: action === 'grant' ? role : 'USER' } : u));
      } else {
        alert('Error al modificar los permisos del usuario.');
      }
    } catch (error) {
      alert('Error de red al modificar los permisos.');
    } finally {
      setIsLoading(false);
      setSelectedUser(null);
    }
  };

  return (
    <div className="w-full max-w-5xl mx-auto flex flex-col gap-6 pb-12">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-black text-brand-primary tracking-tight">Directorio de Usuarios</h1>
          <p className="text-text-primary/70">Gestiona tus clientes y los accesos del personal.</p>
        </div>
        <div className="bg-bg-secondary p-1 rounded-lg border border-brand-primary/20 flex">
          <button
            onClick={() => setActiveTab('list')}
            className={`px-4 py-2 rounded-md font-bold text-sm transition-colors ${activeTab === 'list' ? 'bg-brand-primary text-bg-primary' : 'text-text-primary/60 hover:text-brand-primary'}`}
          >
            Listado
          </button>
          <button
            onClick={() => setActiveTab('create')}
            className={`px-4 py-2 rounded-md font-bold text-sm transition-colors ${activeTab === 'create' ? 'bg-brand-primary text-bg-primary' : 'text-text-primary/60 hover:text-brand-primary'}`}
          >
            + Nuevo
          </button>
        </div>
      </div>

      {status && (
        <div className={`p-4 rounded-xl border font-bold ${status.type === 'success' ? 'bg-brand-primary/10 border-brand-primary text-brand-primary' : 'bg-red-500/10 border-red-500 text-red-500'}`}>
          {status.msg}
        </div>
      )}

      {activeTab === 'list' ? (
        <div className="bg-bg-secondary border border-brand-primary/20 rounded-xl overflow-hidden shadow-sm min-h-[400px]">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="bg-bg-primary/50 border-b border-brand-primary/10">
                <th className="p-4 text-xs font-black text-brand-primary uppercase tracking-wider">Email</th>
                <th className="p-4 text-xs font-black text-brand-primary uppercase tracking-wider">Rol Asignado</th>
                <th className="p-4 text-xs font-black text-brand-primary uppercase tracking-wider text-right">Acciones</th>
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id} className="border-b border-brand-primary/5 hover:bg-bg-primary/30 transition-colors">
                  <td className="p-4 font-medium text-text-primary">{u.email}</td>
                  <td className="p-4">
                    <span className={`px-2 py-1 text-xs font-bold rounded border ${u.role === 'USER' ? 'bg-bg-primary text-text-primary/70 border-brand-primary/10' : 'bg-brand-primary/10 text-brand-primary border-brand-primary/20'}`}>
                      {u.role}
                    </span>
                  </td>
                  <td className="p-4 text-right relative">
                    <button onClick={() => setSelectedUser(selectedUser === u.id ? null : u.id)} className="text-text-primary/50 hover:text-brand-primary font-bold px-2 py-1">
                      Opciones
                    </button>
                    {selectedUser === u.id && (
                      <div className="absolute right-10 top-4 bg-bg-primary border border-brand-primary/20 shadow-xl rounded-lg flex flex-col p-2 z-10 w-48 text-left">
                        <span className="text-[10px] font-bold text-text-primary/40 uppercase mb-1 px-2">Cambiar Rol</span>
                        <button onClick={() => handleRoleAction(u.email, 'grant', 'ADMIN')} className="text-left px-2 py-2 text-sm font-bold text-text-primary hover:bg-bg-secondary rounded">Hacer ADMIN</button>
                        <button onClick={() => handleRoleAction(u.email, 'grant', 'CASHIER')} className="text-left px-2 py-2 text-sm font-bold text-text-primary hover:bg-bg-secondary rounded">Hacer CAJERO</button>
                        <button onClick={() => handleRoleAction(u.email, 'revoke', u.role)} className="text-left px-2 py-2 text-sm font-bold text-red-500 hover:bg-red-500/10 rounded mt-1 border-t border-brand-primary/5">Revocar Permisos</button>
                      </div>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          {users.length === 1 && (
            <p className="text-center text-text-primary/40 text-sm p-8">Crea nuevos clientes y aparecerán aquí durante esta sesión.</p>
          )}
        </div>
      ) : (
        <form onSubmit={handleCreateUser} className="bg-bg-secondary p-6 rounded-xl border border-brand-primary/20 shadow-sm flex flex-col gap-4 max-w-2xl">
          <div className="flex flex-col gap-1">
            <label className="text-xs font-bold text-text-primary/70 uppercase">Correo Electrónico</label>
            <input type="email" required value={email} onChange={e => setEmail(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" placeholder="cliente@correo.com" />
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs font-bold text-text-primary/70 uppercase">Contraseña</label>
            <input type="password" required value={password} onChange={e => setPassword(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary" />
            <span className="text-[10px] text-text-primary/50 mt-1">Mínimo 8 caracteres, 1 mayúscula, 1 número y 1 carácter especial (@#$%^&+=!)</span>
          </div>
          <div className="flex flex-col gap-1">
            <label className="text-xs font-bold text-text-primary/70 uppercase">Tipo de Cuenta</label>
            <select value={roleName} onChange={e => setRoleName(e.target.value)} className="p-3 bg-bg-primary border border-brand-primary/20 rounded-lg text-text-primary outline-none focus:border-brand-primary cursor-pointer">
              <option value="USER">CLIENTE NORMAL (Tienda en línea)</option>
              <option value="CASHIER">CAJERO (Punto de Venta)</option>
              <option value="ADMIN">ADMINISTRADOR</option>
            </select>
          </div>
          <button type="submit" disabled={isLoading} className="mt-4 py-4 bg-brand-primary text-bg-primary font-black text-lg rounded-xl hover:opacity-90 transition-all disabled:opacity-50 shadow-xl shadow-brand-primary/20">
            {isLoading ? 'Registrando...' : 'Crear Cuenta'}
          </button>
        </form>
      )}
    </div>
  );
}