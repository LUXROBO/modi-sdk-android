package com.luxrobo.sample.modidemo.sample_modi_demo.example_scanning

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.luxrobo.modisdk.utils.ModiLog
import com.luxrobo.sample.modidemo.R
import com.luxrobo.sample.modidemo.databinding.ListItemScanBinding
import com.luxrobo.sample.modidemo.sample_modi_demo.Singleton
import com.polidea.rxandroidble2.scan.ScanResult

class ScanAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var itemList                    : MutableList<ScanResult>           = mutableListOf()
    private var listener                    : OnItemClickListener?= null

    interface OnItemClickListener {
        fun onItemClick(deviceAddress: String)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val viewDataBinding : ViewDataBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.list_item_scan, parent, false)

        return ItemHolder(viewDataBinding)
    }

    override fun getItemCount(): Int {


        return itemList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val viewHolder      = holder as ItemHolder
        val binding  = viewHolder.binding
        val item            = getItem(position)

        binding.itemDeviceName.text     = item.bleDevice.name
        binding.itemDeviceAddress.text  = item.bleDevice.macAddress
        binding.itemDeviceRssi.text     = item.rssi.toString()

        binding.root.setOnClickListener {

            ModiLog.d("binding.root.setOnClickListener $listener")

            if(listener != null) {
                listener?.onItemClick(item.bleDevice.macAddress)
            }
        }

    }

    fun setListener(listener : OnItemClickListener) {

        this.listener = listener
    }

    fun getItem(position : Int) : ScanResult {

        return itemList[position]
    }

    fun setItemList(list : MutableList<ScanResult>) {

        itemList = list
        notifyDataSetChanged()
    }

    fun removeAll() {

        itemList.clear()
    }

    class ItemHolder(itemBinding: ViewDataBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        val binding: ListItemScanBinding = itemBinding as ListItemScanBinding

    }

}