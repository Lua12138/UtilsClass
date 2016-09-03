import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by forDream on 2016-01-13.<br/>
 * Last edit on 2016-09-03 <br/>
 * Need JDK >= 1.6 <br/>
 * 使用HttpURLConnection包装了常用的GET/POST请求，不依赖第三方库
 */
public class HttpRequester {
    public static class HttpResponse {
        private InputStream response; // 响应正文
        private int responseCode; // 响应代码
        private Map<String, List<String>> responseHeaders; // 响应头

        public HttpResponse() {
            this(true);
        }

        /**
         * @param autoInit 是否自动初始化复合对象
         */
        public HttpResponse(boolean autoInit) {
            if (autoInit) {
                this.responseHeaders = new HashMap();
            }
        }

        public InputStream getResponse() {
            return response;
        }

        public void setResponse(InputStream response) {
            this.response = response;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public void setResponseCode(int responseCode) {
            this.responseCode = responseCode;
        }

        public Map<String, List<String>> getResponseHeaders() {
            return responseHeaders;
        }

        public void setResponseHeaders(Map<String, List<String>> responseHeaders) {
            this.responseHeaders = responseHeaders;
        }
    }

    public static class HttpInputStream extends java.io.InputStream {
        private java.io.InputStream is;
        private String toString;

        private HttpInputStream(java.io.InputStream is) {
            this.is = is;
            this.toString = null;
        }

        @Override
        public int read() throws IOException {
            return this.is.read();
        }

        @Override
        public String toString() {
            if (this.toString == null) {
                StringBuilder builder = new StringBuilder();
                byte[] bytes = new byte[4096];
                int len;
                try {
                    while (-1 != (len = this.is.read(bytes, 0, bytes.length)))
                        builder.append(new String(bytes, 0, len));
                    this.toString = builder.toString();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return this.toString;
        }
    }

    public static class MyAsynCookieManager extends CookieManager {
        private static MyAsynCookieManager cookieManager;
        private Map<Thread, CookieManager> cookies;

        private MyAsynCookieManager() {
            this.cookies = new HashMap<Thread, CookieManager>();
        }

        public CookieManager currentThreadCookieManager() {
            CookieManager cookieManager = this.cookies.get(Thread.currentThread());
            if (cookieManager == null) {
                cookieManager = new CookieManager();
                this.cookies.put(Thread.currentThread(), cookieManager);
            }
            return cookieManager;
        }

        public void cleanCurrentThreadCookieManager() {
            this.cookies.put(Thread.currentThread(), null);
        }

        public void cleanAllCookieManager() {
            this.cookies.clear();
        }

        public static MyAsynCookieManager getManager() {
            if (cookieManager == null)
                synchronized (MyAsynCookieManager.class) {
                    if (cookieManager == null) cookieManager = new MyAsynCookieManager();
                }

            return cookieManager;
        }

        @Override
        public Map<String, List<String>> get(URI uri, Map<String, List<String>> requestHeaders) throws IOException {
            return this.currentThreadCookieManager().get(uri, requestHeaders);
        }

        @Override
        public void put(URI uri, Map<String, List<String>> responseHeaders) throws IOException {
            this.currentThreadCookieManager().put(uri, responseHeaders);
        }
    }

    private static HttpRequester singleton;

    static {
        singleton = new HttpRequester();
    }

    public static HttpRequester getSingleton() {
        return singleton;
    }

    /**
     * default is no Cookies Support.
     *
     * @return
     */
    public static HttpRequester newInstance() {
        return new HttpRequester();
    }

    /**
     * Global setting
     *
     * @param bool
     */
    public static void setThreadCookiesEnabled(boolean bool) {
        CookieHandler.setDefault(bool ? MyAsynCookieManager.getManager() : null);
    }

    public void cleanCurrentCookies() {
        MyAsynCookieManager.getManager().cleanCurrentThreadCookieManager();
    }

    public void cleanAllCookies() {
        MyAsynCookieManager.getManager().cleanAllCookieManager();
    }

    private HttpRequester() {
    }

    private boolean debug = false;

    protected void log(String pattern, Object... args) {
        if (debug)
            System.out.println(String.format(pattern, args));
    }

    public synchronized void putTraceInfo(boolean put) {
        debug = put;
    }

    /**
     * 键值对转换为URL请求字符串
     *
     * @param args {"key1":"value1","key2":"value2"}
     * @return key1=value1&key2=value2&
     */
    private static String mapToQueryString(Map<String, String> args) throws UnsupportedEncodingException {
        StringBuilder builder = new StringBuilder();
        for (Iterator<String> iterator = args.keySet().iterator(); iterator.hasNext(); ) {
            String key = URLEncoder.encode(iterator.next(), "utf-8");
            String value = URLEncoder.encode(args.get(key), "utf-8");
            builder.append(key).append("=").append(value).append("&");
        }
        return builder.toString();
    }

    /**
     * 将查询字符串转换为键值对
     *
     * @param queryString
     * @return
     */
    private static Map<String, String> queryStringToMap(String queryString) {
        Map<String, String> map = new HashMap();
        String[] queries = queryString.split("&");
        for (String query : queries) {
            int offset = query.indexOf('=');
            if (offset == -1) continue;
            if (offset + 1 > query.length()) continue;

            String key = query.substring(0, offset);
            String value = query.substring(offset + 1);
            map.put(key, value);
        }
        return map;
    }

    /**
     * 发起GET请求
     *
     * @param url            请求的url
     * @param requestArgs    请求的参数，可以直接加在url中，也可以设置此参数，建议直接放到url
     * @param requestHeaders 请求头
     * @return 服务器返回的输入流
     * @throws IOException
     */
    public InputStream doGet(URL url, Map<String, String> requestArgs, Map<String, String> requestHeaders) throws IOException {
        InputStream inputStream = doRequest(url, "GET", requestArgs, requestHeaders);
        return inputStream;
    }

    /**
     * 发起POST请求
     *
     * @param url            请求url
     * @param requestArgs    请求参数，不要把参数放到url中
     * @param requestHeaders 请求头
     * @return 服务器返回的输入流
     * @throws IOException
     */
    public InputStream doPost(URL url, Map<String, String> requestArgs, Map<String, String> requestHeaders) throws IOException {
        InputStream inputStream = doRequest(url, "POST", requestArgs, requestHeaders);
        return inputStream;
    }

    /**
     * 使用默认的设置，无代理，10秒连接和读取超时
     *
     * @throws IOException
     */
    public InputStream doRequest(URL url, String method, Map<String, String> requestArgs, Map<String, String> requestHeaders) throws IOException {
        return doRequest(url, method, requestArgs, requestHeaders, 10000, 10000, null, true).getResponse();
    }

    /**
     * 发起HTTP请求
     *
     * @param url            请求的目标url
     * @param method         请求的方法
     * @param requestArgs    请求的参数
     * @param requestHeaders 请求的头
     * @param connectTimeout 连接超时
     * @param readTimeout    读取超时
     * @param proxy          代理服务器
     * @param autoGzip       自动对GZIP解压缩
     * @return 远程返回的Content，需要手动close
     * @throws IOException
     */
    public HttpResponse doRequest(URL url, String method, Map<String, String> requestArgs, Map<String, String> requestHeaders, int connectTimeout, int readTimeout, Proxy proxy, boolean autoGzip) throws IOException {
        method = method.toUpperCase();
        if (requestArgs == null) requestArgs = new HashMap();
        String queryString = mapToQueryString(requestArgs);
        // add args to uri
        if (!method.matches("(?:POST|PUT)")) {
            String strUrl = url.toString();
            if (strUrl.indexOf('?') == -1)
                if (strUrl.endsWith("/"))
                    strUrl += "/?";
                else
                    strUrl += "?";
            log("Request Query String -> %s", queryString);
            url = new URL(strUrl + queryString); // build new url
            queryString = null;
        }

        URLConnection connection;
        if (proxy == null)
            connection = url.openConnection();
        else
            connection = url.openConnection(proxy);

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        HttpURLConnection httpConnection = (HttpURLConnection) connection;

        log("Request URL -> %s", url.toString());

        if (requestHeaders == null)
            requestHeaders = new HashMap();
        requestHeaders.put("X-Connector", "forDream");

        // set http request headers
        for (Iterator<String> iterator = requestHeaders.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            String value = requestHeaders.get(key);
            httpConnection.setRequestProperty(key, value);
        }
        // set request method
        httpConnection.setRequestMethod(method);

        // set request data
        if (queryString != null) {
            httpConnection.setDoOutput(true);
            OutputStream outputStream = httpConnection.getOutputStream();
            outputStream.write(queryString.getBytes());
            outputStream.close();
        }

        httpConnection.connect(); // connect to remote

        HttpResponse result = new HttpResponse(false);

        result.setResponseHeaders(httpConnection.getHeaderFields());

        int responseCode = httpConnection.getResponseCode();

        result.setResponseCode(responseCode);
        log("Request Method -> %s", httpConnection.getRequestMethod());
        log("Response Code -> %d", responseCode);

        if (responseCode == 200) {
            result.setResponse(httpConnection.getInputStream());
            if (autoGzip) {
                if (httpConnection.getContentEncoding() != null && httpConnection.getContentEncoding().toLowerCase().equals("gzip"))
                    result.setResponse(new HttpInputStream(new GZIPInputStream(httpConnection.getInputStream())));
                else
                    result.setResponse(new HttpInputStream(httpConnection.getInputStream()));
            } else
                result.setResponse(httpConnection.getInputStream());
        }
        return result;
    }
}
