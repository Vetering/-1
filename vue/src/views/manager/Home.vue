<template>
  <div>
    <div style="box-shadow: 0 0 10px rgba(0,0,0,.1); padding: 10px 20px; border-radius: 5px; ">
      {{ user.name }}
    </div>

    <div style="display: flex">
      <el-card style="width: 100%;">
        <div slot="header" class="clearfix">
          <div class="dashboard-container">
            <el-row :gutter="20" >
              <el-col :span="12">
                <el-card shadow="hover">
                  <div slot="header" class="clearfix">
                    <span>系统信息</span>
                  </div>
                  <el-descriptions :column="2">
                    <el-descriptions-item
                        v-for="info in systemInfo"
                        :key="info.label"
                        :label="info.label"
                    >
                      {{ info.value }}
                    </el-descriptions-item>
                  </el-descriptions>
                </el-card>
              </el-col>
            </el-row>
          </div>
        </div>
      </el-card>
    </div>
    <div style="display: flex;">
      <el-card style="width: 50%; margin-right: 10px">
        <div style="margin-bottom: 15px; font-size: 20px; font-weight: bold">系统公告</div>
        <el-timeline style="padding: 0">
          <el-timeline-item v-for="item in notices" :key="item.id" :timestamp="item.time" placement="top">
            <el-card>
              <h4>{{ item.title }}</h4>
              <div v-html="item.content"></div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </el-card>

      <el-card style="width: 50%">
        <div style="margin-bottom: 15px; font-size: 20px; font-weight: bold">系统公告</div>
        <el-collapse v-model="activeName" accordion>
          <el-collapse-item  v-for="(item, index) in notices" :key="item.id" :name="index + ''">
            <template slot="title">
              <div style="display: flex; align-items: center; width: 100%">
                <h4 style="flex: 1">{{ item.title }}</h4>
                <div style="width: 150px; color: #888">{{ item.time }}</div>
              </div>
            </template>
            <div v-html="item.content"></div>
          </el-collapse-item>
        </el-collapse>
      </el-card>
    </div>

  </div>
</template>

<script>
export default {
  name: "Home",
  data() {
    return {
      systemInfo: [],
      user: JSON.parse(localStorage.getItem('StudentSystem-user') || '{}'),
      notices: [],
      activeName: '0'
    }
  },
  created() {
    this.loadNotice(),this.getSystemInfo()
  },
  methods: {
    getSystemInfo() {
      const userAgent = navigator.userAgent;

      // 获取操作系统
      let os = "Unknown OS";
      if (userAgent.indexOf("Win") !== -1) os = "Windows";
      if (userAgent.indexOf("Mac") !== -1) os = "Macintosh";
      if (userAgent.indexOf("Linux") !== -1) os = "Linux";
      if (userAgent.indexOf("Android") !== -1) os = "Android";
      if (userAgent.indexOf("like Mac") !== -1) os = "iOS";
      this.systemInfo.push({ label: "操作系统", value: os });

      // 获取浏览器
      let browser = "Unknown Browser";
      if (userAgent.indexOf("Chrome") !== -1) browser = "Edge/Chrome";
      if (userAgent.indexOf("Firefox") !== -1) browser = "Firefox";
      if (userAgent.indexOf("Edge") !== -1) browser = "Edge";
      if (userAgent.indexOf("Opera") !== -1) browser = "Opera";
      if (userAgent.indexOf("Trident") !== -1) browser = "Internet Explorer";
      this.systemInfo.push({ label: "浏览器", value: browser });
      const now = new Date();
      const year = now.getFullYear();
      const month = now.getMonth() + 1; // 月份需要加 1
      const day = now.getDate();
      const loginTime = `${year}-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}`;
      this.systemInfo.push({ label: "登录时间", value: loginTime });
      },
    loadNotice() {
      this.$request.get('/api/notice/selectUserData').then(res => {
        this.notices = res.data
      })
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 30px;
}

.total-number {
  font-size: 32px;
  font-weight: bold;
}

.clearfix {
  clear: both;
}
</style>