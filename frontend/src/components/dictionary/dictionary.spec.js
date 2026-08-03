import { flushPromises, mount } from '@vue/test-utils';
import { describe, expect, it } from 'vitest';
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

  it('keeps a 9:16 placeholder until a video source is provided', () => {
    const wrapper = mount(BFraudVideoPlayer, {
      props: { fraudType },
    });

    expect(wrapper.find('video').exists()).toBe(false);
    expect(wrapper.get('.video-frame__empty').text()).toContain('영상 준비 중');
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
    expect(wrapper.get('p').text()).toBe('160%');
    expect(wrapper.text()).not.toContain('확대 안내 문구');
  });
});
