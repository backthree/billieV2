// 대여 상태 상수
export const RENTAL_STATUS = {
  CREATED: "CREATED", // 1. 예약 확정 후 대여 생성 시점
  CONFIRMED: "CONFIRMED", // 2. 예약 확정 시점
  REMITTANCE_REQUESTED: "REMITTANCE_REQUESTED", // 3.Renter의 결제(송금) 요청 시점
  BEFORE_PHOTO_ANALYZED: "BEFORE_PHOTO_ANALYZED", // 2.대여 시 Owner가 물품 사진 업로드한 후 시점
  REMITTANCE_COMPLETED: "REMITTANCE_COMPLETED", // 4.Renter의 결제(송금) 완료 시점
  RENTAL_PERIOD_ENDED: "RENTAL_PERIOD_ENDED", // 5.대여 끝난 시점
  BEFORE_AND_AFTER_COMPARED: "BEFORE_AND_AFTER_COMPARED", // 6.반납시 Renter가 물품 사진 업로드한 후 시점
  RENTAL_COMPLETED: "RENTAL_COMPLETED", // 8.대여가 끝난 시점
  CANCELLED: "CANCELLED", // 9.취소 1개 사용 위치
} as const;

// 대여 처리 단계 상수 (Process)
export const RENTAL_PROCESS = {
  BEFORE_RENTAL: "BEFORE_RENTAL", // 대여 시작 등록 후
  RENTAL_IN_ACTIVE: "RENTAL_IN_ACTIVE", // 결제 완료 후
  RETURNED: "RETURNED", // 반납 관련
  RENTAL_COMPLETED: "RENTAL_COMPLETED", // 대여 완료
} as const;

// 상수 객체로부터 타입 유추
export type RentalStatus = (typeof RENTAL_STATUS)[keyof typeof RENTAL_STATUS];
export type RentalProcess =
  (typeof RENTAL_PROCESS)[keyof typeof RENTAL_PROCESS];
