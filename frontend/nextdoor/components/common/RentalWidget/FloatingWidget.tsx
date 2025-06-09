// components/FloatingWidget.tsx
"use client";

import React, { useState, useEffect, useRef } from "react";
import Image from "next/image";
import Draggable from "react-draggable";
import useReservationWebSocket from "@/lib/hooks/widget/useReservationWebSocket";
import useRentalWebSocket, {
  RentalStatusMessage,
} from "@/lib/hooks/widget/useRentalWebSocket";
import ReservationActionButton from "./ReservationActionButton";
import RentalActionButton from "./RentalActionButton";
import useAlertModal from "@/lib/hooks/alert/useAlertModal";
import useUserStore from "@/lib/store/useUserStore";
import { Portal } from "@/lib/utils/widget/portal";
import PaymentApplyModal from "@/components/pays/modals/PaymentApplyModal";

const FloatingWidget: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);
  const [position, setPosition] = useState({ x: 20, y: 200 });
  const [isShining, setIsShining] = useState(false);
  const [prevRequestCount, setPrevRequestCount] = useState(0);
  const [hiddenItemIds, setHiddenItemIds] = useState<number[]>([]);
  const [shiningItems, setShiningItems] = useState<number[]>([]);
  const [prevRentals, setPrevRentals] = useState<RentalStatusMessage[]>([]);
  const [prevReservations, setPrevReservations] = useState<any[]>([]);

  const dragStartPos = useRef({ x: 0, y: 0 });
  const wasDragged = useRef(false);
  const nodeRef = useRef(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedRental, setSelectedRental] = useState<{
    charge: number;
    rentalId: string;
  } | null>(null);

  const { userId } = useUserStore();

  // 예약 WebSocket 훅
  const {
    pendingReservations,
    isConnected,
    loading,
    error,
    confirmReservation,
    cancelReservation,
  } = useReservationWebSocket();

  // 대여 WebSocket 훅
  const {
    activeRentals,
    isConnected: isRentalConnected,
    loading: rentalLoading,
    error: rentalError,
    refreshRentals,
  } = useRentalWebSocket();

  const { showAlert } = useAlertModal();

  const savePosition = (pos: { x: number; y: number }) => {
    if (typeof window !== "undefined") {
      localStorage.setItem("widgetPosition", JSON.stringify(pos));
    }
  };

  useEffect(() => {
    if (isOpen) {
      refreshRentals(); // 서버에서 최신 데이터 가져오기
    }
  }, [isOpen, refreshRentals]);

  useEffect(() => {
    const refreshTimer = setInterval(() => {
      refreshRentals();
    }, 300000); // 5분 = 300,000 밀리초

    return () => {
      clearInterval(refreshTimer);
    };
  }, [refreshRentals]);

  useEffect(() => {
    if (typeof window !== "undefined") {
      const hiddenItemsJson = localStorage.getItem("hiddenWidgetItems");
      if (hiddenItemsJson) {
        try {
          const parsedItems = JSON.parse(hiddenItemsJson);
          const currentTime = Date.now();

          const validHiddenItems = parsedItems.filter(
            (item: { id: number; timestamp: number }) => {
              return currentTime - item.timestamp < 180000;
            }
          );

          setHiddenItemIds(
            validHiddenItems.map((item: { id: number }) => item.id)
          );

          localStorage.setItem(
            "hiddenWidgetItems",
            JSON.stringify(validHiddenItems)
          );
        } catch (e) {
          console.error("숨김 항목 정보 파싱 오류:", e);
          localStorage.removeItem("hiddenWidgetItems");
        }
      }
    }
  }, []);

  useEffect(() => {
    const isInitialLoad =
      prevRentals.length === 0 && prevReservations.length === 0;

    if (!isInitialLoad) {
      const newShiningItems: number[] = [];

      pendingReservations.forEach((res) => {
        const found = prevReservations.find(
          (prev) => prev.reservationId === res.reservationId
        );
        if (!found) {
          newShiningItems.push(res.reservationId);
        }
      });

      actionNeededRentals.forEach((rental) => {
        const found = prevRentals.find(
          (prev) => prev.rentalId === rental.rentalId
        );
        if (!found) {
          newShiningItems.push(rental.rentalId);
        }
      });

      if (newShiningItems.length > 0) {
        console.log("새 항목 감지: ", newShiningItems);
        setShiningItems(newShiningItems);

        // 10초
        setTimeout(() => {
          setShiningItems([]);
        }, 10000);
      }
    }

    setPrevRentals(activeRentals);
    setPrevReservations(pendingReservations);
  }, [activeRentals, pendingReservations]);

  const actionNeededRentals = activeRentals.filter((rental) => {
    if (hiddenItemIds.includes(rental.rentalId)) {
      return false;
    }

    const isOwner = userId === rental.rentalDetail?.ownerId;
    const isRenter = userId === rental.rentalDetail?.renterId;

    if (isOwner) {
      return (
        (rental.process === "BEFORE_RENTAL" &&
          (rental.detailStatus === "CREATED" || rental.detailStatus === "CONFIRMED")) ||
        (rental.process === "RETURNED" &&
          rental.detailStatus === "RENTAL_PERIOD_ENDED") ||
        (rental.process === "RETURNED" &&
          rental.detailStatus === "BEFORE_AND_AFTER_COMPARED")
      );
    } else if (isRenter) {
      return (
        (rental.process === "BEFORE_RENTAL" &&
          rental.detailStatus === "REMITTANCE_REQUESTED") ||
        (rental.process === "BEFORE_RENTAL" &&
          rental.detailStatus === "BEFORE_PHOTO_ANALYZED")
      );
    }

    return false;
  });

  const totalRequestCount =
    pendingReservations.length + actionNeededRentals.length;

  useEffect(() => {
    if (totalRequestCount > prevRequestCount) {
      setIsShining(true);

      const timer = setTimeout(() => {
        setIsShining(false);
      }, 5000);

      return () => clearTimeout(timer);
    }

    setPrevRequestCount(totalRequestCount);
  }, [totalRequestCount, prevRequestCount]);

  const updatePosition = () => {
    if (typeof window !== "undefined") {
      const savedPosition = localStorage.getItem("widgetPosition");
      if (savedPosition) {
        try {
          const parsed = JSON.parse(savedPosition);

          const viewportWidth = window.innerWidth;
          const viewportHeight = window.innerHeight;

          let safeX, safeY;

          const isOnRightSide = parsed.x > viewportWidth / 2;

          if (isOnRightSide) {
            safeX = Math.min(viewportWidth * 0.8, viewportWidth - 60);
          } else {
            safeX = Math.max(viewportWidth * 0.2, 60);
          }

          const heightRatio = parsed.y / viewportHeight;
          safeY = viewportHeight * heightRatio;

          safeY = Math.min(Math.max(20, safeY), viewportHeight - 80);

          setPosition({ x: safeX, y: safeY });
          console.log(" 조정:", { safeX, safeY });
        } catch (e) {
          console.error("위치 정보 파싱 오류:", e);

          setPosition({ x: window.innerWidth - 80, y: window.innerHeight / 2 });
        }
      } else {
        setPosition({ x: window.innerWidth - 80, y: window.innerHeight / 2 });
      }
    }
  };

  useEffect(() => {
    // 초기 위치 설정
    if (!localStorage.getItem("widgetPosition")) {
      const defaultX = window.innerWidth - 80;
      const defaultY = window.innerHeight / 2;
      setPosition({ x: defaultX, y: defaultY });
      savePosition({ x: defaultX, y: defaultY });
    } else {
      updatePosition();
    }

    // 화면 크기 변경 감지
    window.addEventListener("resize", updatePosition);

    return () => {
      savePosition(position);
      window.removeEventListener("resize", updatePosition);
    };
  }, []);

  useEffect(() => {
    savePosition(position);
  }, [position]);

  // 드래그
  const handleStart = (e: any, data: { x: number; y: number }) => {
    dragStartPos.current = { x: data.x, y: data.y };
    wasDragged.current = false;
  };

  const handleDrag = (e: any, data: { x: number; y: number }) => {
    const dx = Math.abs(data.x - dragStartPos.current.x);
    const dy = Math.abs(data.y - dragStartPos.current.y);

    if (dx > 5 || dy > 5) {
      wasDragged.current = true;
    }

    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    const safeX = Math.min(Math.max(0, data.x), viewportWidth - 60);
    const safeY = Math.min(Math.max(0, data.y), viewportHeight - 60);

    setPosition({ x: safeX, y: safeY });
  };

  const handleStop = (e: any, data: { x: number; y: number }) => {
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    const safeX = Math.min(Math.max(0, data.x), viewportWidth - 60);
    const safeY = Math.min(Math.max(0, data.y), viewportHeight - 60);

    setPosition({ x: safeX, y: safeY });

    if (!wasDragged.current) {
      setIsOpen((prev) => !prev);
    }
  };

  const handleRentalAction = (rental: RentalStatusMessage) => {
    const { process, detailStatus } = rental;
    const isOwner = userId === rental.rentalDetail?.ownerId;

    const start = new Date(rental.startDate);
    const end = new Date(rental.endDate);
    const diffDays =
      Math.ceil((end.getTime() - start.getTime()) / (1000 * 60 * 60 * 24)) + 1;

    if (isOwner && process === "BEFORE_RENTAL" && 
        (detailStatus === "CREATED" || detailStatus === "CONFIRMED")) {
      const baseCharge = rental.rentalDetail?.charge || 0;
      const totalCharge = baseCharge * diffDays;

      setSelectedRental({
        charge: totalCharge,
        rentalId: rental.rentalId.toString(),
      });
      setIsModalOpen(true);
      setIsOpen(false); // 위젯 닫기
    }
  };

  const handleRentalNavigate = (rentalId: number) => {
    // 위젯 닫기
    setIsOpen(false);

    setHiddenItemIds((prev) => [...prev, rentalId]);
  };

  // 예약 확정/취소
  const handleReservationAction = async (
    reservationId: number,
    actionType?: string
  ) => {
    try {
      if (actionType === "confirm") {
        const success = await confirmReservation(reservationId);
        if (success) {
          showAlert("예약 확정", "예약이 확정되었습니다.", "success");

          // 확정된 예약 정보 찾기
          const confirmedReservation = pendingReservations.find(
            (res) => res.reservationId === reservationId
          );

          if (confirmedReservation) {
            // 초기 데이터가 반영될 시간을 확보하기 위해 짧은 지연 후 대여 목록 새로고침
            setTimeout(() => {
              refreshRentals();
            }, 500);
          }
        } else {
          showAlert("오류 발생", "예약 확정 중 문제가 발생했습니다.", "error");
        }
      } else if (actionType === "cancel") {
        const success = await cancelReservation(reservationId);
        if (success) {
          showAlert("예약 취소", "예약이 취소되었습니다.", "info");
        } else {
          showAlert("오류 발생", "예약 취소 중 문제가 발생했습니다.", "error");
        }
      }
    } catch (error) {
      console.error("예약 처리 실패:", error);
      showAlert("오류 발생", "요청 처리 중 문제가 발생했습니다.", "error");
    }
  };

  // 예약 항목 처리
  const handleReservationNavigate = (reservationId: number) => {
    // 위젯 닫기
    setIsOpen(false);

    // 해당 항목을 임시로 숨김 처리
    setHiddenItemIds((prev) => [...prev, reservationId]);
  };

  const getRentalActionLink = (rental: RentalStatusMessage) => {
    const { rentalId, process, detailStatus } = rental;
    const isOwner = userId === rental.rentalDetail?.ownerId;

    if (isOwner && process === "BEFORE_RENTAL" && detailStatus === "CREATED") {
      return "";
    }

    if (isOwner) {
      switch (process) {
        case "BEFORE_RENTAL":
          if (detailStatus === "CREATED") {
            return "reservations/lend";
          }
          break;
        case "RETURNED":
          if (detailStatus === "RENTAL_PERIOD_ENDED") {
            return `/safe-deal/${rentalId}/after/photos-register`;
          } else if (detailStatus === "BEFORE_AND_AFTER_COMPARED") {
            return `/safe-deal/${rentalId}/after/analysis`;
          }
          break;
      }
    } else {
      // 렌터인 경우
      switch (process) {
        case "BEFORE_RENTAL":
          if (detailStatus === "REMITTANCE_REQUESTED") {
            return `/safe-deal/${rentalId}/before/photos-register`;
          } else if (detailStatus === "BEFORE_PHOTO_ANALYZED") {
            return `/safe-deal/${rentalId}/before/payment`;
          }
          break;
      }
    }

    return "";
  };

  // 렌탈 버튼 텍스트
  const getRentalButtonText = (rental: RentalStatusMessage) => {
    const { process, detailStatus } = rental;
    const isOwner = userId === rental.rentalDetail?.ownerId;

    if (isOwner) {
      switch (process) {
        case "BEFORE_RENTAL":
          return detailStatus === "CONFIRMED"
            ? "안심 결제 요청"
            : "안심 결제 요청됨";
        case "RENTAL_IN_ACTIVE":
          return "물품 결제 완료";
        case "RETURNED":
          // 모든 "RETURNED" 상태에서 "안심 반납 처리"로 표시
          return "안심 반납 처리";
        case "RENTAL_COMPLETED":
          return "거래 완료";
        default:
          return "처리하기";
      }
    } else {
      switch (process) {
        case "BEFORE_RENTAL":
          return detailStatus === "REMITTANCE_REQUESTED" ||
            detailStatus === "BEFORE_PHOTO_ANALYZED"
            ? "안심 결제"
            : "안심 결제 대기";
        case "RENTAL_IN_ACTIVE":
          return "물품 결제 완료";
        case "RETURNED":
          return "반납 중";
        case "RENTAL_COMPLETED":
          return "거래 완료";
        default:
          return "처리하기";
      }
    }
  };

  const shiningClass = isShining
    ? "drop-shadow-lg select-none w-auto h-auto animate-shine"
    : "drop-shadow-lg select-none w-auto h-auto";

  // 드래그 가능 -- 항상 최상단에
  return (
    <Portal>
      <style jsx global>{`
        @keyframes shine {
          0% {
            transform: scale(1);
            filter: brightness(1);
          }
          25% {
            transform: scale(1.1);
            filter: brightness(1.5) drop-shadow(0 0 8px #4a9df5);
          }
          50% {
            transform: scale(1);
            filter: brightness(1);
          }
          75% {
            transform: scale(1.1);
            filter: brightness(1.5) drop-shadow(0 0 8px #4a9df5);
          }
          100% {
            transform: scale(1);
            filter: brightness(1);
          }
        }

        .animate-shine {
          animation: shine 2s ease-in-out;
          animation-iteration-count: 3;
        }

        @keyframes item-shine {
          0% {
            transform: translateX(0);
            background-color: rgba(255, 255, 255, 1); /* 완전 불투명한 흰색 */
          }
          25% {
            transform: translateX(5px);
            background-color: rgba(237, 242, 255, 1); /* 연한 파란색 배경 */
            box-shadow: 0 0 8px rgba(74, 157, 245, 0.6); /* 글로우 효과 추가 */
          }
          50% {
            transform: translateX(0);
            background-color: rgba(255, 255, 255, 1);
          }
          75% {
            transform: translateX(5px);
            background-color: rgba(237, 242, 255, 1);
            box-shadow: 0 0 8px rgba(74, 157, 245, 0.6);
          }
          100% {
            transform: translateX(0);
            background-color: rgba(255, 255, 255, 1);
          }
        }

        .animate-item-shine {
          animation: item-shine 2s ease-in-out;
          animation-iteration-count: 2;
        }
      `}</style>

      <Draggable
        nodeRef={nodeRef}
        defaultPosition={position}
        scale={2}
        onStart={handleStart}
        onDrag={handleDrag}
        onStop={handleStop}
      >
        <div
          ref={nodeRef}
          className="fixed z-[99999]"
          style={{
            touchAction: "none",
            cursor: "move",
            pointerEvents: "auto",
            left: position.x,
            top: position.y,
            transform: "translate3d(0,0,0)", // 하드웨어 가속 활성화
            willChange: "transform", // 성능 최적화
          }}
        >
          <div className="w-14 h-14 flex items-center justify-center">
            <Image
              src="/images/blueStar.png"
              alt="위젯 열기/닫기"
              width={56}
              height={56}
              priority
              className={shiningClass}
              draggable="false"
            />
            {totalRequestCount > 0 && (
              <div className="absolute -top-1 -right-1 bg-red-400 text-white rounded-full w-5 h-5 flex items-center justify-center text-xs font-bold">
                {totalRequestCount}
              </div>
            )}
          </div>
        </div>
      </Draggable>

      {isOpen && (
        <div
          className="fixed inset-0 bg-black bg-opacity-70 z-[99998] overflow-auto p-2"
          style={{
            width: "100vw",
            height: "100vh",
            maxWidth: "none",
            margin: "0",
            transform: "translate3d(0,0,0)", // 하드웨어 가속 활성화
          }}
        >
          {/* 헤더 */}
          <div className="px-3 md:px-5 py-2 md:py-4 flex items-center justify-between text-white">
            <h2 className="text-xl md:text-3xl font-extrabold mt-5 md:mt-10">
              원클릭 빌리 매니저
              <p className="text-blue-300 font-medium text-base md:text-xl mt-1 md:mt-2 mb-4 md:mb-8">
                {totalRequestCount > 0
                  ? "거래가 멈췄어요! 지금 이어가볼까요?"
                  : "현재 처리할 거래가 없습니다."}
              </p>
            </h2>
          </div>

          {/* 모달 콘텐츠 */}
          <div className="px-5 py-2 text-white max-w-md mx-auto">
            <div className="space-y-4 mb-8">
              {/* 예약 요청 항목 */}
              {loading ? (
                <div className="text-center py-4 text-gray-300">
                  <p>예약 정보 로딩 중...</p>
                </div>
              ) : pendingReservations.length > 0 ? (
                [...pendingReservations].reverse().map((reservation: any, index: number) => (
                  <ReservationActionButton
                    key={`reservation-${reservation.reservationId || index}`}
                    id={reservation.reservationId}
                    title={reservation.postTitle}
                    productImage={
                      reservation.postProductImage ||
                      reservation.postProductImages ||
                      "/icons/icon72.png"
                    }
                    profileImage={
                      reservation.renterProfileImageUrl ||
                      "/images/profileimg.png"
                    }
                    isReservation={true}
                    isOwner={true} // 예약 요청은 항상 오너에게 오는 것
                    onConfirm={() =>
                      handleReservationAction(
                        reservation.reservationId,
                        "confirm"
                      )
                    }
                    onCancel={() =>
                      handleReservationAction(
                        reservation.reservationId,
                        "cancel"
                      )
                    }
                    onNavigate={handleReservationNavigate}
                    isShining={shiningItems.includes(reservation.reservationId)}
                  />
                ))
              ) : null}

              {/* 대여 요청 항목 */}
              {!rentalLoading &&
                actionNeededRentals.length > 0 &&
                [...actionNeededRentals].reverse().map((rental) => {
                  const isOwner = userId === rental.rentalDetail?.ownerId;
                  const actionLink = getRentalActionLink(rental);
                  const buttonText = getRentalButtonText(rental);
                  const displayTitle =
                    rental.rentalDetail?.title || "대여 물품";

                  // 안심결제요청 버튼인지 확인
                  const isPaymentRequestButton =
                    isOwner &&
                    rental.process === "BEFORE_RENTAL" &&
                    rental.detailStatus === "CONFIRMED";

                  return (
                    <RentalActionButton
                      key={`rental-${rental.rentalId}`}
                      rentalId={rental.rentalId}
                      title={displayTitle}
                      isOwner={isOwner}
                      productImage={
                        rental.rentalDetail?.productImageUrl ||
                        "/icons/icon72.png"
                      }
                      profileImage={
                        isOwner
                          ? rental.rentalDetail?.renterProfileImageUrl ||
                            "/images/profileimg.png" // 사용자가 오너면 렌터 프로필
                          : rental.rentalDetail?.ownerProfileImageUrl ||
                            "/images/profileimg.png" // 사용자가 렌터면 오너 프로필
                      }
                      buttonText={buttonText}
                      actionLink={actionLink}
                      onClick={
                        isPaymentRequestButton
                          ? () => handleRentalAction(rental)
                          : undefined
                      }
                      onNavigate={handleRentalNavigate}
                      isShining={shiningItems.includes(rental.rentalId)}
                    />
                  );
                })}

              {/* 전체 로딩 중이거나 데이터가 없는 경우 */}
              {(loading || rentalLoading) && totalRequestCount === 0 ? (
                <div className="text-center py-4 text-gray-300">
                  <p>거래 정보 로딩 중...</p>
                </div>
              ) : totalRequestCount === 0 ? (
                <div className="text-center py-4 text-gray-300">
                  <p>처리할 거래 내역이 없습니다.</p>
                </div>
              ) : null}

              {/* 연결 상태 표시 */}
              {!isConnected && !isRentalConnected && (
                <div className="text-center py-2 text-yellow-300 text-sm">
                  <p>서버 연결 중~~~~~</p>
                </div>
              )}
            </div>

            {/* 닫기 버튼 */}
            <div className="text-center mt-8 mb-6">
              <button
                onClick={() => setIsOpen(false)}
                className="bg-gray-700 hover:bg-gray-600 text-white px-8 py-3 rounded-full text-sm font-medium"
              >
                닫기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 안심결제 모달 */}
      {isModalOpen && selectedRental && (
        <PaymentApplyModal
          charge={selectedRental.charge}
          rentalId={selectedRental.rentalId}
          setIsModalOpen={setIsModalOpen}
        />
      )}
    </Portal>
  );
};

export default FloatingWidget;
