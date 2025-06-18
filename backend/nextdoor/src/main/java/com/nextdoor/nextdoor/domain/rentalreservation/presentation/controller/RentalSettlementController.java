package com.nextdoor.nextdoor.domain.rentalreservation.presentation.controller;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.RequestRemittanceCommand;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.RequestRemittanceResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.UpdateAccountCommand;
import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.UpdateAccountResult;
import com.nextdoor.nextdoor.domain.rentalreservation.application.service.RentalSettlementService;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.request.UpdateAccountRequest;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.RemittanceResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.dto.response.UpdateAccountResponse;
import com.nextdoor.nextdoor.domain.rentalreservation.presentation.mapper.RentalReservationMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rentals")
@RequiredArgsConstructor
public class RentalSettlementController {

    private final RentalSettlementService rentalSettlementService;
    private final RentalReservationMapper rentalReservationMapper;

    @GetMapping("/{rentalId}/request-remittance")
    public ResponseEntity<RemittanceResponse> requestRemittance(@PathVariable Long rentalId) {
        RequestRemittanceCommand command = rentalReservationMapper.toCommand(rentalId);
        RequestRemittanceResult result = rentalSettlementService.requestRemittance(command);
        RemittanceResponse response = rentalReservationMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{rentalId}/remittance-data")
    public ResponseEntity<RemittanceResponse> getRemittanceData(@PathVariable Long rentalId) {
        RequestRemittanceResult result = rentalSettlementService.getRemittanceData(rentalId);
        RemittanceResponse response = rentalReservationMapper.toResponse(result);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{rentalId}/account")
    public ResponseEntity<UpdateAccountResponse> updateAccount(
            @PathVariable Long rentalId,
            @Valid @RequestBody UpdateAccountRequest request) {

        UpdateAccountCommand command = rentalReservationMapper.toUpdateAccountCommand(rentalId, request);
        UpdateAccountResult result = rentalSettlementService.updateAccount(command);
        UpdateAccountResponse response = rentalReservationMapper.toUpdateAccountResponse(result);

        return ResponseEntity.ok(response);
    }
}