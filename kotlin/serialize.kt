/**
 * Serialize an object to a string
 *
 * **Depend on Gson**
 */
fun Any.toJsonString(): String =
        Gson().toJson(this)

/**
 * Convert a string into an object
 *
 * **Depend on Gson**
 *
 * ## How to Deserialize a generic object (only for **Gson**)
 *
 * Suppose you want to deserialize `List<String>`
 *
 * Kotlin Code :
 *
 * `"Your Json String".toJsonObject(object : TypeToken<List<String>>() {}.type)`
 *
 * <del>
 * Java Code :
 *
 * `new TypeToken<List<String>>() {}.getType();`
 * </del>
 *
 * @param targetClass Target type of conversion
 * @return Target type of conversion
 */
fun <T> String.toJsonObject(targetClass: Class<out T>): T =
        Gson().fromJson<T>(this, targetClass)

fun <T> String.toJsonObject(targetType: Class<out T>, exclusionStrategy: ExclusionStrategy): T =
        GsonBuilder().setExclusionStrategies(exclusionStrategy)
                .create()
                .fromJson(this, targetType)

fun <T> String.toJsonGenericsObject() =
        Gson().fromJson<T>(this, object : TypeToken<T>() {}.type)
