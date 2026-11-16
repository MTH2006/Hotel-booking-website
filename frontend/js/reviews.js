(() => {
  const api = 'http://localhost:8080/api';
  const user = JSON.parse(localStorage.getItem('hotel_user') || 'null');
  const headers = () => ({'Content-Type':'application/json', ...(user?.token ? {Authorization:`Bearer ${user.token}`} : {})});
  const el = id => document.getElementById(id);
  const list = el('reviewList');
  let hotels = [], editingId = null, adminMode = new URLSearchParams(location.search).get('admin') === 'true' || user?.role === 'ADMIN';
  async function request(path, options={}) {
    const response = await fetch(api + path, {...options, headers:{...headers(), ...(options.headers||{})}});
    if (!response.ok) { let body={}; try{body=await response.json()}catch{}; throw new Error(body.message || `Lỗi máy chủ (${response.status})`); }
    return response.status === 204 ? null : response.json();
  }
  const esc = value => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
  async function loadHotels() {
    const response = await fetch(api + '/hotels/featured');
    const payload = await response.json(); hotels = Array.isArray(payload) ? payload : (payload.hotels || payload.data || []);
    el('hotelSelect').innerHTML = hotels.map(h=>`<option value="${h.id}">${esc(h.name)}</option>`).join('');
    if (!hotels.length) el('hotelSelect').innerHTML='<option value="">Chưa có khách sạn</option>';
  }
  function reviewCard(r, mode) {
    const controls = mode==='mine' ? `<div class="review-actions"><button data-edit="${r.id}">Sửa</button><button class="danger" data-delete="${r.id}">Xóa</button></div>` : mode==='admin' ? `<div class="review-actions"><button data-approve="${r.id}">Duyệt</button><button class="danger" data-reject="${r.id}">Từ chối</button></div>` : '';
    return `<article class="review-card"><header><strong>${esc(r.authorName)}</strong><span class="stars">${'★'.repeat(r.rating)}${'☆'.repeat(5-r.rating)}</span></header><p>${esc(r.comment)}</p><small class="muted">${esc(r.status)} · ${esc(r.createdAt?.slice(0,10))}</small>${controls}</article>`;
  }
  async function loadPublic() {
    el('listTitle').textContent = adminMode ? 'Đánh giá chờ kiểm duyệt' : 'Đánh giá đã công khai';
    try {
      const items = adminMode ? await request('/reviews/admin/pending') : await request(`/reviews?hotelId=${el('hotelSelect').value}`);
      list.innerHTML = items.length ? items.map(r=>reviewCard(r,adminMode?'admin':'public')).join('') : '<p class="notice">Chưa có đánh giá để hiển thị.</p>';
    } catch(e) { list.innerHTML=`<p class="notice">${esc(e.message)}</p>`; }
  }
  async function initCustomer() {
    if (!user?.token) { el('formMessage').textContent='Vui lòng đăng nhập từ trang chủ để gửi và quản lý đánh giá.'; el('writeSection').classList.remove('hidden'); el('submitReview').disabled=true; return; }
    el('writeSection').classList.remove('hidden');
    try {
      const [bookings, mine] = await Promise.all([request('/reviews/eligible-bookings'), request('/reviews/mine')]);
      const reviewed = new Set(mine.map(r=>r.bookingId));
      const available = bookings.filter(b=>!reviewed.has(b.id));
      el('bookingSelect').innerHTML=available.length ? available.map(b=>`<option value="${b.id}">${esc(b.hotelName)} · trả phòng ${esc(b.checkOut)} · ${esc(b.bookingId)}</option>`).join('') : '<option value="">Không có đơn đủ điều kiện để đánh giá</option>';
      list.innerHTML = mine.length ? mine.map(r=>reviewCard(r,'mine')).join('') : '<p class="notice">Bạn chưa có đánh giá nào.</p>';
      el('listTitle').textContent='Đánh giá của tôi';
    } catch(e) { el('formMessage').textContent=e.message; }
  }
  el('submitReview').addEventListener('click', async () => {
    const message=el('formMessage');
    try {
      const body={bookingId:Number(el('bookingSelect').value),rating:Number(el('rating').value),comment:el('comment').value};
      if (!body.bookingId || body.comment.trim().length<5) throw new Error('Chọn đơn lưu trú và nhập nhận xét tối thiểu 5 ký tự.');
      await request(editingId ? `/reviews/${editingId}` : '/reviews', {method:editingId?'PUT':'POST',body:JSON.stringify(body)});
      editingId=null; el('comment').value=''; message.textContent='Đã gửi. Đánh giá mới hoặc nội dung chỉnh sửa sẽ chờ quản trị viên duyệt.'; await initCustomer();
    } catch(e) { message.textContent=e.message; }
  });
  list.addEventListener('click', async event => {
    const button=event.target.closest('button'); if(!button)return;
    try {
      if(button.dataset.edit){const review=(await request('/reviews/mine')).find(r=>r.id===Number(button.dataset.edit)); if(review){editingId=review.id;el('rating').value=review.rating;el('comment').value=review.comment;el('formMessage').textContent='Đang sửa đánh giá. Gửi lại để đưa nội dung vào hàng chờ duyệt.';el('writeSection').scrollIntoView({behavior:'smooth'});}}
      if(button.dataset.delete){if(confirm('Xóa đánh giá này?')){await request(`/reviews/${button.dataset.delete}`,{method:'DELETE'});await initCustomer();}}
      const action=button.dataset.approve?'PUBLISHED':button.dataset.reject?'REJECTED':null;
      if(action){await request(`/reviews/admin/${button.dataset.approve||button.dataset.reject}/moderation`,{method:'PUT',body:JSON.stringify({status:action})});await loadPublic();}
    } catch(e){alert(e.message);}
  });
  el('hotelSelect').addEventListener('change',loadPublic);
  el('adminRefresh').addEventListener('click',loadPublic);
  (async()=>{try{await loadHotels();}catch{el('hotelSelect').innerHTML='<option value="">Không tải được khách sạn</option>';}
    if(adminMode){el('adminRefresh').classList.remove('hidden');await loadPublic();}else if(user?.role==='ADMIN'){el('adminRefresh').classList.remove('hidden');await initCustomer();await loadPublic();}else{await loadPublic();await initCustomer();}
  })();
})();
