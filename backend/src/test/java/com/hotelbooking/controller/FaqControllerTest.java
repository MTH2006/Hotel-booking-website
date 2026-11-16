
package com.hotelbooking.controller;

import com.hotelbooking.model.Faq;
import com.hotelbooking.model.User;
import com.hotelbooking.security.AccessControlService;
import com.hotelbooking.service.FaqService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FaqControllerTest {

    private FaqService faqService;
    private AccessControlService accessControl;
    private FaqController controller;

    private User admin;
    private User customer;

    @BeforeEach
    void setUp() {

        faqService = mock(FaqService.class);
        accessControl = mock(AccessControlService.class);

        controller = new FaqController(
                faqService,
                accessControl
        );

        admin = createUser(1L, "ADMIN");
        customer = createUser(2L, "USER");
    }

    // FR-96: Khach hang co the xem FAQ cong khai
    @Test
    void shouldGetFaqsWithoutLogin() {

        Faq faq = createFaq();

        when(faqService.getAllFaqs())
                .thenReturn(List.of(faq));

        ResponseEntity<List<Faq>> response =
                controller.getAllFaqs();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());

        verify(faqService).getAllFaqs();
        verifyNoInteractions(accessControl);
    }

    // FR-96: Danh sach FAQ rong van tra ve 200
    @Test
    void shouldReturnEmptyFaqList() {

        when(faqService.getAllFaqs())
                .thenReturn(List.of());

        ResponseEntity<List<Faq>> response =
                controller.getAllFaqs();

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().isEmpty());
    }

    // FR-96: Chua dang nhap khong duoc them FAQ
    @Test
    void shouldRejectCreateWithoutLogin() {

        when(accessControl.authenticate(null))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.createFaq(createFaq(), null);

        assertEquals(401, response.getStatusCode().value());

        verifyNoInteractions(faqService);
    }

    // FR-96: Khach hang khong duoc them FAQ
    @Test
    void shouldRejectCreateForCustomer() {

        when(accessControl.authenticate("Bearer customer"))
                .thenReturn(Optional.of(customer));

        ResponseEntity<?> response =
                controller.createFaq(
                        createFaq(),
                        "Bearer customer"
                );

        assertEquals(403, response.getStatusCode().value());

        verifyNoInteractions(faqService);
    }

    // FR-96: ADMIN them FAQ thanh cong
    @Test
    void shouldAllowAdminToCreateFaq() {

        when(accessControl.authenticate("Bearer admin"))
                .thenReturn(Optional.of(admin));

        when(accessControl.isAdmin(admin))
                .thenReturn(true);

        Faq request = createFaq();
        Faq saved = createFaq();
        saved.setId(7L);

        when(faqService.createFaq(request))
                .thenReturn(saved);

        ResponseEntity<?> response =
                controller.createFaq(
                        request,
                        "Bearer admin"
                );

        assertEquals(201, response.getStatusCode().value());
        assertSame(saved, response.getBody());

        verify(faqService).createFaq(request);
    }

    // FR-96: Du lieu FAQ khong hop le tra ve 400
    @Test
    void shouldRejectInvalidFaqCreation() {

        mockAdmin();

        Faq request = createFaq();
        request.setQuestion("   ");

        when(faqService.createFaq(request))
                .thenThrow(new IllegalArgumentException(
                        "Cau hoi khong duoc de trong."
                ));

        ResponseEntity<?> response =
                controller.createFaq(
                        request,
                        "Bearer admin"
                );

        assertEquals(400, response.getStatusCode().value());
    }

    // FR-96: Chua dang nhap khong duoc sua FAQ
    @Test
    void shouldRejectUpdateWithoutLogin() {

        when(accessControl.authenticate(null))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.updateFaq(7L, createFaq(), null);

        assertEquals(401, response.getStatusCode().value());

        verifyNoInteractions(faqService);
    }

    // FR-96: Khach hang khong duoc sua FAQ
    @Test
    void shouldRejectUpdateForCustomer() {

        when(accessControl.authenticate("Bearer customer"))
                .thenReturn(Optional.of(customer));

        ResponseEntity<?> response =
                controller.updateFaq(
                        7L,
                        createFaq(),
                        "Bearer customer"
                );

        assertEquals(403, response.getStatusCode().value());

        verifyNoInteractions(faqService);
    }

    // FR-96: ADMIN sua FAQ thanh cong
    @Test
    void shouldAllowAdminToUpdateFaq() {

        mockAdmin();

        Faq request = createFaq();
        request.setAnswer("Cau tra loi da cap nhat.");

        Faq updated = createFaq();
        updated.setId(7L);
        updated.setAnswer("Cau tra loi da cap nhat.");

        when(faqService.updateFaq(7L, request))
                .thenReturn(updated);

        ResponseEntity<?> response =
                controller.updateFaq(
                        7L,
                        request,
                        "Bearer admin"
                );

        assertEquals(200, response.getStatusCode().value());
        assertSame(updated, response.getBody());

        verify(faqService).updateFaq(7L, request);
    }

    // FR-96: Khong duoc sua FAQ khong ton tai
    @Test
    void shouldReturn404WhenUpdatingMissingFaq() {

        mockAdmin();

        Faq request = createFaq();

        when(faqService.updateFaq(999L, request))
                .thenThrow(new NoSuchElementException(
                        "Khong tim thay FAQ."
                ));

        ResponseEntity<?> response =
                controller.updateFaq(
                        999L,
                        request,
                        "Bearer admin"
                );

        assertEquals(404, response.getStatusCode().value());
    }

    // FR-96: Du lieu cap nhat sai tra ve 400
    @Test
    void shouldRejectInvalidFaqUpdate() {

        mockAdmin();

        Faq request = createFaq();
        request.setAnswer("");

        when(faqService.updateFaq(7L, request))
                .thenThrow(new IllegalArgumentException(
                        "Cau tra loi khong hop le."
                ));

        ResponseEntity<?> response =
                controller.updateFaq(
                        7L,
                        request,
                        "Bearer admin"
                );

        assertEquals(400, response.getStatusCode().value());
    }

    // FR-96: Chua dang nhap khong duoc xoa FAQ
    @Test
    void shouldRejectDeleteWithoutLogin() {

        when(accessControl.authenticate(null))
                .thenReturn(Optional.empty());

        ResponseEntity<?> response =
                controller.deleteFaq(7L, null);

        assertEquals(401, response.getStatusCode().value());

        verifyNoInteractions(faqService);
    }

    // FR-96: Khach hang khong duoc xoa FAQ
    @Test
    void shouldRejectDeleteForCustomer() {

        when(accessControl.authenticate("Bearer customer"))
                .thenReturn(Optional.of(customer));

        ResponseEntity<?> response =
                controller.deleteFaq(
                        7L,
                        "Bearer customer"
                );

        assertEquals(403, response.getStatusCode().value());

        verifyNoInteractions(faqService);
    }

    // FR-96: ADMIN xoa FAQ thanh cong
    @Test
    void shouldAllowAdminToDeleteFaq() {

        mockAdmin();

        ResponseEntity<?> response =
                controller.deleteFaq(
                        7L,
                        "Bearer admin"
                );

        assertEquals(204, response.getStatusCode().value());

        verify(faqService).deleteFaq(7L);
    }

    // FR-96: Ma FAQ khong hop le tra ve 400
    @Test
    void shouldRejectInvalidFaqId() {

        mockAdmin();

        doThrow(new IllegalArgumentException(
                "Ma FAQ khong hop le."
        )).when(faqService).deleteFaq(0L);

        ResponseEntity<?> response =
                controller.deleteFaq(
                        0L,
                        "Bearer admin"
                );

        assertEquals(400, response.getStatusCode().value());
    }

    // FR-96: Xoa FAQ khong ton tai tra ve 404
    @Test
    void shouldReturn404WhenDeletingMissingFaq() {

        mockAdmin();

        doThrow(new NoSuchElementException(
                "Khong tim thay FAQ."
        )).when(faqService).deleteFaq(999L);

        ResponseEntity<?> response =
                controller.deleteFaq(
                        999L,
                        "Bearer admin"
                );

        assertEquals(404, response.getStatusCode().value());
    }

    // Gia lap tai khoan ADMIN da dang nhap
    private void mockAdmin() {

        when(accessControl.authenticate("Bearer admin"))
                .thenReturn(Optional.of(admin));

        when(accessControl.isAdmin(admin))
                .thenReturn(true);
    }

    // Tao tai khoan gia lap
    private User createUser(Long id, String role) {

        User user = new User();

        user.setId(id);
        user.setRole(role);
        user.setActive(true);

        return user;
    }

    // Tao FAQ mau
    private Faq createFaq() {

        Faq faq = new Faq();

        faq.setQuestion("Lam sao de dat phong?");
        faq.setAnswer(
                "Chon khach san, chon phong va xac nhan."
        );

        return faq;
    }
}
