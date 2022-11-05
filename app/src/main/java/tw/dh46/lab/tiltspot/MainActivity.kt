package tw.dh46.lab.tiltspot

import android.content.Context
import android.content.pm.ActivityInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

import android.hardware.SensorManager




class MainActivity : AppCompatActivity(), SensorEventListener {

    // System sensor manager instance.
    private var mSensorManager: SensorManager? = null

    // Accelerometer and magnetometer sensors, as retrieved from the
    // sensor manager.
    private var mSensorAccelerometer: Sensor? = null
    private var mSensorMagnetometer: Sensor? = null

    // TextViews to display current sensor values.
    private var mTextSensorAzimuth: TextView? = null
    private var mTextSensorPitch: TextView? = null
    private var mTextSensorRoll: TextView? = null

    // Very small values for the accelerometer (on all three axes) should
    // be interpreted as 0. This value is the amount of acceptable
    // non-zero drift.
    private val VALUE_DRIFT = 0.05f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Lock the orientation to portrait (for now)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        mTextSensorAzimuth = findViewById(R.id.value_azimuth);
        mTextSensorPitch = findViewById(R.id.value_pitch);
        mTextSensorRoll = findViewById(R.id.value_roll);

        // Get accelerometer and magnetometer sensors from the sensor manager.
        // The getDefaultSensor() method returns null if the sensor
        // is not available on the device.
        mSensorManager = getSystemService(
                Context.SENSOR_SERVICE) as SensorManager
        mSensorAccelerometer = mSensorManager?.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER)
        mSensorMagnetometer = mSensorManager?.getDefaultSensor(
            Sensor.TYPE_MAGNETIC_FIELD)
    }

    /**
     * Listeners for the sensors are registered in this callback so that
     * they can be unregistered in onStop().
     */
    override fun onStart() {
        super.onStart()

        // Listeners for the sensors are registered in this callback and
        // can be unregistered in onStop().
        //
        // Check to ensure sensors are available before registering listeners.
        // Both listeners are registered with a "normal" amount of delay
        // (SENSOR_DELAY_NORMAL).
        if (mSensorAccelerometer != null) {
            mSensorManager?.registerListener(this, mSensorAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorMagnetometer != null) {
            mSensorManager?.registerListener(this, mSensorMagnetometer,
                SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onStop() {
        super.onStop()

        // Unregister all sensor listeners in this callback so they don't
        // continue to use resources when the app is stopped.
        mSensorManager?.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent?) {
    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    override fun onAccuracyChanged(sensor: Sensor?, status: Int) {
    }

}