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

    // When a sensor event occurs, both the accelerometer and the magnetometer produce arrays of floating-point values
    // representing points on the x-axis, y-axis, and z-axis of the device's coordinate system.
    // You will combine the data from both these sensors, and over several calls to onSensorChanged(),
    // so you need to retain a copy of this data each time it changes.
    private var mAccelerometerData = FloatArray(3)
    private var mMagnetometerData = FloatArray(3)

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
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    //  use the clone() method to explicitly make a copy of the data in the values array.
                    //  The SensorEvent object (and the array of values it contains) is reused across calls to onSensorChanged().
                    //  Cloning those values prevents the data you're currently interested in from being changed by more recent data before you're done with it.
                    mAccelerometerData = it.values.clone()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    mMagnetometerData = it.values.clone()
                }
            }

            // generate a rotation matrix (explained below) from the raw accelerometer and magnetometer data.
            val rotationMatrix = FloatArray(9)
            val rotationOK = SensorManager.getRotationMatrix(rotationMatrix,
                null, mAccelerometerData, mMagnetometerData)

            // Call the SensorManager.getOrientation() method to get the orientation angles from the rotation matrix.
            // As with getRotationMatrix(), the array of float values containing those angles is supplied to the getOrientation() method and modified in place.
            val orientationValues = FloatArray(3)
            if (rotationOK) {
                SensorManager.getOrientation(rotationMatrix, orientationValues);
            }

            val azimuth = orientationValues[0]
            val pitch = orientationValues[1]
            val roll = orientationValues[2]

            mTextSensorAzimuth?.text = resources.getString(R.string.value_format, azimuth)
            mTextSensorPitch?.text = resources.getString(R.string.value_format, pitch)
            mTextSensorRoll?.text = resources.getString(R.string.value_format, roll)
        }
    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    override fun onAccuracyChanged(sensor: Sensor?, status: Int) {
    }

}