import { Duel } from '../types/duel';

type DuelCallback = (duel: Duel) => void;

class WebSocketService {
  private socket: WebSocket | null = null;
  private subscriptions: Map<string, Set<DuelCallback>> = new Map();
  private isConnected = false;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private heartbeatInterval: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.init();
  }

  private getWebSocketUrl(): string {
    const apiBase = import.meta.env.VITE_API_URL || '';
    if (apiBase.startsWith('http://')) {
      return apiBase.replace('http://', 'ws://') + '/ws-duel-native';
    } else if (apiBase.startsWith('https://')) {
      return apiBase.replace('https://', 'wss://') + '/ws-duel-native';
    }

    // Default to relative port or local backend port 8081 in dev
    const isHttps = window.location.protocol === 'https:';
    const protocol = isHttps ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    // In dev, backend runs on 8081
    const port = window.location.port === '5173' ? '8081' : window.location.port;
    return `${protocol}//${host}${port ? `:${port}` : ''}/ws-duel-native`;
  }

  private init() {
    if (typeof window === 'undefined' || !('WebSocket' in window)) return;

    try {
      const url = this.getWebSocketUrl();
      this.socket = new WebSocket(url);

      this.socket.onopen = () => {
        this.isConnected = true;
        // Send STOMP CONNECT frame
        this.sendFrame('CONNECT', { 'accept-version': '1.1,1.2', host: 'localhost', 'heart-beat': '10000,10000' });

        // Resubscribe to all active topics
        this.subscriptions.forEach((_, topic) => {
          this.sendSubscribeFrame(topic);
        });

        // Start client heartbeat
        this.startHeartbeat();
      };

      this.socket.onmessage = (event) => {
        this.handleMessage(event.data);
      };

      this.socket.onclose = () => {
        this.isConnected = false;
        this.stopHeartbeat();
        this.scheduleReconnect();
      };

      this.socket.onerror = () => {
        this.isConnected = false;
      };
    } catch {
      this.scheduleReconnect();
    }
  }

  private startHeartbeat() {
    this.stopHeartbeat();
    this.heartbeatInterval = setInterval(() => {
      if (this.socket && this.isConnected && this.socket.readyState === WebSocket.OPEN) {
        this.socket.send('\n');
      }
    }, 15000);
  }

  private stopHeartbeat() {
    if (this.heartbeatInterval) {
      clearInterval(this.heartbeatInterval);
      this.heartbeatInterval = null;
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) return;
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      this.init();
    }, 4000);
  }

  private sendFrame(command: string, headers: Record<string, string>, body = '') {
    if (!this.socket || this.socket.readyState !== WebSocket.OPEN) return;

    let frame = `${command}\n`;
    for (const [key, value] of Object.entries(headers)) {
      frame += `${key}:${value}\n`;
    }
    frame += `\n${body}\0`;
    this.socket.send(frame);
  }

  private sendSubscribeFrame(topic: string) {
    this.sendFrame('SUBSCRIBE', {
      id: `sub-${topic}`,
      destination: topic,
    });
  }

  private handleMessage(data: string) {
    if (!data || data === '\n') return; // Heartbeat pong

    // Basic STOMP frame parser
    if (data.startsWith('MESSAGE')) {
      const parts = data.split('\n\n');
      if (parts.length >= 2) {
        const headerLines = parts[0].split('\n');
        const bodyWithNull = parts.slice(1).join('\n\n');
        const rawBody = bodyWithNull.replace(/\0$/, '');

        let destination = '';
        for (const line of headerLines) {
          if (line.startsWith('destination:')) {
            destination = line.substring('destination:'.length).trim();
            break;
          }
        }

        if (destination && rawBody) {
          try {
            const parsed = JSON.parse(rawBody);
            const callbacks = this.subscriptions.get(destination);
            if (callbacks) {
              callbacks.forEach((cb) => {
                try {
                  cb(parsed);
                } catch (e) {
                  console.error('Error in STOMP message handler:', e);
                }
              });
            }
          } catch {
            // Non-JSON payload
          }
        }
      }
    }
  }

  /**
   * Subscribes to real-time duel status changes for a specific room.
   * Returns an unsubscribe callback function.
   */
  public subscribeToDuel(roomCode: string, onUpdate: DuelCallback): () => void {
    const topic = `/topic/duel/${roomCode.toUpperCase()}`;

    if (!this.subscriptions.has(topic)) {
      this.subscriptions.set(topic, new Set());
      if (this.isConnected) {
        this.sendSubscribeFrame(topic);
      }
    }

    this.subscriptions.get(topic)!.add(onUpdate);

    // Return cleanup unsubscribe function
    return () => {
      const set = this.subscriptions.get(topic);
      if (set) {
        set.delete(onUpdate);
        if (set.size === 0) {
          this.subscriptions.delete(topic);
          if (this.isConnected) {
            this.sendFrame('UNSUBSCRIBE', { id: `sub-${topic}` });
          }
        }
      }
    };
  }
}

export const websocketService = new WebSocketService();
