INSERT INTO vitamate_review_type (
    review_type, review_type_name, description, enabled, sort_order
)
VALUES
    ('COST_REPORT', '원가계산 검토', '원가계산 결과, 개요, 계산 기준, 산출내역을 기준으로 문서를 검토한다.', TRUE, 10),
    ('ETC_DOCUMENT', '기타서류 검토', '완료계, 청구서, 계약서류 등 보조 제출 문서를 검토한다.', TRUE, 20)
ON DUPLICATE KEY UPDATE
    review_type_name = VALUES(review_type_name),
    description = VALUES(description),
    enabled = VALUES(enabled),
    sort_order = VALUES(sort_order),
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO vitamate_review_template (
    review_type,
    category_code,
    category_name,
    guide_text,
    example_text,
    prompt_template,
    template_version,
    enabled,
    sort_order
)
VALUES
    (
        'COST_REPORT',
        'COST_RESULT',
        'I. 원가계산 결과',
        '원가 총액, 항목별 합계, 계산 결과가 문서 안에서 일관되는지 검토한다.',
        '총괄표와 산출내역의 합계가 일치하는지 확인해주세요.',
        '원가계산 결과 영역을 검토한다. 총액, 항목별 합계, 산출내역 합산값의 일치 여부와 누락된 금액 항목을 확인하고, 불일치가 있으면 근거 문구와 함께 지적한다.',
        'COST_REPORT_V1',
        TRUE,
        10
    ),
    (
        'COST_REPORT',
        'COST_OVERVIEW',
        'II. 원가계산 개요',
        '사업명, 계약 대상, 계산 기간, 작성 기준 등 개요 정보가 충분한지 검토한다.',
        '사업명과 원가계산 대상 기간이 문서에 명확히 적혀 있는지 확인해주세요.',
        '원가계산 개요 영역을 검토한다. 사업명, 계약 대상, 산정 기간, 작성 기준, 전제 조건이 문서에 명확히 제시됐는지 확인하고 부족한 항목을 정리한다.',
        'COST_REPORT_V1',
        TRUE,
        20
    ),
    (
        'COST_REPORT',
        'COST_ELEMENT_CRITERIA',
        'III. 원가요소별 계산기준',
        '재료비, 노무비, 경비 등 원가요소별 계산 기준과 근거가 적절한지 검토한다.',
        '원가요소별 단가와 수량 산정 근거가 있는지 확인해주세요.',
        '원가요소별 계산기준 영역을 검토한다. 재료비, 노무비, 경비 등 요소별 산정 근거, 단가, 수량, 적용 기준이 충분한지 확인하고 모호한 기준을 지적한다.',
        'COST_REPORT_V1',
        TRUE,
        30
    ),
    (
        'COST_REPORT',
        'COST_STATEMENT',
        'IV. 원가계산서',
        '원가계산서 본문의 항목 구성과 금액 계산 흐름이 적절한지 검토한다.',
        '원가계산서 항목별 금액과 총액 계산식을 확인해주세요.',
        '원가계산서를 검토한다. 항목 구성, 금액 계산 흐름, 부가세 또는 수수료 반영 여부, 총액 산출 과정의 오류 가능성을 확인한다.',
        'COST_REPORT_V1',
        TRUE,
        40
    ),
    (
        'COST_REPORT',
        'COST_BREAKDOWN',
        'V. 산출내역',
        '세부 산출내역의 수량, 단가, 금액, 합계가 원가계산서와 연결되는지 검토한다.',
        '산출내역의 수량 x 단가가 금액과 일치하는지 확인해주세요.',
        '산출내역을 검토한다. 수량, 단가, 금액, 합계의 계산 오류와 원가계산서 총액과의 연결 여부를 확인하고, 문서 근거를 함께 제시한다.',
        'COST_REPORT_V1',
        TRUE,
        50
    ),
    (
        'ETC_DOCUMENT',
        'COMPLETION_REPORT',
        '완료계',
        '수신인, 제목, 과업명, 발행날짜, 발행일련번호, 담당자 정보가 있는지 검토한다.',
        '완료계의 수신인과 제목, 과업명, 담당자 정보를 확인해주세요.',
        '완료계를 검토한다. 수신인, 제목, 과업명, 발행날짜, 발행일련번호, 담당자 정보가 문서에 있는지 확인하고 누락 또는 불일치 항목을 정리한다.',
        'ETC_DOCUMENT_V1',
        TRUE,
        10
    ),
    (
        'ETC_DOCUMENT',
        'INVOICE',
        '청구서',
        '수신인, 제목, 과업명, 발행날짜, 발행일련번호, 담당자, 계약금액, 신청금액, 부가세, 청구서 수신인을 검토한다.',
        '청구서의 계약금액, 신청금액, 부가세와 수신인을 확인해주세요.',
        '청구서를 검토한다. 수신인, 제목, 과업명, 발행날짜, 발행일련번호, 담당자, 계약금액, 신청금액, 부가세, 청구서상 수신인을 확인하고 금액 불일치나 누락 정보를 정리한다.',
        'ETC_DOCUMENT_V1',
        TRUE,
        20
    ),
    (
        'ETC_DOCUMENT',
        'CONTRACT_DOCUMENT',
        '계약서류',
        '계약자, 계약일, 계약금, 지불방법, 계약 일반 원칙과 특수사항을 검토한다.',
        '계약서류의 계약자와 계약일, 계약금, 지불방법을 확인해주세요.',
        '계약서류를 검토한다. 계약자, 계약일, 계약금, 지불방법, 계약의 일반 원칙과 특수사항이 명확한지 확인하고 누락 또는 위험 요소를 정리한다.',
        'ETC_DOCUMENT_V1',
        TRUE,
        30
    )
ON DUPLICATE KEY UPDATE
    category_name = VALUES(category_name),
    guide_text = VALUES(guide_text),
    example_text = VALUES(example_text),
    prompt_template = VALUES(prompt_template),
    template_version = VALUES(template_version),
    enabled = VALUES(enabled),
    sort_order = VALUES(sort_order),
    updated_at = CURRENT_TIMESTAMP;
