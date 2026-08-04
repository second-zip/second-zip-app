import { describe, expect, it } from 'vitest';

import { normalizeFraudTypes } from './fraud';
import {
  normalizeDictionaryCharacter,
  resolveDictionaryCharacter,
} from './characters';
import { findLastContentRow, normalizeGuideConfig } from './guides';
import { normalizeDictionaryCards } from './main';
import { normalizeWordItems } from './words';

describe('dictionary data normalization', () => {
  it('resolves authenticated character settings and defaults guests to cat', () => {
    expect(resolveDictionaryCharacter(true, 'MAN')).toBe('man');
    expect(resolveDictionaryCharacter(true, 'woman')).toBe('woman');
    expect(resolveDictionaryCharacter(false, 'WOMAN')).toBe('cat');
    expect(normalizeDictionaryCharacter('unknown')).toBe('cat');
  });

  it('normalizes menu cards and falls back to a supported tone', () => {
    const [card] = normalizeDictionaryCards([
      {
        id: 1,
        title: '용어 정리',
        tone: 'unsupported',
        routeName: 'dictionary-words',
      },
    ]);

    expect(card).toMatchObject({
      id: '1',
      title: '용어 정리',
      tone: 'blue',
      routeName: 'dictionary-words',
    });
  });

  it('normalizes word and fraud entries without mutating source data', () => {
    const wordSource = [['전세', ' 목돈을 맡기는 방식. ']];
    const fraudSource = [
      {
        id: 'gap',
        number: 1,
        title: '깡통전세형',
        hashtags: ['#깡통전세', ' 전세가율확인 '],
      },
    ];

    expect(normalizeWordItems(wordSource)[0]).toMatchObject({
      term: '전세',
      description: '목돈을 맡기는 방식.',
    });
    expect(normalizeFraudTypes(fraudSource)[0]).toMatchObject({
      id: 'gap',
      number: 1,
      title: '깡통전세형',
      hashtags: ['깡통전세', '전세가율확인'],
      videoSrc: '',
    });
    expect(wordSource[0][1]).toBe(' 목돈을 맡기는 방식. ');
  });

  it('normalizes guide tabs and image metadata', () => {
    const config = normalizeGuideConfig({
      tabs: [{ id: 'registry', images: ['/registry.png'] }],
    });

    expect(config.tabs[0].images[0]).toEqual({
      id: 'registry-image-1',
      src: '/registry.png',
      alt: '',
    });
  });
});

describe('comic image trimming', () => {
  it('returns the last row containing enough non-white pixels', () => {
    const width = 4;
    const height = 4;
    const pixels = new Uint8ClampedArray(width * height * 4).fill(255);

    for (let x = 0; x < width; x += 1) {
      const offset = (2 * width + x) * 4;
      pixels[offset] = 0;
      pixels[offset + 1] = 0;
      pixels[offset + 2] = 0;
    }

    expect(findLastContentRow(pixels, width, height)).toBe(2);
  });

  it('keeps the full height for an entirely white image', () => {
    const width = 4;
    const height = 4;
    const pixels = new Uint8ClampedArray(width * height * 4).fill(255);

    expect(findLastContentRow(pixels, width, height)).toBe(height - 1);
  });
});
