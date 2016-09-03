import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Spider {
    /**
     * 标志接口
     */
    private interface SpiderHandler {
    }

    public interface RequestHandler extends SpiderHandler {
        /**
         * 处理请求结果
         *
         * @param responseCode    响应代码
         * @param responseHeaders 响应头
         * @param responseStream  响应正文
         * @return 处理结果标志，对调用者有意义，回调输出结果
         */
        int doHandler(int responseCode, Map<String, List<String>> responseHeaders, InputStream responseStream) throws IOException;
    }

    public interface RegexRequestHandler extends SpiderHandler {
        int doHandler(int responseCode, Map<String, List<String>> responseHeaders, String responseStream, Matcher matcher);
    }

    public interface FlagHandler extends SpiderHandler {
        /**
         * 根据处理标志进行操作
         *
         * @param flagValue 处理标志
         * @return 新的处理标志
         */
        int doHandler(int flagValue);
    }

    private interface InnerRunnable {
        void run0() throws IOException;
    }

    private class InnerThread extends Thread {
        private InnerRunnable runnable;

        public InnerThread(InnerRunnable runnable) {
            this.runnable = runnable;
        }

        public void run() {
            try {
                this.runnable.run0();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private URL host;
    private HttpRequester requester;
    private Map<String, String> requestHeaders;
    private Map<String, String> requestParameters;
    private int requestReturnValue;
    private boolean asynFlag;
    private Proxy proxy;
    private int connectTimeout;
    private int readTimeout;

    public static Spider newHost(URL host) {
        return new Spider(host);
    }

    private Spider(URL host) {
        this.host = host;
        this.requester = requester.newInstance();
        HttpRequester.setThreadCookiesEnabled(true);
        this.asynFlag = false;
        this.proxy = null;
        this.connectTimeout = 10000;
        this.readTimeout = 10000;
    }

    public Spider requestValue(FlagHandler handler) {
        this.requestReturnValue = handler.doHandler(this.requestReturnValue);
        return this;
    }

    public int requestValue() {
        return this.requestReturnValue;
    }

    public Spider setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
        return this;
    }

    public Spider setRequestParameters(Map<String, String> requestParameters) {
        this.requestParameters = requestParameters;
        return this;
    }

    public Spider changeHost(URL host) {
        this.host = host;
        return this;
    }

    /**
     * 下次网络请求将异步返回（仅一次）
     *
     * @return
     */
    public Spider asyn() {
        this.asynFlag = true;
        return this;
    }

    private boolean isAsyn() {
        boolean b = this.asynFlag;
        this.asynFlag = false;
        return b;
    }

    public Spider cleanCookies() {
        this.requester.cleanCurrentCookies();
        return this;
    }

    /**
     * 设置代理服务器，默认为null，null则不使用代理服务器
     *
     * @param proxy
     * @return
     */
    public Spider proxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    /**
     * @return 获得当前代理服务器
     */
    public Proxy proxy() {
        return this.proxy;
    }

    public int readTimeout() {
        return readTimeout;
    }

    public Spider readTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public int connectTimeout() {
        return connectTimeout;
    }

    public Spider connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * do nothing
     */
    public Spider nop() {
        return this;
    }

    public Spider get(RequestHandler handler) throws IOException {
        return this.request("GET", handler);
    }

    public Spider post(RequestHandler handler) throws IOException {
        return this.request("POST", handler);
    }

    private Spider request0(String method, RequestHandler handler) throws IOException {
        HttpRequester.HttpResponse response = this.requester.doRequest(host, method, this.requestParameters, this.requestHeaders, this.connectTimeout, this.readTimeout, this.proxy, true);
        this.requestReturnValue = handler.doHandler(response.getResponseCode(), response.getResponseHeaders(), response.getResponse());
        if (response.getResponse() != null)
            response.getResponse().close();
        return this;
    }

    public Spider request(String method, RequestHandler handler) throws IOException {
        if (this.isAsyn())
            new InnerThread(() -> this.request0(method, handler)).start();
        else
            return this.request0(method, handler);
        return this;
    }

    public Spider regexRequest(String method, String pattern, RegexRequestHandler handler) throws IOException {
        this.request(method, (responseCode, responseHeaders, responseStream) -> {
            Pattern p = Pattern.compile(pattern);
            String response = responseStream.toString();
            Matcher matcher = p.matcher(response);
            return handler.doHandler(responseCode, responseHeaders, response, matcher);
        });
        return this;
    }
}
