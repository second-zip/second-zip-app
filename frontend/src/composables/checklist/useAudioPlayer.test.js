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

  test('재생 중이면 오디오를 일시정지한다', async () => {
    const { player } = setup();
    const audio = { pause: vi.fn(), play: vi.fn() };
    player.audioElement.value = audio;
    player.isPlaying.value = true;

    await player.togglePlayback();

    expect(audio.pause).toHaveBeenCalledOnce();
    expect(audio.play).not.toHaveBeenCalled();
  });

  test('재생 요소가 없거나 재생이 실패해도 안전하게 처리한다', async () => {
    const { player } = setup();

    await expect(player.togglePlayback()).resolves.toBeUndefined();

    player.audioElement.value = {
      pause: vi.fn(),
      play: vi.fn().mockRejectedValue(new Error('play failed')),
    };
    player.isPlaying.value = true;
    await player.togglePlayback();
    player.isPlaying.value = false;
    await player.togglePlayback();

    expect(player.isPlaying.value).toBe(false);
  });

  test('메타데이터와 재생 시간을 반영하고 종료 시 초기화한다', () => {
    const { player } = setup();
    player.audioElement.value = {
      currentTime: 30,
      duration: 120,
      pause: vi.fn(),
    };

    player.updateMetadata();
    player.updateCurrentTime();
    expect(player.duration.value).toBe(120);
    expect(player.currentTime.value).toBe(30);
    expect(player.progress.value).toBe(25);

    player.handleEnded();
    expect(player.currentTime.value).toBe(0);
    expect(player.isPlaying.value).toBe(false);
  });

  test('유효하지 않은 오디오 길이는 fallback을 사용하고 해제 시 재생을 정지한다', () => {
    const { player, wrapper } = setup();
    const pause = vi.fn();
    player.audioElement.value = { duration: Number.NaN, pause };

    player.updateMetadata();
    expect(player.duration.value).toBe(0);
    expect(player.progress.value).toBe(0);

    wrapper.unmount();
    expect(pause).toHaveBeenCalledOnce();
  });
});
