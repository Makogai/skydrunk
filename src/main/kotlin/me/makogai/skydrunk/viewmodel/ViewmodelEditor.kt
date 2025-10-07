package me.makogai.skydrunk.viewmodel

import me.makogai.skydrunk.config.McManaged
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder

object ViewmodelEditor {
    fun open() {
        val mc = MinecraftClient.getInstance()
        val vm = McManaged.data().viewmodel

        val builder: ConfigBuilder = ConfigBuilder.create()
            .setTitle(Text.of("Skydrunk Viewmodel Editor"))
            .setTransparentBackground(true)

        val eb: ConfigEntryBuilder = builder.entryBuilder()
        val cat: ConfigCategory = builder.getOrCreateCategory(Text.of("Viewmodel"))

        // toggles
        cat.addEntry(
            eb.startBooleanToggle(Text.of("Enable"), vm.enabled)
                .setSaveConsumer { vm.enabled = it }
                .build()
        )
        cat.addEntry(
            eb.startBooleanToggle(Text.of("Affect Offhand"), vm.affectOffHand)
                .setSaveConsumer { vm.affectOffHand = it }
                .build()
        )

        fun floatField(
            label: String,
            value: Float,
            min: Float? = null,
            max: Float? = null,
            onSave: (Float) -> Unit
        ) = eb.startFloatField(Text.of(label), value).apply {
            min?.let { setMin(it) }
            max?.let { setMax(it) }
            setSaveConsumer(onSave)
        }.build()

        // Offsets
        cat.addEntry(floatField("Offset X", vm.offX, -1f, 1f) { vm.offX = it })
        cat.addEntry(floatField("Offset Y", vm.offY, -1f, 1f) { vm.offY = it })
        cat.addEntry(floatField("Offset Z", vm.offZ, -1f, 1f) { vm.offZ = it })

        // Scale
        cat.addEntry(floatField("Scale", vm.scale, 0.1f, 3.0f) { vm.scale = it })

        // Speeds
        cat.addEntry(floatField("Equip Speed (×)", vm.equipSpeed, 0.25f, 3.0f) { vm.equipSpeed = it })
        cat.addEntry(floatField("Swing Speed (×)", vm.swingSpeed, 0.25f, 3.0f) { vm.swingSpeed = it })

        // Amplitudes
        cat.addEntry(floatField("Swing Amplitude", vm.swingAmplitude, 0f, 1.5f) { vm.swingAmplitude = it })
        cat.addEntry(floatField("Equip Amplitude", vm.equipAmplitude, 0f, 1.5f) { vm.equipAmplitude = it })

        // Rotations
        cat.addEntry(floatField("Rot X (deg)", vm.rotX, -180f, 180f) { vm.rotX = it })
        cat.addEntry(floatField("Rot Y (deg)", vm.rotY, -180f, 180f) { vm.rotY = it })
        cat.addEntry(floatField("Rot Z (deg)", vm.rotZ, -180f, 180f) { vm.rotZ = it })

        mc.setScreen(builder.build())
    }
}
