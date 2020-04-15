package com.example.workoutapp

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_workout.*


class WorkoutActivity : AppCompatActivity() {

    var seconds: Long = 0
    var workoutDuration: Long = 10*1000 //in milliseconds
    var numWorkouts = 0
    var workoutList = ArrayList<String>()
    var index = 0

    @RequiresApi(Build.VERSION_CODES.N)
//    override fun onConfigurationChanged(newConfig: Configuration) {
//        super.onConfigurationChanged(newConfig)
//        setContentView(R.layout.activity_workout)
//        Log.d("tag","config changed - activity workout")
//    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //keep screen from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_workout)
        Log.d("tag","on create - activity workout")
        Log.d("tag", "Workout Activity started.")

        //extract intents - data from first (main) activity
        val bundle: Bundle? = intent.extras
        workoutList = intent.getStringArrayListExtra("workout_list")
        workoutDuration = intent.getIntExtra("seconds", 10).toLong() * 1000

        //init variables
        var timerRunning = true
        var countStarted = false
        numWorkouts = workoutList.count()
        tv_workout_text.text = workoutList[index]
        show__workout_image(workoutList[index])
        Log.d("tag", "number of workouts" + numWorkouts.toString())


        //init timer for each workout, one after the other
        Log.d("tag", "starting timer for $workoutList[i]")
        object : CountDownTimer(workoutDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                seconds = millisUntilFinished / 1000
                Log.d("tag", "seconds remaining: " + seconds)
                view_timer.text = seconds.toString()
            }

            override fun onFinish() {
                index++
                Log.d("tag", "done!")
                if (index < numWorkouts) {
                    Log.d("tag", "workout " + index.toString() +"out of " + numWorkouts.toString())
                    //start next timer when done
                    startTimer(workoutList[index].toString())
                } else {
                    Log.d("tag", "done!")
                    view_timer.text = "DONE!"
                }
            }
        }.start()

        }


    fun startTimer(workout: String) {
        Log.d("tag", "starting timer for $workout")
        tv_workout_text.text = workout
        show__workout_image(workout)
        object : CountDownTimer(workoutDuration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                seconds = millisUntilFinished / 1000
                Log.d("tag", "seconds remaining: " + seconds)
                view_timer.text = seconds.toString()
            }

            override fun onFinish() {
                index++
                Log.d("tag", "done!")
                if (index < numWorkouts) {
                    Log.d("tag", "workout " + index.toString() +"out of " + numWorkouts.toString())
                    startTimer(workoutList[index])
                } else {
                    Log.d("tag", "done!")
                    view_timer.text = "DONE!"
                }
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