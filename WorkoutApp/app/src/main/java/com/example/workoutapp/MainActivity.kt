package com.example.workoutapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var listOfWorkouts = ArrayList<String>()
        Log.d("tag", "onCreate")

        cb_workout1.setOnClickListener(View.OnClickListener {
            if (cb_workout1.isChecked) {
                Log.d("tag", cb_workout1.text.toString() + " Checked")
                listOfWorkouts.add(cb_workout1.text.toString())
                Log.d("tag", listOfWorkouts.toString())
            } else {
                Log.d("tag", cb_workout1.text.toString() + " UnChecked")
                listOfWorkouts.removeAt(listOfWorkouts.indexOf(cb_workout1.text.toString()))
                Log.d("tag", listOfWorkouts.toString())
            }
        })
        cb_workout2.setOnClickListener(View.OnClickListener {
            if (cb_workout2.isChecked) {
                Log.d("tag", cb_workout2.text.toString() + " Checked")
                listOfWorkouts.add(cb_workout2.text.toString())
                Log.d("tag", listOfWorkouts.toString())
            } else {
                Log.d("tag", cb_workout2.text.toString() + " UnChecked")
                listOfWorkouts.removeAt(listOfWorkouts.indexOf(cb_workout2.text.toString()))
                Log.d("tag", listOfWorkouts.toString())
            }
        })
        cb_workout3.setOnClickListener(View.OnClickListener {
            if (cb_workout3.isChecked) {
                Log.d("tag", cb_workout3.text.toString() + " Checked")
                listOfWorkouts.add(cb_workout3.text.toString())
                Log.d("tag", listOfWorkouts.toString())
            } else {
                Log.d("tag", cb_workout3.text.toString() + " UnChecked")
                listOfWorkouts.removeAt(listOfWorkouts.indexOf(cb_workout3.text.toString()))
                Log.d("tag", listOfWorkouts.toString())
            }
        })
        cb_workout4.setOnClickListener(View.OnClickListener {
            if (cb_workout4.isChecked) {
                Log.d("tag", cb_workout4.text.toString() + " Checked")
                listOfWorkouts.add(cb_workout4.text.toString())
                Log.d("tag", listOfWorkouts.toString())
            } else {
                Log.d("tag", cb_workout4.text.toString() + " UnChecked")
                listOfWorkouts.removeAt(listOfWorkouts.indexOf(cb_workout4.text.toString()))
                Log.d("tag", listOfWorkouts.toString())
            }
        })
        cb_workout5.setOnClickListener(View.OnClickListener {
            if (cb_workout5.isChecked) {
                Log.d("tag", cb_workout5.text.toString() + " Checked")
                listOfWorkouts.add(cb_workout5.text.toString())
                Log.d("tag", listOfWorkouts.toString())
            } else {
                Log.d("tag", cb_workout5.text.toString() + " UnChecked")
                listOfWorkouts.removeAt(listOfWorkouts.indexOf(cb_workout5.text.toString()))
                Log.d("tag", listOfWorkouts.toString())
            }
        })
        cb_workout6.setOnClickListener(View.OnClickListener {
            if (cb_workout6.isChecked) {
                Log.d("tag", cb_workout6.text.toString() + " Checked")
                listOfWorkouts.add(cb_workout6.text.toString())
                Log.d("tag", listOfWorkouts.toString())
            } else {
                Log.d("tag", cb_workout6.text.toString() + " UnChecked")
                listOfWorkouts.removeAt(listOfWorkouts.indexOf(cb_workout6.text.toString()))
                Log.d("tag", listOfWorkouts.toString())
            }
        })
        cb_workout7.setOnClickListener(View.OnClickListener {
            if (cb_workout7.isChecked) {
                Log.d("tag", cb_workout7.text.toString() + " Checked")
                listOfWorkouts.add(cb_workout7.text.toString())
                Log.d("tag", listOfWorkouts.toString())
            } else {
                Log.d("tag", cb_workout7.text.toString() + " UnChecked")
                listOfWorkouts.removeAt(listOfWorkouts.indexOf(cb_workout7.text.toString()))
                Log.d("tag", listOfWorkouts.toString())
            }
        })
        cb_workout8.setOnClickListener(View.OnClickListener {
            if (cb_workout8.isChecked) {
                Log.d("tag", cb_workout8.text.toString() + " Checked")
                listOfWorkouts.add(cb_workout8.text.toString())
                Log.d("tag", listOfWorkouts.toString())
            } else {
                Log.d("tag", cb_workout8.text.toString() + " UnChecked")
                listOfWorkouts.removeAt(listOfWorkouts.indexOf(cb_workout8.text.toString()))
                Log.d("tag", listOfWorkouts.toString())
            }
        })

        b_start_workout.setOnClickListener{
            Log.d("tag", "Button clicked, go to workout activity")
            Log.d("tag", listOfWorkouts.toString())

            val intent = Intent(this, WorkoutActivity::class.java)
            intent.putStringArrayListExtra("workout_list", listOfWorkouts as ArrayList<String?>?)

            startActivity(intent)
        }

    }


    fun message(str: String) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show()
    }


}
