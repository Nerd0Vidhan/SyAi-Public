package com.mato.syai.ui.theme

import androidx.datastore.core.Serializer
import com.mato.syai.data.datastore.ColorPreferences
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

object ColorPreferencesSerializer : Serializer<ColorPreferences> {

    // 1. Default value if the file is corrupted or doesn't exist
    override val defaultValue: ColorPreferences = ColorPreferences.getDefaultInstance()

    // 2. Read from the input stream
    override suspend fun readFrom(input: InputStream): ColorPreferences {
        try {
            return ColorPreferences.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw androidx.datastore.core.CorruptionException("Cannot read proto.", exception)
        }
    }

    // 3. Write to the output stream
    override suspend fun writeTo(t: ColorPreferences, output: OutputStream) {
        t.writeTo(output)
    }
}