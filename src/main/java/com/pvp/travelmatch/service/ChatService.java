package com.pvp.travelmatch.service;

import com.pvp.travelmatch.dto.ChatRequest;
import com.pvp.travelmatch.dto.ChatResponse;
import com.pvp.travelmatch.entity.Message;
import com.pvp.travelmatch.entity.User;
import com.pvp.travelmatch.repository.MessageRepository;
import com.pvp.travelmatch.repository.TravelPartnerRepository;
import com.pvp.travelmatch.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final TravelPartnerRepository travelPartnerRepository;
    private final NotificationService notificationService;
    private final BlockedUserService blockedUserService;

    // 🔥 Send Message
    public Message sendMessage(Long receiverId, String content) {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User sender = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        if (blockedUserService.isBlockedEitherWay(
                sender.getId(),
                receiver.getId())) {

            throw new RuntimeException(
                    "Messaging is unavailable because one of you has blocked the other"
            );
        }
        // ✅ Check if they are travel partners
        boolean isPartner = travelPartnerRepository.arePartners(sender, receiver);

        if (!isPartner) {
            throw new RuntimeException("You can only chat with accepted travel partners");
        }

        Message message = Message.builder()
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .timestamp(LocalDateTime.now())
                .build();

        Message saved = messageRepository.save(message);

        // 🔔 Notify the receiver of the new message - only reached after the
        // message is actually persisted above. Wrapped so a notification
        // failure can never break sending a chat message.
        try {
            notificationService.createChatMessageNotification(receiver, sender);
        } catch (Exception e) {
            // Deliberately swallow: sending a message must always succeed
            // even if the notification side-effect fails.
        }

        return saved;
    }

    // 🔥 Get Conversation
    public List<Message> getConversation(Long otherUserId) {

        String email = (String) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        User otherUser = userRepository.findById(otherUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (blockedUserService.isBlockedEitherWay(
                currentUser.getId(),
                otherUser.getId())) {

            throw new RuntimeException(
                    "This conversation is unavailable because one of you has blocked the other"
            );
        }

        // Optional: also validate partner here (extra security)
        boolean isPartner = travelPartnerRepository.arePartners(currentUser, otherUser);

        if (!isPartner) {
            throw new RuntimeException("Not allowed to view this conversation");
        }

        // 🔔 Opening this conversation means any pending "new message"
        // notifications from this specific partner have now been seen -
        // clear them so they don't linger unread in the notification panel.
        try {
            notificationService.markMessageNotificationsAsRead(currentUser.getId(), otherUser.getId());
        } catch (Exception e) {
            // Deliberately swallow: viewing the conversation must always
            // succeed even if clearing notifications fails.
        }

        return messageRepository
                .findBySenderAndReceiverOrReceiverAndSenderOrderByTimestampAsc(
                        currentUser, otherUser,
                        currentUser, otherUser
                );
    }

//    User receiver = userRepository.findById(receiverId)
//            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));



//    public String processMessage(String message){
//
//        message = message.toLowerCase();
//
//        if(message.contains("hello")){
//            return "Hello 👋 I'm your TravelMatch assistant. How can I help?";
//        }
//
//        if(message.contains("create trip")){
//            return "You can create a travel plan from the dashboard → Create Plan.";
//        }
//
//        if(message.contains("find partner")){
//            return "Go to the Matches page to find travelers going to the same destination.";
//        }
//
//        if(message.contains("chat")){
//            return "Once a match is accepted you can chat with your travel partner.";
//        }
//
//        return "Sorry, I didn't understand that. Try asking about trips, partners, or chat.";
//    }


    private final RestTemplate restTemplate = new RestTemplate();

    private final String AI_URL = "https://travelmatch-chatbot-production.up.railway.app/get_response";

    public ChatResponse askBot(ChatRequest request){

        return restTemplate.postForObject(
                AI_URL,
                request,
                ChatResponse.class
        );

    }
}