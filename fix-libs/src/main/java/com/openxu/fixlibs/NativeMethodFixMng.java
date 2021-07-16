package com.openxu.fixlibs;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import dalvik.system.DexFile;

/**
 * Native hook实现方法替换方案，有2种实现：
 * 1. 通过替换底层ArtMethod成员，实现方法内容的替换，
 * 这种方案兼容性太差，需要适配不同Android版本及厂商
 *
 * 2. 通过替换ArtMethod的地址，实现ArtMethod整体替换，
 * 这种方案不存在兼容性问题，不管ArtMethod内部结构怎么变化，只要保证其存储结构为数组线性结构即可
 */
public class NativeMethodFixMng {

    private static String TAG = "NativeMethodFixMng";

    /**
     * 加载补丁包，完成方法修复
     *
     * @param context
     * @param patchFile 补丁包dex文件
     */
    public static void fix(Context context, File patchFile) {
        Log.w(TAG, "-----------加载补丁包："+patchFile);
        try {
            File optDir = new File(context.getFilesDir(), "opt");
            if(!optDir.exists())
                optDir.mkdirs();

            File optfile = new File(optDir, patchFile.getName());
            if(optfile.exists()){
                Log.w(TAG, "优化文件存在，先删除");
                optfile.delete();
            }
            /**
             * 1. DexFile.loadDex()用于加载一个dex文件，并将优化后的dex数据写入指定文件目录
             *    如果文件不存在，抛异常IOException
             * 第一个参数：含有classes.dex文件的jar、apk、dex等文件绝对路径
             * 第二个参数：将保存dex数据的优化形式的文件目录
             * 第三个参数：以私有模式打开
             */
            final DexFile dexFile = DexFile.loadDex(patchFile.getAbsolutePath(),
                    optDir.getAbsolutePath(), Context.MODE_PRIVATE);
            //系统默认类加载器，用于加载apk包中的bug类
            ClassLoader classLoader = context.getClassLoader();
            //补丁类加载器，下面通过dexFile加载补丁类时需要传递该对象，目的就是和apk中bug类做区分
            ClassLoader patchClassLoader = new ClassLoader(classLoader) {
                @Override
                protected Class<?> findClass(String className) throws ClassNotFoundException {
                    /**
                     * dexFile.loadClass(clazzName, patchClassLoader)加载类时，并不会走这个方法，而是直接从dex或者jar中加载Class对象返回，
                     * 传递patchClassLoader的作用是该Class对象被认为是patchClassLoader加载的
                     *
                     * 如果写成dexFile.loadClass(clazzName, classLoader)，加载的补丁类就直接被认为是系统默认类加载器加载的，
                     * 当打开bug页面时系统类加载器会直接使用已经加载的补丁类，bug类就被忽略了，下面的方法替换就毫无意义
                     */
                    Log.d(TAG, "xxxxxxxx永远不会走这个方法");
                    return null;
                }
            };
            //获取dexFile文件中所有的class类名的枚举
            Enumeration<String> entry=dexFile.entries();
            while (entry.hasMoreElements()) {
                String clazzName= entry.nextElement();
                /**2. 加载补丁dex中的单个补丁Class
                 * ★★★ 此处必须传递一个新的ClassLoader对象，而不是系统默认类加载器
                 */
                Class patchClazz= dexFile.loadClass(clazzName, patchClassLoader);
                if (patchClazz != null){
                    Method[] methods=patchClazz.getDeclaredMethods();
                    Log.w(TAG, "-----------加载补丁类："+patchClazz+"  开始遍历修复方法，总共："+methods.length);
                    /**3. 遍历补丁类的所有方法，通过注解得到需要修复的错误方法*/
                    for (Method fixMethod : methods) {
                        //获取方法上的注解信息（clazz=bug类， method=bug方法）
                        MethodReplace replaceInfo = fixMethod.getAnnotation(MethodReplace.class);
                        Log.w(TAG, "-----------检查方法是否需要修复："+fixMethod.getName()+"   "+replaceInfo);
                        if (replaceInfo == null)  //如果没有MethodReplace注解表示该方法不需要被修复，continue继续遍历下一个方法
                            continue;
                        /**反射得到apk包中有bug的类*/
                        Class<?> bugClazz = Class.forName(replaceInfo.clazz(), true, context.getClassLoader());
                        Log.w(TAG, "补丁类"+patchClazz);
                        Log.w(TAG, "bug类"+bugClazz);
                        Log.w(TAG, "是否是同一个Class对象 "+(bugClazz == patchClazz));
                        /**得到有bug的方法（注意补丁包中的方法参数名和参数列表必须一致）*/
                        Method bugMethod = bugClazz.getDeclaredMethod(replaceInfo.method(), fixMethod.getParameterTypes());
                        Log.e(TAG, "修复"+bugClazz.getName()+"的方法"+bugMethod.getName());
                        /**4. 调用native方法替换有bug的方法*/
                        //获取ArtMethod的内存大小
                        int size = getSizeOfArtMethod(NativeStructsMode.class.getDeclaredMethod("f1"),
                                NativeStructsMode.class.getDeclaredMethod("f2"));
                        int size1 = getSizeOfArtMethod1(NativeStructsMode.class);
                        ///4.2 实现ArtMethod整体替换
                        replaceWhole(bugMethod, fixMethod, size);
                    }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
            Toast.makeText(context, "补丁包"+patchFile+"不存在", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**获取native层ArtMethod指针的大小*/
    public native static int getSizeOfArtMethod(Method f1, Method f2);
    public native static int getSizeOfArtMethod1(Class NativeStructsModeClazz);
    /**native替换ArtMethod地址，从而实现方法替换*/
    public native static void replaceWhole(Method bugMethod, Method fixMethod, int size);
}