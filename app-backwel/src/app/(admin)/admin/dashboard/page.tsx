export default function DashboardPage() {
  return (
    <div className="grid grid-cols-12 gap-6">
      <div className="col-span-12 lg:col-span-4 bg-background-secondary border border-brand-primary/20 rounded-xl p-6 flex flex-col justify-between">
        <div>
          <h2 className="text-sm font-semibold text-text-primary/70 uppercase tracking-wider mb-1">License Management</h2>
          <span className="text-xs text-text-primary/50 font-mono break-all bg-background-primary px-2 py-1 rounded">
            HWID: 019d50e0-1e00-70df-a800-c6fc
          </span>
        </div>
        <div className="mt-6 flex items-end justify-between">
          <div>
            <div className="flex items-center gap-2 mb-2">
              <span className="w-2 h-2 rounded-full bg-green-500"></span>
              <span className="text-sm font-medium text-text-primary">Enterprise Plan</span>
            </div>
            <div className="text-3xl font-black text-text-primary">365 <span className="text-sm font-normal text-text-primary/70">days left</span></div>
          </div>
        </div>
      </div>

      <div className="col-span-12 lg:col-span-8 bg-background-secondary border border-brand-primary/20 rounded-xl p-6">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-sm font-semibold text-text-primary/70 uppercase tracking-wider">System Overview</h2>
          <span className="text-sm font-bold text-brand-secondary">100% Operational</span>
        </div>
        <div className="w-full bg-background-primary rounded-full h-3 mb-6">
          <div className="bg-brand-secondary h-3 rounded-full w-full"></div>
        </div>
        <div className="grid grid-cols-3 gap-4">
          <div className="flex items-center gap-3">
            <div className="w-6 h-6 rounded-full bg-green-500/20 flex items-center justify-center text-green-500 text-xs">✓</div>
            <span className="text-sm text-text-primary">Docker Nodes</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="w-6 h-6 rounded-full bg-green-500/20 flex items-center justify-center text-green-500 text-xs">✓</div>
            <span className="text-sm text-text-primary">Database Connection</span>
          </div>
          <div className="flex items-center gap-3">
            <div className="w-6 h-6 rounded-full bg-green-500/20 flex items-center justify-center text-green-500 text-xs">✓</div>
            <span className="text-sm text-text-primary">API Gateway</span>
          </div>
        </div>
      </div>

      <div className="col-span-12 md:col-span-6 lg:col-span-3 bg-background-secondary border border-brand-primary/20 rounded-xl p-6 transition-all duration-200 hover:-translate-y-1 hover:shadow-lg cursor-pointer">
        <h3 className="text-text-primary/70 text-sm font-medium">Active Discounts</h3>
        <p className="text-3xl font-bold mt-2 text-text-primary">12</p>
      </div>

      <div className="col-span-12 md:col-span-6 lg:col-span-3 bg-background-secondary border border-brand-primary/20 rounded-xl p-6 transition-all duration-200 hover:-translate-y-1 hover:shadow-lg cursor-pointer">
        <h3 className="text-text-primary/70 text-sm font-medium">Active Accounts</h3>
        <p className="text-3xl font-bold mt-2 text-text-primary">1,248</p>
      </div>

      <div className="col-span-12 md:col-span-6 lg:col-span-3 bg-background-secondary border border-brand-primary/20 rounded-xl p-6 transition-all duration-200 hover:-translate-y-1 hover:shadow-lg cursor-pointer">
        <h3 className="text-text-primary/70 text-sm font-medium">Support Tickets</h3>
        <p className="text-3xl font-bold mt-2 text-text-primary">4</p>
      </div>

      <div className="col-span-12 md:col-span-6 lg:col-span-3 bg-background-secondary border border-brand-primary/20 rounded-xl p-6 transition-all duration-200 hover:-translate-y-1 hover:shadow-lg cursor-pointer">
        <h3 className="text-text-primary/70 text-sm font-medium">System Exceptions</h3>
        <p className="text-3xl font-bold mt-2 text-text-primary">0</p>
      </div>

      <div className="col-span-12 lg:col-span-8 bg-background-secondary border border-brand-primary/20 rounded-xl p-6 min-h-[300px] flex flex-col">
        <div className="flex justify-between items-start mb-6">
          <h3 className="text-sm font-semibold text-text-primary/70 uppercase tracking-wider">Monthly Sales Volume</h3>
          <div className="bg-green-500/10 text-green-500 px-3 py-1 rounded-full text-xs font-bold">
            +14.5%
          </div>
        </div>
        <div className="flex-1 bg-background-primary/50 rounded-lg flex items-center justify-center border border-brand-primary/10">
          <span className="text-text-primary/30 text-sm">Chart Data Visualization Area</span>
        </div>
      </div>

      <div className="col-span-12 lg:col-span-4 bg-background-secondary border border-brand-primary/20 rounded-xl p-6 flex flex-col">
        <h3 className="text-sm font-semibold text-text-primary/70 uppercase tracking-wider mb-6">Top Products</h3>
        <div className="flex-1 flex flex-col gap-4">
          <div className="w-full">
            <div className="flex justify-between text-xs text-text-primary mb-1">
              <span>Industrial Server Rack</span>
              <span>450 units</span>
            </div>
            <div className="w-full bg-background-primary rounded-full h-2">
              <div className="bg-brand-primary h-2 rounded-full w-[85%]"></div>
            </div>
          </div>
          <div className="w-full">
            <div className="flex justify-between text-xs text-text-primary mb-1">
              <span>Cooling Unit V2</span>
              <span>320 units</span>
            </div>
            <div className="w-full bg-background-primary rounded-full h-2">
              <div className="bg-brand-primary h-2 rounded-full w-[65%]"></div>
            </div>
          </div>
          <div className="w-full">
            <div className="flex justify-between text-xs text-text-primary mb-1">
              <span>Power Supply 1000W</span>
              <span>180 units</span>
            </div>
            <div className="w-full bg-background-primary rounded-full h-2">
              <div className="bg-brand-primary h-2 rounded-full w-[40%]"></div>
            </div>
          </div>
        </div>
      </div>

      <div className="col-span-12 mt-4">
        <button className="w-full bg-accent hover:brightness-110 active:scale-[0.98] text-white font-bold py-4 rounded-xl shadow-md transition-all duration-200 uppercase tracking-wider text-sm flex items-center justify-center gap-2">
          <span>Report a Problem to Backwel Devs</span>
        </button>
      </div>
    </div>
  );
}      