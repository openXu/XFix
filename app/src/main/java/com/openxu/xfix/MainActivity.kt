package com.openxu.xfix

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.openxu.fixlibs.NativeMethodFixMng
import com.openxu.xfix.databinding.ActivityMainBinding
import com.openxu.xfix.util.permission.PermissionCallBack
import com.openxu.xfix.util.permission.PermissionUtil
import com.yanzhenjie.permission.runtime.Permission
import java.io.File

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)

        /**申请sd卡权限*/
        /**申请sd卡权限 */
        PermissionUtil.requestPermission(this, object : PermissionCallBack {
            override fun onGranted() {
                Toast.makeText(applicationContext, "申请sd卡权限通过", Toast.LENGTH_LONG).show()
            }

            override fun onDenied() {
                finish()
            }
        }, Permission.READ_EXTERNAL_STORAGE, Permission.WRITE_EXTERNAL_STORAGE)



        binding.btnBug.setOnClickListener{
            startActivity(Intent(this@MainActivity, BugActivity::class.java))
        }

        binding.btnFix.setOnClickListener{
            //加载so库，如果NativeMethodFixMng加载到了补丁包，就会调用动态库中的native方法来完成修复
            System.loadLibrary("XFix")
            //加载补丁包
            val patchPath = File(Environment.getExternalStorageDirectory(),
                "hotfix")
            val patchFile = File(patchPath.absolutePath, "xfixPatch.dex")
            NativeMethodFixMng.fix(this, patchFile)

            startActivity(Intent(this@MainActivity, BugActivity::class.java))
        }

    }
}