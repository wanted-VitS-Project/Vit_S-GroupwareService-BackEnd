package com.group3.vitamins.image.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.image.application.command.CreateImageItemsCommand;
import com.group3.vitamins.image.application.command.DeleteImageItemCommand;
import com.group3.vitamins.image.application.command.UpdateImageItemsCommand;
import com.group3.vitamins.image.application.policy.ImageEligibilityPolicy;
import com.group3.vitamins.image.application.port.ImageStoragePort;
import com.group3.vitamins.image.application.port.ImageStoragePort.UploadedImage;
import com.group3.vitamins.image.application.usecase.ImageCommandUseCase;
import com.group3.vitamins.image.domain.exception.ImageErrorCode;
import com.group3.vitamins.image.domain.model.ImageItem;
import com.group3.vitamins.image.domain.repository.ImageBlockRepository;
import com.group3.vitamins.image.domain.repository.ImageRepository;
import com.group3.vitamins.global.application.event.DomainEventPublisher;
import com.group3.vitamins.global.domain.common.error.exception.NotFoundException;
import com.group3.vitamins.global.domain.common.error.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 이미지 블록 생성·삭제는 Block 도메인(동훈님)이 처리한다 — 여기는 항목(내부 데이터) 생성만 담당한다.
 *
 * <p>파일 검증·리사이즈·S3 업로드는 외부 I/O라 체크리스트와 달리 시간이 걸린다 — 그래도 "블록 활성
 * 확인 → 저장" 사이의 락 구간은 체크리스트와 동일한 원칙(비관적 락)을 따른다. 현재는 파일 수가
 * 적은 배치 업로드라 단순하게 하나의 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ImageCommandService implements ImageCommandUseCase {

    private final ImageEligibilityPolicy eligibilityPolicy;
    private final ImageRepository imageRepository;
    private final ImageBlockRepository imageBlockRepository;
    private final ImageStoragePort imageStoragePort;
    private final DomainEventPublisher domainEventPublisher;

    @Override
    public CreateImageItemsView create(CreateImageItemsCommand command) {
        log.info("이미지 항목 생성 요청 - imgBlockId={}, userId={}, fileCount={}",
                command.imgBlockId(), command.userId(), command.files().size());

        eligibilityPolicy.assertBlockActiveOrThrow(command.imgBlockId());
        eligibilityPolicy.assertEditPermission(command.imgBlockId(), command.userId());

        List<MultipartFile> files = command.files();
        List<String> captions = command.captions();

        // captions 를 아예 안 보내는 건 허용(전부 "" 처리)하지만, 보냈는데 개수가 안 맞으면
        // 정상적인 클라이언트가 낼 수 없는 요청이라 즉시 거부한다 (2026-08-04 결정 — IMG-004).
        if (captions != null && captions.size() != files.size()) {
            log.warn("이미지·캡션 개수 불일치 - imgBlockId={}, fileCount={}, captionCount={}",
                    command.imgBlockId(), files.size(), captions.size());
            throw new ValidationException(ImageErrorCode.CAPTION_COUNT_MISMATCH);
        }

        // 확장자는 업로드를 시작하기 전에 전부 검증한다 — 뒤쪽 파일이 걸리면
        // 앞서 이미 올린 파일들이 고아 객체로 S3 에 남기 때문이다.
        List<String> extensions = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            extensions.add(eligibilityPolicy.assertSupportedExtensionOrThrow(file.getOriginalFilename()));
        }

        int nextOrderIndex = imageRepository.findMaxOrderIndex(command.imgBlockId()) + 1;

        List<ImageItem> draftItems = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String extension = extensions.get(i);
            String caption = (captions != null && i < captions.size() && captions.get(i) != null)
                    ? captions.get(i) : "";

            UploadedImage uploaded = imageStoragePort.upload(command.imgBlockId(), file, extension);

            draftItems.add(ImageItem.newItem(
                    command.imgBlockId(),
                    file.getOriginalFilename(),
                    uploaded.storageKey(),
                    extension,
                    uploaded.sizeBytes(),
                    caption,
                    nextOrderIndex + i
            ));
        }

        List<ImageItem> savedItems = imageRepository.createAll(draftItems);

        log.info("이미지 항목 생성 완료 - imgBlockId={}, count={}", command.imgBlockId(), savedItems.size());

        // 활동 로그 — 이미지 항목 생성은 이미지 단위로 한 건씩 남긴다 (§5.3 이미지).
        // 생성처럼 특정 필드가 아닌 활동은 changes 를 비울 수 없어 전부 null인 항목 하나를 담아 보낸다
        // (원본 이미지명 등 사람이 읽을 정보는 activity_log에 스냅샷으로 안 남고, 조회 시 resourceId로
        // image 테이블을 조인해 조합한다 — Block명과 동일한 원칙, `.ai/api/activity-log.md` 참고).
        Long blockId = imageBlockRepository.getBlockId(command.imgBlockId());
        for (ImageItem saved : savedItems) {
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.CREATE,
                    blockId,
                    saved.getImgId(),
                    command.userId(),
                    List.of(new ActivityFieldChange(null, null, null))
            ));
        }

        // DB 에는 저장 키만 있다 — 프론트에 줄 실제 URL은 응답을 만드는 지금 이 시점에 서명해서 발급한다
        // (버킷이 퍼블릭 액세스를 전부 차단하고 있어 영구 URL을 만들 수 없다).
        List<CreatedImageView> imageViews = savedItems.stream()
                .map(item -> new CreatedImageView(
                        item.getImgId(),
                        item.getOriginalName(),
                        imageStoragePort.presignViewUrl(item.getImageUrl()),
                        item.getCaption(),
                        item.getOrderIndex(),
                        item.getCreatedAt()
                ))
                .toList();

        return new CreateImageItemsView(command.imgBlockId(), imageViews);
    }

    @Override
    public UpdateImageItemsView updateItems(UpdateImageItemsCommand command) {
        log.info("이미지 항목 수정 요청 - imgBlockId={}, userId={}, count={}",
                command.imgBlockId(), command.userId(), command.images().size());

        eligibilityPolicy.assertBlockActiveOrThrow(command.imgBlockId());
        eligibilityPolicy.assertEditPermission(command.imgBlockId(), command.userId());

        List<UpdateImageItemsCommand.Entry> requested = command.images();
        Set<Long> requestedIds = requested.stream()
                .map(UpdateImageItemsCommand.Entry::imgId)
                .collect(Collectors.toSet());

        Map<Long, ImageItem> currentByImgId = imageRepository.findAllActiveByImgBlockId(command.imgBlockId())
                .stream()
                .collect(Collectors.toMap(ImageItem::getImgId, item -> item));

        // 요청 배열의 위치가 곧 새 orderIndex 다 — 중복 imgId나 이 블록 소속이 아닌/존재하지 않는
        // imgId가 섞이면 순서 계산 자체가 깨진다. 명세에 이 검증 기준이 없어 임의로 정했다
        // (IMG-005, `.ai/api/image.md` 참고). 요청에서 빠진 이미지는 에러가 아니라 "삭제"로
        // 간주한다 (2026-08-04 결정 — 프론트 삭제 버튼이 같은 화면에 있음) — 그래서 "전체 포함"
        // 요구는 없고 부분집합인지만 확인한다.
        if (requestedIds.size() != requested.size() || !currentByImgId.keySet().containsAll(requestedIds)) {
            log.warn("이미지 목록 불일치 - imgBlockId={}, requested={}, current={}",
                    command.imgBlockId(), requestedIds, currentByImgId.keySet());
            throw new ValidationException(ImageErrorCode.INVALID_IMAGE_LIST);
        }

        Long blockId = imageBlockRepository.getBlockId(command.imgBlockId());

        // 현재 있는데 요청에 없는 이미지 = 삭제 대상. 소프트 삭제만 한다 — S3 객체는 지우지 않는다
        // (복구 기능이 아직 없어 당장 지울 이유도 없지만, 하드 삭제 정책이 나오기 전까지는 보류하기로
        // 팀에서 확인함 — `.ai/api/image.md` 참고).
        Set<Long> toDeleteIds = new HashSet<>(currentByImgId.keySet());
        toDeleteIds.removeAll(requestedIds);
        LocalDateTime deletedAt = LocalDateTime.now();
        for (Long imgId : toDeleteIds) {
            imageRepository.markDeleted(imgId, command.imgBlockId(), deletedAt);
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.DELETE,
                    blockId,
                    imgId,
                    command.userId(),
                    List.of(new ActivityFieldChange(null, null, null))
            ));
        }

        List<UpdatedImageOrderView> resultViews = new ArrayList<>(requested.size());

        for (int i = 0; i < requested.size(); i++) {
            UpdateImageItemsCommand.Entry entry = requested.get(i);
            int newOrderIndex = i + 1;
            // 생성 API와 동일한 규칙으로 통일한다 — 캡션 없음은 항상 "" (2026-08-04 담당자 결정,
            // 원 명세 문구 "없으면 null"에서 변경). "" 하나로만 표현해야 지우기(캡션 있던 것을 빈 값으로
            // 보내는 것)와 원래 없음을 구분 안 해도 되고, before/after 비교에 null 예외 처리가 안 생긴다.
            String newCaption = entry.caption() != null ? entry.caption() : "";
            ImageItem before = currentByImgId.get(entry.imgId());

            // 실제로 바뀐 게 있는 이미지만 DB에 쓴다 — 안 바뀐 이미지까지 매번 UPDATE 하면
            // updated_at 이 의미 없이 매 호출마다 갱신된다 (§5.3 이미지 — 동일 값 수정은 로그도 안 남김).
            //
            // ⚠️ "순서가 밀린 나머지 이미지까지 로그를 남기지 말라"는 명세 원칙과, 이 API가 매번
            // 전체 목록을 통째로 받는 구조가 맞지 않는다 — 어떤 게 사용자가 "직접 옮긴" 한 장인지
            // 서버가 구분할 수 없어서, orderIndex가 실제로 달라진 이미지 전부를 남긴다. 과다 로그로
            // 판단되면 프론트가 이동된 이미지 ID를 별도로 알려주는 방식으로 명세를 바꿔야 한다.
            List<ActivityFieldChange> changes = new ArrayList<>();
            if (!Objects.equals(before.getCaption(), newCaption)) {
                changes.add(new ActivityFieldChange("caption", before.getCaption(), newCaption));
            }
            if (before.getOrderIndex() != newOrderIndex) {
                changes.add(new ActivityFieldChange("orderIndex",
                        String.valueOf(before.getOrderIndex()), String.valueOf(newOrderIndex)));
            }
            if (!changes.isEmpty()) {
                imageRepository.updateCaptionAndOrder(entry.imgId(), command.imgBlockId(), newCaption, newOrderIndex);
                domainEventPublisher.publish(ActivityOccurredEvent.of(
                        ActivityLogAction.MODIFY, blockId, entry.imgId(), command.userId(), changes));
            }

            resultViews.add(new UpdatedImageOrderView(entry.imgId(), newOrderIndex, newCaption));
        }

        log.info("이미지 항목 수정 완료 - imgBlockId={}", command.imgBlockId());

        return new UpdateImageItemsView(resultViews);
    }

    @Override
    public void delete(DeleteImageItemCommand command) {
        log.info("이미지 항목 삭제 요청 - imgId={}, userId={}", command.imgId(), command.userId());

        ImageItem before = eligibilityPolicy.getActiveItemOrThrow(command.imgId());
        eligibilityPolicy.assertEditPermission(before.getImgBlockId(), command.userId());

        // 이 API는 PATCH의 배열 누락 삭제와 동일한 원칙 — 소프트 삭제만, S3는 지우지 않는다
        // (`.ai/api/image.md` §S3 저장 정책 참고. 같은 "삭제"가 어느 버튼이냐에 따라 S3 처리가
        // 달라지면 안 되므로 통일함).
        int deleted = imageRepository.markDeleted(command.imgId(), before.getImgBlockId(), LocalDateTime.now());
        if (deleted == 0) {
            // 조회~삭제 사이에 동시 삭제된 경우. 존재하지 않는 항목과 동일하게 취급한다.
            log.warn("이미지 항목 삭제 경합 발생 - 이미 삭제됨 - imgId={}", command.imgId());
            throw new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
        }

        Long blockId = imageBlockRepository.getBlockId(before.getImgBlockId());
        domainEventPublisher.publish(ActivityOccurredEvent.of(
                ActivityLogAction.DELETE,
                blockId,
                command.imgId(),
                command.userId(),
                List.of(new ActivityFieldChange(null, null, null))
        ));

        log.info("이미지 항목 삭제 완료 - imgId={}", command.imgId());
    }
}
