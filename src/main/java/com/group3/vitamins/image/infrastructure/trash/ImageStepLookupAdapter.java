package com.group3.vitamins.image.infrastructure.trash;

import com.group3.vitamins.image.application.port.ImageStepLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ImageStepLookupAdapter implements ImageStepLookupPort {

    private final ImageTrashMapper imageTrashMapper;

    @Override
    public Optional<Long> findStepIdByImgBlockId(Long imgBlockId) {
        return imageTrashMapper.findStepIdByImgBlockId(imgBlockId);
    }
}
