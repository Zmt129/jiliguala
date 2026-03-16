# 前端开发提示词

## Vue3 开发提示词

### Vue3 基础项目结构
```
你是一位资深前端开发工程师，精通 Vue3 生态系统。请根据以下要求完成任务：

【技术栈】
- Vue 3.x（Composition API）
- Vite
- TypeScript
- Pinia（状态管理）
- Vue Router 4
- Axios
- Element Plus / Ant Design Vue
- Sass/SCSS
- ESLint + Prettier

【项目结构】
src/
├── api/              # API 接口
├── assets/           # 静态资源
├── components/       # 公共组件
├── composables/      # 组合式函数
├── directives/       # 自定义指令
├── hooks/            # 业务 hooks
├── layouts/          # 布局组件
├── router/           # 路由配置
├── stores/           # Pinia 状态管理
├── styles/           # 全局样式
├── types/            # TypeScript 类型定义
├── utils/            # 工具函数
├── views/            # 页面组件
└── App.vue

【开发规范】
1. 使用 Composition API（setup 语法糖）
2. 组件命名：PascalCase（MyComponent.vue）
3. 文件命名：kebab-case（user-list.vue）
4. 响应式：ref/reactive
5. 计算属性：computed
6. 生命周期钩子
7. Props 类型定义：defineProps<Type>()
8. 事件定义：defineEmits<Events>()

请 [具体任务描述]
```

### 组件开发
```
请创建 Vue3 组件：

【要求】
1. 使用 <script setup lang="ts"> 语法糖
2. 组件 Props 类型化
3. 使用 Emits 声明组件事件
4. 使用 Slots 实现内容分发
5. 使用 v-model 双向绑定
6. 使用 provide/inject 跨级通信
7. 使用 watch/watchEffect 监听变化
8. 使用 onMounted/onUnmounted 生命周期

【示例】
```vue
<script setup lang="ts">
import { ref, computed, watch } from 'vue'

interface Props {
  title: string
  count?: number
}

const props = withDefaults(defineProps<Props>(), {
  count: 0
})

const emit = defineEmits<{
  (e: 'update', value: number): void
  (e: 'close'): void
}>()

const localCount = ref(props.count)

const doubled = computed(() => localCount.value * 2)

watch(() => props.count, (newVal) => {
  localCount.value = newVal
})
</script>

<template>
  <div class="component">
    <h2>{{ title }}</h2>
    <slot></slot>
  </div>
</template>

<style scoped lang="scss">
.component {
  // 样式
}
</style>
```

请创建：[具体组件]
```

### 状态管理（Pinia）
```
请设计 Pinia 状态管理：

【要求】
1. 使用 defineStore 定义 store
2. State 类型定义
3. Getters 计算属性
4. Actions 支持同步/异步
5. 使用 $patch 更新状态
6. 支持持久化（pinia-plugin-persistedstate）
7. 模块化拆分

【示例】
```typescript
// stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>('')
  const userInfo = ref<any>(null)
  
  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const userName = computed(() => userInfo.value?.name || 'Guest')
  
  // Actions
  async function login(username: string, password: string) {
    const res = await api.login(username, password)
    token.value = res.token
    userInfo.value = res.userInfo
  }
  
  function logout() {
    token.value = ''
    userInfo.value = null
  }
  
  return { token, userInfo, isLoggedIn, userName, login, logout }
}, {
  persist: true // 持久化
})
```

请创建：[具体 Store 模块]
```

### API 请求封装
```
请封装 Axios 请求：

【要求】
1. 创建 Axios 实例
2. 请求拦截器（添加 Token）
3. 响应拦截器（统一处理错误）
4. 取消重复请求
5. 请求重试机制
6. 上传/下载进度
7. TypeScript 类型定义
8. API 统一管理

【示例】
```typescript
// utils/request.ts
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios'

const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const { code, data, message } = response.data
    if (code === 200) {
      return data
    } else {
      ElMessage.error(message || '请求失败')
      return Promise.reject(new Error(message))
    }
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default service
```

请封装：[具体 API 模块]
```

### 路由配置
```
请配置 Vue Router：

【要求】
1. 使用 createWebHistory 模式
2. 路由懒加载
3. 动态路由（基于权限）
4. 路由守卫（登录验证）
5. 路由元信息（meta）
6. 404 页面处理
7. 路由过渡动画

【示例】
```typescript
// router/index.ts
import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { 
          title: '首页',
          requiresAuth: true,
          roles: ['admin', 'user']
        }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/404.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
```

请配置：[具体路由需求]
```

### 自定义指令
```
请创建 Vue3 自定义指令：

【常见场景】
1. 权限控制（v-permission）
2. 按钮防抖节流（v-debounce/v-throttle）
3. 图片懒加载（v-lazy）
4. 复制功能（v-copy）
5. 数字格式化（v-format）
6. 拖拽排序（v-draggable）

【示例】
```typescript
// directives/permission.ts
import { DirectiveBinding } from 'vue'
import { useUserStore } from '@/stores/user'

export default {
  mounted(el: HTMLElement, binding: DirectiveBinding) {
    const { value } = binding
    const userStore = useUserStore()
    const roles = userStore.userInfo.roles
    
    if (value && value instanceof Array && value.length > 0) {
      const hasPermission = roles.some(role => value.includes(role))
      
      if (!hasPermission) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    }
  }
}
```

请创建：[具体指令]
```

### Composables 封装
```
请创建可复用的 Composable 函数：

【要求】
1. 使用 useXxx 命名
2. 返回响应式数据和方法
3. 支持参数配置
4. 生命周期管理
5. 错误处理

【示例】
```typescript
// composables/useTable.ts
import { ref, reactive, toRefs } from 'vue'

export function useTable(apiFunction: Function, options = {}) {
  const { pageSize = 10 } = options
  
  const tableData = ref([])
  const loading = ref(false)
  const pagination = reactive({
    page: 1,
    pageSize,
    total: 0
  })
  
  async function fetchData(params = {}) {
    loading.value = true
    try {
      const res = await apiFunction({
        page: pagination.page,
        pageSize: pagination.pageSize,
        ...params
      })
      tableData.value = res.list
      pagination.total = res.total
    } finally {
      loading.value = false
    }
  }
  
  function handlePageChange(page: number) {
    pagination.page = page
    fetchData()
  }
  
  return {
    tableData,
    loading,
    pagination,
    fetchData,
    handlePageChange
  }
}
```

请创建：[具体 Composable]
```

## HTML/CSS 开发提示词

### 响应式布局
```
请实现响应式页面布局：

【技术方案】
1. Flexbox 弹性布局
2. Grid 网格布局
3. Media Query 媒体查询
4. 移动端适配（rem/vw/vh）
5. CSS 预处理器（Sass/Less）

【断点标准】
- 手机：< 768px
- 平板：768px - 1024px
- 桌面：> 1024px

【要求】
1. 移动优先（Mobile First）
2. 图片响应式
3. 字体大小自适应
4. 触摸友好

请实现：[具体布局需求]
```

### CSS 动画效果
```
请创建 CSS 动画效果：

【动画类型】
1. Transition 过渡
2. Keyframe 关键帧动画
3. Transform 变换
4. Animation 复合动画

【性能优化】
1. 使用 transform 和 opacity
2. 避免 animate 布局属性
3. 使用 will-change
4. GPU 加速

【示例】
```scss
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.fade-in-up {
  animation: fadeInUp 0.6s ease-out;
}
```

请创建：[具体动画效果]
```

### 主题切换
```
请实现主题切换功能：

【方案选择】
1. CSS Variables（推荐）
2. Sass 变量 + 多套样式
3. CSS Modules
4. Styled Components

【示例（CSS Variables）】
```css
/* 默认主题 */
:root {
  --primary-color: #409eff;
  --bg-color: #ffffff;
  --text-color: #333333;
}

/* 暗黑主题 */
.dark-theme {
  --primary-color: #0a84ff;
  --bg-color: #1c1c1e;
  --text-color: #ffffff;
}

body {
  background-color: var(--bg-color);
  color: var(--text-color);
}
```

```javascript
// 切换主题
function toggleTheme(theme) {
  document.documentElement.className = theme
  localStorage.setItem('theme', theme)
}
```

请实现：[具体主题需求]
```

## 前后端交互提示词

### 接口对接
```
请完成前后端接口对接：

【要求】
1. 明确接口文档（Swagger/Apifox）
2. 请求方法（GET/POST/PUT/DELETE）
3. 请求参数格式（Query/Body/Form）
4. 响应数据结构
5. 错误码处理
6. Loading 状态
7. 表单验证
8. 文件上传/下载

【联调流程】
1. 阅读接口文档
2. 定义 TypeScript 类型
3. 编写 API 请求函数
4. 调用接口并处理响应
5. 异常处理
6. 测试验证

请对接：[具体接口]
```

### 数据可视化
```
请实现数据可视化图表：

【图表库选择】
1. ECharts（百度，功能强大）
2. AntV（蚂蚁，易用）
3. Chart.js（轻量）
4. D3.js（灵活定制）

【常见图表】
- 折线图/柱状图/饼图
- 散点图/气泡图
- 地图/热力图
- 雷达图/漏斗图
- 关系图/树图

【要求】
1. 响应式适配
2. 数据动态更新
3. 交互效果
4. 主题适配
5. 性能优化（大数据量）

请实现：[具体图表]
```

### 表单处理
```
请实现复杂表单功能：

【功能清单】
1. 表单项动态显示/隐藏
2. 表单验证（必填/格式/自定义）
3. 表单联动
4. 分步表单
5. 动态表单（可增删）
6. 表单重置
7. 表单缓存（草稿箱）
8. 批量操作

【验证规则】
- 必填项
- 邮箱格式
- 手机号格式
- 身份证格式
- URL 格式
- 长度限制
- 自定义验证

请实现：[具体表单]
```

### 列表页开发
```
请实现列表页面：

【功能清单】
1. 搜索筛选
2. 表格展示
3. 分页
4. 排序
5. 行操作（编辑/删除）
6. 批量操作
7. 导出 Excel
8. 导入数据

【表格组件】
- Element Plus Table
- Ant Design Table
- AG Grid（企业级）

请实现：[具体列表页]
```

## 性能优化提示词

### 前端性能优化
```
请优化前端性能：

【优化手段】
1. 代码分割（路由懒加载）
2. 组件懒加载
3. 图片优化（懒加载/WebP）
4. 减少 HTTP 请求（合并/雪碧图）
5. 使用 CDN
6. Gzip 压缩
7. 虚拟列表（大数据量）
8. 防抖节流
9. 合理使用缓存

【构建优化】
1. Tree Shaking
2. 按需引入 UI 组件
3. 压缩混淆
4. 提取公共代码
5. Source Map 配置

【监控指标】
- FCP（首次内容绘制）
- LCP（最大内容绘制）
- FID（首次输入延迟）
- CLS（累积布局偏移）

请优化：[具体页面]
```

### 包体积优化
```
请优化打包体积：

【分析工具】
1. webpack-bundle-analyzer
2. rollup-plugin-visualizer

【优化策略】
1. 路由懒加载
2. 组件异步加载
3. 第三方库按需引入
4. 大组件拆分
5. 移除 console.log
6. 图片压缩
7. 开启 Gzip

请优化：[具体项目]
```

## 安全防护提示词

### 前端安全
```
请加强前端安全防护：

【防护要点】
1. XSS 攻击（转义/过滤）
2. CSRF 攻击（Token 验证）
3. 点击劫持（X-Frame-Options）
4. 敏感信息加密存储
5. 接口鉴权
6. 防止重放攻击
7. 内容安全策略（CSP）

【安全措施】
- 输入校验
- 输出编码
- HTTPS
- Token 机制
- 敏感操作二次验证

请加固：[具体模块]
```
