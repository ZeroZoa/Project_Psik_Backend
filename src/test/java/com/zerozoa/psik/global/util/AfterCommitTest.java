package com.zerozoa.psik.global.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AfterCommitTest {

    @AfterEach
    void cleanUp() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("트랜잭션이 없으면 즉시 실행한다")
    void noTransaction_runsImmediately() {
        // given
        AtomicBoolean ran = new AtomicBoolean(false);

        // when
        AfterCommit.run(() -> ran.set(true));

        // then
        assertThat(ran).isTrue();
    }

    @Test
    @DisplayName("트랜잭션 중에는 커밋 전까지 실행하지 않고, 커밋되면 실행한다")
    void inTransaction_runsAfterCommit() {
        // given
        TransactionSynchronizationManager.initSynchronization();
        AtomicBoolean ran = new AtomicBoolean(false);

        // when
        AfterCommit.run(() -> ran.set(true));

        // then: 아직 커밋 전
        assertThat(ran).isFalse();

        // when: 커밋 발생
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        // then
        assertThat(ran).isTrue();
    }

    @Test
    @DisplayName("롤백되면 실행하지 않는다")
    void rollback_doesNotRun() {
        // given
        TransactionSynchronizationManager.initSynchronization();
        AtomicBoolean ran = new AtomicBoolean(false);
        AfterCommit.run(() -> ran.set(true));

        // when: afterCommit 없이 롤백으로 종료
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        // then
        assertThat(ran).isFalse();
    }

    @Test
    @DisplayName("커밋 후 작업이 예외를 던져도 밖으로 전파하지 않는다")
    void taskFails_exceptionSwallowed() {
        // given
        TransactionSynchronizationManager.initSynchronization();
        AfterCommit.run(() -> {
            throw new RuntimeException("gcs down");
        });

        // when & then
        assertThatCode(() ->
                TransactionSynchronizationManager.getSynchronizations()
                        .forEach(TransactionSynchronization::afterCommit))
                .doesNotThrowAnyException();
    }
}
