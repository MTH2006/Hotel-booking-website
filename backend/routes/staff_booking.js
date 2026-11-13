const express = require('express');
const router = express.Router();

let staffBookings = [
    { id: 1, bookingCode: 'BK101', customerName: 'Nguyễn Văn A', hotel: 'Hotel Ha Long', date: '2026-06-10', status: 'Pending' },
    { id: 2, bookingCode: 'BK102', customerName: 'Trần Thị B', hotel: 'Hotel Da Nang', date: '2026-06-12', status: 'Confirmed' }
];

// [FR-71] Xem danh sách đơn đặt phòng thuộc phạm vi quản lý của nhân viên
router.get('/staff/bookings', async (req, res) => {
    try {
        res.json({ status: 'success', data: staffBookings });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-72] Tìm kiếm đơn đặt phòng theo mã đơn, khách hàng, khách sạn hoặc thời gian
router.get('/staff/bookings/search', async (req, res) => {
    try {
        const { keyword, hotelId, date } = req.query;
        let results = staffBookings;

        if (keyword) {
            const lowerKey = keyword.toLowerCase();
            results = results.filter(b => 
                b.bookingCode.toLowerCase().includes(lowerKey) || 
                b.customerName.toLowerCase().includes(lowerKey) ||
                b.hotel.toLowerCase().includes(lowerKey)
            );
        }

        if (date) {
            results = results.filter(b => b.date === date);
        }

        res.json({ status: 'success', data: results });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-73] Xác nhận đơn đặt phòng theo quy trình nghiệp vụ
router.patch('/staff/bookings/:id/confirm', async (req, res) => {
    try {
        const bookingId = parseInt(req.params.id);
        const booking = staffBookings.find(b => b.id === bookingId);
        if (!booking) return res.status(404).json({ status: 'error', message: 'Không tìm thấy đơn đặt phòng.' });

        booking.status = 'Confirmed';
        res.json({ status: 'success', message: 'Đã xác nhận đơn đặt phòng thành công!', data: booking });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-74] Từ chối yêu cầu đặt phòng (ghi nhận lý do)
router.patch('/staff/bookings/:id/reject', async (req, res) => {
    try {
        const bookingId = parseInt(req.params.id);
        const { reason } = req.body;
        const booking = staffBookings.find(b => b.id === bookingId);
        if (!booking) return res.status(404).json({ status: 'error', message: 'Không tìm thấy đơn đặt phòng.' });

        booking.status = 'Rejected';
        booking.rejectReason = reason;
        res.json({ status: 'success', message: 'Đã từ chối đơn đặt phòng.', data: { booking, reason } });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-75] Cập nhật trạng thái đơn theo tiến trình xử lý thực tế
router.patch('/staff/bookings/:id/status', async (req, res) => {
    try {
        const bookingId = parseInt(req.params.id);
        const { status } = req.body;
        const booking = staffBookings.find(b => b.id === bookingId);
        if (!booking) return res.status(404).json({ status: 'error', message: 'Không tìm thấy đơn đặt phòng.' });

        booking.status = status;
        res.json({ status: 'success', message: 'Cập nhật trạng thái thành công!', data: booking });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-76] Xử lý yêu cầu hủy phòng theo chính sách
router.post('/staff/bookings/:id/cancel', async (req, res) => {
    try {
        const bookingId = parseInt(req.params.id);
        const booking = staffBookings.find(b => b.id === bookingId);
        if (!booking) return res.status(404).json({ status: 'error', message: 'Không tìm thấy đơn đặt phòng.' });

        booking.status = 'Cancelled';
        res.json({ status: 'success', message: 'Đã xử lý yêu cầu hủy phòng!', data: booking });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-77] Quản lý nhận phòng (Ghi nhận khách đã check-in)
router.patch('/staff/bookings/:id/check-in', async (req, res) => {
    try {
        const bookingId = parseInt(req.params.id);
        const booking = staffBookings.find(b => b.id === bookingId);
        if (!booking) return res.status(404).json({ status: 'error', message: 'Không tìm thấy đơn đặt phòng.' });

        booking.status = 'Checked-In';
        res.json({ status: 'success', message: 'Check-in thành công!', data: booking });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-78] Quản lý trả phòng (Ghi nhận khách đã check-out)
router.patch('/staff/bookings/:id/check-out', async (req, res) => {
    try {
        const bookingId = parseInt(req.params.id);
        const booking = staffBookings.find(b => b.id === bookingId);
        if (!booking) return res.status(404).json({ status: 'error', message: 'Không tìm thấy đơn đặt phòng.' });

        booking.status = 'Checked-Out';
        res.json({ status: 'success', message: 'Check-out thành công!', data: booking });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

module.exports = router;
