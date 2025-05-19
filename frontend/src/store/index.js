import Vue from 'vue'
import Vuex from 'vuex'

Vue.use(Vuex)

export default new Vuex.Store({
  state: {
    isRunning: false,
    currentTime: '00:00:00',
    seconds: 0,
    timer: null
  },
  mutations: {
    SET_RUNNING(state, isRunning) {
      state.isRunning = isRunning
    },
    SET_CURRENT_TIME(state, time) {
      state.currentTime = time
    },
    SET_SECONDS(state, seconds) {
      state.seconds = seconds
    },
    SET_TIMER(state, timer) {
      state.timer = timer
    }
  },
  actions: {
    startClock({ commit, state, dispatch }) {
      commit('SET_RUNNING', true)
      const timer = setInterval(() => {
        commit('SET_SECONDS', state.seconds + 1)
        dispatch('updateDisplay')
      }, 1000)
      commit('SET_TIMER', timer)
    },
    pauseClock({ commit, state }) {
      commit('SET_RUNNING', false)
      if (state.timer) {
        clearInterval(state.timer)
        commit('SET_TIMER', null)
      }
    },
    updateDisplay({ commit, state }) {
      const hours = Math.floor(state.seconds / 3600)
      const minutes = Math.floor((state.seconds % 3600) / 60)
      const secs = state.seconds % 60
      const time = `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
      commit('SET_CURRENT_TIME', time)
    }
  },
  getters: {
    isClockRunning: state => state.isRunning,
    currentTime: state => state.currentTime
  },
  modules: {
  }
}) 