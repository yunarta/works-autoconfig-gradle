package com.mobilesolutionworks.android.autoconfig

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by yunarta on 15/12/15.
 */
public class AutoConfigPlugin implements Plugin<Project> {

    boolean autoConfigure

    int dictateCompileSdk
    String autoCompileSdk

    String dictateAutoBuildTools
    String autoBuildTools

    String dictateAutoRepositoryRevision
    String autoRepositoryRevision

    @Override
    void apply(Project project) {
        def tmpDir = System.getenv('TMPDIR')
        if (tmpDir == null) {
            def lp = project.rootProject.file('local.properties')
            Properties properties = new Properties()

            def is = lp.newDataInputStream()
            properties.load(is)
            is.close()

            if (properties.containsKey("tmpDir")) {
                tmpDir = properties["tmpDir"]
            } else {
                println 'no env TMPDIR found, you can set a temporary dir in local.properties with key \'tmpDir\''
                throw new IllegalStateException("tmp dir is required")
            }
        }

        def writable = new File(tmpDir).canWrite()

        def repositoryXml = new File(tmpDir + '/auto-configure.repository.xml')
        def addOnXml = new File(tmpDir + '/auto-configure.addon.xml')

        def downloadRepository = !repositoryXml.exists() || repositoryXml.lastModified() + (24 * 60 * 60) < System.currentTimeMillis()
        def downloadAddOn = !addOnXml.exists() || addOnXml.lastModified() + (24 * 60 * 60) < System.currentTimeMillis()

        def repository = null
        def addOn = null

        autoConfigure = true
        dictateCompileSdk = -1
        dictateAutoBuildTools
        dictateAutoRepositoryRevision

        def lp = new File(project.rootProject.projectDir, 'local.properties')
        if (lp.exists()) {
            Properties properties = new Properties()

            def is = lp.newDataInputStream()
            properties.load(is)
            is.close()

            if (properties['autoConfigure.enable'] != null) {
                autoConfigure = Boolean.valueOf(properties['autoConfigure.enable']).booleanValue()
            }

            if (properties['autoConfigure.forceCompileSdk'] != null) {
                dictateCompileSdk = Integer.parseInt(properties['autoConfigure.forceCompileSdk']).intValue()
            } else {
                dictateCompileSdk = -1;
            }

            if (properties['autoConfigure.forceBuildTools'] != null) {
                dictateAutoBuildTools = properties['autoConfigure.forceBuildTools']
            } else {
                dictateAutoBuildTools = null
            }


            if (properties['autoConfigure.forceRepositoryRevision'] != null) {
                dictateAutoRepositoryRevision = properties['autoConfigure.forceRepositoryRevision']
            } else {
                dictateAutoRepositoryRevision = null
            }
        }

        if (autoConfigure && !project.gradle.startParameter.offline) {
            if (downloadRepository && writable) {
                new URL('https://dl.google.com/android/repository/repository-11.xml').withInputStream {
                    i -> repositoryXml.withOutputStream { it << i }
                }
                repository = new XmlParser().parse(repositoryXml)
            } else {
                repository = new XmlParser().parse('https://dl.google.com/android/repository/repository-11.xml')
            }

            if (downloadAddOn && writable) {
                new URL('https://dl.google.com/android/repository/addon.xml').withInputStream {
                    i -> addOnXml.withOutputStream { it << i }
                }
                addOn = new XmlParser().parse(addOnXml)
            } else {
                addOn = new XmlParser().parse('https://dl.google.com/android/repository/addon.xml')
            }
        }

//        def autoBuildTools, autoCompileSdk, autoRepositoryRevision

        println 'auto configure is ' + (autoConfigure ? 'enabled' : 'disabled')

        if (autoConfigure) {
            if (repository != null && addOn != null) {
                def highestApiLevel = 0
                for (apiLevel in repository.'sdk:platform'.'sdk:api-level') {
                    highestApiLevel = Math.max(highestApiLevel, Integer.parseInt(apiLevel.text()))
                }

                autoCompileSdk = String.valueOf(highestApiLevel)

                def buildToolsList = []
                for (buildTools in repository.'sdk:build-tool'.'sdk:revision') {
                    def revision = buildTools.'sdk:major'.text() + '.' + buildTools.'sdk:minor'.text() + '.' + buildTools.'sdk:micro'.text()
                    buildToolsList.add(revision)
                }

                autoBuildTools = mostRecentVersion(buildToolsList)
                for (extra in addOn.'sdk:extra') {
                    if ('support'.equals(extra.'sdk:path'.text())) {
                        def revision = extra.'sdk:revision'
                        autoRepositoryRevision = revision.'sdk:major'.text() + '.' + revision.'sdk:minor'.text() + '.' + revision.'sdk:micro'.text()
                    }
                }
            } else {
                // get android home
                def androidHome
                def localProperties = project.rootProject.file('local.properties')
                if (localProperties.exists()) {
                    Properties properties = new Properties()

                    def is = localProperties.newDataInputStream()
                    properties.load(is)
                    is.close();

                    androidHome = properties['sdk.dir']
                } else {
                    def env = System.getenv()
                    androidHome = env['ANDROID_HOME']
                }

                def buildToolsList = []
                new File(androidHome + '/build-tools').eachFile {
                    file -> buildToolsList.add(file.name)
                }

                autoBuildTools = mostRecentVersion(buildToolsList)

                def platformDirs = []
                new File(androidHome + '/platforms').eachFile {
                    file -> platformDirs.add(file.name)
                }

                platformDirs.sort()
                String platformDir = platformDirs.last()
                autoCompileSdk = platformDir.substring(platformDir.lastIndexOf('-') + 1)

                def supportDirs = []
                new File(androidHome + '/extras/android/m2repository/com/android/support/support-v13').eachFile {
                    file ->
                        if (file.isDirectory()) {
                            supportDirs.add(file.name)
                        }
                }

                autoRepositoryRevision = mostRecentVersion(supportDirs)
            }

            println '  auto sdk to android-' + autoCompileSdk
            println '  auto build tools to revision ' + autoBuildTools
            println '  auto repository to revision ' + autoRepositoryRevision
        } else {
            if (dictateCompileSdk != -1) {
                println '  forcing compile sdk to android-' + dictateCompileSdk
            }

            if (dictateAutoBuildTools != null) {
                println '  forcing build tools to revision ' + dictateAutoBuildTools
            }

            if (dictateAutoRepositoryRevision != null) {
                println '  forcing repository to revision ' + dictateAutoRepositoryRevision
            }
        }

        project.extensions.extraProperties.putAt('autoConfig') {
//            def config = new AutoConfig()
//
//            config.autoConfigure = autoConfigure
//
//            config.autoBuildTools = autoBuildTools
//            config.autoCompileSdk = autoCompileSdk
//            config.autoRepositoryRevision = autoRepositoryRevision
//
//            config.dictateAutoBuildTools = dictateAutoBuildTools
//            config.dictateCompileSdk = dictateCompileSdk
//            config.dictateAutoRepositoryRevision = dictateAutoRepositoryRevision

            return this
        }
    }

    public int sdk(int version) {
        if (!autoConfigure && dictateCompileSdk != -1) {
            return dictateCompileSdk
        }

        if (autoConfigure && autoCompileSdk != null) {
            version = Integer.parseInt(autoCompileSdk)
        }

        return version
    }

    public String buildTools(String revision) {
        if (!autoConfigure && dictateAutoBuildTools != null) {
            return dictateAutoBuildTools
        }

        if (autoConfigure && autoBuildTools != null) {
            revision = autoBuildTools
        }

        return revision
    }

    public String support(String version) {
        if (!autoConfigure && dictateAutoRepositoryRevision != null) {
            return dictateAutoRepositoryRevision
        }

        if (autoConfigure && autoRepositoryRevision != null) {
            version = autoRepositoryRevision
        }

        return version
    }

    static String mostRecentVersion(List versions) {
        versions.sort(false) { a, b ->
            [a, b]*.tokenize('.')*.collect { it as int }.with { u, v ->
                [u, v].transpose().findResult { x, y -> x <=> y ?: null } ?: u.size() <=> v.size()
            }
        }[-1]
    }
}