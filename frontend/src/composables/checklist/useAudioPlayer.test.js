import { mount } from '@vue/test-utils';
import { describe, expect, test, vi } from 'vitest';

import { useAudioPlayer } from './useAudioPlayer';

const setup = (beforePlay) => {
  let player;
  const wrapper = mount({
    setup() {
      player = useAudioPlayer({ fallbackDuration: 0, beforePlay });
      return () => null;
    },
  });
  return { player, wrapper };
};

describe('useAudioPlayer', () => {
  test('재생 전에 presigned URL 갱신 작업을 기다린다', async () => {
    const callOrder = [];
    const beforePlay = vi.fn(async () => callOrder.push('refresh'));
    const { player } = setup(beforePlay);
    player.audioElement.value = {
      pause: vi.fn(),
      play: vi.fn(async () => callOrder.push('play')),
    };

    await player.togglePlayback();

    expect(beforePlay).toHaveBeenCalledOnce();
    expect(callOrder).toEqual(['refresh', 'play']);
  });
});
