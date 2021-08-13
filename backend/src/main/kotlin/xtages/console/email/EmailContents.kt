package xtages.console.email

import com.fizzed.rocker.runtime.StringBuilderOutput

data class EmailContents(val subject: String, val html: StringBuilderOutput, val plain: StringBuilderOutput)
