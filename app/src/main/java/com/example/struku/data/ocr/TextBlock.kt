package com.example.struku.data.ocr

import android.graphics.Point
import android.graphics.Rect

/**
 * Domain model for a text block recognized in an image
 */
data class TextBlock(
    val text: String,
    val boundingBox: Rect?,
    val cornerPoints: Array<Point>?,
    val lines: List<TextLine>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextBlock

        if (text != other.text) return false
        if (boundingBox != other.boundingBox) return false
        if (cornerPoints != null) {
            if (other.cornerPoints == null) return false
            if (!cornerPoints.contentEquals(other.cornerPoints)) return false
        } else if (other.cornerPoints != null) return false
        if (lines != other.lines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (boundingBox?.hashCode() ?: 0)
        result = 31 * result + (cornerPoints?.contentHashCode() ?: 0)
        result = 31 * result + lines.hashCode()
        return result
    }
}

/**
 * Domain model for a line of text
 */
data class TextLine(
    val text: String,
    val boundingBox: Rect?,
    val cornerPoints: Array<Point>?,
    val elements: List<TextElement>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextLine

        if (text != other.text) return false
        if (boundingBox != other.boundingBox) return false
        if (cornerPoints != null) {
            if (other.cornerPoints == null) return false
            if (!cornerPoints.contentEquals(other.cornerPoints)) return false
        } else if (other.cornerPoints != null) return false
        if (elements != other.elements) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (boundingBox?.hashCode() ?: 0)
        result = 31 * result + (cornerPoints?.contentHashCode() ?: 0)
        result = 31 * result + elements.hashCode()
        return result
    }
}

/**
 * Domain model for a text element (word)
 */
data class TextElement(
    val text: String,
    val boundingBox: Rect?,
    val cornerPoints: Array<Point>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TextElement

        if (text != other.text) return false
        if (boundingBox != other.boundingBox) return false
        if (cornerPoints != null) {
            if (other.cornerPoints == null) return false
            if (!cornerPoints.contentEquals(other.cornerPoints)) return false
        } else if (other.cornerPoints != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + (boundingBox?.hashCode() ?: 0)
        result = 31 * result + (cornerPoints?.contentHashCode() ?: 0)
        return result
    }
}