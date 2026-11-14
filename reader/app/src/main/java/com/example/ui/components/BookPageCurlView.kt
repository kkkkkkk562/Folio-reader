package com.example.ui.components

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * High-performance, realistic 3D book page-turning custom [View].
 *
 * Features:
 * 1. Exact finger coordinate tracking via [onTouchEvent] with edge curl physics.
 * 2. Mathematical fold-line reflection, dual-sided page rendering, and realistic depth gradients:
 *    - Unturned current page layer
 *    - Revealing underlying next page
 *    - Curled page flap with mirrored backing, spine shadow, crease shadow, and 3D specular highlight sheen.
 * 3. Smooth spring animation fallback on gesture release (completes flip or snaps shut).
 * 4. Flexible [PageProvider] interface directly compatible with standard [Bitmap] pages
 *    (e.g., rendered from Android's native [android.graphics.pdf.PdfRenderer]).
 * 5. Zero external dependencies; operates purely on native Android Canvas and Math.
 */
class BookPageCurlView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * Interface for supplying page [Bitmap]s to the curl view.
     */
    interface PageProvider {
        val pageCount: Int
        fun getPage(pageIndex: Int): Bitmap?
    }

    /**
     * Callback listener for page flip events.
     */
    fun interface OnPageTurnListener {
        fun onPageTurned(newPageIndex: Int)
    }

    // --- State & Config ---
    private var pageProvider: PageProvider? = null
    private var pageTurnListener: OnPageTurnListener? = null

    var currentPageIndex: Int = 0
        private set

    /** If true, reads from right to left (Manga / Arabic reading mode). */
    var isRightToLeft: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    /** Background paper tint for the back of the turning page. */
    @ColorInt
    var backOfPageTint: Int = 0xFFF5F0E6.toInt()
        set(value) {
            field = value
            invalidate()
        }

    /** Color for book spine shadow. */
    @ColorInt
    var spineShadowColor: Int = 0x55000000
        set(value) {
            field = value
            invalidate()
        }

    // --- Bitmaps ---
    private var currentBitmap: Bitmap? = null
    private var targetBitmap: Bitmap? = null

    // --- Geometry & Tracking Points ---
    private val touchPos = PointF()
    private val originCorner = PointF()
    private val foldMidpoint = PointF()
    private var isDragging: Boolean = false
    private var isTurningForward: Boolean = true
    private var isAnimating: Boolean = false

    // Geometry intersection points for the fold line
    private val foldP1 = PointF()
    private val foldP2 = PointF()

    // Canvas paths & matrices
    private val pathCurrentPage = Path()
    private val pathNextPage = Path()
    private val pathCurledBack = Path()
    private val reflectionMatrix = Matrix()

    // Reusable Paints
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val backTintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val shadowCreasePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dropShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spinePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Touch & Animation helpers
    private var velocityTracker: VelocityTracker? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var curlAnimator: ValueAnimator? = null

    private val viewRect = RectF()
    private val srcRect = Rect()
    private val dstRect = RectF()

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        viewRect.set(0f, 0f, w.toFloat(), h.toFloat())
        updateSpineShadowGradient(w.toFloat(), h.toFloat())
        refreshCurrentPages()
    }

    private fun updateSpineShadowGradient(w: Float, h: Float) {
        if (w <= 0f || h <= 0f) return
        val spineX = if (isRightToLeft) w else 0f
        val spineEnd = if (isRightToLeft) w - 24f else 24f
        spinePaint.shader = LinearGradient(
            spineX, 0f, spineEnd, 0f,
            spineShadowColor, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
    }

    /**
     * Sets the [PageProvider] and displays the given initial page.
     */
    fun setPageProvider(provider: PageProvider, initialPage: Int = 0) {
        this.pageProvider = provider
        val maxPage = max(0, provider.pageCount - 1)
        this.currentPageIndex = initialPage.coerceIn(0, maxPage)
        refreshCurrentPages()
        invalidate()
    }

    /**
     * Sets the page turn callback.
     */
    fun setOnPageTurnListener(listener: OnPageTurnListener?) {
        this.pageTurnListener = listener
    }

    /**
     * Convenience method to directly supply the current and target page bitmaps.
     */
    fun setDirectPages(current: Bitmap?, target: Bitmap?, isForward: Boolean) {
        this.currentBitmap = current
        this.targetBitmap = target
        this.isTurningForward = isForward
        invalidate()
    }

    private fun refreshCurrentPages() {
        val provider = pageProvider ?: return
        currentBitmap = provider.getPage(currentPageIndex)
    }

    // =========================================================================
    // Touch Handling (Edge detection, drag coordinate tracking, gesture release)
    // =========================================================================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isAnimating) return true
        val provider = pageProvider ?: return super.onTouchEvent(event)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return false

        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialTouchX = event.x
                initialTouchY = event.y

                // Determine forward vs backward flip zone based on reading direction
                val isForwardZone = if (!isRightToLeft) {
                    event.x > w * 0.55f
                } else {
                    event.x < w * 0.45f
                }

                val isBackwardZone = if (!isRightToLeft) {
                    event.x < w * 0.45f
                } else {
                    event.x > w * 0.55f
                }

                if (isForwardZone && currentPageIndex < provider.pageCount - 1) {
                    isTurningForward = true
                    targetBitmap = provider.getPage(currentPageIndex + 1)
                    setupCornerAndTouch(event.x, event.y, forward = true)
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                } else if (isBackwardZone && currentPageIndex > 0) {
                    isTurningForward = false
                    targetBitmap = provider.getPage(currentPageIndex - 1)
                    setupCornerAndTouch(event.x, event.y, forward = false)
                    isDragging = true
                    parent?.requestDisallowInterceptTouchEvent(true)
                    return true
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) {
                    val dx = event.x - initialTouchX
                    val dy = event.y - initialTouchY
                    if (hypot(dx, dy) > touchSlop) {
                        // Check if we can begin drag
                        val forwardCandidate = if (!isRightToLeft) dx < 0 else dx > 0
                        if (forwardCandidate && currentPageIndex < provider.pageCount - 1) {
                            isTurningForward = true
                            targetBitmap = provider.getPage(currentPageIndex + 1)
                            setupCornerAndTouch(event.x, event.y, forward = true)
                            isDragging = true
                            parent?.requestDisallowInterceptTouchEvent(true)
                        } else if (!forwardCandidate && currentPageIndex > 0) {
                            isTurningForward = false
                            targetBitmap = provider.getPage(currentPageIndex - 1)
                            setupCornerAndTouch(event.x, event.y, forward = false)
                            isDragging = true
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    }
                }

                if (isDragging) {
                    updateTouchCoordinates(event.x, event.y)
                    postInvalidateOnAnimation()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    velocityTracker?.computeCurrentVelocity(1000)
                    val xVel = velocityTracker?.xVelocity ?: 0f
                    velocityTracker?.recycle()
                    velocityTracker = null

                    evaluateFlipCompletion(xVel)
                    return true
                }
                velocityTracker?.recycle()
                velocityTracker = null
            }
        }
        return super.onTouchEvent(event)
    }

    private fun setupCornerAndTouch(x: Float, y: Float, forward: Boolean) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Choose top or bottom corner depending on where touch landed
        val cornerY = if (y < h * 0.5f) 0f else h
        val cornerX = if (!isRightToLeft) {
            if (forward) w else 0f
        } else {
            if (forward) 0f else w
        }

        originCorner.set(cornerX, cornerY)
        touchPos.set(x, y)
    }

    private fun updateTouchCoordinates(rawX: Float, rawY: Float) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Clamp touch inside viewport bounds with margin
        var x = rawX.coerceIn(4f, w - 4f)
        var y = rawY.coerceIn(0f, h)

        // Mathematical constraint: The maximum distance the corner can be pulled
        // cannot exceed the page diagonal length
        val maxDist = hypot(w, h)
        val distFromCorner = hypot(x - originCorner.x, y - originCorner.y)
        if (distFromCorner > maxDist) {
            val ratio = maxDist / distFromCorner
            x = originCorner.x + (x - originCorner.x) * ratio
            y = originCorner.y + (y - originCorner.y) * ratio
        }

        touchPos.set(x, y)
    }

    // =========================================================================
    // Animation Fallback (Smooth completion or snap back shut)
    // =========================================================================

    private fun evaluateFlipCompletion(xVelocity: Float) {
        val w = width.toFloat()
        val h = height.toFloat()

        // Determine destination based on direction and release position/velocity
        val shouldFlip: Boolean = if (!isRightToLeft) {
            if (isTurningForward) {
                touchPos.x < w * 0.55f || xVelocity < -800f
            } else {
                touchPos.x > w * 0.45f || xVelocity > 800f
            }
        } else {
            if (isTurningForward) {
                touchPos.x > w * 0.45f || xVelocity > 800f
            } else {
                touchPos.x < w * 0.55f || xVelocity < -800f
            }
        }

        val targetEndX: Float
        val targetEndY = originCorner.y

        if (shouldFlip) {
            // Flipped completely across to the opposite edge
            targetEndX = if (originCorner.x > 0f) -w * 0.15f else w * 1.15f
        } else {
            // Snapped back shut to original corner
            targetEndX = originCorner.x
        }

        animateCurlTo(targetEndX, targetEndY, completed = shouldFlip)
    }

    private fun animateCurlTo(destX: Float, destY: Float, completed: Boolean) {
        isAnimating = true
        val startX = touchPos.x
        val startY = touchPos.y

        curlAnimator?.cancel()
        curlAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 320
            interpolator = DecelerateInterpolator(1.4f)
            addUpdateListener { anim ->
                val frac = anim.animatedValue as Float
                touchPos.x = startX + (destX - startX) * frac
                touchPos.y = startY + (destY - startY) * frac
                postInvalidateOnAnimation()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isAnimating = false
                    if (completed) {
                        val provider = pageProvider
                        if (provider != null) {
                            currentPageIndex = if (isTurningForward) {
                                (currentPageIndex + 1).coerceAtMost(provider.pageCount - 1)
                            } else {
                                (currentPageIndex - 1).coerceAtLeast(0)
                            }
                            refreshCurrentPages()
                            pageTurnListener?.onPageTurned(currentPageIndex)
                        }
                    }
                    targetBitmap = null
                    postInvalidateOnAnimation()
                }
            })
            start()
        }
    }

    /**
     * Programmatically flip to the next page with a 3D curl animation.
     */
    fun flipToNextPage(animated: Boolean = true) {
        val provider = pageProvider ?: return
        if (currentPageIndex >= provider.pageCount - 1 || isAnimating) return

        if (!animated) {
            currentPageIndex++
            refreshCurrentPages()
            pageTurnListener?.onPageTurned(currentPageIndex)
            invalidate()
            return
        }

        isTurningForward = true
        targetBitmap = provider.getPage(currentPageIndex + 1)
        val w = width.toFloat()
        val h = height.toFloat()

        setupCornerAndTouch(
            if (!isRightToLeft) w else 0f,
            h * 0.85f,
            forward = true
        )
        val destX = if (!isRightToLeft) -w * 0.15f else w * 1.15f
        animateCurlTo(destX, h * 0.85f, completed = true)
    }

    /**
     * Programmatically flip to the previous page with a 3D curl animation.
     */
    fun flipToPreviousPage(animated: Boolean = true) {
        val provider = pageProvider ?: return
        if (currentPageIndex <= 0 || isAnimating) return

        if (!animated) {
            currentPageIndex--
            refreshCurrentPages()
            pageTurnListener?.onPageTurned(currentPageIndex)
            invalidate()
            return
        }

        isTurningForward = false
        targetBitmap = provider.getPage(currentPageIndex - 1)
        val w = width.toFloat()
        val h = height.toFloat()

        setupCornerAndTouch(
            if (!isRightToLeft) 0f else w,
            h * 0.85f,
            forward = false
        )
        val destX = if (!isRightToLeft) w * 1.15f else -w * 0.15f
        animateCurlTo(destX, h * 0.85f, completed = true)
    }

    // =========================================================================
    // 3D Canvas Manipulation & Realistic Shadow Rendering
    // =========================================================================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        val curBmp = currentBitmap
        val tgtBmp = targetBitmap

        // When stationary (not dragging or animating curl), simply draw the current page
        if (!isDragging && !isAnimating) {
            if (curBmp != null && !curBmp.isRecycled) {
                drawPageBitmap(canvas, curBmp, 0f, 0f, w, h)
            }
            drawSpineShadow(canvas, w, h)
            return
        }

        // Calculate the fold line geometry between origin corner and touch position
        computeFoldGeometry(w, h)

        // 1. LAYER 1: Draw revealing underneath page (Target Page)
        if (tgtBmp != null && !tgtBmp.isRecycled) {
            drawPageBitmap(canvas, tgtBmp, 0f, 0f, w, h)
        }

        // 2. LAYER 2: Draw drop shadow cast onto the next page by the curled flap
        drawDropShadowOnNextPage(canvas)

        // 3. LAYER 3: Draw the un-curled portion of the current page
        canvas.save()
        canvas.clipPath(pathCurrentPage)
        if (curBmp != null && !curBmp.isRecycled) {
            drawPageBitmap(canvas, curBmp, 0f, 0f, w, h)
        }
        // Subtle shadow along the unturned crease
        drawCreaseShadow(canvas)
        canvas.restore()

        // 4. LAYER 4: Draw the back of the turning page (Curled 3D flap)
        drawCurledFlap(canvas, curBmp, w, h)

        // 5. LAYER 5: Spine shadow
        drawSpineShadow(canvas, w, h)
    }

    private fun computeFoldGeometry(w: Float, h: Float) {
        val cx = originCorner.x
        val cy = originCorner.y
        val tx = touchPos.x
        val ty = touchPos.y

        // Fold line passes through the midpoint between corner and finger
        foldMidpoint.set((cx + tx) / 2f, (cy + ty) / 2f)

        // Vector from corner to finger
        val dx = tx - cx
        val dy = ty - cy
        val angle = atan2(dy, dx)
        val perpAngle = angle + (Math.PI / 2.0).toFloat()

        // Line equation for the fold line: through foldMidpoint with direction perpAngle
        val lineDirX = cos(perpAngle)
        val lineDirY = sin(perpAngle)

        // Extend fold line far enough to intersect page borders
        val length = hypot(w, h) * 1.8f
        foldP1.set(foldMidpoint.x - lineDirX * length, foldMidpoint.y - lineDirY * length)
        foldP2.set(foldMidpoint.x + lineDirX * length, foldMidpoint.y + lineDirY * length)

        // Build path for the unturned current page (polygon on the side opposite to touchPos)
        pathCurrentPage.reset()
        pathNextPage.reset()
        pathCurledBack.reset()

        // Create polygon for curled back by clipping to the fold half-plane containing touchPos
        pathCurledBack.moveTo(foldP1.x, foldP1.y)
        pathCurledBack.lineTo(foldP2.x, foldP2.y)
        // Add polygon points around touchPos
        pathCurledBack.lineTo(tx, ty)
        pathCurledBack.close()

        // For current page: canvas rect minus the curl area
        pathCurrentPage.moveTo(0f, 0f)
        pathCurrentPage.lineTo(w, 0f)
        pathCurrentPage.lineTo(w, h)
        pathCurrentPage.lineTo(0f, h)
        pathCurrentPage.close()

        // Clip out the curled area
        val foldClipPath = Path()
        foldClipPath.moveTo(foldP1.x, foldP1.y)
        foldClipPath.lineTo(foldP2.x, foldP2.y)
        foldClipPath.lineTo(cx, cy)
        foldClipPath.close()

        pathCurrentPage.op(foldClipPath, Path.Op.DIFFERENCE)
    }

    private fun drawCurledFlap(canvas: Canvas, curBmp: Bitmap?, w: Float, h: Float) {
        canvas.save()

        // Calculate 2D reflection matrix across the fold line
        val dx = touchPos.x - originCorner.x
        val dy = touchPos.y - originCorner.y
        val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        reflectionMatrix.reset()
        // Translate to fold midpoint, rotate, mirror across Y-axis, rotate back, translate back
        reflectionMatrix.preTranslate(foldMidpoint.x, foldMidpoint.y)
        reflectionMatrix.preRotate(angleDeg)
        reflectionMatrix.preScale(-1f, 1f)
        reflectionMatrix.preRotate(-angleDeg)
        reflectionMatrix.preTranslate(-foldMidpoint.x, -foldMidpoint.y)

        // Clip to the curled flap region
        val flapPath = Path()
        flapPath.moveTo(foldP1.x, foldP1.y)
        flapPath.lineTo(foldP2.x, foldP2.y)
        flapPath.lineTo(touchPos.x, touchPos.y)
        flapPath.close()

        canvas.clipPath(flapPath)

        // 1. Draw paper backing tint (rich cream / parchment or dark slate)
        backTintPaint.color = backOfPageTint
        canvas.drawRect(viewRect, backTintPaint)

        // 2. Mirror translucent content of the page showing through paper backing
        if (curBmp != null && !curBmp.isRecycled) {
            canvas.save()
            canvas.concat(reflectionMatrix)
            bitmapPaint.alpha = 50 // Semi-translucent bleed-through
            drawPageBitmap(canvas, curBmp, 0f, 0f, w, h)
            bitmapPaint.alpha = 255
            canvas.restore()
        }

        // 3. Draw 3D curved lighting sheen across the cylindrical roll of the flap
        drawCylindricalSheen(canvas, foldMidpoint, touchPos)

        canvas.restore()
    }

    private fun drawCylindricalSheen(canvas: Canvas, foldMid: PointF, touch: PointF) {
        val dist = hypot(touch.x - foldMid.x, touch.y - foldMid.y)
        if (dist <= 0f) return

        sheenPaint.shader = LinearGradient(
            foldMid.x, foldMid.y,
            touch.x, touch.y,
            intArrayOf(
                0x77000000,           // Crease deep shadow
                0x11000000,           // Mid curl shadow
                0x44FFFFFF.toInt(),   // 3D Specular highlight sheen
                0x00FFFFFF            // Fade to edge
            ),
            floatArrayOf(0f, 0.25f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(viewRect, sheenPaint)
    }

    private fun drawCreaseShadow(canvas: Canvas) {
        val dx = originCorner.x - touchPos.x
        val dy = originCorner.y - touchPos.y
        val dist = hypot(dx, dy)
        if (dist <= 0f) return

        val shadowRadius = min(48f, dist * 0.25f)
        val nx = (dx / dist) * shadowRadius
        val ny = (dy / dist) * shadowRadius

        shadowCreasePaint.shader = LinearGradient(
            foldMidpoint.x, foldMidpoint.y,
            foldMidpoint.x + nx, foldMidpoint.y + ny,
            0x44000000, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(viewRect, shadowCreasePaint)
    }

    private fun drawDropShadowOnNextPage(canvas: Canvas) {
        val dx = touchPos.x - originCorner.x
        val dy = touchPos.y - originCorner.y
        val dist = hypot(dx, dy)
        if (dist <= 0f) return

        val shadowWidth = min(64f, dist * 0.35f)
        val nx = (dx / dist) * shadowWidth
        val ny = (dy / dist) * shadowWidth

        dropShadowPaint.shader = LinearGradient(
            foldMidpoint.x, foldMidpoint.y,
            foldMidpoint.x + nx, foldMidpoint.y + ny,
            0x66000000, Color.TRANSPARENT,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(viewRect, dropShadowPaint)
    }

    private fun drawSpineShadow(canvas: Canvas, w: Float, h: Float) {
        val spineWidth = 28f
        val rect = if (!isRightToLeft) {
            RectF(0f, 0f, spineWidth, h)
        } else {
            RectF(w - spineWidth, 0f, w, h)
        }
        canvas.drawRect(rect, spinePaint)
    }

    private fun drawPageBitmap(canvas: Canvas, bmp: Bitmap, left: Float, top: Float, w: Float, h: Float) {
        srcRect.set(0, 0, bmp.width, bmp.height)
        dstRect.set(left, top, left + w, top + h)
        canvas.drawBitmap(bmp, srcRect, dstRect, bitmapPaint)
    }
}

/**
 * Jetpack Compose adapter to host [BookPageCurlView] smoothly within modern Compose screens.
 */
@Composable
fun BookPageCurlBox(
    modifier: Modifier = Modifier,
    currentPage: Bitmap?,
    targetPage: Bitmap?,
    isRightToLeft: Boolean = false,
    backOfPageTint: Int = 0xFFF5F0E6.toInt(),
    onPageFlipped: (isNext: Boolean) -> Unit = {}
) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            BookPageCurlView(ctx).apply {
                this.isRightToLeft = isRightToLeft
                this.backOfPageTint = backOfPageTint
                this.setOnPageTurnListener {
                    onPageFlipped(true)
                }
            }
        },
        update = { view ->
            view.isRightToLeft = isRightToLeft
            view.backOfPageTint = backOfPageTint
            view.setDirectPages(currentPage, targetPage, isForward = true)
        }
    )
}
