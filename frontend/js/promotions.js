/**
 * HESTORA - Mục Ưu đãi (FR-51: danh sách, FR-52: chi tiết, FR-53: chọn mã để áp dụng khi đặt phòng)
 * Dữ liệu lấy qua window.HotelAPI.getPromotions() (do main.js cung cấp: gọi GET /api/promotions,
 * tự dùng dữ liệu dự phòng khi backend chưa chạy).
 * Việc nhập/kiểm tra mã trong form đặt phòng (FR-53, FR-54) do main.js xử lý qua POST /api/promotions/apply.
 */
(function () {
  'use strict';

  const $ = (id) => document.getElementById(id);
  const esc = (v) => String(v ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
  const vnd = (n) => new Intl.NumberFormat('vi-VN').format(Math.round(Number(n) || 0)) + 'đ';
  const day = (s) => String(s || '').slice(0, 10);
  const fmtDate = (s) => { const p = day(s).split('-'); return p.length === 3 ? `${p[2]}/${p[1]}/${p[0]}` : '—'; };
  const todayISO = () => { const d = new Date(); return new Date(d - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10); };
  const toast = (m, t) => (window.HestoraToast ? window.HestoraToast(m, t) : console.log(m));

  let promos = [];

  // Chỉ hiển thị chương trình đang áp dụng: bật, trong thời hạn, còn lượt
  function isRunning(p) {
    const t = todayISO();
    if (p.isActive === false) return false;
    if (day(p.startDate) && t < day(p.startDate)) return false;
    if (day(p.endDate) && t > day(p.endDate)) return false;
    if (p.usageLimit != null && p.usedCount != null && p.usedCount >= p.usageLimit) return false;
    return true;
  }

  const daysLeft = (p) => Math.max(0, Math.round((new Date(day(p.endDate)) - new Date(todayISO())) / 86400000));
  const remaining = (p) => (p.usageLimit != null ? Math.max(0, p.usageLimit - (p.usedCount || 0)) : null);

  function renderList() {
    const grid = $('promoGrid');
    if (!grid) return;
    if (!promos.length) {
      grid.innerHTML = '<div class="promo-empty">Hiện chưa có chương trình khuyến mãi nào đang áp dụng.</div>';
      return;
    }
    grid.innerHTML = promos.map((p) => {
      const left = remaining(p);
      const cond = [];
      if (p.minBookingAmount > 0) cond.push(`Đơn từ ${vnd(p.minBookingAmount)}`);
      if (p.maxDiscountAmount > 0) cond.push(`giảm tối đa ${vnd(p.maxDiscountAmount)}`);
      return `
      <article class="promo-card">
        <span class="promo-badge">Giảm ${esc(p.discountPercent)}%</span>
        <h3>${esc(p.title || p.code)}</h3>
        <p class="promo-cond">${cond.length ? esc(cond.join(', ')) + '.' : 'Áp dụng cho mọi đơn đặt phòng.'}</p>
        <div class="promo-code-chip"><code>${esc(p.code)}</code>
          <button type="button" class="promo-copy-btn" data-promo-copy="${esc(p.code)}">Sao chép</button></div>
        <div class="promo-meta"><span>HSD: ${fmtDate(p.endDate)} (còn ${daysLeft(p)} ngày)</span>${left != null ? `<span>Còn ${left} lượt</span>` : ''}</div>
        <div class="promo-card-actions">
          <button type="button" class="btn-pill btn-ghost" data-promo-detail="${esc(p.code)}">Chi tiết</button>
          <button type="button" class="btn-pill btn-primary" data-promo-use="${esc(p.code)}">Dùng mã</button>
        </div>
      </article>`;
    }).join('');
  }

  // FR-52: hiển thị mức giảm, thời hạn, điều kiện áp dụng
  function showDetail(code) {
    const p = promos.find((x) => x.code === code);
    if (!p) return;
    const left = remaining(p);
    const used = p.usageLimit ? Math.min(100, Math.round(((p.usedCount || 0) / p.usageLimit) * 100)) : 0;
    $('promoDetailBody').innerHTML = `
      <span class="promo-badge">Giảm ${esc(p.discountPercent)}%</span>
      <h3 class="promo-detail-title" id="promoDetailTitle">${esc(p.title || p.code)}</h3>
      <ul class="promo-detail-list">
        <li><span>Mức giảm</span><strong>${esc(p.discountPercent)}%${p.maxDiscountAmount > 0 ? ` (tối đa ${vnd(p.maxDiscountAmount)})` : ''}</strong></li>
        <li><span>Thời hạn</span><strong>${fmtDate(p.startDate)} – ${fmtDate(p.endDate)}</strong></li>
        <li><span>Điều kiện</span><strong>${p.minBookingAmount > 0 ? `Đơn đặt phòng từ ${vnd(p.minBookingAmount)}` : 'Không yêu cầu đơn tối thiểu'}</strong></li>
        <li><span>Lượt sử dụng còn lại</span><strong>${left != null ? `${left} / ${p.usageLimit}` : 'Không giới hạn'}</strong></li>
        <li><span>Mã áp dụng</span><strong>${esc(p.code)}</strong></li>
      </ul>
      ${p.usageLimit ? `<div class="promo-progress" title="Đã dùng ${used}%"><i style="width:${used}%"></i></div>` : ''}
      <div class="promo-detail-actions">
        <button type="button" class="btn-pill btn-ghost" data-promo-copy="${esc(p.code)}">Sao chép mã</button>
        <button type="button" class="btn-pill btn-primary" data-promo-use="${esc(p.code)}">Dùng mã ngay</button>
      </div>`;
    $('promoDetailModal')?.classList.add('active');
  }

  async function copy(code) {
    try { await navigator.clipboard.writeText(code); } catch (_) { /* file:// hoặc trình duyệt chặn clipboard */ }
    toast(`Đã chép mã ưu đãi: ${code}`);
  }

  // FR-53: chuyển mã sang form đặt phòng (main.js sẽ điền sẵn vào ô "Mã khuyến mãi" khi mở modal)
  function useCode(code) {
    copy(code);
    window.__pendingPromoCode = code;
    const input = $('bookPromoCode');
    if (input) input.value = code;
    $('promoDetailModal')?.classList.remove('active');
    $('hotelsSection')?.scrollIntoView({ behavior: 'smooth' });
    toast(`Mã ${code} đã sẵn sàng. Hãy chọn khách sạn và nhấn "Áp dụng" khi đặt phòng.`);
  }

  async function load() {
    const grid = $('promoGrid');
    if (grid) grid.innerHTML = '<div class="promo-empty">Đang tải danh sách ưu đãi...</div>';
    try {
      const list = window.HotelAPI ? await window.HotelAPI.getPromotions() : [];
      promos = (Array.isArray(list) ? list : []).filter(isRunning);
    } catch (_) { promos = []; }
    renderList();
  }

  document.addEventListener('DOMContentLoaded', () => {
    document.addEventListener('click', (e) => {
      const t = e.target.closest('[data-promo-copy],[data-promo-detail],[data-promo-use]');
      if (!t) return;
      if (t.dataset.promoCopy) copy(t.dataset.promoCopy);
      else if (t.dataset.promoDetail) showDetail(t.dataset.promoDetail);
      else if (t.dataset.promoUse) useCode(t.dataset.promoUse);
    });
    const modal = $('promoDetailModal');
    $('closePromoDetail')?.addEventListener('click', () => modal?.classList.remove('active'));
    modal?.addEventListener('click', (e) => { if (e.target === modal) modal.classList.remove('active'); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') modal?.classList.remove('active'); });
    load();
  });

  window.HestoraPromotions = { reload: load };
})();
