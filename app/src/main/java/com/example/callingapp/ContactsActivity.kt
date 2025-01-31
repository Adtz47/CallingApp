package com.example.callingapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ContactsActivity : AppCompatActivity() {
    private val REQUEST_CODE_READ_CONTACTS = 1
    private val REQUEST_CODE_CALL_PHONE = 2
    private var allContacts = mutableListOf<ContactData>()
    private var filteredContacts = mutableListOf<ContactData>()
    private lateinit var adapter: ContactsAdapter
    private var selectedContact: ContactData? = null

    // Data class for contact information
    data class ContactData(
        val name: String,
        val phoneNumber: String,
        var isSelected: Boolean = false
    )

    // Custom Adapter class within the Activity
    private inner class ContactsAdapter : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nameTextView: TextView = itemView.findViewById(android.R.id.text1)

            init {
                itemView.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        // Deselect previous selection
                        selectedContact?.isSelected = false
                        val previousSelectedPosition = filteredContacts.indexOf(selectedContact)
                        if (previousSelectedPosition != -1) {
                            notifyItemChanged(previousSelectedPosition)
                        }

                        // Update new selection
                        selectedContact = filteredContacts[position]
                        selectedContact?.isSelected = true
                        notifyItemChanged(position)
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(android.R.layout.simple_list_item_1, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contact = filteredContacts[position]
            holder.nameTextView.text = "${contact.name} (${contact.phoneNumber})"

            holder.itemView.setBackgroundColor(
                if (contact.isSelected)
                    ContextCompat.getColor(this@ContactsActivity, android.R.color.holo_blue_light)
                else
                    ContextCompat.getColor(this@ContactsActivity, android.R.color.white)
            )
        }

        override fun getItemCount() = filteredContacts.size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contacts)

        // Setup RecyclerView
        val recyclerView = findViewById<RecyclerView>(R.id.contactsRecyclerView)
        adapter = ContactsAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Setup Search
        setupSearchFunctionality()

        // Setup Call Button
        setupCallButton()

        // Check for permissions and load contacts
        checkPermissionAndLoadContacts()
    }

    private fun setupSearchFunctionality() {
        findViewById<TextInputEditText>(R.id.searchEditText).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterContacts(s?.toString() ?: "")
            }
        })
    }

    private fun filterContacts(query: String) {
        filteredContacts = if (query.isEmpty()) {
            ArrayList(allContacts)
        } else {
            allContacts.filter { contact ->
                contact.name.contains(query, ignoreCase = true) ||
                        contact.phoneNumber.contains(query)
            }.toMutableList()
        }
        adapter.notifyDataSetChanged()
    }

    private fun setupCallButton() {
        findViewById<MaterialButton>(R.id.fab_call).setOnClickListener {
            selectedContact?.let { contact ->
                makePhoneCall(contact.phoneNumber)
            } ?: run {
                Toast.makeText(this, "Please select a contact first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionAndLoadContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            loadContacts()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                REQUEST_CODE_READ_CONTACTS
            )
        }
    }

    private fun loadContacts() {
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        ) ?: return

        val uniqueContacts = HashSet<String>()

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))

                if (uniqueContacts.add(name)) {
                    allContacts.add(ContactData(name, number))
                }
            }
        }

        filteredContacts = ArrayList(allContacts)
        adapter.notifyDataSetChanged()
    }

    private fun makePhoneCall(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            startCall(phoneNumber)
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CODE_CALL_PHONE
            )
        }
    }

    private fun startCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to make call: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    loadContacts()
                } else {
                    Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_CODE_CALL_PHONE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectedContact?.let { contact ->
                        startCall(contact.phoneNumber)
                    }
                } else {
                    Toast.makeText(this, "Permission denied to make calls", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}