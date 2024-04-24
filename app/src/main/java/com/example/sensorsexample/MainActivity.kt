package com.example.sensorsexample

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity(), SensorEventListener {

    private val TAG = "MainActivity"

    private var myMediaPlayer : MediaPlayer? = null

    // this is variable to store a threshold value (used for shake detection)
    private var accelerationThreshold = 0.0

    // You can play around with this number to see which value is a good threshold for the demo app
    private val MINIMUM_SENSITIVITY = 9.9


    private lateinit var sensorManager: SensorManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Get the SensorManager service to access device sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // I put the sensors that I would like to use/register in a list
        // so that I can enable them (in a less repetitive manner)
        val sensors = listOf(
            // Accelerometer Sensor
            Sensor.TYPE_ACCELEROMETER,
            // Gyroscope Sensor
            Sensor.TYPE_GYROSCOPE,
            // MagneticField Sensor
            Sensor.TYPE_MAGNETIC_FIELD,
            // Proximity Sensor
            Sensor.TYPE_PROXIMITY,
            // Light Sensor
            Sensor.TYPE_LIGHT,
            // Gravity Sensor
            Sensor.TYPE_GRAVITY
        )
        // Iterate the list above to enable each sensor through the helper function
        for (sensor in sensors) {
            registerSensor(sensorManager.getDefaultSensor(sensor))
        }

        initializeSeekBar()
    }
    // A helper function to register sensor with a delay, this can be done onResume instead of onCreate
    private fun registerSensor(sensor: Sensor?){
        if (sensor != null) {
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    private fun initializeSeekBar() {

        val seekBar = findViewById<SeekBar>(R.id.seekBar)
        accelerationThreshold = seekBar.max + MINIMUM_SENSITIVITY
        Log.d(TAG, "currentThreshold:$accelerationThreshold ")

        // Set a SeekBar change listener
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, value: Int, b: Boolean) {
                // Display the current progress of SeekBar
                findViewById<TextView>(R.id.tv_threshold).text = "Value: $value"
                accelerationThreshold = value * 0.5 + MINIMUM_SENSITIVITY
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // Nothing to do
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                // Nothing to do
            }
        })
    }


    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Do something here if sensor accuracy changes.
    }

    override fun onSensorChanged(event: SensorEvent?) {

        // Many sensors return 3 values, one for each axis.

        val sensor = event?.sensor ?: return
        when (sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {

                // Axis of the Accelerometer sample, not normalized yet.
                val x: Float = event.values[0]
                val y: Float = event.values[1]
                val z: Float = event.values[2]

                Log.d(TAG, "Accelerometer: ${event.values.joinToString(" ")}")

                findViewById<TextView>(R.id.tv_accelerometer).text = "X: $x" + ", Y: $y" + ", Z: $z"

                // To detect shake movement
                shakeDetection(x, y, z)

                // To measure the real acceleration of the device, the contribution of the force of
                // gravity must be removed from the accelerometer data.
                // This can be achieved by applying a high-pass filter.
                // Conversely, a low-pass filter can be used to isolate the force of gravity.

                //val alpha: Float = 0.8f

                // Isolate the force of gravity with the low-pass filter.
                //gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0]
                //gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1]
                //gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2]

                // Remove the gravity contribution with the high-pass filter.
                //linear_acceleration[0] = event.values[0] - gravity[0]
                //linear_acceleration[1] = event.values[1] - gravity[1]
                //linear_acceleration[2] = event.values[2] - gravity[2]
            }
            Sensor.TYPE_GYROSCOPE -> {


                // Axis of the rotation sample, not normalized yet.
                val x: Float = event.values[0]
                val y: Float = event.values[1]
                val z: Float = event.values[2]

                Log.d(TAG, event.values.joinToString(" "))

                findViewById<TextView>(R.id.tv_gyroscope).text = "X: $x" + ", Y: $y" + ", Z: $z"

            }

            Sensor.TYPE_MAGNETIC_FIELD -> {

                // Axis of the magnetic filed, not normalized yet.
                val x: Float = event.values[0]
                val y: Float = event.values[1]
                val z: Float = event.values[2]

                Log.d(TAG, event.values.joinToString(" "))

                findViewById<TextView>(R.id.tv_magnetic_field).text = "X: $x" + ", Y: $y" + ", Z: $z"
            }
            Sensor.TYPE_PROXIMITY -> {
                //lets you determine how far away an object is from a device

                Log.d(TAG, event.values.joinToString(" "))

                findViewById<TextView>(R.id.tv_proximity).text = "${event.values[0]}"

            }
            Sensor.TYPE_LIGHT -> {

                // The light sensor returns a single value.
                val lux = event.values[0]
                // Do something with this sensor value.
                Log.d(TAG, "lux: $lux")
                findViewById<TextView>(R.id.tv_light).text = "$lux"
            }

        }
    }

    private fun shakeDetection(x: Float, y: Float, z: Float) {

        // This line calculates the magnitude of acceleration vector using the Euclidean distance
        // formula, which is the square root of the sum of squares of acceleration values along each axis (x, y, and z).
        // This gives us the overall acceleration magnitude.
        val overallAcceleration = Math.sqrt( (x * x + y * y + z * z).toDouble() )
        Log.d(TAG, "Overall Acceleration: $overallAcceleration ")

        // checks if the calculated acceleration magnitude is greater than a predefined threshold value
        // If the condition is true, it implies that the device is experiencing a shake event. Play sound.
        if (overallAcceleration > accelerationThreshold) {
            Log.d(TAG, "x: $x, y: $y, z: $z")
            Log.d(TAG, "Overall Acceleration: $overallAcceleration, currentSensitivity: $accelerationThreshold")
            playSound()
        }
    }




    override fun onResume() {
        super.onResume()
        /* mLight?.also { light ->
             sensorManager.registerListener(this, light, SensorManager.SENSOR_DELAY_NORMAL)
         }*/

    }


    override fun onPause() {
        super.onPause()
        // Make sure to unregister the sensor listener when you are done using the sensor
        // unless you unregister the sensor the sensor will continue to acquire data and use battery resources
        sensorManager.unregisterListener(this)

        // Stop the song as well
        stopSong()
    }

    /*
       A button for Starting sound.
       If it is the first time, it creates the MediaPlayer object with the song provided
   */
    private fun playSound() {
        if (myMediaPlayer == null) {
            myMediaPlayer = MediaPlayer.create(this, R.raw.whip_crack)
        }
        myMediaPlayer?.start()
    }



    /*
        Helper function to handle stopping the sound
    */
    private fun stopSong() {
        if(myMediaPlayer != null) {
            //myMediaPlayer?.stop()
            // Release is probably better option to release the system resources used
            myMediaPlayer?.release()
            myMediaPlayer = null
        }
    }


}
