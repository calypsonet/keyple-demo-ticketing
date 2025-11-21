package org.calypsonet.keyple.demo.validation.ui.activities

import android.widget.Toast
import dagger.android.support.DaggerAppCompatActivity
import org.calypsonet.keyple.demo.validation.domain.TicketingService
import javax.inject.Inject

abstract class BaseActivity : DaggerAppCompatActivity() {

  @Inject
  lateinit var ticketingService: TicketingService

  fun showToast(message: String) {
    runOnUiThread { Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show() }
  }
}