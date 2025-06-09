// hooks/useReservationWebSocket.ts
// 웹소켓 연결 및 실시간 데이터 처리
import { useState, useEffect, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import axiosInstance from '@/lib/api/instance';
import useUserStore from '@/lib/store/useUserStore';
import { fetchOwnerReservations } from '@/lib/api/reservations/request';
import SockJS from 'sockjs-client';

interface ReservationData {
  reservationId: number;
  postTitle: string;
  postProductImage: string;
  startDate: string;
  endDate: string;
  rentalFee: number;
  deposit: number;
  status: string;
  ownerName: string;
  ownerProfileImageUrl: string;
  renterName: string;
  renterProfileImageUrl: string;
  renterId: number;
}

export default function useReservationWebSocket() {
  const [pendingReservations, setPendingReservations] = useState<ReservationData[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const { userId } = useUserStore();

  // 대기 중인 예약 가져오기 
  const fetchPendingReservations = useCallback(async () => {
    if (!userId) {
      setLoading(false);
      return;
    }

    setLoading(true);
    try {
      const response = await fetchOwnerReservations(userId);

      // API 응답 데이터 형식 변환
      const reservations = Array.isArray(response) ? response : response.content || [];
      const formattedReservations = reservations.map((reservation: any) => {
        if (!reservation.reservationId) {
          console.warn('예약 ID가 없는 데이터 발견:', reservation);
          return null;
        }
        return {
          reservationId: reservation.reservationId,
          postTitle: reservation.title || reservation.postTitle,
          postProductImage: reservation.productImageUrl || reservation.postProductImage,
          postProductImages: reservation.productImageUrl || reservation.postProductImage,
          startDate: reservation.startDate,
          endDate: reservation.endDate,
          rentalFee: reservation.rentalFee,
          deposit: reservation.deposit,
          status: reservation.reservationStatus || reservation.status,
          ownerName: reservation.ownerName || '소유자',
          ownerProfileImageUrl: reservation.ownerProfileImageUrl || '/images/profileimg.png',
          renterName: reservation.renterName || '렌터',
          renterProfileImageUrl: reservation.renterProfileImageUrl || '/images/profileimg.png'
        };
      }).filter(Boolean); // null 값 제거

      setPendingReservations(formattedReservations);
      setError(null);
    } catch (err) {
      console.error('예약 목록 요청 실패:', err);
      setError('예약 목록을 불러오는 데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, [userId]);

  // 웹소켓 연결
  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    const uuid = localStorage.getItem('uuid');

    if (!token || !uuid) {
      setError('인증 정보가 없습니다.');
      setLoading(false);
      return;
    }

    let client: Client | null = null;

    try {
      const baseUrl = process.env.NEXT_PUBLIC_API_URL;
      const socket = new SockJS(`${baseUrl}/ws-rental`);

      // STOMP 클라이언트 생성
      client = new Client({
        webSocketFactory: () => socket,
        debug: (str) => {
          // console.log('STOMP Reservation: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
        connectHeaders: {
          Authorization: `Bearer ${token}`
        }
      });

      client.onConnect = () => {
        setIsConnected(true);
        console.log('Connected to WebSocket');

        client?.subscribe(`/topic/rental-reservation/${uuid}/status`, (message) => {
          try {
            const reservationData = JSON.parse(message.body);
            console.log('Received reservation data:', reservationData);

            // 웹소켓으로 받은 데이터 형식 변환
            const formattedReservation = {
              reservationId: reservationData.reservationId,
              postTitle: reservationData.postTitle || reservationData.title,
              postProductImage: reservationData.postProductImage || reservationData.productImageUrl,
              postProductImages: reservationData.productImageUrl || reservationData.postProductImage,
              startDate: reservationData.startDate,
              endDate: reservationData.endDate,
              rentalFee: reservationData.rentalFee,
              deposit: reservationData.deposit,
              status: reservationData.status || reservationData.reservationStatus,
              ownerName: reservationData.ownerName || '소유자',
              ownerProfileImageUrl: reservationData.ownerProfileImageUrl || '/images/profileimg.png',
              renterName: reservationData.renterName || '렌터',
              renterProfileImageUrl: reservationData.renterProfileImageUrl || '/images/profileimg.png',
              renterId: reservationData.renterId || 0
            };

            handleReservationUpdate(formattedReservation);
          } catch (e) {
            console.error('메시지 처리 오류:', e);
          }
        });

        // 초기 데이터 로드
        fetchPendingReservations();
      };

      client.onStompError = (frame) => {
        console.error('STOMP Error:', frame.headers['message']);
        console.error('Additional details:', frame.body);
        setError('서버 연결에 문제가 발생했습니다.');
        setIsConnected(false);
      };

      client.onWebSocketClose = () => {
        setIsConnected(false);
      };

      client.activate();
    } catch (e) {
      console.error('웹소켓 연결 오류:', e);
      setError('웹소켓 연결에 실패했습니다.');
    }

    return () => {
      if (client && client.connected) {
        try {
          client.deactivate();
        } catch (e) {
          console.error('웹소켓 종료 오류:', e);
        }
      }
    };
  }, [fetchPendingReservations]);

  // 예약 상태 업데이트 처리
  const handleReservationUpdate = (updatedReservation: ReservationData) => {
    console.log('Handling reservation update:', updatedReservation);
    
    setPendingReservations(prev => {
      // 현재 예약이 PENDING 상태인 경우에만 처리
      if (updatedReservation.status === 'PENDING') {
        // 이미 존재하는지 확인
        const exists = prev.some(res => res.reservationId === updatedReservation.reservationId);
        if (!exists) {
          console.log('Adding new pending reservation:', updatedReservation);
          return [...prev, updatedReservation];
        }
      } else {
        // PENDING이 아닌 경우 제거
        console.log('Removing non-pending reservation:', updatedReservation);
        return prev.filter(res => res.reservationId !== updatedReservation.reservationId);
      }
      return prev;
    });
  };

  // 예약 확정 
  const confirmReservation = async (reservationId: number) => {
    if (!userId) return false;

    try {
      await axiosInstance.patch(
        `/api/v1/reservations/${reservationId}/status`,
        {
          status: 'CONFIRMED'
        },
        {
          headers: {
            'Content-Type': 'application/json',
            'X-User-Id': userId
          }
        }
      );

      // 성공하면 목록에서 해당 예약 제거
      setPendingReservations(prev =>
        prev.filter(res => res.reservationId !== reservationId)
      );

      return true;
    } catch (error) {
      console.error('예약 확정 실패:', error);
      return false;
    }
  };

  // 예약 취소 
  const cancelReservation = async (reservationId: number) => {
    if (!userId) return false;

    try {
      await axiosInstance.delete(
        `/api/v1/reservations/${reservationId}`,
        {
          headers: {
            'Content-Type': 'application/json',
            'X-User-Id': userId
          }
        }
      );

      // 성공하면 목록에서 해당 예약 제거
      setPendingReservations(prev =>
        prev.filter(res => res.reservationId !== reservationId)
      );

      return true;
    } catch (error) {
      console.error('예약 취소 실패:', error);
      return false;
    }
  };

  return {
    pendingReservations,
    isConnected,
    loading,
    error,
    confirmReservation,
    cancelReservation,
    refreshReservations: fetchPendingReservations
  };
}