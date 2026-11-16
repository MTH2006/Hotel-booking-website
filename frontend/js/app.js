// Giả định User ID hiện tại là 1 (tích hợp Auth sau)
const CURRENT_USER_ID = 1;

document.addEventListener('DOMContentLoaded', () => {
    initBookingForm();
    loadUserBookingHistory();
});

async function initBookingForm() {
    try {
        const hotels = await api.getHotels();
        const select = document.getElementById('booking-hotel-select');
        if (!select) return;

        select.innerHTML = '<option value="">-- Chọn khách sạn --</option>';
        hotels.forEach(h => {
            select.innerHTML += `<option value="${h.id}">${h.name} - ${h.city}</option>`;
        });

        // Đặt mặc định ngày check-in là hôm nay, check-out là ngày mai
        const today = new Date().toISOString().split('T')[0];
        const tomorrow = new Date(Date.now() + 86400000).toISOString().split('T')[0];
        
        document.getElementById('check-in-date').value = today;
        document.getElementById('check-in-date').min = today;
        document.getElementById('check-out-date').value = tomorrow;
        document.getElementById('check-out-date').min = today;

        document.getElementById('booking-form').addEventListener('submit', handleCreateBooking);
    } catch (err) {
        console.error('Lỗi khởi tạo form:', err);
    }
}

async function loadAvailableRooms() {
    const hotelId = document.getElementById('booking-hotel-select').value;
    const roomSelect = document.getElementById('booking-room-select');
    roomSelect.innerHTML = '<option value="">-- Chọn phòng --</option>';

    if (!hotelId) return;

    try {
        const rooms = await api.getRoomsByHotel(hotelId);
        const availableRooms = rooms.filter(r => r.isAvailable);

        if (availableRooms.length === 0) {
            roomSelect.innerHTML = '<option value="">Hết phòng khả dụng</option>';
            return;
        }

        availableRooms.forEach(room => {
            roomSelect.innerHTML += `
                <option value="${room.id}">
                    Phòng ${room.roomNumber} (${room.type}) - ${Number(room.pricePerNight).toLocaleString('vi-VN')} đ/đêm
                </option>`;
        });
    } catch (err) {
        alert('Lỗi khi tải phòng: ' + err.message);
    }
}

async function handleCreateBooking(e) {
    e.preventDefault();

    const roomId = document.getElementById('booking-room-select').value;
    const checkInDate = document.getElementById('check-in-date').value;
    const checkOutDate = document.getElementById('check-out-date').value;

    if (!roomId) {
        alert('Vui lòng chọn phòng!');
        return;
    }

    const bookingPayload = {
        userId: CURRENT_USER_ID,
        room: { id: parseInt(roomId) },
        checkInDate: checkInDate,
        checkOutDate: checkOutDate
    };

    try {
        await api.createBooking(bookingPayload);
        alert('Đặt phòng thành công!');
        document.getElementById('booking-form').reset();
        loadUserBookingHistory();
    } catch (err) {
        alert('Đặt phòng thất bại: ' + err.message);
    }
}

async function loadUserBookingHistory() {
    const tbody = document.getElementById('user-booking-list');
    if (!tbody) return;

    try {
        const bookings = await api.getUserBookings(CURRENT_USER_ID);
        if (bookings.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7">Bạn chưa có đơn đặt phòng nào.</td></tr>';
            return;
        }

        tbody.innerHTML = bookings.map(b => `
            <tr>
                <td>#${b.id}</td>
                <td>${b.room.hotel.name} - Phòng ${b.room.roomNumber} (${b.room.type})</td>
                <td>${b.checkInDate}</td>
                <td>${b.checkOutDate}</td>
                <td><strong>${Number(b.totalPrice).toLocaleString('vi-VN')} đ</strong></td>
                <td><span class="badge status-${b.status.toLowerCase()}">${b.status}</span></td>
                <td>
                    ${b.status !== 'CANCELLED' ? `<button onclick="handleCancelBooking(${b.id})" class="btn-delete">Hủy đơn</button>` : 'N/A'}
                </td>
            </tr>
        `).join('');
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7">Lỗi tải lịch sử: ${err.message}</td></tr>`;
    }
}

async function handleCancelBooking(bookingId) {
    if (!confirm('Bạn có chắc muốn hủy đơn đặt phòng này?')) return;

    try {
        await api.cancelBooking(bookingId);
        alert('Đã hủy đơn thành công!');
        loadUserBookingHistory();
    } catch (err) {
        alert('Không thể hủy đơn: ' + err.message);
    }
}