package com.example.workoutapp

import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_workout.*


class WorkoutActivity : AppCompatActivity() {

    var seconds: Long = 0
    val workoutDuration: Long = 30000

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_workout)
        Log.d("tag", "Workout Activity started.")

        val bundle: Bundle? = intent.extras
        //val workoutList = bundle!!.getString("workout_list")
        val workoutList = intent.getStringArrayListExtra("workout_list")

        var timerRunning = true
        var countStarted = false
        val numWorkouts = workoutList.count()
        var i = 0

        tv_workout_text.text = workoutList[i]
        show__workout_image(workoutList[i])
        Log.d("tag", "number of workouts" + numWorkouts.toString())



        object : CountDownTimer(workoutDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                seconds = millisUntilFinished / 1000
                Log.d("tag", "seconds remaining: " + seconds)
                view_timer.text = seconds.toString()
            }

            override fun onFinish() {
                i++
                Log.d("tag", "done!")
                if (i < numWorkouts) {
                    startTimer(workoutList[i].toString())
                } else {
                    view_timer.text = "DONE!"
                }
            }
        }.start()

        }


    fun startTimer(workout: String) {
        tv_workout_text.text = workout
        show__workout_image(workout)
        object : CountDownTimer(workoutDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                seconds = millisUntilFinished / 1000
                Log.d("tag", "seconds remaining: " + seconds)
                view_timer.text = seconds.toString()
            }

            override fun onFinish() {
                Log.d("tag", "done!")
                view_timer.text = "DONE!"
            }
        }.start()
    }

    fun show__workout_image(workout: String) {
        iv_lunges.visibility = View.INVISIBLE
        iv_pushups.visibility = View.INVISIBLE
        iv_squats.visibility = View.INVISIBLE
        iv_burpees.visibility = View.INVISIBLE
        iv_planks.visibility = View.INVISIBLE
        iv_crunches.visibility = View.INVISIBLE
        iv_situp.visibility = View.INVISIBLE
        iv_jumpingjacks.visibility = View.INVISIBLE

        when (workout) {
            "Lunges" -> iv_lunges.visibility = View.VISIBLE
            "Push Ups" -> iv_pushups.visibility = View.VISIBLE
            "Squats" -> iv_squats.visibility = View.VISIBLE
            "Burpees" -> iv_burpees.visibility = View.VISIBLE
            "Planks" -> iv_planks.visibility = View.VISIBLE
            "Crunches" -> iv_crunches.visibility = View.VISIBLE
            "Sit Ups" -> iv_situp.visibility = View.VISIBLE
            "Jumping Jacks" -> iv_jumpingjacks.visibility = View.VISIBLE
        }
    }



    fun message(str: String) {
        Toast.makeText(this, str, Toast.LENGTH_LONG).show()
    }


}
