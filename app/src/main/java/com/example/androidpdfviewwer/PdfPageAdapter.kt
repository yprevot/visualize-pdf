package com.example.androidpdfviewwer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.androidpdfviewwer.databinding.ItemPdfPageBinding

class PdfPageAdapter(
    private val engine: PdfEngine
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

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.recycle()
    }

    override fun getItemCount(): Int = engine.pageCount

    inner class PageViewHolder(val binding: ItemPdfPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var boundPage = RecyclerView.NO_POSITION
        private var hiResRunnable: Runnable? = null

        fun bind(pageIndex: Int) {
            boundPage = pageIndex
            val context = binding.root.context
            val metrics = context.resources.displayMetrics
            // 8dp margins on each side + 8dp card padding ≈ 32dp total.
            val marginPx = (32 * metrics.density).toInt()
            val screenWidth = (metrics.widthPixels - marginPx).coerceAtLeast(320)

            // Fix height upfront from aspect so mixed page sizes (A4/landscape)
            // don't collapse or jump. Default to A4 portrait until known.
            val aspect = engine.pageAspect(pageIndex) ?: (595f / 842f)
            val targetHeight = (screenWidth / aspect).toInt().coerceIn(200, 4096)
            binding.ivPdfPage.layoutParams = binding.ivPdfPage.layoutParams.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = targetHeight
            }

            // Placeholder: clear stale bitmap to avoid flashing the wrong page.
            binding.ivPdfPage.setImageDrawable(null)
            binding.ivPdfPage.resetZoom()
            binding.ivPdfPage.onZoomEnd = { scale ->
                // Re-render sharp when user zooms in (debounced).
                hiResRunnable?.let { binding.ivPdfPage.removeCallbacks(it) }
                val r = Runnable {
                    if (bindingAdapterPosition == pageIndex) {
                        val hiResWidth = (screenWidth * scale).toInt()
                            .coerceIn(screenWidth, 4096)
                        engine.renderPage(pageIndex, hiResWidth) { bitmap ->
                            if (bindingAdapterPosition == pageIndex) {
                                binding.ivPdfPage.setImageBitmap(bitmap)
                            }
                        }
                    }
                }
                hiResRunnable = r
                binding.ivPdfPage.postDelayed(r, 180)
            }

            engine.renderPage(pageIndex, screenWidth) { bitmap ->
                if (bindingAdapterPosition == pageIndex && boundPage == pageIndex) {
                    binding.ivPdfPage.setImageBitmap(bitmap)
                    // Post the zoom reset so view dims are measured with the new drawable.
                    binding.ivPdfPage.post { binding.ivPdfPage.resetZoom() }
                }
            }
        }

        fun recycle() {
            hiResRunnable?.let { binding.ivPdfPage.removeCallbacks(it) }
            hiResRunnable = null
            binding.ivPdfPage.onZoomEnd = null
            if (boundPage != RecyclerView.NO_POSITION) {
                engine.cancelPending(boundPage)
            }
            boundPage = RecyclerView.NO_POSITION
            binding.ivPdfPage.setImageDrawable(null)
        }
    }
}
