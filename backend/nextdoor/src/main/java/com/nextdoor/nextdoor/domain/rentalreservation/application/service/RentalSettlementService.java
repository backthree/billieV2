package com.nextdoor.nextdoor.domain.rentalreservation.application.service;

import com.nextdoor.nextdoor.domain.fintech.event.DepositCompletedEvent;
import com.nextdoor.nextdoor.domain.fintech.event.RemittanceCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.*;
import org.springframework.stereotype.Service;

/**
 * 대여 정산 서비스
 * 책임: 대여와 관련된 송금, 보증금 처리, 계좌 정보 업데이트 등 금전적인 정산 로직을 담당합니다.
 */
@Service
public interface RentalSettlementService {

    /**
     * 송금 요청을 처리합니다.
     */
    RequestRemittanceResult requestRemittance(RequestRemittanceCommand command);

    /**
     * 송금 데이터를 조회합니다.
     */
    RequestRemittanceResult getRemittanceData(Long rentalId);

    /**
     * 송금 완료 처리를 수행합니다.
     */
    void completeRemittanceProcessing(RemittanceCompletedEvent remittanceCompletedEvent);

    /**
     * 보증금 완료 처리를 수행합니다.
     */
    void completeDepositProcessing(DepositCompletedEvent depositCompletedEvent);

    /**
     * 대여 보증금 ID를 업데이트합니다.
     */
    void updateRentalDepositId(Long rentalId, Long depositId);

    /**
     * 계좌 정보를 업데이트합니다.
     */
    UpdateAccountResult updateAccount(UpdateAccountCommand command);
}