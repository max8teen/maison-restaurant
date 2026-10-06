// ─── admin.js ─── shared JS for all admin pages ───────────────────────────────

var API = 'api';

// ─── Auth check ───────────────────────────────────────────────────────────────
var _user = JSON.parse(localStorage.getItem('maison_user') || 'null');
var _token = localStorage.getItem('maison_token') || '';

if (!_token || !_user || _user.role !== 'admin') {
  window.location.href = '../login.html';
  throw new Error('Not authenticated');
}

// Show admin name in sidebar
document.addEventListener('DOMContentLoaded', () => {
  const nameEl = document.getElementById('admin-name');
  if (nameEl) nameEl.textContent = (_user && _user.full_name) ? _user.full_name : 'Admin';
});

// ─── API helper ───────────────────────────────────────────────────────────────
async function apiCall(method, endpoint, body = null) {
  const options = {
    method,
    headers: {
      'Content-Type': 'application/json'
    }
  };
  var bodyWithToken = body ? Object.assign({}, body, {_token: _token}) : {_token: _token};
  options.body = JSON.stringify(bodyWithToken);
  try {
    const res  = await fetch(`${API}/${endpoint}?t=${encodeURIComponent(_token)}`, options);
    const data = await res.json();
    return data;
  } catch(e) {
    return { success: false, message: 'Network error. Please try again.' };
  }
}

// ─── Sidebar toggle ───────────────────────────────────────────────────────────
function toggleSidebar() {
  document.getElementById('sidebar').classList.toggle('mobile-open');
  document.getElementById('sidebar-overlay').classList.toggle('show');
}
function closeSidebar() {
  document.getElementById('sidebar').classList.remove('mobile-open');
  document.getElementById('sidebar-overlay').classList.remove('show');
}

// ─── Logout ───────────────────────────────────────────────────────────────────
function doLogout() {
  localStorage.removeItem('maison_user');
  localStorage.removeItem('maison_token');
  window.location.href = '../login.html';
}

// ─── Toast ────────────────────────────────────────────────────────────────────
function toast(msg, type = 'success') {
  const c = document.getElementById('toast-container');
  const t = document.createElement('div');
  t.className = 'toast ' + type;
  t.textContent = msg;
  c.appendChild(t);
  setTimeout(() => {
    t.style.opacity = '0';
    t.style.transform = 'translateY(10px)';
    t.style.transition = 'all .3s';
    setTimeout(() => t.remove(), 300);
  }, 2800);
}

// ─── Modal helpers ────────────────────────────────────────────────────────────
function openModal(id)  { document.getElementById(id).classList.add('show'); }
function closeModal(id) { document.getElementById(id).classList.remove('show'); }
document.addEventListener('keydown', e => {
  if (e.key === 'Escape')
    document.querySelectorAll('.modal-bg.show').forEach(m => m.classList.remove('show'));
});

// ─── Status helpers ───────────────────────────────────────────────────────────
const STATUS_PILL = {
  confirmed: 'pill-green', pending: 'pill-amber', seated: 'pill-blue',
  completed: 'pill-purple', cancelled: 'pill-red', no_show: 'pill-muted'
};
const STATUS_COLORS = {
  confirmed: 'var(--green)', pending: 'var(--amber)', seated: 'var(--blue)',
  completed: 'var(--purple)', cancelled: 'var(--red)', no_show: 'var(--muted2)'
};
const TABLE_COLORS = {
  available: 'var(--green)', occupied: 'var(--blue)',
  reserved: 'var(--amber)', maintenance: 'var(--red)'
};
const STATUSES = ['confirmed','pending','seated','completed','cancelled','no_show'];
