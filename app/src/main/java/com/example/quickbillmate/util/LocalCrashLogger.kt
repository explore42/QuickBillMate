package com.example.quickbillmate.util

import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** 一条本地崩溃记录（设置页展示用）。 */
data class CrashRecord(
    val fileName: String,
    val timeMillis: Long,
    val summary: String,
)

/**
 * 本地崩溃日志：崩溃记录完全本地化，不联网、不接入任何第三方 SDK。
 * Application 启动时注册默认未捕获异常处理器，把崩溃堆栈写入应用私有目录
 * `filesDir/crash_logs/`，随后链式调用原处理器（保持系统默认崩溃行为）。
 * 日志仅含系统与异常信息，不包含任何客户/单据/商品等业务数据。
 */
object LocalCrashLogger {
    private const val MAX_FILES = 10
    private const val MAX_TOTAL_BYTES = 1024L * 1024L
    private const val DIR_NAME = "crash_logs"

    private val fileTimeFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val displayTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @Volatile
    private var crashDir: File? = null
    @Volatile
    private var appVersion = ""
    @Volatile
    private var deviceInfo = ""

    fun init(context: Context) {
        val appContext = context.applicationContext
        crashDir = File(appContext.filesDir, DIR_NAME)
        appVersion = runCatching {
            val info = appContext.packageManager.getPackageInfo(appContext.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        }.getOrElse { "" }
        deviceInfo =
            "${Build.MANUFACTURER} ${Build.MODEL} / Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeCrash(thread, throwable)
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            } else {
                // 无前序处理器时确定终止进程，避免残留异常状态
                Process.killProcess(Process.myPid())
            }
        }
    }

    // ---------- 设置页读取 / 清除 / 复制 ----------

    fun listLogs(context: Context): List<CrashRecord> = listLogs(crashDir(context))

    fun clearLogs(context: Context) {
        val dir = crashDir(context)
        dir.listFiles()?.forEach { file -> runCatching { file.delete() } }
    }

    fun fullText(context: Context): String {
        val dir = crashDir(context)
        val sb = StringBuilder()
        listLogs(dir).forEach { record ->
            sb.append("===== ").append(record.fileName).append(" =====\n")
            sb.append(runCatching { File(dir, record.fileName).readText(Charsets.UTF_8) }.getOrElse { "" })
            sb.append("\n\n")
        }
        return sb.toString().trim()
    }

    // ---------- 纯逻辑（JVM 单元测试直接覆盖） ----------

    /** 崩溃文本：仅时间/版本/设备/线程/异常堆栈，不含业务数据。 */
    internal fun formatCrash(
        threadName: String,
        throwable: Throwable,
        timestamp: String,
        version: String,
        device: String,
    ): String = buildString {
        appendLine("时间：$timestamp")
        appendLine("应用版本：$version")
        appendLine("设备：$device")
        appendLine("线程：$threadName")
        appendLine("异常：${throwable.javaClass.name}: ${throwable.message.orEmpty()}")
        appendLine("堆栈：")
        append(throwable.stackTraceToString())
        throwable.cause?.let { cause ->
            appendLine()
            appendLine("Caused by: ${cause.javaClass.name}: ${cause.message.orEmpty()}")
            append(cause.stackTraceToString())
        }
    }

    /** 按修改时间倒序返回崩溃记录；忽略非崩溃文件。 */
    internal fun listLogs(dir: File): List<CrashRecord> {
        val files = dir.listFiles { f ->
            f.isFile && f.name.startsWith("crash_") && f.name.endsWith(".txt")
        } ?: return emptyList()
        return files
            .sortedByDescending { it.lastModified() }
            .map { file ->
                CrashRecord(
                    fileName = file.name,
                    timeMillis = file.lastModified(),
                    summary = readSummary(file),
                )
            }
    }

    /** 保留最近 [maxFiles] 个文件且总大小不超过 [maxBytes]；超限时从最旧文件开始删除。 */
    internal fun pruneLogs(dir: File, maxFiles: Int = MAX_FILES, maxBytes: Long = MAX_TOTAL_BYTES) {
        val files = dir.listFiles { f ->
            f.isFile && f.name.startsWith("crash_") && f.name.endsWith(".txt")
        } ?: return
        val sorted = files.sortedByDescending { it.lastModified() }
        // 先按数量裁剪：删除超出 maxFiles 的最旧文件
        val toDelete = sorted.drop(maxFiles).toMutableList()
        var total = sorted.take(maxFiles).sumOf { it.length() }
        // 再按总大小裁剪：从最旧开始删，直到不超限
        for (file in sorted.take(maxFiles).asReversed()) {
            if (total <= maxBytes) break
            toDelete += file
            total -= file.length()
        }
        toDelete.forEach { file ->
            runCatching { file.delete() }
        }
    }

    // ---------- 内部 ----------

    private fun crashDir(context: Context): File = crashDir ?: File(context.filesDir, DIR_NAME)

    private fun writeCrash(thread: Thread, throwable: Throwable) {
        val dir = crashDir ?: return
        runCatching {
            dir.mkdirs()
            val now = LocalDateTime.now()
            val fileName = "crash_${now.format(fileTimeFormat)}.txt"
            File(dir, fileName).writeText(
                formatCrash(
                    threadName = thread.name,
                    throwable = throwable,
                    timestamp = now.format(displayTimeFormat),
                    version = appVersion,
                    device = deviceInfo,
                ),
                Charsets.UTF_8,
            )
            pruneLogs(dir)
        }
    }

    /** 仪器测试用：写入一条合成崩溃记录，不终止进程。 */
    fun writeLogForTest(throwable: Throwable, context: Context) {
        val dir = crashDir(context)
        runCatching {
            dir.mkdirs()
            val now = LocalDateTime.now()
            val fileName = "crash_${now.format(fileTimeFormat)}.txt"
            File(dir, fileName).writeText(
                formatCrash(
                    threadName = "test",
                    throwable = throwable,
                    timestamp = now.format(displayTimeFormat),
                    version = appVersion,
                    device = deviceInfo,
                ),
                Charsets.UTF_8,
            )
            pruneLogs(dir)
        }
    }

    private fun readSummary(file: File): String {
        val text = runCatching { file.readText(Charsets.UTF_8) }.getOrElse { return "读取失败" }
        return text.lineSequence()
            .firstOrNull { it.startsWith("异常：") }
            ?.removePrefix("异常：")
            ?: text.lineSequence().firstOrNull { it.isNotBlank() } ?: ""
    }
}
