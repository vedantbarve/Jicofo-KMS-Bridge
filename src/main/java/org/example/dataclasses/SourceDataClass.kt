package org.example.dataclasses

data class SourceDataClass (val ssrc : String = "",val parameters : List<SourceParameterDataClass> = emptyList())