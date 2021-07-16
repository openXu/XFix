package com.openxu.xfix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.openxu.fixlibs.MethodReplace
import com.openxu.xfix.databinding.ActivityBugBinding

class BugActivity : AppCompatActivity() {

    lateinit var binding:ActivityBugBinding

    @MethodReplace(
        clazz = "com.openxu.xfix.BugActivity",
        method = "onCreate"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.
        setContentView(this, R.layout.activity_bug)
        binding.tvResult1.text = "onCreate方法错误"
//        binding.tvResult1.text = "onCreate方法修复"
        otherMethod()

    }

    @MethodReplace(
        clazz = "com.openxu.xfix.BugActivity",
        method = "otherMethod"
    )
    private fun otherMethod() {
        binding.tvResult2.text = Util.getText()
    }


}