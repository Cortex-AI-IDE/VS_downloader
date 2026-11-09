package com.hakeem.videosaver

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.hakeem.videosaver.databinding.ItemDownloadBinding

class DownloadAdapter(
    private val onOpen: (DownloadsRepository.SavedFile) -> Unit,
    private val onShare: (DownloadsRepository.SavedFile) -> Unit,
    private val onDelete: (DownloadsRepository.SavedFile) -> Unit
) : RecyclerView.Adapter<DownloadAdapter.Holder>() {

    private val items = mutableListOf<DownloadsRepository.SavedFile>()

    fun submit(newItems: List<DownloadsRepository.SavedFile>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemDownloadBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemDownloadBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val context = holder.binding.root.context

        holder.binding.fileName.text = item.name
        holder.binding.fileMeta.text = context.getString(
            R.string.file_meta,
            DownloadsRepository.formatSize(item.size),
            DownloadsRepository.formatDate(item.dateAdded)
        )

        holder.binding.root.setOnClickListener { onOpen(item) }
        holder.binding.btnOpen.setOnClickListener { onOpen(item) }
        holder.binding.btnShare.setOnClickListener { onShare(item) }
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount(): Int = items.size
}
