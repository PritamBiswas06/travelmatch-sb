package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.TravelCommentRequest;
import com.pvp.travelmatch.dto.TravelCommentResponse;
import com.pvp.travelmatch.entity.NotificationType;
import com.pvp.travelmatch.entity.TravelComment;
import com.pvp.travelmatch.entity.TravelPlan;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.TravelCommentRepository;
import com.pvp.travelmatch.repository.TravelPlanRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelCommentService {
    private static final int MAX_COMMENT_LENGTH = 500;
    private final TravelCommentRepository commentRepository;
    private final TravelPlanRepository travelPlanRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<TravelCommentResponse> getForPlan(Long travelPlanId) {
        if (!travelPlanRepository.existsById(travelPlanId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel post not found");
        }
        Long currentUserId = currentUser().getId();
        return commentRepository.findByTravelPlanIdOrderByCreatedAtAsc(travelPlanId).stream()
                .map(c -> TravelCommentResponse.fromEntity(c, currentUserId))
                .toList();
    }

    @Transactional
    public TravelCommentResponse create(Long travelPlanId, TravelCommentRequest request) {
        User user = currentUser();
        if (request == null || request.getComment() == null || request.getComment().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment cannot be empty");
        }
        String text = request.getComment().trim();
        if (text.length() > MAX_COMMENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Comment must be 500 characters or fewer");
        }

        TravelPlan plan = travelPlanRepository.findById(travelPlanId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Travel post not found"));

        TravelComment comment = TravelComment.builder()
                .user(user)
                .travelPlan(plan)
                .comment(text)
                .createdAt(LocalDateTime.now())
                .build();
        TravelComment saved = commentRepository.save(comment);

        if (plan.getUser() != null && !plan.getUser().getId().equals(user.getId())) {
            try {
                notificationService.createNotification(
                        plan.getUser(),
                        user,
                        "💬 " + user.getName() + " commented on your travel post.",
                        NotificationType.TRAVEL_COMMENT,
                        plan.getId()
                );
            } catch (Exception ignored) {
                // A notification failure must never fail the comment itself.
            }
        }

        return TravelCommentResponse.fromEntity(saved, user.getId());
    }

    @Transactional
    public void delete(Long commentId) {
        User user = currentUser();
        TravelComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));
        if (!comment.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own comments");
        }
        commentRepository.delete(comment);
    }

    private User currentUser() {
        String email = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }
}
