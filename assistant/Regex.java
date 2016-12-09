import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 正则表达式链式操作辅助类<br>
 * 要求Java 8及以上支持
 */
public class Regex {
    @FunctionalInterface
    /**
     * 不迭代调用匹配结果的接口
     */
    public interface MatcherNonIteratableInterface extends MatcherIteratableInterface {
    }

    @FunctionalInterface
    /**
     * 迭代调用匹配结果的接口
     */
    public interface MatcherIteratableInterface {
        /**
         * @param matcher    匹配的结果，不要调用find方法
         * @param tempResult 连续匹配过程中的中间结果集
         */
        void apply(Matcher matcher, List<String> tempResult);
    }

    @FunctionalInterface
    /**
     * 将不迭代调用的接口转换为迭代调用匹配结果。<br>
     * 应当使用抽象类，为了使用函数式接口，用了default
     */
    public interface MatcherIteratorHelper extends MatcherNonIteratableInterface {
        /**
         * 为了重载，调换参数顺序
         *
         * @param tempResult 连续匹配过程中的中间结果集
         * @param matcher    匹配的结果，不要调用find方法
         */
        void apply(List<String> tempResult, Matcher matcher);

        @Override
        default void apply(Matcher matcher, List<String> tempResult) {
            while (matcher.find())
                this.apply(tempResult, matcher);
        }
    }

    protected Pattern regex;
    protected List<String> innerResult;

    protected Regex(String regex) {
        this(regex, 0);
    }

    protected Regex(String regex, int flag) {
        this.newPattern(regex, flag);
        this.innerResult = new ArrayList<>();
    }

    /**
     * 获得一个新的对象，不应当在实例对象中调用该方法。
     *
     * @param regex 正则表达式
     */
    public static Regex newIns(String regex) {
        return new Regex(regex);
    }

    public static Regex newIns(String regex, int flag) {
        return new Regex(regex, flag);
    }

    /**
     * 修改正则表达式
     *
     * @param regex 正则表达式
     * @return 对象自身
     */
    public Regex newPattern(String regex) {
        return this.newPattern(regex, 0);
    }

    public Regex newPattern(String regex, int flag) {
        this.regex = Pattern.compile(regex, flag);
        return this;
    }

    /**
     * 使用指定的正则表达式匹配结果
     *
     * @param input    待匹配的输入
     * @param callback 处理匹配结果的回调方法
     * @return 对象自身
     */
    public Regex match(CharSequence input, MatcherIteratableInterface callback) {
        Matcher matcher = this.regex.matcher(input);

        if (callback instanceof MatcherNonIteratableInterface) {
            callback.apply(matcher, this.innerResult);
        } else {
            while (matcher.find())
                callback.apply(matcher, this.innerResult);
        }

        return this;
    }

    /**
     * 使用指定的正则表达式匹配保存的中间结果
     *
     * @param callback 处理匹配结果的回调方法
     * @return 对象自身
     */
    public Regex match(MatcherNonIteratableInterface callback) {
        List<String> temp = new ArrayList<>();
        for (String item : this.innerResult) {
            Matcher matcher = this.regex.matcher(item);
            //if (!callback.apply(matcher, temp)) break;
            callback.apply(matcher, temp);
        }

        this.innerResult = temp;
        return this;
    }

    /**
     * 使用指定的正则表达式匹配保存的中间结果，并将匹配全部信息保存回中间结果。
     *
     * @return 对象自身
     */
    public Regex match() {
        List<String> temp = new ArrayList<>();
        for (String item : this.innerResult) {
            Matcher matcher = this.regex.matcher(item);
            while (matcher.find()) temp.add(matcher.group());
        }
        this.innerResult = temp;
        return this;
    }

    /**
     * 清除匹配过程中保存的中间结果
     *
     * @return 对象自身
     */
    public Regex cleanResult() {
        this.innerResult.clear();
        return this;
    }

    /**
     * 根据指定正则表达式切割输入
     *
     * @param input 待切割的输入
     * @return 切割结果的流
     */
    public Stream<String> split(CharSequence input) {
        return this.regex.splitAsStream(input);
    }

    /**
     * @return 获得中间结果的异步流
     */
    public Stream<String> result() {
        return this.innerResult.parallelStream();
    }
}
