import { afterEach, describe, expect, it, vi } from 'vitest';

import { logger } from './logger';

describe('logger', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('문맥, metadata와 원본 Error 객체를 함께 기록한다', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    const error = new Error('network failure');
    const metadata = { key: 'SIDO' };

    logger.error('region-map.load-metric', error, metadata);

    expect(consoleError).toHaveBeenCalledWith(
      '[region-map.load-metric]',
      metadata,
      error,
    );
  });

  it('metadata를 생략하면 빈 객체를 사용한다', () => {
    const consoleError = vi.spyOn(console, 'error').mockImplementation(() => {});
    const error = new Error('failure');

    logger.error('main.fetch-user', error);

    expect(consoleError).toHaveBeenCalledWith('[main.fetch-user]', {}, error);
  });
});
