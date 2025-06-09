package com.nextdoor.nextdoor.domain.rentalreservation.service;

import com.nextdoor.nextdoor.domain.fintech.event.DepositCompletedEvent;
import com.nextdoor.nextdoor.domain.fintech.event.RemittanceCompletedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.event.RentalReservationConfirmedEvent;
import com.nextdoor.nextdoor.domain.rentalreservation.service.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
public interface RentalService {

    UploadImageResult registerBeforePhoto(UploadImageCommand command);
    RequestRemittanceResult requestRemittance(RequestRemittanceCommand command);
    RequestRemittanceResult getRemittanceData(Long rentalId);
    void completeRemittanceProcessing(RemittanceCompletedEvent remittanceCompletedEvent);
    UploadImageResult registerAfterPhoto(UploadImageCommand command);
    Page<SearchRentalResult> searchRentals(SearchRentalCommand command);
    void completeDepositProcessing(DepositCompletedEvent depositCompletedEvent);
    void updateRentalDepositId(Long rentalId, Long depositId);
    AiComparisonResult getAiAnalysis(Long rentalId);
    void updateDamageAnalysis(Long rentalId, String damageAnalysis);
    void updateComparedAnalysis(Long rentalId, String comparedAnalysis);
    UpdateAccountResult updateAccount(UpdateAccountCommand command);
    ManagedRentalCountResult countManagedRentals(Long ownerId);
    SearchRentalResult getRentalById(Long rentalId);
    DeleteRentalResult deleteRental(DeleteRentalCommand command);
    void createAiImageComparisonPair(Long rentalId, Long beforeImageId, Long afterImageId, String pairComparisonResult);
    void deleteAiImageComparisonPairByRentalId(Long rentalId);
    void createFromRentalReservation(RentalReservationConfirmedEvent event);
}