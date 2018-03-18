/*
备注
city: 城市（在程序载入时获取一次）
count: 返回结果数量
apiList: api列表
hotTag: 搜索页热门类型bannerList: 首页（热映页）轮播图列表列表
skinList: “我的”页面背景列表
shakeSound: 摇一摇音效地址（带url表示远程地址）
shakeWelcomeImg: 摇一摇欢迎图片
*/
var url = '/dist'
module.exports = {
  city: 'nanjing',
  count: 20,
  apiList: {
    popular: 'https://www.soft256.com/v2/movie/newest',
    coming: 'https://www.soft256.com/v2/movie/coming_soon',
    top: 'https://www.soft256.com/v2/movie/top250',
    search: {
      byKeyword: 'https://www.soft256.com/v2/movie/search?q=',
      byTag: 'https://www.soft256.com/v2/movie/search?tag='
    },
    filmDetail: 'https://www.soft256.com/v2/movie/subject/',
    personDetail: 'https://www.soft256.com/v2/movie/celebrity/'
  },
  hotTag: ['动作', '喜剧', '爱情', '悬疑'],
  bannerList: [
    { type: 'film', id: '26698897', imgUrl: url + '/images/banner_1.png' },
    { type: 'film', id: '26752852', imgUrl: url + '/images/banner_2.png' },
    { type: 'film', id: '6390825', imgUrl: url + '/images/banner_3.png' },
    { type: 'film', id: '3445906', imgUrl: url + '/images/banner_4.png' },
    { type: 'film', id: '3025375', imgUrl: url + '/images/banner_5.jpg' }
  ],
  skinList: [
    { title: '公路', imgUrl: url + '/images/user_bg_1.jpg' }
  ],
  shakeSound: {
    startUrl: url + '/sound/shake.mp3',
    start: '',
    completeUrl: url + '/sound/shakeComplete.wav',
    complete: ''
  },
  shakeWelcomeImg: url + '/images/shake_welcome.png'
}