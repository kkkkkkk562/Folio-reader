package com.example.data.sample

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.example.data.model.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object SampleBooksGenerator {

    suspend fun generateSampleBooksIfEmpty(context: Context): List<BookEntity> = withContext(Dispatchers.IO) {
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }

        val artOfWarFile = File(booksDir, "art_of_war.pdf")
        val aliceFile = File(booksDir, "alice_in_wonderland.pdf")

        val generatedList = mutableListOf<BookEntity>()

        if (!artOfWarFile.exists()) {
            createArtOfWarPdf(artOfWarFile)
            val coverPath = renderFirstPageCover(artOfWarFile, File(coversDir, "art_of_war_cover.png"))
            generatedList.add(
                BookEntity(
                    title = "The Art of War",
                    author = "Sun Tzu",
                    filePath = artOfWarFile.absolutePath,
                    coverPath = coverPath,
                    pageCount = 5,
                    currentPage = 0,
                    percentRead = 0,
                    fileSizeFormatted = "48 KB",
                    isFavorite = true
                )
            )
        }

        if (!aliceFile.exists()) {
            createAlicePdf(aliceFile)
            val coverPath = renderFirstPageCover(aliceFile, File(coversDir, "alice_cover.png"))
            generatedList.add(
                BookEntity(
                    title = "Alice's Adventures in Wonderland",
                    author = "Lewis Carroll",
                    filePath = aliceFile.absolutePath,
                    coverPath = coverPath,
                    pageCount = 5,
                    currentPage = 0,
                    percentRead = 0,
                    fileSizeFormatted = "54 KB",
                    isFavorite = false
                )
            )
        }

        val mangaFile = File(booksDir, "shadow_blade_manga.pdf")
        if (!mangaFile.exists()) {
            createMangaPdf(mangaFile)
            val coverPath = renderFirstPageCover(mangaFile, File(coversDir, "manga_cover.png"))
            generatedList.add(
                BookEntity(
                    title = "Shadow Blade: Shinobi Tales",
                    author = "Kenji Sato • Manga Edition",
                    filePath = mangaFile.absolutePath,
                    coverPath = coverPath,
                    pageCount = 4,
                    currentPage = 0,
                    percentRead = 0,
                    fileSizeFormatted = "58 KB",
                    isFavorite = true,
                    isManga = true
                )
            )
        }

        generatedList
    }

    private fun renderFirstPageCover(pdfFile: File, destCoverFile: File): String? {
        return try {
            val pfd = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            if (renderer.pageCount > 0) {
                val page = renderer.openPage(0)
                val width = page.width
                val height = page.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                renderer.close()
                pfd.close()

                FileOutputStream(destCoverFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }
                destCoverFile.absolutePath
            } else {
                renderer.close()
                pfd.close()
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createArtOfWarPdf(file: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val pagesContent = listOf(
            PageSpec(
                title = "THE ART OF WAR",
                subtitle = "By Sun Tzu • Translated by Lionel Giles",
                paragraphs = listOf(
                    "PREFACE TO THE MILITARY TREATISE",
                    "The Art of War is of vital importance to the State. It is a matter of life and death, a road either to safety or to ruin. Hence it is a subject of inquiry which can on no account be neglected.",
                    "The moral law causes the people to be in complete accord with their ruler, so that they will follow him regardless of their lives, undismayed by any danger.",
                    "Heaven signifies night and day, cold and heat, times and seasons. Earth comprises distances, great and small; danger and security; open ground and narrow passes; the chances of life and death.",
                    "The Commander stands for the virtues of wisdom, sincerely, benevolence, strictness and courage. By method and discipline are to be understood the marshaling of the army in its proper subdivisions, the graduations of rank among the officers, the maintenance of roads by which supplies may reach the army, and the control of military expenditure."
                )
            ),
            PageSpec(
                title = "CHAPTER I: LAYING PLANS",
                subtitle = "The Calculation of Victory",
                paragraphs = listOf(
                    "1. Sun Tzu said: The art of war, then, is governed by five constant factors, to be taken into account in one's deliberations, when seeking to determine the conditions obtaining in the field.",
                    "2. These are: (1) The Moral Law; (2) Heaven; (3) Earth; (4) The Commander; (5) Method and discipline.",
                    "3. These five heads should be familiar to every general: he who knows them will be victorious; he who knows them not will fail.",
                    "4. Therefore, in your deliberations, when seeking to determine the military conditions, let them be made the basis of a comparison, in this wise:—",
                    "(a) Which of the two sovereigns is imbued with the Moral law?",
                    "(b) Which of the two generals has most ability?",
                    "(c) With whom lie the advantages derived from Heaven and Earth?",
                    "(d) On which side is discipline most rigorously enforced?",
                    "(e) Which army is stronger?",
                    "5. All warfare is based on deception. Hence, when able to attack, we must seem unable; when using our forces, we must seem inactive; when we are near, we must make the enemy believe we are far away."
                )
            ),
            PageSpec(
                title = "CHAPTER II: WAGING WAR",
                subtitle = "The Cost and Momentum of Campaigning",
                paragraphs = listOf(
                    "1. Sun Tzu said: In the operations of war, where there are in the field a thousand swift chariots, as many heavy chariots, and a hundred thousand mail-clad soldiers, with provisions enough to carry them a thousand li, the expenditure at home and at the front, including entertainment of guests, small items such as glue and paint, and sums spent on chariots and armor, will reach the total of a thousand ounces of silver per day. Such is the cost of raising an army of 100,000 men.",
                    "2. When you engage in actual fighting, if victory is long in coming, then men's weapons will grow dull and their ardor will be damped. If you lay siege to a town, you will exhaust your strength.",
                    "3. Again, if the campaign is protracted, the resources of the State will not be equal to the strain.",
                    "4. Now, when your weapons are dulled, your ardor damped, your strength exhausted and your treasure spent, other chieftains will spring up to take advantage of your extremity. Then no man, however wise, will be able to avert the consequences that must ensue.",
                    "5. Thus, though we have heard of stupid haste in war, cleverness has never been seen associated with long delays."
                )
            ),
            PageSpec(
                title = "CHAPTER III: ATTACK BY STRATAGEM",
                subtitle = "Supreme Excellence in Warfare",
                paragraphs = listOf(
                    "1. Sun Tzu said: In the practical art of war, the best thing of all is to take the enemy's country whole and intact; to shatter and destroy it is not so good. So, too, it is better to recapture an army entire than to destroy it, to capture a regiment, a detachment or a company entire than to destroy them.",
                    "2. Hence to fight and conquer in all your battles is not supreme excellence; supreme excellence consists in breaking the enemy's resistance without fighting.",
                    "3. Thus the highest form of generalship is to balk the enemy's plans; the next best is to prevent the junction of the enemy's forces; the next in order is to attack the enemy's army in the field; and the worst policy of all is to besiege walled cities.",
                    "4. If you know the enemy and know yourself, you need not fear the result of a hundred battles.",
                    "5. If you know yourself but not the enemy, for every victory gained you will also suffer a defeat. If you know neither the enemy nor yourself, you will succumb in every battle."
                )
            ),
            PageSpec(
                title = "CHAPTER IV: TACTICAL DISPOSITIONS",
                subtitle = "Defeat and Victory in Advance",
                paragraphs = listOf(
                    "1. Sun Tzu said: The good fighters of old first put themselves beyond the possibility of defeat, and then waited for an opportunity of defeating the enemy.",
                    "2. To secure ourselves against defeat lies in our own hands, but the opportunity of defeating the enemy is provided by the enemy himself.",
                    "3. Thus the good fighter is able to secure himself against defeat, but cannot make certain of defeating the enemy.",
                    "4. Hence the saying: One may KNOW how to conquer without being able to DO it.",
                    "5. Security against defeat implies defensive tactics; ability to defeat the enemy means taking the offensive.",
                    "6. Standing on the defensive indicates insufficient strength; attacking, a superabundance of strength.",
                    "7. The general who is skilled in defense hides in the most secret recesses of the earth; he who is skilled in attack flashes forth from the topmost heights of heaven."
                )
            )
        )

        renderPagesToPdf(document, pagesContent, pageWidth, pageHeight, isClassicalTheme = true)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
    }

    private fun createAlicePdf(file: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val pagesContent = listOf(
            PageSpec(
                title = "ALICE'S ADVENTURES IN WONDERLAND",
                subtitle = "By Lewis Carroll • Illustrated Reader Edition",
                paragraphs = listOf(
                    "CHAPTER I: DOWN THE RABBIT-HOLE",
                    "Alice was beginning to get very tired of sitting by her sister on the bank, and of having nothing to do: once or twice she had peeped into the book her sister was reading, but it had no pictures or conversations in it, 'and what is the use of a book,' thought Alice 'without pictures or conversations?'",
                    "So she was considering in her own mind (as well as she could, for the hot day made her feel very sleepy and stupid), whether the pleasure of making a daisy-chain would be worth the trouble of getting up and picking the daisies, when suddenly a White Rabbit with pink eyes ran close by her.",
                    "There was nothing so VERY remarkable in that; nor did Alice think it so VERY much out of the way to hear the Rabbit say to itself, 'Oh dear! Oh dear! I shall be late!'",
                    "Burning with curiosity, she ran across the field after it, and fortunately was just in time to see it pop down a large rabbit-hole under the hedge. In another moment down went Alice after it, never once considering how in the world she was to get out again."
                )
            ),
            PageSpec(
                title = "CHAPTER II: THE POOL OF TEARS",
                subtitle = "Growing and Shrinking Mysteries",
                paragraphs = listOf(
                    "1. 'Curiouser and curiouser!' cried Alice (she was so much surprised, that for the moment she quite forgot how to speak good English); 'now I'm opening out like the largest telescope that ever was! Good-bye, feet!'",
                    "2. (for when she looked down at her feet, they seemed to be almost out of sight, they were getting so far off). 'Oh, my poor little feet, I wonder who will put on your shoes and stockings for you now, dears? I'm sure I shan't be able!'",
                    "3. Just at this moment her head struck against the roof of the hall: in fact she was now more than nine feet high, and she at once took up the little golden key and hurried off to the garden door.",
                    "4. Poor Alice! It was as much as she could do, lying down on one side, to look through into the garden with one eye; but to get through was more hopeless than ever: she sat down and began to cry again.",
                    "5. 'You ought to be ashamed of yourself,' said Alice, 'a great girl like you, to go on crying in this way! Stop this moment, I tell you!' But she went on all the same, shedding gallons of tears, until there was a large pool all round her."
                )
            ),
            PageSpec(
                title = "CHAPTER III: A CAUCUS-RACE",
                subtitle = "And a Very Long Tale",
                paragraphs = listOf(
                    "1. They were indeed a queer-looking party that assembled on the bank—the birds with draggled feathers, the animals with their fur clinging close to them, and all dripping wet, cross, and uncomfortable.",
                    "2. The first question of course was, how to get dry again: they had a consultation about this, and after a few minutes it seemed quite natural to Alice to find herself talking familiarly with them, as if she had known them all her life.",
                    "3. 'What I was going to say,' said the Dodo in an offended tone, 'was, that the best thing to get us dry would be a Caucus-race.'",
                    "4. 'What IS a Caucus-race?' said Alice; not that she wanted much to know, but the Dodo had paused as if it thought that SOMEBODY ought to speak, and no one else seemed inclined to say anything.",
                    "5. 'Why,' said the Dodo, 'the best way to explain it is to do it.' First it marked out a race-course, in a sort of circle, and then all the party were placed along the course, here and there. There was no 'One, two, three, and away,' but they began running when they liked, and left off when they liked."
                )
            ),
            PageSpec(
                title = "CHAPTER IV: THE RABBIT SENDS IN A LITTLE BILL",
                subtitle = "Trapped in the White Rabbit's Cottage",
                paragraphs = listOf(
                    "1. It was the White Rabbit, trotting slowly back again, and looking anxiously about as it went, as if it had lost something; and she heard it muttering to itself 'The Duchess! The Duchess! Oh my dear paws! Oh my fur and whiskers! She'll get me executed, as sure as ferrets are ferrets!'",
                    "2. Alice guessed in a moment that it was looking for the fan and the pair of white kid gloves, and she very good-naturedly began hunting about for them, but they were nowhere to be seen.",
                    "3. Very soon the Rabbit noticed Alice, as she went hunting about, and called out to her in an angry tone, 'Why, Mary Ann, what ARE you doing out here? Run home this moment, and fetch me a pair of gloves and a fan! Quick, now!'",
                    "4. Alice went in, and took up the fan and a pair of gloves, and was just going to leave the room, when her eye fell upon a little bottle that stood near the looking-glass.",
                    "5. 'I know SOMETHING interesting is sure to happen,' she said to herself, 'whenever I eat or drink anything; so I'll just see what this bottle does. I do hope it'll make me grow large again, for really I'm quite tired of being such a tiny little thing!'"
                )
            ),
            PageSpec(
                title = "CHAPTER V: ADVICE FROM A CATERPILLAR",
                subtitle = "Questions of Identity and Mushrooms",
                paragraphs = listOf(
                    "1. The Caterpillar and Alice looked at each other for some time in silence: at last the Caterpillar took the hookah out of its mouth, and addressed her in a languid, sleepy voice.",
                    "2. 'Who are YOU?' said the Caterpillar. This was not an encouraging opening for a conversation. Alice replied, rather shyly, 'I—I hardly know, sir, just at present—at least I know who I WAS when I got up this morning, but I think I must have been changed several times since that.'",
                    "3. 'What do you mean by that?' said the Caterpillar sternly. 'Explain yourself!'",
                    "4. 'I can't explain MYSELF, I'm afraid, sir,' said Alice, 'because I'm not myself, you see.'",
                    "5. 'I don't see,' said the Caterpillar. 'I'm afraid I can't put it more clearly,' Alice replied very politely, 'for I can't understand it myself to begin with; and being so many different sizes in a day is very confusing.'",
                    "6. 'It isn't,' said the Caterpillar. 'Well, perhaps you haven't found it so yet,' said Alice; 'but when you have to turn into a chrysalis—you will some day, you know—and then after that into a butterfly, I should think you'll feel it a little queer, won't you?'",
                    "7. 'Not a bit,' said the Caterpillar."
                )
            )
        )

        renderPagesToPdf(document, pagesContent, pageWidth, pageHeight, isClassicalTheme = false)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
    }

    private fun renderPagesToPdf(
        document: PdfDocument,
        pages: List<PageSpec>,
        pageWidth: Int,
        pageHeight: Int,
        isClassicalTheme: Boolean
    ) {
        val bgPaint = Paint().apply {
            color = if (isClassicalTheme) Color.rgb(251, 246, 238) else Color.rgb(255, 255, 255)
            style = Paint.Style.FILL
        }

        val borderPaint = Paint().apply {
            color = if (isClassicalTheme) Color.rgb(180, 150, 100) else Color.rgb(200, 210, 220)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }

        val innerBorderPaint = Paint().apply {
            color = if (isClassicalTheme) Color.rgb(220, 200, 160) else Color.rgb(230, 235, 240)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        val headerPaint = Paint().apply {
            color = if (isClassicalTheme) Color.rgb(60, 40, 20) else Color.rgb(30, 41, 59)
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val subtitlePaint = Paint().apply {
            color = if (isClassicalTheme) Color.rgb(120, 90, 60) else Color.rgb(100, 116, 139)
            textSize = 12f
            typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val bodyPaint = Paint().apply {
            color = if (isClassicalTheme) Color.rgb(40, 30, 20) else Color.rgb(30, 41, 59)
            textSize = 12.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val pageNumPaint = Paint().apply {
            color = if (isClassicalTheme) Color.rgb(140, 110, 80) else Color.rgb(148, 163, 184)
            textSize = 10f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        pages.forEachIndexed { index, spec ->
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // Background
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            // Decorative borders
            val margin = 36f
            canvas.drawRect(margin, margin, pageWidth - margin, pageHeight - margin, borderPaint)
            canvas.drawRect(margin + 5f, margin + 5f, pageWidth - margin - 5f, pageHeight - margin - 5f, innerBorderPaint)

            // Page header
            var currentY = margin + 45f
            canvas.drawText(spec.title, (pageWidth / 2).toFloat(), currentY, headerPaint)

            currentY += 22f
            canvas.drawText(spec.subtitle, (pageWidth / 2).toFloat(), currentY, subtitlePaint)

            // Divider rule
            currentY += 16f
            val dividerMargin = 120f
            canvas.drawLine(dividerMargin, currentY, pageWidth - dividerMargin, currentY, borderPaint)

            currentY += 32f

            // Paragraphs wrapped nicely
            val contentLeft = margin + 28f
            val contentRight = pageWidth - margin - 28f
            val maxWidth = contentRight - contentLeft
            val lineHeight = 19f

            for (paragraph in spec.paragraphs) {
                val lines = wrapText(paragraph, bodyPaint, maxWidth)
                for (line in lines) {
                    if (currentY > pageHeight - margin - 40f) break
                    canvas.drawText(line, contentLeft, currentY, bodyPaint)
                    currentY += lineHeight
                }
                currentY += 12f // paragraph spacing
            }

            // Footer with page number
            canvas.drawText(
                "— ${index + 1} —",
                (pageWidth / 2).toFloat(),
                pageHeight - margin + 20f,
                pageNumPaint
            )

            document.finishPage(page)
        }
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            if (width <= maxWidth) {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }

    private fun createMangaPdf(file: File) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842

        val blackPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val whitePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val strokePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        val thinStrokePaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
            isAntiAlias = true
        }

        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val subtitlePaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        val dialoguePaint = Paint().apply {
            color = Color.BLACK
            textSize = 11.5f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val sfxPaint = Paint().apply {
            color = Color.BLACK
            textSize = 22f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }

        val badgePaint = Paint().apply {
            color = Color.rgb(20, 20, 20)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // PAGE 1: Manga Cover
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), whitePaint)

            // Outer manga frame
            canvas.drawRect(24f, 24f, pageWidth - 24f, pageHeight - 24f, strokePaint)

            // Top Header: Manga reading orientation guide
            canvas.drawRect(24f, 24f, pageWidth - 24f, 64f, blackPaint)
            val guidePaint = Paint().apply {
                color = Color.WHITE
                textSize = 11f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("◀ READ RIGHT-TO-LEFT ◀  JAPANESE MANGA FORMAT", (pageWidth / 2).toFloat(), 48f, guidePaint)

            // Title block
            canvas.drawText("SHADOW BLADE", (pageWidth / 2).toFloat(), 130f, titlePaint)
            canvas.drawText("影の刃 • CHAPTER 1: THE MOONLESS CASTLE", (pageWidth / 2).toFloat(), 155f, subtitlePaint)

            // Speed lines & cover panel
            val coverRect = RectF(48f, 180f, pageWidth - 48f, pageHeight - 120f)
            canvas.drawRect(coverRect, thinStrokePaint)

            // Draw speed lines from center
            val cx = (pageWidth / 2).toFloat()
            val cy = 400f
            for (angle in 0 until 360 step 8) {
                val rad = Math.toRadians(angle.toDouble())
                val startR = 80f
                val endR = 260f
                val x1 = cx + (Math.cos(rad) * startR).toFloat()
                val y1 = cy + (Math.sin(rad) * startR).toFloat()
                val x2 = cx + (Math.cos(rad) * endR).toFloat()
                val y2 = cy + (Math.sin(rad) * endR).toFloat()
                if (coverRect.contains(x2, y2)) {
                    canvas.drawLine(x1, y1, x2, y2, thinStrokePaint)
                }
            }

            // Silhouette circle in center
            canvas.drawCircle(cx, cy, 75f, blackPaint)
            val ninjaPaint = Paint().apply {
                color = Color.WHITE
                textSize = 36f
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("忍", cx, cy + 12f, ninjaPaint)

            // Speech box
            val speechRect = RectF(cx - 160f, cy + 120f, cx + 160f, cy + 180f)
            canvas.drawRoundRect(speechRect, 16f, 16f, whitePaint)
            canvas.drawRoundRect(speechRect, 16f, 16f, strokePaint)
            val bubbleTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 12f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("\"The night belongs to those who move without a sound.\"", cx, cy + 155f, bubbleTextPaint)

            // Author credit
            canvas.drawText("Created by Kenji Sato • Official Reader Edition", cx, pageHeight - 60f, subtitlePaint)
            document.finishPage(page)
        }

        // PAGE 2: Manga Act I (RTL Panel sequence: Top-Right [1], Top-Left [2], Bottom [3])
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), whitePaint)
            canvas.drawRect(24f, 24f, pageWidth - 24f, pageHeight - 24f, strokePaint)

            val mLeft = 32f
            val mRight = pageWidth - 32f
            val mWidth = mRight - mLeft
            val midX = mLeft + (mWidth / 2f)

            // Top-Right Panel [1] (Read First in Manga!)
            val panel1 = RectF(midX + 6f, 36f, mRight, 380f)
            canvas.drawRect(panel1, strokePaint)
            canvas.drawRoundRect(RectF(panel1.right - 44f, panel1.top + 8f, panel1.right - 8f, panel1.top + 30f), 6f, 6f, badgePaint)
            canvas.drawText("[1] START", panel1.right - 26f, panel1.top + 23f, badgeTextPaint)

            canvas.drawText("EDO CASTLE — MIDNIGHT", panel1.left + 14f, panel1.top + 38f, subtitlePaint)
            canvas.drawText("The watchman holds his torch.", panel1.left + 14f, panel1.top + 68f, textPaint)
            canvas.drawText("Heavy clouds cover the moon.", panel1.left + 14f, panel1.top + 88f, textPaint)
            canvas.drawText("All is dead silence...", panel1.left + 14f, panel1.top + 108f, textPaint)

            // Top-Left Panel [2] (Read Second)
            val panel2 = RectF(mLeft, 36f, midX - 6f, 380f)
            canvas.drawRect(panel2, strokePaint)
            canvas.drawRoundRect(RectF(panel2.right - 34f, panel2.top + 8f, panel2.right - 8f, panel2.top + 30f), 6f, 6f, badgePaint)
            canvas.drawText("[2]", panel2.right - 21f, panel2.top + 23f, badgeTextPaint)

            // Speech balloon
            val balloon = RectF(panel2.left + 16f, panel2.top + 50f, panel2.right - 16f, panel2.top + 140f)
            canvas.drawRoundRect(balloon, 14f, 14f, strokePaint)
            canvas.drawText("WAIT!", panel2.left + 30f, panel2.top + 80f, dialoguePaint)
            canvas.drawText("Did the roof just creak?", panel2.left + 30f, panel2.top + 102f, textPaint)
            canvas.drawText("Who goes there?!", panel2.left + 30f, panel2.top + 124f, dialoguePaint)

            // Sound effect
            canvas.drawText("*CREAK...*", panel2.left + 40f, panel2.top + 220f, sfxPaint)

            // Bottom Full-Width Panel [3]
            val panel3 = RectF(mLeft, 396f, mRight, pageHeight - 36f)
            canvas.drawRect(panel3, strokePaint)
            canvas.drawRoundRect(RectF(panel3.right - 34f, panel3.top + 8f, panel3.right - 8f, panel3.top + 30f), 6f, 6f, badgePaint)
            canvas.drawText("[3]", panel3.right - 21f, panel3.top + 23f, badgeTextPaint)

            canvas.drawText("SHINGGGG!!", panel3.left + 30f, panel3.top + 70f, sfxPaint)
            canvas.drawText("From the high rafter beam, a shadow drops like a hawk!", panel3.left + 30f, panel3.top + 110f, textPaint)
            canvas.drawText("Silver steel glints in the darkness.", panel3.left + 30f, panel3.top + 130f, textPaint)

            // Speed lines on bottom panel
            for (i in 0..12) {
                val y = panel3.top + 160f + (i * 18f)
                canvas.drawLine(panel3.left + 20f, y, panel3.right - 20f, y - 10f, thinStrokePaint)
            }

            document.finishPage(page)
        }

        // PAGE 3: Manga Act II (The Confrontation)
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), whitePaint)
            canvas.drawRect(24f, 24f, pageWidth - 24f, pageHeight - 24f, strokePaint)

            val mLeft = 32f
            val mRight = pageWidth - 32f
            val mWidth = mRight - mLeft
            val midX = mLeft + (mWidth / 2f)

            // Top-Right Panel [1]
            val panel1 = RectF(midX + 6f, 36f, mRight, 360f)
            canvas.drawRect(panel1, strokePaint)
            canvas.drawRoundRect(RectF(panel1.right - 34f, panel1.top + 8f, panel1.right - 8f, panel1.top + 30f), 6f, 6f, badgePaint)
            canvas.drawText("[1]", panel1.right - 21f, panel1.top + 23f, badgeTextPaint)

            canvas.drawText("\"You won't leave this chamber alive!\"", panel1.left + 16f, panel1.top + 60f, dialoguePaint)
            canvas.drawText("The Commander draws his twin nodachi.", panel1.left + 16f, panel1.top + 85f, textPaint)
            canvas.drawText("His stance is unbreakable.", panel1.left + 16f, panel1.top + 105f, textPaint)

            // Top-Left Panel [2]
            val panel2 = RectF(mLeft, 36f, midX - 6f, 360f)
            canvas.drawRect(panel2, strokePaint)
            canvas.drawRoundRect(RectF(panel2.right - 34f, panel2.top + 8f, panel2.right - 8f, panel2.top + 30f), 6f, 6f, badgePaint)
            canvas.drawText("[2]", panel2.right - 21f, panel2.top + 23f, badgeTextPaint)

            canvas.drawText("\"I only came for the scroll.\"", panel2.left + 16f, panel2.top + 60f, dialoguePaint)
            canvas.drawText("\"The secrets of the dragon clan", panel2.left + 16f, panel2.top + 85f, textPaint)
            canvas.drawText("do not belong to a corrupt magistrate!\"", panel2.left + 16f, panel2.top + 105f, textPaint)

            // Bottom Panel [3]: Clash
            val panel3 = RectF(mLeft, 376f, mRight, pageHeight - 36f)
            canvas.drawRect(panel3, strokePaint)
            canvas.drawRoundRect(RectF(panel3.right - 34f, panel3.top + 8f, panel3.right - 8f, panel3.top + 30f), 6f, 6f, badgePaint)
            canvas.drawText("[3]", panel3.right - 21f, panel3.top + 23f, badgeTextPaint)

            canvas.drawText("*CLANGGGGG!!*", panel3.left + 40f, panel3.top + 65f, sfxPaint)
            canvas.drawText("Steel meets steel with blinding intensity!", panel3.left + 40f, panel3.top + 105f, textPaint)
            canvas.drawText("Sparks illuminate the dark hall for a split second.", panel3.left + 40f, panel3.top + 125f, textPaint)

            document.finishPage(page)
        }

        // PAGE 4: Manga Act III (The Escape & Cliffhanger)
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), whitePaint)
            canvas.drawRect(24f, 24f, pageWidth - 24f, pageHeight - 24f, strokePaint)

            val mLeft = 32f
            val mRight = pageWidth - 32f

            // Panel 1: Smoke bomb
            val panel1 = RectF(mLeft, 36f, mRight, 240f)
            canvas.drawRect(panel1, strokePaint)
            canvas.drawRoundRect(RectF(panel1.right - 34f, panel1.top + 8f, panel1.right - 8f, panel1.top + 30f), 6f, 6f, badgePaint)
            canvas.drawText("[1]", panel1.right - 21f, panel1.top + 23f, badgeTextPaint)
            canvas.drawText("*BOOOM! POOOF!*", panel1.left + 30f, panel1.top + 60f, sfxPaint)
            canvas.drawText("A black smoke pellet bursts across the entire room!", panel1.left + 30f, panel1.top + 100f, textPaint)
            canvas.drawText("\"Cough! I can't see! Guards, seal the gates!\"", panel1.left + 30f, panel1.top + 125f, dialoguePaint)

            // Panel 2: The Rooftop Escape
            val panel2 = RectF(mLeft, 256f, mRight, pageHeight - 120f)
            canvas.drawRect(panel2, strokePaint)
            canvas.drawRoundRect(RectF(panel2.right - 34f, panel2.top + 8f, panel2.right - 8f, panel2.top + 30f), 6f, 6f, badgePaint)
            canvas.drawText("[2]", panel2.right - 21f, panel2.top + 23f, badgeTextPaint)

            canvas.drawText("High above the castle rooftops, the Shinobi lands gracefully.", panel2.left + 30f, panel2.top + 60f, textPaint)
            canvas.drawText("Tucked safely inside his robes is the golden cipher scroll.", panel2.left + 30f, panel2.top + 85f, textPaint)
            canvas.drawText("\"The mission is complete.\"", panel2.left + 30f, panel2.top + 120f, dialoguePaint)

            // Panel 3: Cliffhanger
            val panel3 = RectF(mLeft, pageHeight - 105f, mRight, pageHeight - 36f)
            canvas.drawRect(panel3, blackPaint)
            val endPaint = Paint().apply {
                color = Color.WHITE
                textSize = 15f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("TO BE CONTINUED IN CHAPTER 2: THE MOUNTAIN RETREAT", (pageWidth / 2).toFloat(), pageHeight - 65f, endPaint)

            document.finishPage(page)
        }

        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
    }

    private data class PageSpec(
        val title: String,
        val subtitle: String,
        val paragraphs: List<String>
    )
}
