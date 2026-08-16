-- Worker가 실행마다 반복 조회하는 findProcessableTasks(crawlRunId, taskStatus 필터, pageNumber 정렬)를
-- 인덱스 없이 crawl_run_task 전체를 훑으며 처리하고 있었다. task 수가 많은 실행일수록 O(N^2)로 느려진다.
ALTER TABLE crawl_run_task
    ADD INDEX idx_crawl_run_task_processable (crawl_run_id, task_status, page_number, crawl_run_task_id);
