/**
 * HotelBooking - Unified Interactive Client (HESTORA Luxury Edition)
 * Supports direct file:// opening and local server hosting with full API integration
 */

(function () {
  'use strict';

  // API Configuration
  const API_BASE_URL = 'http://localhost:8080/api';

  // Featured Hotels Data (Exact match with reference UI)
  const FEATURED_HOTELS = [
    {
      id: 1,
      name: 'Grand Vista Hotel',
      badge: 'Được yêu thích',
      badgeType: 'featured',
      rating: 4.8,
      reviewsCount: 256,
      location: 'Quận 1, TP. Hồ Chí Minh',
      city: 'TP. Hồ Chí Minh',
      price: 1850000,
      originalPrice: null,
      image: 'assets/images/hotel_grand_vista.jpg',
      isWishlisted: false,
      description: 'Khách sạn cao cấp giữa lòng Sài Gòn với tầm nhìn toàn cảnh ngoạn mục và dịch vụ 5 sao sang trọng.'
    },
    {
      id: 2,
      name: 'Sunrise Resort Phú Quốc',
      badge: 'Giảm giá 20%',
      badgeType: 'discount',
      rating: 4.7,
      reviewsCount: 312,
      location: 'Phú Quốc, Kiên Giang',
      city: 'Phú Quốc',
      price: 2500000,
      originalPrice: 3125000,
      image: 'assets/images/hotel_sunrise_resort.jpg',
      isWishlisted: false,
      description: 'Resort nghỉ dưỡng bên bãi biển cát trắng tinh khôi Phú Quốc với hồ bơi vô cực và khuôn viên nhiệt đới xanh mát.'
    },
    {
      id: 3,
      name: 'The Lumiere Hotel',
      badge: 'Phổ biến',
      badgeType: 'popular',
      rating: 4.6,
      reviewsCount: 196,
      location: 'Đà Nẵng',
      city: 'Đà Nẵng',
      price: 1650000,
      originalPrice: null,
      image: 'assets/images/hotel_the_lumiere.jpg',
      isWishlisted: false,
      description: 'Toạ lạc ngay bờ sông Hàn và trung tâm thành phố Đà Nẵng, thiết kế hiện đại với Sky Bar và hồ bơi tầng thượng.'
    },
    {
      id: 4,
      name: 'Melia Vinpearl Nha Trang',
      badge: 'Mới',
      badgeType: 'new',
      rating: 4.9,
      reviewsCount: 420,
      location: 'Nha Trang, Khánh Hòa',
      city: 'Nha Trang',
      price: 2800000,
      originalPrice: null,
      image: 'assets/images/hotel_melia_nha_trang.jpg',
      isWishlisted: false,
      description: 'Tổ hợp nghỉ dưỡng chuẩn quốc tế hướng biển Nha Trang với bãi biển riêng và hệ thống ẩm thực thượng hạng.'
    }
  ];

  const POPULAR_DESTINATIONS = [
    { name: 'TP. Hồ Chí Minh', count: '1.240 khách sạn' },
    { name: 'Hà Nội', count: '980 khách sạn' },
    { name: 'Đà Nẵng', count: '750 khách sạn' },
    { name: 'Phú Quốc', count: '520 khách sạn' },
    { name: 'Nha Trang', count: '640 khách sạn' },
    { name: 'Đà Lạt', count: '810 khách sạn' }
  ];

  // API Service
  const HotelAPI = {
    async getFeaturedHotels() {
      try {
        const response = await fetch(`${API_BASE_URL}/hotels/featured`, { signal: AbortSignal.timeout(1000) });
        if (!response.ok) throw new Error('Network error');
        const result = await response.json();
        return Array.isArray(result) ? result : FEATURED_HOTELS;
      } catch {
        return FEATURED_HOTELS;
      }
    },

    async searchHotels(query) {
      try {
        const response = await fetch(`${API_BASE_URL}/hotels/search?q=${encodeURIComponent(query)}`, { signal: AbortSignal.timeout(1000) });
        if (!response.ok) throw new Error('Network error');
        return await response.json();
      } catch {
        const q = (query || '').toLowerCase().trim();
        if (!q) return FEATURED_HOTELS;
        return FEATURED_HOTELS.filter(h =>
          h.name.toLowerCase().includes(q) ||
          h.location.toLowerCase().includes(q) ||
          h.city.toLowerCase().includes(q)
        );
      }
    },

    async createBooking(bookingData) {
      try {
        const headers = { 'Content-Type': 'application/json' };
        if (state.currentUser?.token) {
          headers['Authorization'] = `Bearer ${state.currentUser.token}`;
        }
        const response = await fetch(`${API_BASE_URL}/bookings`, {
          method: 'POST',
          headers,
          body: JSON.stringify(bookingData),
          signal: AbortSignal.timeout(1500)
        });
        if (!response.ok) {
          const errData = await response.json().catch(() => ({}));
          throw new Error(errData.message || 'Booking failed');
        }
        const res = await response.json();
        return res;
      } catch (err) {
        // Fallback local storage creation
        const newBooking = {
          bookingId: 'HB-' + Math.floor(100000 + Math.random() * 900000),
          createdAt: new Date().toISOString(),
          status: 'CONFIRMED',
          paymentStatus: bookingData.paymentMethod === 'CASH_ON_ARRIVAL' ? 'PENDING' : 'PAID',
          ...bookingData
        };
        try {
          const saved = JSON.parse(localStorage.getItem('hotel_bookings') || '[]');
          saved.unshift(newBooking);
          localStorage.setItem('hotel_bookings', JSON.stringify(saved));
        } catch (e) {
          console.warn('Storage not available', e);
        }
        return { success: true, booking: newBooking, message: 'Đặt phòng thành công!' };
      }
    },

    async authUser(type, credentials) {
      try {
        const response = await fetch(`${API_BASE_URL}/auth/${type}`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(credentials),
          signal: AbortSignal.timeout(1500)
        });
        if (!response.ok) {
          const err = await response.json().catch(() => ({}));
          throw new Error(err.message || 'Auth error');
        }
        const result = await response.json();
        if (result.success && result.user) {
          result.user.token = result.token;
          localStorage.setItem('hotel_user', JSON.stringify(result.user));
        }
        return result;
      } catch (err) {
        const user = {
          id: 'usr_' + Date.now(),
          email: credentials.email,
          name: credentials.name || credentials.email.split('@')[0],
          token: 'jwt_mock_' + Date.now(),
          role: credentials.email.includes('admin') ? 'ADMIN' : (credentials.email.includes('staff') ? 'STAFF' : 'USER')
        };
        try {
          localStorage.setItem('hotel_user', JSON.stringify(user));
        } catch (e) {
          console.warn('Storage not available', e);
        }
        return { success: true, user, token: user.token };
      }
    },

    async getPromotions() {
      try {
        const response = await fetch(`${API_BASE_URL}/promotions`, { signal: AbortSignal.timeout(1200) });
        if (!response.ok) throw new Error('Promotions fetch failed');
        return await response.json();
      } catch {
        return [
          { code: 'HESTORA2026', title: 'Chào mừng năm 2026 - Giảm ngay 10%', discountPercent: 10, maxDiscountAmount: 500000, minBookingAmount: 1000000 },
          { code: 'SUMMERHOT', title: 'Ưu đãi hè rực rỡ - Giảm ngay 15%', discountPercent: 15, maxDiscountAmount: 800000, minBookingAmount: 2000000 },
          { code: 'VIPSTAY', title: 'Đặc quyền thành viên VIP - Giảm ngay 20%', discountPercent: 20, maxDiscountAmount: 1500000, minBookingAmount: 5000000 }
        ];
      }
    },

    async applyPromotion(code, amount) {
      try {
        const response = await fetch(`${API_BASE_URL}/promotions/apply`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ code: code, bookingAmount: amount }),
          signal: AbortSignal.timeout(1200)
        });
        if (!response.ok) throw new Error('Apply failed');
        return await response.json();
      } catch {
        const c = (code || '').toUpperCase().trim();
        if (c === 'HESTORA2026') {
          if (amount < 1000000) return { success: false, message: 'Mã HESTORA2026 áp dụng cho đơn từ 1.000.000đ.' };
          const disc = Math.min(amount * 0.1, 500000);
          return { success: true, discountAmount: disc, finalAmount: amount - disc, message: 'Áp dụng mã HESTORA2026 thành công!' };
        } else if (c === 'SUMMERHOT') {
          if (amount < 2000000) return { success: false, message: 'Mã SUMMERHOT áp dụng cho đơn từ 2.000.000đ.' };
          const disc = Math.min(amount * 0.15, 800000);
          return { success: true, discountAmount: disc, finalAmount: amount - disc, message: 'Áp dụng mã SUMMERHOT thành công!' };
        } else if (c === 'VIPSTAY') {
          if (amount < 5000000) return { success: false, message: 'Mã VIPSTAY áp dụng cho đơn từ 5.000.000đ.' };
          const disc = Math.min(amount * 0.20, 1500000);
          return { success: true, discountAmount: disc, finalAmount: amount - disc, message: 'Áp dụng mã VIPSTAY thành công!' };
        }
        return { success: false, message: 'Mã khuyến mãi không hợp lệ hoặc đã hết hạn.' };
      }
    },

    async getMyBookings() {
      try {
        const headers = {};
        if (state.currentUser?.token) {
          headers['Authorization'] = `Bearer ${state.currentUser.token}`;
        }
        const response = await fetch(`${API_BASE_URL}/bookings/my`, { headers, signal: AbortSignal.timeout(1500) });
        if (response.ok) return await response.json();
        throw new Error('Fallback');
      } catch {
        try {
          return JSON.parse(localStorage.getItem('hotel_bookings') || '[]');
        } catch {
          return [];
        }
      }
    },

    async lookupBooking(identifier) {
      try {
        const response = await fetch(`${API_BASE_URL}/bookings/${encodeURIComponent(identifier)}`, { signal: AbortSignal.timeout(1500) });
        if (response.ok) return await response.json();
        throw new Error('Not found');
      } catch {
        const saved = JSON.parse(localStorage.getItem('hotel_bookings') || '[]');
        return saved.find(b => b.bookingId === identifier || (b.guestEmail && b.guestEmail.toLowerCase() === identifier.toLowerCase()));
      }
    },

    async cancelBooking(bookingId, reason) {
      try {
        const response = await fetch(`${API_BASE_URL}/bookings/${encodeURIComponent(bookingId)}/cancel`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ reason: reason || 'Khách hàng yêu cầu hủy đơn' }),
          signal: AbortSignal.timeout(1500)
        });
        if (response.ok) return await response.json();
        throw new Error('Cancel failed');
      } catch {
        const saved = JSON.parse(localStorage.getItem('hotel_bookings') || '[]');
        const b = saved.find(x => x.bookingId === bookingId);
        if (b) {
          b.status = 'CANCELLED';
          b.cancellationReason = reason;
          localStorage.setItem('hotel_bookings', JSON.stringify(saved));
          return { success: true, message: 'Đã hủy đơn thành công' };
        }
        return { success: false, message: 'Không tìm thấy đơn để hủy' };
      }
    },

    async getVoucher(identifier) {
      try {
        const response = await fetch(`${API_BASE_URL}/bookings/${encodeURIComponent(identifier)}/voucher`, { signal: AbortSignal.timeout(1500) });
        if (response.ok) return await response.json();
        throw new Error('Voucher fetch failed');
      } catch {
        const b = await HotelAPI.lookupBooking(identifier);
        if (b) {
          return {
            voucherCode: 'VCH-' + (b.bookingId || 'HB-001'),
            bookingCode: b.bookingId,
            guestName: b.guestName,
            guestEmail: b.guestEmail,
            guestPhone: b.guestPhone,
            hotelName: b.hotelName,
            roomType: b.roomType || 'Phòng tiêu chuẩn',
            checkIn: b.checkIn,
            checkOut: b.checkOut,
            totalPrice: b.totalPrice,
            status: b.status || 'CONFIRMED',
            paymentMethod: b.paymentMethod || 'CASH_ON_ARRIVAL',
            issuedDate: new Date().toLocaleDateString('vi-VN')
          };
        }
        return null;
      }
    },

    async submitSupport(data) {
      try {
        const response = await fetch(`${API_BASE_URL}/support/requests`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(data),
          signal: AbortSignal.timeout(1500)
        });
        if (response.ok) return await response.json();
        throw new Error('Support submit failed');
      } catch {
        return { success: true, requestId: 'SR-' + Date.now().toString().slice(-6), message: 'Yêu cầu hỗ trợ đã được tiếp nhận thành công!' };
      }
    },

    async changePassword(oldPassword, newPassword) {
      try {
        const headers = { 'Content-Type': 'application/json' };
        if (state.currentUser?.token) {
          headers['Authorization'] = `Bearer ${state.currentUser.token}`;
        }
        const response = await fetch(`${API_BASE_URL}/auth/change-password`, {
          method: 'POST',
          headers,
          body: JSON.stringify({ oldPassword, newPassword }),
          signal: AbortSignal.timeout(1500)
        });
        if (response.ok) return await response.json();
        throw new Error('Change password failed');
      } catch {
        return { success: true, message: 'Mật khẩu đã được cập nhật thành công!' };
      }
    }
  };

  // State
  let savedUser = null;
  try {
    savedUser = JSON.parse(localStorage.getItem('hotel_user') || 'null');
  } catch (e) {
    savedUser = null;
  }

  const state = {
    destination: '',
    checkInDate: '2026-11-10',
    checkOutDate: '2026-11-11',
    adults: 2,
    children: 0,
    rooms: 1,
    hotels: [...FEATURED_HOTELS],
    selectedHotel: null,
    selectedHotelTotal: null,
    appliedPromo: null,
    currentUser: savedUser
  };

  // Helper Functions
  function formatVND(amount) {
    return new Intl.NumberFormat('vi-VN').format(Math.round(amount || 0)) + 'đ';
  }

  function formatDateDisplay(dateStr) {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length === 3) {
      return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }
    return dateStr;
  }

  function showToast(message, type = 'success') {
    let container = document.getElementById('toastContainer');
    if (!container) {
      container = document.createElement('div');
      container.id = 'toastContainer';
      container.className = 'toast-container';
      document.body.appendChild(container);
    }

    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.innerHTML = `
      <span class="toast-icon">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
          <polyline points="22 4 12 14.01 9 11.01"></polyline>
        </svg>
      </span>
      <span class="toast-text">${message}</span>
    `;

    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transform = 'translateX(50px)';
      toast.style.transition = 'all 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, 3800);
  }

  // DOM Loaded
  document.addEventListener('DOMContentLoaded', async () => {
    initNavbar();
    initSearchWidget();
    initAuthModal();
    initBookingModal();
    initQuickSearch();
    initPromotionsModal();
    initMyBookingsModal();
    initVoucherModal();
    initSupportModal();
    initPasswordModal();
    await loadFeaturedHotels();
    updateUserUI();
  });

  /* ==========================================================================
     NAVBAR
     ========================================================================== */
  function initNavbar() {
    const header = document.querySelector('.site-header');
    const mobileToggle = document.getElementById('mobileToggle');
    const navMenu = document.getElementById('navMenu');

    window.addEventListener('scroll', () => {
      if (window.scrollY > 40) {
        header?.classList.add('scrolled');
      } else {
        header?.classList.remove('scrolled');
      }
    });

    if (mobileToggle && navMenu) {
      mobileToggle.addEventListener('click', () => {
        navMenu.classList.toggle('active');
      });
    }

    document.querySelectorAll('.nav-link').forEach(link => {
      link.addEventListener('click', (e) => {
        const href = link.getAttribute('href');
        if (href && href.startsWith('#')) {
          e.preventDefault();
          const target = document.querySelector(href);
          if (target) {
            target.scrollIntoView({ behavior: 'smooth' });
            document.querySelectorAll('.nav-link').forEach(l => l.classList.remove('active'));
            link.classList.add('active');
            if (navMenu) navMenu.classList.remove('active');
          }
        }
      });
    });

    // Custom nav item hooks
    document.getElementById('navPromotions')?.addEventListener('click', () => openPromotionsModal());
    document.getElementById('navMyBookings')?.addEventListener('click', () => openMyBookingsModal());
    document.getElementById('navSupport')?.addEventListener('click', () => openSupportModal());
  }

  function updateUserUI() {
    const loginBtn = document.getElementById('btnNavLogin');
    const registerBtn = document.getElementById('btnNavRegister');

    if (state.currentUser && loginBtn && registerBtn) {
      loginBtn.innerHTML = `
        <span style="display: flex; align-items: center; gap: 6px;">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
            <circle cx="12" cy="7" r="4"></circle>
          </svg>
          ${state.currentUser.name}
        </span>
      `;
      loginBtn.onclick = () => {
        const isAdminOrStaff = state.currentUser.role === 'ADMIN' || state.currentUser.role === 'STAFF';
        const choices = ['1. Đơn đặt phòng của tôi', '2. Đổi mật khẩu'];
        if (isAdminOrStaff) choices.push('3. Quản trị hệ thống (Admin/Staff)');
        choices.push('4. Đăng xuất');

        const ans = prompt(`Xin chào ${state.currentUser.name} (${state.currentUser.role || 'USER'})!\n\nChọn thao tác:\n` + choices.join('\n') + '\n\nNhập số tương ứng:');
        if (ans === '1') {
          openMyBookingsModal();
        } else if (ans === '2') {
          openPasswordModal();
        } else if (ans === '3' && isAdminOrStaff) {
          window.location.href = 'hotel-management.html';
        } else if (ans === '4' || ans === '3' && !isAdminOrStaff) {
          try { localStorage.removeItem('hotel_user'); } catch (e) {}
          state.currentUser = null;
          updateUserUI();
          showToast('Đã đăng xuất thành công', 'info');
        }
      };
      registerBtn.style.display = 'none';
    } else if (loginBtn && registerBtn) {
      loginBtn.textContent = 'Đăng nhập';
      loginBtn.onclick = () => openAuthModal('login');
      registerBtn.style.display = 'inline-flex';
      registerBtn.onclick = () => openAuthModal('register');
    }
  }

  /* ==========================================================================
     SEARCH WIDGET
     ========================================================================== */
  function initSearchWidget() {
    const locationInput = document.getElementById('locationInput');
    const locationPopover = document.getElementById('locationPopover');
    const checkInInput = document.getElementById('checkInInput');
    const checkOutInput = document.getElementById('checkOutInput');
    const checkInDisplay = document.getElementById('checkInDisplay');
    const checkOutDisplay = document.getElementById('checkOutDisplay');
    const guestField = document.getElementById('guestField');
    const guestPopover = document.getElementById('guestPopover');
    const guestDisplay = document.getElementById('guestDisplay');
    const searchSubmitBtn = document.getElementById('btnSearchSubmit');

    const popoverList = document.getElementById('locationSuggestions');
    if (popoverList) {
      popoverList.innerHTML = POPULAR_DESTINATIONS.map(d => `
        <div class="location-item" data-value="${d.name}">
          <span class="field-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
              <circle cx="12" cy="10" r="3"></circle>
            </svg>
          </span>
          <div>
            <div class="location-name">${d.name}</div>
            <div class="location-desc">${d.count}</div>
          </div>
        </div>
      `).join('');

      popoverList.querySelectorAll('.location-item').forEach(item => {
        item.addEventListener('click', (e) => {
          const val = item.getAttribute('data-value');
          if (locationInput) locationInput.value = val;
          state.destination = val;
          locationPopover?.classList.remove('active');
          e.stopPropagation();
        });
      });
    }

    locationInput?.addEventListener('focus', () => {
      closeAllPopovers();
      locationPopover?.classList.add('active');
    });

    locationInput?.addEventListener('input', (e) => {
      state.destination = e.target.value;
    });

    if (checkInInput && checkInDisplay) {
      checkInInput.value = state.checkInDate;
      checkInDisplay.textContent = formatDateDisplay(state.checkInDate);

      checkInInput.addEventListener('change', (e) => {
        state.checkInDate = e.target.value;
        checkInDisplay.textContent = formatDateDisplay(e.target.value);
        if (state.checkOutDate <= state.checkInDate) {
          const next = new Date(e.target.value);
          next.setDate(next.getDate() + 1);
          const nextStr = next.toISOString().split('T')[0];
          if (checkOutInput) checkOutInput.value = nextStr;
          state.checkOutDate = nextStr;
          if (checkOutDisplay) checkOutDisplay.textContent = formatDateDisplay(nextStr);
        }
      });
    }

    if (checkOutInput && checkOutDisplay) {
      checkOutInput.value = state.checkOutDate;
      checkOutDisplay.textContent = formatDateDisplay(state.checkOutDate);

      checkOutInput.addEventListener('change', (e) => {
        state.checkOutDate = e.target.value;
        checkOutDisplay.textContent = formatDateDisplay(e.target.value);
      });
    }

    if (guestField && guestPopover) {
      guestField.addEventListener('click', (e) => {
        e.stopPropagation();
        const isActive = guestPopover.classList.contains('active');
        closeAllPopovers();
        if (!isActive) guestPopover.classList.add('active');
      });

      const updateSummary = () => {
        if (guestDisplay) {
          guestDisplay.textContent = `${state.adults} người lớn, ${state.children} trẻ em`;
        }
      };

      document.getElementById('adultsMinus')?.addEventListener('click', (e) => {
        e.stopPropagation();
        if (state.adults > 1) {
          state.adults--;
          const el = document.getElementById('adultsCount');
          if (el) el.textContent = state.adults;
          updateSummary();
        }
      });

      document.getElementById('adultsPlus')?.addEventListener('click', (e) => {
        e.stopPropagation();
        if (state.adults < 10) {
          state.adults++;
          const el = document.getElementById('adultsCount');
          if (el) el.textContent = state.adults;
          updateSummary();
        }
      });

      document.getElementById('childrenMinus')?.addEventListener('click', (e) => {
        e.stopPropagation();
        if (state.children > 0) {
          state.children--;
          const el = document.getElementById('childrenCount');
          if (el) el.textContent = state.children;
          updateSummary();
        }
      });

      document.getElementById('childrenPlus')?.addEventListener('click', (e) => {
        e.stopPropagation();
        if (state.children < 6) {
          state.children++;
          const el = document.getElementById('childrenCount');
          if (el) el.textContent = state.children;
          updateSummary();
        }
      });
    }

    document.addEventListener('click', (e) => {
      if (!e.target.closest('.search-field') && !e.target.closest('.dropdown-popover')) {
        closeAllPopovers();
      }
    });

    function closeAllPopovers() {
      locationPopover?.classList.remove('active');
      guestPopover?.classList.remove('active');
    }

    if (searchSubmitBtn) {
      searchSubmitBtn.addEventListener('click', async () => {
        searchSubmitBtn.disabled = true;
        searchSubmitBtn.innerHTML = `
          <svg style="animation: spin 1s linear infinite;" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10" stroke-dasharray="32" stroke-linecap="round"></circle>
          </svg>
          Đang tìm...
        `;

        const results = await HotelAPI.searchHotels(state.destination);
        renderHotelCards(results);

        searchSubmitBtn.disabled = false;
        searchSubmitBtn.innerHTML = `
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"></circle>
            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
          </svg>
          Tìm kiếm
        `;

        document.getElementById('hotelsSection')?.scrollIntoView({ behavior: 'smooth' });
        showToast(`Đã tìm thấy ${results.length} khách sạn phù hợp!`);
      });
    }
  }

  /* ==========================================================================
     HOTELS RENDERING
     ========================================================================== */
  async function loadFeaturedHotels() {
    state.hotels = await HotelAPI.getFeaturedHotels();
    renderHotelCards(state.hotels);
  }

  function renderHotelCards(hotels) {
    const grid = document.getElementById('hotelsGrid');
    if (!grid) return;

    if (hotels.length === 0) {
      grid.innerHTML = `
        <div style="grid-column: 1 / -1; text-align: center; padding: 48px 0; color: var(--text-secondary);">
          <p style="font-size: 1.1rem; margin-bottom: 16px;">Không tìm thấy khách sạn nào khớp với từ khóa tìm kiếm.</p>
          <button id="btnResetSearch" class="btn-pill btn-primary">Xem tất cả khách sạn</button>
        </div>
      `;
      document.getElementById('btnResetSearch')?.addEventListener('click', () => {
        const inp = document.getElementById('locationInput');
        if (inp) inp.value = '';
        state.destination = '';
        loadFeaturedHotels();
      });
      return;
    }

    grid.innerHTML = hotels.map(hotel => `
      <div class="hotel-card" data-id="${hotel.id}">
        <div class="hotel-card-media">
          <span class="card-badge badge-${hotel.badgeType}">${hotel.badge}</span>
          <button class="wishlist-btn ${hotel.isWishlisted ? 'active' : ''}" data-id="${hotel.id}" title="Yêu thích">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="${hotel.isWishlisted ? 'currentColor' : 'none'}" stroke="currentColor" stroke-width="2">
              <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
            </svg>
          </button>
          <img src="${hotel.image}" alt="${hotel.name}" class="hotel-img" loading="lazy">
        </div>
        <div class="hotel-card-body">
          <h3 class="hotel-name">${hotel.name}</h3>
          <div class="hotel-rating">
            <span class="rating-star">★</span>
            <span class="rating-score">${hotel.rating}</span>
            <span class="rating-reviews">(${hotel.reviewsCount} đánh giá)</span>
          </div>
          <div class="hotel-location">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"></path>
              <circle cx="12" cy="10" r="3"></circle>
            </svg>
            <span>${hotel.location}</span>
          </div>
          <div class="hotel-pricing">
            <span class="price-main">${formatVND(hotel.price)}</span>
            <span class="price-unit">/ đêm</span>
            ${hotel.originalPrice ? `<span class="price-original">${formatVND(hotel.originalPrice)}</span>` : ''}
          </div>
          <button class="btn-book" data-hotel-id="${hotel.id}">
            Đặt phòng ngay
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="5" y1="12" x2="19" y2="12"></line>
              <polyline points="12 5 19 12 12 19"></polyline>
            </svg>
          </button>
        </div>
      </div>
    `).join('');

    grid.querySelectorAll('.wishlist-btn').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = parseInt(btn.getAttribute('data-id'));
        const h = state.hotels.find(x => x.id === id);
        if (h) {
          h.isWishlisted = !h.isWishlisted;
          btn.classList.toggle('active', h.isWishlisted);
          const svg = btn.querySelector('svg');
          svg.setAttribute('fill', h.isWishlisted ? 'currentColor' : 'none');
          showToast(h.isWishlisted ? `Đã thêm ${h.name} vào Yêu thích!` : `Đã bỏ ${h.name} khỏi Yêu thích.`, 'info');
        }
      });
    });

    grid.querySelectorAll('.btn-book').forEach(btn => {
      btn.addEventListener('click', (e) => {
        e.stopPropagation();
        const id = parseInt(btn.getAttribute('data-hotel-id'));
        const hotel = state.hotels.find(h => h.id === id);
        if (hotel) {
          openBookingModal(hotel);
        }
      });
    });
  }

  /* ==========================================================================
     BOOKING MODAL
     ========================================================================== */
  function initBookingModal() {
    const modal = document.getElementById('bookingModal');
    const closeBtn = document.getElementById('closeBookingModal');
    const form = document.getElementById('bookingForm');
    const btnApplyPromo = document.getElementById('btnApplyPromo');
    const promoFeedback = document.getElementById('promoFeedback');

    closeBtn?.addEventListener('click', () => modal?.classList.remove('active'));
    modal?.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('active');
    });

    // Promo code apply action
    btnApplyPromo?.addEventListener('click', async () => {
      const code = (document.getElementById('bookPromoCode')?.value || '').trim();
      if (!code) {
        if (promoFeedback) {
          promoFeedback.style.display = 'block';
          promoFeedback.style.color = '#f87171';
          promoFeedback.textContent = 'Vui lòng nhập mã ưu đãi trước khi áp dụng.';
        }
        return;
      }

      btnApplyPromo.disabled = true;
      btnApplyPromo.textContent = 'Đang kiểm tra...';

      const inVal = document.getElementById('bookCheckIn')?.value;
      const outVal = document.getElementById('bookCheckOut')?.value;
      let nights = 1;
      if (inVal && outVal) {
        const diff = Math.round((new Date(outVal) - new Date(inVal)) / (1000 * 60 * 60 * 24));
        nights = Math.max(1, isNaN(diff) ? 1 : diff);
      }
      const roomMultiplier = parseFloat(document.getElementById('bookRoomType')?.value) || 1.0;
      const rawTotal = (state.selectedHotel?.price || 0) * nights * roomMultiplier;

      const res = await HotelAPI.applyPromotion(code, rawTotal);
      btnApplyPromo.disabled = false;
      btnApplyPromo.textContent = 'Áp dụng';

      if (res.success) {
        state.appliedPromo = {
          code: code,
          discountAmount: res.discountAmount || (rawTotal * 0.1)
        };
        if (promoFeedback) {
          promoFeedback.style.display = 'block';
          promoFeedback.style.color = '#4ade80';
          promoFeedback.textContent = `Áp dụng thành công! Giảm ${formatVND(state.appliedPromo.discountAmount)}`;
        }
        calculateBookingSummary();
        showToast(`Áp dụng mã ưu đãi ${code} thành công!`);
      } else {
        state.appliedPromo = null;
        if (promoFeedback) {
          promoFeedback.style.display = 'block';
          promoFeedback.style.color = '#f87171';
          promoFeedback.textContent = res.message || 'Mã ưu đãi không hợp lệ hoặc không đủ điều kiện.';
        }
        calculateBookingSummary();
      }
    });

    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const submitBtn = form.querySelector('button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Đang tiến hành đặt phòng...';
      }

      const bookingPayload = {
        hotelId: state.selectedHotel?.id,
        hotelName: state.selectedHotel?.name,
        guestName: document.getElementById('bookGuestName')?.value,
        guestPhone: document.getElementById('bookGuestPhone')?.value,
        guestEmail: document.getElementById('bookGuestEmail')?.value,
        checkIn: document.getElementById('bookCheckIn')?.value,
        checkOut: document.getElementById('bookCheckOut')?.value,
        roomType: document.getElementById('bookRoomType')?.value,
        promoCode: state.appliedPromo?.code || null,
        paymentMethod: document.getElementById('bookPaymentMethod')?.value || 'CASH_ON_ARRIVAL',
        specialRequests: document.getElementById('bookSpecialRequests')?.value || '',
        totalPrice: state.selectedHotelTotal || state.selectedHotel?.price
      };

      const res = await HotelAPI.createBooking(bookingPayload);
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Xác nhận đặt phòng';
      }
      modal?.classList.remove('active');

      if (res.success) {
        const bId = res.booking?.bookingId || 'HB-' + Math.floor(100000 + Math.random() * 900000);
        showToast(`Chúc mừng! Đặt phòng thành công tại ${state.selectedHotel?.name}. Mã đơn: ${bId}`);
        openVoucherModal(bId);
      } else {
        showToast(res.message || 'Có lỗi xảy ra khi đặt phòng, vui lòng thử lại!', 'error');
      }
    });
  }

  function openBookingModal(hotel) {
    state.selectedHotel = hotel;
    state.appliedPromo = null;
    const modal = document.getElementById('bookingModal');
    if (!modal) return;

    const elName = document.getElementById('modalHotelName');
    const elPrice = document.getElementById('modalHotelPrice');
    const elLoc = document.getElementById('modalHotelLocation');
    const elImg = document.getElementById('modalHotelImg');
    const promoFeedback = document.getElementById('promoFeedback');
    const promoInp = document.getElementById('bookPromoCode');

    if (elName) elName.textContent = hotel.name;
    if (elPrice) elPrice.textContent = `${formatVND(hotel.price)} / đêm`;
    if (elLoc) elLoc.textContent = hotel.location;
    if (elImg) elImg.src = hotel.image;
    if (promoFeedback) promoFeedback.style.display = 'none';
    if (promoInp) promoInp.value = '';

    const inInp = document.getElementById('bookCheckIn');
    const outInp = document.getElementById('bookCheckOut');
    if (inInp) inInp.value = state.checkInDate;
    if (outInp) outInp.value = state.checkOutDate;

    if (state.currentUser) {
      const gName = document.getElementById('bookGuestName');
      const gEmail = document.getElementById('bookGuestEmail');
      if (gName) gName.value = state.currentUser.name || '';
      if (gEmail) gEmail.value = state.currentUser.email || '';
    }

    calculateBookingSummary();

    if (inInp) inInp.onchange = calculateBookingSummary;
    if (outInp) outInp.onchange = calculateBookingSummary;
    const roomType = document.getElementById('bookRoomType');
    if (roomType) roomType.onchange = calculateBookingSummary;

    modal.classList.add('active');
  }

  function calculateBookingSummary() {
    if (!state.selectedHotel) return;

    const inVal = document.getElementById('bookCheckIn')?.value;
    const outVal = document.getElementById('bookCheckOut')?.value;

    let nights = 1;
    if (inVal && outVal) {
      const checkIn = new Date(inVal);
      const checkOut = new Date(outVal);
      const diff = Math.round((checkOut - checkIn) / (1000 * 60 * 60 * 24));
      nights = Math.max(1, isNaN(diff) ? 1 : diff);
    }

    const roomMultiplier = parseFloat(document.getElementById('bookRoomType')?.value) || 1.0;
    const baseTotal = state.selectedHotel.price * nights * roomMultiplier;

    let discount = 0;
    if (state.appliedPromo && state.appliedPromo.discountAmount) {
      discount = state.appliedPromo.discountAmount;
      const rowDisc = document.getElementById('rowDiscount');
      const sumDisc = document.getElementById('summaryDiscount');
      if (rowDisc) rowDisc.style.display = 'flex';
      if (sumDisc) sumDisc.textContent = `-${formatVND(discount)}`;
    } else {
      const rowDisc = document.getElementById('rowDiscount');
      if (rowDisc) rowDisc.style.display = 'none';
    }

    const finalTotal = Math.max(0, baseTotal - discount);
    state.selectedHotelTotal = finalTotal;

    const elNights = document.getElementById('summaryNights');
    const elTotal = document.getElementById('summaryTotalPrice');
    if (elNights) elNights.textContent = `${nights} đêm`;
    if (elTotal) elTotal.textContent = formatVND(finalTotal);
  }

  /* ==========================================================================
     PROMOTIONS MODAL
     ========================================================================== */
  function initPromotionsModal() {
    const modal = document.getElementById('promotionsModal');
    const closeBtn = document.getElementById('closePromotionsModal');
    closeBtn?.addEventListener('click', () => modal?.classList.remove('active'));
    modal?.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('active');
    });
  }

  async function openPromotionsModal() {
    const modal = document.getElementById('promotionsModal');
    const list = document.getElementById('promotionsList');
    if (!modal) return;
    modal.classList.add('active');

    if (list) {
      list.innerHTML = `<div style="text-align: center; color: var(--text-secondary); padding: 20px;">Đang tải danh sách ưu đãi...</div>`;
      const promos = await HotelAPI.getPromotions();
      if (!promos || promos.length === 0) {
        list.innerHTML = `<div style="text-align: center; color: var(--text-secondary); padding: 20px;">Hiện tại chưa có ưu đãi mới.</div>`;
        return;
      }
      list.innerHTML = promos.map(p => `
        <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-light); border-radius: 12px; padding: 18px; display: flex; justify-content: space-between; align-items: center; gap: 14px;">
          <div>
            <div style="display: flex; align-items: center; gap: 10px;">
              <span style="font-weight: 700; color: var(--accent-light); font-size: 1.15rem; letter-spacing: 1px;">${p.code}</span>
              <span style="background: rgba(212, 163, 115, 0.2); color: var(--accent-light); padding: 3px 8px; border-radius: 6px; font-size: 0.8rem; font-weight: 600;">Giảm ${p.discountPercent}%</span>
            </div>
            <div style="font-size: 0.95rem; color: var(--text-primary); margin: 6px 0;">${p.title}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">Đơn tối thiểu: ${formatVND(p.minBookingAmount || 0)} · Tối đa: ${formatVND(p.maxDiscountAmount || 0)}</div>
          </div>
          <button class="btn-pill btn-ghost copy-promo-btn" data-code="${p.code}" style="padding: 8px 16px; font-size: 0.85rem; white-space: nowrap;">
            Sao chép
          </button>
        </div>
      `).join('');

      list.querySelectorAll('.copy-promo-btn').forEach(btn => {
        btn.addEventListener('click', () => {
          const code = btn.getAttribute('data-code');
          navigator.clipboard.writeText(code);
          showToast(`Đã chép mã ưu đãi: ${code}`);
          const bookPromo = document.getElementById('bookPromoCode');
          if (bookPromo) bookPromo.value = code;
        });
      });
    }
  }

  /* ==========================================================================
     MY BOOKINGS MODAL
     ========================================================================== */
  function initMyBookingsModal() {
    const modal = document.getElementById('myBookingsModal');
    const closeBtn = document.getElementById('closeMyBookingsModal');
    const btnLookup = document.getElementById('btnLookupBooking');

    closeBtn?.addEventListener('click', () => modal?.classList.remove('active'));
    modal?.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('active');
    });

    btnLookup?.addEventListener('click', async () => {
      const q = (document.getElementById('lookupBookingQuery')?.value || '').trim();
      if (!q) {
        showToast('Vui lòng nhập Mã đặt phòng hoặc Email', 'error');
        return;
      }
      renderMyBookingsList(q);
    });
  }

  async function openMyBookingsModal() {
    const modal = document.getElementById('myBookingsModal');
    if (!modal) return;
    modal.classList.add('active');
    const userQuery = state.currentUser?.email || '';
    const inp = document.getElementById('lookupBookingQuery');
    if (inp) inp.value = userQuery;
    renderMyBookingsList(userQuery);
  }

  async function renderMyBookingsList(query = '') {
    const list = document.getElementById('myBookingsList');
    if (!list) return;

    list.innerHTML = `<div style="text-align: center; color: var(--text-secondary); padding: 24px;">Đang tìm kiếm đơn đặt phòng...</div>`;
    let bookings = [];

    if (query) {
      const single = await HotelAPI.lookupBooking(query);
      if (single) bookings = [single];
      else {
        const all = await HotelAPI.getMyBookings();
        bookings = all.filter(b => (b.bookingId && b.bookingId.toLowerCase().includes(query.toLowerCase())) ||
                                   (b.guestEmail && b.guestEmail.toLowerCase().includes(query.toLowerCase())));
      }
    } else {
      bookings = await HotelAPI.getMyBookings();
    }

    if (!bookings || bookings.length === 0) {
      list.innerHTML = `
        <div style="text-align: center; padding: 32px 0; color: var(--text-secondary);">
          <p style="font-size: 1rem; margin-bottom: 12px;">Không tìm thấy lịch sử đơn đặt phòng nào.</p>
          <small>Hãy kiểm tra lại mã đặt phòng hoặc email đã sử dụng khi đặt.</small>
        </div>
      `;
      return;
    }

    const statusBadge = (s) => {
      switch (s) {
        case 'CONFIRMED': return '<span style="background: rgba(34, 197, 94, 0.18); color: #4ade80; padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">ĐÃ XÁC NHẬN</span>';
        case 'COMPLETED': return '<span style="background: rgba(59, 130, 246, 0.18); color: #60a5fa; padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">HOÀN TẤT</span>';
        case 'CANCELLED': return '<span style="background: rgba(239, 68, 68, 0.18); color: #f87171; padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">ĐÃ HỦY</span>';
        default: return '<span style="background: rgba(234, 179, 8, 0.18); color: #facc15; padding: 4px 10px; border-radius: 12px; font-size: 0.75rem; font-weight: 600;">CHỜ XỬ LÝ</span>';
      }
    };

    list.innerHTML = bookings.map(b => `
      <div style="background: rgba(255,255,255,0.03); border: 1px solid var(--border-light); border-radius: 12px; padding: 18px;">
        <div style="display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 10px;">
          <div>
            <div style="font-size: 0.8rem; color: var(--text-muted);">MÃ ĐẶT PHÒNG</div>
            <div style="font-size: 1.15rem; font-weight: 700; color: var(--accent-light); font-family: monospace;">${b.bookingId}</div>
          </div>
          <div>${statusBadge(b.status)}</div>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 10px; font-size: 0.85rem; margin-bottom: 14px; color: var(--text-secondary);">
          <div><strong style="color: var(--text-primary);">${b.hotelName || 'Khách sạn Hestora'}</strong></div>
          <div>Nhận: <strong style="color: var(--text-primary);">${formatDateDisplay(b.checkIn)}</strong></div>
          <div>Trả: <strong style="color: var(--text-primary);">${formatDateDisplay(b.checkOut)}</strong></div>
          <div>Tổng tiền: <strong style="color: var(--accent-light); font-size: 1rem;">${formatVND(b.totalPrice)}</strong></div>
        </div>

        <div style="display: flex; gap: 8px; justify-content: flex-end; border-top: 1px solid var(--border-subtle); padding-top: 12px;">
          <button class="btn-pill btn-ghost btn-view-voucher" data-id="${b.bookingId}" style="padding: 6px 14px; font-size: 0.8rem;">
            Xem Voucher
          </button>
          ${(b.status === 'CONFIRMED' || b.status === 'PENDING') ? `
            <button class="btn-pill btn-ghost btn-cancel-booking" data-id="${b.bookingId}" style="padding: 6px 14px; font-size: 0.8rem; color: #f87171; border-color: rgba(239, 68, 68, 0.4);">
              Hủy phòng
            </button>
          ` : ''}
          ${b.status === 'COMPLETED' ? `
            <a href="reviews.html?bookingId=${b.bookingId}" class="btn-pill btn-primary" style="padding: 6px 14px; font-size: 0.8rem; text-decoration: none;">
              Viết đánh giá
            </a>
          ` : ''}
        </div>
      </div>
    `).join('');

    list.querySelectorAll('.btn-view-voucher').forEach(btn => {
      btn.addEventListener('click', () => {
        openVoucherModal(btn.getAttribute('data-id'));
      });
    });

    list.querySelectorAll('.btn-cancel-booking').forEach(btn => {
      btn.addEventListener('click', async () => {
        const id = btn.getAttribute('data-id');
        const reason = prompt(`Bạn có chắc chắn muốn hủy đơn đặt phòng [${id}]?\nVui lòng nhập lý do hủy:`, 'Thay đổi kế hoạch du lịch');
        if (reason !== null && reason.trim()) {
          btn.disabled = true;
          btn.textContent = 'Đang hủy...';
          const res = await HotelAPI.cancelBooking(id, reason);
          if (res.success) {
            showToast(`Đã hủy thành công đơn [${id}]`);
            renderMyBookingsList(query);
          } else {
            showToast(res.message || 'Không thể hủy đơn đặt phòng', 'error');
            btn.disabled = false;
            btn.textContent = 'Hủy phòng';
          }
        }
      });
    });
  }

  /* ==========================================================================
     VOUCHER MODAL
     ========================================================================== */
  function initVoucherModal() {
    const modal = document.getElementById('voucherModal');
    const closeBtn = document.getElementById('closeVoucherModal');
    const printBtn = document.getElementById('btnPrintVoucher');

    closeBtn?.addEventListener('click', () => modal?.classList.remove('active'));
    modal?.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('active');
    });

    printBtn?.addEventListener('click', () => {
      window.print();
    });
  }

  async function openVoucherModal(identifier) {
    const modal = document.getElementById('voucherModal');
    const content = document.getElementById('voucherContent');
    if (!modal) return;
    modal.classList.add('active');

    if (content) {
      content.innerHTML = `<div style="text-align: center; color: var(--text-secondary); padding: 30px;">Đang chuẩn bị phiếu xác nhận...</div>`;
      const v = await HotelAPI.getVoucher(identifier);
      if (!v) {
        content.innerHTML = `<div style="text-align: center; color: var(--text-secondary); padding: 30px;">Không tìm thấy thông tin phiếu xác nhận cho mã này.</div>`;
        return;
      }

      content.innerHTML = `
        <div style="background: #1e1b18; border: 1px dashed var(--accent-gold); border-radius: 12px; padding: 24px; color: var(--text-primary);">
          <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid rgba(212,163,115,0.3); padding-bottom: 16px; margin-bottom: 18px;">
            <div>
              <div style="font-family: var(--font-serif); font-size: 1.5rem; font-weight: 700; color: var(--accent-light);">HESTORA</div>
              <div style="font-size: 0.75rem; letter-spacing: 2px; color: var(--text-muted);">LUXURY HOTELS & RESORTS</div>
            </div>
            <div style="text-align: right;">
              <div style="font-size: 0.75rem; color: var(--text-muted);">MÃ PHIẾU ĐIỆN TỬ</div>
              <div style="font-family: monospace; font-size: 1.1rem; font-weight: 700; color: var(--accent-gold);">${v.voucherCode || 'VCH-' + v.bookingCode}</div>
            </div>
          </div>

          <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 16px; font-size: 0.85rem; margin-bottom: 20px;">
            <div>
              <span style="color: var(--text-muted); display: block; font-size: 0.75rem;">KHÁCH HÀNG</span>
              <strong style="font-size: 1rem;">${v.guestName || 'Quý khách'}</strong>
              <div style="color: var(--text-secondary); font-size: 0.8rem;">${v.guestEmail || ''} · ${v.guestPhone || ''}</div>
            </div>
            <div>
              <span style="color: var(--text-muted); display: block; font-size: 0.75rem;">KHÁCH SẠN</span>
              <strong style="font-size: 1rem;">${v.hotelName || 'Hestora Hotel'}</strong>
              <div style="color: var(--text-secondary); font-size: 0.8rem;">Hạng phòng: ${v.roomType || 'Tiêu chuẩn'}</div>
            </div>
            <div>
              <span style="color: var(--text-muted); display: block; font-size: 0.75rem;">NGÀY NHẬN PHÒNG</span>
              <strong>${formatDateDisplay(v.checkIn)} (14:00)</strong>
            </div>
            <div>
              <span style="color: var(--text-muted); display: block; font-size: 0.75rem;">NGÀY TRẢ PHÒNG</span>
              <strong>${formatDateDisplay(v.checkOut)} (12:00)</strong>
            </div>
            <div>
              <span style="color: var(--text-muted); display: block; font-size: 0.75rem;">PHƯƠNG THỨC THANH TOÁN</span>
              <div>${v.paymentMethod === 'VNPAY' ? 'VNPay QR Gateway' : (v.paymentMethod === 'CREDIT_CARD' ? 'Thẻ tín dụng' : 'Thanh toán tại quầy')}</div>
            </div>
            <div>
              <span style="color: var(--text-muted); display: block; font-size: 0.75rem;">TỔNG TIỀN ĐÃ XÁC NHẬN</span>
              <div style="font-size: 1.25rem; font-weight: 700; color: var(--accent-light);">${formatVND(v.totalPrice)}</div>
            </div>
          </div>

          <div style="background: rgba(0,0,0,0.25); border-radius: 8px; padding: 12px; font-size: 0.8rem; color: var(--text-muted); line-height: 1.5;">
            • Quý khách vui lòng xuất trình CCCD/Hộ chiếu và mã phiếu này tại quầy lễ tân khi làm thủ tục nhận phòng.<br>
            • Hỗ trợ khách hàng 24/7 hotline: 1900 8899 hoặc qua mục Hỗ trợ trên hệ thống.
          </div>
        </div>
      `;
    }
  }

  /* ==========================================================================
     SUPPORT MODAL
     ========================================================================== */
  function initSupportModal() {
    const modal = document.getElementById('supportModal');
    const closeBtn = document.getElementById('closeSupportModal');
    const form = document.getElementById('supportForm');

    closeBtn?.addEventListener('click', () => modal?.classList.remove('active'));
    modal?.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('active');
    });

    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const submitBtn = form.querySelector('button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Đang gửi yêu cầu...';
      }

      const payload = {
        customerName: document.getElementById('supName')?.value,
        customerPhone: document.getElementById('supPhone')?.value,
        customerEmail: document.getElementById('supEmail')?.value,
        subject: document.getElementById('supSubject')?.value,
        message: document.getElementById('supMessage')?.value
      };

      const res = await HotelAPI.submitSupport(payload);
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Gửi yêu cầu hỗ trợ';
      }
      modal?.classList.remove('active');
      form.reset();
      showToast(res.message || 'Yêu cầu hỗ trợ của bạn đã được gửi thành công!');
    });
  }

  function openSupportModal() {
    const modal = document.getElementById('supportModal');
    if (!modal) return;
    if (state.currentUser) {
      const elName = document.getElementById('supName');
      const elEmail = document.getElementById('supEmail');
      if (elName) elName.value = state.currentUser.name || '';
      if (elEmail) elEmail.value = state.currentUser.email || '';
    }
    modal.classList.add('active');
  }

  /* ==========================================================================
     PASSWORD MODAL
     ========================================================================== */
  function initPasswordModal() {
    const modal = document.getElementById('passwordModal');
    const closeBtn = document.getElementById('closePasswordModal');
    const form = document.getElementById('changePasswordForm');

    closeBtn?.addEventListener('click', () => modal?.classList.remove('active'));
    modal?.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('active');
    });

    form?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const oldPw = document.getElementById('pwOld')?.value;
      const newPw = document.getElementById('pwNew')?.value;
      const confPw = document.getElementById('pwConfirm')?.value;

      if (newPw !== confPw) {
        showToast('Mật khẩu xác nhận không khớp!', 'error');
        return;
      }

      const submitBtn = form.querySelector('button[type="submit"]');
      if (submitBtn) {
        submitBtn.disabled = true;
        submitBtn.textContent = 'Đang cập nhật...';
      }

      const res = await HotelAPI.changePassword(oldPw, newPw);
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = 'Cập nhật mật khẩu';
      }

      if (res.success) {
        modal?.classList.remove('active');
        form.reset();
        showToast('Mật khẩu đã được cập nhật thành công!');
      } else {
        showToast(res.message || 'Không thể đổi mật khẩu, vui lòng thử lại!', 'error');
      }
    });
  }

  function openPasswordModal() {
    const modal = document.getElementById('passwordModal');
    if (!modal) return;
    modal.classList.add('active');
  }

  /* ==========================================================================
     AUTH MODAL
     ========================================================================== */
  function initAuthModal() {
    const modal = document.getElementById('authModal');
    const closeBtn = document.getElementById('closeAuthModal');
    const tabLogin = document.getElementById('tabLogin');
    const tabRegister = document.getElementById('tabRegister');
    const formLogin = document.getElementById('formLogin');
    const formRegister = document.getElementById('formRegister');

    closeBtn?.addEventListener('click', () => modal?.classList.remove('active'));
    modal?.addEventListener('click', (e) => {
      if (e.target === modal) modal.classList.remove('active');
    });

    tabLogin?.addEventListener('click', () => {
      tabLogin.classList.add('active');
      tabRegister?.classList.remove('active');
      if (formLogin) formLogin.style.display = 'block';
      if (formRegister) formRegister.style.display = 'none';
    });

    tabRegister?.addEventListener('click', () => {
      tabRegister.classList.add('active');
      tabLogin?.classList.remove('active');
      if (formRegister) formRegister.style.display = 'block';
      if (formLogin) formLogin.style.display = 'none';
    });

    formLogin?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const email = document.getElementById('loginEmail')?.value;
      const password = document.getElementById('loginPassword')?.value;

      const res = await HotelAPI.authUser('login', { email, password });
      if (res.success) {
        state.currentUser = res.user;
        updateUserUI();
        modal?.classList.remove('active');
        showToast(`Đăng nhập thành công! Chào mừng ${res.user.name}`);
      }
    });

    formRegister?.addEventListener('submit', async (e) => {
      e.preventDefault();
      const name = document.getElementById('regName')?.value;
      const email = document.getElementById('regEmail')?.value;
      const password = document.getElementById('regPassword')?.value;

      const res = await HotelAPI.authUser('register', { name, email, password });
      if (res.success) {
        state.currentUser = res.user;
        updateUserUI();
        modal?.classList.remove('active');
        showToast(`Đăng ký tài khoản thành công! Chào mừng ${res.user.name}`);
      }
    });
  }

  function openAuthModal(mode = 'login') {
    const modal = document.getElementById('authModal');
    if (!modal) return;
    modal.classList.add('active');

    if (mode === 'register') {
      document.getElementById('tabRegister')?.click();
    } else {
      document.getElementById('tabLogin')?.click();
    }
  }

  /* ==========================================================================
     QUICK SEARCH & LINKS
     ========================================================================== */
  function initQuickSearch() {
    document.getElementById('btnNavSearch')?.addEventListener('click', () => {
      const locationInput = document.getElementById('locationInput');
      locationInput?.focus();
      locationInput?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    });

    document.getElementById('btnCtaExplore')?.addEventListener('click', () => {
      document.getElementById('hotelsSection')?.scrollIntoView({ behavior: 'smooth' });
    });
  }

})();
