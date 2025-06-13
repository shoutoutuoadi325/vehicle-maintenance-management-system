const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 3000, // 前端端口设置为3000，避免与后端8080冲突
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        logLevel: 'debug'
      }
    }
  }
})
