package com.example.tipcalculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

private const val TAG = "MainActivity"

class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // get reference to button
        val b_tip_plus = findViewById(R.id.b_tip_plus) as Button
        val b_tip_minus = findViewById(R.id.b_tip_minus) as Button

        var tipPercent = et_tip_percent_val.text.toString().toDouble()

        // listen for bill amount input - when user inputs bill amount
        et_bill_amount_input.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                Log.i(TAG, "afterTextChanged $s")
                computeTip()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }
        })

        // listen for tip percent input - when user inputs tip percent
        et_tip_percent_val.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.i(TAG, "afterTextChanged $s")
                computeTip()
            }
        })

        et_tip_percent_val.setOnKeyListener(View.OnKeyListener { v, keyCode, event ->
            if ( (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) or (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) ) {
                if (!et_tip_percent_val.text.isNullOrEmpty()){
                    tipPercent = et_tip_percent_val.text.toString().toDouble()
                    et_tip_percent_val.setText("%.2f".format(tipPercent))

                }
                return@OnKeyListener true
            }
            false
        })


        // set on-click listener - when user clicks button
        b_tip_plus.setOnClickListener {
            // your code to perform when the user clicks on the button
            tipPercent++
            et_tip_percent_val.setText("%.2f".format(tipPercent))
            computeTip()

        }

        // set on-click listener - when user clicks button
        b_tip_minus.setOnClickListener {
            // your code to perform when the user clicks on the button
            if(tipPercent > 0){
                if (tipPercent < 1){
                    tipPercent = 0.0
                } else {
                    tipPercent --
                }
                et_tip_percent_val.setText("%.2f".format(tipPercent))
                computeTip()
            }
        }

        // set on-click listener - when user clicks button
        b_10_percent.setOnClickListener {
            // your code to perform when the user clicks on the button
            tipPercent = 10.0
            et_tip_percent_val.setText("%.2f".format(tipPercent))
            computeTip()
        }

        // set on-click listener - when user clicks button
        b_15_percent.setOnClickListener {
            // your code to perform when the user clicks on the button
            tipPercent = 15.0
            et_tip_percent_val.setText("%.2f".format(tipPercent))
            computeTip()
        }

        // set on-click listener - when user clicks button
        b_20_percent.setOnClickListener {
            // your code to perform when the user clicks on the button
            tipPercent = 20.0
            et_tip_percent_val.setText("%.2f".format(tipPercent))
            computeTip()
        }

        // set on-click listener - when user clicks button
        b_30_percent.setOnClickListener {
            // your code to perform when the user clicks on the button
            tipPercent = 30.0
            et_tip_percent_val.setText("%.2f".format(tipPercent))
            computeTip()
        }
    }

    private fun computeTip() {
        if (et_bill_amount_input.text.isEmpty() or et_tip_percent_val.text.isEmpty()){
            Log.i(TAG, "bill or tip empty")
            tv_tip_amount_output.text = "Enter Bill/Tip Amount"
            tv_total_amount_output.text = "Enter Bill/Tip Amount"
            return
        }

        val billAmount = et_bill_amount_input.text.toString().toDouble()
        val tipPercent = et_tip_percent_val.text.toString().toDouble()
        val tipAmount = billAmount * tipPercent / 100
        val totalAmount = billAmount + tipAmount

        tv_tip_amount_output.text = "%.2f".format(tipAmount)
        tv_total_amount_output.text = "%.2f".format(totalAmount)

    }
}

