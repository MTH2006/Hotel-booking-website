
package com.hotelbooking.controller;

import com.hotelbooking.model.SupportRequest;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.SupportRequestService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupportRequestManagementControllerTest {

    private SupportRequestService supportRequestService;
    private AccessControlService accessControl;
    private SupportRequestManagementController controller;

    private User admin;
    private User manager;
    private User customer;

    @BeforeEach
    void setUp() {

        supportRequestService = mock(SupportRequestService.class);
        accessControl = mock(AccessControlService.class);

        controller = new SupportRequestManagementController(
                supportRequestService,
                accessControl
        );

        admin = createUser(1L, "ADMIN");
        manager = createUser(2L, "MANAGER");
        customer = createUser(3L, "USER");
    }

    // FR-95: Chua dang nhap khong duoc xem danh sach
    @Test
    void shouldRejectGetRequestsWithoutLogin() {

        when(accessControl.authenticate(null))
                .thenReturn(Optional.empty());

        ResponseEntity<List<SupportRequest>> response =
                controller.getAllSupportRequests(null);

        assertEquals(401, response.getStatusCode().value());

        verifyNoInteractions(supportRequestService);
    }

    // FR-95: Khach hang khong duoc xem danh sach
    @Test
    void shouldRejectGetRequestsForCustomer() {

        when(accessControl.authenticate("Bearer customer"))
                .thenReturn(Optional.of(customer));

        ResponseEntity<List<SupportRequest>> response =
                controller.getAllSupportRequests("Bearer customer");

        assertEquals(403, response.getStatusCode().value());

        verifyNoInteractions(supportRequestService);
    }

    // FR-95: MANAGER duoc xem danh sach
    @Test
    void shouldAllowManagerToGetRequests() {

        when(accessControl.authenticate("Bearer manager"))
                .thenReturn(Optional.of(manager));

        when(accessControl.isManagerOrAdmin(manager))
                .thenReturn(true);

        SupportRequest request = createSupportRequest();

        when(supportRequestService.getAllSupportRequests())
                .thenReturn(List.of(request));

        ResponseEntity<List<SupportRequest>> response =
                controller.getAllSupportRequests("Bearer manager");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(supportRequestService).getAllSupportRequests();
    }

    // FR-95: ADMIN duoc xem danh sach
    @Test
    void shouldAllowAdminToGetRequests() {

        when(accessControl.authenticate("Bearer admin"))
                .thenReturn(Optional.of(admin));

        when(accessControl.isManagerOrAdmin(admin))
                .thenReturn(true);

        when(supportRequestService.getAllSupportRequests())
                .thenReturn(List.of());

        ResponseEntity<List<SupportRequest>> response =
                controller.getAllSupportRequests("Bearer admin");

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());

        verify(supportRequestService).getAllSupportRequests();
    }

    // FR-95: Khong cho tiep nhan khi chua dang nhap
    @Test
    void shouldRejectAcceptWithoutLogin() {

        when(accessControl.authenticate(null))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.acceptSupportRequest(7L, null);

        assertEquals(401, response.getStatusCode().value());

        verifyNoInteractions(supportRequestService);
    }

    // FR-95: Khach hang khong duoc tiep nhan
    @Test
    void shouldRejectAcceptForCustomer() {

        when(accessControl.authenticate("Bearer customer"))
                .thenReturn(Optional.of(customer));

        ResponseEntity<?> response =
                controller.acceptSupportRequest(
                        7L, "Bearer customer"
                );

        assertEquals(403, response.getStatusCode().value());

        verifyNoInteractions(supportRequestService);
    }

    // FR-95: MANAGER tiep nhan yeu cau thanh cong
    @Test
    void shouldAllowManagerToAcceptRequest() {

        when(accessControl.authenticate("Bearer manager"))
                .thenReturn(Optional.of(manager));

        when(accessControl.isManagerOrAdmin(manager))
                .thenReturn(true);

        SupportRequest request = createSupportRequest();
        request.setStatus("IN_PROGRESS");

        when(supportRequestService.acceptSupportRequest(7L))
                .thenReturn(request);

        ResponseEntity<?> response =
                controller.acceptSupportRequest(
                        7L, "Bearer manager"
                );

        assertEquals(200, response.getStatusCode().value());

        Map<?, ?> body = (Map<?, ?>) response.getBody();

        assertNotNull(body);
        assertEquals(true, body.get("success"));
        assertEquals(7L, body.get("requestId"));
        assertEquals("IN_PROGRESS", body.get("status"));

        verify(supportRequestService).acceptSupportRequest(7L);
    }

    // FR-95: ADMIN tiep nhan yeu cau thanh cong
    @Test
    void shouldAllowAdminToAcceptRequest() {

        when(accessControl.authenticate("Bearer admin"))
                .thenReturn(Optional.of(admin));

        when(accessControl.isManagerOrAdmin(admin))
                .thenReturn(true);

        SupportRequest request = createSupportRequest();
        request.setStatus("IN_PROGRESS");

        when(supportRequestService.acceptSupportRequest(7L))
                .thenReturn(request);

        ResponseEntity<?> response =
                controller.acceptSupportRequest(
                        7L, "Bearer admin"
                );

        assertEquals(200, response.getStatusCode().value());

        verify(supportRequestService).acceptSupportRequest(7L);
    }

    // FR-95: Ma yeu cau khong hop le tra ve 400
    @Test
    void shouldReturn400ForInvalidRequestId() {

        when(accessControl.authenticate("Bearer admin"))
                .thenReturn(Optional.of(admin));

        when(accessControl.isManagerOrAdmin(admin))
                .thenReturn(true);

        when(supportRequestService.acceptSupportRequest(0L))
                .thenThrow(new IllegalArgumentException(
                        "Ma yeu cau ho tro khong hop le."
                ));

        ResponseEntity<?> response =
                controller.acceptSupportRequest(
                        0L, "Bearer admin"
                );

        assertEquals(400, response.getStatusCode().value());

        verify(supportRequestService).acceptSupportRequest(0L);
    }

    // FR-95: Yeu cau da duoc tiep nhan tra ve 409
    @Test
    void shouldReturn409ForAlreadyAcceptedRequest() {

        when(accessControl.authenticate("Bearer manager"))
                .thenReturn(Optional.of(manager));

        when(accessControl.isManagerOrAdmin(manager))
                .thenReturn(true);

        when(supportRequestService.acceptSupportRequest(7L))
                .thenThrow(new IllegalStateException(
                        "Yeu cau khong o trang thai cho xu ly."
                ));

        ResponseEntity<?> response =
                controller.acceptSupportRequest(
                        7L, "Bearer manager"
                );

        assertEquals(409, response.getStatusCode().value());

        verify(supportRequestService).acceptSupportRequest(7L);
    }

    // Tao tai khoan gia lap
    private User createUser(Long id, String role) {

        User user = new User();

        user.setId(id);
        user.setRole(role);
        user.setActive(true);

        return user;
    }

    // Tao yeu cau ho tro mau
    private SupportRequest createSupportRequest() {

        SupportRequest request = new SupportRequest();

        request.setId(7L);
        request.setCustomerName("Nguyen Van A");
        request.setCustomerEmail("customer@example.com");
        request.setSubject("Ho tro dat phong");
        request.setMessage("Toi can ho tro don dat phong.");
        request.setStatus("PENDING");

        return request;
    }
}
