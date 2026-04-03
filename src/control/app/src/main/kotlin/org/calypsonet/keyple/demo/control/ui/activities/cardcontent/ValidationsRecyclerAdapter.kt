/* ******************************************************************************
 * Copyright (c) 2021 Calypso Networks Association https://calypsonet.org/
 *
 * See the NOTICE file(s) distributed with this work for additional information
 * regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the BSD 3-Clause License which is available at
 * https://opensource.org/licenses/BSD-3-Clause.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 ****************************************************************************** */
package org.calypsonet.keyple.demo.control.ui.activities.cardcontent

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.calypsonet.keyple.demo.control.databinding.ValidationRecyclerRowBinding
import org.calypsonet.keyple.demo.control.ui.model.UiValidation

class ValidationsRecyclerAdapter(private val validations: ArrayList<UiValidation>) :
    RecyclerView.Adapter<ValidationsRecyclerAdapter.LastValidationHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LastValidationHolder {
    val binding =
        ValidationRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return LastValidationHolder(binding)
  }

  class LastValidationHolder(private val binding: ValidationRecyclerRowBinding) :
      RecyclerView.ViewHolder(binding.root) {

    private var validation: UiValidation? = null

    fun bindItem(validation: UiValidation) {
      this.validation = validation
      val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy\nHH:mm:ss", Locale.ENGLISH)
      binding.titleLocation.text =
          String.format("%s\n%s", validation.name, validation.location.name)
      binding.date.text = validation.dateTime.format(formatter)
    }
  }

  override fun getItemCount() = validations.size

  override fun onBindViewHolder(holder: LastValidationHolder, position: Int) {
    val validationItem = validations[position]
    holder.bindItem(validationItem)
  }
}
