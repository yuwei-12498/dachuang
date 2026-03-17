<template>
  <div class="landing-page">
    <HomeHero @scrollTo="handleScroll" />
    <HomeCoreSection />
    <HomeScenarioCards />
    <HomeFeatureSection />
    <HomeRouteExamples />
    <HomeFooterCTA @scrollTo="handleScroll" />
  </div>
</template>

<script setup>
import HomeHero from '@/components/home/HomeHero.vue'
import HomeCoreSection from '@/components/home/HomeCoreSection.vue'
import HomeScenarioCards from '@/components/home/HomeScenarioCards.vue'
import HomeFeatureSection from '@/components/home/HomeFeatureSection.vue'
import HomeRouteExamples from '@/components/home/HomeRouteExamples.vue'
import HomeFooterCTA from '@/components/home/HomeFooterCTA.vue'

const handleScroll = (selector) => {
  const el = document.querySelector(selector)
  if (el) {
    const y = el.getBoundingClientRect().top + window.scrollY - 80
    window.scrollTo({ top: y, behavior: 'smooth' })
  }
}
</script>

<style scoped>
.landing-page {
  width: 100%;
  display: flex;
  flex-direction: column;
}
</style>
