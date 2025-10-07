package me.makogai.skydrunk.dungeons

import me.makogai.skydrunk.config.McManaged
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumerProvider
import net.minecraft.client.render.VertexRendering
import net.minecraft.client.render.debug.DebugRenderer
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper

object TripwireHighlighter {

    fun init() {
        WorldRenderEvents.AFTER_ENTITIES.register { context ->
            val cfg = McManaged.data().dungeons
            if (!cfg.highlightTripwire) return@register


            val mc = MinecraftClient.getInstance()
            val world = mc.world ?: return@register
            val player = mc.player ?: return@register

            val matrices: MatrixStack = context.matrixStack() ?: return@register
            val consumers: VertexConsumerProvider = context.consumers() ?: return@register
            val cam = context.camera().pos

            val range = MathHelper.floor(cfg.tripwireRange.coerceIn(4f, 48f).toDouble()).coerceAtLeast(1)
            val p = BlockPos.ofFloored(player.pos)
            val minX = p.x - range; val maxX = p.x + range
            val minY = (p.y - range).coerceAtLeast(world.bottomY)
            val maxY = (p.y + range).coerceAtMost(world.bottomY + world.height - 1)
            val minZ = p.z - range; val maxZ = p.z + range

            // Loud color
            val r = 1.0f; val g = 1.0f; val b = 0.15f
            val outlineA = 1.0f
            val fillA = 0.55f

            val targets = ArrayList<Box>()
            for (x in minX..maxX) for (y in minY..maxY) for (z in minZ..maxZ) {
                val pos = BlockPos(x, y, z)
                if (!world.getBlockState(pos).isOf(Blocks.TRIPWIRE)) continue
                val c = pos.toCenterPos().add(0.0, -0.46, 0.0) // tripwire string ~0.04 above floor
                targets += Box.of(c, 1.00, 0.22, 1.00).expand(0.002)
            }
            if (targets.isEmpty()) return@register

            val lineBuf = consumers.getBuffer(RenderLayer.getLines())
            val fillBuf = consumers.getBuffer(RenderLayer.getDebugFilledBox()) // translucent quads

            matrices.push()
            try {
                for (box in targets) {
                    val x1 = (box.minX - cam.x).toFloat()
                    val y1 = (box.minY - cam.y).toFloat()
                    val z1 = (box.minZ - cam.z).toFloat()
                    val x2 = (box.maxX - cam.x).toFloat()
                    val y2 = (box.maxY - cam.y).toFloat()
                    val z2 = (box.maxZ - cam.z).toFloat()

                    // 1) Translucent fill (very visible)
                    VertexRendering.drawFilledBox(matrices, fillBuf, x1, y1, z1, x2, y2, z2, r, g, b, fillA)

                    // 2) Solid outline (normal depth)
                    VertexRendering.drawBox(matrices, lineBuf, x1.toDouble(), y1.toDouble(), z1.toDouble(),
                        x2.toDouble(), y2.toDouble(), z2.toDouble(), r, g, b, outlineA)

                    // 3) ESP outline (through walls) via DebugRenderer (no manual GL state needed)
                    DebugRenderer.drawBox(
                        matrices, consumers,
                        x1.toDouble(), y1.toDouble(), z1.toDouble(),
                        x2.toDouble(), y2.toDouble(), z2.toDouble(),
                        r, g, b, 0.9f
                    )
                }
            } finally {
                matrices.pop()
            }
        }
    }
}
