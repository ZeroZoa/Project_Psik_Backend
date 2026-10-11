package com.zerozoa.psik.global.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 현재 트랜잭션이 "커밋된 뒤"에 작업을 실행하는 유틸.
 * DB와 외부 시스템(GCS, Pub/Sub 등)은 하나의 트랜잭션으로 묶을 수 없으므로,
 * 롤백될 수 있는 DB 작업을 먼저 확정한 뒤 되돌릴 수 없는 외부 작업을 수행하는 데 쓴다.
 */
@Slf4j
public final class AfterCommit {

    private AfterCommit() {
    }

    /**
     * 트랜잭션이 커밋되면 task를 실행한다. 롤백되면 실행하지 않는다.
     * 활성 트랜잭션이 없으면 즉시 실행한다.
     */
    public static void run(Runnable task) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            task.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    task.run();
                } catch (Exception e) {
                    // 커밋은 이미 끝났으므로 요청을 실패시키지 않고 로그만 남긴다
                    log.warn("[AfterCommit] 커밋 후 작업 실패", e);
                }
            }
        });
    }
}
