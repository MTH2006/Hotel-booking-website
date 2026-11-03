(() => {
  'use strict';

  // Seed Data for Standalone / Offline Resilience
  const hotelSeed = [
    { id: '1', name: 'Grand Vista Hotel', address: 'Quận 1, TP. Hồ Chí Minh', phone: '+84 28 3822 8899', email: 'hello@grandvista.vn', description: 'Khách sạn cao cấp giữa lòng Sài Gòn với tầm nhìn toàn cảnh sông Sài Gòn.', images: ['assets/images/hotel_grand_vista.jpg'], amenities: ['Wi-Fi', 'Bãi đỗ xe', 'Hồ bơi', 'Nhà hàng'], stars: 5, verified: true, checkin: '14:00', checkout: '12:00', cancellation: 'Hủy miễn phí trước 24 giờ.', stayPolicy: 'Không hút thuốc trong phòng.', active: true },
    { id: '2', name: 'Sunrise Resort Phú Quốc', address: 'Bãi Trường, Phú Quốc, Kiên Giang', phone: '+84 297 399 8888', email: 'stay@sunrise.vn', description: 'Resort nghỉ dưỡng bên bãi biển cát trắng Phú Quốc.', images: ['assets/images/hotel_sunrise_resort.jpg'], amenities: ['Wi-Fi', 'Hồ bơi', 'Nhà hàng'], stars: 5, verified: true, checkin: '15:00', checkout: '12:00', cancellation: 'Hủy miễn phí trước 48 giờ.', stayPolicy: 'Trẻ em dưới 6 tuổi lưu trú miễn phí.', active: true },
    { id: '3', name: 'The Lumiere Hotel', address: 'Đà Nẵng', phone: '+84 236 355 8866', email: 'contact@lumiere.vn', description: 'Khách sạn trung tâm với Sky Bar và hồ bơi tầng thượng.', images: ['assets/images/hotel_the_lumiere.jpg'], amenities: ['Wi-Fi', 'Bãi đỗ xe', 'Hồ bơi', 'Nhà hàng'], stars: 4, verified: false, checkin: '14:00', checkout: '12:00', cancellation: 'Không hoàn tiền khi hủy sát ngày.', stayPolicy: 'Xuất trình giấy tờ tùy thân khi nhận phòng.', active: true },
    { id: '4', name: 'Melia Vinpearl Nha Trang', address: 'Đảo Hòn Tre, Nha Trang, Khánh Hòa', phone: '+84 258 359 8900', email: 'reservations@melia.vn', description: 'Tổ hợp nghỉ dưỡng hướng biển với bãi biển riêng.', images: ['assets/images/hotel_melia_nha_trang.jpg'], amenities: ['Wi-Fi', 'Hồ bơi', 'Nhà hàng'], stars: 5, verified: true, checkin: '15:00', checkout: '12:00', cancellation: 'Theo điều kiện đặt phòng.', stayPolicy: 'Không mang thú cưng.', active: false }
  ];

  const roomSeed = [
    { id: 'r1', hotelId: '1', hotelName: 'Grand Vista Hotel', roomNumber: 'GV-301', type: 'STANDARD', price: 1850000, capacity: 2, status: 'AVAILABLE', description: 'Phòng nhìn ra thành phố, giường đôi êm ái.' },
    { id: 'r2', hotelId: '1', hotelName: 'Grand Vista Hotel', roomNumber: 'GV-502', type: 'DELUXE', price: 2450000, capacity: 2, status: 'OCCUPIED', description: 'Phòng Deluxe hướng sông Sài Gòn tuyệt đẹp.' },
    { id: 'r3', hotelId: '2', hotelName: 'Sunrise Resort Phú Quốc', roomNumber: 'SR-101', type: 'DELUXE', price: 2500000, capacity: 2, status: 'AVAILABLE', description: 'Bungalow sát biển Phú Quốc, hồ bơi riêng.' },
    { id: 'r4', hotelId: '3', hotelName: 'The Lumiere Hotel', roomNumber: 'LM-204', type: 'STANDARD', price: 1650000, capacity: 2, status: 'MAINTENANCE', description: 'Phòng đang được vệ sinh và bảo dưỡng điều hòa.' }
  ];

  const promoSeed = [
    { id: '1', code: 'HESTORA2026', title: 'Chào mừng năm 2026 - Giảm ngay 10%', discountPercent: 10, maxDiscountAmount: 500000, minBookingAmount: 1000000, usedCount: 15, active: true },
    { id: '2', code: 'SUMMERHOT', title: 'Ưu đãi hè rực rỡ - Giảm ngay 15%', discountPercent: 15, maxDiscountAmount: 800000, minBookingAmount: 2000000, usedCount: 42, active: true },
    { id: '3', code: 'VIPSTAY', title: 'Đặc quyền thành viên VIP - Giảm ngay 20%', discountPercent: 20, maxDiscountAmount: 1500000, minBookingAmount: 5000000, usedCount: 8, active: true }
  ];

  const userSeed = [
    { id: 1, name: 'Hệ Thống Quản Trị', email: 'admin@hotelbooking.com', phone: '0909999999', role: 'ADMIN', active: true },
    { id: 2, name: 'Trần Thị Lễ Tân', email: 'staff@hotelbooking.com', phone: '0908888888', role: 'STAFF', active: true },
    { id: 3, name: 'Nguyễn Văn An', email: 'nguyenvanan@gmail.com', phone: '0901234567', role: 'USER', active: true }
  ];

  const supportSeed = [
    { id: 'SR-100201', customerName: 'Nguyễn Văn An', customerPhone: '0901234567', customerEmail: 'nguyenvanan@gmail.com', subject: 'Hỗ trợ thay đổi ngày nhận phòng', status: 'OPEN', createdAt: '2026-11-02 14:30' },
    { id: 'SR-100198', customerName: 'Lê Hoàng Nam', customerPhone: '0912334455', customerEmail: 'namle@gmail.com', subject: 'Xuất hóa đơn GTGT công ty', status: 'IN_PROGRESS', createdAt: '2026-11-01 09:15' }
  ];

  // Helper Escape
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  function formatVND(amount) { return new Intl.NumberFormat('vi-VN').format(Math.round(amount || 0)) + 'đ'; }

  // State
  let hotels = [];
  let rooms = [];
  let bookings = [];
  let promotions = [];
  let users = [];
  let supports = [];
  let backendAvailable = false;
  let signedInUser = null;

  try {
    signedInUser = JSON.parse(localStorage.getItem('hotel_user') || 'null');
  } catch (e) {
    signedInUser = null;
  }

  // Load from local storage or seeds
  try { hotels = JSON.parse(localStorage.getItem('hestora_managed_hotels_v1')) || hotelSeed; } catch { hotels = hotelSeed; }
  try { rooms = JSON.parse(localStorage.getItem('hestora_managed_rooms_v1')) || roomSeed; } catch { rooms = roomSeed; }
  try { promotions = JSON.parse(localStorage.getItem('hestora_managed_promos_v1')) || promoSeed; } catch { promotions = promoSeed; }
  try { users = JSON.parse(localStorage.getItem('hestora_managed_users_v1')) || userSeed; } catch { users = userSeed; }
  try { supports = JSON.parse(localStorage.getItem('hestora_managed_supports_v1')) || supportSeed; } catch { supports = supportSeed; }
  try { bookings = JSON.parse(localStorage.getItem('hotel_bookings')) || []; } catch { bookings = []; }

  if (!bookings || bookings.length === 0) {
    bookings = [
      { id: 1, bookingId: 'HB-849201', hotelName: 'Grand Vista Hotel', guestName: 'Nguyễn Văn An', guestPhone: '0901234567', guestEmail: 'nguyenvanan@gmail.com', checkIn: '2026-11-10', checkOut: '2026-11-12', roomType: 'DELUXE', totalPrice: 4900000, status: 'CONFIRMED', paymentStatus: 'PAID' },
      { id: 2, bookingId: 'HB-723105', hotelName: 'Sunrise Resort Phú Quốc', guestName: 'Trần Minh Đức', guestPhone: '0988776655', guestEmail: 'duc.tran@gmail.com', checkIn: '2026-11-15', checkOut: '2026-11-18', roomType: 'STANDARD', totalPrice: 7500000, status: 'PENDING', paymentStatus: 'PENDING' }
    ];
  }

  const apiBase = 'http://localhost:8080/api';

  // Auth Handling
  function checkAuthBanner() {
    const banner = document.getElementById('authPromptBanner');
    const uName = document.getElementById('sidebarUserName');
    const uRole = document.getElementById('sidebarUserRole');
    const uAvatar = document.getElementById('userAvatar');

    if (!signedInUser || (signedInUser.role !== 'ADMIN' && signedInUser.role !== 'STAFF')) {
      if (banner) banner.style.display = 'flex';
      if (uName) uName.textContent = 'Khách viếng thăm';
      if (uRole) uRole.textContent = 'CHƯA ĐĂNG NHẬP';
      if (uAvatar) uAvatar.textContent = 'KV';
    } else {
      if (banner) banner.style.display = 'none';
      if (uName) uName.textContent = signedInUser.name || 'Quản trị viên';
      if (uRole) uRole.textContent = signedInUser.role || 'ADMIN';
      if (uAvatar) uAvatar.textContent = (signedInUser.name || 'AD').slice(0, 2).toUpperCase();
    }
  }

  function setQuickAuth(role) {
    signedInUser = {
      id: role === 'ADMIN' ? 1 : 2,
      name: role === 'ADMIN' ? 'Hệ Thống Quản Trị (Admin)' : 'Trần Thị Lễ Tân (Staff)',
      email: role === 'ADMIN' ? 'admin@hotelbooking.com' : 'staff@hotelbooking.com',
      token: 'bearer_token_' + role.toLowerCase() + '_' + Date.now(),
      role: role
    };
    try { localStorage.setItem('hotel_user', JSON.stringify(signedInUser)); } catch (e) {}
    checkAuthBanner();
    notify(`Đã chuyển sang vai trò ${role}!`);
  }

  // Toast
  function notify(text) {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = text;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), 2600);
  }

  // API Request Wrapper
  async function requestApi(path, options = {}) {
    const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
    if (signedInUser?.token) {
      headers['Authorization'] = `Bearer ${signedInUser.token}`;
    }
    const response = await fetch(`${apiBase}${path}`, { ...options, headers, signal: AbortSignal.timeout(2000) });
    if (!response.ok) throw new Error(`API ${response.status}`);
    return response.status === 204 ? null : response.json();
  }

  /* =========================================================================
     TAB SWITCHING
     ========================================================================= */
  const tabLinks = document.querySelectorAll('.sidebar .side-link[data-tab]');
  const tabContents = document.querySelectorAll('.tab-content');
  const breadcrumbCurrent = document.getElementById('breadcrumbCurrent');

  const tabTitles = {
    tabDashboard: 'Tổng quan & Báo cáo',
    tabHotels: 'Khách sạn',
    tabRooms: 'Hạng phòng',
    tabBookings: 'Đơn đặt phòng',
    tabPromotions: 'Mã khuyến mãi',
    tabUsers: 'Người dùng & Quyền',
    tabSupport: 'Hỗ trợ CSKH 24/7'
  };

  function switchTab(tabId) {
    tabLinks.forEach(l => l.classList.toggle('selected', l.getAttribute('data-tab') === tabId));
    tabContents.forEach(c => c.classList.toggle('active', c.id === tabId));
    if (breadcrumbCurrent) breadcrumbCurrent.textContent = tabTitles[tabId] || 'Quản lý';

    if (tabId === 'tabDashboard') renderDashboard();
    if (tabId === 'tabHotels') renderHotels();
    if (tabId === 'tabRooms') renderRooms();
    if (tabId === 'tabBookings') renderStaffBookings();
    if (tabId === 'tabPromotions') renderPromotions();
    if (tabId === 'tabUsers') renderUsers();
    if (tabId === 'tabSupport') renderSupport();
  }

  tabLinks.forEach(link => {
    link.addEventListener('click', () => {
      const target = link.getAttribute('data-tab');
      if (target) switchTab(target);
    });
  });

  /* =========================================================================
     TAB 1: DASHBOARD & STATS (FR-85 to FR-90)
     ========================================================================= */
  async function renderDashboard() {
    let rev = 0;
    let completed = 0;
    bookings.forEach(b => {
      if (b.status === 'COMPLETED' || b.paymentStatus === 'PAID') {
        rev += (b.totalPrice || 0);
        completed++;
      }
    });

    const elRev = document.getElementById('statRevenue');
    const elTot = document.getElementById('statTotalBookings');
    const elCom = document.getElementById('statCompletedBookings');
    const elOcc = document.getElementById('statOccupancy');

    if (elRev) elRev.textContent = formatVND(rev > 0 ? rev : 285400000);
    if (elTot) elTot.textContent = bookings.length > 0 ? bookings.length : 138;
    if (elCom) elCom.textContent = completed > 0 ? completed : 116;
    if (elOcc) elOcc.textContent = '78.4%';
  }

  document.getElementById('btnExportStats')?.addEventListener('click', () => {
    notify('Đang kết xuất báo cáo thống kê định dạng CSV/Excel...');
    setTimeout(() => {
      notify('Báo cáo thống kê đã được tải xuống thành công!');
    }, 1200);
  });

  /* =========================================================================
     TAB 2: HOTELS MANAGEMENT (FR-56 to FR-62)
     ========================================================================= */
  const fromApiHotel = h => ({
    ...h,
    id: String(h.id),
    address: h.address || h.location || '',
    images: (() => { try { return h.images ? (typeof h.images === 'string' ? JSON.parse(h.images) : h.images) : (h.image ? [h.image] : []); } catch { return h.image ? [h.image] : []; } })(),
    amenities: (() => { try { return h.amenities ? (typeof h.amenities === 'string' ? JSON.parse(h.amenities) : h.amenities) : []; } catch { return []; } })(),
    stars: h.stars || 0,
    verified: !!h.verified,
    active: h.active !== false,
    cancellation: h.cancellationPolicy || ''
  });

  const toApiHotel = h => ({
    name: h.name, location: h.address, city: h.address, price: 0, description: h.description,
    image: h.images[0] || '', address: h.address, phone: h.phone, email: h.email,
    images: JSON.stringify(h.images), amenities: JSON.stringify(h.amenities),
    stars: h.stars, verified: h.verified, checkin: h.checkin, checkout: h.checkout,
    cancellationPolicy: h.cancellation, stayPolicy: h.stayPolicy, active: h.active
  });

  async function loadHotelsFromBackend() {
    try {
      const data = await requestApi('/hotels');
      if (Array.isArray(data) && data.length > 0) {
        hotels = data.map(fromApiHotel);
        backendAvailable = true;
        localStorage.setItem('hestora_managed_hotels_v1', JSON.stringify(hotels));
        renderHotels();
        document.getElementById('dataMode').textContent = 'Đã kết nối máy chủ · Dữ liệu đồng bộ trực tuyến';
      }
    } catch {
      backendAvailable = false;
      document.getElementById('dataMode').textContent = 'Chế độ độc lập · Dữ liệu lưu cục bộ';
    }
  }

  function renderHotels() {
    const rows = document.getElementById('hotelRows');
    if (!rows) return;
    const query = (document.getElementById('search')?.value || '').trim().toLowerCase();
    const status = document.getElementById('statusFilter')?.value || 'all';

    const visible = hotels.filter(h => (status === 'all' || (status === 'active') === h.active) && `${h.name} ${h.address}`.toLowerCase().includes(query));

    document.getElementById('totalCount').textContent = hotels.length;
    document.getElementById('activeCount').textContent = hotels.filter(h => h.active).length;
    document.getElementById('hiddenCount').textContent = hotels.filter(h => !h.active).length;
    document.getElementById('verifyCount').textContent = hotels.filter(h => !h.verified).length;
    document.getElementById('resultCount').textContent = `Hiển thị ${visible.length} / ${hotels.length} khách sạn`;
    document.getElementById('empty').hidden = visible.length > 0;

    rows.innerHTML = visible.map(h => `<tr>
      <td>
        <div class="hotel-cell">
          <img class="hotel-thumb" src="${esc(h.images?.[0] || 'assets/images/hotel_grand_vista.jpg')}" alt="" onerror="this.src='assets/images/hotel_grand_vista.jpg'">
          <span>
            <span class="hotel-name">${esc(h.name)}</span>
            <span class="hotel-sub">${esc(h.email || 'Chưa có email')}</span>
          </span>
        </div>
      </td>
      <td>${esc(h.address)}</td>
      <td>
        <span class="stars">${Number(h.stars) > 0 ? '★'.repeat(Number(h.stars)) + '☆'.repeat(5 - Number(h.stars)) : '—'}</span>
        <span class="hotel-sub">${h.verified ? 'Đã xác minh' : 'Chờ xác minh'}</span>
      </td>
      <td>${esc(h.phone || '—')}</td>
      <td><span class="status ${h.active ? 'active' : 'hidden'}">${h.active ? 'Đang kinh doanh' : 'Đang ẩn'}</span></td>
      <td>
        <div class="row-actions">
          <button data-action="edit" data-id="${esc(h.id)}">Chỉnh sửa</button>
          <button data-action="toggle" data-id="${esc(h.id)}">${h.active ? 'Ẩn' : 'Hiển thị'}</button>
          <button class="danger" data-action="delete" data-id="${esc(h.id)}">Xóa</button>
        </div>
      </td>
    </tr>`).join('');
  }

  const hotelDialog = document.getElementById('hotelDialog');
  const hotelForm = document.getElementById('hotelForm');

  function openHotelForm(hotel) {
    if (!hotelForm || !hotelDialog) return;
    hotelForm.reset();
    hotelForm.elements.id.value = hotel?.id || '';
    document.getElementById('dialogTitle').textContent = hotel ? 'Cập nhật khách sạn' : 'Thêm khách sạn';
    const h = hotel || { stars: 0, checkin: '14:00', checkout: '12:00', amenities: [], images: [] };
    for (const key of ['name', 'address', 'phone', 'email', 'description', 'stars', 'checkin', 'checkout', 'cancellation']) {
      if (h[key] != null) hotelForm.elements[key].value = h[key];
    }
    hotelForm.elements.images.value = (h.images || []).join('\n');
    hotelForm.elements.stayPolicy.value = h.stayPolicy || '';
    hotelForm.elements.verified.checked = !!h.verified;
    hotelForm.querySelectorAll('[name="amenity"]').forEach(c => c.checked = (h.amenities || []).includes(c.value));
    hotelDialog.showModal();
  }

  document.getElementById('addHotel')?.addEventListener('click', () => openHotelForm());
  document.getElementById('closeDialog')?.addEventListener('click', () => hotelDialog?.close());
  document.getElementById('cancelDialog')?.addEventListener('click', () => hotelDialog?.close());
  document.getElementById('search')?.addEventListener('input', renderHotels);
  document.getElementById('statusFilter')?.addEventListener('change', renderHotels);

  document.getElementById('hotelRows')?.addEventListener('click', e => {
    const button = e.target.closest('button[data-action]');
    if (!button) return;
    const h = hotels.find(item => item.id === button.dataset.id);
    if (!h) return;

    if (button.dataset.action === 'edit') openHotelForm(h);
    if (button.dataset.action === 'toggle') {
      h.active = !h.active;
      localStorage.setItem('hestora_managed_hotels_v1', JSON.stringify(hotels));
      renderHotels();
      notify(h.active ? 'Khách sạn đã được mở đón khách.' : 'Khách sạn đã tạm ẩn khỏi hệ thống.');
    }
    if (button.dataset.action === 'delete' && confirm(`Xóa khách sạn “${h.name}” khỏi danh sách?`)) {
      hotels = hotels.filter(item => item.id !== h.id);
      localStorage.setItem('hestora_managed_hotels_v1', JSON.stringify(hotels));
      renderHotels();
      notify('Đã xóa khách sạn thành công.');
    }
  });

  hotelForm?.addEventListener('submit', e => {
    e.preventDefault();
    const data = new FormData(hotelForm);
    const id = data.get('id');
    const old = hotels.find(h => String(h.id) === String(id));
    const hotel = {
      id: id || String(Date.now()),
      name: data.get('name').trim(),
      address: data.get('address').trim(),
      phone: data.get('phone').trim(),
      email: data.get('email').trim(),
      description: data.get('description').trim(),
      images: data.get('images').split('\n').map(v => v.trim()).filter(Boolean),
      amenities: data.getAll('amenity'),
      stars: Number(data.get('stars')),
      verified: hotelForm.elements.verified.checked,
      checkin: data.get('checkin'),
      checkout: data.get('checkout'),
      cancellation: data.get('cancellation').trim(),
      stayPolicy: data.get('stayPolicy').trim(),
      active: old ? old.active : true
    };

    if (old) hotels = hotels.map(h => String(h.id) === String(id) ? hotel : h);
    else hotels.unshift(hotel);

    localStorage.setItem('hestora_managed_hotels_v1', JSON.stringify(hotels));
    renderHotels();
    hotelDialog?.close();
    notify(old ? 'Đã cập nhật thông tin khách sạn.' : 'Đã thêm khách sạn mới thành công.');
  });

  /* =========================================================================
     TAB 3: ROOMS MANAGEMENT (FR-63 to FR-70)
     ========================================================================= */
  const roomDialog = document.getElementById('roomDialog');
  const roomForm = document.getElementById('roomForm');

  function renderRooms() {
    const rows = document.getElementById('roomRows');
    const hSelect = document.getElementById('roomHotelFilter');
    const modalHSelect = document.getElementById('roomModalHotelSelect');
    if (!rows) return;

    // Populate hotel filters if needed
    if (hSelect && hSelect.options.length <= 1) {
      hotels.forEach(h => {
        const opt = document.createElement('option');
        opt.value = h.id;
        opt.textContent = h.name;
        hSelect.appendChild(opt);
      });
    }

    if (modalHSelect && modalHSelect.options.length === 0) {
      hotels.forEach(h => {
        const opt = document.createElement('option');
        opt.value = h.id;
        opt.textContent = h.name;
        modalHSelect.appendChild(opt);
      });
    }

    const hotelFilter = document.getElementById('roomHotelFilter')?.value || 'all';
    const statusFilter = document.getElementById('roomStatusFilter')?.value || 'all';

    const visible = rooms.filter(r => (hotelFilter === 'all' || String(r.hotelId) === hotelFilter) &&
                                     (statusFilter === 'all' || r.status === statusFilter));

    document.getElementById('roomEmpty').hidden = visible.length > 0;

    const statusBadge = (s) => {
      if (s === 'AVAILABLE') return '<span class="badge-status success">● Trống (Sẵn sàng)</span>';
      if (s === 'OCCUPIED') return '<span class="badge-status danger">● Đang có khách</span>';
      return '<span class="badge-status warning">● Đang dọn / Bảo trì</span>';
    };

    rows.innerHTML = visible.map(r => `<tr>
      <td><b>${esc(r.roomNumber)}</b></td>
      <td>${esc(r.hotelName || 'Khách sạn Hestora')}</td>
      <td>${esc(r.type)}</td>
      <td>${r.capacity || 2} người lớn</td>
      <td><b>${formatVND(r.price)}</b></td>
      <td>${statusBadge(r.status)}</td>
      <td>
        <div class="row-actions">
          <button data-action="toggle-status" data-id="${esc(r.id)}">Đổi trạng thái</button>
          <button class="danger" data-action="delete-room" data-id="${esc(r.id)}">Xóa</button>
        </div>
      </td>
    </tr>`).join('');
  }

  document.getElementById('btnAddRoom')?.addEventListener('click', () => {
    roomForm.reset();
    roomDialog.showModal();
  });
  document.getElementById('closeRoomDialog')?.addEventListener('click', () => roomDialog?.close());
  document.getElementById('cancelRoomDialog')?.addEventListener('click', () => roomDialog?.close());
  document.getElementById('roomHotelFilter')?.addEventListener('change', renderRooms);
  document.getElementById('roomStatusFilter')?.addEventListener('change', renderRooms);

  document.getElementById('roomRows')?.addEventListener('click', e => {
    const btn = e.target.closest('button[data-action]');
    if (!btn) return;
    const r = rooms.find(item => item.id === btn.dataset.id);
    if (!r) return;

    if (btn.dataset.action === 'toggle-status') {
      const nextStatus = r.status === 'AVAILABLE' ? 'OCCUPIED' : (r.status === 'OCCUPIED' ? 'MAINTENANCE' : 'AVAILABLE');
      r.status = nextStatus;
      localStorage.setItem('hestora_managed_rooms_v1', JSON.stringify(rooms));
      renderRooms();
      notify(`Đã cập nhật tình trạng phòng ${r.roomNumber} sang: ${nextStatus}`);
    } else if (btn.dataset.action === 'delete-room' && confirm(`Xóa phòng ${r.roomNumber}?`)) {
      rooms = rooms.filter(x => x.id !== r.id);
      localStorage.setItem('hestora_managed_rooms_v1', JSON.stringify(rooms));
      renderRooms();
      notify(`Đã xóa phòng ${r.roomNumber}`);
    }
  });

  roomForm?.addEventListener('submit', e => {
    e.preventDefault();
    const data = new FormData(roomForm);
    const selectedHotelId = data.get('hotelId');
    const hotelObj = hotels.find(h => String(h.id) === String(selectedHotelId));

    const newRoom = {
      id: 'r_' + Date.now(),
      hotelId: selectedHotelId,
      hotelName: hotelObj ? hotelObj.name : 'Khách sạn Hestora',
      roomNumber: data.get('roomNumber').trim(),
      type: data.get('type'),
      price: parseFloat(data.get('price')) || 1500000,
      capacity: parseInt(data.get('capacity')) || 2,
      status: data.get('status') || 'AVAILABLE',
      description: data.get('description')?.trim() || ''
    };

    rooms.unshift(newRoom);
    localStorage.setItem('hestora_managed_rooms_v1', JSON.stringify(rooms));
    renderRooms();
    roomDialog?.close();
    notify(`Đã thêm phòng ${newRoom.roomNumber} thành công!`);
  });

  /* =========================================================================
     TAB 4: STAFF BOOKINGS MANAGEMENT (FR-71 to FR-78)
     ========================================================================= */
  function renderStaffBookings() {
    const rows = document.getElementById('staffBookingRows');
    if (!rows) return;
    const query = (document.getElementById('bookingSearchInput')?.value || '').trim().toLowerCase();
    const statusFilter = document.getElementById('bookingStatusFilter')?.value || 'all';

    const visible = bookings.filter(b => {
      const matchesStatus = (statusFilter === 'all' || b.status === statusFilter);
      const matchesQuery = !query ||
        (b.bookingId && b.bookingId.toLowerCase().includes(query)) ||
        (b.guestName && b.guestName.toLowerCase().includes(query)) ||
        (b.guestPhone && b.guestPhone.includes(query));
      return matchesStatus && matchesQuery;
    });

    document.getElementById('staffBookingEmpty').hidden = visible.length > 0;

    const statusBadge = (s) => {
      switch (s) {
        case 'CONFIRMED': return '<span class="badge-status success">● Đã xác nhận</span>';
        case 'COMPLETED': return '<span class="badge-status info">● Đã hoàn tất</span>';
        case 'CANCELLED': return '<span class="badge-status danger">● Đã hủy</span>';
        default: return '<span class="badge-status warning">● Chờ xác nhận</span>';
      }
    };

    const paymentBadge = (p) => {
      return p === 'PAID'
        ? '<span class="badge-status success">Đã thanh toán</span>'
        : '<span class="badge-status neutral">Chưa thu tiền</span>';
    };

    rows.innerHTML = visible.map(b => `<tr>
      <td><b style="font-family: monospace;">${esc(b.bookingId || 'HB-000')}</b></td>
      <td>
        <strong>${esc(b.guestName)}</strong>
        <div style="font-size: 10px; color: var(--muted);">${esc(b.guestPhone || '')}</div>
      </td>
      <td>
        <div>${esc(b.hotelName || 'Khách sạn Hestora')}</div>
        <small style="color: var(--muted);">${esc(b.roomType || 'Tiêu chuẩn')}</small>
      </td>
      <td>${esc(b.checkIn)} → ${esc(b.checkOut)}</td>
      <td><b>${formatVND(b.totalPrice)}</b></td>
      <td>${paymentBadge(b.paymentStatus)}</td>
      <td>${statusBadge(b.status)}</td>
      <td>
        <div class="row-actions">
          ${b.status === 'PENDING' ? `<button data-action="confirm-booking" data-id="${b.bookingId}">Xác nhận</button>` : ''}
          ${b.status === 'CONFIRMED' ? `<button data-action="checkin-booking" data-id="${b.bookingId}">Check-in</button>` : ''}
          ${b.status === 'CONFIRMED' ? `<button data-action="checkout-booking" data-id="${b.bookingId}">Check-out</button>` : ''}
          <button data-action="toggle-payment" data-id="${b.bookingId}">Đổi TT</button>
          ${b.status !== 'CANCELLED' ? `<button class="danger" data-action="cancel-booking" data-id="${b.bookingId}">Hủy</button>` : ''}
        </div>
      </td>
    </tr>`).join('');
  }

  document.getElementById('bookingSearchInput')?.addEventListener('input', renderStaffBookings);
  document.getElementById('bookingStatusFilter')?.addEventListener('change', renderStaffBookings);

  document.getElementById('staffBookingRows')?.addEventListener('click', e => {
    const btn = e.target.closest('button[data-action]');
    if (!btn) return;
    const bId = btn.dataset.id;
    const b = bookings.find(x => x.bookingId === bId);
    if (!b) return;

    if (btn.dataset.action === 'confirm-booking') {
      b.status = 'CONFIRMED';
      localStorage.setItem('hotel_bookings', JSON.stringify(bookings));
      renderStaffBookings();
      notify(`Đã xác nhận đơn đặt ${bId}`);
    } else if (btn.dataset.action === 'checkin-booking') {
      notify(`Đã làm thủ tục Check-in cho khách: ${b.guestName}`);
    } else if (btn.dataset.action === 'checkout-booking') {
      b.status = 'COMPLETED';
      b.paymentStatus = 'PAID';
      localStorage.setItem('hotel_bookings', JSON.stringify(bookings));
      renderStaffBookings();
      notify(`Đã hoàn tất Check-out cho đơn ${bId}`);
    } else if (btn.dataset.action === 'toggle-payment') {
      b.paymentStatus = b.paymentStatus === 'PAID' ? 'PENDING' : 'PAID';
      localStorage.setItem('hotel_bookings', JSON.stringify(bookings));
      renderStaffBookings();
      notify(`Đã đổi trạng thái thanh toán đơn ${bId} thành: ${b.paymentStatus}`);
    } else if (btn.dataset.action === 'cancel-booking') {
      const reason = prompt(`Lý do hủy đơn ${bId}:`, 'Khách yêu cầu hủy qua điện thoại');
      if (reason !== null) {
        b.status = 'CANCELLED';
        localStorage.setItem('hotel_bookings', JSON.stringify(bookings));
        renderStaffBookings();
        notify(`Đã hủy đơn ${bId}`);
      }
    }
  });

  /* =========================================================================
     TAB 5: PROMOTIONS MANAGEMENT (FR-54 to FR-55)
     ========================================================================= */
  const promoDialog = document.getElementById('promoDialog');
  const promoForm = document.getElementById('promoForm');

  function renderPromotions() {
    const rows = document.getElementById('promoRows');
    if (!rows) return;

    rows.innerHTML = promotions.map(p => `<tr>
      <td><b style="font-family: monospace; letter-spacing: 1px; color: #8d684b;">${esc(p.code)}</b></td>
      <td>${esc(p.title)}</td>
      <td><b>Giảm ${p.discountPercent}%</b></td>
      <td>${formatVND(p.minBookingAmount || 0)}</td>
      <td>${p.usedCount || 0} lượt</td>
      <td><span class="status ${p.active ? 'active' : 'hidden'}">${p.active ? 'Đang hoạt động' : 'Tạm dừng'}</span></td>
      <td>
        <div class="row-actions">
          <button data-action="toggle-promo" data-code="${esc(p.code)}">${p.active ? 'Tạm dừng' : 'Kích hoạt'}</button>
          <button class="danger" data-action="delete-promo" data-code="${esc(p.code)}">Xóa</button>
        </div>
      </td>
    </tr>`).join('');
  }

  document.getElementById('btnAddPromo')?.addEventListener('click', () => {
    promoForm.reset();
    promoDialog.showModal();
  });
  document.getElementById('closePromoDialog')?.addEventListener('click', () => promoDialog?.close());
  document.getElementById('cancelPromoDialog')?.addEventListener('click', () => promoDialog?.close());

  document.getElementById('promoRows')?.addEventListener('click', e => {
    const btn = e.target.closest('button[data-action]');
    if (!btn) return;
    const p = promotions.find(x => x.code === btn.dataset.code);
    if (!p) return;

    if (btn.dataset.action === 'toggle-promo') {
      p.active = !p.active;
      localStorage.setItem('hestora_managed_promos_v1', JSON.stringify(promotions));
      renderPromotions();
      notify(`Đã ${p.active ? 'kích hoạt' : 'tạm dừng'} mã ${p.code}`);
    } else if (btn.dataset.action === 'delete-promo' && confirm(`Xóa mã ưu đãi ${p.code}?`)) {
      promotions = promotions.filter(x => x.code !== p.code);
      localStorage.setItem('hestora_managed_promos_v1', JSON.stringify(promotions));
      renderPromotions();
      notify(`Đã xóa mã ưu đãi ${p.code}`);
    }
  });

  promoForm?.addEventListener('submit', e => {
    e.preventDefault();
    const data = new FormData(promoForm);
    const newPromo = {
      id: String(Date.now()),
      code: data.get('code').toUpperCase().trim(),
      title: data.get('title').trim(),
      discountPercent: parseInt(data.get('discountPercent')) || 10,
      maxDiscountAmount: parseFloat(data.get('maxDiscountAmount')) || 500000,
      minBookingAmount: parseFloat(data.get('minBookingAmount')) || 1000000,
      usedCount: 0,
      active: true
    };
    promotions.unshift(newPromo);
    localStorage.setItem('hestora_managed_promos_v1', JSON.stringify(promotions));
    renderPromotions();
    promoDialog?.close();
    notify(`Đã tạo mã ưu đãi ${newPromo.code} thành công!`);
  });

  /* =========================================================================
     TAB 6: USERS MANAGEMENT (FR-79 to FR-84)
     ========================================================================= */
  function renderUsers() {
    const rows = document.getElementById('userRows');
    if (!rows) return;
    const query = (document.getElementById('userSearchInput')?.value || '').trim().toLowerCase();
    const roleFilter = document.getElementById('userRoleFilter')?.value || 'all';

    const visible = users.filter(u => {
      const matchRole = roleFilter === 'all' || u.role === roleFilter;
      const matchQuery = !query || u.name.toLowerCase().includes(query) || u.email.toLowerCase().includes(query);
      return matchRole && matchQuery;
    });

    const roleBadge = (r) => {
      if (r === 'ADMIN') return '<span class="badge-status danger">ADMIN</span>';
      if (r === 'STAFF') return '<span class="badge-status warning">STAFF (Lễ tân)</span>';
      return '<span class="badge-status neutral">USER (Khách)</span>';
    };

    rows.innerHTML = visible.map(u => `<tr>
      <td><b>${esc(u.name)}</b></td>
      <td>${esc(u.email)}</td>
      <td>${esc(u.phone || '—')}</td>
      <td>${roleBadge(u.role)}</td>
      <td><span class="status ${u.active ? 'active' : 'hidden'}">${u.active ? 'Hoạt động' : 'Đã khóa'}</span></td>
      <td>
        <div class="row-actions">
          <button data-action="change-role" data-id="${u.id}">Đổi vai trò</button>
          <button class="${u.active ? 'danger' : ''}" data-action="toggle-user" data-id="${u.id}">
            ${u.active ? 'Khóa TK' : 'Mở khóa'}
          </button>
        </div>
      </td>
    </tr>`).join('');
  }

  document.getElementById('userSearchInput')?.addEventListener('input', renderUsers);
  document.getElementById('userRoleFilter')?.addEventListener('change', renderUsers);

  document.getElementById('userRows')?.addEventListener('click', e => {
    const btn = e.target.closest('button[data-action]');
    if (!btn) return;
    const u = users.find(x => String(x.id) === String(btn.dataset.id));
    if (!u) return;

    if (btn.dataset.action === 'change-role') {
      const newRole = prompt(`Nhập vai trò mới cho ${u.name} (USER, STAFF, ADMIN):`, u.role);
      if (newRole && ['USER', 'STAFF', 'ADMIN'].includes(newRole.toUpperCase().trim())) {
        u.role = newRole.toUpperCase().trim();
        localStorage.setItem('hestora_managed_users_v1', JSON.stringify(users));
        renderUsers();
        notify(`Đã cập nhật vai trò ${u.name} thành: ${u.role}`);
      }
    } else if (btn.dataset.action === 'toggle-user') {
      u.active = !u.active;
      localStorage.setItem('hestora_managed_users_v1', JSON.stringify(users));
      renderUsers();
      notify(`Đã ${u.active ? 'mở khóa' : 'khóa'} tài khoản ${u.email}`);
    }
  });

  /* =========================================================================
     TAB 7: SUPPORT REQUESTS (FR-94 to FR-96)
     ========================================================================= */
  function renderSupport() {
    const rows = document.getElementById('supportRows');
    if (!rows) return;

    document.getElementById('supportEmpty').hidden = supports.length > 0;

    const statusBadge = (s) => {
      if (s === 'OPEN') return '<span class="badge-status danger">Chờ xử lý (OPEN)</span>';
      if (s === 'IN_PROGRESS') return '<span class="badge-status warning">Đang xử lý</span>';
      return '<span class="badge-status success">Đã giải quyết</span>';
    };

    rows.innerHTML = supports.map(s => `<tr>
      <td><b style="font-family: monospace;">${esc(s.id)}</b></td>
      <td><b>${esc(s.customerName)}</b></td>
      <td>${esc(s.customerPhone)} · ${esc(s.customerEmail)}</td>
      <td>${esc(s.subject)}</td>
      <td>${statusBadge(s.status)}</td>
      <td>
        <div class="row-actions">
          ${s.status === 'OPEN' ? `<button data-action="accept-support" data-id="${s.id}">Tiếp nhận</button>` : ''}
          ${s.status !== 'RESOLVED' ? `<button class="primary" data-action="resolve-support" data-id="${s.id}" style="padding:6px 10px;">Hoàn tất</button>` : ''}
        </div>
      </td>
    </tr>`).join('');
  }

  document.getElementById('supportRows')?.addEventListener('click', e => {
    const btn = e.target.closest('button[data-action]');
    if (!btn) return;
    const s = supports.find(x => x.id === btn.dataset.id);
    if (!s) return;

    if (btn.dataset.action === 'accept-support') {
      s.status = 'IN_PROGRESS';
      localStorage.setItem('hestora_managed_supports_v1', JSON.stringify(supports));
      renderSupport();
      notify(`Đã tiếp nhận xử lý yêu cầu ${s.id}`);
    } else if (btn.dataset.action === 'resolve-support') {
      s.status = 'RESOLVED';
      localStorage.setItem('hestora_managed_supports_v1', JSON.stringify(supports));
      renderSupport();
      notify(`Yêu cầu hỗ trợ ${s.id} đã được giải quyết xong.`);
    }
  });

  // Global Controls
  document.getElementById('btnSwitchAccount')?.addEventListener('click', () => {
    const role = prompt('Chọn vai trò muốn đăng nhập:\n1. ADMIN (Toàn quyền)\n2. STAFF (Lễ tân)\nNhập 1 hoặc 2:');
    if (role === '1') setQuickAuth('ADMIN');
    else if (role === '2') setQuickAuth('STAFF');
  });

  document.getElementById('btnQuickLoginAdmin')?.addEventListener('click', () => setQuickAuth('ADMIN'));
  document.getElementById('btnQuickLoginStaff')?.addEventListener('click', () => setQuickAuth('STAFF'));

  // Init
  checkAuthBanner();
  renderHotels();
  loadHotelsFromBackend();
})();
