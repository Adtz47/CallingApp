package com.example.callingapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var gestureDetector: GestureDetector
    private lateinit var telecomManager: TelecomManager
    private val REQUEST_CALL_PERMISSION = 1  // Permission request code

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        // Display the caller number
        val callerNumber = intent.getStringExtra("CALLER_NUMBER")
        findViewById<TextView>(R.id.callerNumber).text = callerNumber ?: "Unknown Caller"

        // Initialize GestureDetector and TelecomManager
        gestureDetector = GestureDetector(this, GestureListener())
        telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager

        // Setup buttons
        findViewById<ImageButton>(R.id.answerCallButton).setOnClickListener { answerCall() }
        findViewById<ImageButton>(R.id.rejectCallButton).setOnClickListener { rejectCall() }


        // Request permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), REQUEST_CALL_PERMISSION)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Detect gestures such as swipe right (answer) or left (reject)
        return gestureDetector.onTouchEvent(event)
    }

    private fun answerCall() {
        // Check if the device is running Android 5.0 (API level 21) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        telecomManager.acceptRingingCall()  // Answer the call on supported devices
                    }
                    Toast.makeText(this, "Call Answered!", Toast.LENGTH_SHORT).show()
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ANSWER_PHONE_CALLS), REQUEST_CALL_PERMISSION)
                    Toast.makeText(this, "Permission required to answer calls!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(this, "Cannot answer call due to security restrictions!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Answer call feature is only available on Android 5.0 and above.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun rejectCall() {
        // Check if the device is running Android 9.0 (API level 28) or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                telecomManager.endCall()  // Reject the call on supported devices
                Toast.makeText(this, "Call Rejected!", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                e.printStackTrace()
                Toast.makeText(this, "Cannot reject call due to security restrictions!", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(this, "Reject call feature is only available on Android 9.0 and above.", Toast.LENGTH_SHORT).show()
        }
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied! Cannot answer calls.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        @Override
        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val deltaX = e2.x - e1!!.x

            if (deltaX > 150) {
                answerCall() // Swipe Right to Answer
            } else if (deltaX < -150) {
                rejectCall() // Swipe Left to Reject
            }

            return true
        }
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }
    }
}