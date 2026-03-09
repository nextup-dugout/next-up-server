package com.nextup.infrastructure.adapter.pdf

import com.lowagie.text.Document
import com.lowagie.text.Element
import com.lowagie.text.Font
import com.lowagie.text.PageSize
import com.lowagie.text.Paragraph
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.BaseFont
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import com.nextup.core.port.PdfGeneratorPort
import com.nextup.core.service.game.dto.BatterScoresheetDto
import com.nextup.core.service.game.dto.PitcherScoresheetDto
import com.nextup.core.service.game.dto.ScoresheetDto
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.awt.Color
import java.io.ByteArrayOutputStream

/**
 * OpenPDF 기반 공식 기록지 PDF 생성기
 *
 * 한글 지원을 위해 내장 폰트를 사용합니다.
 */
@Component
@Primary
class OpenPdfScoresheetGenerator : PdfGeneratorPort {
    private val baseFont: BaseFont =
        BaseFont.createFont(
            BaseFont.HELVETICA,
            BaseFont.CP1252,
            BaseFont.NOT_EMBEDDED,
        )

    private val titleFont = Font(baseFont, 16f, Font.BOLD)
    private val headerFont = Font(baseFont, 11f, Font.BOLD)
    private val cellFont = Font(baseFont, 9f, Font.NORMAL)
    private val sectionFont = Font(baseFont, 12f, Font.BOLD)
    private val infoFont = Font(baseFont, 10f, Font.NORMAL)

    private val headerBgColor = Color(220, 220, 220)

    override fun generateScoresheetPdf(scoresheet: ScoresheetDto): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val document = Document(PageSize.A4, 36f, 36f, 36f, 36f)
        PdfWriter.getInstance(document, outputStream)

        document.open()

        addTitle(document, scoresheet)
        addGameInfo(document, scoresheet)
        addInningScores(document, scoresheet)
        addBattingRecords(document, scoresheet)
        addPitchingRecords(document, scoresheet)
        addKeyEvents(document, scoresheet)

        document.close()
        return outputStream.toByteArray()
    }

    private fun addTitle(
        document: Document,
        scoresheet: ScoresheetDto,
    ) {
        val title =
            Paragraph(
                "Official Scoresheet - ${scoresheet.gameInfo.competitionName}",
                titleFont,
            )
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 10f
        document.add(title)
    }

    private fun addGameInfo(
        document: Document,
        scoresheet: ScoresheetDto,
    ) {
        val info = scoresheet.gameInfo
        val section = Paragraph("Game Info", sectionFont)
        section.spacingBefore = 10f
        section.spacingAfter = 5f
        document.add(section)

        val table = PdfPTable(2)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(1f, 3f))

        addInfoRow(table, "Date", info.scheduledAt.toString())
        addInfoRow(table, "Location", "${info.location ?: "TBD"} ${info.fieldName?.let { "($it)" } ?: ""}")
        addInfoRow(table, "Status", info.status)
        addInfoRow(
            table,
            "Result",
            "${scoresheet.teams.away.teamName} ${scoresheet.teams.away.totalScore}" +
                " vs " +
                "${scoresheet.teams.home.teamName} ${scoresheet.teams.home.totalScore}",
        )

        table.setSpacingAfter(10f)
        document.add(table)
    }

    private fun addInfoRow(
        table: PdfPTable,
        label: String,
        value: String,
    ) {
        val labelCell = PdfPCell(Phrase(label, headerFont))
        labelCell.backgroundColor = headerBgColor
        labelCell.setPadding(4f)
        table.addCell(labelCell)

        val valueCell = PdfPCell(Phrase(value, infoFont))
        valueCell.setPadding(4f)
        table.addCell(valueCell)
    }

    private fun addInningScores(
        document: Document,
        scoresheet: ScoresheetDto,
    ) {
        val section = Paragraph("Inning Scores", sectionFont)
        section.spacingBefore = 10f
        section.spacingAfter = 5f
        document.add(section)

        val innings = scoresheet.inningScores.innings
        val cols = innings + 4 // Team + innings + R + H + E
        val table = PdfPTable(cols)
        table.widthPercentage = 100f

        // Header row
        addHeaderCell(table, "Team")
        for (i in 1..innings) {
            addHeaderCell(table, i.toString())
        }
        addHeaderCell(table, "R")
        addHeaderCell(table, "H")
        addHeaderCell(table, "E")

        // Away row
        addCell(table, scoresheet.teams.away.teamName)
        scoresheet.inningScores.awayScores.forEach { addCell(table, it.toString()) }
        addCell(table, scoresheet.teams.away.totalScore.toString())
        addCell(table, scoresheet.teams.away.totalHits.toString())
        addCell(table, scoresheet.teams.away.totalErrors.toString())

        // Home row
        addCell(table, scoresheet.teams.home.teamName)
        scoresheet.inningScores.homeScores.forEach { addCell(table, it.toString()) }
        addCell(table, scoresheet.teams.home.totalScore.toString())
        addCell(table, scoresheet.teams.home.totalHits.toString())
        addCell(table, scoresheet.teams.home.totalErrors.toString())

        table.setSpacingAfter(10f)
        document.add(table)
    }

    private fun addBattingRecords(
        document: Document,
        scoresheet: ScoresheetDto,
    ) {
        addBattingTable(document, scoresheet.teams.away.teamName, scoresheet.battingRecords.away)
        addBattingTable(document, scoresheet.teams.home.teamName, scoresheet.battingRecords.home)
    }

    private fun addBattingTable(
        document: Document,
        teamName: String,
        batters: List<BatterScoresheetDto>,
    ) {
        val section = Paragraph("Batting - $teamName", sectionFont)
        section.spacingBefore = 10f
        section.spacingAfter = 5f
        document.add(section)

        val headers = listOf("Player", "Pos", "PA", "AB", "R", "H", "2B", "3B", "HR", "RBI", "BB", "SO", "AVG")
        val table = PdfPTable(headers.size)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(3f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1.5f))

        headers.forEach { addHeaderCell(table, it) }

        batters.forEach { batter ->
            addCell(table, batter.name)
            addCell(table, batter.position)
            addCell(table, batter.plateAppearances.toString())
            addCell(table, batter.atBats.toString())
            addCell(table, batter.runs.toString())
            addCell(table, batter.hits.toString())
            addCell(table, batter.doubles.toString())
            addCell(table, batter.triples.toString())
            addCell(table, batter.homeRuns.toString())
            addCell(table, batter.rbis.toString())
            addCell(table, batter.walks.toString())
            addCell(table, batter.strikeouts.toString())
            addCell(table, batter.avg)
        }

        table.setSpacingAfter(10f)
        document.add(table)
    }

    private fun addPitchingRecords(
        document: Document,
        scoresheet: ScoresheetDto,
    ) {
        addPitchingTable(document, scoresheet.teams.away.teamName, scoresheet.pitchingRecords.away)
        addPitchingTable(document, scoresheet.teams.home.teamName, scoresheet.pitchingRecords.home)
    }

    private fun addPitchingTable(
        document: Document,
        teamName: String,
        pitchers: List<PitcherScoresheetDto>,
    ) {
        val section = Paragraph("Pitching - $teamName", sectionFont)
        section.spacingBefore = 10f
        section.spacingAfter = 5f
        document.add(section)

        val headers = listOf("Player", "IP", "H", "R", "ER", "BB", "SO", "HR", "Dec", "ERA")
        val table = PdfPTable(headers.size)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(3f, 1.5f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1.5f))

        headers.forEach { addHeaderCell(table, it) }

        pitchers.forEach { pitcher ->
            addCell(table, pitcher.name)
            addCell(table, pitcher.inningsPitched)
            addCell(table, pitcher.hitsAllowed.toString())
            addCell(table, pitcher.runsAllowed.toString())
            addCell(table, pitcher.earnedRuns.toString())
            addCell(table, pitcher.walks.toString())
            addCell(table, pitcher.strikeouts.toString())
            addCell(table, pitcher.homeRunsAllowed.toString())
            addCell(table, pitcher.decision ?: "-")
            addCell(table, pitcher.era)
        }

        table.setSpacingAfter(10f)
        document.add(table)
    }

    private fun addKeyEvents(
        document: Document,
        scoresheet: ScoresheetDto,
    ) {
        if (scoresheet.keyEvents.isEmpty()) return

        val section = Paragraph("Key Events", sectionFont)
        section.spacingBefore = 10f
        section.spacingAfter = 5f
        document.add(section)

        val table = PdfPTable(3)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(1f, 2f, 5f))

        addHeaderCell(table, "Inning")
        addHeaderCell(table, "Time")
        addHeaderCell(table, "Description")

        scoresheet.keyEvents.take(20).forEach { event ->
            addCell(table, event.inning)
            addCell(table, event.timestamp)
            addCell(table, event.description)
        }

        document.add(table)
    }

    private fun addHeaderCell(
        table: PdfPTable,
        text: String,
    ) {
        val cell = PdfPCell(Phrase(text, headerFont))
        cell.backgroundColor = headerBgColor
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.setPadding(3f)
        table.addCell(cell)
    }

    private fun addCell(
        table: PdfPTable,
        text: String,
    ) {
        val cell = PdfPCell(Phrase(text, cellFont))
        cell.horizontalAlignment = Element.ALIGN_CENTER
        cell.setPadding(2f)
        table.addCell(cell)
    }
}
