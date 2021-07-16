package com.openxu.fixlibs;

/**
 * Author: openXu
 * Time: 2020/9/27 17:11
 * class: NativeStructsMode
 * Description: 用于计算ART虚拟机底层ArtMethod的size
 * 该类只有两个方法，对应的ArtMethod在方法数组中肯定是相邻的，
 * 通过两个ArtMethod的起始地址差值就能得到ArtMethod的大小
 */
public class NativeStructsMode {
    final public static void f1(){}
    final public static void f2(){}
}