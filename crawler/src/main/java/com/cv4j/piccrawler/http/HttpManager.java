package com.cv4j.piccrawler.http;

import com.safframework.tony.common.utils.Preconditions;
import org.apache.http.HttpHost;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;

/**
 * Created by tony on 2017/10/19.
 */
public class HttpManager {

    /**
     * 全局连接池对象
     */
    private static PoolingHttpClientConnectionManager connManager = null;
    private CloseableHttpClient httpClient;
    private HttpParam httpParam;

    /**
     * 配置连接池信息，支持http/https
     */
    static {

        SSLContext sslcontext = null;
        try {
            //获取TLS安全协议上下文
            sslcontext = SSLContext.getInstance("TLS");
            sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[]{};
                }
            }}, null);

            SSLConnectionSocketFactory scsf = new SSLConnectionSocketFactory(sslcontext, NoopHostnameVerifier.INSTANCE);
            RequestConfig defaultConfig = RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD_STRICT)
                    .setExpectContinueEnabled(true)
                    .setTargetPreferredAuthSchemes(Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))
                    .setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC)).build();
            Registry<ConnectionSocketFactory> sfr = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.INSTANCE)
                    .register("https", scsf).build();

            connManager = new PoolingHttpClientConnectionManager(sfr);

            // 设置最大连接数
            connManager.setMaxTotal(200);
            // 设置每个连接的路由数
            connManager.setDefaultMaxPerRoute(20);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
    }

    private HttpManager() {
    }

    public static HttpManager get() {
        return HttpManager.Holder.MANAGER;
    }


    public HttpParam getHttpParam() {
        return httpParam;
    }

    public void setHttpParam(HttpParam httpParam) {
        this.httpParam = httpParam;
    }

    /**
     * 获取Http客户端连接对象
     * @return Http客户端连接对象
     */
    private CloseableHttpClient createHttpClient() {

        return createHttpClient(20000,null,null);
    }

    /**
     * 获取Http客户端连接对象
     * @param timeOut 超时时间
     * @param proxy   代理
     * @param cookie  Cookie
     * @return Http客户端连接对象
     */
    private CloseableHttpClient createHttpClient(int timeOut,HttpHost proxy,BasicClientCookie cookie) {

        // 创建Http请求配置参数
        RequestConfig.Builder builder = RequestConfig.custom()
                // 获取连接超时时间
                .setConnectionRequestTimeout(timeOut)
                // 请求超时时间
                .setConnectTimeout(timeOut)
                // 响应超时时间
                .setSocketTimeout(timeOut)
                .setCookieSpec(CookieSpecs.STANDARD);

        if (proxy!=null) {
            builder.setProxy(proxy);
        }

        RequestConfig requestConfig = builder.build();

        // 创建httpClient
        HttpClientBuilder httpClientBuilder = HttpClients.custom();

        httpClientBuilder
                // 把请求相关的超时信息设置到连接客户端
                .setDefaultRequestConfig(requestConfig)
                // 把请求重试设置到连接客户端
                .setRetryHandler(new RetryHandler())
                // 配置连接池管理对象
                .setConnectionManager(connManager);

        if (cookie!=null) {
            CookieStore cookieStore = new BasicCookieStore();
            cookieStore.addCookie(cookie);
            httpClientBuilder.setDefaultCookieStore(cookieStore);
        }

        return httpClientBuilder.build();
    }


    /**
     * 创建网络请求
     * @param url
     * @return
     */
    public CloseableHttpResponse createHttpWithPost(String url) {

        // 获取客户端连接对象
        CloseableHttpClient httpClient = getHttpClient();
        // 创建Post请求对象
        HttpPost httpPost = new HttpPost(url);

        if (Preconditions.isNotBlank(httpParam)) {

            Map<String,String> header = httpParam.getHeader();

            if (Preconditions.isNotBlank(header)) {
                for (String key : header.keySet()) {
                    httpPost.setHeader(key,header.get(key));
                }
            }
        }

        CloseableHttpResponse response = null;

        // 执行请求
        try {
            response = httpClient.execute(httpPost);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    public CloseableHttpResponse createHttpWithGet(String url) {

        // 获取客户端连接对象
        CloseableHttpClient httpClient = getHttpClient();
        // 创建Get请求对象
        HttpGet httpGet = new HttpGet(url);

        if (Preconditions.isNotBlank(httpParam)) {

            Map<String,String> header = httpParam.getHeader();

            if (Preconditions.isNotBlank(header)) {
                for (String key : header.keySet()) {
                    httpGet.setHeader(key,header.get(key));
                }
            }
        }

        CloseableHttpResponse response = null;

        // 执行请求
        try {
            response = httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return response;
    }

    /**
     * 检测代理是否可用
     * @param proxy
     * @return
     */
    private boolean checkProxy(HttpHost proxy) {

        if (proxy == null) return false;

        // 创建Http请求配置参数
        RequestConfig.Builder builder = RequestConfig.custom()
                // 获取连接超时时间
                .setConnectionRequestTimeout(20000)
                // 请求超时时间
                .setConnectTimeout(20000)
                // 响应超时时间
                .setSocketTimeout(20000)
                .setProxy(proxy);

        RequestConfig requestConfig = builder.build();

        // 创建httpClient
        HttpClientBuilder httpClientBuilder = HttpClients.custom();

        httpClientBuilder
                // 把请求相关的超时信息设置到连接客户端
                .setDefaultRequestConfig(requestConfig)
                // 配置连接池管理对象
                .setConnectionManager(connManager);

        CloseableHttpClient client =  httpClientBuilder.build();

        HttpClientContext httpClientContext = HttpClientContext.create();
        CloseableHttpResponse response = null;
        try {
            HttpGet request = new HttpGet("http://www.163.com/");
            response = client.execute(request, httpClientContext);

            int statusCode = response.getStatusLine().getStatusCode();// 连接代码

            if (statusCode == 200) {

                return true;
            }
        } catch (IOException e) {
//            e.printStackTrace();
            return false;
        }

        return false;
    }

    private CloseableHttpClient getHttpClient() {

        if (httpClient!=null) return httpClient;

        if (Preconditions.isNotBlank(httpParam)) {

            int timeOut = httpParam.getTimeOut();
            HttpHost proxy = httpParam.getProxy();
            BasicClientCookie cookie = httpParam.getCookie();

            if (proxy!=null) {
                boolean check = checkProxy(proxy);
                if (check) {
                    httpClient = createHttpClient(timeOut,proxy,cookie);
                } else {
                    httpClient = createHttpClient(timeOut,null,cookie);
                }
            } else {
                httpClient = createHttpClient(timeOut,null,cookie);
            }
        } else {
            httpClient = createHttpClient();
        }

        return httpClient;
    }

    private static class Holder {
        private static final HttpManager MANAGER = new HttpManager();
    }
}
