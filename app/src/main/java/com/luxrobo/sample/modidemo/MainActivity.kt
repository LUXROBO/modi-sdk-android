package com.luxrobo.sample.modidemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.luxrobo.sample.modidemo.databinding.ActivityMainBinding
import com.luxrobo.sample.modidemo.sample_modi_demo.example_scanning.ScanActivity


private const val SAMPLE_MODI_DEMO               = 0
private const val SAMPLE_MODI_RX_DEMO            = 1


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        initUI()
    }

    private fun initUI() {


        setAdapter()

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
