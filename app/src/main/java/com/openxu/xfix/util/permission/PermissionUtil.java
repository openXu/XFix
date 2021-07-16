package com.openxu.xfix.util.permission;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.runtime.Permission;

import java.util.List;

/**
 * Author: openXu
 * Time: 2019/3/14 15:44
 * class: PermissionUtile
 * Description: 权限工具类
 */
public class PermissionUtil {

    public static final int PERMISSION_REQUEST_CODE = 1001;

   /**
     * 申请相关权限
     * @param activity
     * @param callBack
     * @param requestPermissions 需要申请的权限
     */
    public static void requestPermission(Activity activity, PermissionCallBack callBack, String... requestPermissions){
        AndPermission.with(activity)
                .runtime()
                .permission(requestPermissions)
                //拒绝一次后提示重试,部分中国产手机，由于系统原因，不会调用rationale()的回调方法
                .rationale((context, data, executor) -> {
                    new AlertDialog.Builder(context)
                            .setCancelable(false)
                            .setTitle("权限申请提醒")
                            .setMessage(getPermissionMsg(false, Permission.transformText(context, requestPermissions)))
                            .setPositiveButton("好的", (dialog, which) -> {
                                dialog.cancel();
                                executor.execute();  //重试
                            })
                            .setNegativeButton("拒绝", (dialog, which) -> {
                                dialog.cancel();
                                executor.cancel();  //拒绝
                            })
                            .show();
                })
                .onGranted(permissions -> {
                    // Storage permission are allowed.
                    callBack.onGranted();
                })
                .onDenied(permissions -> {
                    // Storage permission are not allowed.
                    //AndPermission.hasAlwaysDeniedPermission()只能在onDenied()的回调中调用，不能在其它地方使用。
                    if (AndPermission.hasAlwaysDeniedPermission(activity, permissions)) {
                        // 用Dialog展示没有某权限，询问用户是否去设置中授权。
//                        AndPermission.with(this).runtime() .setting().start(100);
                    }
                    new AlertDialog.Builder(activity)
                            .setCancelable(false)
                            .setTitle("权限申请提醒")
                            .setMessage(getPermissionMsg(true, Permission.transformText(activity, permissions)))
                            .setPositiveButton("去设置", (dialog, which) -> {
                                dialog.cancel();
                                Intent intent = new Intent();
                                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                                intent.setData(uri);
                                activity.startActivityForResult(intent, PERMISSION_REQUEST_CODE);
                            })
                            .setNegativeButton("取消", (dialog, which) -> {
                                dialog.cancel();
                                callBack.onDenied();
                            })
                            .show();
                })
                .start();
    }

    private static String getPermissionMsg(boolean setting, List<String> msgs){
        String message = "";
        if(null!=msgs)
            for(String msg : msgs)
                message += (TextUtils.isEmpty(message)?msg:"、"+msg);
        if(TextUtils.isEmpty(message)){
            message = setting?"请在应用设置中开启相关权限后继续使用 !":"这里需要相关权限才能更好的为您服务 !";
        }else{
            message = setting?"请在应用设置中开启 ["+message+"] 权限后继续使用 !":"这里需要 ["+message+"] 权限才能更好的为您服务!";
        }
        return message;
    }


    /**
     * 获取AndPermission库中的Authority
     * @param context
     * @return
     */
    public static String getAuthority(Context context){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return context.getPackageName() + ".file.path.share";
        }
        return "";
    }
}
