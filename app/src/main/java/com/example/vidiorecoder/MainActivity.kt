package com.example.vidiorecoder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.iceteck.silicompressorr.SiliCompressor
import java.io.File
import java.net.URISyntaxException

class MainActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private var outRequestCode: Int = 122
    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        videoView = findViewById(R.id.videoView)
        val startButton = findViewById<Button>(R.id.startButton)

        // Set up the media controller for play, pause, etc.
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        videoView.setMediaController(mediaController)

        startButton.setOnClickListener {
            checkCameraPermission()
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission already granted, proceed with video capture
            startVideo()
            videoView.visibility = View.VISIBLE

        }
    }

    private fun startVideo() {
        // Start the intent to capture video
        val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, outRequestCode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with video capture
                startVideo()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == outRequestCode && resultCode == RESULT_OK) {
            // Get video data from URI
            val videoUri = data?.data
            videoView.setVideoURI(videoUri)
            // Set full screen
            videoView.setOnPreparedListener { mp ->
                val videoParams = videoView.layoutParams
                videoParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                videoParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                videoView.layoutParams = videoParams
                videoView.start()

                // Initialize file
                val file = File(videoUri!!.path!!)
                // Initialize uri
                val uri = videoUri
                // Create compress video method
                CompressVideo().execute("false", uri.toString(), file.path)
            }
        }
    }

    private inner class CompressVideo :
        AsyncTask<String, String, String>() {

        override fun onPreExecute() {
            super.onPreExecute()
            // You can show progress dialog here if needed
        }

        override fun doInBackground(vararg strings: String): String {
            // Initialize video path
            var videoPath: String? = null
            try {
                // Initialize uri
                val uri = Uri.parse(strings[1])
                // Compress video
                videoPath = SiliCompressor.with(this@MainActivity)
                    .compressVideo(uri, strings[2])
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            // Return Video path
            return videoPath ?: ""
        }

        override fun onPostExecute(s: String) {
            super.onPostExecute(s)

            // Display a toast message indicating the result of compression
            if (s.isNotEmpty()) {
                Toast.makeText(this@MainActivity, "Video compressed successfully", Toast.LENGTH_SHORT).show()
                // You can also handle the compressed video here if needed
            } else {
                Toast.makeText(this@MainActivity, "Failed to compress video", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
