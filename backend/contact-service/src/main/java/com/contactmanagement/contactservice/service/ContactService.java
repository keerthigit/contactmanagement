package com.contactmanagement.contactservice.service;

import com.contactmanagement.contactservice.dto.ContactSearchRequest;
import com.contactmanagement.contactservice.dto.PaginatedResponse;
import com.contactmanagement.contactservice.model.Contact;
import com.contactmanagement.contactservice.repository.ContactRepository;
import com.contactmanagement.contactservice.repository.ContactSpecification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ContactService {

    private final ContactRepository contactRepository;

    @Autowired
    public ContactService(ContactRepository contactRepository) {
        this.contactRepository = contactRepository;
    }

    public Contact create(Contact contact) {
        return contactRepository.save(contact);
    }

    public Optional<Contact> read(UUID id) {
        return contactRepository.findById(id);
    }

    public List<Contact> readAll() {
        return contactRepository.findAll();
    }

    public Contact update(UUID id, Contact contactDetails) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Contact not found with id: " + id));

        contact.setFirstName(contactDetails.getFirstName());
        contact.setLastName(contactDetails.getLastName());
        contact.setMobile(contactDetails.getMobile());
        contact.setHomePhone(contactDetails.getHomePhone());
        contact.setEmail(contactDetails.getEmail());
        contact.setAddresses(contactDetails.getAddresses());
        contact.setTags(contactDetails.getTags());
        contact.setStatus(contactDetails.getStatus());

        return contactRepository.save(contact);
    }

    public void delete(UUID id) {
        if (!contactRepository.existsById(id)) {
            throw new RuntimeException("Contact not found with id: " + id);
        }
        contactRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<Contact> searchContacts(ContactSearchRequest request) {
        // Build specification from search request
        Specification<Contact> spec = ContactSpecification.buildSpecification(request);

        // Create sort direction
        Sort.Direction direction = request.getSortDirection().equalsIgnoreCase("ASC") 
            ? Sort.Direction.ASC 
            : Sort.Direction.DESC;

        // Create sort object
        Sort sort = Sort.by(direction, request.getSortBy());

        // Create pageable with pagination and sorting
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

        // Execute query with specification and pagination
        Page<Contact> page = contactRepository.findAll(spec, pageable);

        // Convert Page to PaginatedResponse
        return new PaginatedResponse<>(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements()
        );
    }
}
