import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it, vi } from 'vitest';
import { createMemoryHistory, createRouter } from 'vue-router';

import FraudVideoView from '@/views/dictionary/FraudVideoView.vue';
import BGuideTabs from './guides/B_GuideTabs.vue';
import CComicScroller from './guides/C_ComicScroller.vue';
import FGuideCharacter from './guides/F_GuideCharacter.vue';
import CFraudTypeCard from './fraud/C_FraudTypeCard.vue';
import BFraudVideoPlayer from './fraud/B_FraudVideoPlayer.vue';

const fraudType = {
  id: 'trust-property',
  number: 3,
  title: '신탁 부동산 사기형',
  hashtags: ['신탁사기', '신탁회사동의여부'],
  description: '신탁회사의 동의 여부를 확인해야 합니다.',
  videoSrc: '',
};

describe('fraud dictionary components', () => {
  it('renders related hashtags before the fraud description', () => {
    const wrapper = mount(CFraudTypeCard, {
      props: { type: fraudType },
    });

    const hashtags = wrapper.findAll('.fraud-card__hashtags li');
    const hashtagList = wrapper.find('.fraud-card__hashtags');
    const description = wrapper.find('.fraud-card__body p');

    expect(hashtags.map((item) => item.text())).toEqual([
      '#신탁사기',
      '#신탁회사동의여부',
    ]);
    expect(hashtagList.element.compareDocumentPosition(description.element)).toBe(
      Node.DOCUMENT_POSITION_FOLLOWING,
    );
  });

  it('emits the selected fraud type when the short-form preview is clicked', async () => {
    const wrapper = mount(CFraudTypeCard, {
      props: { type: fraudType },
    });

    await wrapper.get('button').trigger('click');

    expect(wrapper.emitted('play')).toEqual([[fraudType.id]]);
  });

  it('shows the YouTube thumbnail on the short-form preview', () => {
    const wrapper = mount(CFraudTypeCard, {
      props: {
        type: {
          ...fraudType,
          thumbnailSrc:
            'https://i.ytimg.com/vi/99bV3YRu8pI/maxresdefault.jpg',
          thumbnailFallbackSrc:
            'https://i.ytimg.com/vi/99bV3YRu8pI/hqdefault.jpg',
        },
      },
    });

    expect(wrapper.get('button img').attributes('src')).toContain(
      '99bV3YRu8pI/maxresdefault.jpg',
    );
  });

  it('keeps a 9:16 placeholder until a video source is provided', () => {
    const wrapper = mount(BFraudVideoPlayer, {
      props: { fraudType },
    });

    expect(wrapper.find('video').exists()).toBe(false);
    expect(wrapper.get('.video-frame__empty').text()).toContain('영상 준비 중');
  });

  it('embeds a YouTube short inside the video player', () => {
    const wrapper = mount(BFraudVideoPlayer, {
      props: {
        fraudType: {
          ...fraudType,
          videoSrc: 'https://www.youtube-nocookie.com/embed/99bV3YRu8pI',
        },
      },
    });

    expect(wrapper.get('iframe').attributes('src')).toBe(
      'https://www.youtube-nocookie.com/embed/99bV3YRu8pI',
    );
    expect(wrapper.find('video').exists()).toBe(false);
    expect(wrapper.find('.video-frame__empty').exists()).toBe(false);
  });

  it('returns to the fraud type list from the video page', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/dictionary/fraud',
          component: { template: '<div>유형 목록</div>' },
        },
        {
          path: '/dictionary/fraud/:typeId',
          component: FraudVideoView,
        },
      ],
    });
    await router.push('/dictionary/fraud/trust-property');
    await router.isReady();

    const wrapper = mount(FraudVideoView, {
      global: { plugins: [router] },
    });

    await wrapper.get('.video-page__back').trigger('click');
    await flushPromises();

    expect(router.currentRoute.value.path).toBe('/dictionary/fraud');
  });
});

describe('comic guide components', () => {
  it('updates the active guide tab', async () => {
    const tabs = [
      { id: 'registry', label: '등기부등본' },
      { id: 'building', label: '건축물대장' },
    ];
    const wrapper = mount(BGuideTabs, {
      props: {
        tabs,
        modelValue: tabs[0].id,
      },
    });

    await wrapper.findAll('button')[1].trigger('click');

    expect(wrapper.emitted('update:modelValue')).toEqual([[tabs[1].id]]);
  });

  it('emits zoom changes only for Ctrl and mouse-wheel input', async () => {
    const wrapper = mount(CComicScroller, {
      props: {
        tab: { id: 'registry', images: [] },
      },
    });
    const scroller = wrapper.get('.comic-scroller');

    scroller.element.dispatchEvent(
      new WheelEvent('wheel', {
        bubbles: true,
        cancelable: true,
        ctrlKey: false,
        deltaY: -1,
        clientX: 10,
        clientY: 10,
      }),
    );
    scroller.element.dispatchEvent(
      new WheelEvent('wheel', {
        bubbles: true,
        cancelable: true,
        ctrlKey: true,
        deltaY: -1,
        clientX: 10,
        clientY: 10,
      }),
    );
    await wrapper.vm.$nextTick();

    expect(wrapper.emitted('zoom-change')).toEqual([[1], [1.1]]);
  });

  it('clamps comic zoom between the configured minimum and maximum', async () => {
    const wrapper = mount(CComicScroller, {
      props: {
        tab: {
          id: 'registry',
          images: [{ id: 'page-1', src: '/comic.png', alt: 'comic' }],
        },
      },
    });
    const scroller = wrapper.get('.comic-scroller');
    const zoom = (deltaY) =>
      scroller.element.dispatchEvent(
        new WheelEvent('wheel', {
          bubbles: true,
          cancelable: true,
          ctrlKey: true,
          deltaY,
          clientX: 0,
          clientY: 0,
        }),
      );

    for (let index = 0; index < 30; index += 1) zoom(-1);
    await wrapper.vm.$nextTick();
    expect(wrapper.get('.comic-scroller__strip').attributes('style')).toContain(
      'width: 250%',
    );

    for (let index = 0; index < 30; index += 1) zoom(1);
    await wrapper.vm.$nextTick();
    expect(wrapper.get('.comic-scroller__strip').attributes('style')).toContain(
      'width: 100%',
    );
  });

  it('moves the comic viewport by pointer drag only while zoomed', async () => {
    const wrapper = mount(CComicScroller, {
      props: { tab: { id: 'registry', images: [] } },
    });
    const element = wrapper.get('.comic-scroller').element;
    element.setPointerCapture = vi.fn();
    element.hasPointerCapture = vi.fn(() => true);
    element.releasePointerCapture = vi.fn();
    const pointerEvent = (type, clientX, clientY) => {
      const event = new Event(type, { bubbles: true });
      Object.defineProperties(event, {
        button: { value: 0 },
        clientX: { value: clientX },
        clientY: { value: clientY },
        pointerId: { value: 1 },
      });
      return event;
    };

    element.dispatchEvent(pointerEvent('pointerdown', 50, 60));
    element.dispatchEvent(pointerEvent('pointermove', 30, 20));
    expect(element.scrollLeft).toBe(0);
    expect(element.scrollTop).toBe(0);

    element.dispatchEvent(
      new WheelEvent('wheel', {
        bubbles: true,
        cancelable: true,
        ctrlKey: true,
        deltaY: -1,
        clientX: 0,
        clientY: 0,
      }),
    );
    await wrapper.vm.$nextTick();
    element.dispatchEvent(pointerEvent('pointerdown', 50, 60));
    element.dispatchEvent(pointerEvent('pointermove', 30, 20));
    await wrapper.vm.$nextTick();

    expect(element.scrollLeft).toBe(20);
    expect(element.scrollTop).toBe(40);
    expect(wrapper.classes()).toContain('is-dragging');

    element.dispatchEvent(pointerEvent('pointerup', 30, 20));
    await wrapper.vm.$nextTick();
    expect(element.releasePointerCapture).toHaveBeenCalledWith(1);
    expect(wrapper.classes()).not.toContain('is-dragging');
  });

  it('collapses the assistant message to the current zoom percentage', () => {
    const wrapper = mount(FGuideCharacter, {
      props: {
        image: '/cat.png',
        message: '확대 안내 문구',
        compact: true,
        zoom: 1.6,
      },
    });

    expect(wrapper.classes()).toContain('guide-character--compact');
    expect(wrapper.get('.guide-character__message').text()).toBe('160%');
    expect(wrapper.text()).not.toContain('확대 안내 문구');
  });

  it('hides the guide bubble and restores it from the compact avatar', async () => {
    const wrapper = mount(FGuideCharacter, {
      props: {
        image: '/man.png',
        message: 'Ctrl과 드래그로 자세히 볼 수 있어!',
      },
    });

    await wrapper.get('.guide-character__message').trigger('click');
    expect(wrapper.emitted('toggle')).toHaveLength(1);

    await wrapper.setProps({ dismissed: true });
    expect(wrapper.find('.guide-character__message').exists()).toBe(false);
    expect(wrapper.classes()).toContain('guide-character--compact');

    await wrapper.get('.guide-character__avatar').trigger('click');
    expect(wrapper.emitted('toggle')).toHaveLength(2);
  });

  it('also hides the guide bubble when the visible avatar is clicked', async () => {
    const wrapper = mount(FGuideCharacter, {
      props: {
        image: '/man.png',
        message: 'Ctrl과 드래그로 자세히 볼 수 있어!',
      },
    });

    await wrapper.get('.guide-character__avatar').trigger('click');

    expect(wrapper.emitted('toggle')).toHaveLength(1);
  });
});
