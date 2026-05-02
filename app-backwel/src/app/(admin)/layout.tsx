import Link from "next/link";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex flex-col min-h-screen bg-background-primary">
      <nav className="sticky top-0 z-50 bg-background-secondary border-b border-brand-primary/20">
        <div className="max-w-screen-2xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-8">
            <span className="text-xl font-bold text-text-primary tracking-tight">BACKWEL</span>
            <div className="hidden md:flex items-center gap-6">
              <div className="relative group h-16 flex items-center">
                <span className="text-sm font-medium text-text-primary/80 group-hover:text-brand-secondary cursor-pointer transition-colors">
                  System
                </span>
                <div className="absolute top-16 left-0 w-48 bg-background-secondary border border-brand-primary/20 shadow-lg rounded-b-md opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-150 ease-out flex flex-col">
                  <Link href="/admin/system/config" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Configuration</Link>
                  <Link href="/admin/system/health" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Health</Link>
                  <Link href="/admin/system/logs" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Logs</Link>
                  <Link href="/admin/system/traffic" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Traffic Analytics</Link>
                </div>
              </div>
              <div className="relative group h-16 flex items-center">
                <span className="text-sm font-medium text-text-primary/80 group-hover:text-brand-secondary cursor-pointer transition-colors">
                  Users
                </span>
                <div className="absolute top-16 left-0 w-48 bg-background-secondary border border-brand-primary/20 shadow-lg rounded-b-md opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-150 ease-out flex flex-col">
                  <Link href="/admin/users/overview" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Overview</Link>
                  <Link href="/admin/users/clients" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Clients</Link>
                  <Link href="/admin/users/managers" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Managers</Link>
                  <Link href="/admin/users/admins" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Administrators</Link>
                  <Link href="/admin/users/security-logs" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Security Logs</Link>
                </div>
              </div>
              <div className="relative group h-16 flex items-center">
                <span className="text-sm font-medium text-text-primary/80 group-hover:text-brand-secondary cursor-pointer transition-colors">
                  Inventory
                </span>
                <div className="absolute top-16 left-0 w-48 bg-background-secondary border border-brand-primary/20 shadow-lg rounded-b-md opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-150 ease-out flex flex-col">
                  <Link href="/admin/inventory/products" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Products</Link>
                  <Link href="/admin/inventory/stock" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Stock Control</Link>
                  <Link href="/admin/inventory/traceability" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Traceability</Link>
                </div>
              </div>
              <div className="relative group h-16 flex items-center">
                <span className="text-sm font-medium text-text-primary/80 group-hover:text-brand-secondary cursor-pointer transition-colors">
                  Sales
                </span>
                <div className="absolute top-16 left-0 w-48 bg-background-secondary border border-brand-primary/20 shadow-lg rounded-b-md opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-150 ease-out flex flex-col">
                  <Link href="/admin/sales/overview" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Overview</Link>
                  <Link href="/admin/sales/logs" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Sales Logs</Link>
                  <Link href="/admin/sales/invoices" className="px-4 py-3 text-sm text-text-primary hover:bg-brand-primary/10 hover:text-brand-secondary">Invoices</Link>
                </div>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-4">
            <div className="w-8 h-8 rounded-full bg-brand-primary flex items-center justify-center text-white text-sm font-bold">
              A
            </div>
          </div>
        </div>
      </nav>
      <main className="flex-1 w-full max-w-screen-2xl mx-auto px-6 py-8">
        {children}
      </main>
    </div>
  );
}