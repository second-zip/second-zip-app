import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it, vi } from "vitest";
import { createMemoryHistory, createRouter } from "vue-router";

import DictionaryView from "./DictionaryView.vue";

const authStore = vi.hoisted(() => ({
  isAuthenticated: false,
  myPage: null,
  fetchMyPage: vi.fn(),
}));

vi.mock("@/stores/auth", () => ({
  useAuthStore: () => authStore,
}));

const createDictionaryRouter = async () => {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      {
        path: "/dictionary",
        component: DictionaryView,
      },
      {
        path: "/dictionary/words",
        name: "dictionary-words",
        component: { template: "<div>words</div>" },
      },
      {
        path: "/dictionary/fraud",
        name: "dictionary-fraud",
        component: { template: "<div>fraud</div>" },
      },
      {
        path: "/dictionary/register",
        name: "dictionary-register",
        component: { template: "<div>register</div>" },
      },
      {
        path: "/dictionary/move-in",
        name: "dictionary-move-in",
        component: { template: "<div>move-in</div>" },
      },
    ],
  });

  await router.push("/dictionary");
  await router.isReady();

  return router;
};

describe("dictionary character selection", () => {
  it("uses the cat character for guests regardless of profile data", async () => {
    authStore.isAuthenticated = false;
    authStore.myPage = { characterType: "WOMAN" };
    const router = await createDictionaryRouter();

    const wrapper = mount(DictionaryView, {
      global: { plugins: [router] },
    });

    expect(wrapper.get(".menu-card__visual img").attributes("src")).toContain(
      "cat-dict-1",
    );
    expect(wrapper.get(".dictionary-guide p").text()).toContain(
      "막는다냥",
    );

    await wrapper.get(".menu-card").trigger("click");
    await flushPromises();

    expect(router.currentRoute.value.name).toBe("dictionary-words");
  });

  it("uses the saved man character and matching speech style", async () => {
    authStore.isAuthenticated = true;
    authStore.myPage = { characterType: "MAN" };
    const router = await createDictionaryRouter();

    const wrapper = mount(DictionaryView, {
      global: { plugins: [router] },
    });

    expect(wrapper.get(".menu-card__visual img").attributes("src")).toContain(
      "man-dict-1",
    );
    expect(wrapper.get(".dictionary-guide p").text()).toContain(
      "막을 수 있어",
    );
  });

  it("uses the saved woman character and formal speech style", async () => {
    authStore.isAuthenticated = true;
    authStore.myPage = { characterType: "WOMAN" };
    const router = await createDictionaryRouter();

    const wrapper = mount(DictionaryView, {
      global: { plugins: [router] },
    });

    expect(wrapper.get(".menu-card__visual img").attributes("src")).toContain(
      "woman-dict-1",
    );
    expect(wrapper.get(".dictionary-guide p").text()).toContain(
      "예방할 수 있어요",
    );
  });

  it("uses the saved cat character for authenticated users", async () => {
    authStore.isAuthenticated = true;
    authStore.myPage = { characterType: "CAT" };
    const router = await createDictionaryRouter();

    const wrapper = mount(DictionaryView, {
      global: { plugins: [router] },
    });

    expect(wrapper.get(".menu-card__visual img").attributes("src")).toContain(
      "cat-dict-1",
    );
    expect(wrapper.get(".dictionary-guide p").text()).toContain("막는다냥");
  });

  it.each([
    [0, "dictionary-words"],
    [1, "dictionary-fraud"],
    [2, "dictionary-register"],
    [3, "dictionary-move-in"],
  ])("routes card %i to %s", async (cardIndex, routeName) => {
    authStore.isAuthenticated = false;
    authStore.myPage = null;
    const router = await createDictionaryRouter();
    const wrapper = mount(DictionaryView, {
      global: { plugins: [router] },
    });

    await wrapper.findAll(".menu-card")[cardIndex].trigger("click");
    await flushPromises();

    expect(router.currentRoute.value.name).toBe(routeName);
  });
});
