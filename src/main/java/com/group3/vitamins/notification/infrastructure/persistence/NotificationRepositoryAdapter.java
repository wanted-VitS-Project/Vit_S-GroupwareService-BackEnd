package com.group3.vitamins.notification.infrastructure.persistence;

import com.group3.vitamins.notification.domain.model.Notification;
import com.group3.vitamins.notification.domain.model.NotificationPage;
import com.group3.vitamins.notification.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class NotificationRepositoryAdapter implements NotificationRepository {

    private final SpringDataNotificationRepository springDataRepository;

    @Override
    public Notification save(Notification notification) {
        return NotificationMapper.toDomain(
                springDataRepository.save(NotificationMapper.toEntity(notification)));
    }

    @Override
    public Optional<Notification> findActiveById(Long notificationId) {
        return springDataRepository.findByNotificationIdAndDeletedAtIsNull(notificationId)
                .map(NotificationMapper::toDomain);
    }

    @Override
    public NotificationPage search(String userId, String categoryPrefix, Boolean isRead, int page, int size) {
        Page<NotificationJpaEntity> result = springDataRepository.search(
                userId, categoryPrefix, isRead, PageRequest.of(page, size));

        return new NotificationPage(
                result.getContent().stream().map(NotificationMapper::toDomain).toList(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public int markAllRead(String userId, LocalDateTime now) {
        return springDataRepository.markAllRead(userId, now);
    }

    @Override
    public int deleteCreatedBefore(LocalDateTime createdBefore, LocalDateTime deletedAt) {
        return springDataRepository.deleteCreatedBefore(createdBefore, deletedAt);
    }
}
