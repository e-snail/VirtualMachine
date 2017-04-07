package com.inject;

import javassist.CannotCompileException;
import javassist.ClassPool
import javassist.CtClass
import javassist.CtField
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.NotFoundException
import org.apache.commons.io.FileUtils
import org.gjt.jclasslib.tools.DecodeClass

import java.lang.reflect.Modifier

/**
 * Created by AItsuki on 2016/4/7.
 * 注入代码分为两种情况，一种是目录，需要遍历里面的class进行注入
 * 另外一种是jar包，需要先解压jar包，注入代码之后重新打包成jar
 */
public class Inject {

    private static ClassPool pool= ClassPool.getDefault()
    private static BufferedWriter out = null;
    /**
     * 添加classPath到ClassPool
     * @param libPath
     */
    public static void appendClassPath(String libPath) {
        println("appendClassPath = "+libPath)
        pool.appendClassPath(libPath)
    }

    public static void initMd5MappingFile(String path) {
        File mapping = new File(path);
        if(mapping.exists()) {
            mapping.delete();
        }
        println("new file = "+path)
        mapping.createNewFile();
        out = new BufferedWriter(new FileWriter(path));
    }

    public static void closeMd5MappingFile() {
        try {
            if(out != null) out.close()
        }catch (Exception e) {

        }
    }

    /**
     * 遍历该目录下的所有class，对所有class进行代码注入。
     * 其中以下class是不需要注入代码的：
     * --- 1. R文件相关
     * --- 2. 配置文件相关（BuildConfig）
     * --- 3. Application
     * @param path 目录的路径
     */
    public static void injectDir(String path, String model) {
        System.out.println("---> to injectDir classpath = "+path)
        pool.appendClassPath(path)
        File dir = new File(path)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->

                String filePath = file.absolutePath
                if (filePath.endsWith(".class")
                        && !filePath.contains('InstallDex')
                        && !filePath.contains('ForceCloseExceptionHandler')
                        && !filePath.contains('VersionManager')
                        && !filePath.contains('R$')
                        && !filePath.contains('R.class')
                        && !filePath.contains("BuildConfig.class")
                        && !filePath.contains("Application.class")) {

                    //println("filepath = "+filePath)
                    String classname = filePath.replace(path, "");
                    int firstSeparator = classname.indexOf("/");
                    classname = classname.substring(firstSeparator == 0 ? 1 : 0,
                            classname.indexOf(".class"));
                    classname = classname.replace('/','.');
                    injectClass(classname, path, model)
                }
            }
        }
    }

    /**
     * 这里需要将jar包先解压，注入代码后再重新生成jar包
     * @path jar包的绝对路径
     */
    public static void injectJar(String path, String model) {
        System.out.println("---> to injectJar path = "+path)
        if (path.endsWith(".jar")) {
            try {
                File jarFile = new File(path)
                // jar包解压后的保存路径
                String jarZipDir = jarFile.getParent() + "/" + jarFile.getName().replace('.jar', '')
                // 解压jar包, 返回jar包中所有class的完整类名的集合（带.class后缀）
                List classNameList = JarZipUtil.unzipJar(path, jarZipDir)
                // 注入代码
                pool.appendClassPath(jarZipDir)
                for (String className : classNameList) {
                    if (className.endsWith(".class")
                            && !className.contains('R$')
                            && !className.contains('R.class')
                            && !className.contains("BuildConfig.class")) {
                        className = className.substring(0, className.length() - 6)
                        injectClass(className, jarZipDir, model)
                    }
                }
                // 删除原来的jar包
                jarFile.delete()
                // 从新打包jar
                JarZipUtil.zipJar(jarZipDir, path)
                // 删除目录
                FileUtils.deleteDirectory(new File(jarZipDir))
            }catch (IOException e) {
                println("IOException = "+e.getMessage())
            }
        }

    }

    public static void injectClass(String className, String path, String model) {
        System.out.println("---> to inject = "+className)
        try {
            CtClass cls = pool.getCtClass(className)
            if (cls.isFrozen()) {
                cls.defrost()
            }
            String fullpath = path+"/"+className.replace('.','/')+".class";
            String md5 = CoreString.getMD5(DecodeClass.decodeclass(fullpath));
            if(out != null) {
                out.write(model+":"+className+":"+md5)
                out.newLine()
                out.flush()
            }

            def init_constructor = cls.getConstructor("<init>");
            init_constructor.insertAfter("System.out.println(\"<init>\" + getClass().name);");
            cls.writeFile(path)

            def clinit_constructor = cls.getDeclaredMethod("<clinit>");
            clinit_constructor.insertAfter("System.out.println(\"<clinit>\" + getClass().name);");
            cls.writeFile(path)

        }catch (NotFoundException e) {
            println("NotFoundException = "+e.getMessage())
        }catch (CannotCompileException e) {
            println("CannotCompileException = "+e.getMessage())
        }catch (IndexOutOfBoundsException e) {
            println("IndexOutOfBoundsException = "+e.getMessage())
        }catch (Exception e) {
            println("otherException = "+e.getMessage())
        }
    }

}