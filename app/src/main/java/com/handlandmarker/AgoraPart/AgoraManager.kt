package com.handlandmarker.AgoraPart


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.view.SurfaceView
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.studify.R
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets

open class AgoraManager(context: Context) {
    // The reference to the Android activity you use for video calling
    private val activity: Activity
    protected val mContext: Context

    protected var agoraEngine: RtcEngine? = null // The RTCEngine instance
    protected var mListener: AgoraManagerListener? = null // The event handler for AgoraEngine events
    protected var config: JSONObject? // Configuration parameters from the config.json file
    protected val appId: String // Your App ID from Agora console
    var currentProduct = ProductName.VIDEO_CALLING // The Agora product to test
    var channelName: String // The name of the channel to join
    var localUid: Int // UID of the local user
    var remoteUids = HashSet<Int>() // An object to store uids of remote users
    var isJoined = false // Status of the video call
        private set
    var isBroadcaster = true // Local user role
    fun setBroadcasterRole(isBroadcaster: Boolean) {
        this.isBroadcaster = isBroadcaster
    }

    enum class ProductName {
        VIDEO_CALLING,
        VOICE_CALLING,
        INTERACTIVE_LIVE_STREAMING,
        BROADCAST_STREAMING
    }

    init {
        config = readConfig(context)
        appId = config!!.optString("appId")
        channelName = config!!.optString("channelName")
        localUid = config!!.optInt("uid")
        mContext = context
        activity = mContext as Activity
        if (!checkSelfPermission()) {
            ActivityCompat.requestPermissions(activity, REQUESTED_PERMISSIONS, PERMISSION_REQ_ID)
        }
    }

    fun setListener(mListener: AgoraManagerListener?) {
        this.mListener = mListener
    }

    private fun readConfig(context: Context): JSONObject? {
        // Read parameters from the config.json file
        try {
            val inputStream = context.resources.openRawResource(R.raw.config)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, StandardCharsets.UTF_8)
            return JSONObject(jsonString)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    val localVideo: SurfaceView
        get() {
            // Create a SurfaceView object for the local video
            val localSurfaceView = SurfaceView(mContext)
            localSurfaceView.visibility = View.VISIBLE
            // Call setupLocalVideo with a VideoCanvas having uid set to 0.
            agoraEngine!!.setupLocalVideo(
                VideoCanvas(
                    localSurfaceView,
                    VideoCanvas.RENDER_MODE_HIDDEN,
                    0
                )
            )
            return localSurfaceView
        }

    protected fun setupRemoteVideo(remoteUid: Int) {
        // Create a new SurfaceView
        val remoteSurfaceView = SurfaceView(mContext)
        remoteSurfaceView.setZOrderMediaOverlay(true)
        // Create a VideoCanvas using the remoteSurfaceView
        val videoCanvas = VideoCanvas(
            remoteSurfaceView,
            VideoCanvas.RENDER_MODE_FIT, remoteUid
        )
        agoraEngine!!.setupRemoteVideo(videoCanvas)
        // Set the visibility
        remoteSurfaceView.visibility = View.VISIBLE
        // Notify the UI to display the video
        mListener!!.onRemoteUserJoined(remoteUid, remoteSurfaceView)
    }




    open fun leaveChannel() {
        if (!isJoined) {
            sendMessage("Join a channel first")
        } else {
            // To leave a channel, call the `leaveChannel` method
            agoraEngine!!.leaveChannel()
            sendMessage("You left the channel")

            // Set the `joined` status to false
            isJoined = false
            // Destroy the engine instance
            destroyAgoraEngine()
        }
    }

    protected fun destroyAgoraEngine() {
        // Release the RtcEngine instance to free up resources
        RtcEngine.destroy()
        agoraEngine = null
    }



    protected fun checkSelfPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            mContext,
            REQUESTED_PERMISSIONS[0]
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    mContext,
                    REQUESTED_PERMISSIONS[1]
                ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        protected const val PERMISSION_REQ_ID = 22
        protected val REQUESTED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
    }

    interface AgoraManagerListener {
        fun onMessageReceived(message: String?)
        fun onRemoteUserJoined(remoteUid: Int, surfaceView: SurfaceView?)
        fun onRemoteUserLeft(remoteUid: Int)
        fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int)
        fun onEngineEvent(eventName: String, eventArgs: Map<String, Any>)
    }

    protected fun sendMessage(message: String?) {
        mListener!!.onMessageReceived(message)
    }

}