package edu.uw.eep523.androidblecontrol


import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.ActivityNotFoundException
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.text.InputType
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

const val DEVICE_NAME = "circuit_playground"
const val EMERGENCY_CONTACT = "5099428579"

class MainActivity : AppCompatActivity(), BLEControl.Callback {


    // Bluetooth
    private var ble: BLEControl? = null
    private var messages: TextView? = null
    private var rssiAverage:Double = 0.0

    // Alarm
    var alarmDuration: Long = 10*1000 //in milliseconds
    var seconds: Long = 0




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        val adapter: BluetoothAdapter?
        adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            if (!adapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)

            }        }

        // Get Bluetooth
        messages = findViewById(R.id.bluetoothText)
        messages!!.movementMethod = ScrollingMovementMethod()
        ble = BLEControl(applicationContext, DEVICE_NAME)

        // Check permissions
        ActivityCompat.requestPermissions(this,
                arrayOf( Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1)



    }

    //Function that reads the RSSI value associated with the bluetooth connection between the phone and the Arudino board
    //If you use the RSSI to calculate distance, you may want to record a set of values over a period of time
    //and obtain the average
    override fun onRSSIread(uart:BLEControl,rssi:Int){
        rssiAverage = rssi.toDouble()
        writeLine("RSSI $rssiAverage")
    }
    fun getRSSI (v:View){
        ble!!.getRSSI()
    }

    fun clearText (v:View){
        messages!!.text=""
        //showDialog()
        Log.d("TAG", "text cleared")
    }

    override fun onResume() {
        super.onResume()
        //updateButtons(false)
        ble!!.registerCallback(this)
    }

    override fun onStop() {
        super.onStop()
        ble!!.unregisterCallback(this)
        ble!!.disconnect()
    }

    fun connect(v: View) {
        startScan()
    }

    private fun startScan() {
        writeLine("Scanning for devices ...")
        ble!!.connectFirstAvailable()
    }


    /**
     * Press button to receive the temperature value form the board
     */
    fun buttTouch(v: View) {
        ble!!.send("readtemp")
        Log.i("BLE", "READ TEMP")
    }

    /**
     * Press button to set the lEDs to color red (see arduino code)
     */
    fun buttRed(v: View) {
        ble!!.send("red")
        Log.i("BLE", "SEND RED")
    }


    /**
     * Writes a line to the messages textbox
     * @param text: the text that you want to write
     */
    private fun writeLine(text: CharSequence) {
        runOnUiThread {
            messages!!.append(text)
            messages!!.append("\n")
        }
    }

    /**
     * Called when a UART device is discovered (after calling startScan)
     * @param device: the BLE device
     */
    override fun onDeviceFound(device: BluetoothDevice) {
        writeLine("Found device : " + device.name)
        writeLine("Waiting for a connection ...")
    }

    /**
     * Prints the devices information
     */
    override fun onDeviceInfoAvailable() {
        writeLine(ble!!.deviceInfo)
    }

    /**
     * Called when UART device is connected and ready to send/receive data
     * @param ble: the BLE UART object
     */
    override fun onConnected(ble: BLEControl) {
        writeLine("Connected!")

    }

    /**
     * Called when some error occurred which prevented UART connection from completing
     * @param ble: the BLE UART object
     */
    override fun onConnectFailed(ble: BLEControl) {
        writeLine("Error connecting to device!")
    }

    /**
     * Called when the UART device disconnected
     * @param ble: the BLE UART object
     */
    override fun onDisconnected(ble: BLEControl) {
        writeLine("Disconnected!")
    }

    /**
     * Called when data is received by the UART
     * @param ble: the BLE UART object
     * @param rx: the received characteristic
     */
    override fun onReceive(ble: BLEControl, rx: BluetoothGattCharacteristic) {
        writeLine("Received value: " + rx.getStringValue(0))

        //TODO: Check for received notification, if so call showDialog()

    }
    /**
     * set up dialog box
     * @param
     */
    private fun showDialog() {
        runOnUiThread {
            Log.d("TAG", "entered showDialog()")
//        val taskEditText = EditText(this)
//        taskEditText.setInputType(InputType.TYPE_CLASS_NUMBER);
//        taskEditText.hint = "hint for edit text"
            var alarmCancelled = false

            // Initialize a new instance of
            val builder = AlertDialog.Builder(this@MainActivity)

            // Set the alert dialog title
            builder.setTitle("Arduino Alarmed!")

            // Display a message on alert dialog
            builder.setMessage("Do you want to cancel the alarm?")

            // Display an edit text field
//        builder.setView(taskEditText)

            // Set a positive button and its click listener on alert dialog
//            builder.setPositiveButton("Yes") { dialog, which -> //goalPullUps = taskEditText.text.toString()
//                // Do something when user press the positive button
//
//            }


            // Display a negative button on alert dialog
            builder.setNegativeButton("Cancel Alarm") { dialog, which ->
                alarmCancelled = true
                // Do something when user press the cancel button
                // TODO: Light neopixels green
                // TODO: go back to monitoring arduino sensors
            }


            // Display a neutral button on alert dialog
//        builder.setNeutralButton("Cancel"){_,_ ->
//            //Do something when user pressed neutral button
//        }

            // Finally, make the alert dialog using builder
            val dialog: AlertDialog = builder.create()

            // Displaying the alert dialog on app interface
            dialog.show()
            //dialog.dismiss()

            Log.d("tag", "starting timer for alarm")

            object : CountDownTimer(alarmDuration, 1000) {
                // perform this every 'tick' (countDownInterval)
                override fun onTick(millisUntilFinished: Long) {
                    if (alarmCancelled)
                        cancel()
                    seconds = millisUntilFinished / 1000
                    Log.d("tag", "seconds remaining: " + seconds)
                    dialog.setMessage("Do you want to cancel the alarm?\n$seconds seconds remaining.")
                }

                override fun onFinish() {
                    // remove dialog
                    dialog.dismiss()

                    //timer is done
                    //TODO: blink neopixel red and play noise

                    //make phone call
                    makePhoneCall()
                }
            }.start()
        }
    }

    fun makePhoneCall() {
        Log.d("TAG", "entered makePhoneCall")
        try {
            val uri: String = "tel:" + EMERGENCY_CONTACT
            val intent = Intent(Intent.ACTION_CALL, Uri.parse(uri))
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {

                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CALL_PHONE)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.CALL_PHONE),
                        42)
                }

                // here to request the missing permissions, and then overriding
                // public void onRequestPermissionsResult(int requestCode, String[] permissions,
                // int[] grantResults)
                // to handle the case where the user grants the permission.
                return
            }
            startActivity(intent)
            Log.d("TAG", "making phone call")
        } catch (e: ActivityNotFoundException) {
            Log.d("TAG", "error in making call: $e")
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == 42) {
            // If request is cancelled, the result arrays are empty.
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // permission was granted, yay!
                makePhoneCall()
            } else {
                // permission denied, boo! Disable the
                // functionality
            }
            return
        }
    }



    companion object {
        private val REQUEST_ENABLE_BT = 0
    }
}
