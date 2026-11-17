package fr.isep.campuspass.pdf

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import fr.isep.campuspass.domain.Event
import fr.isep.campuspass.domain.Ticket
import fr.isep.campuspass.domain.Transaction
import fr.isep.campuspass.domain.formatEuros
import fr.isep.campuspass.qr.QrCodes
import fr.isep.campuspass.ui.formatDateTime
import java.io.File

/** Generates a PDF e-ticket (one page per ticket, with the QR code) followed by the payment receipt. */
object TicketPdfGenerator {
    private const val WIDTH = 595 // A4 in PostScript points
    private const val HEIGHT = 842
    private const val NAVY = 0xFF16133A.toInt()
    private const val CORAL = 0xFFFF5A5F.toInt()
    private const val GREY = 0xFF6B6880.toInt()

    fun create(context: Context, tickets: List<Ticket>, event: Event, transaction: Transaction, buyer: String): File {
        val document = PdfDocument()
        tickets.forEachIndexed { index, ticket ->
            val page = document.startPage(PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, index + 1).create())
            drawTicket(page.canvas, ticket, event, index + 1, tickets.size)
            document.finishPage(page)
        }
        val receipt = document.startPage(PdfDocument.PageInfo.Builder(WIDTH, HEIGHT, tickets.size + 1).create())
        drawReceipt(receipt.canvas, transaction, event, buyer)
        document.finishPage(receipt)

        val dir = File(context.cacheDir, "pdf").apply { mkdirs() }
        val file = File(dir, "CampusPass-${transaction.id}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun paint(color: Int, size: Float, bold: Boolean = false) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        typeface = Typeface.create(Typeface.SANS_SERIF, if (bold) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun header(canvas: Canvas, subtitle: String) {
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), 120f, paint(NAVY, 0f))
        canvas.drawText("CampusPass", 40f, 62f, paint(0xFFFFFFFF.toInt(), 30f, bold = true))
        canvas.drawText(subtitle, 40f, 92f, paint(CORAL, 14f, bold = true))
    }

    private fun drawTicket(canvas: Canvas, ticket: Ticket, event: Event, number: Int, total: Int) {
        header(canvas, "E-TICKET  ·  $number / $total")
        var y = 180f
        canvas.drawText(event.title, 40f, y, paint(NAVY, 24f, bold = true)); y += 34f
        canvas.drawText(formatDateTime(event.startsAt), 40f, y, paint(GREY, 15f)); y += 22f
        canvas.drawText(event.venue, 40f, y, paint(GREY, 15f)); y += 22f
        canvas.drawText("Organised by ${event.organizer}", 40f, y, paint(GREY, 15f)); y += 40f

        val qr = QrCodes.generate(ticket.code, 600)
        val qrSize = 300f
        val left = (WIDTH - qrSize) / 2
        canvas.drawRoundRect(RectF(left - 16, y - 16, left + qrSize + 16, y + qrSize + 16), 16f, 16f, paint(0xFFF2F1F7.toInt(), 0f))
        canvas.drawBitmap(qr, null, RectF(left, y, left + qrSize, y + qrSize), null)
        y += qrSize + 50f

        val center = paint(NAVY, 16f, bold = true).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText(ticket.holderName, WIDTH / 2f, y, center); y += 22f
        canvas.drawText("Ticket ${ticket.id}  ·  General admission", WIDTH / 2f, y, paint(GREY, 13f).apply { textAlign = Paint.Align.CENTER })
        y += 60f
        canvas.drawText("Single-use ticket: it is invalidated as soon as it is scanned at the entrance.", 40f, y, paint(GREY, 11f))
        canvas.drawText("The QR code is cryptographically signed. Do not share it.", 40f, y + 16f, paint(GREY, 11f))
    }

    private fun drawReceipt(canvas: Canvas, tx: Transaction, event: Event, buyer: String) {
        header(canvas, "PAYMENT RECEIPT")
        var y = 180f
        val label = paint(GREY, 13f)
        val value = paint(NAVY, 15f, bold = true)
        fun row(name: String, text: String) {
            canvas.drawText(name, 40f, y, label)
            canvas.drawText(text, 220f, y, value)
            y += 32f
        }
        row("Order", tx.id)
        row("Buyer", buyer)
        row("Event", event.title)
        row("Date of event", formatDateTime(event.startsAt))
        row("Quantity", "${tx.quantity} × ${event.priceCents.formatEuros()}")
        row("Payment method", tx.method.label)
        row("Status", tx.status.name)
        y += 10f
        canvas.drawLine(40f, y, WIDTH - 40f, y, paint(0xFFDDDBE8.toInt(), 0f).apply { strokeWidth = 1.5f }); y += 40f
        canvas.drawText("TOTAL", 40f, y, paint(NAVY, 18f, bold = true))
        canvas.drawText(tx.amountCents.formatEuros(), WIDTH - 40f, y, paint(CORAL, 22f, bold = true).apply { textAlign = Paint.Align.RIGHT })
        y += 60f
        canvas.drawText("Demo app for the ISEP II.3510 course — Google Pay TEST environment, no real money was charged.", 40f, y, paint(GREY, 10f))
    }

    /** Opens the PDF in a viewer, or falls back to the share sheet if no viewer is installed. */
    fun open(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val view = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(view)
        } catch (e: ActivityNotFoundException) {
            val share = Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(Intent.createChooser(share, "Share your ticket").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
