const express = require('express');
const router = express.Router();

let promotions = [
    {
        id: 1,
        code: 'SUMMER2026',
        discountPercent: 15,
        minOrderValue: 500000,
        startDate: '2026-06-01',
        endDate: '2026-08-31',
        usageLimit: 100,
        usedCount: 12,
        isActive: true,
        description: 'Giảm 15% cho tất cả các phòng vào mùa hè'
    },
    {
        id: 2,
        code: 'WELCOME',
        discountPercent: 10,
        minOrderValue: 200000,
        startDate: '2026-01-01',
        endDate: '2026-12-31',
        usageLimit: 500,
        usedCount: 85,
        isActive: true,
        description: 'Mã chào mừng khách hàng mới'
    }
];

// [FR-51] Xem danh sách khuyến mãi đang áp dụng
router.get('/promotions', async (req, res) => {
    try {
        const activePromos = promotions.filter(p => p.isActive);
        res.json({ status: 'success', data: activePromos });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-52] Xem chi tiết khuyến mãi
router.get('/promotions/:id', async (req, res) => {
    try {
        const promoId = parseInt(req.params.id);
        const promo = promotions.find(p => p.id === promoId);
        if (!promo) {
            return res.status(404).json({ status: 'error', message: 'Không tìm thấy chương trình khuyến mãi' });
        }
        res.json({ status: 'success', data: promo });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-53 & FR-54] Kiểm tra và Áp dụng mã khuyến mãi
router.post('/promotions/apply', async (req, res) => {
    try {
        const { code, orderTotal } = req.body;
        const promo = promotions.find(p => p.code === code && p.isActive);

        if (!promo) {
            return res.status(400).json({ status: 'error', message: 'Mã khuyến mãi không tồn tại hoặc đã hết hạn.' });
        }

        const currentDate = new Date().toISOString().split('T')[0];
        if (currentDate < promo.startDate || currentDate > promo.endDate) {
            return res.status(400).json({ status: 'error', message: 'Mã khuyến mãi đã hết thời gian hiệu lực.' });
        }

        if (orderTotal && orderTotal < promo.minOrderValue) {
            return res.status(400).json({ 
                status: 'error', 
                message: `Đơn hàng tối thiểu phải từ ${promo.minOrderValue.toLocaleString()} VNĐ để sử dụng mã này.` 
            });
        }

        if (promo.usedCount >= promo.usageLimit) {
            return res.status(400).json({ status: 'error', message: 'Mã khuyến mãi đã hết lượt sử dụng.' });
        }

        const discountAmount = (orderTotal * promo.discountPercent) / 100;

        res.json({ 
            status: 'success', 
            message: 'Áp dụng mã khuyến mãi thành công!',
            data: {
                code: promo.code,
                discountPercent: promo.discountPercent,
                discountAmount: discountAmount
            }
        });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

// [FR-55] Quản trị viên tạo, sửa, kích hoạt hoặc ngừng chương trình khuyến mãi
router.post('/admin/promotions', async (req, res) => {
    try {
        const { code, discountPercent, minOrderValue, startDate, endDate, usageLimit, description } = req.body;
        
        const newPromo = {
            id: promotions.length + 1,
            code,
            discountPercent,
            minOrderValue,
            startDate,
            endDate,
            usageLimit,
            usedCount: 0,
            isActive: true,
            description
        };

        promotions.push(newPromo);
        res.status(201).json({ status: 'success', message: 'Tạo chương trình khuyến mãi thành công!', data: newPromo });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

router.put('/admin/promotions/:id', async (req, res) => {
    try {
        const promoId = parseInt(req.params.id);
        const index = promotions.findIndex(p => p.id === promoId);
        
        if (index === -1) {
            return res.status(404).json({ status: 'error', message: 'Không tìm thấy khuyến mãi để cập nhật.' });
        }

        promotions[index] = { ...promotions[index], ...req.body };
        res.json({ status: 'success', message: 'Cập nhật chương trình khuyến mãi thành công!', data: promotions[index] });
    } catch (error) {
        res.status(500).json({ status: 'error', message: error.message });
    }
});

module.exports = router;
