package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.TravelMemoryResponse;
import com.pvp.travelmatch.entity.TravelMemory;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.TravelMemoryRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TravelMemoryService {
    private static final long MAX_PHOTO_SIZE = 5L * 1024 * 1024;
    private static final int MAX_CAPTION_LENGTH = 500;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final TravelMemoryRepository memoryRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final UserRepository userRepository;

    @Transactional
    public TravelMemoryResponse create(Long travelPlanId, String caption, MultipartFile file) {
        User user = currentUser();
        TravelPlan plan = travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel plan not found"));

        if (plan.getUser() == null || !plan.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create memories for your own trips");
        }
        if (!"COMPLETED".equalsIgnoreCase(plan.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Memories can only be created for completed trips");
        }
        if (plan.getEndDate() != null && plan.getEndDate().isAfter(java.time.LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This trip has not finished yet");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose a photo");
        }
        if (file.getSize() > MAX_PHOTO_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be 5MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Photo must be JPEG, PNG, or WEBP");
        }
        String cleanCaption = caption == null ? null : caption.trim();
        if (cleanCaption != null && cleanCaption.length() > MAX_CAPTION_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Caption must be 500 characters or fewer");
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            TravelMemory memory = TravelMemory.builder()
                    .user(user)
                    .travelPlan(plan)
                    .caption(cleanCaption == null || cleanCaption.isBlank() ? null : cleanCaption)
                    .photo(file.getBytes())
                    .photoContentType(contentType)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            return TravelMemoryResponse.fromEntity(memoryRepository.save(memory));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read the memory photo");
        }
    }

    @Transactional(readOnly = true)
    public List<TravelMemoryResponse> getForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Traveler not found");
        }
        return memoryRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(TravelMemoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TravelMemoryResponse getOne(Long id) {
        return memoryRepository.findById(id)
                .map(TravelMemoryResponse::fromEntity)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memory not found"));
    }

    @Transactional
    public void delete(Long id) {
        User user = currentUser();
        TravelMemory memory = memoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Memory not found"));
        if (!memory.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own memories");
        }
        memoryRepository.delete(memory);
    }

    private User currentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
