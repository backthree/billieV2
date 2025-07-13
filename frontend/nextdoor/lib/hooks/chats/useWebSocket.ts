import { useEffect, useRef, useState, useCallback } from 'react';
import { ChatMessageDto, Message } from '@/types/chats/chat';
import useUserStore from '@/lib/store/useUserStore';
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

interface UseWebSocketProps {
  roomId: number;
  onMessage?: (message: ChatMessageDto) => void;
}

/**
 * WebSocket 연결 훅
 */
export const useWebSocket = ({ roomId, onMessage }: UseWebSocketProps) => {
  const userId = useUserStore(state => state.userId);
  const [isConnected, setIsConnected] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const stompClientRef = useRef<Client | null>(null);

  const WS_BASE_URL = process.env.NEXT_PUBLIC_WS_URL;

  // 토큰 가져오기
  const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;

  const connect = useCallback(() => {
    if (!userId || !roomId) {
      console.warn('WebSocket 연결에 필요한 userId 또는 roomId가 없습니다.');
      return;
    }

    // 이미 연결되어 있으면 재연결하지 않음
    if (stompClientRef.current && stompClientRef.current.connected) {
      return;
    }

    // 기존 연결 해제
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }

    const stompClient = new Client({
      webSocketFactory: () => new SockJS(`${WS_BASE_URL}/ws-chat`),
      connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
      debug: (str) => console.log('STOMP Debug:', str),
      // ← 0이 아니라 5초 정도 여유를 두세요
      reconnectDelay: 5000,
      // 서버쪽 heartbeat 값(10s)과 맞추거나 default로 두셔도 무방합니다
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => { /* … */ },
      onStompError: (frame) => { /* … */ },
      onWebSocketClose: (evt) => {
        // console.warn('WebSocket 연결 종료:', evt);
        setIsConnected(false);
        // reconnectDelay>0 이면 STOMP.js 가 자동으로 connect()를 호출합니다
      },
    });
    stompClient.activate();

    stompClient.onConnect = () => {
      console.log('STOMP 연결 성공');
      setIsConnected(true);
      setError(null);

      // 구독 설정
      stompClient.subscribe(`/topic/rooms/${roomId}`, (message) => {
        try {
          const data = JSON.parse(message.body);
          console.log('메시지 수신:', data);

          // console.log("onMessage 호출 여부", onMessage);

          if (onMessage) {
            const chatMessage: ChatMessageDto = {
              roomId: data.roomId || roomId,
              senderId: data.senderId || 0,
              content: data.content || '',
              sentAt: data.sentAt || new Date().toISOString()
            };
            const newMessage: Message = {
              id: Number(`${chatMessage.roomId}${chatMessage.senderId}${new Date(chatMessage.sentAt).getTime()}`),
              text: chatMessage.content,
              sender: Number(chatMessage.senderId) === Number(userId) ? "user" : "other",
              timestamp: new Date(chatMessage.sentAt),
              read: false,
            };
            onMessage(chatMessage);
          }
        } catch (err) {
          console.error('메시지 파싱 오류:', err, '원본 데이터:', message.body);
        }
      });
    };

    stompClient.onStompError = (frame) => {
      console.error('STOMP 오류:', frame);
      setError('연결에 실패했습니다. 다시 시도 중...');
      setIsConnected(false);
    };

    stompClient.onWebSocketClose = () => {
      console.log('WebSocket 연결 종료');
      setTimeout(() => connect(), 3000);
      setIsConnected(false);
    };

    // stompClient.activate();
    stompClientRef.current = stompClient;
  }, [userId, roomId, onMessage, WS_BASE_URL, token]);

  const disconnect = useCallback(() => {
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }
  }, []);

  const sendMessage = useCallback((content: string) => {

    if (!userId) {
      setError('사용자 정보가 없습니다');
      return false;
    }
    if (!stompClientRef.current || !stompClientRef.current.connected) {
      setError('연결이 활성화되지 않았습니다');
      return false;
    }
    try {
      console.log(`try 호출, ${userId}, ${roomId}, ${content}`);
      const message = {
        roomId,
        senderId: userId,
        content,
        sentAt: new Date().toISOString(),
      };
      console.log("connected 여부", stompClientRef.current.connected);
      stompClientRef.current.publish({
        destination: '/app/chat.send',
        body: JSON.stringify(message)
      });
      return true;
    } catch (err) {
      console.log("catch 호출", err);
      setError('메시지를 전송할 수 없습니다');
      return false;
    }
  }, [roomId, userId]);

  useEffect(() => {
    if (userId && roomId) {
      connect();

      const handleOnline = () => {
        console.log('네트워크 연결됨. WebSocket 재연결 시도');
        connect();
      };

      window.addEventListener('online', handleOnline);

      return () => {
        disconnect();
        window.removeEventListener('online', handleOnline);
      };
    }
  }, [userId, roomId]);

  return {
    isConnected,
    error,
    sendMessage,
    connect,
    disconnect,
  };
};
