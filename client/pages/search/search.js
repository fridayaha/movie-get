var message = require('../../component/message/message')
var douban = require('../../comm/script/fetch')
var config = require('../../comm/script/config')
Page({
  data: {
    searchType: 'keyword',
    hotKeywords: [],
    hotTag: config.hotTag
  },
  onLoad: function (options) {
    var that = this
    wx.getStorage({
      key: 'film_search_history',
      success: function (res) {
        console.log(res.data);
        console.log('----加载搜索记录成功----')
        that.setData({
          hotKeywords: res.data
        })
      }
    })
  },
  changeSearchType: function () {
    var types = ['默认', '类型'];
    var searchType = ['keyword', 'tag']
    var that = this
    wx.showActionSheet({
      itemList: types,
      success: function (res) {
        console.log(res)
        if (!res.cancel) {
          that.setData({
            searchType: searchType[res.tapIndex]
          })
        }
      }
    })
  },
  search: function (e) {
    var that = this
    var keyword = e.detail.value.keyword
    if (keyword == '') {
      message.show.call(that, {
        content: '请输入内容',
        icon: 'null',
        duration: 1500
      })
      return false
    } else {
      var hotKeywords = that.data.hotKeywords;
      hotKeywords.push(keyword);
      console.log(hotKeywords);
        wx.setStorage({
          key: "film_search_history",
          data: hotKeywords,
          success: function (res) {
            console.log(hotKeywords);
            console.log('----保存搜索记录成功----');
          }
        })
      var searchUrl = that.data.searchType == 'keyword' ? config.apiList.search.byKeyword : config.apiList.search.byTag
      wx.navigateTo({
        url: '../searchResult/searchResult?url=' + encodeURIComponent(searchUrl) + '&keyword=' + keyword
      })
    }
  },
  searchByKeyword: function (e) {
    var that = this
    var keyword = e.currentTarget.dataset.keyword
    wx.navigateTo({
      url: '../searchResult/searchResult?url=' + encodeURIComponent(config.apiList.search.byKeyword) + '&keyword=' + keyword
    })
  },
  searchByTag: function (e) {
    var that = this
    var keyword = e.currentTarget.dataset.keyword
    wx.navigateTo({
      url: '../searchResult/searchResult?url=' + encodeURIComponent(config.apiList.search.byTag) + '&keyword=' + keyword
    })
  }
})