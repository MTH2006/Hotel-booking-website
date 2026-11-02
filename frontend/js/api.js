const API_BASE_URL = 'http://localhost:8080/api';

const api = {
    // --- API Hotel ---
    async getHotels() {
        const res = await fetch(`${API_BASE_URL}/hotels`);
        if (!res.ok) throw new Error('Không thể tải danh sách khách sạn');
        return await res.json();
    },

    // --- API Room ---
    async getRoomsByHotel(hotelId) {
        const res = await fetch(`${API_BASE_URL}/hotels/${hotelId}/rooms`);
        if (!res.ok) throw new Error('Không thể tải danh sách phòng');
        return await res.json();
    },

    async createRoom(hotelId, roomData) {
        const res = await fetch(`${API_BASE_URL}/hotels/${hotelId}/rooms`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(roomData)
        });
        if (!res.ok) throw new Error((await res.json()).message || 'Thêm phòng thất bại');
        return await res.json();
    },

    async updateRoom(roomId, roomData) {
        const res = await fetch(`${API_BASE_URL}/hotels/rooms/${roomId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(roomData)
        });
        if (!res.ok) throw new Error('Cập nhật phòng thất bại');
        return await res.json();
    },

    async deleteRoom(roomId) {
        const res = await fetch(`${API_BASE_URL}/hotels/rooms/${roomId}`, {
            method: 'DELETE'
        });
        if (!res.ok) throw new Error('Xóa phòng thất bại');
    },

    // --- API Booking ---
    async createBooking(bookingData) {
        const res = await fetch(`${API_BASE_URL}/bookings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(bookingData)
        });
        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Đặt phòng thất bại');
        }
        return await res.json();
    },

    async getUserBookings(userId) {
        const res = await fetch(`${API_BASE_URL}/bookings/user/${userId}`);
        if (!res.ok) throw new Error('Không thể tải lịch sử đặt phòng');
        return await res.json();
    },

    async cancelBooking(bookingId) {
        const res = await fetch(`${API_BASE_URL}/bookings/${bookingId}/cancel`, {
            method: 'PUT'
        });
        if (!res.ok) throw new Error('Hủy đặt phòng thất bại');
        return await res.json();
    }
};