package com.group3.vitamins.image.application.service;

import com.group3.vitamins.activitylog.contract.ActivityFieldChange;
import com.group3.vitamins.activitylog.contract.ActivityOccurredEvent;
import com.group3.vitamins.activitylog.domain.ActivityLogAction;
import com.group3.vitamins.image.application.command.CreateImageItemsCommand;
import com.group3.vitamins.image.application.command.DeleteImageItemCommand;
import com.group3.vitamins.image.application.command.PurgeImageItemsCommand;
import com.group3.vitamins.image.application.command.RestoreImageItemsCommand;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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

    // 한 번의 요청 안에서 S3 업로드를 몇 번이나 반복하며 트랜잭션(비관적 락 포함)을 붙잡고 있을지의
    // 최악값을 유한하게 만든다 — 개수 자체는 명세에 없어 구현 시 임의 결정 (.ai/api/image.md 참고).
    private static final int MAX_FILES_PER_REQUEST = 20;

    @Override
    public CreateImageItemsView create(CreateImageItemsCommand command) {
        log.info("이미지 항목 생성 요청 - imgBlockId={}, userId={}, fileCount={}",
                command.imgBlockId(), command.userId(), command.files().size());

        List<MultipartFile> files = command.files();
        if (files.size() > MAX_FILES_PER_REQUEST) {
            log.warn("이미지 생성 요청 파일 개수 초과 - imgBlockId={}, fileCount={}",
                    command.imgBlockId(), files.size());
            throw new ValidationException(ImageErrorCode.TOO_MANY_FILES);
        }

        eligibilityPolicy.assertBlockActiveOrThrow(command.imgBlockId());
        eligibilityPolicy.assertEditPermission(command.imgBlockId(), command.userId(), command.role());

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
        // 생성처럼 특정 필드가 아닌 활동은 changes 를 비울 수 없어 전부 null인 항목 하나를 담아 보낸다.
        // resourceName 에는 원본 파일명을 스냅샷으로 남긴다.
        Long blockId = imageBlockRepository.getBlockId(command.imgBlockId());
        for (ImageItem saved : savedItems) {
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.CREATE,
                    blockId,
                    saved.getImgId(),
                    saved.getOriginalName(),
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
        eligibilityPolicy.assertEditPermission(command.imgBlockId(), command.userId(), command.role());

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
            int deleted = imageRepository.markDeleted(imgId, command.imgBlockId(), deletedAt);
            if (deleted == 0) {
                // 조회~삭제 사이에 동시에 삭제된 경우(레이스). 이미 삭제됐으니 다시 지울 필요도,
                // 이번 호출이 지웠다는 로그를 남길 이유도 없다.
                log.warn("이미지 항목 삭제 경합 발생(수정 API 배열 누락 처리) - imgId={}", imgId);
                continue;
            }
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.DELETE,
                    blockId,
                    imgId,
                    currentByImgId.get(imgId).getOriginalName(),
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
                int updated = imageRepository.updateCaptionAndOrder(
                        entry.imgId(), command.imgBlockId(), newCaption, newOrderIndex);
                if (updated == 0) {
                    // 조회~수정 사이에 동시에 삭제된 경우(레이스). 안 바뀐 걸 바뀌었다고 응답하거나
                    // 일어나지 않은 변경을 활동 로그에 남기면 안 된다.
                    log.warn("이미지 항목 수정 경합 발생 - imgId={}", entry.imgId());
                    throw new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
                }
                domainEventPublisher.publish(ActivityOccurredEvent.of(
                        ActivityLogAction.MODIFY, blockId, entry.imgId(), before.getOriginalName(),
                        command.userId(), changes));
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
        eligibilityPolicy.assertEditPermission(before.getImgBlockId(), command.userId(), command.role());

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
                before.getOriginalName(),
                command.userId(),
                List.of(new ActivityFieldChange(null, null, null))
        ));

        log.info("이미지 항목 삭제 완료 - imgId={}", command.imgId());
    }

    @Override
    public RestoreImageItemsView restore(RestoreImageItemsCommand command) {
        log.info("이미지 항목 복구 요청 - userId={}, imgIds={}", command.userId(), command.imgIds());

        List<Long> imgIds = command.imgIds();
        if (imgIds == null || imgIds.isEmpty() || new HashSet<>(imgIds).size() != imgIds.size()) {
            log.warn("복구 요청 목록이 유효하지 않음 - imgIds={}", imgIds);
            throw new ValidationException(ImageErrorCode.INVALID_IMAGE_LIST);
        }
        Set<Long> uniqueIds = new HashSet<>(imgIds);

        Map<Long, ImageItem> foundByImgId = imageRepository.findAllByImgIds(imgIds).stream()
                .collect(Collectors.toMap(ImageItem::getImgId, item -> item));

        // 요청한 imgId 전부가 실제로 존재하고, 실제로 삭제된 상태여야 복구 대상이다.
        // 존재 자체가 없는 것과 이미 활성인 것을 구분할 필요가 없어(둘 다 "복구할 수 없는 항목") 같은 코드로 묶는다.
        for (Long imgId : uniqueIds) {
            ImageItem item = foundByImgId.get(imgId);
            if (item == null || item.getDeletedAt() == null) {
                log.warn("복구 대상 아님(존재하지 않거나 이미 활성) - imgId={}", imgId);
                throw new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
            }
        }

        // 이미지가 속한 블록별로(=스텝별로) 편집 권한부터 확인하고, 그다음 블록 생존 여부를 확인한다.
        // assertEditPermissionEvenIfBlockDeleted는 블록이 삭제돼 있어도 그 블록이 속했던 스텝을 직접
        // 찾아 정확한 권한 판정을 하므로(§ImageEligibilityPolicy 참고), 권한이 없는 사용자는 블록
        // 상태와 무관하게 항상 403만 보고 "블록이 삭제됐다"는 정보 자체를 알 수 없다. 권한이 있는
        // 사용자만 다음 단계(블록 생존 여부)까지 도달해서, 블록이 삭제된 경우 정확한 사유(IMG-009)를
        // 안내받는다 (2026-08-06, 순서 재정리).
        Set<Long> imgBlockIds = foundByImgId.values().stream()
                .map(ImageItem::getImgBlockId)
                .collect(Collectors.toSet());
        for (Long imgBlockId : imgBlockIds) {
            eligibilityPolicy.assertEditPermissionEvenIfBlockDeleted(imgBlockId, command.userId(), command.role());
            eligibilityPolicy.assertBlockActiveForRestoreOrThrow(imgBlockId);
        }

        // 복구된 이미지는 각 블록의 현재 활성 목록 맨 뒤에 붙는다(원래 순서 복원이 아님) — 요청에 여러
        // 블록이 섞여 있어도, 같은 블록끼리는 요청에 나열된 순서 그대로 이어 붙인다.
        Map<Long, Integer> nextOrderIndexByBlock = new HashMap<>();
        Map<Long, Long> blockIdByImgBlockId = new HashMap<>();
        List<RestoredImageView> resultViews = new ArrayList<>(imgIds.size());

        for (Long imgId : imgIds) {
            ImageItem item = foundByImgId.get(imgId);
            Long imgBlockId = item.getImgBlockId();
            int orderIndex = nextOrderIndexByBlock.compute(imgBlockId,
                    (blockId, current) -> current == null ? imageRepository.findMaxOrderIndex(blockId) + 1 : current + 1);

            int restored = imageRepository.restore(imgId, imgBlockId, orderIndex);
            if (restored == 0) {
                // 검증~복구 사이에 동시에 상태가 바뀐 경우(레이스). 존재하지 않는 항목과 동일하게 취급한다.
                log.warn("이미지 항목 복구 경합 발생 - imgId={}", imgId);
                throw new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
            }

            // 활동 로그(§5.3 이미지 — 휴지통 복원) — develop 병합으로 ActivityLogAction.RESTORE가
            // 생기기 전까지는 보류했던 부분(`.ai/api/image.md` 참고). 생성·삭제와 동일하게 changes는
            // 전부 null인 항목 하나만 담아 보낸다.
            Long blockId = blockIdByImgBlockId.computeIfAbsent(imgBlockId, imageBlockRepository::getBlockId);
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.RESTORE,
                    blockId,
                    imgId,
                    item.getOriginalName(),
                    command.userId(),
                    List.of(new ActivityFieldChange(null, null, null))
            ));

            resultViews.add(new RestoredImageView(imgBlockId, imgId, item.getOriginalName(), orderIndex));
        }

        log.info("이미지 항목 복구 완료 - count={}", resultViews.size());

        return new RestoreImageItemsView(resultViews);
    }

    @Override
    public void purge(PurgeImageItemsCommand command) {
        log.info("이미지 영구 삭제 요청 - userId={}, imgIds={}", command.userId(), command.imgIds());

        List<Long> imgIds = command.imgIds();
        if (imgIds == null || imgIds.isEmpty() || new HashSet<>(imgIds).size() != imgIds.size()) {
            log.warn("영구 삭제 요청 목록이 유효하지 않음 - imgIds={}", imgIds);
            throw new ValidationException(ImageErrorCode.INVALID_IMAGE_LIST);
        }
        Set<Long> uniqueIds = new HashSet<>(imgIds);

        Map<Long, ImageItem> foundByImgId = imageRepository.findAllByImgIds(imgIds).stream()
                .collect(Collectors.toMap(ImageItem::getImgId, item -> item));

        // 완전 삭제는 휴지통(소프트 삭제된) 항목만 대상이다 — 복구 API의 대상 판정과 대칭.
        for (Long imgId : uniqueIds) {
            ImageItem item = foundByImgId.get(imgId);
            if (item == null || item.getDeletedAt() == null) {
                log.warn("영구 삭제 대상 아님(존재하지 않거나 아직 삭제 안 됨) - imgId={}", imgId);
                throw new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
            }
        }

        // 이미지가 속한 블록별로(=스텝별로) 편집 권한을 확인한다. 완전 삭제는 블록이 이미 삭제된
        // 이미지(§복구 API의 IMG-009 상황)도 대상이 될 수 있는데, 블록이 살아있어야만 판정 가능한
        // assertEditPermission 대신 assertEditPermissionEvenIfBlockDeleted를 써서 블록 삭제 여부와
        // 무관하게 정확한 편집 권한을 판정한다 (§ImageEligibilityPolicy 참고).
        Set<Long> imgBlockIds = foundByImgId.values().stream()
                .map(ImageItem::getImgBlockId)
                .collect(Collectors.toSet());
        for (Long imgBlockId : imgBlockIds) {
            eligibilityPolicy.assertEditPermissionEvenIfBlockDeleted(imgBlockId, command.userId(), command.role());
        }

        Map<Long, Long> blockIdByImgBlockId = new HashMap<>();
        List<String> storageKeysToDelete = new ArrayList<>(uniqueIds.size());

        // DB 행을 먼저 전부 지운다 — S3는 아직 안 건드린다. 배치 중간에 어떤 imgId가 레이스로 실패하면
        // 여기서 예외가 나서 트랜잭션이 롤백되는데, 이 시점까진 S3에 손도 안 댔으니 되돌릴 것도 없다.
        for (Long imgId : uniqueIds) {
            ImageItem item = foundByImgId.get(imgId);
            Long imgBlockId = item.getImgBlockId();

            int deleted = imageRepository.hardDelete(imgId);
            if (deleted == 0) {
                // 검증~삭제 사이에 동시에 복구되거나 이미 삭제된 경우(레이스).
                log.warn("이미지 항목 완전 삭제 경합 발생 - imgId={}", imgId);
                throw new NotFoundException(ImageErrorCode.ITEM_NOT_FOUND);
            }
            storageKeysToDelete.add(item.getImageUrl());

            Long blockId = blockIdByImgBlockId.computeIfAbsent(imgBlockId, imageBlockRepository::getBlockId);
            domainEventPublisher.publish(ActivityOccurredEvent.of(
                    ActivityLogAction.PURGE,
                    blockId,
                    imgId,
                    item.getOriginalName(),
                    command.userId(),
                    List.of(new ActivityFieldChange(null, null, null))
            ));
        }

        // S3 객체는 DB 트랜잭션이 실제로 커밋된 뒤에만 지운다. 이전엔 항목마다 "S3 지우고 → DB 지우고"를
        // 반복했는데, 배치 중 다른 imgId가 나중에 실패해서 트랜잭션 전체가 롤백되면 이미 지운 S3 객체는
        // 되돌릴 수 없어서 "DB엔 있는데 실제 파일은 없는" 상태가 남는 문제가 있었다(2026-08-06, 코드
        // 리뷰로 발견). 커밋 후 S3 삭제가 실패해도 DB는 이미 확정된 상태라 고아 S3 객체만 남을 뿐 —
        // 정합성은 안 깨지고, 나중에 정리 배치로 치울 수 있다(§하드 삭제 정책 백로그).
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String storageKey : storageKeysToDelete) {
                    try {
                        imageStoragePort.delete(storageKey);
                    } catch (RuntimeException e) {
                        log.error("영구 삭제 커밋 후 S3 객체 삭제 실패 - storageKey={}", storageKey, e);
                    }
                }
            }
        });

        log.info("이미지 완전 삭제 완료 - count={}", uniqueIds.size());
    }
}
