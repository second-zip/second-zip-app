import { flushPromises, mount } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { useAudioAnalyser } from './useAudioAnalyser';

const mountAnalyser = () =>
  mount({
    setup: () => useAudioAnalyser(),
    template: '<div />',
  });

describe('useAudioAnalyser', () => {
  let analyser;
  let context;
  let source;

  beforeEach(() => {
    analyser = {
      fftSize: 0,
      getByteTimeDomainData: vi.fn((samples) => samples.fill(255)),
      smoothingTimeConstant: 0,
    };
    source = { connect: vi.fn(), disconnect: vi.fn() };
    context = {
      close: vi.fn().mockResolvedValue(undefined),
      createAnalyser: vi.fn(() => analyser),
      createMediaStreamSource: vi.fn(() => source),
      state: 'running',
    };
    vi.stubGlobal('AudioContext', vi.fn(() => context));
    vi.spyOn(window, 'requestAnimationFrame').mockReturnValue(17);
    vi.spyOn(window, 'cancelAnimationFrame').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
  });

  it('오디오 스트림의 볼륨을 분석해 파형 레벨을 갱신한다', () => {
    const stream = { id: 'stream' };
    const wrapper = mountAnalyser();

    wrapper.vm.startAnalysis(stream);

    expect(context.createMediaStreamSource).toHaveBeenCalledWith(stream);
    expect(analyser.fftSize).toBe(256);
    expect(analyser.smoothingTimeConstant).toBe(0.75);
    expect(source.connect).toHaveBeenCalledWith(analyser);
    expect(wrapper.vm.waveformLevels.at(-1)).toBe(1);
    expect(window.requestAnimationFrame).toHaveBeenCalled();
  });

  it('분석을 정지하면 자원과 파형을 초기화한다', async () => {
    const wrapper = mountAnalyser();
    wrapper.vm.startAnalysis({});

    wrapper.vm.stopAnalysis();
    await flushPromises();

    expect(window.cancelAnimationFrame).toHaveBeenCalledWith(17);
    expect(source.disconnect).toHaveBeenCalledOnce();
    expect(context.close).toHaveBeenCalledOnce();
    expect(wrapper.vm.waveformLevels).toEqual(Array(8).fill(0.08));
  });

  it('AudioContext를 지원하지 않으면 안전하게 종료한다', () => {
    vi.stubGlobal('AudioContext', undefined);
    vi.stubGlobal('webkitAudioContext', undefined);
    const wrapper = mountAnalyser();

    expect(() => wrapper.vm.startAnalysis({})).not.toThrow();
    expect(context.createAnalyser).not.toHaveBeenCalled();
  });

  it('컴포넌트 해제 시 분석을 정리하고 이미 닫힌 context는 다시 닫지 않는다', () => {
    context.state = 'closed';
    const wrapper = mountAnalyser();
    wrapper.vm.startAnalysis({});

    wrapper.unmount();

    expect(source.disconnect).toHaveBeenCalledOnce();
    expect(context.close).not.toHaveBeenCalled();
  });
});
