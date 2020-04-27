package edu.uw.eep523.pullups

import android.app.AlertDialog
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.WindowManager
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import java.text.NumberFormat
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt



class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mSeriesXaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesYaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesZaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesMagAccel: LineGraphSeries<DataPoint>

    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var mSensorG: Sensor

    var pullUpMode = modes[0]
    var pullUpStarted : Boolean = false
    var pullUpPosUp : Boolean = false
    var pullUpCounter = 0

    //on app start up, speed is high, this bypasses that to prevent pull up mode to start instantly
    var initFlag = false
    var shakeFlag = false

    val accel: Array<Float> = arrayOf(0.0f,0.0f,0.0f,0.0f)
    val last_accel: Array<Float> = arrayOf(0.0f,0.0f,0.0f,0.0f)
    var mean = 0.0

    private var slidingWindow = DoubleArray(5)
    private var meanWindow = DoubleArray(2)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        mSensor = if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else {
            // Sorry, there are no accelerometers on your device.
            null!!
        }
        mSensorG =  (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE))

        mSensorManager.registerListener(this, mSensor, 40000)

        mSeriesXaccel = LineGraphSeries()
        mSeriesYaccel = LineGraphSeries()
        mSeriesZaccel = LineGraphSeries()
        mSeriesMagAccel = LineGraphSeries()

    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }


    override fun onSensorChanged(event: SensorEvent) {
        // In this example, alpha is calculated as t / (t + dT),
        // where t is the low-pass filter's time-constant and
        // dT is the event delivery rate.
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER)
            return

        /*
             * It is not necessary to get accelerometer events at a very high
             * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
             * automatic low-pass filter, which "extracts" the gravity component
             * of the acceleration. As an added benefit, we use less power and
             * CPU resources.
       */

        // read values from accelerometer and calculate magnitude
        accel[0] = event.values[0] // X
        accel[1] = event.values[1] // Y
        accel[2] = event.values[2] // Z
        accel[3] = magnitude(accel[0], accel[1], accel[2]) // magnitude
        //Log.d("sensor", "magnitude = ${accel[3].toDouble()}")
        mean = addToWindow(slidingWindow, accel[3].toDouble())

        // plot values
        val xval = System.currentTimeMillis()/1000.toDouble()//graphLastXValue += 0.1
        mSeriesXaccel!!.appendData(DataPoint(xval, accel[0].toDouble()), true, 50)
        mSeriesYaccel!!.appendData(DataPoint(xval, accel[1].toDouble()), true, 50)
        mSeriesZaccel!!.appendData(DataPoint(xval, accel[2].toDouble()), true, 50)
        mSeriesMagAccel!!.appendData(DataPoint(xval, accel[3].toDouble()), true, 50)

        // calculate speed to determine shake
        val speed: Float =
            Math.abs(accel[0] + accel[1] + accel[2] - last_accel[0] - last_accel[1] - last_accel[2]) / 4 * 10000

        //shake detected
        if (speed > SHAKE_THRESHOLD && initFlag && !shakeFlag ) {
            Log.d("sensor", "shake detected w/ speed: $speed")
            showDialog()
            shakeFlag = true
        }

        last_accel[0] = accel[0]
        last_accel[1] = accel[1]
        last_accel[2] = accel[2]

        // detect pull ups if in pull up mode
        addToWindow(meanWindow, mean)
        if (pullUpStarted && initFlag) {
            //Log.d("sensor", "pull up detected")
            Log.d("sensor", "mean of sliding window = $mean")
            if (meanWindow[0] > 11) {
                if (!pullUpPosUp){
                    if (floor(meanWindow[1]) == 10.0) {
                        pullUpPosUp = !pullUpPosUp //set pos to up
                        pullUpCounter += 1
                        Log.d("sensor", "PULL UP POS = UP")
                    }
                } else if (pullUpPosUp) {
                    if (floor(meanWindow[1]) == 10.0) {
                        pullUpPosUp = !pullUpPosUp //set pos to down
                        Log.d("sensor", "PULL UP POS = DOWN")
                    }
                }
                //Log.d("sensor", "PULL UP POS = $pullUpPos")
            }

        }

        //on app start up, speed is high, this bypasses that to prevent pull up mode to start instantly
        initFlag = true

    }

    private fun magnitude(x: Float, y: Float, z: Float): Float {
        var mag = x.pow(2) + y.pow(2) + z.pow(2)
        mag = sqrt(mag)
        return mag
    }

    private fun addToWindow(window: DoubleArray, x: Double) : Double {
        var sum = 0.0;
        var values = ""
        // Shift everything one to the left
        for (i in 1 until window.size) {
            window[i - 1] = window[i]
        }
        // Add the new data point
        window[window.size - 1] = x

        for (num in window){
            sum += num
            values += "$num "
        }

        //Log.d("sensor", "sliding window values: " + values)
        return sum/window.size
    }


    private fun initGraphRT(mGraph: GraphView, mSeriesXaccel :LineGraphSeries<DataPoint>){

        mGraph.getViewport().setXAxisBoundsManual(true)
        //mGraph.getViewport().setMinX(0.0)
        //mGraph.getViewport().setMaxX(4.0)
        mGraph.getViewport().setYAxisBoundsManual(true);


        mGraph.getViewport().setMinY(0.0);
        mGraph.getViewport().setMaxY(12.0);
        mGraph.getGridLabelRenderer().setLabelVerticalWidth(100)

        // first mSeries is a line
        mSeriesXaccel.setDrawDataPoints(false)
        mSeriesXaccel.setDrawBackground(false)
        mGraph.addSeries(mSeriesXaccel)
        setLabelsFormat(mGraph,1,2)

    }

    /* Formatting the plot*/
    fun setLabelsFormat(mGraph:GraphView,maxInt:Int,maxFraction:Int){
        val nf = NumberFormat.getInstance()
        nf.setMaximumFractionDigits(maxFraction)
        nf.setMaximumIntegerDigits(maxInt)

        mGraph.getGridLabelRenderer().setVerticalAxisTitle("Accel data")
        mGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time")

        mGraph.getGridLabelRenderer().setLabelFormatter(object : DefaultLabelFormatter(nf,nf) {
            override fun formatLabel(value: Double, isValueX: Boolean): String {
                return if (isValueX) {
                    super.formatLabel(value, isValueX)+ "s"
                } else {
                    super.formatLabel(value, isValueX)
                }
            }
        })

    }


    override fun onResume() {
        Log.d("tag","onResume")
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
        Log.d("tag","onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        mSensorManager.unregisterListener(this)
    }

    private fun showDialog() {
        val taskEditText = EditText(this)
        taskEditText.setInputType(InputType.TYPE_CLASS_NUMBER);

        // Initialize a new instance of
        val builder = AlertDialog.Builder(this@MainActivity)

        // Set the alert dialog title
        builder.setTitle("Starting Pull Ups!")

        // Display a message on alert dialog
        builder.setMessage("Please select a mode. \nEnter a number below for goal mode!")

        // Display an edit text field
        builder.setView(taskEditText)

        // Set a positive button and its click listener on alert dialog
        builder.setPositiveButton("Goal Mode"){dialog, which -> val goal = taskEditText.text.toString()
            // Do something when user press the positive button
            pullUpMode = modes[1]
            if (goal < 1.toString() || goal.isEmpty()) {
                Toast.makeText(applicationContext,"Please enter a valid number!",Toast.LENGTH_SHORT).show()
                shakeFlag = false
                return@setPositiveButton
            }
            tv_mode.text = modes[1] + ", Number of Pull Ups: " + goal
            Toast.makeText(applicationContext,"Counter started in free mode!",Toast.LENGTH_SHORT).show()
            pullUpStarted = true
        }


        // Display a negative button on alert dialog
        builder.setNegativeButton("Free Mode"){dialog,which ->
            pullUpMode = modes[0]
            tv_mode.text = modes[0]
            Toast.makeText(applicationContext,"Please enter the number of pull ups",Toast.LENGTH_SHORT).show()
            pullUpStarted = true
        }


        // Display a neutral button on alert dialog
        builder.setNeutralButton("Cancel"){_,_ ->
            shakeFlag = false
            Toast.makeText(applicationContext,"Maybe next time.",Toast.LENGTH_SHORT).show()
        }

        // Finally, make the alert dialog using builder
        val dialog: AlertDialog = builder.create()

        // Display the alert dialog on app interface
        dialog.show()
    }

    companion object {
        private const val SHAKE_THRESHOLD = 11000
        private val modes = arrayOf("Free", "Goal")
    }

}
