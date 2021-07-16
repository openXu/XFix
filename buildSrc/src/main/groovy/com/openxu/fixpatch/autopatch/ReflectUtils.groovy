package com.openxu.fixpatch.autopatch

import com.android.SdkConstants
import com.android.build.api.transform.TransformInput
import javassist.*
import org.apache.commons.io.FileUtils

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.regex.Matcher

class ReflectUtils {

    /**
     * 从输入中读取所有class文件，包括jar包，放入classpool池中
     * 然后获取所有CtClass对象集合返回
     * @param inputs
     * @param classPool
     * @return
     */
    static List<CtClass> getAllCtClass(Collection<TransformInput> inputs, ClassPool classPool) {
        List<String> classNames = new ArrayList<>()
        List<CtClass> allClass = new ArrayList<>()
        def startTime = System.currentTimeMillis()
        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
//                println("********读取目录$dirPath 中的class文件")
                classPool.insertClassPath(it.file.absolutePath)
                FileUtils.listFiles(it.file, null, true).each {
                    if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        //com.openxu.xfix.BugActivity
                        def className = it.absolutePath.substring(dirPath.length() + 1, it.absolutePath.length() - SdkConstants.DOT_CLASS.length())
                                .replaceAll(Matcher.quoteReplacement(File.separator), '.')
                        classNames.add(className)
                    }
                }
            }
            it.jarInputs.each {
                classPool.insertClassPath(it.file.absolutePath)
                def jarFile = new JarFile(it.file)
//                println("**********读取$jarFile 中的class文件")
                Enumeration<JarEntry> classes = jarFile.entries()
                while (classes.hasMoreElements()) {
                    JarEntry libClass = classes.nextElement()
                    String className = libClass.getName()
                    if (className.endsWith(SdkConstants.DOT_CLASS)) {
                        className = className.substring(0, className.length() - SdkConstants.DOT_CLASS.length()).replaceAll('/', '.')
                        classNames.add(className)
                    }
                }
            }
        }

        classNames.each {
            try {
                allClass.add(classPool.get(it));
            } catch (NotFoundException e) {
                println "找不到类:  $it "
                e.printStackTrace()

            }
        }
        def cost = (System.currentTimeMillis() - startTime) / 1000
        println "********xfix读取所有class文件耗时$cost s，总共${allClass.size()}个类"
        return allClass
    }



}
