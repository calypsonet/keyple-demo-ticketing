package org.calypsonet.keyple.demo.control.ui.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.calypsonet.keyple.demo.control.domain.model.AuthenticationMode
import org.calypsonet.keyple.demo.control.domain.model.Status

@Parcelize
data class UiCardReaderResponse(
    val status: Status,
    val authenticationMode: AuthenticationMode,
    val lastValidationsList: ArrayList<UiValidation>? = null,
    val titlesList: ArrayList<UiContract>,
    val errorTitle: String? = null,
    val errorMessage: String? = null
) : Parcelable

