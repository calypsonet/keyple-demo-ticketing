package org.calypsonet.keyple.demo.control.ui.mappers

import org.calypsonet.keyple.demo.control.domain.model.Contract
import org.calypsonet.keyple.demo.control.ui.model.UiContract

fun Contract.toUi(): UiContract =
    UiContract(
        name = name,
        valid = valid,
        validationDateTime = validationDateTime,
        record = record,
        expired = expired,
        contractValidityStartDate = contractValidityStartDate,
        contractValidityEndDate = contractValidityEndDate,
        nbTicketsLeft = nbTicketsLeft
)

fun UiContract.toDomain(): Contract =
    Contract(
        name = name,
        valid = valid,
        validationDateTime = validationDateTime,
        record = record,
        expired = expired,
        contractValidityStartDate = contractValidityStartDate,
        contractValidityEndDate = contractValidityEndDate,
        nbTicketsLeft = nbTicketsLeft
)