import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('../views/Home.vue')
    },
    {
      path: '/process',
      name: 'process',
      component: () => import('../views/Process.vue')
    },
    {
      path: '/memory',
      name: 'memory',
      component: () => import('../views/Memory.vue')
    },
    {
      path: '/filesystem',
      name: 'filesystem',
      component: () => import('../views/Filesystem.vue')
    },
    {
      path: '/device',
      name: 'device',
      component: () => import('../views/Device.vue')
    }
  ]
})

export default router 