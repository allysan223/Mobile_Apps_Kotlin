package edu.uw.eep523.takepicture

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.divyanshu.draw.widget.DrawView
import com.google.firebase.FirebaseApp
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFace
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.face.FirebaseVisionFaceLandmark
import kotlinx.android.synthetic.main.activity_main.*
import java.io.IOException
import java.util.*



class MainActivity : AppCompatActivity() {

    private var isLandScape: Boolean = false
    private var imageUri: Uri? = null
    private var currentPane = 1
    private var drawViewPic1: DrawView? = null
    private var drawViewPic2: DrawView? = null
    private var imageBitmap1: Bitmap? = null
    private var imageBitmap2: Bitmap? = null

    val options = FirebaseVisionFaceDetectorOptions.Builder()
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
        .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
        .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
        .setMinFaceSize(0.15f)
        .build()



    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        getRuntimePermissions()
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
        isLandScape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        savedInstanceState?.let {
            imageUri = it.getParcelable(KEY_IMAGE_URI)
        }

        // Setup draw view for previewPane 1
        drawViewPic1 = findViewById(R.id.previewPane_1)
        drawViewPic1?.setStrokeWidth(10.0f)
        drawViewPic1?.setColor(Color.WHITE)

        drawViewPic1?.setOnTouchListener { _, event ->
            drawViewPic1?.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
            }
            true
        }

        // Setup draw view for previewPane 2
        drawViewPic2 = findViewById(R.id.previewPane_2)
        drawViewPic2?.setStrokeWidth(10.0f)
        drawViewPic2?.setColor(Color.WHITE)

        drawViewPic2?.setOnTouchListener { _, event ->
            drawViewPic2?.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
            }
            true
        }

        FirebaseApp.initializeApp(this);

    }

    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager
                    .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.isNotEmpty()) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    return false
                }
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            permission?.let {
                if (!isPermissionGranted(this, it)) {
                    allNeededPermissions.add(permission)
                }
            }
        }

        if (allNeededPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                    this, allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        return false
    }


    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
       with(outState) {
            putParcelable(KEY_IMAGE_URI, imageUri)
        }
    }

     fun startCameraIntentForResult(view:View) {
        // Clean up last time's image
        imageUri = null
         if( view.getId() == R.id.b_takePic_1){
             Log.d("DEBUG", "send pic to preview pane 1")
             currentPane = 1
             previewPane_1?.setBackgroundDrawable(null)
         } else if (view.getId() == R.id.b_takePic_2) {
             Log.d("DEBUG", "send pic to preview pane 2")
             currentPane = 2
             //previewPane_1?.background = d

         }



        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePictureIntent.resolveActivity(packageManager)?.let {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "New Picture")
            values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera")
            imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

     fun startChooseImageIntentForResult(view:View) {
         if (view.getId() == R.id.b_selectPic_1){
             currentPane = 1
         } else if (view.getId() == R.id.b_selectPic_2) {
             currentPane = 2
         }
         val intent = Intent()
         intent.type = "image/*"
         intent.action = Intent.ACTION_GET_CONTENT
         startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE)
    }

    fun ClearDrawView(view:View) {
        if (view.getId() == R.id.b_clearPic_1){
            currentPane = 1
            drawViewPic1?.clearCanvas()
            val d: Drawable = BitmapDrawable(resources, imageBitmap1)
            previewPane_1?.background = d

        } else if (view.getId() == R.id.b_clearPic_2) {
            currentPane = 2
            drawViewPic2?.clearCanvas()
            val d: Drawable = BitmapDrawable(resources, imageBitmap2)
            previewPane_2?.background = d
        }
    }

    private fun processFaceResult( result: List<FirebaseVisionFace>) {
        var count = 0;
        for (face in result) {
            val bounds = face.boundingBox
            count++
        }
        Log.d("DEBUG", "number of faces is $count")


    }

    fun detectSmile(bitmap: Bitmap) {
        var bitmap = bitmap?.copy(Bitmap.Config.ARGB_8888,true);
        Log.d("DEBUG","config = $bitmap.Config")

        bitmap = bitmap?.let { rescaleImage(it) }

        val faceImage = FirebaseVisionImage.fromBitmap(bitmap!!)
        //val face2 = FirebaseVisionImage.fromBitmap(imageBitmap2!!)

        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)


        val result = detector.detectInImage(faceImage)
            .addOnSuccessListener { faces ->
                // Task completed successfully
                // [START_EXCLUDE]
                // [START get_face_info]
                for (face in faces) {
                    val bounds = face.boundingBox
                    Log.d("DEBUG","bounds = $bounds")
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                    // nose available):
                    val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                    leftEar?.let {
                        val leftEarPos = leftEar.position
                    }

                    // If classification was enabled:
                    if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                        val smileProb = face.smilingProbability
                        Log.d("DEBUG","smile prob = " + smileProb)
                    }
                    if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                        val leftEyeOpenProb = face.leftEyeOpenProbability
                        Log.d("DEBUG","eyes prob, right eye = $rightEyeOpenProb, left eye = $leftEyeOpenProb")
                        if (rightEyeOpenProb < .70 && leftEyeOpenProb < .70){
                            Toast.makeText (this, "Are your eyes open? Take another pic!", Toast.LENGTH_SHORT).show()
                        }

                    }

                    // If face tracking was enabled:
                    if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                        val id = face.trackingId
                    }



                }
                // [END get_face_info]
                // [END_EXCLUDE]
            }
            .addOnFailureListener{ e ->
                Toast.makeText (this, e.message, Toast.LENGTH_SHORT).show()
                Log.e("DEBUG", "Failed: ${e.message}")
                e.printStackTrace()
            }

    }

    fun rescaleImage(scaleImage: Bitmap): Bitmap {
        var scaleImage = scaleImage

        val width = scaleImage?.width
        val height = scaleImage?.height
        Log.d("DEBUG","hieght = $height, width = $width")

        if (width != null && height != null) {
            if (width > 1000 && height > 1000){
                scaleImage = scaleImage?.let {
                    Bitmap.createScaledBitmap(
                        it,
                        (scaleImage!!.width / 3),
                        (scaleImage!!.height / 3),
                        true)
                }
            }
        }
        return scaleImage
    }

    fun FaceSwap(view:View) {
        imageBitmap1 = imageBitmap1?.let { rescaleImage(it) }

        imageBitmap1 = imageBitmap1?.copy(Bitmap.Config.ARGB_8888,true);
        Log.d("DEBUG","config = $imageBitmap1.Config")

        val face1 = FirebaseVisionImage.fromBitmap(imageBitmap1!!)
        //val face2 = FirebaseVisionImage.fromBitmap(imageBitmap2!!)

        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)


        val result = detector.detectInImage(face1)
            .addOnSuccessListener { faces ->
                // Task completed successfully
                // [START_EXCLUDE]
                // [START get_face_info]
                for (face in faces) {
                    val bounds = face.boundingBox
                    Log.d("DEBUG","bounds = $bounds")
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                    // nose available):
                    val leftEar = face.getLandmark(FirebaseVisionFaceLandmark.LEFT_EAR)
                    leftEar?.let {
                        val leftEarPos = leftEar.position
                    }

                    // If classification was enabled:
                    if (face.smilingProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                        val smileProb = face.smilingProbability
                        Log.d("DEBUG","smile prob = " + smileProb)
                    }
                    if (face.rightEyeOpenProbability != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
                        val rightEyeOpenProb = face.rightEyeOpenProbability
                        val leftEyeOpenProb = face.leftEyeOpenProbability
                        Log.d("DEBUG","eyes prob, right eye = $rightEyeOpenProb, left eye = $leftEyeOpenProb")
                        if (rightEyeOpenProb < 70 && leftEyeOpenProb < 70){
                            Toast.makeText (this, "Are your eyes open? Take another pic!", Toast.LENGTH_SHORT).show()
                        }

                    }

                    // If face tracking was enabled:
                    if (face.trackingId != FirebaseVisionFace.INVALID_ID) {
                        val id = face.trackingId
                    }



                }
                // [END get_face_info]
                // [END_EXCLUDE]
            }
            .addOnFailureListener{ e ->
                Toast.makeText (this, e.message, Toast.LENGTH_SHORT).show()
                Log.e("DEBUG", "Failed: ${e.message}")
                e.printStackTrace()
            }

    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            tryReloadAndDetectInImage()
        } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == Activity.RESULT_OK) {
            // In this case, imageUri is returned by the chooser, save it.
            imageUri = data!!.data
            tryReloadAndDetectInImage()
        }
    }

    private fun tryReloadAndDetectInImage() {
        try {
            if (imageUri == null) {
                return
            }
            val imageBitmap = if (Build.VERSION.SDK_INT < 29) {
                MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            } else {
                val source = ImageDecoder.createSource(contentResolver, imageUri!!)
                ImageDecoder.decodeBitmap(source)
            }
            detectSmile(imageBitmap)
            val d: Drawable = BitmapDrawable(resources, imageBitmap)
            if (currentPane == 1){
                imageBitmap1 = imageBitmap
                previewPane_1?.background = d //previewPane is the ImageView from the layout
            } else if (currentPane == 2){
                imageBitmap2 = imageBitmap
                previewPane_2?.background = d //previewPane is the ImageView from the layout

            }
        } catch (e: IOException) {
        }
    }

    fun blurView(view:View) {
        val scaleFactor = 20
        var imageBitmap = if (Build.VERSION.SDK_INT < 29) {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        } else {
            val source = ImageDecoder.createSource(contentResolver, imageUri!!)
            ImageDecoder.decodeBitmap(source)
        }

        when (view.getId()) {
            R.id.b_blurPic_1 -> imageBitmap = imageBitmap1
            R.id.b_blurPic_2 -> imageBitmap = imageBitmap2
            else -> {
                null
            }
        }

        val resizedBitmap = Bitmap.createScaledBitmap(
            imageBitmap,
            (imageBitmap.width / scaleFactor),
            (imageBitmap.height / scaleFactor),
            true)

        imageBitmap = bitmapBlur(resizedBitmap, 1.0f, 3)
        val d: Drawable = BitmapDrawable(resources, imageBitmap)
        if ( view.getId() == R.id.b_blurPic_1){
            previewPane_1?.background = d //previewPane is the ImageView from the layout

        } else if (view.getId() == R.id.b_blurPic_2) {
            previewPane_2?.background = d //previewPane is the ImageView from the layout
        }


    }

    fun bitmapBlur(sentBitmap: Bitmap, scale: Float, radius: Int): Bitmap? {
        var sentBitmap = sentBitmap
        val width = Math.round(sentBitmap.width * scale)
        val height = Math.round(sentBitmap.height * scale)
        sentBitmap = Bitmap.createScaledBitmap(sentBitmap, width, height, false)

        val bitmap = sentBitmap.copy(Bitmap.Config.ARGB_8888, true)

        if (radius < 1) {
            return null
        }

        val w = bitmap.width
        val h = bitmap.height

        val pix = IntArray(w * h)
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(Math.max(w, h))

        var divsum = div + 1 shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }
        yi = 0
        yw = yi

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            i = -radius
            while (i <= radius) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))]
                sir = stack[i + radius]
                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - Math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius
            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = p and 0xff0000 shr 16
                sir[1] = p and 0x00ff00 shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
                x++

            }
            yw += w
            y++
        }
        x = 0
        while (x < w) {
            bsum = 0
            gsum = bsum
            rsum = gsum
            boutsum = rsum
            goutsum = boutsum
            routsum = goutsum
            binsum = routsum
            ginsum = binsum
            rinsum = ginsum
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = Math.max(0, yp) + x

                sir = stack[i + radius]

                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]

                rbs = r1 - Math.abs(i)

                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs

                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }

                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius
            y = 0
            while (y < h) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = -0x1000000 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w
                }
                p = x + vmin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
                y++
            }
            x++
        }

        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)

        return bitmap

    }



    companion object {
        private const val KEY_IMAGE_URI = "edu.uw.eep523.takepicture.KEY_IMAGE_URI"
        private const val REQUEST_IMAGE_CAPTURE = 1001
        private const val REQUEST_CHOOSE_IMAGE = 1002
        private const val PERMISSION_REQUESTS = 1
    }
}
