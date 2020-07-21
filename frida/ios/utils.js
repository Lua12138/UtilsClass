    function eachNSDictionary(obj, callback) {
        var dict = new ObjC.Object(obj);
        var enumerator = dict.keyEnumerator();
        var key;
        while ((key = enumerator.nextObject()) !== null) {
            var value = dict.objectForKey_(key);
            callback(key, value)
        }
    }

    function eachNSArray(obj, callback) {
        var array = new ObjC.Object(obj);
        var count = array.count().valueOf();
        for (var i = 0; i !== count; i++) {
            var element = array.objectAtIndex_(i);
            callback(element, i)
        }
    }

    function convertNSDataToString(obj) {
        var objcData = ObjC.Object(obj);  //NSData
        var strBody = objcData.bytes().readUtf8String(objcData.length()); //NSData to string
        return strBody
    }

    function getClassName(obj) {
        return new ObjC.Object(obj).$className
    }

    function convertNSUrlToString(obj) {
        return new ObjC.Object(obj).absoluteString().toString()
    }

    /* NSData -> NSString */
    function convertNSData2NSString(nsdata) {
        return ObjC.classes.NSString.alloc().initWithData_encoding_(nsdata, 4);
    }

    function convertJavascriptString2NSString(str) {
        return ObjC.classes.NSString.stringWithString_(str)
    }

    function convertNSString2JavascriptString(str) {
        return new ObjC.Object(str).UTF8String();
    }

    function printTraceStack() {
        // just call [NSThread callStackSymbols]
        var threadClass = ObjC.classes.NSThread
        var symbols = threadClass["+ callStackSymbols"]()
        console.log('trace:', symbols)
    }

    function findMethod(methods, name) {
        for (var n in methods) {
            var method = methods[n]
            if (method.indexOf(name) > 0) {
                console.log('Method is:', method)
            }
        }
    }

    function callNewInstanceMethod(className, methodName, ...args) {
        var clazz = ObjC.classes[className]
        var instance = clazz.alloc()
        return callInstanceMethod(className, instance, methodName, ...args)
    }

    function callInstanceMethod(className, instance, methodName, ...args) {
        var clazz = ObjC.classes[className]
        var method = instance[methodName]
        console.log('invoke class:', clazz)
        console.log('invoke instance:', instance)
        console.log('invoke method:', method)
        return method.call(instance, ...args)
    }

    function callStaticMethod(className, methodName, ...args) {
        var clazz = ObjC.classes[className]
        var method = clazz[methodName]
        console.log('invoke class:', clazz)
        console.log('invoke method:', method)
        return method.call(clazz, ...args)
    }

    function convertJavascriptObject2NSDictionary(obj) {
        const dictionary = ObjC.classes.NSMutableDictionary.alloc().init()
        for (let key in obj) {
            dictionary.setObject_forKey_(obj[key], key)
        }
        return dictionary
    }
