package edu.uw.eep523.takepicture

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Rect
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
import com.google.firebase.ml.vision.common.FirebaseVisionPoint
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
    private var imageBitmap1_orig: Bitmap? = null
    private var imageBitmap2_orig: Bitmap? = null

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
            if ( imageBitmap1_orig == null ){
                Toast.makeText (this, "Select an image!", Toast.LENGTH_SHORT).show()
                return
            }
            currentPane = 1
            drawViewPic1?.clearCanvas()
            val d: Drawable = BitmapDrawable(resources, imageBitmap1_orig)
            previewPane_1?.background = d

        } else if (view.getId() == R.id.b_clearPic_2) {
            if ( imageBitmap2_orig  == null){
                Toast.makeText (this, "Select an image!", Toast.LENGTH_SHORT).show()
                return
            }
            currentPane = 2
            drawViewPic2?.clearCanvas()
            val d: Drawable = BitmapDrawable(resources, imageBitmap2_orig)
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
        Log.d("DEBUG","rescaled hieght = " + scaleImage?.height + " rescaled width = " + scaleImage?.width)
        return scaleImage
    }

    fun FaceSwap(view:View) {
        if ( imageBitmap1 == null || imageBitmap2 == null){
            Toast.makeText (this, "Select an image!", Toast.LENGTH_SHORT).show()
            return
        }

        imageBitmap1 = imageBitmap1?.let { rescaleImage(it) }
        imageBitmap2 = imageBitmap2?.let { rescaleImage(it) }


        imageBitmap1 = imageBitmap1?.copy(Bitmap.Config.ARGB_8888,true);
        imageBitmap2 = imageBitmap2?.copy(Bitmap.Config.ARGB_8888,true);

        Log.d("DEBUG","config pic 1 = $imageBitmap1.Config")
        Log.d("DEBUG","config pic 2 = $imageBitmap2.Config")

        val face1 = FirebaseVisionImage.fromBitmap(imageBitmap1!!)
        val face2 = FirebaseVisionImage.fromBitmap(imageBitmap2!!)

        val detector = FirebaseVision.getInstance()
            .getVisionFaceDetector(options)

        //used to count number of faces in each photo
        var faceCount1 = 0
        var faceCount2 = 0
        //used to get bounds of face in each photo
        var bounds_pic1: Rect? = null
        var bounds_pic2: Rect? = null
        //used to store just face from each photo
        var bitmap_face1_cutout: Bitmap?
        var bitmap_face2_cutout: Bitmap?
        //create a copy of the imageBitmap to swap face into a new bitmap
        var bitmap_face1: Bitmap?
        var bitmap_face2: Bitmap?
        var nose_pic1: FirebaseVisionPoint? = null



        val result_pic1 = detector.detectInImage(face1)
            .addOnSuccessListener { faces ->
                // Task completed successfully
                // [START_EXCLUDE]
                // [START get_face_info]
                for (face in faces) {
                    bounds_pic1 = face.boundingBox
                    //Log.d("DEBUG","bounds pic 1 = $bounds_pic1")
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees

                    // If landmark detection was enabled (mouth, ears, eyes, cheeks, and
                    // nose available):
                    val nose = face.getLandmark(FirebaseVisionFaceLandmark.NOSE_BASE)
                    nose?.let {
                        nose_pic1 = nose.position
                    }

                    faceCount1++

                }
            }
            .addOnFailureListener{ e ->
                Toast.makeText (this, e.message, Toast.LENGTH_SHORT).show()
                Log.e("DEBUG", "Pic 1 Failed: ${e.message}")
                e.printStackTrace()
            }
            .addOnCompleteListener(){
                Log.d("DEBUG", "faces in pic 1 (complete): $faceCount1, pic 2: $faceCount2")
            }

        val result_pic2 = detector.detectInImage(face2)
            .addOnSuccessListener { faces ->
                // Task completed successfully
                // [START_EXCLUDE]
                // [START get_face_info]
                for (face in faces) {
                    bounds_pic2 = face.boundingBox
                    //Log.d("DEBUG","bounds pic 2 = $bounds_pic2")
                    val rotY = face.headEulerAngleY // Head is rotated to the right rotY degrees
                    val rotZ = face.headEulerAngleZ // Head is tilted sideways rotZ degrees



                    faceCount2++

                }
            }
            .addOnFailureListener{ e ->
                Toast.makeText (this, e.message, Toast.LENGTH_SHORT).show()
                Log.e("DEBUG", "Pic 2 Failed: ${e.message}")
                e.printStackTrace()
            }
            .addOnCompleteListener(){
                Log.d("DEBUG", "faces in pic 1 (complete): $faceCount1, pic 2 (complete): $faceCount2")
                if (faceCount1 > 1 || faceCount2 > 1){
                    Toast.makeText (this, "Too many faces in the photo!", Toast.LENGTH_SHORT).show()
                    Log.d("DEBUG", "TOO MANY FACES RETURN \n - faces in pic 1: $faceCount1, pic 2: $faceCount2")
                    return@addOnCompleteListener
                } else if (faceCount1 < 1 || faceCount2 < 1) {
                    Toast.makeText (this, "No faces detected, please try another photo.", Toast.LENGTH_SHORT).show()
                    Log.d("DEBUG", "no faces detected \n - faces in pic 1: $faceCount1, pic 2: $faceCount2")
                    return@addOnCompleteListener
                }

                val bottom = bounds_pic1?.bottom
                val left = bounds_pic1?.left
                val top = bounds_pic1?.top
                val right = bounds_pic1?.right


                Log.d("DEBUG","bounds pic 1 = $bounds_pic1")
                Log.d("DEBUG","done processing - left bounds pic 1 = $left")
                Log.d("DEBUG","done processing - top bounds pic 1 = $top")
                Log.d("DEBUG","done processing - right bounds pic 1 = $right")
                Log.d("DEBUG","done processing - bottom bounds pic 1 = $bottom")
                Log.d("DEBUG","done processing - nose pic 1 = $nose_pic1")

                val nose_x = nose_pic1?.getX()?.minus(100)?.toInt()
                Log.d("DEBUG","done processing - bounds pic 2 = $bounds_pic2")

                //create copy of bitmap of just the face in first pic
                bitmap_face1_cutout = Bitmap.createBitmap(imageBitmap1!!, left!!, top!!, bounds_pic1?.width()!!, bounds_pic1?.height()!!)

                //create copy of bitmap of just the face in second pic
                bitmap_face2_cutout = Bitmap.createBitmap(imageBitmap2!!, bounds_pic2!!.left, bounds_pic2!!.top, bounds_pic2?.width()!!, bounds_pic2?.height()!!)


                //bitmap_face1 = Bitmap.createScaledBitmap( bitmap_face1!!, (bitmap_face1!!.width / 2), (bitmap_face1!!.height / 2), true)
                Log.d("DEBUG","face 1 rescaled hieght = " + bitmap_face1_cutout!!.getWidth() + " rescaled width = " + bitmap_face1_cutout!!.getHeight())
                Log.d("DEBUG","face 2 rescaled hieght = " + bitmap_face2_cutout!!.getWidth() + " rescaled width = " + bitmap_face2_cutout!!.getHeight())


                val bitmapSize = bitmap_face1_cutout!!.getWidth() * bitmap_face1_cutout!!.getHeight()
                val intArrayFace1 = IntArray(bitmapSize)
                Log.d("DEBUG","int array init size =  $bitmapSize")
                val intArrayFace2 = IntArray(bitmap_face2_cutout!!.getWidth() * bitmap_face2_cutout!!.getHeight())

                //get pixels to send to second pic
                Log.d("DEBUG","BEFORE: int array with colors = " + intArrayFace1[0])
                bitmap_face1_cutout!!.getPixels(
                    intArrayFace1,
                    0,
                    bitmap_face1_cutout!!.getWidth(),
                    0,
                    0,
                    bitmap_face1_cutout!!.getWidth(),
                    bitmap_face1_cutout!!.getHeight()
                )

                Log.d("DEBUG","AFTER: int array with colors = " + intArrayFace1[0])

                //get pixels to send to first pic
                bitmap_face2_cutout!!.getPixels(
                    intArrayFace2,
                    0,
                    bitmap_face2_cutout!!.getWidth(),
                    0,
                    0,
                    bitmap_face2_cutout!!.getWidth(),
                    bitmap_face2_cutout!!.getHeight()
                )

                //add face from first to second pic
                bitmap_face2 = Bitmap.createBitmap(imageBitmap2!!, 0, 0, imageBitmap2!!.getWidth()!!, imageBitmap2?.getHeight()!!)
                bitmap_face2 = bitmap_face2?.copy(Bitmap.Config.ARGB_8888,true);

                //add face from first to second pic
                bitmap_face1 = Bitmap.createBitmap(imageBitmap1!!, 0, 0, imageBitmap1!!.getWidth()!!, imageBitmap1?.getHeight()!!)
                bitmap_face1 = bitmap_face1?.copy(Bitmap.Config.ARGB_8888,true);

                Log.d("DEBUG","int bitmap face 2 copy - test")
                Log.d("DEBUG","face 2 rescaled hieght = " + bitmap_face2!!.getWidth() + " rescaled width = " + bitmap_face2!!.getHeight())

                //add face from first bitmap to the second bitmap
                bitmap_face2!!.setPixels( //edit second bitmap
                    intArrayFace1, //colors from first face
                    0,
                    bitmap_face1_cutout!!.getWidth(),
                    bounds_pic2!!.left, //offset for second face
                    bounds_pic2!!.top,
                    bitmap_face1_cutout!!.getWidth(), //sizing from first face
                    bitmap_face1_cutout!!.getHeight()
                )

                //add face from second bitmap to the first bitmap
                bitmap_face1!!.setPixels( //edit first bitmap
                    intArrayFace2, //colors from second face
                    0,
                    bitmap_face2_cutout!!.getWidth(),
                    bounds_pic1!!.left, //get offset for first face
                    bounds_pic1!!.top,
                    bitmap_face2_cutout!!.getWidth(), //sizing from second face
                    bitmap_face2_cutout!!.getHeight()
                )

                Log.d("DEBUG","int bitmap face 2 copy pixel set - test")

                //create copy of bitmap of just the face
                //bitmap_face2 = Bitmap.createBitmap(imageBitmap2!!, left!!, top!!, imageBitmap2!!.getWidth()!!, imageBitmap2?.getHeight()!!)
                val d1: Drawable = BitmapDrawable(resources, bitmap_face1)
                previewPane_1?.background = d1
                val d2: Drawable = BitmapDrawable(resources, bitmap_face2)
                previewPane_2?.background = d2


            }

        Log.d("DEBUG","this line of code is right after the face detect ")


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
                imageBitmap1_orig = imageBitmap
                previewPane_1?.background = d //previewPane is the ImageView from the layout
            } else if (currentPane == 2){
                imageBitmap2 = imageBitmap
                imageBitmap2_orig = imageBitmap
                previewPane_2?.background = d //previewPane is the ImageView from the layout

            }
        } catch (e: IOException) {
        }
    }

    fun blurView(view:View) {
        val scaleFactor = 20
        val canBlurPic1 = imageBitmap1 != null && view.getId() == R.id.b_blurPic_1
        val canBlurPic2 = imageBitmap2 != null && view.getId() == R.id.b_blurPic_2

        if ( !canBlurPic1 && !canBlurPic2 ) {
            Toast.makeText (this, "Select an image!", Toast.LENGTH_SHORT).show()
            return
        }

        var imageBitmap = if (Build.VERSION.SDK_INT < 29) {
            MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
        } else {
            val source = ImageDecoder.createSource(contentResolver, imageUri!!)
            ImageDecoder.decodeBitmap(source)
        }

        when (view.getId()) {
            R.id.b_blurPic_1 -> {
                imageBitmap = imageBitmap1
            }
            R.id.b_blurPic_2 -> {
                imageBitmap = imageBitmap2
            }
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
