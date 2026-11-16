package com.hotelbooking.controller;

import com.hotelbooking.model.Hotel;
import com.hotelbooking.service.HotelService;
import com.hotelbooking.service.AuthSessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@CrossOrigin(origins = "*")
public class HotelController {

    private final HotelService hotelService;
    private final AuthSessionService sessions;

    public HotelController(HotelService hotelService, AuthSessionService sessions) {
        this.hotelService = hotelService;
        this.sessions = sessions;
    }

    @GetMapping("/featured")
    public ResponseEntity<List<Hotel>> getFeaturedHotels() {
        return ResponseEntity.ok(hotelService.getFeaturedHotels());
    }

    @GetMapping
    public ResponseEntity<List<Hotel>> getAllHotelsForManagement(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!sessions.isAdmin(authorization)) return authorization == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        return ResponseEntity.ok(hotelService.getAllHotels());
    }

    @GetMapping("/search")
    public ResponseEntity<List<Hotel>> searchHotels(@RequestParam(value = "q", required = false) String query) {
        return ResponseEntity.ok(hotelService.searchHotels(query));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hotel> getHotelById(@PathVariable Long id) {
        Hotel hotel = hotelService.getHotelById(id);
        if (hotel == null || Boolean.FALSE.equals(hotel.getActive())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(hotel);
    }

    @PostMapping
    public ResponseEntity<Hotel> createHotel(@RequestBody Hotel hotel, @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!sessions.isAdmin(authorization)) return authorization == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (hotel.getStars() != null && hotel.getStars() > 0 && !Boolean.TRUE.equals(hotel.getVerified())) return ResponseEntity.badRequest().build();
        hotel.setId(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(hotelService.saveHotel(hotel));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Hotel> updateHotel(@PathVariable Long id, @RequestBody Hotel changes, @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!sessions.isAdmin(authorization)) return authorization == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Hotel hotel = hotelService.getHotelById(id);
        if (hotel == null) return ResponseEntity.notFound().build();
        if (!java.util.Objects.equals(hotel.getStars(), changes.getStars()) && !Boolean.TRUE.equals(changes.getVerified())) return ResponseEntity.badRequest().build();
        hotel.setName(changes.getName());
        hotel.setLocation(changes.getLocation());
        hotel.setCity(changes.getCity());
        hotel.setDescription(changes.getDescription());
        hotel.setImage(changes.getImage());
        hotel.setAddress(changes.getAddress());
        hotel.setPhone(changes.getPhone());
        hotel.setEmail(changes.getEmail());
        hotel.setImages(changes.getImages());
        hotel.setAmenities(changes.getAmenities());
        hotel.setStars(changes.getStars());
        hotel.setVerified(changes.getVerified());
        hotel.setCheckin(changes.getCheckin());
        hotel.setCheckout(changes.getCheckout());
        hotel.setCancellationPolicy(changes.getCancellationPolicy());
        hotel.setStayPolicy(changes.getStayPolicy());
        hotel.setActive(changes.getActive());
        return ResponseEntity.ok(hotelService.saveHotel(hotel));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!sessions.isAdmin(authorization)) return authorization == null ? ResponseEntity.status(HttpStatus.UNAUTHORIZED).build() : ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        if (hotelService.getHotelById(id) == null) return ResponseEntity.notFound().build();
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}
