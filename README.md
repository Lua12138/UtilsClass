# UtilsClass
Provide a number of commonly used functions / class of the package.

# Package

## locals
Provide the operating system related classes.

Class|Description
:---:|:---:
CommandLine.java|Execute command line.
DynamicLoader.java|Dynamically compile, load, and execute the Java source file
JdepsHelper.java|Dependency analysis of jdeps based on JDK8
JdepsTestCase.java|JUnit unit test class and guidelines.

## network
Provide network related operations.

Class|Description
:---:|:---:
HttpRequester.java|Does not rely on the third party run library, you can initiate a HTTP request, support for Cookie automatic management and thread isolation.
HttpRequesterTestCase.java|The JUnit test case of HttpRequester
Spider.java|The helper of HttpRequester.java to provide chain programming.

## algorithm
Provide some algorithm.

Class|Description
:---:|:---:
SecurityHelper.java|Support MD2/MD5/SHA-1/SHA-224/SHA-256/SHA-384/SHA-512/AES128 with CBC/ECB.

## convert
Provide convert between diff type

Class|Description
:---:|:---:
StreamConvert.java|convert InputStream/OutputStream, File/String
CommandLineParser.java|Help parse command line arguments

## assistant
Encapsulate some of the commonly used classes.

Class|Description
:---:|:---:
Regex.java|Operation method similar to Stream.
RegexTest.java|JUnit of Regex.java

## kotlin
Some extension method of Kotlin.

Class|Description
:---:|:---:
serialize.kt|Some method of serialize & deserialize.
io.kt|Some method of IO
ChineseIdentityCard.kt|Which is use for verifying that validity of the Chinese identity card
