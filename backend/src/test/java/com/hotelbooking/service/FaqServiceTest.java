
package com.hotelbooking.service;

import com.hotelbooking.model.Faq;
import com.hotelbooking.repository.FaqRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FaqServiceTest {

    private FaqRepository repository;
    private FaqService service;

    @BeforeEach
    void setUp() {
        repository = mock(FaqRepository.class);
        service = new FaqService(repository);

        // Gia lap luu FAQ thanh cong
        when(repository.save(any(Faq.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // Tao du lieu FAQ mau
    private Faq createValidFaq() {
        Faq faq = new Faq();
        faq.setQuestion("Lam sao de dat phong?");
        faq.setAnswer("Chon khach san va xac nhan dat phong.");
        return faq;
    }

    // FR-96: Lay danh sach FAQ
    @Test
    void shouldGetAllFaqs() {
        Faq faq = createValidFaq();

        when(repository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        )).thenReturn(List.of(faq));

        List<Faq> result = service.getAllFaqs();

        assertEquals(1, result.size());
        assertEquals("Lam sao de dat phong?",
                result.get(0).getQuestion());

        verify(repository).findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    // FR-96: Them FAQ thanh cong
    @Test
    void shouldCreateFaqSuccessfully() {
        Faq request = new Faq();
        request.setQuestion("  Lam sao de dat phong?  ");
        request.setAnswer("  Chon khach san.  ");

        Faq result = service.createFaq(request);

        assertNotNull(result);
        assertEquals("Lam sao de dat phong?",
                result.getQuestion());
        assertEquals("Chon khach san.",
                result.getAnswer());
        assertNull(result.getId());

        verify(repository, times(1))
                .save(any(Faq.class));
    }

    // FR-96: Tu choi FAQ null
    @Test
    void shouldRejectNullFaq() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFaq(null)
        );

        verify(repository, never())
                .save(any(Faq.class));
    }

    // FR-96: Tu choi cau hoi rong
    @Test
    void shouldRejectBlankQuestion() {
        Faq request = createValidFaq();
        request.setQuestion("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFaq(request)
        );

        verify(repository, never())
                .save(any(Faq.class));
    }

    // FR-96: Tu choi cau hoi vuot 500 ky tu
    @Test
    void shouldRejectTooLongQuestion() {
        Faq request = createValidFaq();
        request.setQuestion("A".repeat(501));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFaq(request)
        );

        verify(repository, never())
                .save(any(Faq.class));
    }

    // FR-96: Tu choi cau tra loi rong
    @Test
    void shouldRejectBlankAnswer() {
        Faq request = createValidFaq();
        request.setAnswer("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFaq(request)
        );

        verify(repository, never())
                .save(any(Faq.class));
    }

    // FR-96: Tu choi cau tra loi vuot 10000 ky tu
    @Test
    void shouldRejectTooLongAnswer() {
        Faq request = createValidFaq();
        request.setAnswer("A".repeat(10001));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.createFaq(request)
        );

        verify(repository, never())
                .save(any(Faq.class));
    }

    // FR-96: Cap nhat FAQ thanh cong
    @Test
    void shouldUpdateFaqSuccessfully() {
        Faq existing = createValidFaq();
        existing.setId(1L);

        Faq request = new Faq();
        request.setQuestion("  Cau hoi moi  ");
        request.setAnswer("  Cau tra loi moi  ");

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        Faq result = service.updateFaq(1L, request);

        assertEquals(1L, result.getId());
        assertEquals("Cau hoi moi",
                result.getQuestion());
        assertEquals("Cau tra loi moi",
                result.getAnswer());

        verify(repository).save(existing);
    }

    // FR-96: Khong cho sua FAQ khong ton tai
    @Test
    void shouldRejectUpdatingNonExistingFaq() {
        Faq request = createValidFaq();

        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.updateFaq(999L, request)
        );

        verify(repository, never())
                .save(any(Faq.class));
    }

    // FR-96: Xoa FAQ thanh cong
    @Test
    void shouldDeleteFaqSuccessfully() {
        Faq existing = createValidFaq();
        existing.setId(1L);

        when(repository.findById(1L))
                .thenReturn(Optional.of(existing));

        service.deleteFaq(1L);

        verify(repository, times(1))
                .delete(existing);
    }

    // FR-96: Khong cho xoa FAQ khong ton tai
    @Test
    void shouldRejectDeletingNonExistingFaq() {
        when(repository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.deleteFaq(999L)
        );

        verify(repository, never())
                .delete(any(Faq.class));
    }

    // FR-96: Tu choi ma FAQ khong hop le
    @Test
    void shouldRejectInvalidFaqId() {
        Faq request = createValidFaq();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.updateFaq(0L, request)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteFaq(-1L)
        );

        verify(repository, never())
                .delete(any(Faq.class));
    }
}
