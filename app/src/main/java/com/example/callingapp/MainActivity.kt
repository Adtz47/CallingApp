package com.example.callingapp

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private val REQUEST_CALL_PERMISSION = 1
    private val REQUEST_CONTACTS_PERMISSION = 2
    private val REQUEST_PHONE_STATE_PERMISSION = 3
    private val REQUEST_CODE_SET_DEFAULT_DIALER = 123

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val phoneNumberEditText: EditText = findViewById(R.id.phoneNumberEditText)
        val callButton: ImageButton = findViewById(R.id.callbutton)
        val callLogButton: ImageButton = findViewById(R.id.button3)
        val contactsButton: ImageButton = findViewById(R.id.button4)
        val setDefaultDialerButton: Button = findViewById(R.id.setDefaultDialerButton)

        // Check for CALL_PHONE permission at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PERMISSION)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), REQUEST_PHONE_STATE_PERMISSION)
        }

        // Request ANSWER_PHONE_CALLS permission at runtime (API 28 or higher)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), REQUEST_PHONE_STATE_PERMISSION)
        }

        // Set the onClick listener for the Call Button
        callButton.setOnClickListener {
            val phoneNumber = phoneNumberEditText.text.toString()
            if (phoneNumber.isNotEmpty()) {
                makePhoneCall(phoneNumber)
            } else {
                Toast.makeText(this, "Please enter a phone number", Toast.LENGTH_SHORT).show()
            }
        }

        // Set the onClick listener for the Call Log Button
        callLogButton.setOnClickListener {
            // Open the Call Log
            openCallLogActivity()
        }

        // Set the onClick listener for the Contacts Button
        contactsButton.setOnClickListener {
            openContactsActivity()
        }

        // Set the onClick listener for the Set Default Dialer Button
        setDefaultDialerButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val componentName = ComponentName(packageName, CallActivity::class.java.name)
                packageManager.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
            }
        }
    }

    private fun openContactsActivity() {
        val intent = Intent(this, ContactsActivity::class.java)
        startActivity(intent)
    }

    private fun openCallLogActivity() {
        val intent = Intent(this, CallLogActivity::class.java)
        startActivity(intent)
    }

    // Function to initiate a phone call
    private fun makePhoneCall(phoneNumber: String) {
        val dialIntent = Intent(Intent.ACTION_CALL)
        dialIntent.data = Uri.parse("tel:$phoneNumber")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startActivity(dialIntent)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PERMISSION)
        }
    }

    // Handle the permissions result for CALL_PHONE and READ_CONTACTS
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CALL_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission Denied. Cannot make calls.", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CONTACTS_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Open Contacts if permission is granted
                    val contactsIntent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                    startActivity(contactsIntent)
                } else {
                    Toast.makeText(this, "Permission Denied. Cannot access contacts.", Toast.LENGTH_SHORT).show()
                }
            }

            REQUEST_PHONE_STATE_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Phone State Permission Granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Phone State Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SET_DEFAULT_DIALER) {
            // Handle result of setting default dialer
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "App is set as the default dialer.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to set as default dialer.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
