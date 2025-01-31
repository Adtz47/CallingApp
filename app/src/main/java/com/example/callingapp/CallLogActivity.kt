package com.example.callingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Locale

class CallLogActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var callLogAdapter: CallLogAdapter
    private lateinit var searchEditText: TextInputEditText
    private lateinit var makeCallButton: MaterialButton
    private var allCallLogs = listOf<CallLogItem>()

    companion object {
        private const val PERMISSIONS_REQUEST_READ_CALL_LOG = 100
        private const val PERMISSIONS_REQUEST_CALL_PHONE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call_log)

        // Initialize views
        recyclerView = findViewById(R.id.call_log_recycler_view)
        searchEditText = findViewById(R.id.searchEditText)
        makeCallButton = findViewById(R.id.fab_call)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Check for call log permission
        checkCallLogPermission()

        // Setup search functionality
        setupSearch()

        // Setup call button
        setupCallButton()
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterCallLogs(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterCallLogs(query: String) {
        val filteredList = if (query.isEmpty()) {
            allCallLogs
        } else {
            allCallLogs.filter { callLog ->
                callLog.number.contains(query, ignoreCase = true) ||
                        callLog.contactName?.contains(query, ignoreCase = true) == true
            }
        }
        callLogAdapter.updateData(filteredList)
    }

    private fun setupCallButton() {
        makeCallButton.setOnClickListener {
            val selectedNumber = callLogAdapter.getSelectedNumber()
            if (selectedNumber != null) {
                checkCallPhonePermission(selectedNumber)
            } else {
                Toast.makeText(this, "Please select a contact to call", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCallLogPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CALL_LOG),
                PERMISSIONS_REQUEST_READ_CALL_LOG
            )
        } else {
            loadCallLogs()
        }
    }

    private fun checkCallPhonePermission(number: String) {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                PERMISSIONS_REQUEST_CALL_PHONE
            )
        } else {
            makePhoneCall(number)
        }
    }

    private fun makePhoneCall(number: String) {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$number")
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to make call", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("Range")
    private fun loadCallLogs() {
        val callLogs = mutableListOf<CallLogItem>()

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.DATE,
                CallLog.Calls.TYPE,
                CallLog.Calls.CACHED_NAME
            ),
            null, null, CallLog.Calls.DATE + " DESC"
        )

        cursor?.use {
            while (it.moveToNext()) {
                val number = it.getString(it.getColumnIndex(CallLog.Calls.NUMBER))
                val date = it.getLong(it.getColumnIndex(CallLog.Calls.DATE))
                val type = it.getInt(it.getColumnIndex(CallLog.Calls.TYPE))
                val contactName = it.getString(it.getColumnIndex(CallLog.Calls.CACHED_NAME))

                val callType = when (type) {
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    else -> "Unknown"
                }

                callLogs.add(CallLogItem(number, date, callType, contactName))
            }
        }

        allCallLogs = callLogs
        callLogAdapter = CallLogAdapter(callLogs)  // Remove the lambda argument
        recyclerView.adapter = callLogAdapter

// Set up the listener separately
        callLogAdapter.setOnItemSelectedListener { number ->
            makeCallButton.isEnabled = true
            makeCallButton.alpha = 1.0f
        }
        recyclerView.adapter = callLogAdapter
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSIONS_REQUEST_READ_CALL_LOG -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadCallLogs()
                } else {
                    Toast.makeText(this, "Permission denied to read call logs", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            PERMISSIONS_REQUEST_CALL_PHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callLogAdapter.getSelectedNumber()?.let { makePhoneCall(it) }
                } else {
                    Toast.makeText(this, "Permission denied to make phone calls", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

data class CallLogItem(
    val number: String,
    val date: Long,
    val type: String,
    val contactName: String? = null
)