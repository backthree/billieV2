// lib/hooks/widget/useRentalWebSocket.ts
import { useState, useEffect, useCallback, useRef } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import useUserStore from "@/lib/store/useUserStore";
import {
  RentalProcess,
  RentalStatus,
  RENTAL_PROCESS,
  RENTAL_STATUS,
} from "@/types/rental/status";
import { fetchRentals } from "@/lib/api/rental/request";

// 렌탈 상태
export interface RentalStatusMessage {
  startDate: string;
  endDate: string;
  rentalId: number;
  process: RentalProcess;
  detailStatus: RentalStatus;
  rentalDetail?: {
    title: string;
    rentalId: number;
    charge: number;
    deposit: number;
    renterId: number;
    ownerId: number;
    productImageUrl: string;
    ownerProfileImageUrl: string;
    renterProfileImageUrl: string;
  };
}

// 훅 반환 타입
export interface UseRentalWebSocketReturn {
  activeRentals: RentalStatusMessage[];
  isConnected: boolean;
  loading: boolean;
  error: string | null;
  refreshRentals: () => Promise<void>;
}

export default function useRentalWebSocket(): UseRentalWebSocketReturn {
  const [activeRentals, setActiveRentals] = useState<RentalStatusMessage[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const stompClientRef = useRef<Client | null>(null);
  const lastRefreshTimeRef = useRef<number>(0);

  const { userId } = useUserStore();

  const fetchRentalData = useCallback(async () => {
    const now = Date.now();
    if (now - lastRefreshTimeRef.current < 1000) {
      // console.log("[대여-API] 요청 제한: 이전 요청으로부터 1초가 지나지 않았습니다.");
      return;
    }
    lastRefreshTimeRef.current = now;

    if (!userId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    // console.log("[대여-API] 렌탈 데이터 요청 시작 - userId:", userId);

    try {
      const ownerRentals = await fetchRentals({
        userId,
        userRole: "OWNER",
        condition: "active",
      });

      const renterRentals = await fetchRentals({
        userId,
        userRole: "RENTER",
        condition: "active",
      });

      // console.log("[대여-API] 오너 렌탈 데이터 개수:", ownerRentals.length);
      // console.log("[대여-API] 렌터 렌탈 데이터 개수:", renterRentals.length);
      // console.log(
      //   "[대여-API] 오너 렌탈 데이터 샘플:",
      //   ownerRentals.length > 0 ? ownerRentals[0] : "empty"
      // );
      // console.log(
      //   "[대여-API] 렌터 렌탈 데이터 샘플:",
      //   renterRentals.length > 0 ? renterRentals[0] : "empty"
      // );

      // if (ownerRentals.length > 0) {
      //   console.log("[대여-API] 프로필 이미지 필드 확인:", {
      //     ownerProfileImageUrl: ownerRentals[0].ownerProfileImageUrl,
      //     renterProfileImageUrl: ownerRentals[0].renterProfileImageUrl,
      //     productImageUrls: ownerRentals[0].productImageUrls,
      //   });
      // }

      // 합치기
      const allRentals = [...ownerRentals, ...renterRentals].map(
        (rental: any) => {
          console.log("[대여-API] 렌탈 상태 정보:", {
            rentalId: rental.rentalId,
            process: rental.rentalProcess,
            status: rental.rentalStatus,
            title: rental.title
          });
          return {
            startDate: rental.startDate,
            endDate: rental.endDate,
            rentalId: rental.rentalId,
            process: rental.rentalProcess,
            detailStatus: rental.rentalStatus,
            rentalDetail: {
              title: rental.title,
              rentalId: rental.rentalId,
              charge: rental.rentalFee,
              deposit: rental.deposit,
              renterId: rental.renterId,
              ownerId: rental.ownerId,
              // productImageUrl: rental.productImageUrl,
              // 배열의 첫 번째 항목 사용
              productImageUrl:
                rental.productImageUrls && rental.productImageUrls.length > 0
                  ? rental.productImageUrls[0]
                  : rental.productImageUrl || "",
              ownerProfileImageUrl:
                rental.ownerProfileImageUrl || "/images/profileimg.png",
              renterProfileImageUrl:
                rental.renterProfileImageUrl || "/images/profileimg.png",
            },
          };
        }
      );

      // console.log("[대여-API] 처리된 렌탈 목록:", allRentals);

      setActiveRentals(allRentals);
      setError(null);
    } catch (err) {
      console.error("[대여-API] 대여 목록 요청 실패:", err);
      setError("대여 목록을 불러오는 데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  }, [userId]);

  const setupWebSocketConnection = useCallback(() => {
    const token = localStorage.getItem("accessToken");
    const uuid = localStorage.getItem("uuid");

    // console.log(
    //   "[대여-웹소켓] 연결 시도 - token 존재:",
    //   !!token,
    //   "uuid 존재:",
    //   !!uuid,
    //   "userId 존재:",
    //   !!userId
    // );

    if (!token || !uuid) {
      setLoading(false);
      setIsConnected(false);
      return;
    }

    if (stompClientRef.current && stompClientRef.current.connected) {
      try {
        stompClientRef.current.unsubscribe(`/topic/rental-reservation/${uuid}/status`);
        stompClientRef.current.deactivate();
      } catch (e) {
        console.error("[대여-웹소켓] 기존 연결 해제 오류:", e);
      }
    }

    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL;
      const socket = new SockJS(`${baseUrl}/ws-rental`);
      // console.log("[대여-웹소켓] 소켓 생성:", `${baseUrl}/ws-rental`);

      // STOMP 클라이언트
      const client = new Client({
        webSocketFactory: () => socket,
        debug: (str) => {
          // console.log("STOMP Rental: " + str);
        },
        reconnectDelay: 0,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        connectHeaders: {
          Authorization: `Bearer ${token}`,
        },
      });

      client.onConnect = () => {
        setIsConnected(true);
        // console.log("[대여-웹소켓] 연결 성공");

        // console.log("[대여-웹소켓] 토픽 구독:", `/topic/rental-reservation/${uuid}/status`);
        client.subscribe(`/topic/rental-reservation/${uuid}/status`, (message) => {
          try {
            const rentalStatusData: RentalStatusMessage = JSON.parse(
              message.body
            );
            console.log("[대여-웹소켓] 수신 데이터:", rentalStatusData);

            const profileInfo = {
              "rentalDetail 존재": !!rentalStatusData.rentalDetail,
              ownerProfileImageUrl:
                rentalStatusData.rentalDetail?.ownerProfileImageUrl,
              renterProfileImageUrl:
                rentalStatusData.rentalDetail?.renterProfileImageUrl,
              productImageUrl: rentalStatusData.rentalDetail?.productImageUrl,
            };
            console.log("[대여-웹소켓] 프로필 이미지 정보:", profileInfo);

            handleRentalUpdate(rentalStatusData);

            setTimeout(() => {
              fetchRentalData();
            }, 500);
          } catch (e) {
            console.error("[대여-웹소켓] 메시지 처리 오류:", e);
          }
        });

        // console.log("[대여-웹소켓] 초기 데이터 로드 시작");
        fetchRentalData();
      };

      client.onStompError = (frame) => {
        console.error("[대여-웹소켓] STOMP 오류:", frame.headers["message"]);
        console.error("[대여-웹소켓] 추가 정보:", frame.body);
        setError("서버 연결에 문제가 발생했습니다.");
        setIsConnected(false);

        // 오류 발생 시 5초 후 재연결 시도
        setTimeout(() => {
          console.log("[대여-웹소켓] 연결 오류 후 재시도");
          setupWebSocketConnection();
        }, 5000);
      };

      client.onWebSocketClose = () => {
        // console.log("[대여-웹소켓] 연결 종료");
        setIsConnected(false);

        setTimeout(() => {
          // console.log("[대여-웹소켓] 연결 종료 후 재시도");
          setupWebSocketConnection();
        }, 5000);
      };

      client.onWebSocketError = (error) => {
        console.error("[대여-웹소켓] 웹소켓 오류:", error);
        setIsConnected(false);

        setTimeout(() => {
          console.log("[대여-웹소켓] 웹소켓 오류 후 재시도");
          setupWebSocketConnection();
        }, 5000);
      };

      // console.log("[대여-웹소켓] 활성화 중...");
      client.activate();

      stompClientRef.current = client;
    } catch (e) {
      console.error("[대여-웹소켓] 연결 오류:", e);
      setError("웹소켓 연결에 실패했습니다.");
      setIsConnected(false);

      setTimeout(() => {
        console.log("[대여-웹소켓] 예외 후 재연결 시도");
        setupWebSocketConnection();
      }, 5000);
    }
  }, [userId, fetchRentalData]);

  useEffect(() => {
    setupWebSocketConnection();

    const handleNetworkChange = () => {
      if (navigator.onLine) {
        console.log("[대여-웹소켓] 네트워크 다시 연결됨. 웹소켓 재연결 시도");
        setupWebSocketConnection();
      }
    };

    window.addEventListener("online", handleNetworkChange);

    const connectionCheckInterval = setInterval(() => {
      if (!isConnected) {
        console.log("[대여-웹소켓] 연결 상태 확인: 재연결 필요");
        setupWebSocketConnection();
      }
    }, 15000); // 15초마다 체크

    return () => {
      if (stompClientRef.current && stompClientRef.current.connected) {
        try {
          const uuid = localStorage.getItem("uuid");
          if (uuid) {
            // console.log("[대여-웹소켓] 연결 해제 중...");
            stompClientRef.current?.unsubscribe(`/topic/rental-reservation/${uuid}/status`);
            stompClientRef.current.deactivate();
          }
        } catch (e) {
          // console.error("[대여-웹소켓] 종료 오류:", e);
        }
      }
      window.removeEventListener("online", handleNetworkChange);
      clearInterval(connectionCheckInterval);
    };
  }, [userId, setupWebSocketConnection, isConnected]);

  const handleRentalUpdate = (updatedRental: RentalStatusMessage) => {
    console.log(
      "[대여-웹소켓] 렌탈 업데이트 처리 - rentalId:",
      updatedRental.rentalId
    );

    setActiveRentals((prev) => {
      const existingIndex = prev.findIndex(
        (r) => r.rentalId === updatedRental.rentalId
      );
      console.log("[대여-웹소켓] 기존 인덱스:", existingIndex);

      if (existingIndex >= 0) {
        const currentItem = prev[existingIndex];
        const statusChanged =
          currentItem.process !== updatedRental.process ||
          currentItem.detailStatus !== updatedRental.detailStatus;

        if (statusChanged) {
          console.log("[대여-웹소켓] 상태 변경 감지:", {
            old: {
              process: currentItem.process,
              detailStatus: currentItem.detailStatus,
            },
            new: {
              process: updatedRental.process,
              detailStatus: updatedRental.detailStatus,
            },
          });
        }

        // 기존 대여 업데이트한다
        const updated = [...prev];
        updated[existingIndex] = {
          ...updated[existingIndex],
          process: updatedRental.process,
          detailStatus: updatedRental.detailStatus,
          rentalDetail: {
            ...updated[existingIndex].rentalDetail,
            ...updatedRental.rentalDetail,
            title:
              updatedRental.rentalDetail?.title ||
              updated[existingIndex].rentalDetail?.title ||
              "",
            rentalId:
              updatedRental.rentalDetail?.rentalId ||
              updated[existingIndex].rentalDetail?.rentalId ||
              0,
            charge:
              updatedRental.rentalDetail?.charge ||
              updated[existingIndex].rentalDetail?.charge ||
              0,
            deposit:
              updatedRental.rentalDetail?.deposit ||
              updated[existingIndex].rentalDetail?.deposit ||
              0,
            renterId:
              updatedRental.rentalDetail?.renterId ||
              updated[existingIndex].rentalDetail?.renterId ||
              0,
            ownerId:
              updatedRental.rentalDetail?.ownerId ||
              updated[existingIndex].rentalDetail?.ownerId ||
              0,
            ownerProfileImageUrl:
              updatedRental.rentalDetail?.ownerProfileImageUrl ||
              updated[existingIndex].rentalDetail?.ownerProfileImageUrl ||
              "/images/profileimg.png",
            renterProfileImageUrl:
              updatedRental.rentalDetail?.renterProfileImageUrl ||
              updated[existingIndex].rentalDetail?.renterProfileImageUrl ||
              "/images/profileimg.png",
            productImageUrl:
              updatedRental.rentalDetail?.productImageUrl ||
              updated[existingIndex].rentalDetail?.productImageUrl ||
              "",
          },
        };
        console.log("[대여-웹소켓] 업데이트된 렌탈:", updated[existingIndex]);
        return updated;
      } else {
        // 새 대여 추가시에는 프로필 이미지가 없는 경우 기본값 설정
        const newRental = {
          ...updatedRental,
          rentalDetail: {
            title: updatedRental.rentalDetail?.title || "",
            rentalId: updatedRental.rentalDetail?.rentalId || 0,
            charge: updatedRental.rentalDetail?.charge || 0,
            deposit: updatedRental.rentalDetail?.deposit || 0,
            renterId: updatedRental.rentalDetail?.renterId || 0,
            ownerId: updatedRental.rentalDetail?.ownerId || 0,
            ownerProfileImageUrl:
              updatedRental.rentalDetail?.ownerProfileImageUrl ||
              "/images/profileimg.png",
            renterProfileImageUrl:
              updatedRental.rentalDetail?.renterProfileImageUrl ||
              "/images/profileimg.png",
            productImageUrl: updatedRental.rentalDetail?.productImageUrl || "",
          },
        };
        console.log("[대여-웹소켓] 새 렌탈 추가:", newRental);
        return [newRental, ...prev];
      }
    });
  };

  // 외부에 노출할 새로고침 함수에 강제 웹소켓 재연결 로직 추가
  const refreshRentalsWithReconnect = useCallback(async () => {
    await fetchRentalData();

    // 연결이 끊어진 경우 재연결 시도
    if (!isConnected) {
      console.log("[대여-웹소켓] 수동 새로고침 중 연결 다시 시도");
      setupWebSocketConnection();
    }
  }, [fetchRentalData, isConnected, setupWebSocketConnection]);

  return {
    activeRentals,
    isConnected,
    loading,
    error,
    refreshRentals: refreshRentalsWithReconnect,
  };
}
