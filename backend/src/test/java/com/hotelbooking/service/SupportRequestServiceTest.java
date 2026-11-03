
package com.hotelbooking.service;

import com.hotelbooking.model.SupportRequest;
import com.hotelbooking.repository.SupportRequestRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

class SupportRequestServiceTest {

    private SupportRequestRepository repository;
    private SupportRequestService service;

    @BeforeEach
    void setUp() {
        repository = mock(SupportRequestRepository.class);
        service = new SupportRequestService(repository);

        // Gia lap thao tac luu du lieu
        when(repository.save(any(SupportRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // FR-94: Gui yeu cau ho tro hop le
    @Test
    void shouldCreateSupportRequestSuccessfully() {

        SupportRequest request = createValidRequest();

        SupportRequest result =
                service.createSupportRequest(request);

        assertNotNull(result);
        assertEquals("Nguyen Van A", result.getCustomerName());
        assertEquals("PENDING", result.getStatus());
        assertNotNull(result.getCreatedAt());

        verify(repository, times(1))
                .save(any(SupportRequest.class));
    }

    // FR-94: Tu choi email khong hop le
    @Test
    void shouldRejectInvalidEmail() {

        SupportRequest request = createValidRequest();
        request.setCustomerEmail("email-khong-hop-le");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSupportRequest(request)
        );

        verify(repository, never())
                .save(any(SupportRequest.class));
    }

    // FR-94: Tu choi noi dung trong
    @Test
    void shouldRejectBlankMessage() {

        SupportRequest request = createValidRequest();
        request.setMessage("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSupportRequest(request)
        );

        verify(repository, never())
                .save(any(SupportRequest.class));
    }

    // FR-94: Khach hang khong duoc tu thay doi trang thai
    @Test
    void shouldForcePendingStatus() {

        SupportRequest request = createValidRequest();
        request.setStatus("RESOLVED");
        request.setId(999L);

        SupportRequest result =
                service.createSupportRequest(request);

        assertEquals("PENDING", result.getStatus());
        assertNull(result.getId());

        verify(repository, times(1))
                .save(any(SupportRequest.class));
    }

    // FR-94: Tu choi yeu cau null
    @Test
    void shouldRejectNullRequest() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createSupportRequest(null)
        );

        verify(repository, never())
                .save(any(SupportRequest.class));
    }

    // Tao du lieu mau cho cac bai kiem thu
    private SupportRequest createValidRequest() {

        SupportRequest request = new SupportRequest();

        request.setCustomerName("Nguyen Van A");
        request.setCustomerEmail("nguyenvana@gmail.com");
        request.setSubject("Ho tro dat phong");
        request.setMessage("Toi can ho tro ve don dat phong.");

        return request;
    }
    
    
    // FR-95: Tiep nhan yeu cau dang cho xu ly
    @Test
    void shouldAcceptPendingSupportRequest() {

        SupportRequest request = createValidRequest();
        request.setId(1L);
        request.setStatus("IN_PROGRESS");

        // Gia lap cap nhat thanh cong trong PostgreSQL
        when(repository.acceptIfPending(1L))
                .thenReturn(1);

        when(repository.findById(1L))
                .thenReturn(Optional.of(request));

        SupportRequest result =
                service.acceptSupportRequest(1L);

        assertEquals("IN_PROGRESS", result.getStatus());

        verify(repository, times(1))
                .acceptIfPending(1L);

        verify(repository, times(1))
                .findById(1L);

        verify(repository, never())
                .save(any(SupportRequest.class));
    }

    // FR-95: Tu choi tiep nhan yeu cau da duoc xu ly
    @Test
    void shouldRejectAlreadyAcceptedRequest() {

        SupportRequest request = createValidRequest();
        request.setId(1L);
        request.setStatus("IN_PROGRESS");

        when(repository.acceptIfPending(1L))
                .thenReturn(0);

        when(repository.findById(1L))
                .thenReturn(Optional.of(request));

        assertThrows(
                IllegalStateException.class,
                () -> service.acceptSupportRequest(1L)
        );

        verify(repository, times(1))
                .acceptIfPending(1L);

        verify(repository, never())
                .save(any(SupportRequest.class));
    }

    // FR-95: Tu choi ma yeu cau khong ton tai
    @Test
    void shouldRejectNonExistingRequest() {

        when(repository.acceptIfPending(999L))
                .thenReturn(0);

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.acceptSupportRequest(999L)
        );

        verify(repository, times(1))
                .acceptIfPending(999L);

        verify(repository, never())
                .save(any(SupportRequest.class));
    }

    // FR-95: Tu choi ma yeu cau khong hop le
    @Test
    void shouldRejectInvalidRequestId() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.acceptSupportRequest(0L)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.acceptSupportRequest(null)
        );

        // Khong truy cap Repository khi ma khong hop le
        verify(repository, never())
                .acceptIfPending(anyLong());

        verify(repository, never())
                .findById(anyLong());

        verify(repository, never())
                .save(any(SupportRequest.class));
    }
}
