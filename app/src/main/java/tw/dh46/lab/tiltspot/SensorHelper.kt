package tw.dh46.lab.tiltspot

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 *  Created by Daniel on 2022/11/6
 */
class SensorHelper(
    context: Context,
    private val onSensorUpdated: (azimuth: Float, pitch: Float, roll: Float) -> Unit,
    private val enableOrientation: Boolean = true,
    private var enableSmoothAzimuth: Boolean = true
) : SensorEventListener {

    private val mSensorManager: SensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    private var mSensorAccelerometer: Sensor? = null
    private var mSensorMagnetometer: Sensor? = null

    private var mAccelerometerData = FloatArray(3)
    private var mMagnetometerData = FloatArray(3)

    private val orientationEventListener: OrientationEventListener by lazy {
        initOrientationEventListener(context)
    }

    // 預設為零
    var currentOrientation = Surface.ROTATION_0

    // 過濾方位角
    private var mLastAzimuth: Float = 0.0F      // 前一個方位角
    private var mAzimuthUpdateTime: Long = 0L   // 方位角更新時間
    // 方位角更新限制
    private var mAzimuthUpdateRate: Int = AZIMUTH_UPDATE_RATE    // 最小間隔時間
    private var mAzimuthMinUpdate: Float = AZIMUTH_MIN_UPDATE     // 最小相差角度

    init {
        mSensorAccelerometer = mSensorManager.getDefaultSensor(
            Sensor.TYPE_ACCELEROMETER)
        mSensorMagnetometer = mSensorManager.getDefaultSensor(
            Sensor.TYPE_MAGNETIC_FIELD)
    }

    fun start() {
        if (enableOrientation && orientationEventListener.canDetectOrientation()) {
            orientationEventListener.enable()
        }

        if (mSensorAccelerometer != null) {
            mSensorManager.registerListener(this, mSensorAccelerometer,
                SensorManager.SENSOR_DELAY_NORMAL)
        }
        if (mSensorMagnetometer != null) {
            mSensorManager.registerListener(this, mSensorMagnetometer,
                SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    fun stop() {
        mSensorManager.unregisterListener(this)

        if (enableOrientation) {
            orientationEventListener.disable()
        }
    }

    // --------------------------------------------------------------------

    private fun initOrientationEventListener(context: Context): OrientationEventListener {
        return object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) {
                    //手機平放時，檢測不到有效的角度
                    Log.d(TAG, "onOrientationChanged: 裝置平放無法偵測")
                    return
                } else {
                    currentOrientation = if (orientation > 350 || orientation < 10) { // 0度
                        Surface.ROTATION_0
                    } else if (orientation in 81..99) { //90度
                        Surface.ROTATION_90
                    } else if (orientation in 171..189) { //180度
                        Surface.ROTATION_180
                    } else if (orientation in 261..279) { //270度
                        Surface.ROTATION_270
                    } else {
                        return
                    }
                }
            }
        }
    }

    /**
     * Convert sensor radians to degrees
     */
    private fun toDegreeValues(originalRadians: FloatArray): FloatArray {
        val azimuth = originalRadians[0]
        val pitch = originalRadians[1]
        val roll = originalRadians[2]
        val result = FloatArray(3)
        result[0] = ((Math.toDegrees(azimuth.toDouble()) + 360) % 360).toFloat()
        result[1] = Math.toDegrees(pitch.toDouble()).toFloat()
        result[1] = Math.toDegrees(roll.toDouble()).toFloat()
        return result
    }

    // --------------------------------------------------------------------

    /*-------------------- 過濾方位角(防止抖動) --------------------*/

    /**
     * 計算低通過濾後的方位角
     * - 參考: https://christine-coenen.de/blog/2014/07/02/smooth-compass-needle-in-android-or-any-angle-with-low-pass-filter/
     * @param azimuth
     */
    private fun calcAzimuthLowPassFilter(azimuth: Float): Float {
        // 平滑因子
        // 移動角度大於設定角度: 平滑 0.6
        // 移動角度小於設定角度: 平滑 0.8
        val smoothingFactor = if (abs(mLastAzimuth - azimuth) > mAzimuthMinUpdate) {
            0.6F
        } else {
            0.8F
        }

        val lastAngle = Math.toRadians(mLastAzimuth.toDouble())
        val newAngle = Math.toRadians(azimuth.toDouble())

        val lastSin = (smoothingFactor * sin(lastAngle) + (1 - smoothingFactor) * sin(newAngle))
        val lastCos = (smoothingFactor * cos(lastAngle) + (1 - smoothingFactor) * cos(newAngle))

        val angle = Math.toDegrees(getAngle(lastSin, lastCos))
        return if (angle < 0) {
            angle + 360
        } else {
            angle
        }.toFloat()
    }

    /**
     * 取得角度
     * @param lastSin
     * @param lastCos
     * @return
     */
    private fun getAngle(lastSin: Double, lastCos: Double): Double {
        return atan2(lastSin, lastCos)
    }

    /**
     * 確認方位角是否要更新
     * - 方位角一致就不更新
     * - 新方位角跟舊方位角跟最小更新值比對
     * - 大於: 直接更新
     * - 小於: 判斷間隔時間
     * @param newAzimuth
     * @return
     */
    private fun checkAzimuthUpdate(newAzimuth: Float): Boolean {
        if (mLastAzimuth == newAzimuth) {
            return false
        }
        return if (abs(mLastAzimuth - newAzimuth) > mAzimuthMinUpdate) {
            true
        } else {
            checkAzimuthUpdateTime()
        }
    }

    /**
     * 判斷更新時間是否大於更新畫面時間
     * @return
     */
    private fun checkAzimuthUpdateTime(): Boolean {
        return (System.currentTimeMillis() - mAzimuthUpdateTime) > mAzimuthUpdateRate
    }

    // --------------------------------------------------------------------

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
            when (currentOrientation) {
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

            // Convert to degrees
            val degreeValues = toDegreeValues(orientationValues)
            val azimuth = if (enableSmoothAzimuth) {
                val smoothed = calcAzimuthLowPassFilter(degreeValues[0])
                if (checkAzimuthUpdate(smoothed)) {
                    mLastAzimuth = smoothed
                }
                mLastAzimuth
            } else {
                degreeValues[0]
            }
            val pitch = degreeValues[1]
            val roll = degreeValues[2]

            onSensorUpdated.invoke(azimuth, pitch, roll)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, p1: Int) {

    }

    companion object {
        val TAG: String = SensorHelper::class.java.simpleName

        // 更新方位角畫面，最小間隔時間
        const val AZIMUTH_UPDATE_RATE = 2000   // 2秒
        // 更新方位角畫面，最小更新方位角值 (要大於才更新)
        const val AZIMUTH_MIN_UPDATE = 1F
    }
}