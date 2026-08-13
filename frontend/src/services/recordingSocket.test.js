import { beforeEach, describe, expect, test, vi } from 'vitest';

import { createRecordingSocket } from './recordingSocket';

class FakeWebSocket extends EventTarget {
  static OPEN = 1;
  static CLOSING = 2;
  static instances = [];

  constructor(url) {
    super();
    this.url = url;
    this.readyState = FakeWebSocket.OPEN;
    this.bufferedAmount = 0;
    this.send = vi.fn();
    FakeWebSocket.instances.push(this);
  }

  close() {
    this.readyState = FakeWebSocket.CLOSING;
  }
}

const currentSocket = () => FakeWebSocket.instances.at(-1);

describe('recordingSocket', () => {
  beforeEach(() => {
    FakeWebSocket.instances = [];
    vi.stubGlobal('WebSocket', FakeWebSocket);
    vi.stubEnv('VITE_API_BASE_URL', '/api');
  });

  test('API 주소를 WebSocket 주소로 바꾸어 연결한다', async () => {
    const client = createRecordingSocket();
    const connecting = client.connect(17);
    currentSocket().dispatchEvent(new Event('open'));

    await connecting;
    const url = new URL(currentSocket().url);
    expect(url.protocol).toBe('ws:');
    expect(url.pathname).toBe('/ws/recordings/17');
    expect(currentSocket().binaryType).toBe('arraybuffer');
  });

  test('PCM 데이터를 서버 제한인 8000 byte 단위로 나누어 보낸다', async () => {
    const client = createRecordingSocket();
    const connecting = client.connect(1);
    currentSocket().dispatchEvent(new Event('open'));
    await connecting;

    client.send(new ArrayBuffer(17000));

    expect(currentSocket().send).toHaveBeenCalledTimes(3);
    expect(currentSocket().send.mock.calls.map(([chunk]) => chunk.byteLength))
      .toEqual([8000, 8000, 1000]);
  });

  test('예상하지 못한 종료만 사용자 오류로 전달한다', async () => {
    const onError = vi.fn();
    const client = createRecordingSocket(onError);
    let connecting = client.connect(1);
    currentSocket().dispatchEvent(new Event('open'));
    await connecting;
    currentSocket().dispatchEvent(new Event('close'));
    expect(onError).toHaveBeenCalledWith(expect.objectContaining({
      message: '녹음 서버와의 연결이 끊어졌어요.',
    }));

    connecting = client.connect(2);
    currentSocket().dispatchEvent(new Event('open'));
    await connecting;
    client.close();
    currentSocket().dispatchEvent(new Event('close'));
    expect(onError).toHaveBeenCalledTimes(1);
  });
});
