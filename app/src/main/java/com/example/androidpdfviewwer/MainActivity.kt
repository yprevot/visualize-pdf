package com.example.androidpdfviewwer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidpdfviewwer.databinding.ActivityMainBinding
import com.example.androidpdfviewwer.databinding.DialogJumpPageBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var renderEngine: HybridPdfEngine
    private var pageAdapter: PdfPageAdapter? = null
    private var currentPdfUri: Uri? = null
    private var isLoading = false

    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Provider doesn't support persistable grants (e.g. Gmail). Best effort.
            }
            loadPdf(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderEngine = HybridPdfEngine(this)

        setupToolbar()
        setupRecyclerView()
        setupListeners()

        val restored = savedInstanceState?.getString(KEY_URI)?.let { Uri.parse(it) }
        if (restored != null) {
            loadPdf(restored)
        } else {
            handleIncomingIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        currentPdfUri?.let { outState.putString(KEY_URI, it.toString()) }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.app_name)
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        binding.rvPdfPages.layoutManager = layoutManager

        binding.rvPdfPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                if (firstVisible != RecyclerView.NO_POSITION && renderEngine.pageCount > 0) {
                    updatePageIndicator(firstVisible + 1, renderEngine.pageCount)
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnOpenFile.setOnClickListener { openPdfPicker() }
        binding.btnOpenSample.setOnClickListener { openSamplePdf() }
        binding.btnJumpPage.setOnClickListener { showJumpPageDialog() }
    }

    private fun openPdfPicker() {
        openDocumentLauncher.launch(arrayOf("application/pdf"))
    }

    private fun openSamplePdf() {
        if (isLoading) return
        isLoading = true
        binding.progressIndicator.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val sampleUri = SamplePdfGenerator.createSamplePdf(this@MainActivity)
            withContext(Dispatchers.Main) {
                isLoading = false
                binding.progressIndicator.visibility = View.GONE
                if (sampleUri != null) {
                    loadPdf(sampleUri, isSample = true)
                } else {
                    Toast.makeText(this@MainActivity, "Error al generar PDF de muestra", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { loadPdf(it) }
            }
            Intent.ACTION_SEND -> {
                // Gmail / WhatsApp / Drive share via EXTRA_STREAM, not data.
                @Suppress("DEPRECATION")
                val stream: Uri? = intent.getParcelableExtra(Intent.EXTRA_STREAM)
                stream?.let { loadPdf(it) } ?: intent.data?.let { loadPdf(it) }
            }
        }
    }

    private fun loadPdf(uri: Uri, isSample: Boolean = false, password: String? = null) {
        if (isLoading) return
        isLoading = true
        binding.progressIndicator.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            val result = renderEngine.openPdf(uri, password)
            withContext(Dispatchers.Main) {
                isLoading = false
                binding.progressIndicator.visibility = View.GONE
                when (result) {
                    is PdfOpenResult.Success -> {
                        currentPdfUri = uri
                        binding.rvPdfPages.visibility = View.VISIBLE
                        binding.bottomPageBar.visibility = View.VISIBLE

                        pageAdapter = PdfPageAdapter(renderEngine)
                        binding.rvPdfPages.adapter = pageAdapter

                        val fileName = if (isSample) getString(R.string.sample_pdf_title)
                        else getFileName(uri) ?: "Documento PDF"
                        supportActionBar?.title = fileName
                        supportActionBar?.subtitle = "${result.pageCount} páginas"
                        updatePageIndicator(1, result.pageCount)
                        binding.rvPdfPages.scrollToPosition(0)
                        Snackbar.make(binding.root, "PDF cargado con éxito", Snackbar.LENGTH_SHORT).show()
                    }
                    is PdfOpenResult.Failure -> {
                        if (result.error == PdfError.PASSWORD_REQUIRED && password == null) {
                            showPasswordDialog(uri, isSample)
                        } else {
                            showEmptyStateWithError(result.error)
                        }
                    }
                }
            }
        }
    }

    private fun showEmptyStateWithError(error: PdfError) {
        binding.rvPdfPages.visibility = View.GONE
        binding.bottomPageBar.visibility = View.GONE
        binding.emptyView.visibility = View.VISIBLE
        supportActionBar?.title = getString(R.string.app_name)
        supportActionBar?.subtitle = null

        val message = when (error) {
            PdfError.PASSWORD_REQUIRED -> getString(R.string.error_password)
            PdfError.CORRUPT -> getString(R.string.error_corrupt)
            PdfError.TOO_LARGE -> getString(R.string.error_too_large)
            PdfError.OOM -> getString(R.string.error_oom)
            else -> getString(R.string.error_loading_pdf)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Error al abrir PDF")
            .setMessage(message)
            .setPositiveButton("Aceptar", null)
            .show()
    }

    private fun showPasswordDialog(uri: Uri, isSample: Boolean) {
        val input = TextInputEditText(this).apply {
            hint = getString(R.string.password_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val padded = android.widget.FrameLayout(this).apply {
            val pad = (24 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad / 2, pad, 0)
            addView(input)
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.error_password))
            .setView(padded)
            .setPositiveButton(getString(R.string.open)) { _, _ ->
                loadPdf(uri, isSample, input.text?.toString().orEmpty())
            }
            .setNegativeButton(getString(R.string.cancel)) { _, _ ->
                binding.emptyView.visibility = View.VISIBLE
            }
            .show()
    }

    private fun updatePageIndicator(current: Int, total: Int) {
        binding.tvPageIndicator.text = getString(R.string.page_format, current, total)
    }

    private fun showJumpPageDialog() {
        if (renderEngine.pageCount <= 0) return

        val dialogBinding = DialogJumpPageBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tilPageNumber.hint = getString(R.string.enter_page_number, renderEngine.pageCount)

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.go) { _, _ ->
                val pageNum = dialogBinding.etPageNumber.text.toString().toIntOrNull()
                if (pageNum != null && pageNum in 1..renderEngine.pageCount) {
                    binding.rvPdfPages.scrollToPosition(pageNum - 1)
                } else {
                    Toast.makeText(this, "Número de página inválido", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun sharePdf() {
        val uri = currentPdfUri ?: return
        // Normalize file:// to FileProvider content:// to avoid FileUriExposedException.
        val safeUri: Uri = if (uri.scheme == "file") {
            val file = uri.path?.let { java.io.File(it) }
            if (file != null && file.exists()) PdfFileHelper.fileProviderUri(this, file) else uri
        } else {
            uri
        }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, safeUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                contentResolver.query(uri, null, null, null, null)?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index >= 0) result = it.getString(index)
                    }
                }
            } catch (_: Exception) {}
        }
        if (result == null) {
            result = uri.lastPathSegment?.substringAfterLast('/')
        }
        return result
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_open -> {
                openPdfPicker()
                true
            }
            R.id.action_sample -> {
                openSamplePdf()
                true
            }
            R.id.action_share -> {
                if (currentPdfUri != null) sharePdf()
                else Toast.makeText(this, "Abre un PDF primero para compartirlo", Toast.LENGTH_SHORT).show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        pageAdapter = null
        binding.rvPdfPages.adapter = null
        try { renderEngine.shutdown() } catch (_: Exception) {}
        super.onDestroy()
    }

    companion object {
        private const val KEY_URI = "current_pdf_uri"
    }
}
