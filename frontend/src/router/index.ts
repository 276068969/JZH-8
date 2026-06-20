import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import PortalView from '../views/PortalView.vue'
import LoginView from '../views/LoginView.vue'
import AdminView from '../views/AdminView.vue'
import MerchantWorkbench from '../views/MerchantWorkbench.vue'
import ProductDetailView from '../views/ProductDetailView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: PortalView },
    { path: '/product/:id', component: ProductDetailView },
    { path: '/login', component: LoginView },
    { path: '/admin', component: AdminView, meta: { requiresAuth: true, roles: ['ADMIN', 'REGULATOR'] } },
    { path: '/merchant', component: MerchantWorkbench, meta: { requiresAuth: true, roles: ['MERCHANT'] } }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) {
    return '/login'
  }
  if (to.meta.roles && Array.isArray(to.meta.roles) && auth.role) {
    if (!to.meta.roles.includes(auth.role)) {
      if (auth.role === 'MERCHANT') {
        return '/merchant'
      } else if (auth.role === 'ADMIN' || auth.role === 'REGULATOR') {
        return '/admin'
      } else {
        return '/'
      }
    }
  }
})

export default router
