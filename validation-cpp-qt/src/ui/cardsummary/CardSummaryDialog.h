/* ******************************************************************************
 * Copyright (c) 2025 Calypso Networks Association https://calypsonet.org/
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */

#pragma once

#include <QDialog>
#include "domain/model/CardReaderResponse.h"

namespace Ui {
class CardSummaryDialog;
}

namespace ui {

/**
 * @brief Dialog displaying validation results
 *
 * Équivalent de CardSummaryActivity.kt
 */
class CardSummaryDialog : public QDialog
{
    Q_OBJECT

public:
    explicit CardSummaryDialog(const domain::model::CardReaderResponse& response,
                              QWidget *parent = nullptr);
    ~CardSummaryDialog();

private:
    Ui::CardSummaryDialog *ui;
    domain::model::CardReaderResponse m_response;

    /**
     * @brief Apply styling based on validation status
     */
    void applyStatusStyling();

    /**
     * @brief Display result details
     */
    void displayResult();

    /**
     * @brief Play audio feedback
     */
    void playAudioFeedback();
};

} // namespace ui
