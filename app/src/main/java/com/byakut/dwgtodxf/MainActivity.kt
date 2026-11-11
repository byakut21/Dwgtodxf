package com.byakut.dwgtodxf

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.byakut.dwgtodxf.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private enum class Mode(val from: String, val to: String) {
        DWG_TO_DXF("dwg", "dxf"),
        DXF_TO_DWG("dxf", "dwg"),
    }

    private lateinit var b: ActivityMainBinding
    private val main = Handler(Looper.getMainLooper())
    private var mode = Mode.DWG_TO_DXF
    private var busy = false
    private var lastOutput: File? = null

    private val pickFile =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) startConversion(uri, mode)
        }

    private val saveFile =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            val out = lastOutput
            if (uri != null && out != null) saveTo(uri, out)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        b.btnDwgToDxf.setOnClickListener { pick(Mode.DWG_TO_DXF) }
        b.btnDxfToDwg.setOnClickListener { pick(Mode.DXF_TO_DWG) }
        b.btnSave.setOnClickListener { lastOutput?.let { saveFile.launch(it.name) } }
        b.btnShare.setOnClickListener { lastOutput?.let { share(it) } }
        showResult(null)

        if (savedInstanceState == null) handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncoming(intent)
    }

    /** File opened from a file manager or shared from another app. */
    private fun handleIncoming(intent: Intent?) {
        val uri: Uri? = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND ->
                if (Build.VERSION.SDK_INT >= 33)
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM)
            else -> null
        }
        if (uri == null) return
        val name = displayName(uri).lowercase()
        val m = when {
            name.endsWith(".dwg") -> Mode.DWG_TO_DXF
            name.endsWith(".dxf") -> Mode.DXF_TO_DWG
            else -> detectByContent(uri)
        }
        if (m == null) {
            setStatus(getString(R.string.err_unknown_type), error = true)
            return
        }
        startConversion(uri, m)
    }

    private fun pick(m: Mode) {
        if (busy) return
        mode = m
        pickFile.launch(arrayOf("*/*"))
    }

    private fun startConversion(uri: Uri, m: Mode) {
        if (busy) return
        val inName = displayName(uri)
        val base = inName.substringBeforeLast('.', inName).ifBlank { "cizim" }

        busy = true
        showResult(null)
        b.progress.visibility = View.VISIBLE
        setButtonsEnabled(false)
        setStatus(getString(R.string.converting, inName), error = false)

        // LibreDWG can recurse deeply on big drawings: use a thread with a large stack.
        Thread(null, {
            val result = runCatching { convert(uri, m, base) }
            main.post {
                busy = false
                b.progress.visibility = View.GONE
                setButtonsEnabled(true)
                result.fold(
                    onSuccess = { out ->
                        lastOutput = out
                        setStatus(getString(R.string.done, out.name, humanSize(out.length())), error = false)
                        showResult(out)
                    },
                    onFailure = { e -> setStatus(e.message ?: getString(R.string.err_generic), error = true) }
                )
            }
        }, "convert", 256L * 1024 * 1024).start()
    }

    /** Runs on the worker thread. Returns the converted file in cache/out/. */
    private fun convert(uri: Uri, m: Mode, base: String): File {
        val work = File(cacheDir, "work").apply { deleteRecursively(); mkdirs() }
        val outDir = File(cacheDir, "out").apply { deleteRecursively(); mkdirs() }
        val input = File(work, "input.${m.from}")
        contentResolver.openInputStream(uri).use { ins ->
            requireNotNull(ins) { getString(R.string.err_read) }
            input.outputStream().use { ins.copyTo(it) }
        }
        if (input.length() == 0L) error(getString(R.string.err_empty))

        val header = input.inputStream().use { s -> ByteArray(32).also { s.read(it) } }
        val head = String(header, Charsets.ISO_8859_1)
        when (m) {
            Mode.DWG_TO_DXF -> if (!head.startsWith("AC1")) error(getString(R.string.err_not_dwg))
            Mode.DXF_TO_DWG -> if (head.startsWith("AC1")) error(getString(R.string.err_not_dxf))
        }

        val output = File(outDir, "$base.${m.to}")
        val code = when (m) {
            Mode.DWG_TO_DXF -> Converter.dwgToDxf(input.absolutePath, output.absolutePath)
            Mode.DXF_TO_DWG -> Converter.dxfToDwg(input.absolutePath, output.absolutePath)
        }
        input.delete()
        if (code != 0 || !output.exists() || output.length() == 0L) {
            output.delete()
            error(
                if (code == -1) getString(R.string.err_write)
                else getString(R.string.err_convert, String.format("0x%X", code))
            )
        }
        return output
    }

    private fun saveTo(uri: Uri, file: File) {
        runCatching {
            contentResolver.openOutputStream(uri, "wt").use { os ->
                requireNotNull(os)
                file.inputStream().use { it.copyTo(os) }
            }
        }.onSuccess {
            setStatus(getString(R.string.saved, displayName(uri)), error = false)
        }.onFailure {
            setStatus(getString(R.string.err_save), error = true)
        }
    }

    private fun share(file: File) {
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(send, getString(R.string.share)))
    }

    private fun detectByContent(uri: Uri): Mode? = runCatching {
        contentResolver.openInputStream(uri)?.use { s ->
            val buf = ByteArray(64)
            val n = s.read(buf)
            val head = String(buf, 0, maxOf(n, 0), Charsets.ISO_8859_1)
            when {
                head.startsWith("AC1") -> Mode.DWG_TO_DXF
                head.contains("SECTION") || head.startsWith("AutoCAD Binary DXF") -> Mode.DXF_TO_DWG
                else -> null
            }
        }
    }.getOrNull()

    private fun displayName(uri: Uri): String {
        runCatching {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getString(0)
                    if (!n.isNullOrBlank()) return n
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "cizim"
    }

    private fun setButtonsEnabled(on: Boolean) {
        b.btnDwgToDxf.isEnabled = on
        b.btnDxfToDwg.isEnabled = on
    }

    private fun showResult(file: File?) {
        b.resultRow.visibility = if (file != null) View.VISIBLE else View.GONE
    }

    private fun setStatus(text: String, error: Boolean) {
        b.status.text = text
        b.status.setTextColor(
            getColor(if (error) R.color.error else R.color.text_secondary)
        )
    }

    private fun humanSize(bytes: Long): String = when {
        bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / 1048576.0)
        bytes >= 1024 -> String.format("%.0f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
