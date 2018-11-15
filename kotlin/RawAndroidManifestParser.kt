import org.dom4j.Document
import org.dom4j.Node
import org.dom4j.io.SAXReader
import java.io.File
import java.util.zip.ZipFile

/**
 *  Dependency:
 *  implementation 'net.dongliu:apk-parser:2.6.2'
 *  implementation 'org.dom4j:dom4j:2.1.1'
 */
class RawAndroidManifestParser {
    enum class AndroidComponentType(name: String) {
        Application("application"),
        Activity("activity"),
        ContentProvider("provider"),
        Service("service"),
        BroadcastReceiver("receiver");

        val description: String = name

    }

    data class AndroidComponent(
            val name: String,
            val exported: Boolean,
            val intentFilters: List<IntentFilter>,
            val configChanges: String?,
            val launchMode: String?,
            val process: String?,
            val screenOrientation: String?,
            val taskAffinity: String?,
            val theme: String?,
            val label: String?
    ) {
        data class IntentFilter(
                val priority: Int,
                val actions: List<String>,
                val category: List<String>
        )


        companion object {
            /**
             * 序列化一个组件
             */
            fun parse(componentNode: Node): AndroidComponent {
                val name = componentNode.selectNodes("attribute::android:name").firstOrNull()?.text!!
                val exported: Boolean = componentNode.selectNodes("attribute::android:exported").firstOrNull()?.text?.toBoolean()
                        ?: false
                val configChanges = componentNode.selectNodes("attribute::android:configChanges").firstOrNull()?.text
                val launchMode = componentNode.selectNodes("attribute::android:launchMode").firstOrNull()?.text
                val process = componentNode.selectNodes("attribute::android:process").firstOrNull()?.text
                val screenOrientation = componentNode.selectNodes("attribute::android:screenOrientation").firstOrNull()?.text
                val taskAffinity = componentNode.selectNodes("attribute::android:taskAffinity").firstOrNull()?.text
                val theme = componentNode.selectNodes("attribute::android:theme").firstOrNull()?.text
                val authorities = componentNode.selectNodes("attribute::android:authorities").firstOrNull()?.text
                val label = componentNode.selectNodes("attribute::android:label").firstOrNull()?.text

                val intentFilters = mutableListOf<IntentFilter>()

                componentNode.selectNodes("./intent-filter").forEach { filter ->
                    val action = mutableListOf<String>()
                    val category = mutableListOf<String>()
                    val priority: Int = filter.selectNodes("attribute::android:priority").firstOrNull()?.text?.toInt()
                            ?: 0

                    filter.selectNodes("./action/attribute::android:name").forEach { a -> action.add(a.text) }
                    filter.selectNodes("./category/attribute::android:name").forEach { c -> category.add(c.text) }

                    intentFilters.add(IntentFilter(priority, action, category))
                }

                return AndroidComponent(name, exported, intentFilters, configChanges, launchMode, process, screenOrientation, taskAffinity, theme, label)
            }

            /**
             * 序列化一个文档中一类组件
             */
            fun parse(rootDocument: Document, type: AndroidComponentType): List<AndroidComponent> {
                val lists = mutableListOf<AndroidComponent>()

                return when (type) {
                    AndroidComponentType.Activity,
                    AndroidComponentType.BroadcastReceiver,
                    AndroidComponentType.Service,
                    AndroidComponentType.ContentProvider -> {
                        rootDocument.selectNodes("/manifest/application/${type.description}")
                    }
                    AndroidComponentType.Application -> {
                        rootDocument.selectNodes("/manifest/application")
                    }
                }.let { components ->
                    components.forEach { lists.add(AndroidComponent.parse(it)) }
                    lists
                }
            }
        }
    }

    val activities: List<AndroidComponent>
    val contentProviders: List<AndroidComponent>
    val services: List<AndroidComponent>
    val broadcastReceiver: List<AndroidComponent>
    val application: AndroidComponent
    val packageName: String
    val versionCode: Int
    val versionName: String?
    val minSdkVersion: Int?
    val targetSdkVersion: Int?

    /**
     * 从AndroidManifest.xml创建
     * @param androidManifestXml AndroidManifest.xml文件
     */
    constructor(androidManifestXml: File) {
        SAXReader().read(androidManifestXml).let {
            this.activities = AndroidComponent.parse(it, AndroidComponentType.Activity)
            this.contentProviders = AndroidComponent.parse(it, AndroidComponentType.ContentProvider)
            this.services = AndroidComponent.parse(it, AndroidComponentType.Service)
            this.broadcastReceiver = AndroidComponent.parse(it, AndroidComponentType.BroadcastReceiver)

            this.application = AndroidComponent.parse(it, AndroidComponentType.Application)[0]

            this.packageName = it.selectNodes("/*/attribute::package").first().text
            this.versionCode = it.selectNodes("/*/attribute::android:versionCode").first().text.toInt()
            this.versionName = it.selectNodes("/*/attribute::android:versionName")?.firstOrNull()?.text
            this.minSdkVersion = it.selectNodes("/manifest/uses-sdk/attribute::android:minSdkVersion").firstOrNull()?.text?.toInt()
            this.targetSdkVersion = it.selectNodes("/manifest/uses-sdk/attribute::android:targetSdkVersion").firstOrNull()?.text?.toInt()
        }
    }

    /**
     * 从AAR创建
     * @param androidResourcePackage AAR文件
     */
    constructor(androidResourcePackage: ZipFile) {
        val f = File.createTempFile("manifest", ".xml")
        f.deleteOnExit()
        androidResourcePackage.getInputStream(androidResourcePackage.getEntry("AndroidManifest.xml"))
                .use {
                    f.writeBytes(it.readBytes())
                }

        SAXReader().read(f).let {
            this.activities = AndroidComponent.parse(it, AndroidComponentType.Activity)
            this.contentProviders = AndroidComponent.parse(it, AndroidComponentType.ContentProvider)
            this.services = AndroidComponent.parse(it, AndroidComponentType.Service)
            this.broadcastReceiver = AndroidComponent.parse(it, AndroidComponentType.BroadcastReceiver)

            this.application = AndroidComponent.parse(it, AndroidComponentType.Application)[0]

            this.packageName = it.selectNodes("/*/attribute::package").first().text
            this.versionCode = it.selectNodes("/*/attribute::android:versionCode").first().text.toInt()
            this.versionName = it.selectNodes("/*/attribute::android:versionName")?.firstOrNull()?.text
            this.minSdkVersion = it.selectNodes("/manifest/uses-sdk/attribute::android:minSdkVersion").firstOrNull()?.text?.toInt()
            this.targetSdkVersion = it.selectNodes("/manifest/uses-sdk/attribute::android:targetSdkVersion").firstOrNull()?.text?.toInt()

        }
    }
}
