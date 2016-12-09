import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;

import static org.junit.Assert.*;

public class RegexTest {
    protected final String input = "k1=v1\n" +
            "k2=v2\n" +
            "k3=v3:d3\n" +
            "k4=v4:d4\n" +
            "k5=v5";

    protected final String keyValuePattern = "(\\S+)=(\\S+)(?:\\s|$)";

    @Before
    public void init() {

    }

    @Test
    public void newPattern() throws Exception {
        Assert.assertEquals(2,
                Regex.newIns(keyValuePattern)
                        .match(input, (matcher, tempResult) -> tempResult.add(matcher.group(2)))
                        .newPattern(":") // <- newPattern
                        .match()
                        .result().count()
        );
    }

    @Test
    public void match() throws Exception {
        Assert.assertEquals(2,
                Regex.newIns(keyValuePattern)
                        .match(input, (m, t) -> t.add(m.group(2)))
                        .newPattern("(\\S+):(\\S+)")
                        .match()
                        .newPattern(":")
                        .match((Regex.MatcherIteratorHelper) (t, m) -> t.add(m.group()))
                        .result()
                        .count()
        );
    }

    @Test
    public void cleanResult() throws Exception {
        Assert.assertEquals(0,
                Regex.newIns(keyValuePattern)
                        .match(input, (m, t) -> t.add(m.group()))
                        .cleanResult()
                        .result()
                        .count()
        );
    }

    @Test
    public void split() throws Exception {
        Assert.assertEquals(5,
                Regex.newIns("\n")
                        .split(input)
                        .count()
        );
    }

    @Test
    public void result() throws Exception {
        Assert.assertEquals(5,
                Regex.newIns(keyValuePattern)
                        .match(input, (m, t) -> t.add(m.group(2)))
                        .result()
                        .peek(System.err::println)
                        .count()
        );
    }

}
