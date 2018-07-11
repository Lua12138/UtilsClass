/**
 * create an instance of the `Class`
 * @param argsTypes the type lists for constructor, put empty array if there is no params
 * @param argsValues the value lists for constructor, put empty array if there is no params
 * @return return the instance of `Class`
 */
fun <T> Class<T>.newInstance(argsTypes: Array<Class<out Any>>, argsValues: Array<out Any>): T {
    val constructor = this.getConstructor(*argsTypes)
    return constructor.newInstance(*argsValues)
}

/**
 * invoke method
 *
 * T is the ClassType
 * R is the method returns type
 * @param invoker the method of instance, null for static method
 * @param methodName the name of method which you want to invoke
 * @param argsTypes the type lists for method, put empty array if there is no params
 * @param argsValues the value lists for method, put empty array if there is no params
 * @return return value for the method
 */
fun <T, R> Class<T>.invokeMethod(invoker: T? = null, methodName: String, argsTypes: Array<Class<out Any>>, argsValues: Array<out Any>): R {
    val method = this.getMethod(methodName, *argsTypes)
    method.isAccessible = true
    return method.invoke(invoker, argsValues) as R
}

/**
 * get field value
 * T is the ClassType
 * R is the method returns type
 * @param invoker the field of instance, null for static field
 * @param fieldName the name of field
 * @return field value
 */
fun <T, R> Class<T>.queryField(invoker: T? = null, fieldName: String): R {
    val field = this.getField(fieldName)
    field.isAccessible = true
    return field.get(invoker) as R
}
