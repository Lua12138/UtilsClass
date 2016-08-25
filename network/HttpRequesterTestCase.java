import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * Created by forDream on 2016-08-25.
 */
public class HttpRequesterTestCase {
    public static class ExecThread extends Thread {
        //private String threadName;
        private int testCaseCount;
        private Random random;

        private String randomString(int length) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < length; i++) {
                char c = (char) random.nextInt(26);
                c += 'a';
                builder.append(c);
            }
            return builder.toString();
        }

        private String threadName() {
            return this.getName() + "-";
        }

        public ExecThread(int testCaseCount) {
            System.out.println(this.getName());
            this.testCaseCount = testCaseCount;
            this.random = new Random();
        }

        @Override
        public void run() {
            int count = this.testCaseCount;
            HttpRequester requester = HttpRequester.newInstance();
            Map<String, String> cookie = new HashMap();
            try {
                while (count-- > 0) {
                    String cookieName = this.threadName() + this.randomString(5);
                    String cookieValue = this.randomString(10);
                    cookie.put(cookieName, cookieValue);
                    HttpRequesterTestCase.setCookie(requester, cookieName, cookieValue);
                }
                String cookieReturn = HttpRequesterTestCase.getCookie(requester);
                // check cookie result
                for (Iterator<String> iterator = cookie.keySet().iterator(); iterator.hasNext(); ) {
                    String cookieName = iterator.next();
                    cookieReturn = cookieReturn.replace(cookieName, "REPLACE_FLAG");
                }

                Assert.assertTrue(cookieReturn.indexOf("Thread-") == -1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void setCookie(HttpRequester requester, String cookieName, String cookieValue) throws IOException {
        requester.doGet(new URL("http://httpbin.org/cookies/set?" + cookieName + "=" + cookieValue), null, null);
    }

    private static String getCookie(HttpRequester requester) throws IOException {
        return requester.doGet(new URL("http://httpbin.org/cookies"), null, null).toString();
    }

    private Random random;

    private String randomString(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) random.nextInt(26);
            c += 'a';
            builder.append(c);
        }
        return builder.toString();
    }

    private void checkCookieResult(String cookieRetun, Map<String, String> requestCookie, boolean expected) {
        for (Iterator<String> iterator = requestCookie.keySet().iterator(); iterator.hasNext(); ) {
            String key = iterator.next();
            String value = requestCookie.get(key);
            Assert.assertEquals(expected, cookieRetun.indexOf(key) > 0);
            Assert.assertEquals(expected, cookieRetun.indexOf(value) > 0);
        }
    }

    private void invoked(boolean expected) throws IOException {
        HttpRequester requester = HttpRequester.newInstance();
        final int randomCookieCount = 3;
        Map<String, String> cookie = new HashMap();
        int count = randomCookieCount;
        while (count-- > 0) {
            String testCookieKey = "TestCookieKey_" + this.randomString(5);
            String testCookieValue = "TestCookieValue_" + this.randomString(5);

            cookie.put(testCookieKey, testCookieValue);
            HttpRequesterTestCase.setCookie(requester, testCookieKey, testCookieValue);
        }

        String cookieResult = HttpRequesterTestCase.getCookie(requester);

        this.checkCookieResult(cookieResult, cookie, expected);
    }

    @Before
    public void initEnv() {
        this.random = new Random();
    }

    @Test
    public void SyncInvokeWithCookies() throws IOException {
        HttpRequester.setThreadCookiesEnabled(true);
        this.invoked(true);
    }

    @Test
    public void AsynInvokeWithCookies() throws InterruptedException {
        final int threadCount = 3;
        final int everyThreadExecCount = 3;
        HttpRequester.setThreadCookiesEnabled(true);
        int c1 = threadCount;
        Thread[] threads = new Thread[c1];
        while (c1-- > 0) {
            threads[c1] = new ExecThread(everyThreadExecCount);
            threads[c1].start();
        }

        for (Thread thread : threads)
            while (thread.getState() != Thread.State.TERMINATED) Thread.sleep(50);
    }

    @Test
    public void SyncInvokeWithoutCookies() throws IOException {
        HttpRequester.setThreadCookiesEnabled(false);
        this.invoked(false);
    }
}
