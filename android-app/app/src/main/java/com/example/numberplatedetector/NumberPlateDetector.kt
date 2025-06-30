package com.example.numberplatedetector

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object NumberPlateDetector {
    // Regex pattern for Saudi Arabia license plates
    // Format: 1234 ABC (4 numbers followed by 3 letters)
    private val SAUDI_PLATE_PATTERN = Regex("""(\d{4})\s*([A-Za-z]{3})""")

    fun detectPlate(bitmap: Bitmap, callback: (plateText: String?, error: String?) -> Unit) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    var detectedPlate: String? = null
                    
                    // Process each text block to find plate pattern
                    for (block in visionText.textBlocks) {
                        val matchResult = SAUDI_PLATE_PATTERN.find(block.text)
                        if (matchResult != null) {
                            // Format the plate number according to Saudi standard
                            val numbers = matchResult.groupValues[1]
                            val letters = matchResult.groupValues[2].uppercase()
                            detectedPlate = "$numbers $letters"
                            break
                        }
                    }

                    if (detectedPlate != null) {
                        callback(detectedPlate, null)
                    } else {
                        callback(null, "No valid Saudi plate pattern found")
                    }
                }
                .addOnFailureListener { e ->
                    callback(null, "Text recognition failed: ${e.message}")
                }
        } catch (e: Exception) {
            callback(null, "Image processing failed: ${e.message}")
        }
    }

    // Helper function to validate Saudi plate format
    private fun isValidSaudiPlate(plate: String): Boolean {
        return SAUDI_PLATE_PATTERN.matches(plate)
    }

    // Helper function to clean and format the detected text
    private fun cleanPlateText(text: String): String {
        // Remove any special characters and extra spaces
        return text.replace(Regex("[^A-Za-z0-9]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")
            .uppercase()
    }
}
