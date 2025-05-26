// app/src/main/java/com/example/novascope/ai/T5DebugHelper.kt
package com.example.novascope.ai

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Debug helper for T5 model issues
 */
object T5DebugHelper {
    private const val TAG = "T5DebugHelper"

    /**
     * Comprehensive T5 model debugging
     */
    fun debugT5Model(context: Context): String {
        val debugInfo = StringBuilder()

        try {
            debugInfo.appendLine("=== T5 Model Debug Information ===")
            debugInfo.appendLine("Timestamp: ${System.currentTimeMillis()}")

            // Check files directory
            val filesDir = context.filesDir
            debugInfo.appendLine("Files directory: ${filesDir.absolutePath}")
            debugInfo.appendLine("Files directory exists: ${filesDir.exists()}")
            debugInfo.appendLine("Files directory readable: ${filesDir.canRead()}")
            debugInfo.appendLine("Files directory writable: ${filesDir.canWrite()}")

            // Check model file
            val modelFile = File(filesDir, ModelFileManager.MODEL_FILE)
            debugInfo.appendLine("\n--- Model File ---")
            debugInfo.appendLine("Model file path: ${modelFile.absolutePath}")
            debugInfo.appendLine("Model file exists: ${modelFile.exists()}")
            if (modelFile.exists()) {
                debugInfo.appendLine("Model file size: ${modelFile.length()} bytes")
                debugInfo.appendLine("Model file readable: ${modelFile.canRead()}")
                debugInfo.appendLine("Model file last modified: ${modelFile.lastModified()}")

                // Check file header to see if it's a valid TensorFlow Lite file
                try {
                    val header = modelFile.readBytes().take(8)
                    debugInfo.appendLine("Model file header: ${header.joinToString(" ") { "%02x".format(it) }}")

                    // Check for TensorFlow Lite magic number (TFL3)
                    val tflMagic = byteArrayOf(0x54, 0x46, 0x4C, 0x33)
                    val hasCorrectHeader = header.take(4).toByteArray().contentEquals(tflMagic)
                    debugInfo.appendLine("Has TensorFlow Lite header: $hasCorrectHeader")
                } catch (e: Exception) {
                    debugInfo.appendLine("Error reading model file header: ${e.message}")
                }
            }

            // Check vocabulary file
            val vocabFile = File(filesDir, ModelFileManager.VOCAB_FILE)
            debugInfo.appendLine("\n--- Vocabulary File ---")
            debugInfo.appendLine("Vocab file path: ${vocabFile.absolutePath}")
            debugInfo.appendLine("Vocab file exists: ${vocabFile.exists()}")
            if (vocabFile.exists()) {
                debugInfo.appendLine("Vocab file size: ${vocabFile.length()} bytes")
                debugInfo.appendLine("Vocab file readable: ${vocabFile.canRead()}")

                try {
                    val lines = vocabFile.readLines()
                    debugInfo.appendLine("Vocab file lines: ${lines.size}")
                    debugInfo.appendLine("First 5 vocab entries: ${lines.take(5)}")
                } catch (e: Exception) {
                    debugInfo.appendLine("Error reading vocab file: ${e.message}")
                }
            }

            // Check storage space
            debugInfo.appendLine("\n--- Storage Information ---")
            val totalSpace = filesDir.totalSpace
            val freeSpace = filesDir.freeSpace
            val usableSpace = filesDir.usableSpace
            debugInfo.appendLine("Total space: ${totalSpace / 1024 / 1024} MB")
            debugInfo.appendLine("Free space: ${freeSpace / 1024 / 1024} MB")
            debugInfo.appendLine("Usable space: ${usableSpace / 1024 / 1024} MB")

            // Check TensorFlow Lite availability
            debugInfo.appendLine("\n--- TensorFlow Lite Information ---")
            try {
                val interpreterClass = Class.forName("org.tensorflow.lite.Interpreter")
                debugInfo.appendLine("TensorFlow Lite Interpreter class found: ${interpreterClass.name}")

                // Try to create a simple interpreter to test the library
                try {
                    val dummyModel = createDummyTFLiteModel()
                    val interpreter = org.tensorflow.lite.Interpreter(java.nio.ByteBuffer.wrap(dummyModel))
                    debugInfo.appendLine("TensorFlow Lite interpreter creation: SUCCESS")
                    interpreter.close()
                } catch (e: Exception) {
                    debugInfo.appendLine("TensorFlow Lite interpreter creation failed: ${e.message}")
                }

            } catch (e: ClassNotFoundException) {
                debugInfo.appendLine("TensorFlow Lite Interpreter class NOT found")
            } catch (e: Exception) {
                debugInfo.appendLine("Error checking TensorFlow Lite: ${e.message}")
            }

            // Network connectivity check
            debugInfo.appendLine("\n--- Network Information ---")
            try {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
                val activeNetwork = connectivityManager.activeNetworkInfo
                debugInfo.appendLine("Network available: ${activeNetwork?.isConnected == true}")
                debugInfo.appendLine("Network type: ${activeNetwork?.typeName}")
            } catch (e: Exception) {
                debugInfo.appendLine("Error checking network: ${e.message}")
            }

            // Check ModelFileManager state
            debugInfo.appendLine("\n--- ModelFileManager State ---")
            try {
                val modelFileManager = ModelFileManager(context)
                debugInfo.appendLine("Model imported: ${modelFileManager.isModelImported}")
                debugInfo.appendLine("Import state: ${modelFileManager.importState.value}")
            } catch (e: Exception) {
                debugInfo.appendLine("Error checking ModelFileManager: ${e.message}")
            }

            debugInfo.appendLine("\n=== End Debug Information ===")

        } catch (e: Exception) {
            debugInfo.appendLine("ERROR in debug helper: ${e.message}")
            debugInfo.appendLine("Stack trace: ${e.stackTraceToString()}")
        }

        val result = debugInfo.toString()
        Log.d(TAG, result)
        return result
    }

    /**
     * Create a minimal dummy TensorFlow Lite model for testing
     */
    private fun createDummyTFLiteModel(): ByteArray {
        // This creates a minimal valid TensorFlow Lite model
        // Just enough to test if the TensorFlow Lite library is working

        val header = byteArrayOf(
            0x54, 0x46, 0x4C, 0x33, // TFL3 magic number
            0x00, 0x00, 0x00, 0x00, // Version
            0x10, 0x00, 0x00, 0x00, // Schema offset
            0x20, 0x00, 0x00, 0x00, // Model offset
        )

        // Create a minimal model (just header + some padding)
        val model = ByteArray(64)
        System.arraycopy(header, 0, model, 0, header.size)

        return model
    }

    /**
     * Test T5 model initialization step by step
     */
    fun testT5Initialization(context: Context): String {
        val testResult = StringBuilder()

        try {
            testResult.appendLine("=== T5 Initialization Test ===")

            // Step 1: Test ModelFileManager creation
            testResult.appendLine("Step 1: Creating ModelFileManager...")
            val modelFileManager = ModelFileManager(context)
            testResult.appendLine("✓ ModelFileManager created successfully")

            // Step 2: Test vocabulary creation
            testResult.appendLine("Step 2: Testing vocabulary creation...")
            val vocabFile = File(context.filesDir, ModelFileManager.VOCAB_FILE)
            if (vocabFile.exists()) {
                testResult.appendLine("✓ Vocabulary file exists")
            } else {
                testResult.appendLine("⚠ Vocabulary file does not exist")
            }

            // Step 3: Test ArticleSummarizer creation
            testResult.appendLine("Step 3: Creating ArticleSummarizer...")
            val summarizer = ArticleSummarizer(context)
            testResult.appendLine("✓ ArticleSummarizer created successfully")

            // Step 4: Test model import status
            testResult.appendLine("Step 4: Checking model import status...")
            val isImported = summarizer.isModelImported
            testResult.appendLine("Model imported: $isImported")

            testResult.appendLine("=== Test Complete ===")

        } catch (e: Exception) {
            testResult.appendLine("ERROR in initialization test: ${e.message}")
            testResult.appendLine("Stack trace: ${e.stackTraceToString()}")
        }

        val result = testResult.toString()
        Log.d(TAG, result)
        return result
    }
}