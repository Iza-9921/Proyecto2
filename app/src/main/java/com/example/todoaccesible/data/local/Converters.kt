package com.example.todoaccesible.data.local

import androidx.room.TypeConverter
import com.example.todoaccesible.data.model.AnswerValue
import com.example.todoaccesible.data.model.Credito
import com.example.todoaccesible.data.model.DiagnosticStatus
import com.example.todoaccesible.data.model.Nivel
import com.example.todoaccesible.data.model.Role

class Converters {
    @TypeConverter
    fun fromRole(value: Role): String = value.name

    @TypeConverter
    fun toRole(value: String): Role = Role.valueOf(value)

    @TypeConverter
    fun fromCredito(value: Credito): String = value.name

    @TypeConverter
    fun toCredito(value: String): Credito = Credito.valueOf(value)

    @TypeConverter
    fun fromAnswerValue(value: AnswerValue?): String? = value?.name

    @TypeConverter
    fun toAnswerValue(value: String?): AnswerValue? = value?.let { AnswerValue.valueOf(it) }

    @TypeConverter
    fun fromNivel(value: Nivel?): String? = value?.name

    @TypeConverter
    fun toNivel(value: String?): Nivel? = value?.let { Nivel.valueOf(it) }

    @TypeConverter
    fun fromDiagnosticStatus(value: DiagnosticStatus): String = value.name

    @TypeConverter
    fun toDiagnosticStatus(value: String): DiagnosticStatus = DiagnosticStatus.valueOf(value)
}
