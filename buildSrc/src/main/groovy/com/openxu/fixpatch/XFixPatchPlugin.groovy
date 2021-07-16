package com.openxu.fixpatch
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.openxu.fixpatch.autopatch.Config
import com.openxu.fixpatch.autopatch.ReflectUtils
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Author: openXu 
 * Time: 2020/9/30 14:48
 * class: ClassInsertPlugin
 * Description: 自定义class字节码插桩的插件，同时也是一个Transform，当然我们可以分开定义
 *
 * 每个Transform其实都是一个gradle task，Android Gradle插件API中的TaskManager将每个Transform串连起来，
 * 第一个Transform接收来自javac编译的结果，每个Transform节点可以对class进行处理再传递给下一个Transform。
 * 我们常见的混淆等逻辑都是封装在Transform中的
 *
 * 我们自定义的Transform会插入到这个Transform链条的最前面，直接接受javac编译后的class文件
 *
 */
class XFixPatchPlugin extends Transform implements Plugin<Project>{


    Project project

    private static String jar2DexCommand


    @Override
    void apply(Project project) {
        println("=============热修复遍历补丁类，注册Transform")
        this.project = project
        project.extensions.create('xfix', XFix)
        project.android.registerTransform(this) //注册Transform



//        jar2DexCommand = "   java -jar ${dxFilePath} --dex --output=$Constants.CLASSES_DEX_NAME  " + Constants.ZIP_FILE_NAME;


    }

    @Override
    String getName() {
        return "xfix_transform"
    }
    // 输入类型
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }
    //要操作的范围，这里返回整个项目工程
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }
    //是否支持增量编译
    @Override
    boolean isIncremental() {
        return false
    }

    ClassPool classPool
    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        //在这里对输入输出的class进行处理
        super.transform(transformInvocation)

        //transformInvocation.inputs是class输入集合， 他有两种格式，一种jar包格式，一种目录格式
        Collection<TransformInput> inputs = transformInvocation.inputs
        //TransformOutputProvider用于获取输出路径的类，操作了字节码之后，要让新的字节码输出到某个jar文件中或文件夹下，否则后续的编译没法执行（没有输入）
        TransformOutputProvider outputProvider = transformInvocation.outputProvider

        XFix xfix = project.extensions.getByName("xfix")
        if(!xfix.patch){
            notPatch(inputs, outputProvider)
            return
        }

        println('================寻找补丁类 start================')
        def startTime = System.currentTimeMillis()

        //Class容器，使用javassist操作class文件时，使用到的类必须先添加到容器中，然后才能从ClassPool中获取
        classPool = new ClassPool()
        project.android.bootClasspath.each {
//            println "----添加${it.absolutePath}到classpool"
            //sdk\platforms\android-29\android.jar 添加android sdk到Class容器中
            classPool.appendClassPath((String) it.absolutePath)
        }
        /**将所有class文件封装成CtClass并放入一个集合中List<CtClass> allClass*/
        def box = ReflectUtils.getAllCtClass(inputs, classPool)
        /**打补丁包*/
        autoPatch(box)

        def cost = (System.currentTimeMillis() - startTime) / 1000
        println("================寻找补丁类 end   耗时 $cost second================")
        throw new RuntimeException("★★★★★★补丁包制作完毕")
    }

    /**如果不是打补丁包，将class、jar直接拷贝到目录中，让下一个transform处理*/
    private void notPatch(Collection<TransformInput> inputs, TransformOutputProvider outputProvider){
        inputs.each {
            it.directoryInputs.each {
                // 获取输出目录
                def dest = outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.DIRECTORY)
                FileUtils.copyDirectory(it.file, dest)  // 将input的目录复制到output指定目录
            }
            it.jarInputs.each {
                //jar文件一般是第三方依赖库jar包，这里不需要做处理，但是要输出到指定路径，方便作为下一个Transform的输入
                def dest = outputProvider.getContentLocation(it.name, it.contentTypes, it.scopes, Format.JAR)
                //.gradle\caches\transforms-1\files-1.1\appcompat-1.1.0.aar\xx\jars\classes.jar
                // build\intermediates\transforms\ClassInsertTransform\debug\0.jar
//                println("-------拷贝$it.file.absolutePath 到指定目标: $dest.absolutePath")
                FileUtils.copyFile(it.file, dest)
            }
        }
    }

    /**
     * 遍历所有class，寻找带有MethodReplace注解的类，将其打入补丁包中
     * @param box
     * @return
     */
    String xfixGenerateDirectory
    def autoPatch(List<CtClass> box) {
        File buildDir = project.getBuildDir()
        //app/build/outputs/xfix/
        xfixGenerateDirectory = buildDir.getAbsolutePath() + File.separator + Config.PATCH_OUT_DIR + File.separator
        new File(xfixGenerateDirectory).deleteDir()

        //混淆
        /* if(Config.supportProGuard) {
             ReadMapping.getInstance().initMappingInfo();
         }*/

        /**读取class上的注解信息 @Modify @Add，并将一些信息缓存起来，方便后面使用*/
        Class methodReplaceClass = classPool.get("com.openxu.fixlibs.MethodReplace").toClass();
        println "----找到方法替换注解类 ${methodReplaceClass}"
        List<String> patchClassNameList = new ArrayList<String>()
        box.forEach{ctclass ->
//                println "   检查类 ${ctclass.getName()}"
            try{
                if(ctclass.declaredMethods.find {   //遍历方法，找到第一个带有注解的方法
                    return it.hasAnnotation(methodReplaceClass)   //检查方法是否有@MethodReplace注解
                }){
                    println "☆☆☆☆☆☆遍历寻找到补丁类 ${ctclass.name}"
                    patchClassNameList.add(ctclass.name)
                    ctclass.writeFile(xfixGenerateDirectory) //将补丁class保存到目录中
                }
            }catch(Exception e){
                project.logger.error "xxxxxxxxxxxxxxxxxxxxxxxxxxxxx检查${ctclass.getName()}发生错误：${e.getMessage()}"
            }
        }

        /**将class导入zip包*/
        zipPatchClassesFile()
        /**2. jar->dex*/
        // d8 G:\openXu\openXuHome\openXu-Class\AndroidHotFix\XFix\app\build\outputs\xfix/com/openxu/xfix/*.class
        // --output G:\openXu\openXuHome\openXu-Class\AndroidHotFix\XFix\app\build\outputs\xfix
        String con = "d8 G:/openXu/openXuHome/openXu-Class/AndroidHotFix/XFix/app/build/outputs/xfix/com/openxu/xfix/*.class --output G:/openXu/openXuHome/openXu-Class/AndroidHotFix/XFix/app/build/outputs/xfix"
        con.execute()
//        executeCommand("d8 ${xfixGenerateDirectory}/com/openxu/xfix/*.class")
//        executeCommand(jar2DexCommand)

        /**3. dex-smali*/
//        executeCommand(dex2SmaliCommand)
        /**4. 修改smali*/
//        SmaliTool.getInstance().dealObscureInSmali();
        /**5. smali汇编->dex*/
//        executeCommand(smali2DexCommand)
        //package patch.dex to patch.jar
        /**6. 将dex压缩成jar包*/
//        packagePatchDex2Jar()
//        deleteTmpFiles()

    }

    def executeCommand(String commond) {
        //Process提供用于执行来自进程的输入、执行对进程的输出、等待进程完成、检查进程的退出状态以及销毁（终止）进程的方法。
//        Process output = commond.execute(null, new File(xfixGenerateDirectory))
        Process output = commond.execute()
        output.inputStream.eachLine { println commond + " inputStream output   " + it }
        output.errorStream.eachLine {
            println commond + " errorStream output   " + it
            throw new RuntimeException("execute command " + commond + " error");
        }
    }



    def zipPatchClassesFile(){
        println("******开始压缩文件")
        // app/build/outputs/xfix/xfix.zip
        ZipOutputStream zipOut = new ZipOutputStream(
                new FileOutputStream(xfixGenerateDirectory + Config.PATCH_CLASS_ZIP))
        // app/build/outputs/xfix/
        zipAllPatchClasses(xfixGenerateDirectory, "", zipOut)
        zipOut.close()
    }
    /**递归压缩*/
    def zipAllPatchClasses(String path, String fullClassName, ZipOutputStream zipOut) {
        println("    压缩 ${path}    fullClassName=${fullClassName} ")
        File file = new File(path)  // app/build/outputs/xfix/
        if (file.exists()) {
            fullClassName = fullClassName + file.name
            if (file.isDirectory()) {
                fullClassName += File.separator
                File[] files = file.listFiles()
                if (files.length == 0) {
                    return
                } else {
                    for (File file2 : files) {
                        // app/build/outputs/xfix/com
                        // app/build/outputs/xfix/com/openxu
                        // app/build/outputs/xfix/com/openxu/xfix
                        zipAllPatchClasses(file2.getAbsolutePath(), fullClassName, zipOut)
                    }
                }
            } else {//文件
                zipFile(file, zipOut, fullClassName)
            }
        } else {
            project.logger.error("-----文件不存在!");
        }
    }
    /**压缩单个文件到压缩包*/
    def zipFile(File inputFile, ZipOutputStream zipOut, String entryName){
        project.logger.error "++++++压缩${inputFile.getName()}    $entryName"
        ZipEntry entry = new ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        FileInputStream fis = new FileInputStream(inputFile)
        byte[] buffer = new byte[4092]
        int byteCount = 0
        while ((byteCount = fis.read(buffer)) != -1) {
            zipOut.write(buffer, 0, byteCount);
        }
        fis.close()
        zipOut.closeEntry()
        zipOut.flush()
    }


}
