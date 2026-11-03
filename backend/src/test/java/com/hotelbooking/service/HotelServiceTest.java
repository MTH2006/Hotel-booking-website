package com.hotelbooking.service;

import com.hotelbooking.model.Hotel;
import com.hotelbooking.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class HotelServiceTest {

    private HotelRepository hotelRepository;
    private HotelService hotelService;

    @BeforeEach
    void setUp() {
        hotelRepository = mock(HotelRepository.class);
        hotelService = new HotelService(hotelRepository);
    }

    @Test
    void testGetFeaturedHotels() {
        Hotel h1 = new Hotel();
        h1.setId(1L);
        h1.setName("Grand Vista");

        when(hotelRepository.findAll()).thenReturn(List.of(h1));

        List<Hotel> result = hotelService.getFeaturedHotels();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Grand Vista", result.get(0).getName());
    }

    @Test
    void testSearchHotels() {
        Hotel h1 = new Hotel();
        h1.setName("Melia Vinpearl");
        h1.setCity("Nha Trang");

        when(hotelRepository.searchHotels("Nha Trang")).thenReturn(List.of(h1));

        List<Hotel> result = hotelService.searchHotels("Nha Trang");
        assertEquals(1, result.size());
        assertEquals("Melia Vinpearl", result.get(0).getName());
    }

    @Test
    void testSaveHotel() {
        Hotel h = new Hotel();
        h.setName("New Luxury Hotel");

        when(hotelRepository.save(h)).thenReturn(h);

        Hotel saved = hotelService.saveHotel(h);
        assertNotNull(saved);
        assertEquals("New Luxury Hotel", saved.getName());
    }
}
