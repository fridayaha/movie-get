const superagent = require('superagent');
const cheerio = require('cheerio');
const reptileUrl = "http://www.dytt8.net/";

superagent.get(reptileUrl).end(function (err, res) {
    // 抛错拦截
    if (err) {
        throw Error(err);
    }
    /**
    * res.text 包含未解析前的响应内容
    * 我们通过cheerio的load方法解析整个文档，就是html页面所有内容，可以通过console.log($.html());在控制台查看
    */
    var $ = cheerio.load(res.text);
    var patt=/^\/html(\/\w+)*\/\d+\/\d+\.html$/;
    $("[href]").each(function (i, elem) {
        var hrefLink = $(elem).attr('href');
        // console.log(hrefLink)
        if(hrefLink.endsWith("index.html")){
            //递归遍历所有文章首页
        }
        
        if(patt.test(hrefLink)){
            console.log(hrefLink)
        }
    });
});