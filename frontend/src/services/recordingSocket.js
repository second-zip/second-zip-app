const makeSocketUrl = (recordingSessionId) => {
  const baseUrl = new URL(
    import.meta.env.VITE_API_BASE_URL,
    window.location.origin,
  );
  baseUrl.protocol = baseUrl.protocol === 'https:' ? 'wss:' : 'ws:';
  baseUrl.pathname = `${baseUrl.pathname.replace(/\/api\/?$/, '')}/ws/recordings/${recordingSessionId}`;
  baseUrl.search = '';
  return baseUrl.toString();
};

const MAX_FRAME_SIZE = 8000;

export const createRecordingSocket = (onError) => {
  let socket;
  const ignoredCloseSockets = new WeakSet();

  const connect = (recordingSessionId) => new Promise((resolve, reject) => {
    socket = new WebSocket(makeSocketUrl(recordingSessionId));
    socket.binaryType = 'arraybuffer';
    socket.addEventListener('open', resolve, { once: true });
    socket.addEventListener('error', (event) => {
      const error = new Error('녹음 서버에 연결하지 못했어요.');
      ignoredCloseSockets.add(event.currentTarget);
      onError?.(error);
      reject(error);
    });
    socket.addEventListener('close', (event) => {
      if (!ignoredCloseSockets.has(event.currentTarget)) {
        onError?.(new Error('녹음 서버와의 연결이 끊어졌어요.'));
      }
    });
  });

  const send = (chunk) => {
    const size = chunk instanceof Blob ? chunk.size : chunk?.byteLength;
    if (socket?.readyState !== WebSocket.OPEN || !size) return;
    for (let offset = 0; offset < size; offset += MAX_FRAME_SIZE) {
      socket.send(chunk.slice(offset, offset + MAX_FRAME_SIZE));
    }
  };

  const waitUntilSent = async () => {
    const startedAt = Date.now();
    while (socket?.bufferedAmount > 0 && Date.now() - startedAt < 3000) {
      await new Promise((resolve) => window.setTimeout(resolve, 30));
    }
  };

  const close = () => {
    if (socket) ignoredCloseSockets.add(socket);
    if (socket?.readyState < WebSocket.CLOSING) socket.close();
    socket = undefined;
  };

  return { close, connect, send, waitUntilSent };
};
