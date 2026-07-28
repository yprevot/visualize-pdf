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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.androidpdfviewwer.databinding.ActivityMainBinding
import com.example.androidpdfviewwer.databinding.DialogJumpPageBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var renderEngine: PdfRenderEngine
    private var pageAdapter: PdfPageAdapter? = null
    private var currentPdfUri: Uri? = null

    // Register Activity Result Launcher for Document Picker (SAF)
    private val openDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Persist read permission if available
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Ignore if not supported by provider
            }
            loadPdf(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        renderEngine = PdfRenderEngine(this)

        setupToolbar()
        setupRecyclerView()
        setupListeners()

        // Handle incoming intent (e.g. from File Manager, Email, Chrome)
        handleIncomingIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
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
                    val currentPage = firstVisible + 1
                    updatePageIndicator(currentPage, renderEngine.pageCount)
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnOpenFile.setOnClickListener {
            openPdfPicker()
        }

        binding.btnOpenSample.setOnClickListener {
            openSamplePdf()
        }

        binding.btnJumpPage.setOnClickListener {
            showJumpPageDialog()
        }
    }

    private fun openPdfPicker() {
        openDocumentLauncher.launch(arrayOf("application/pdf"))
    }

    private fun openSamplePdf() {
        binding.progressIndicator.visibility = View.VISIBLE
        val sampleUri = SamplePdfGenerator.createSamplePdf(this)
        binding.progressIndicator.visibility = View.GONE
        if (sampleUri != null) {
            loadPdf(sampleUri, isSample = true)
        } else {
            Toast.makeText(this, "Error al generar PDF de muestra", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleIncomingIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val data: Uri? = intent.data

        if ((Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action) && data != null) {
            loadPdf(data)
        }
    }

    private fun loadPdf(uri: Uri, isSample: Boolean = false) {
        binding.progressIndicator.visibility = View.VISIBLE
        binding.emptyView.visibility = View.GONE

        currentPdfUri = uri
        val success = renderEngine.openPdf(uri)

        binding.progressIndicator.visibility = View.GONE

        if (success && renderEngine.pageCount > 0) {
            binding.rvPdfPages.visibility = View.VISIBLE
            binding.bottomPageBar.visibility = View.VISIBLE

            pageAdapter = PdfPageAdapter(renderEngine)
            binding.rvPdfPages.adapter = pageAdapter

            val fileName = getFileName(uri) ?: if (isSample) getString(R.string.sample_pdf_title) else "Documento PDF"
            supportActionBar?.title = fileName
            supportActionBar?.subtitle = "${renderEngine.pageCount} páginas"

            updatePageIndicator(1, renderEngine.pageCount)

            Snackbar.make(binding.root, "PDF cargado con éxito", Snackbar.LENGTH_SHORT).show()
        } else {
            binding.rvPdfPages.visibility = View.GONE
            binding.bottomPageBar.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
            supportActionBar?.title = getString(R.string.app_name)
            supportActionBar?.subtitle = null

            AlertDialog.Builder(this)
                .setTitle("Error al abrir PDF")
                .setMessage(getString(R.string.error_loading_pdf))
                .setPositiveButton("Aceptar", null)
                .show()
        }
    }

    private fun updatePageIndicator(current: Int, total: Int) {
        binding.tvPageIndicator.text = getString(R.string.page_format, current, total)
    }

    private fun showJumpPageDialog() {
        if (renderEngine.pageCount <= 0) return

        val dialogBinding = DialogJumpPageBinding.inflate(LayoutInflater.from(this))
        dialogBinding.tilPageNumber.hint = getString(R.string.enter_page_number, renderEngine.pageCount)

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.go) { _, _ ->
                val input = dialogBinding.etPageNumber.text.toString()
                val pageNum = input.toIntOrNull()
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
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir PDF"))
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        result = it.getString(index)
                    }
                }
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
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
                if (currentPdfUri != null) {
                    sharePdf()
                } else {
                    Toast.makeText(this, "Abre un PDF primero para compartirlo", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        renderEngine.close()
    }
}
