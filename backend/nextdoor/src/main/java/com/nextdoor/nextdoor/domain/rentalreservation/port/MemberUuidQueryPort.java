package com.nextdoor.nextdoor.domain.rentalreservation.port;


public interface MemberUuidQueryPort {

    String getMemberUuidByRentalIdAndRole(Long rentalId, String userRole);
}