import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by forDream on 2016-01-13.<br/>
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
                this.responseHeaders = new HashMap<>();
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

    private static boolean debug = false;

    protected static void log(String pattern, Object... args) {
        if (debug)
            System.out.println(String.format(pattern, args));
    }

    public static synchronized void putTraceInfo(boolean put) {
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
        Map<String, String> map = new HashMap<>();
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
    public static InputStream doGet(URL url, Map<String, String> requestArgs, Map<String, String> requestHeaders) throws IOException {
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
    public static InputStream doPost(URL url, Map<String, String> requestArgs, Map<String, String> requestHeaders) throws IOException {
        InputStream inputStream = doRequest(url, "POST", requestArgs, requestHeaders);
        return inputStream;
    }

    public static InputStream doRequest(URL url, String method, Map<String, String> requestArgs, Map<String, String> requestHeaders) throws IOException {
        return doRequest(url, method, requestArgs, requestHeaders, true).getResponse();
    }

    /**
     * 发起HTTP请求
     *
     * @param url            请求的目标url
     * @param method         请求的方法
     * @param requestArgs    请求的参数
     * @param requestHeaders 请求的头
     * @param autoGzip       自动对GZIP解压缩
     * @return 远程返回的Content，需要手动close
     * @throws IOException
     */
    public static HttpResponse doRequest(URL url, String method, Map<String, String> requestArgs, Map<String, String> requestHeaders, boolean autoGzip) throws IOException {
        method = method.toUpperCase();
        if (requestArgs == null) requestArgs = new HashMap<>();
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

        URLConnection connection = url.openConnection();
        HttpURLConnection httpConnection = (HttpURLConnection) connection;

        log("Request URL -> %s", url.toString());

        if (requestHeaders == null)
            requestHeaders = new HashMap<>();
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

        result.setResponse(httpConnection.getInputStream());
        if (autoGzip) {
            if (httpConnection.getContentEncoding() != null && httpConnection.getContentEncoding().toLowerCase().equals("gzip"))
                result.setResponse(new GZIPInputStream(httpConnection.getInputStream()));
            else
                result.setResponse(httpConnection.getInputStream());
        } else
            result.setResponse(httpConnection.getInputStream());
        return result;
    }
}
