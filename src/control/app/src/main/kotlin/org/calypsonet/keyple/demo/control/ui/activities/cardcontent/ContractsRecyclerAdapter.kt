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
import java.util.*
import org.calypsonet.keyple.demo.common.model.type.PriorityCode
import org.calypsonet.keyple.demo.control.R
import org.calypsonet.keyple.demo.control.databinding.ContractRecyclerRowBinding
import org.calypsonet.keyple.demo.control.ui.model.UiContract

class ContractsRecyclerAdapter(private val contracts: ArrayList<UiContract>) :
    RecyclerView.Adapter<ContractsRecyclerAdapter.ContractHolder>() {

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContractHolder {
    val binding =
        ContractRecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    return ContractHolder(binding)
  }

  class ContractHolder(private val binding: ContractRecyclerRowBinding) :
      RecyclerView.ViewHolder(binding.root) {

    private var contract: UiContract? = null

    fun bindItem(contract: UiContract) {
      val context = binding.root.context
      val contractDescription =
          if (contract.name == PriorityCode.SEASON_PASS.value) {
            val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH)
            context.getString(
                R.string.card_content_description_season_pass,
                contract.contractValidityStartDate.format(formatter),
                contract.contractValidityEndDate.format(formatter))
          } else {
            when (val remainingTrips = contract.remainingTrips ?: 0) {
              0 -> context.getString(R.string.card_content_description_multi_trip_zero)
              1 ->
                  context.getString(
                      R.string.card_content_description_multi_trip_single, remainingTrips)
              else ->
                  context.getString(
                      R.string.card_content_description_multi_trip_multiple, remainingTrips)
            }
          }
      this.contract = contract
      binding.contractName.text = contract.name
      binding.contractDescription.text = contractDescription
      binding.validImg.setImageResource(
          if (contract.valid) R.drawable.ic_tick else R.drawable.ic_fail)
    }
  }

  override fun getItemCount() = contracts.size

  override fun onBindViewHolder(holder: ContractHolder, position: Int) {
    val contractItem = contracts[position]
    holder.bindItem(contractItem)
  }
}
