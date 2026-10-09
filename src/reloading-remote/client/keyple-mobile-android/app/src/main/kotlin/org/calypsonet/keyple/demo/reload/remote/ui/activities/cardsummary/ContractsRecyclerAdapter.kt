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
package org.calypsonet.keyple.demo.reload.remote.ui.activities.cardsummary

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.calypsonet.keyple.demo.reload.remote.databinding.ContractRecyclerRowBinding
import org.calypsonet.keyple.demo.reload.remote.ui.model.UiContract

class ContractsRecyclerAdapter(private val contracts: List<UiContract>) :
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
      this.contract = contract
      binding.contractName.text = contract.name
      binding.contractDescription.text = contract.description
    }
  }

  override fun getItemCount() = contracts.size

  override fun onBindViewHolder(holder: ContractHolder, position: Int) {
    val contractItem = contracts[position]
    holder.bindItem(contractItem)
  }
}
