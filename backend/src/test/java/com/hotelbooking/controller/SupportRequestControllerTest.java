
package com.hotelbooking.controller;

import com.hotelbooking.model.SupportRequest;
import com.hotelbooking.service.SupportRequestService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupportRequestControllerTest {

    private SupportRequestService supportRequestService;
    private SupportRequestController controller;

    @BeforeEach
    void setUp() {
        supportRequestService = mock(SupportRequestService.class);
        controller = new SupportRequestController(supportRequestService);
    }

    // FR-94: Gui yeu cau ho tro thanh cong
    @Test
    void shouldCreateSupportRequestSuccessfully() {

        SupportRequest request = createValidRequest();

        SupportRequest saved = createValidRequest();
        saved.setId(7L);
        saved.setStatus("PENDING");

        when(supportRequestService.createSupportRequest(request))
                .thenReturn(saved);

        ResponseEntity<Map<String, Object>> response =
                controller.createSupportRequest(request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(true, response.getBody().get("success"));
        assertEquals(7L, response.getBody().get("requestId"));
        assertEquals("PENDING", response.getBody().get("status"));

        verify(supportRequestService).createSupportRequest(request);
    }

    // FR-94: Tra ve 400 khi du lieu khong hop le
    @Test
    void shouldRejectInvalidSupportRequest() {

        SupportRequest request = createValidRequest();
        request.setCustomerEmail("invalid-email");

        when(supportRequestService.createSupportRequest(request))
                .thenThrow(new IllegalArgumentException(
                        "Email lien he khong hop le."
                ));

        ResponseEntity<Map<String, Object>> response =
                controller.createSupportRequest(request);

        assertEquals(400, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(false, response.getBody().get("success"));
    }

    // FR-94: Khong tra ve email va noi dung nhay cam
    @Test
    void shouldNotExposeCustomerDataInResponse() {

        SupportRequest request = createValidRequest();

        SupportRequest saved = createValidRequest();
        saved.setId(8L);

        when(supportRequestService.createSupportRequest(request))
                .thenReturn(saved);

        ResponseEntity<Map<String, Object>> response =
                controller.createSupportRequest(request);

        assertEquals(201, response.getStatusCode().value());
        assertNotNull(response.getBody());

        assertFalse(response.getBody().containsKey("customerEmail"));
        assertFalse(response.getBody().containsKey("messageContent"));
        assertFalse(response.getBody().containsKey("customerName"));
    }

    private SupportRequest createValidRequest() {

        SupportRequest request = new SupportRequest();

        request.setCustomerName("Nguyen Van A");
        request.setCustomerEmail("customer@example.com");
        request.setSubject("Ho tro dat phong");
        request.setMessage("Toi can ho tro don dat phong.");

        return request;
    }
}
