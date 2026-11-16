package com.hotelbooking.service;

import com.hotelbooking.model.Hotel;
import com.hotelbooking.repository.HotelRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);
    private final HotelRepository hotelRepository;

    public HotelService(HotelRepository hotelRepository) {
        this.hotelRepository = hotelRepository;
    }

    @PostConstruct
    @Transactional
    public void initSeedDataIfEmpty() {
        if (hotelRepository.count() == 0) {
            log.info("Hotels table is empty, initializing default hotels into PostgreSQL...");
            hotelRepository.save(new Hotel(
                    null,
                    "Grand Vista Hotel",
                    "Được yêu thích",
                    "featured",
                    4.8,
                    256,
                    "Quận 1, TP. Hồ Chí Minh",
                    "TP. Hồ Chí Minh",
                    1850000L,
                    null,
                    "assets/images/hotel_grand_vista.jpg",
                    "Khách sạn cao cấp giữa lòng Sài Gòn với tầm nhìn toàn cảnh ngoạn mục và dịch vụ 5 sao sang trọng."
            ));

            hotelRepository.save(new Hotel(
                    null,
                    "Sunrise Resort Phú Quốc",
                    "Giảm giá 20%",
                    "discount",
                    4.7,
                    312,
                    "Phú Quốc, Kiên Giang",
                    "Phú Quốc",
                    2500000L,
                    3125000L,
                    "assets/images/hotel_sunrise_resort.jpg",
                    "Resort nghỉ dưỡng bên bãi biển cát trắng tinh khôi Phú Quốc với hồ bơi vô cực và khuôn viên nhiệt đới xanh mát."
            ));

            hotelRepository.save(new Hotel(
                    null,
                    "The Lumiere Hotel",
                    "Phổ biến",
                    "popular",
                    4.6,
                    196,
                    "Đà Nẵng",
                    "Đà Nẵng",
                    1650000L,
                    null,
                    "assets/images/hotel_the_lumiere.jpg",
                    "Toạ lạc ngay bờ sông Hàn và trung tâm thành phố Đà Nẵng, thiết kế hiện đại với Sky Bar và hồ bơi tầng thượng."
            ));

            hotelRepository.save(new Hotel(
                    null,
                    "Melia Vinpearl Nha Trang",
                    "Mới",
                    "new",
                    4.9,
                    420,
                    "Nha Trang, Khánh Hòa",
                    "Nha Trang",
                    2800000L,
                    null,
                    "assets/images/hotel_melia_nha_trang.jpg",
                    "Tổ hợp nghỉ dưỡng chuẩn quốc tế hướng biển Nha Trang với bãi biển riêng và hệ thống ẩm thực thượng hạng."
            ));
            log.info("Default hotels successfully seeded into PostgreSQL.");
        }
    }

    public List<Hotel> getFeaturedHotels() {
        return hotelRepository.findAll().stream().filter(h -> !Boolean.FALSE.equals(h.getActive())).toList();
    }

    public List<Hotel> getAllHotels() {
        return hotelRepository.findAll();
    }

    public List<Hotel> searchHotels(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getFeaturedHotels();
        }
        return hotelRepository.searchHotels(query.trim()).stream().filter(h -> !Boolean.FALSE.equals(h.getActive())).toList();
    }

    public Hotel getHotelById(Long id) {
        return hotelRepository.findById(id).orElse(null);
    }

    public Hotel saveHotel(Hotel hotel) {
        hotel.setUpdatedAt(java.time.LocalDateTime.now());
        return hotelRepository.save(hotel);
    }

    public void deleteHotel(Long id) {
        hotelRepository.deleteById(id);
    }
}
