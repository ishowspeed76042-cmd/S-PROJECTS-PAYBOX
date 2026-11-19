import React, { useState, useEffect } from 'react';
import {
  Volume2,
  Bluetooth,
  BluetoothConnected,
  ShieldCheck,
  ShieldAlert,
  Power,
  PowerOff,
  Settings,
  RefreshCw,
  LogOut,
  Plus,
  Trash2,
  CheckCircle2,
  AlertTriangle,
  Lock,
  Smartphone,
  CreditCard,
  ListFilter,
  Check,
  X,
  VolumeX,
  Radio
} from 'lucide-react';

interface PaymentRecord {
  id: string;
  amount: number;
  senderName: string;
  appName: string;
  timestamp: number;
  spokenText: string;
}

interface User {
  email: string;
  shopName: string;
}

const UPI_APPS = [
  { id: 'phonepe', name: 'PhonePe', color: 'bg-purple-600' },
  { id: 'gpay', name: 'Google Pay (GPay)', color: 'bg-blue-600' },
  { id: 'paytm', name: 'Paytm', color: 'bg-cyan-600' },
  { id: 'fampay', name: 'FamPay / FamX', color: 'bg-emerald-600' },
  { id: 'navi', name: 'Navi', color: 'bg-indigo-600' },
  { id: 'bhim', name: 'BHIM UPI', color: 'bg-orange-600' },
  { id: 'cred', name: 'CRED', color: 'bg-neutral-800' },
];

export default function App() {
  const [user, setUser] = useState<User | null>({ email: 'merchant@sprojects.in', shopName: 'S-PROJECTS Main Store' });
  const [loginEmail, setLoginEmail] = useState('');
  const [loginPassword, setLoginPassword] = useState('');

  // Subscription state
  const [isPaid, setIsPaid] = useState(true);
  const [expiryDate, setExpiryDate] = useState<number>(Date.now() + 14 * 24 * 60 * 60 * 1000); // 14 days trial/validity
  const [showSubDialog, setShowSubDialog] = useState(false);

  // PayBox states
  const [isPayBoxRunning, setIsPayBoxRunning] = useState(true);
  const [audioRouteMode, setAudioRouteMode] = useState<'BLUETOOTH_ONLY' | 'PHONE_SPEAKER'>('BLUETOOTH_ONLY');
  const [bluetoothConnected, setBluetoothConnected] = useState(true);
  const [btDeviceName, setBtDeviceName] = useState('S-Projects SoundBox BT-88');
  const [notificationAccess, setNotificationAccess] = useState(true);
  const [warningMessage, setWarningMessage] = useState<string | null>(null);

  // UPI Apps selection
  const [enableAllUpiApps, setEnableAllUpiApps] = useState(true);
  const [selectedUpiAppIds, setSelectedUpiAppIds] = useState<string[]>(UPI_APPS.map(a => a.id));
  const [showUpiPicker, setShowUpiPicker] = useState(false);

  // Simulated payments history
  const [payments, setPayments] = useState<PaymentRecord[]>([
    {
      id: '1',
      amount: 150,
      senderName: 'Rahul Sharma',
      appName: 'PhonePe',
      timestamp: Date.now() - 12 * 60 * 1000,
      spokenText: 'Received rupees 150 from Rahul Sharma via PhonePe'
    },
    {
      id: '2',
      amount: 499,
      senderName: 'Priya Patel',
      appName: 'Google Pay (GPay)',
      timestamp: Date.now() - 45 * 60 * 1000,
      spokenText: 'Received rupees 499 from Priya Patel via Google Pay'
    },
    {
      id: '3',
      amount: 50,
      senderName: 'Amit Kumar',
      appName: 'Paytm',
      timestamp: Date.now() - 120 * 60 * 1000,
      spokenText: 'Received rupees 50 from Amit Kumar via Paytm'
    }
  ]);

  const [showSimulateDialog, setShowSimulateDialog] = useState(false);
  const [simAmount, setSimAmount] = useState('250');
  const [simSender, setSimSender] = useState('Vikram Singh');
  const [simApp, setSimApp] = useState('PhonePe');

  const isExpired = expiryDate < Date.now() && !isPaid;
  const daysLeft = Math.max(0, Math.ceil((expiryDate - Date.now()) / (1000 * 60 * 60 * 24)));

  // Web Speech API for Text-to-Speech announcement simulation
  const announcePayment = (amount: number, sender: string, app: string) => {
    if (isExpired || !isPayBoxRunning) return;
    const text = `Received rupees ${amount} from ${sender} via ${app}`;
    
    // Add to records
    const newRecord: PaymentRecord = {
      id: Date.now().toString(),
      amount,
      senderName: sender,
      appName: app,
      timestamp: Date.now(),
      spokenText: text
    };
    setPayments(prev => [newRecord, ...prev]);

    // Speak using Web Speech API if supported & routed correctly
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
      const utterance = new SpeechSynthesisUtterance(text);
      utterance.rate = 1.0;
      utterance.pitch = 1.0;
      window.speechSynthesis.speak(utterance);
    }
  };

  const handleSimulateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const amt = parseFloat(simAmount) || 100;
    announcePayment(amt, simSender || 'Guest Customer', simApp);
    setShowSimulateDialog(false);
  };

  const handleTestSound = () => {
    announcePayment(10, 'S-Projects Test', 'SoundBox');
  };

  if (!user) {
    return (
      <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4">
        <div className="max-w-md w-full bg-slate-900 border border-slate-800 rounded-2xl p-8 shadow-2xl">
          <div className="text-center mb-8">
            <div className="inline-flex p-3 rounded-2xl bg-cyan-500/10 text-cyan-400 mb-4 border border-cyan-500/20">
              <Radio className="w-8 h-8" />
            </div>
            <h1 className="text-2xl font-black text-cyan-400 tracking-wider">S-PROJECTS PAYBOX</h1>
            <p className="text-slate-400 text-sm mt-1">Automated UPI Payment Sound Box Console</p>
          </div>

          <form onSubmit={(e) => { e.preventDefault(); setUser({ email: loginEmail || 'merchant@sprojects.in', shopName: 'S-PROJECTS Store' }); }} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Merchant Email / ID</label>
              <input
                type="text"
                value={loginEmail}
                onChange={e => setLoginEmail(e.target.value)}
                placeholder="merchant@sprojects.in"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-slate-100 focus:outline-none focus:border-cyan-500 text-sm"
              />
            </div>
            <div>
              <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Secure Password / PIN</label>
              <input
                type="password"
                value={loginPassword}
                onChange={e => setLoginPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-slate-100 focus:outline-none focus:border-cyan-500 text-sm"
              />
            </div>
            <button
              type="submit"
              className="w-full bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold py-3 rounded-xl transition duration-200 text-sm shadow-lg shadow-cyan-500/20"
            >
              Sign In to PayBox Console
            </button>
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 pb-20">
      {/* Top Header */}
      <header className="sticky top-0 z-30 bg-slate-900/90 backdrop-blur-md border-b border-slate-800">
        <div className="max-w-4xl mx-auto px-4 h-16 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className={`w-3 h-3 rounded-full ${isExpired ? 'bg-red-500' : !isPayBoxRunning ? 'bg-amber-500' : bluetoothConnected ? 'bg-emerald-500' : 'bg-red-500'} animate-pulse`} />
            <div>
              <h1 className="font-black text-cyan-400 tracking-wider text-base">S-PROJECTS PAY BOX</h1>
              <p className="text-[11px] text-slate-400">
                {isExpired ? 'Subscription Expired • Locked' : isPayBoxRunning ? 'Active • Multi-UPI Detection' : 'Service Stopped'}
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => setShowSubDialog(true)}
              className={`p-2 rounded-xl border ${isExpired ? 'border-red-500/50 bg-red-500/10 text-red-400' : 'border-emerald-500/50 bg-emerald-500/10 text-emerald-400'} hover:opacity-80 transition`}
              title="Subscription Status"
            >
              <CreditCard className="w-5 h-5" />
            </button>
            <button
              onClick={() => {
                setBluetoothConnected(prev => !prev);
                setBtDeviceName(prev => prev ? '' : 'S-Projects SoundBox BT-88');
              }}
              className="p-2 rounded-xl bg-slate-800 text-slate-300 hover:text-cyan-400 transition"
              title="Toggle Bluetooth Speaker"
            >
              <RefreshCw className="w-5 h-5" />
            </button>
            <button
              onClick={() => setUser(null)}
              className="p-2 rounded-xl bg-slate-800 text-slate-300 hover:text-red-400 transition"
              title="Sign Out"
            >
              <LogOut className="w-5 h-5" />
            </button>
          </div>
        </div>
      </header>

      {/* Main Content Area */}
      <main className="max-w-4xl mx-auto px-4 py-6 space-y-4">
        {/* Warning Banner */}
        {warningMessage && (
          <div className="bg-red-500/10 border border-red-500/30 rounded-2xl p-4 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <AlertTriangle className="w-5 h-5 text-red-400 shrink-0" />
              <p className="text-sm text-slate-200">{warningMessage}</p>
            </div>
            <button onClick={() => setWarningMessage(null)} className="text-slate-400 hover:text-slate-200">
              <X className="w-5 h-5" />
            </button>
          </div>
        )}

        {/* 1. Subscription & Rent Management Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className={`p-3 rounded-xl ${isExpired ? 'bg-red-500/10 text-red-400 border border-red-500/20' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20'}`}>
                {isExpired ? <Lock className="w-6 h-6" /> : <ShieldCheck className="w-6 h-6" />}
              </div>
              <div>
                <h3 className="font-bold text-slate-100 text-base">
                  {isExpired ? 'Subscription Expired' : daysLeft <= 7 ? '7-Day Free Trial Active' : 'Paid Subscription Active'}
                </h3>
                <p className="text-xs text-slate-400">
                  {isExpired ? 'Payment pending. Please pay rent to unlock sound box.' : `Valid for ${daysLeft} more days (${new Date(expiryDate).toLocaleDateString()})`}
                </p>
              </div>
            </div>
            <button
              onClick={() => setShowSubDialog(true)}
              className={`px-4 py-2 rounded-xl font-bold text-xs transition shadow-md ${isExpired ? 'bg-red-500 hover:bg-red-400 text-slate-950 shadow-red-500/20' : 'bg-emerald-500 hover:bg-emerald-400 text-slate-950 shadow-emerald-500/20'}`}
            >
              {isExpired ? 'Pay Rent Now' : 'Manage Rent'}
            </button>
          </div>
        </div>

        {/* 2. Dual Audio Output Mode Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center space-x-2 mb-3">
            <Volume2 className="w-5 h-5 text-cyan-400" />
            <h3 className="font-bold text-slate-100 text-sm">Audio Output Mode</h3>
          </div>
          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => setAudioRouteMode('BLUETOOTH_ONLY')}
              className={`py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center space-x-2 border transition ${audioRouteMode === 'BLUETOOTH_ONLY' ? 'bg-cyan-500 border-cyan-400 text-slate-950 shadow-lg shadow-cyan-500/20' : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'}`}
            >
              <Bluetooth className="w-4 h-4" />
              <span>Bluetooth Speaker Only</span>
            </button>
            <button
              onClick={() => setAudioRouteMode('PHONE_SPEAKER')}
              className={`py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center space-x-2 border transition ${audioRouteMode === 'PHONE_SPEAKER' ? 'bg-amber-500 border-amber-400 text-slate-950 shadow-lg shadow-amber-500/20' : 'bg-slate-950 border-slate-800 text-slate-400 hover:border-slate-700'}`}
            >
              <Smartphone className="w-4 h-4" />
              <span>Phone/Tablet Speaker</span>
            </button>
          </div>
        </div>

        {/* 3. Pay Box Service Control Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <div className={`p-3 rounded-xl ${isPayBoxRunning ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-slate-800 text-slate-400'}`}>
                {isPayBoxRunning ? <Power className="w-6 h-6" /> : <PowerOff className="w-6 h-6" />}
              </div>
              <div>
                <h3 className="font-bold text-slate-100 text-base">Pay Box Service</h3>
                <p className="text-xs text-slate-400">
                  {isExpired ? 'Locked due to expired subscription' : isPayBoxRunning ? 'Running • Monitoring UPI notifications' : 'Stopped'}
                </p>
              </div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={isPayBoxRunning && !isExpired}
                onChange={() => {
                  if (isExpired) setShowSubDialog(true);
                  else setIsPayBoxRunning(prev => !prev);
                }}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-emerald-500"></div>
            </label>
          </div>

          <div className="grid grid-cols-2 gap-3">
            <button
              onClick={() => {
                if (isExpired) setShowSubDialog(true);
                else setIsPayBoxRunning(prev => !prev);
              }}
              className={`py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center space-x-2 transition ${isExpired ? 'bg-red-500 text-slate-950' : isPayBoxRunning ? 'bg-amber-500/20 border border-amber-500/40 text-amber-400 hover:bg-amber-500/30' : 'bg-emerald-500 text-slate-950'}`}
            >
              {isExpired ? <Lock className="w-4 h-4" /> : isPayBoxRunning ? <PowerOff className="w-4 h-4" /> : <Power className="w-4 h-4" />}
              <span>{isExpired ? 'Pay Rent to Unlock' : isPayBoxRunning ? 'Stop Service' : 'Start Service'}</span>
            </button>
            <button
              onClick={() => alert('PayBox application background service stopped & closed.')}
              className="py-3 px-4 rounded-xl font-bold text-xs flex items-center justify-center space-x-2 border border-red-500/30 bg-red-500/10 text-red-400 hover:bg-red-500/20 transition"
            >
              <LogOut className="w-4 h-4" />
              <span>Stop & Close</span>
            </button>
          </div>
        </div>

        {/* 4. Monitored UPI Apps Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-3">
              <div className="p-3 rounded-xl bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                <CreditCard className="w-6 h-6" />
              </div>
              <div>
                <h3 className="font-bold text-slate-100 text-base">Monitored UPI Apps</h3>
                <p className="text-xs text-slate-400">
                  {enableAllUpiApps ? `All ${UPI_APPS.length} verified UPI apps enabled` : `${selectedUpiAppIds.length} of ${UPI_APPS.length} apps selected`}
                </p>
              </div>
            </div>
            <label className="relative inline-flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={enableAllUpiApps}
                onChange={e => setEnableAllUpiApps(e.target.checked)}
                className="sr-only peer"
              />
              <div className="w-11 h-6 bg-slate-800 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-slate-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-cyan-500"></div>
            </label>
          </div>

          <button
            onClick={() => setShowUpiPicker(true)}
            className="w-full py-3 px-4 rounded-xl border border-cyan-500/30 bg-cyan-500/10 text-cyan-400 font-semibold text-xs flex items-center justify-center space-x-2 hover:bg-cyan-500/20 transition"
          >
            <ListFilter className="w-4 h-4" />
            <span>Choose Specific UPI Apps</span>
          </button>
        </div>

        {/* 5. Bluetooth Speaker Connection Card */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg">
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center space-x-3">
              <div className={`p-3 rounded-xl ${bluetoothConnected ? 'bg-cyan-500/10 text-cyan-400 border border-cyan-500/20' : 'bg-red-500/10 text-red-400 border border-red-500/20'}`}>
                {bluetoothConnected ? <BluetoothConnected className="w-6 h-6" /> : <Bluetooth className="w-6 h-6" />}
              </div>
              <div>
                <h3 className="font-bold text-slate-100 text-base">Bluetooth Speaker</h3>
                <p className="text-xs text-slate-400">
                  {bluetoothConnected ? `Connected to: ${btDeviceName}` : 'No Bluetooth Speaker Connected'}
                </p>
              </div>
            </div>
            <span className={`px-3 py-1 rounded-full text-[10px] font-black ${bluetoothConnected ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30' : 'bg-red-500/10 text-red-400 border border-red-500/30'}`}>
              {bluetoothConnected ? 'CONNECTED' : 'DISCONNECTED'}
            </span>
          </div>

          <div className="grid grid-cols-2 gap-3 mt-4">
            <button
              onClick={() => alert('Opening Bluetooth Settings...')}
              className="py-3 px-4 rounded-xl border border-cyan-500/30 bg-cyan-500/10 text-cyan-400 font-semibold text-xs flex items-center justify-center space-x-2 hover:bg-cyan-500/20 transition"
            >
              <Settings className="w-4 h-4" />
              <span>BT Settings</span>
            </button>
            <button
              onClick={handleTestSound}
              className="py-3 px-4 rounded-xl bg-cyan-500 text-slate-950 font-bold text-xs flex items-center justify-center space-x-2 hover:bg-cyan-400 transition shadow-lg shadow-cyan-500/20"
            >
              <Volume2 className="w-4 h-4" />
              <span>Test Sound</span>
            </button>
          </div>
        </div>

        {/* 6. Verified Payment Logs Header */}
        <div className="flex items-center justify-between pt-2">
          <h2 className="text-lg font-black text-cyan-400 tracking-wide">
            Verified Payment Logs ({payments.length})
          </h2>
          {payments.length > 0 && (
            <button
              onClick={() => setPayments([])}
              className="text-xs font-semibold text-red-400 hover:text-red-300 flex items-center space-x-1"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Clear All</span>
            </button>
          )}
        </div>

        {/* Payment History List */}
        {payments.length === 0 ? (
          <div className="bg-slate-900/60 border border-slate-800/80 rounded-2xl p-10 text-center">
            <div className="inline-flex p-4 rounded-2xl bg-slate-800 text-slate-500 mb-3">
              <CreditCard className="w-8 h-8" />
            </div>
            <p className="text-sm text-slate-400">No verified payments received yet.</p>
          </div>
        ) : (
          <div className="space-y-3">
            {payments.map(record => (
              <div key={record.id} className="bg-slate-900 border border-slate-800 rounded-2xl p-4 flex items-center justify-between shadow-md hover:border-cyan-500/30 transition">
                <div className="space-y-1">
                  <div className="flex items-center space-x-2">
                    <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                      {record.appName}
                    </span>
                    <span className="font-semibold text-sm text-slate-100">{record.senderName}</span>
                  </div>
                  <p className="text-xs text-slate-400">{record.spokenText}</p>
                  <p className="text-[10px] text-slate-500">{new Date(record.timestamp).toLocaleString()}</p>
                </div>
                <div className="text-right">
                  <span className="text-xl font-black text-emerald-400">₹{record.amount}</span>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Floating Simulate Payment Button */}
      {!isExpired && (
        <div className="fixed bottom-6 right-6 z-40">
          <button
            onClick={() => setShowSimulateDialog(true)}
            className="bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold p-4 rounded-2xl shadow-2xl shadow-cyan-500/40 flex items-center space-x-2 transition transform hover:scale-105"
          >
            <Plus className="w-6 h-6" />
            <span className="text-sm">Simulate Payment</span>
          </button>
        </div>
      )}

      {/* Simulate Payment Dialog */}
      {showSimulateDialog && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-cyan-400">Simulate UPI Payment</h3>
              <button onClick={() => setShowSimulateDialog(false)} className="text-slate-400 hover:text-slate-200">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSimulateSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Amount (₹)</label>
                <input
                  type="number"
                  value={simAmount}
                  onChange={e => setSimAmount(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-slate-100 focus:outline-none focus:border-cyan-500 text-sm"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-1">Sender Name</label>
                <input
                  type="text"
                  value={simSender}
                  onChange={e => setSimSender(e.target.value)}
                  className="w-full bg-slate-950 border border-slate-800 rounded-xl px-4 py-3 text-slate-100 focus:outline-none focus:border-cyan-500 text-sm"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase text-slate-400 mb-2">Payment App</label>
                <div className="grid grid-cols-2 gap-2">
                  {UPI_APPS.map(app => (
                    <button
                      key={app.id}
                      type="button"
                      onClick={() => setSimApp(app.name)}
                      className={`py-2 px-3 rounded-xl text-xs font-semibold border transition text-left ${simApp === app.name ? 'bg-cyan-500 border-cyan-400 text-slate-950' : 'bg-slate-950 border-slate-800 text-slate-300 hover:border-slate-700'}`}
                    >
                      {app.name}
                    </button>
                  ))}
                </div>
              </div>

              <div className="pt-2 flex space-x-3">
                <button
                  type="button"
                  onClick={() => setShowSimulateDialog(false)}
                  className="flex-1 bg-slate-800 hover:bg-slate-700 text-slate-300 font-bold py-3 rounded-xl text-sm transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold py-3 rounded-xl text-sm transition shadow-lg shadow-cyan-500/20"
                >
                  Announce & Save
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Subscription Dialog */}
      {showSubDialog && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-cyan-400">Subscription & Rent Management</h3>
              <button onClick={() => setShowSubDialog(false)} className="text-slate-400 hover:text-slate-200">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="bg-slate-950 border border-slate-800 rounded-xl p-4 space-y-2">
              <div className="flex justify-between text-sm">
                <span className="text-slate-400">Current Status:</span>
                <span className={`font-bold ${isExpired ? 'text-red-400' : 'text-emerald-400'}`}>{isExpired ? 'Expired / Locked' : 'Active'}</span>
              </div>
              <div className="flex justify-between text-sm">
                <span className="text-slate-400">Expiry Date:</span>
                <span className="text-slate-200 font-semibold">{new Date(expiryDate).toLocaleDateString()}</span>
              </div>
            </div>

            <div className="space-y-3">
              <button
                onClick={() => {
                  setExpiryDate(Date.now() + 30 * 24 * 60 * 60 * 1000);
                  setIsPaid(true);
                  setShowSubDialog(false);
                  alert('Rent paid successfully! Subscription extended by 30 days.');
                }}
                className="w-full bg-emerald-500 hover:bg-emerald-400 text-slate-950 font-bold py-3 rounded-xl text-sm transition shadow-lg shadow-emerald-500/20"
              >
                Pay Monthly Rent (₹299 / month)
              </button>
              <button
                onClick={() => {
                  setExpiryDate(Date.now() + 365 * 24 * 60 * 60 * 1000);
                  setIsPaid(true);
                  setShowSubDialog(false);
                  alert('Annual subscription activated successfully!');
                }}
                className="w-full bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold py-3 rounded-xl text-sm transition shadow-lg shadow-cyan-500/20"
              >
                Pay Annual Rent (₹2,999 / year)
              </button>
            </div>
          </div>
        </div>
      )}

      {/* UPI App Picker Dialog */}
      {showUpiPicker && (
        <div className="fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-lg font-bold text-cyan-400">Select Monitored UPI Apps</h3>
              <button onClick={() => setShowUpiPicker(false)} className="text-slate-400 hover:text-slate-200">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
              {UPI_APPS.map(app => {
                const isSelected = selectedUpiAppIds.includes(app.id);
                return (
                  <div
                    key={app.id}
                    onClick={() => {
                      if (isSelected) {
                        setSelectedUpiAppIds(prev => prev.filter(id => id !== app.id));
                      } else {
                        setSelectedUpiAppIds(prev => [...prev, app.id]);
                      }
                      setEnableAllUpiApps(false);
                    }}
                    className={`p-3 rounded-xl border flex items-center justify-between cursor-pointer transition ${isSelected ? 'border-cyan-500/50 bg-cyan-500/10 text-cyan-400' : 'border-slate-800 bg-slate-950 text-slate-400'}`}
                  >
                    <span className="font-semibold text-sm">{app.name}</span>
                    <div className={`w-5 h-5 rounded-lg border flex items-center justify-center ${isSelected ? 'bg-cyan-500 border-cyan-400 text-slate-950' : 'border-slate-700 bg-slate-900'}`}>
                      {isSelected && <Check className="w-3.5 h-3.5 stroke-[3]" />}
                    </div>
                  </div>
                );
              })}
            </div>

            <button
              onClick={() => setShowUpiPicker(false)}
              className="w-full bg-cyan-500 hover:bg-cyan-400 text-slate-950 font-bold py-3 rounded-xl text-sm transition shadow-lg shadow-cyan-500/20"
            >
              Save Selection
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
