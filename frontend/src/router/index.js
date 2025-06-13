import Vue from 'vue'
import VueRouter from 'vue-router'
import IdentitySelection from '../components/IdentitySelection.vue'
import AuthForm from '../components/AuthForm.vue'
import CustomerDashboard from '../views/CustomerDashboard.vue'
import TechnicianDashboard from '../views/TechnicianDashboard.vue'
import AdminDashboard from '../views/AdminDashboard.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'IdentitySelection',
    component: IdentitySelection
  },
  {
    path: '/auth/:role',
    name: 'Auth',
    component: AuthForm,
    props: true
  },
  {
    path: '/customer',
    name: 'CustomerDashboard',
    component: CustomerDashboard,
    meta: { requiresAuth: true, role: 'customer' }
  },
  {
    path: '/technician',
    name: 'TechnicianDashboard',
    component: TechnicianDashboard,
    meta: { requiresAuth: true, role: 'technician' }
  },
  {
    path: '/admin',
    name: 'AdminDashboard',
    component: AdminDashboard,
    meta: { requiresAuth: true, role: 'admin' }
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  const requiredRole = to.meta.role;
  const user = JSON.parse(localStorage.getItem('user') || 'null');
  const userRole = localStorage.getItem('userRole');

  if (requiresAuth) {
    if (!user || !userRole) {
      // 用户未登录，重定向到身份选择页面
      next('/');
    } else if (requiredRole && userRole !== requiredRole) {
      // 用户角色不匹配，重定向到对应的dashboard
      const dashboardMap = {
        'customer': '/customer',
        'technician': '/technician',
        'admin': '/admin'
      };
      next(dashboardMap[userRole] || '/');
    } else {
      // 用户已登录且角色匹配
      next();
    }
  } else {
    // 不需要认证的页面
    if (user && userRole && (to.path === '/' || to.path.startsWith('/auth'))) {
      // 已登录用户访问登录页面，重定向到对应dashboard
      const dashboardMap = {
        'customer': '/customer',
        'technician': '/technician',
        'admin': '/admin'
      };
      next(dashboardMap[userRole] || '/');
    } else {
      next();
    }
  }
});

export default router
