package com.group3.vitamins.issue.application.port;

import java.util.List;

public interface IssueBlockPort {

    List<BlockView> validateLinkable(Long stepId, List<Long> blockIds);

    record BlockView(Long blockId, String title, String type) {
    }
}
