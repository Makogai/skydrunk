package me.makogai.skydrunk.dungeons

import me.makogai.skydrunk.config.McManaged
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.block.Blocks
import net.minecraft.client.MinecraftClient
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.VertexConsumer
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import com.mojang.blaze3d.systems.RenderSystem

object TripwireHighlighter {

    fun init() {
        WorldRenderEvents.AFTER_ENTITIES.register(WorldRenderEvents.AfterEntities { context ->
            val cfg = McManaged.data().dungeons
            if (!cfg.highlightTripwire /* || true */) return@AfterEntities

            val mc = MinecraftClient.getInstance() ?: return@AfterEntities
            val world = mc.world ?: return@AfterEntities
            val player = mc.player ?: return@AfterEntities

            val camera = context.camera() ?: return@AfterEntities
            val matrices = context.matrixStack() ?: return@AfterEntities
            val consumers = context.consumers() ?: return@AfterEntities

            val range = MathHelper.floor(cfg.tripwireRange.coerceIn(4f, 64f).toDouble()).coerceAtLeast(1)
            val px = MathHelper.floor(player.x)
            val py = MathHelper.floor(player.y)
            val pz = MathHelper.floor(player.z)

            matrices.push()
            val camPos = camera.pos
            matrices.translate(-camPos.x, -camPos.y, -camPos.z)

            // color (neon magenta)
            val r = 1.0f; val g = 0.2f; val b = 1.0f; val a = 0.95f

            val lineConsumer = consumers.getBuffer(RenderLayer.getLines())

            val minX = px - range
            val maxX = px + range
            val minY = (py - range).coerceAtLeast(world.bottomY)
            val maxY = (py + range).coerceAtMost(world.bottomY + world.height - 1)
            val minZ = pz - range
            val maxZ = pz + range

            for (x in minX..maxX) for (y in minY..maxY) for (z in minZ..maxZ) {
                val pos = BlockPos(x, y, z)
                if (!world.getBlockState(pos).isOf(Blocks.TRIPWIRE)) continue

                // Slightly inflated Y so the string pops
                val cam = camera.pos
                // Slightly inflated so it pops
                val box = Box.of(pos.toCenterPos(), 1.01, 0.11, 1.01)
                drawBoxOutline(cam.x, cam.y, cam.z, lineConsumer, box, r, g, b, a)

                // ---- Optional x-ray pass ----
                // matrices.push()
                // matrices.translate(0.0, 0.0, 0.0005) // tiny view offset
                // drawBoxOutline(matrices, lineConsumer, box, r, g, b, a)
                // matrices.pop()
            }

            matrices.pop()
        })
    }

    private fun end(vc: net.minecraft.client.render.VertexConsumer) {
        // Compatible with mappings that use either next() or endVertex()
        val cls = vc.javaClass
        val mNext = cls.methods.firstOrNull { it.name == "next" && it.parameterCount == 0 }
        if (mNext != null) {
            mNext.invoke(vc); return
        }
        val mEnd = cls.methods.firstOrNull { it.name == "endVertex" && it.parameterCount == 0 }
        if (mEnd != null) {
            mEnd.invoke(vc); return
        }
        // If neither exists (very unlikely), we silently continue.
    }

    private fun drawBoxOutline(
        cameraX: Double, cameraY: Double, cameraZ: Double,
        vc: net.minecraft.client.render.VertexConsumer,
        box: net.minecraft.util.math.Box,
        rf: Float, gf: Float, bf: Float, af: Float
    ) {
        // Pack RGBA -> 0xAARRGGBB (common in these mappings)
        val r = (rf * 255f).toInt().coerceIn(0, 255)
        val g = (gf * 255f).toInt().coerceIn(0, 255)
        val b = (bf * 255f).toInt().coerceIn(0, 255)
        val a = (af * 255f).toInt().coerceIn(0, 255)
        val color = (a shl 24) or (r shl 16) or (g shl 8) or b

        // Fullbright-ish light (safe for lines), overlay = 0; UV unused; normals arbitrary for lines
        val u = 0f; val v = 0f
        val overlay = 0
        val light = 0x00F000F0.toInt()
        val nx = 0f; val ny = 1f; val nz = 0f

        // camera-relative corners (so we don't need matrix transforms)
        val x1 = (box.minX - cameraX).toFloat()
        val y1 = (box.minY - cameraY).toFloat()
        val z1 = (box.minZ - cameraZ).toFloat()
        val x2 = (box.maxX - cameraX).toFloat()
        val y2 = (box.maxY - cameraY).toFloat()
        val z2 = (box.maxZ - cameraZ).toFloat()

        fun seg(xa: Float, ya: Float, za: Float, xb: Float, yb: Float, zb: Float) {
            vc.vertex(xa, ya, za, color, u, v, overlay, light, nx, ny, nz)
            vc.vertex(xb, yb, zb, color, u, v, overlay, light, nx, ny, nz)
        }

        // bottom
        seg(x1,y1,z1, x2,y1,z1); seg(x2,y1,z1, x2,y1,z2)
        seg(x2,y1,z2, x1,y1,z2); seg(x1,y1,z2, x1,y1,z1)
        // top
        seg(x1,y2,z1, x2,y2,z1); seg(x2,y2,z1, x2,y2,z2)
        seg(x2,y2,z2, x1,y2,z2); seg(x1,y2,z2, x1,y2,z1)
        // pillars
        seg(x1,y1,z1, x1,y2,z1); seg(x2,y1,z1, x2,y2,z1)
        seg(x2,y1,z2, x2,y2,z2); seg(x1,y1,z2, x1,y2,z2)
    }


}
