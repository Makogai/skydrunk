package me.makogai.skydrunk.hud

import me.makogai.skydrunk.config.McManaged
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import kotlin.math.roundToInt

class DragScreen : Screen(Text.of("Skydrunk – Drag Overlay")) {
     private var dragging = false
     private var dragDX = 0
     private var dragDY = 0
     private enum class DragTarget { NONE, OVERLAY, MILESTONE }
     private var dragTarget = DragTarget.NONE

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // 1.21.5 signature requires mouseX/mouseY/delta
        // Draw a slightly dark translucent overlay but exclude the HUD and milestone areas so elements remain fully visible.
        val overlayAlpha = 72 // even lighter overlay (~28% dark)
        val overlayColor = (overlayAlpha shl 24) or 0x000000

        // Compute HUD and milestone rects (screen coords) to exclude from overlay
        val hudBounds = ShardHud.currentBounds() // [x, y, w, h] already scaled
        val msCfg = McManaged.data().hunting.milestone
        val msBounds = if (msCfg.enabled && msCfg.showOverlay && msCfg.targetAmount > 0.0) {
            val scaleM = msCfg.scale.coerceIn(0.5f, 2f)
            val mx = (msCfg.posX * scaleM).roundToInt()
            val my = (msCfg.posY * scaleM).roundToInt()
            val mw = (240 * scaleM).roundToInt()
            val mh = ((if (msCfg.showInstaSell) 78 else 54) * scaleM).roundToInt()
            intArrayOf(mx, my, mw, mh)
        } else null

        // union rect of hud and milestone (if present)
        val unionRect = if (msBounds != null) {
            val minX = minOf(hudBounds[0], msBounds[0])
            val minY = minOf(hudBounds[1], msBounds[1])
            val maxX = maxOf(hudBounds[0] + hudBounds[2], msBounds[0] + msBounds[2])
            val maxY = maxOf(hudBounds[1] + hudBounds[3], msBounds[1] + msBounds[3])
            intArrayOf(minX, minY, maxX - minX, maxY - minY)
        } else {
            hudBounds
        }

        // draw overlay in 4 rects around unionRect
        val ux = unionRect[0]
        val uy = unionRect[1]
        val uw = unionRect[2]
        val uh = unionRect[3]
        // top
        context.fill(0, 0, this.width, uy.coerceAtLeast(0), overlayColor)
        // left
        context.fill(0, uy.coerceAtLeast(0), ux.coerceAtLeast(0), (uy + uh).coerceAtMost(this.height), overlayColor)
        // right
        context.fill((ux + uw).coerceAtLeast(0), uy.coerceAtLeast(0), this.width, (uy + uh).coerceAtMost(this.height), overlayColor)
        // bottom
        context.fill(0, (uy + uh).coerceAtLeast(0), this.width, this.height, overlayColor)

        // render HUD preview while dragging. Draw HUD OVER the overlay so elements are 100% visible
        ShardHud.render(context, /* tickCounter not used */ net.minecraft.client.MinecraftClient.getInstance().renderTickCounter)

        context.drawCenteredTextWithShadow(textRenderer, title, width / 2, 8, 0xFFFFFF)
        // don't call super.render to avoid default background drawing that can darken/blur our HUD preview
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button)
        val cfg = McManaged.data().hunting.overlay
        val scale = cfg.scale.coerceIn(0.5f, 2f)

        val font = textRenderer
        val pad = 4
        val lineH = 10
        val labels = listOf("Glacite Shards", "Session", "Elapsed", "Shards/hr", "Coins/hr")
        val contentWidth = labels.maxOf { font.getWidth(it) } + pad * 2 + 60
        val contentHeight = lineH * labels.size + pad * 2

        val x = (cfg.posX * scale).roundToInt()
        val y = (cfg.posY * scale).roundToInt()
        val w = (contentWidth * scale).roundToInt()
        val h = (contentHeight * scale).roundToInt()

        // check overlay box
        if (mouseX.toInt() in x..(x + w) && mouseY.toInt() in y..(y + h)) {
            dragging = true
            dragTarget = DragTarget.OVERLAY
            dragDX = mouseX.toInt() - x
            dragDY = mouseY.toInt() - y
            return true
        }

        // check milestone box if enabled and visible
        val msCfg = McManaged.data().hunting.milestone
        if (msCfg.enabled && msCfg.showOverlay && msCfg.targetAmount > 0.0) {
            val scaleM = msCfg.scale.coerceIn(0.5f, 2f)
            val mx = (msCfg.posX * scaleM).roundToInt()
            val my = (msCfg.posY * scaleM).roundToInt()
            val widthM = (220 * scaleM).roundToInt()
            val heightM = (56 * scaleM).roundToInt()
            if (mouseX.toInt() in mx..(mx + widthM) && mouseY.toInt() in my..(my + heightM)) {
                dragging = true
                dragTarget = DragTarget.MILESTONE
                dragDX = mouseX.toInt() - mx
                dragDY = mouseY.toInt() - my
                return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        if (!dragging || button != 0) return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
        val cfg = McManaged.data().hunting
        when (dragTarget) {
            DragTarget.OVERLAY -> {
                val o = cfg.overlay
                val scale = o.scale.coerceIn(0.5f, 2f)
                val inv = 1f / scale
                val nx = ((mouseX.toInt() - dragDX).toFloat() * inv).roundToInt().coerceAtLeast(0)
                val ny = ((mouseY.toInt() - dragDY).toFloat() * inv).roundToInt().coerceAtLeast(0)
                o.posX = nx.toFloat()
                o.posY = ny.toFloat()
            }
            DragTarget.MILESTONE -> {
                val ms = cfg.milestone
                val scaleM = ms.scale.coerceIn(0.5f, 2f)
                val inv = 1f / scaleM
                val nx = ((mouseX.toInt() - dragDX).toFloat() * inv).roundToInt().coerceAtLeast(0)
                val ny = ((mouseY.toInt() - dragDY).toFloat() * inv).roundToInt().coerceAtLeast(0)
                ms.posX = nx.toFloat()
                ms.posY = ny.toFloat()
            }
            else -> {}
        }
        return true
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (dragging && button == 0) {
            dragging = false
            dragTarget = DragTarget.NONE
            try { me.makogai.skydrunk.config.McManaged.save() } catch (_: Throwable) {}
            return true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        // vertical is scroll direction; positive = up
        val mx = mouseX.toInt()
        val my = mouseY.toInt()
        val cfg = McManaged.data().hunting

        // detect overlay bounds
        val o = cfg.overlay
        val scale = o.scale.coerceIn(0.5f, 2f)
        val font = textRenderer
        val pad = 4
        val lineH = 10
        val labels = listOf("Glacite Shards", "Session", "Elapsed", "Shards/hr", "Coins/hr")
        val contentWidth = labels.maxOf { font.getWidth(it) } + pad * 2 + 60
        val contentHeight = lineH * labels.size + pad * 2
        val x = (o.posX * scale).roundToInt()
        val y = (o.posY * scale).roundToInt()
        val w = (contentWidth * scale).roundToInt()
        val h = (contentHeight * scale).roundToInt()

        var handled = false
        if (mx in x..(x + w) && my in y..(y + h)) {
            // scroll changes scale; Ctrl+scroll changes opacity
            val win = net.minecraft.client.MinecraftClient.getInstance().window.handle
            val ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
            if (ctrl) {
                val cur = o.opacity
                val delta = if (vertical > 0) 0.05f else -0.05f
                o.opacity = (cur + delta).coerceIn(0.05f, 1.0f)
            } else {
                val cur = o.scale
                val delta = if (vertical > 0) 0.05f else -0.05f
                o.scale = (cur + delta).coerceIn(0.5f, 2.0f)
            }
            // do not save every scroll; save will happen on mouse release
            handled = true
        }

        // milestone bounds
        val ms = cfg.milestone
        if (!handled && ms.enabled && ms.showOverlay && ms.targetAmount > 0.0) {
            val scaleM = ms.scale.coerceIn(0.5f, 2f)
            val mx0 = (ms.posX * scaleM).roundToInt()
            val my0 = (ms.posY * scaleM).roundToInt()
            val widthM = (220 * scaleM).roundToInt()
            val heightM = (56 * scaleM).roundToInt()
            if (mx in mx0..(mx0 + widthM) && my in my0..(my0 + heightM)) {
                val win = net.minecraft.client.MinecraftClient.getInstance().window.handle
                val ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS
                if (ctrl) {
                    val cur = ms.opacity
                    val delta = if (vertical > 0) 0.05f else -0.05f
                    ms.opacity = (cur + delta).coerceIn(0.05f, 1.0f)
                } else {
                    val cur = ms.scale
                    val delta = if (vertical > 0) 0.05f else -0.05f
                    ms.scale = (cur + delta).coerceIn(0.5f, 2.0f)
                }
                // do not save every scroll; save will happen on mouse release
                handled = true
            }
        }

        return if (handled) true else super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

}
