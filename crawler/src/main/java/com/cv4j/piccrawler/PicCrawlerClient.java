package com.cv4j.piccrawler;

import com.cv4j.piccrawler.domain.Proxy;
import com.cv4j.piccrawler.download.DownloadManager;
import com.cv4j.piccrawler.http.HttpManager;
import com.cv4j.piccrawler.http.HttpParam;
import com.cv4j.piccrawler.download.strategy.FileStrategy;
import com.cv4j.piccrawler.parser.PageParser;
import com.cv4j.piccrawler.parser.PicParser;
import com.safframework.tony.common.utils.IOUtils;
import com.safframework.tony.common.utils.Preconditions;
import io.reactivex.*;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Created by tony on 2017/9/11.
 */
@Slf4j
public class PicCrawlerClient {

    private int repeat = 1;                    // 下载单张图片的重复次数，对下载网页中对图片无效
    private int sleepTime = 0;                 // 每次请求url时先sleep一段时间
    private HttpManager httpManager;           // 网络框架的管理类
    private DownloadManager downloadManager;   // 下载的管理类
    private HttpParam.HttpParamBuilder httpParamBuilder = new HttpParam.HttpParamBuilder(); // 网络请求的参数builder
    private boolean isWebPage = false;         // 是否下载网页的图片
    private PageParser pageParser;

    private PicCrawlerClient() {

        httpManager = HttpManager.get();
        downloadManager = DownloadManager.get();
    }

    public static PicCrawlerClient get() {

        return new PicCrawlerClient();
    }

    /******************* PicCrawlerClient 的配置 Start *******************／

    /**
     * @param userAgent 添加User-Agent
     * @return
     */
    public PicCrawlerClient ua(String userAgent) {

        if (Preconditions.isNotBlank(userAgent)) {
            addHeader("User-Agent",userAgent);
        }
        return this;
    }

    /**
     * @param referer
     * @return
     */
    public PicCrawlerClient referer(String referer) {

        if (Preconditions.isNotBlank(referer)) {
            addHeader("Referer",referer);
        }
        return this;
    }

    /**
     * 使用了autoReferer()，就不需要再使用referer()，比较适合懒人使用
     * @return
     */
    public PicCrawlerClient autoReferer() {

        httpParamBuilder.autoReferer();
        return this;
    }

    /**
     * @param timeOut 设置超时时间
     * @return
     */
    public PicCrawlerClient timeOut(int timeOut) {

        if (timeOut>0) {
            httpParamBuilder.timeOut(timeOut);
        }
        return this;
    }

    /**
     * @param fileStrategy 设置生成文件的策略
     * @return
     */
    public PicCrawlerClient fileStrategy(FileStrategy fileStrategy) {

        if (fileStrategy!=null) {
            downloadManager.setFileStrategy(fileStrategy);
        }
        return this;
    }

    /**
     * @param repeat 设置重复次数，只对单个图片有效，对下载网页中的图片无效。
     * @return
     */
    public PicCrawlerClient repeat(int repeat) {

        if (repeat > 0) {
            this.repeat = repeat;
        }
        return this;
    }

    /**
     * @param sleepTime 每次请求url时先sleep一段时间，单位是milliseconds
     * @return
     */
    public PicCrawlerClient sleep(int sleepTime) {

        if (sleepTime > 0) {
            this.sleepTime = sleepTime;
        }
        return this;
    }

    /**
     *
     * @param proxy 代理的host
     * @return
     */
    public PicCrawlerClient addProxy(Proxy proxy) {

        if (proxy!=null) {
            httpParamBuilder.addProxy(proxy);
        }
        return this;
    }

    /**
     *
     * @param proxyList 代理的host的列表
     * @return
     */
    public PicCrawlerClient addProxyPool(List<Proxy> proxyList) {

        if (Preconditions.isNotBlank(proxyList)) {
            httpParamBuilder.addProxyPool(proxyList);
        }
        return this;
    }

    /**
     *
     * @param cookie 设置浏览器的cookie
     * @return
     */
    public PicCrawlerClient cookie(BasicClientCookie cookie) {

        if (Preconditions.isNotBlank(cookie)) {
            httpParamBuilder.cookie(cookie);
        }
        return this;
    }

    /**
     * 添加header
     * @param name
     * @param value
     * @return
     */
    public PicCrawlerClient addHeader(String name, String value) {

        httpParamBuilder.addHeader(name,value);
        return this;
    }

    /**
     * 对于CrawlerClient必须要使用builder()，设置的一些配置就无效了
     * @return
     */
    public PicCrawlerClient build() {

        httpManager.setHttpParam(httpParamBuilder.build());
        return this;
    }

    /******************* PicCrawlerClient 的配置 End *******************／

    /**
     * 下载图片
     *
     * @param url 图片地址
     * @return
     */
    public void downloadPic(String url) {

        if (isWebPage) {

            // 如果是下载网页上的图片，则repeat不用起任何作用
            if (sleepTime>0) {

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }

            doDownloadPic(url);

        } else {

            for (int i = 0; i < repeat; i++) {

                if (sleepTime>0) {

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }

                doDownloadPic(url);
            }
        }
    }

    /**
     * 具体实现图片下载的方法
     *
     * @param url
     */
    private void doDownloadPic(String url) {

        try {

            if (Preconditions.isNotBlank(httpParamBuilder.getHeader("Referer")) || httpParamBuilder.isAutoReferer()) { // 针对需要Referer的图片，我们使用Get请求

                downloadManager.writeImageToFile(httpManager.createHttpWithGet(url),url);
            } else {
                downloadManager.writeImageToFile(httpManager.createHttpWithPost(url),url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 下载图片
     *
     * @param url 图片地址
     * @return
     */
    public void downloadPicUseRx(String url) {

        Flowable<File> flowable = downloadPicToFlowable(url);

        if (flowable!=null) {

            flowable.subscribe();
        }
    }

    /**
     * 下载图片
     *
     * @param url 图片地址
     * @return
     */
    public Flowable<File> downloadPicToFlowable(final String url) {

        if (repeat==1) {

            return Flowable.create((FlowableEmitter<String> e) -> {

                if (sleepTime>0) {

                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }

                e.onNext(url);

            }, BackpressureStrategy.BUFFER)
                    .map(s->httpManager.createHttpWithPost(s))
                    .map(response->downloadManager.writeImageToFile(response,url));

        } else if (repeat>1) {
            return Flowable.create((FlowableEmitter<String> e) -> {

                for (int i = 0; i < repeat; i++) {

                    if (sleepTime>0) {

                        try {
                            Thread.sleep(sleepTime);
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }
                    }

                    e.onNext(url);
                }

            }, BackpressureStrategy.BUFFER)
                    .map(s->httpManager.createHttpWithPost(s))
                    .observeOn(Schedulers.io())
                    .map(response->downloadManager.writeImageToFile(response,url));
        }

        return null;
    }

    /**
     * 下载多张图片
     * @param urls
     */
    public void downloadPics(List<String> urls) {

        if (Preconditions.isNotBlank(urls)) {
            urls.stream().parallel().forEach(url->{

                try {
                    CompletableFuture.runAsync(() -> downloadPic(url)).get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * 下载整个网页的全部图片
     * @param url
     */
    public void downloadWebPageImages(String url) {

        if (Preconditions.isNotBlank(url)) {

            isWebPage = true;

            pageParser = new PicParser();

            Flowable.just(url)
                    .map(s->httpManager.createHttpWithGet(s))
                    .map(response->parseHtmlToImages(response,(PicParser)pageParser))
                    .subscribe(urls -> downloadPics(urls),
                            throwable-> System.out.println(throwable.getMessage()));
        }
    }

    /**
     * 下载多个网页的全部图片
     * @param urls
     */
    public void downloadWebPageImages(List<String> urls) {

        if (Preconditions.isNotBlank(urls)) {

            isWebPage = true;

            pageParser = new PicParser();

            Flowable.fromIterable(urls)
                    .parallel()
                    .map(url->httpManager.createHttpWithGet(url))
                    .map(response->parseHtmlToImages(response,(PicParser)pageParser))
                    .sequential()
                    .subscribe(list -> downloadPics(list),
                            throwable-> System.out.println(throwable.getMessage()));
        }
    }

    /**
     * 将response进行解析，解析出图片的url，存放到List中
     * @param response
     * @param picParser
     * @return
     */
    private List<String> parseHtmlToImages(CloseableHttpResponse response,PicParser picParser) {

        // 获取响应实体
        HttpEntity entity = response.getEntity();

        InputStream is = null;
        String html = null;

        try {
            is = entity.getContent();
            html = IOUtils.inputStream2String(is);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Document doc = Jsoup.parse(html);

        List<String> urls = picParser.parse(doc);

        if (response != null) {
            try {
                EntityUtils.consume(response.getEntity());
                response.close();
            } catch (IOException e) {
                System.err.println("释放链接错误");
                e.printStackTrace();
            }
        }

        return urls;
    }
 }