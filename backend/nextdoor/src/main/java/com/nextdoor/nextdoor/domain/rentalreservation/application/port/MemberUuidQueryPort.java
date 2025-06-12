package com.nextdoor.nextdoor.domain.rentalreservation.application.port;


public interface MemberUuidQueryPort {

    String getMemberUuidByRentalIdAndRole(Long rentalId, String userRole);
}