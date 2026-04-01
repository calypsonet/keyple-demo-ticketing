package org.calypsonet.keyple.demo.control.domain.model

data class ControlResult(
    val status: Status,
    val authenticationMode: AuthenticationMode,
    val lastValidationsList: ArrayList<Validation>? = null,
    val titlesList: ArrayList<Contract>,
    val errorTitle: String? = null,
    val errorMessage: String? = null
)