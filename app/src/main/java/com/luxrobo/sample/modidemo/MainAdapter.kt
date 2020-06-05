package com.luxrobo.sample.modidemo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView
import com.luxrobo.sample.modidemo.databinding.ListItemMainBinding

class MainAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var itemList                    : ArrayList<String>         = arrayListOf()
    private var listener                    : OnItemClickListener?= null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val viewDataBinding : ViewDataBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.list_item_main, parent, false)

        return ItemHolder(viewDataBinding)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val viewHolder = holder as ItemHolder
        val binding = viewHolder.binding
        val item = getItem(position)

        binding.itemText.text = item

        binding.itemText.setOnClickListener {

            if(listener != null) {
                listener?.onItemClick(position)
            }
        }


    }

    fun setListener(listener : OnItemClickListener) {

        this.listener = listener
    }

    fun getItem(position : Int) : String {

        return itemList[position]
    }

    fun setItemList(list : ArrayList<String>)  {

        itemList.clear()
        itemList.addAll(list)

    }



    class ItemHolder(itemBinding: ViewDataBinding) : RecyclerView.ViewHolder(itemBinding.root) {

        val binding: ListItemMainBinding = itemBinding as ListItemMainBinding

    }

}