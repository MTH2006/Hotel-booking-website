
package com.hotelbooking.service;

import com.hotelbooking.model.Faq;
import com.hotelbooking.repository.FaqRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class FaqService {

    private final FaqRepository faqRepository;

    public FaqService(FaqRepository faqRepository) {
        this.faqRepository = faqRepository;
    }

    // FR-96: Lay danh sach cau hoi thuong gap
    @Transactional(readOnly = true)
    public List<Faq> getAllFaqs() {

        return faqRepository.findAll(
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
    }

    // FR-96: ADMIN them cau hoi thuong gap
    @Transactional
    public Faq createFaq(Faq request) {

        validateFaq(request);

        Faq faq = new Faq();

        faq.setQuestion(request.getQuestion().trim());
        faq.setAnswer(request.getAnswer().trim());

        // ID va thoi gian do he thong quan ly
        return faqRepository.save(faq);
    }

    // FR-96: ADMIN chinh sua cau hoi thuong gap
    @Transactional
    public Faq updateFaq(Long id, Faq request) {

        validateId(id);
        validateFaq(request);

        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Khong tim thay cau hoi FAQ."
                ));

        // Chi cap nhat noi dung, khong thay doi ID
        faq.setQuestion(request.getQuestion().trim());
        faq.setAnswer(request.getAnswer().trim());

        return faqRepository.save(faq);
    }

    // FR-96: ADMIN xoa cau hoi thuong gap
    @Transactional
    public void deleteFaq(Long id) {

        validateId(id);

        Faq faq = faqRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Khong tim thay cau hoi FAQ."
                ));

        faqRepository.delete(faq);
    }

    // FR-96: Kiem tra ma FAQ
    private void validateId(Long id) {

        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "Ma FAQ khong hop le."
            );
        }
    }

    // FR-96: Kiem tra du lieu FAQ
    private void validateFaq(Faq request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Thong tin FAQ khong hop le."
            );
        }

        String question = request.getQuestion();
        String answer = request.getAnswer();

        if (question == null
                || question.trim().isBlank()
                || question.trim().length() > 500) {

            throw new IllegalArgumentException(
                    "Cau hoi FAQ phai co tu 1 den 500 ky tu."
            );
        }

        if (answer == null
                || answer.trim().isBlank()
                || answer.trim().length() > 10000) {

            throw new IllegalArgumentException(
                    "Cau tra loi FAQ phai co tu 1 den 10000 ky tu."
            );
        }
    }
}
