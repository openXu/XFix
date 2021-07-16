package com.openxu.xfix

import com.openxu.fixlibs.MethodReplace

/**
 * Author: openXu
 * Time: 2021/7/16 12:55
 * class: Util
 * Description:
 */
object Util {
    @MethodReplace(
        clazz = "com.openxu.xfix.Util",
        method = "getText"
    )
    fun getText():String{
//        return "我是工具类修复字符串"
        return "我是工具类返回的错误字符串"
    }

}