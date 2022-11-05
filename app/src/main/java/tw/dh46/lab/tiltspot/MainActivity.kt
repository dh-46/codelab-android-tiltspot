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
import android.os.Build
import android.view.Display
import android.view.Surface
import android.widget.ImageView
import kotlin.math.abs
import android.view.WindowManager


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


    private var mSpotTop: ImageView? = null
    private var mSpotBottom: ImageView? = null
    private var mSpotLeft: ImageView? = null
    private var mSpotRight: ImageView? = null

    private var mDisplay: Display? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Lock the orientation to portrait (for now)
        // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        mDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            @Suppress("DEPRECATION")
            wm.defaultDisplay
        }

        initViews()

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

            // remap coordination
            var rotationMatrixAdjusted = FloatArray(9)
            when (mDisplay?.rotation) {
                Surface.ROTATION_0 -> {
                    rotationMatrixAdjusted = rotationMatrix.clone()
                }
                Surface.ROTATION_90 -> {
                    // This method takes as arguments the original rotation matrix,
                    // the two new axes on which you want to remap the existing x-axis and y-axis,
                    // and an array to populate with the new data.
                    // Use the axis constants from the SensorManager class to represent the coordinate system axes.
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X,
                        rotationMatrixAdjusted)
                }
                Surface.ROTATION_180 -> {
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,
                        rotationMatrixAdjusted)
                }
                Surface.ROTATION_270 -> {
                    SensorManager.remapCoordinateSystem(rotationMatrix,
                        SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X,
                        rotationMatrixAdjusted)
                }
            }

            // Call the SensorManager.getOrientation() method to get the orientation angles from the rotation matrix.
            // As with getRotationMatrix(), the array of float values containing those angles is supplied to the getOrientation() method and modified in place.
            val orientationValues = FloatArray(3)
            if (rotationOK) {
                SensorManager.getOrientation(rotationMatrixAdjusted, orientationValues);
            }

            val azimuth = orientationValues[0]
            val pitch = orientationValues[1]
            val roll = orientationValues[2]

            updateViews(pitch, roll, azimuth)
        }
    }

    private fun updateViews(_pitch: Float, _roll: Float, _azimuth: Float) {
        //  reset the pitch or roll values that are close to 0 (less than the value of the VALUE_DRIFT constant) to be 0:
        val pitch = if (abs(_pitch) < VALUE_DRIFT) {
            0f
        } else {
            _pitch
        }

        val roll = if (abs(_roll) < VALUE_DRIFT) {
            0f
        } else {
            _roll
        }

        mTextSensorAzimuth?.text = resources.getString(R.string.value_format, _azimuth)
        mTextSensorPitch?.text = resources.getString(R.string.value_format, pitch)
        mTextSensorRoll?.text = resources.getString(R.string.value_format, roll)

        // reset ui state
        mSpotTop?.alpha = 0f;
        mSpotBottom?.alpha = 0f;
        mSpotLeft?.alpha = 0f;
        mSpotRight?.alpha = 0f;

        // Update the alpha value for the appropriate spot with the values for pitch and roll.
        // Note that the pitch and roll values you calculated in the previous task are in radians,
        // and their values range from -π to +π. Alpha values, on the other hand, range only from 0.0 to 1.0.
        // You could do the math to convert radian units to alpha values,
        // but you may have noted earlier that the higher pitch and roll values only occur
        // when the device is tilted vertical or even upside down.
        // For the TiltSpot app you're only interested in displaying dots in response to some device tilt,
        // not the full range. This means that you can conveniently use the radian units directly as input to the alpha.
        if (pitch > 0) {
            mSpotBottom?.alpha = pitch
        } else {
            mSpotTop?.alpha = abs(pitch)
        }
        if (roll > 0) {
            mSpotLeft?.alpha = roll
        } else {
            mSpotRight?.alpha = abs(roll)
        }
    }

    /**
     * Must be implemented to satisfy the SensorEventListener interface;
     * unused in this app.
     */
    override fun onAccuracyChanged(sensor: Sensor?, status: Int) {
    }


    private fun initViews() {
        mTextSensorAzimuth = findViewById(R.id.value_azimuth)
        mTextSensorPitch = findViewById(R.id.value_pitch)
        mTextSensorRoll = findViewById(R.id.value_roll)

        mSpotTop = findViewById(R.id.spot_top)
        mSpotBottom = findViewById(R.id.spot_bottom)
        mSpotRight = findViewById(R.id.spot_right)
        mSpotLeft = findViewById(R.id.spot_left)
    }
}