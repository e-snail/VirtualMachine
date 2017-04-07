package com.inject;

import groovy.xml.MarkupBuilder
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import org.xml.sax.SAXException

public class PreDexTransform extends Transform {
    Project project

    public PreDexTransform(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return "preDex"
    }
    // 指定input的类
    @Override
    Set getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }
    // 指定Transfrom的作用范围
    @Override
    Set getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }
    @Override
    boolean isIncremental() {
        return false
    }
    @Override
    void transform(Context context, Collection inputs, Collection referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental)
            throws IOException, TransformException, InterruptedException {

        System.out.println("PreDexTransform transform")
        String appClasspathDebug = project.rootProject.rootDir.absolutePath+"/app/build/intermediates/classes/debug"
        String appClasspathRelease = project.rootProject.rootDir.absolutePath+"/app/build/intermediates/classes/release"
        String androidsdk = project.rootProject.rootDir.absolutePath + "/buildSrc/libs/android.jar";
        Inject.appendClassPath(appClasspathDebug)
        Inject.appendClassPath(appClasspathRelease)
        Inject.appendClassPath(androidsdk)
        /**
         * 遍历输入文件
         */
        inputs.each { TransformInput input ->
            /**
             * 遍历jar
             */
            input.jarInputs.each { JarInput jarInput ->
                String projectName = project.rootProject.name;
                String jarPath = jarInput.file.absolutePath;
                Inject.appendClassPath(jarPath)
                if(jarPath.endsWith("classes.jar") && jarPath.contains("exploded-aar/"+projectName)) {
                    String indexstr = "exploded-aar/"+projectName
                    String model = jarPath.substring(jarPath.indexOf(indexstr) + indexstr.length() + 1)
                    model = model.substring(0, model.indexOf("/"))
                    Inject.injectJar(jarPath, model)
                }
                //重名名输出文件,因为可能同名,会覆盖
                String destName = jarInput.name;
                def hexName = DigestUtils.md5Hex(jarInput.file.absolutePath);
                if (destName.endsWith(".jar")) {
                    destName = destName.substring(0, destName.length() - 4);
                }
                /**
                 * 获得输出文件
                 */
                File dest = outputProvider.getContentLocation(destName + "_" + hexName, jarInput.contentTypes, jarInput.scopes, Format.JAR);

                FileUtils.copyFile(new File(jarPath), dest);
            }

            input.directoryInputs.each { DirectoryInput directoryInput ->
                Inject.injectDir(directoryInput.file.absolutePath, "app")
                File dest = outputProvider.getContentLocation(directoryInput.name, directoryInput.contentTypes, directoryInput.scopes, Format.DIRECTORY);
                FileUtils.copyDirectory(directoryInput.file, dest);
            }

        }
        Inject.closeMd5MappingFile();
    }
}