package com.example.androidpdfviewwer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidpdfviewwer.databinding.ItemPdfPageBinding

class PdfPageAdapter(
    private val engine: PdfRenderEngine
) : RecyclerView.Adapter<PdfPageAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPdfPageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return engine.pageCount
    }

    inner class PageViewHolder(private val binding: ItemPdfPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pageIndex: Int) {
            val displayMetrics = binding.root.context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels - 32 // Margin consideration

            engine.renderPage(pageIndex, screenWidth) { bitmap ->
                if (bindingAdapterPosition == pageIndex) {
                    binding.ivPdfPage.setImageBitmap(bitmap)
                    binding.ivPdfPage.resetZoom()
                }
            }
        }
    }
}
