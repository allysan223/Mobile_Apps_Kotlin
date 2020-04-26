package edu.uw.eep523.pullups

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.jjoe64.graphview.DefaultLabelFormatter
import com.jjoe64.graphview.GraphView
import com.jjoe64.graphview.series.DataPoint
import com.jjoe64.graphview.series.LineGraphSeries
import kotlinx.android.synthetic.main.activity_main.*
import java.text.NumberFormat
import kotlin.math.pow
import kotlin.math.sqrt

private const val SHAKE_THRESHOLD = 10000

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var mSeriesXaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesYaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesZaccel: LineGraphSeries<DataPoint>
    private lateinit var mSeriesMagAccel: LineGraphSeries<DataPoint>

    private lateinit var mSensorManager: SensorManager
    private lateinit var mSensor: Sensor
    private lateinit var mSensorG: Sensor

    val accel: Array<Float> = arrayOf(0.0f,0.0f,0.0f,0.0f)
    val last_accel: Array<Float> = arrayOf(0.0f,0.0f,0.0f,0.0f)

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

        initGraphRT(mGraphX,mSeriesXaccel!!)
        initGraphRT(mGraphY,mSeriesYaccel!!)
        initGraphRT(mGraphZ,mSeriesZaccel!!)
        initGraphRT(mGraphMag,mSeriesMagAccel!!)
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

        accel[0] = event.values[0] //X
        accel[1] = event.values[1] //Y
        accel[2] = event.values[2] //Z
        accel[3] = magnitude(accel[0], accel[1], accel[2])


        val xval = System.currentTimeMillis()/1000.toDouble()//graphLastXValue += 0.1
        mSeriesXaccel!!.appendData(DataPoint(xval, accel[0].toDouble()), true, 50)
        mSeriesYaccel!!.appendData(DataPoint(xval, accel[1].toDouble()), true, 50)
        mSeriesZaccel!!.appendData(DataPoint(xval, accel[2].toDouble()), true, 50)
        mSeriesMagAccel!!.appendData(DataPoint(xval, accel[3].toDouble()), true, 50)

        val speed: Float =
            Math.abs(accel[0] + accel[1] + accel[2] - last_accel[0] - last_accel[1] - last_accel[2]) / 4 * 10000

        if (speed > SHAKE_THRESHOLD) {
            Log.d("sensor", "shake detected w/ speed: $speed")
            Toast.makeText(this, "shake detected w/ speed: $speed", Toast.LENGTH_SHORT).show()
        }
        last_accel[0] = accel[0]
        last_accel[1] = accel[1]
        last_accel[2] = accel[2]

    }

    private fun magnitude(x: Float, y: Float, z: Float): Float {
        var mag = x.pow(2) + y.pow(2) + z.pow(2)
        mag = sqrt(mag)
        return mag
    }


    private fun initGraphRT(mGraph: GraphView, mSeriesXaccel :LineGraphSeries<DataPoint>){

        mGraph.getViewport().setXAxisBoundsManual(true)
        //mGraph.getViewport().setMinX(0.0)
        //mGraph.getViewport().setMaxX(4.0)
        mGraph.getViewport().setYAxisBoundsManual(true);


        mGraph.getViewport().setMinY(0.0);
        mGraph.getViewport().setMaxY(10.0);
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


}
