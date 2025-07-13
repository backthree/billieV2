package com.nextdoor.nextdoor.domain.rentalreservation.application.service;

import com.nextdoor.nextdoor.domain.rentalreservation.application.dto.*;
import com.nextdoor.nextdoor.domain.rentalreservation.application.port.RentalQueryPort;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.model.RentalReservation;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.exception.NoSuchRentalException;
import com.nextdoor.nextdoor.domain.rentalreservation.domain.repository.RentalReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RentalQueryService {

    private final RentalReservationRepository rentalReservationRepository;
    private final RentalQueryPort rentalQueryPort;

    public Page<SearchRentalResult> searchRentals(SearchRentalCommand command) {
        return rentalQueryPort.searchRentals(command);
    }

    @Transactional(readOnly = true)
    public SearchRentalResult getRentalById(Long rentalId) {
        return rentalQueryPort.findRentalById(rentalId)
                .orElseThrow(() -> new NoSuchRentalException("ID가 " + rentalId + "인 대여 정보가 존재하지 않습니다."));
    }

    public ManagedRentalCountResult countManagedRentals(Long ownerId) {
        return rentalQueryPort.countManagedRentals(ownerId);
    }

    @Transactional
    public DeleteRentalResult deleteRental(DeleteRentalCommand command) {
        try {
            RentalReservation rental = rentalReservationRepository.findById(command.getRentalId())
                    .orElseThrow(() -> new NoSuchRentalException("ID가 " + command.getRentalId() + "인 대여 정보가 존재하지 않습니다."));

            rentalReservationRepository.delete(rental);

            return DeleteRentalResult.builder()
                    .rentalId(command.getRentalId())
                    .success(true)
                    .message("대여 정보가 성공적으로 삭제되었습니다.")
                    .build();
        } catch (NoSuchRentalException e) {
            return DeleteRentalResult.builder()
                    .rentalId(command.getRentalId())
                    .success(false)
                    .message(e.getMessage())
                    .build();
        } catch (Exception e) {
            return DeleteRentalResult.builder()
                    .rentalId(command.getRentalId())
                    .success(false)
                    .message("대여 정보 삭제 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
        }
    }
}