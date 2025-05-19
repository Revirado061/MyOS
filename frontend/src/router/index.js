import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/process'
  },
  {
    path: '/process',
    name: 'Process',
    component: () => import('../views/Process.vue')
  },
  {
    path: '/memory',
    name: 'Memory',
    component: () => import('../views/Memory.vue')
  },
  {
    path: '/filesystem',
    name: 'FileSystem',
    component: () => import('../views/FileSystem.vue')
  },
  {
    path: '/device',
    name: 'Device',
    component: () => import('../views/Device.vue')
  },
  {
    path: '/interrupt',
    name: 'Interrupt',
    component: () => import('../views/Interrupt.vue')
  }
]

const router = new VueRouter({
  routes
})

export default router 