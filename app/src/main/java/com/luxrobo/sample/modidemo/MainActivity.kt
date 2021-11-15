package com.luxrobo.sample.modidemo

import android.Manifest
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.luxrobo.modisdk.utils.ModiLog
import com.luxrobo.sample.modidemo.databinding.ActivityMainBinding
import com.luxrobo.sample.modidemo.sample_modi_demo.example_scanning.ScanActivity


private const val SAMPLE_MODI_DEMO               = 0
private const val SAMPLE_MODI_RX_DEMO            = 1


class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initUI()

        permissionCheck()
    }

    private fun initUI() {


        setAdapter()

    }

    private fun permissionCheck() {

        ModiLog.d("kstlove permissionCheck()")

        if (Build.VERSION.SDK_INT >= 23) {
            var permissionCheck = ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION)

            val permissionList: ArrayList<String> = arrayListOf()

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(ACCESS_FINE_LOCATION)
            }


            permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

            if (permissionList.isNotEmpty()) {
                var strArray = arrayOfNulls<String>(permissionList.size)
                strArray = permissionList.toArray(strArray)

                ActivityCompat.requestPermissions(this, strArray, 1000)
            }


        }
    }

    private fun setAdapter() {

        val adapter = MainAdapter()
        adapter.setItemList(setArrayList())
        adapter.setListener(object : MainAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {

                when(position) {

                    SAMPLE_MODI_DEMO -> {

                        //move to scan activity
                        val intent = Intent(this@MainActivity, ScanActivity::class.java)
                        startActivity(intent)
                    }

                    SAMPLE_MODI_RX_DEMO -> {
                        //TODO sdk rx버전 제작 필요
                    }
                }
            }
        })

        val layoutManager = LinearLayoutManager(this)
        binding.list.layoutManager = layoutManager
        binding.list.adapter = adapter

    }

    private fun setArrayList() : ArrayList<String> {

        val arrayList = arrayListOf<String>()

        arrayList.add("SAMPLE_MODI_DEMO")
//        arrayList.add("SAMPLE_MODI_RX_DEMO")

        return arrayList
    }


}
