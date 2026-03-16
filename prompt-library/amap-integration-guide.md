# 高德地图接入配置说明

## 一、准备工作

### 1.1 注册高德开放平台账号
1. 访问 [高德开放平台](https://lbs.amap.com/)
2. 点击右上角"注册"按钮
3. 使用手机号或邮箱完成注册
4. 登录控制台

### 1.2 创建应用
1. 登录后进入 [控制台](https://console.amap.com/)
2. 点击"应用管理" → "我的应用"
3. 点击"创建新应用"
4. 填写应用信息：
   - **应用名称**：自定义（如：jiliguala-map）
   - **应用类型**：根据实际选择（Web 端/移动端/服务端）
   - **白名单**：根据平台配置域名或包名

### 1.3 获取 Key 和 Secret
1. 在创建的应用下，点击"添加 Key"
2. 选择服务平台：
   - **Web 端 (JS API)**：用于前端页面地图展示
   - **后端服务 (Web 服务)**：用于服务端地理编码、路径规划等
   - **Android/iOS**：用于移动端应用
3. 填写 Key 信息：
   - **Key 名称**：自定义（如：web-js-key）
   - **安全代码**：Web 端填写域名白名单
4. 提交后获得：
   - **Key**（公钥）：类似 `a1b2c3d4e5f6g7h8i9j0`
   - **Secret**（密钥）：仅后端服务需要

## 二、前端接入（Vue3 + TypeScript）

### 2.1 安装依赖
```bash
npm install @amap/amap-jsapi-loader
```

### 2.2 配置文件
```typescript
// src/config/map.config.ts
export const MAP_CONFIG = {
  // 高德地图 Key
  key: import.meta.env.VITE_AMAP_KEY,
  
  // 使用的地图版本
  version: '2.0',
  
  // 默认中心点坐标（北京）
  center: [116.397428, 39.90923],
  
  // 默认缩放级别
  zoom: 12,
  
  // 支持的缩放级别范围
  zooms: [3, 20],
  
  // 地图类型
  mapStyle: 'amap://styles/normal' // 可选：normal, dark, blue, warm
}
```

### 2.3 环境变量配置
```bash
# .env.development
VITE_AMAP_KEY=你的 Web 端 JS API Key

# .env.production
VITE_AMAP_KEY=你的 Web 端 JS API Key
```

### 2.4 封装 Map 组件
```vue
<!-- src/components/AMap/index.vue -->
<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'

interface Props {
  width?: string
  height?: string
  zoom?: number
  center?: [number, number]
}

const props = withDefaults(defineProps<Props>(), {
  width: '100%',
  height: '400px',
  zoom: 12,
  center: () => [116.397428, 39.90923]
})

const emit = defineEmits<{
  (e: 'map-loaded', map: any): void
  (e: 'click', data: { lnglat: [number, number]; address: string }): void
}>()

const mapContainer = ref<HTMLElement | null>(null)
let map: any = null
let geolocation: any = null

// 初始化地图
const initMap = async () => {
  try {
   const AMap = await AMapLoader.load({
      key: import.meta.env.VITE_AMAP_KEY,
      version: '2.0',
      plugins: ['AMap.Scale', 'AMap.ToolBar', 'AMap.MapType', 'AMap.Geolocation']
    })
    
    map = new AMap.Map(mapContainer.value!, {
      zoom: props.zoom,
      center: props.center,
      viewMode: '3D',
      mapStyle: 'amap://styles/normal'
    })
    
    // 添加控件
    map.addControl(new AMap.Scale())
    map.addControl(new AMap.ToolBar())
    map.addControl(new AMap.MapType())
    
    // 添加定位功能
    geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
     timeout: 10000,
      buttonPosition: 'RB',
      buttonOffset: new AMap.Pixel(10, 20),
      zoomToAccuracy: true
    })
    
    map.addControl(geolocation)
    
    // 监听点击事件
    map.on('click', async (e: any) => {
     const lnglat = e.lnglat
     const address = await getAddressByLocation(lnglat)
      emit('click', { lnglat: [lnglat.lng, lnglat.lat], address })
    })
    
    emit('map-loaded', map)
  } catch (error) {
   console.error('地图加载失败:', error)
  }
}

// 逆地理编码（坐标转地址）
const getAddressByLocation = async (lnglat: any): Promise<string> => {
  return new Promise((resolve, reject) => {
    AMap.plugin('AMap.Geocoder', () => {
     const geocoder = new AMap.Geocoder({
        radius: 1000,
        extensions: 'base'
      })
      
      geocoder.getAddress(lnglat, (status: string, result: any) => {
        if (status === 'complete' && result.regeocode) {
         resolve(result.regeocode.formattedAddress)
        } else {
         reject(new Error('地址解析失败'))
        }
      })
    })
  })
}

// 定位到当前位置
const locateToCurrent = () => {
  geolocation.getCurrentPosition((status: string, result: any) => {
    if (status === 'complete') {
      map.setCenter(result.position)
    } else {
      ElMessage.error('定位失败：' + result.message)
    }
  })
}

// 添加标记点
const addMarker = (position: [number, number], title?: string) => {
  const marker = new AMap.Marker({
    position,
   title: title || ''
  })
  map.add(marker)
  return marker
}

// 清除所有标记
const clearMarkers = () => {
  map.clearMap()
}

onMounted(() => {
  initMap()
})

onBeforeUnmount(() => {
  if (map) {
    map.destroy()
  }
})

defineExpose({
  locateToCurrent,
  addMarker,
  clearMarkers
})
</script>

<template>
  <div ref="mapContainer" :style="{ width, height }"></div>
</template>

<style scoped>
:deep(.amap-logo),
:deep(.amap-copyright) {
  z-index: 1;
}
</style>
```

### 2.5 使用示例
```vue
<!-- 页面中使用 -->
<script setup lang="ts">
import { ref } from 'vue'
import AMap from '@/components/AMap/index.vue'

const mapRef = ref<any>(null)

const handleMapClick = (data: any) => {
  console.log('地图点击:', data)
  ElMessage.success(`已选择：${data.address}`)
}

const handleLocate = () => {
  mapRef.value?.locateToCurrent()
}

const handleAddMarker = () => {
  mapRef.value?.addMarker([116.397428, 39.90923], '测试标记')
}
</script>

<template>
  <div class="map-page">
    <div class="controls">
      <el-button @click="handleLocate">定位到我这里</el-button>
      <el-button @click="handleAddMarker">添加标记</el-button>
    </div>
    
    <AMap
     ref="mapRef"
      width="100%"
      height="600px"
      @map-loaded="(map) => console.log('地图加载成功')"
      @click="handleMapClick"
    />
  </div>
</template>
```

## 三、后端接入（Spring Boot）

### 3.1 添加依赖
```xml
<!-- pom.xml -->
<dependencies>
    <!-- HTTP 客户端 -->
    <dependency>
        <groupId>org.apache.httpcomponents</groupId>
        <artifactId>httpclient</artifactId>
        <version>4.5.14</version>
    </dependency>
    
    <!-- JSON 处理 -->
    <dependency>
        <groupId>com.alibaba.fastjson2</groupId>
        <artifactId>fastjson2</artifactId>
        <version>2.0.40</version>
    </dependency>
</dependencies>
```

### 3.2 配置文件
```yaml
# application.yml
amap:
  key: ${AMAP_KEY:你的后端服务 Key}
  secret: ${AMAP_SECRET:你的后端服务 Secret}
  # Web 服务 API 基础 URL
  api:
    base-url: https://restapi.amap.com/v3
    # 地理编码
    geocode: /geocode/geo
    # 逆地理编码
   regeocode: /geocode/regeo
    # 地点搜索
    place-search: /place/text
    # 路径规划
    driving: /direction/driving
    # IP 定位
    ip-location: /ip
    # 静态地图
   static-map: /staticmap
```

### 3.3 配置类
```java
// config/AmapConfig.java
package com.company.project.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {
    
    /**
     * 高德地图 Key
     */
    private String key;
    
    /**
     * 高德地图 Secret
     */
    private String secret;
    
    /**
     * API 配置
     */
    private ApiConfig api = new ApiConfig();
    
    @Data
    public static class ApiConfig {
        private String baseUrl = "https://restapi.amap.com/v3";
        private String geocode;
        private String regeocode;
        private String placeSearch;
        private String driving;
        private String ipLocation;
        private String staticMap;
    }
}
```

### 3.4 服务封装
```java
// service/impl/AmapServiceImpl.java
package com.company.project.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.company.project.config.AmapConfig;
import com.company.project.service.AmapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmapServiceImpl implements AmapService {
    
    private final AmapConfig amapConfig;
    
    @Override
    public GeocodeResponse geocode(String address) {
       try {
            Map<String, String> params = new HashMap<>();
           params.put("address", address);
           params.put("key", amapConfig.getKey());
            
            String response = get(amapConfig.getApi().getBaseUrl() + 
                                amapConfig.getApi().getGeocode(), params);
            
            JSONObject result = JSON.parseObject(response);
            if ("1".equals(result.getString("status"))) {
               return parseGeocodeResponse(result);
            } else {
                log.error("地理编码失败：{}", result.getString("info"));
                throw new RuntimeException("地理编码失败：" + result.getString("info"));
            }
        } catch (Exception e) {
            log.error("地理编码异常", e);
            throw new RuntimeException("地理编码异常", e);
        }
    }
    
    @Override
    public RegeocodeResponse regeocode(Double longitude, Double latitude) {
       try {
            Map<String, String> params = new HashMap<>();
           params.put("location", longitude + "," + latitude);
           params.put("key", amapConfig.getKey());
           params.put("extensions", "all");
           params.put("coordsys", "gps");
            
            String response = get(amapConfig.getApi().getBaseUrl() + 
                                amapConfig.getApi().getRegeocode(), params);
            
            JSONObject result = JSON.parseObject(response);
            if ("1".equals(result.getString("status"))) {
               return parseRegeocodeResponse(result);
            } else {
                log.error("逆地理编码失败：{}", result.getString("info"));
                throw new RuntimeException("逆地理编码失败：" + result.getString("info"));
            }
        } catch (Exception e) {
            log.error("逆地理编码异常", e);
            throw new RuntimeException("逆地理编码异常", e);
        }
    }
    
    @Override
    public IpLocationResponse ipLocation(String ip) {
       try {
            Map<String, String> params = new HashMap<>();
           params.put("key", amapConfig.getKey());
            if (ip != null && !ip.isEmpty()) {
               params.put("ip", ip);
            }
            
            String response = get(amapConfig.getApi().getBaseUrl() + 
                                amapConfig.getApi().getIpLocation(), params);
            
            JSONObject result = JSON.parseObject(response);
            if ("1".equals(result.getString("status"))) {
               return parseIpLocationResponse(result);
            } else {
                log.error("IP 定位失败：{}", result.getString("info"));
                throw new RuntimeException("IP 定位失败：" + result.getString("info"));
            }
        } catch (Exception e) {
            log.error("IP 定位异常", e);
            throw new RuntimeException("IP 定位异常", e);
        }
    }
    
    /**
     * HTTP GET 请求
     */
    private String get(String url, Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder(url);
        sb.append("?");
        for (Map.Entry<String, String> entry: params.entrySet()) {
            sb.append(entry.getKey())
              .append("=")
              .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
              .append("&");
        }
        
        String fullUrl = sb.toString();
        
       try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(fullUrl);
           try (CloseableHttpResponse response = httpClient.execute(request)) {
               return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            }
        }
    }
    
    /**
     * 添加签名（部分接口需要）
     */
    private String addSignature(Map<String, String> params) {
       params.put("key", amapConfig.getKey());
        long timestamp = System.currentTimeMillis();
       params.put("timestamp", String.valueOf(timestamp));
        
        StringBuilder sb = new StringBuilder();
       params.entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .forEach(entry -> sb.append(entry.getKey())
                                  .append("=")
                                  .append(entry.getValue())
                                  .append("&"));
        sb.append(amapConfig.getSecret());
        
       try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String hexStr = Integer.toHexString(b & 0xFF);
                if (hexStr.length() == 1) {
                    hex.append("0");
                }
                hex.append(hexStr);
            }
           return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("签名生成失败", e);
        }
    }
    
    // 解析响应的方法省略...
}
```

### 3.5 Controller 接口
```java
// controller/AmapController.java
package com.company.project.controller;

import com.company.project.dto.*;
import com.company.project.service.AmapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/map")
@RequiredArgsConstructor
public class AmapController {
    
    private final AmapService amapService;
    
    /**
     * 地址转坐标
     */
    @GetMapping("/geocode")
    public Result<GeocodeResponse> geocode(@RequestParam String address) {
       return Result.success(amapService.geocode(address));
    }
    
    /**
     * 坐标转地址
     */
    @GetMapping("/regeocode")
    public Result<RegeocodeResponse> regeocode(
            @RequestParam Double longitude,
            @RequestParam Double latitude) {
       return Result.success(amapService.regeocode(longitude, latitude));
    }
    
    /**
     * IP 定位
     */
    @GetMapping("/ip-location")
    public Result<IpLocationResponse> ipLocation(
            @RequestParam(required = false) String ip) {
       return Result.success(amapService.ipLocation(ip));
    }
}
```

## 四、常用功能示例

### 4.1 地址解析（地理编码）
```typescript
// 前端调用
const searchLocation = async (address: string) => {
  try {
   const res = await axios.get('/api/v1/map/geocode', {
     params: { address }
    })
   console.log('坐标信息:', res.data)
    // 输出：{ province: '北京市', city: '北京市', location: { lng: 116.397428, lat: 39.90923 } }
  } catch (error) {
   console.error('查询失败:', error)
  }
}
```

### 4.2 周边搜索
```typescript
// 搜索周边的设施
const searchNearby = async (location: [number, number], keywords: string, radius: number = 1000) => {
  const AMap = await AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY,
    version: '2.0'
  })
  
  AMap.plugin('AMap.PlaceSearch', () => {
   const placeSearch = new AMap.PlaceSearch({
     pageSize: 10,
     pageIndex: 1,
      extensions: 'all' // 返回详细信息
    })
    
    placeSearch.searchNearBy(keywords, location, radius, (status: string, result: any) => {
      if (status === 'complete') {
       console.log('周边搜索结果:', result.poiList.pois)
      }
    })
  })
}

// 使用示例
searchNearby([116.397428, 39.90923], '餐厅', 2000)
```

### 4.3 路径规划
```typescript
// 驾车路线规划
const planDrivingRoute = async (start: [number, number], end: [number, number]) => {
  const AMap = await AMapLoader.load({
    key: import.meta.env.VITE_AMAP_KEY,
    version: '2.0'
  })
  
  AMap.plugin('AMap.Driving', () => {
   const driving = new AMap.Driving({
      map: mapInstance, // 地图对象
      policy: AMap.DrivingPolicy.LEAST_TIME // 策略：最快路线
    })
    
    driving.search(
     new AMap.LngLat(start[0], start[1]),
     new AMap.LngLat(end[0], end[1]),
      (status: string, result: any) => {
        if (status === 'complete') {
         const route = result.routes[0]
         console.log('距离:', route.distance, '米')
         console.log('预计时间:', route.duration, '秒')
        }
      }
    )
  })
}
```

### 4.4 天气查询
```java
// 后端服务方法
public WeatherResponse queryWeather(String city) {
   try {
        Map<String, String> params = new HashMap<>();
       params.put("key", amapConfig.getKey());
       params.put("city", city);
       params.put("extensions", "base"); // base:实时/all:预报
        
        String response = get("https://restapi.amap.com/v3/weather/weatherInfo", params);
        JSONObject result = JSON.parseObject(response);
        
        if ("1".equals(result.getString("status"))) {
            // 解析天气数据
            JSONArray lives = result.getJSONArray("lives");
            if (lives != null && lives.size() > 0) {
                JSONObject live = lives.getJSONObject(0);
                WeatherResponse weather = new WeatherResponse();
                weather.setCity(live.getString("city"));
                weather.setWeather(live.getString("weather"));
                weather.setTemperature(live.getString("temperature"));
                weather.setWindDirection(live.getString("winddirection"));
                weather.setWindPower(live.getString("windpower"));
                weather.setHumidity(live.getString("humidity"));
               return weather;
            }
        }
        throw new RuntimeException("天气查询失败");
    } catch (Exception e) {
        log.error("天气查询异常", e);
        throw new RuntimeException("天气查询异常", e);
    }
}
```

## 五、常见问题与解决方案

### 5.1 Key 安全配置
**问题**：Key 被盗用导致额度超限

**解决方案**：
1. **设置白名单**
   - Web 端：绑定域名（如：`*.example.com`）
   - 移动端：绑定包名 + SHA1
   - 服务端：绑定 IP 地址

2. **使用 Referer 验证**
   ```nginx
   # Nginx 配置
   location / {
       valid_referers none blocked *.example.com example.com;
       if ($invalid_referer) {
          return 403;
       }
   }
   ```

3. **后端代理请求**
   - 敏感操作通过后端转发
   - 避免在前端暴露 Secret

### 5.2 配额限制
**免费额度**：
- JS API：个人认证用户 30 万次/天
- Web 服务：个人认证用户 5000 次/天
- 静态地图：个人认证用户 1000 次/天

**提升方案**：
1. 完成实名认证
2. 企业认证提升更高额度
3. 购买商业授权

### 5.3 坐标系转换
**问题**：GPS 坐标与高德坐标不一致

**解决方案**：
```java
// GPS 转高德坐标（GCJ-02）
public class CoordinateConverter {
    
    private static final double PI = Math.PI;
    private static final double AXIS = 6378245.0;
    private static final double EE = 0.00669342162296594323;
    
    public static double[] gpsToGcj02(double lat, double lon) {
        if (outOfChina(lat, lon)) {
           return new double[]{lat, lon};
        }
        
       double dLat = transformLat(lon- 105.0, lat - 35.0);
       double dLon = transformLon(lon - 105.0, lat - 35.0);
       double radLat = lat/ 180.0 * PI;
       double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
       double sqrtMagic = Math.sqrt(magic);
        
        dLat = (dLat * 180.0) / ((AXIS * (1 - EE)) / (magic * sqrtMagic) * PI);
       dLon = (dLon * 180.0) / (AXIS / sqrtMagic * Math.cos(radLat) * PI);
        
       double mgLat = lat + dLat;
       double mgLon = lon + dLon;
        
       return new double[]{mgLat, mgLon};
    }
    
    private static boolean outOfChina(double lat, double lon) {
       return !(lon >= 73.66 && lon <= 135.05 && lat >= 3.86 && lat <= 53.55);
    }
    
    private static double transformLat(double x, double y) {
       double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
       ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
       ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
       ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
       return ret;
    }
    
    private static double transformLon(double x, double y) {
       double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
       ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
       ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
       ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
       return ret;
    }
}
```

### 5.4 错误码处理
```typescript
// 常见错误码
const ERROR_CODES = {
  '10000': '请求正常',
  '10001': 'KEY 不正确或服务过期',
  '10002': 'KEY 不存在或已删除',
  '10003': '自增 id 不正确',
  '10004': '安全代码校验失败',
  '10005': '日请求量超过限额',
  '10006': '用户类型被禁止',
  '10007': 'IP 白名单校验失败',
  '10008': 'Referer 白名单校验失败',
  '10009': '签名校验失败',
  '10010': '无权限访问',
  '10011': 'HOST 绑定校验失败',
  '10012': 'MCode 绑定校验失败',
  '10013': '起终点太近或相同',
  '10014': '路径规划失败',
  '10015': '行政区划非法',
  '10016': '没有可用路线',
  '10017': '无效的参数',
  '10018': '缺少必填参数',
  '10019': '参数格式错误',
  '10020': '超出服务范围'
}

// 统一错误处理
const handleAmapError = (code: string) => {
  const message = ERROR_CODES[code as keyof typeof ERROR_CODES] || '未知错误'
  ElMessage.error(`高德地图错误 (${code}): ${message}`)
  return message
}
```

## 六、性能优化建议

### 6.1 前端优化
1. **按需加载插件**
   ```typescript
   // 只加载需要的插件
   AMapLoader.load({
     key: import.meta.env.VITE_AMAP_KEY,
     version: '2.0',
     plugins: ['AMap.Scale', 'AMap.ToolBar'] // 按需引入
   })
   ```

2. **防抖节流**
   ```typescript
   // 搜索框输入防抖
  const searchInput = ref('')
  const debouncedSearch = useDebounceFn((value: string) => {
     searchLocation(value)
   }, 500)
   
   watch(searchInput, (val) => {
    debouncedSearch(val)
   })
   ```

3. **标记点聚合**
   ```typescript
   // 大量标记点时使用聚合
   AMap.plugin('AMap.MarkerClusterer', () => {
    const markers = [...] // 大量标记点
    new AMap.MarkerClusterer(map, markers, {
       gridSize: 80,
       maxZoom: 17
     })
   })
   ```

### 6.2 后端优化
1. **缓存热点数据**
   ```java
   @Cacheable(value = "geocode", key = "#address")
   public GeocodeResponse geocode(String address) {
      return amapService.geocode(address);
   }
   ```

2. **批量处理**
   ```java
   // 批量地理编码（一次最多 10 个）
   public List<GeocodeResponse> batchGeocode(List<String> addresses) {
       // 分批处理，每批 10 个
      return Lists.partition(addresses, 10).stream()
           .flatMap(batch -> {
               // 调用批量接口
              return batch.stream().map(this::geocode);
           })
           .collect(Collectors.toList());
   }
   ```

3. **异步处理**
   ```java
   @Async
   public CompletableFuture<GeocodeResponse> geocodeAsync(String address) {
      return CompletableFuture.completedFuture(geocode(address));
   }
   ```

## 七、监控与告警

### 7.1 使用量监控
```java
// 记录 API 调用次数
@Component
@Aspect
public class AmapMonitorAspect {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @Around("@annotation(MonitorAmap)")
    public Object recordUsage(ProceedingJoinPoint pjp) throws Throwable {
        String methodName = pjp.getSignature().getName();
        String date = LocalDate.now().toString();
        String key = String.format("amap:usage:%s:%s", date, methodName);
        
        long count = redisTemplate.opsForValue().increment(key);
       redisTemplate.expire(key, 7, TimeUnit.DAYS);
        
        // 检查是否接近配额
        if (count > 4500) { // 假设配额 5000
            sendAlert(methodName, count);
        }
        
       return pjp.proceed();
    }
    
    private void sendAlert(String method, long count) {
        // 发送告警通知
        log.warn("高德地图 API [{}] 今日调用量已达：{}", method, count);
    }
}
```

### 7.2 成功率监控
```java
// 记录失败率
@Component
public class AmapMetrics {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    private final Counter apiCallCounter;
    private final Counter apiErrorCounter;
    
    public AmapMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.apiCallCounter = meterRegistry.counter("amap.api.call.total");
        this.apiErrorCounter = meterRegistry.counter("amap.api.error.total");
    }
    
    public void recordSuccess() {
        apiCallCounter.increment();
    }
    
    public void recordFailure() {
        apiCallCounter.increment();
        apiErrorCounter.increment();
    }
    
    public double getSuccessRate() {
       double total = apiCallCounter.count();
       double errors = apiErrorCounter.count();
       return total > 0 ? (total - errors) / total : 1.0;
    }
}
```

## 八、附录

### 8.1 官方文档链接
- [高德开放平台](https://lbs.amap.com/)
- [Web 服务 API 文档](https://lbs.amap.com/api/webservice/guide/create-project/get-key)
- [JavaScript API 文档](https://lbs.amap.com/api/javascript-api-v2/summary)
- [Android SDK 文档](https://lbs.amap.com/api/android-sdk/summary)
- [iOS SDK 文档](https://lbs.amap.com/api/ios-sdk/summary)

### 8.2 技术支持
- 工单系统：https://console.amap.com/dev/ticket
- 社区论坛：https://lbs.amap.com/bbs/
- QQ 群：12345678（示例）

### 8.3 更新日志
- v1.0 - 2024-01-XX - 初始版本
- 后续根据实际需求持续更新
