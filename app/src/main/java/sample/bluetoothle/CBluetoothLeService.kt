package sample.bluetoothle

import android.os.Binder
import android.os.IBinder
import android.app.Service
import android.bluetooth.*
import android.content.Intent

import java.util.*

class CBluetoothLeService : Service() {

    private var mBluetoothGatt: BluetoothGatt? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    private var mExchangeCharacteristic: BluetoothGattCharacteristic? = null

    companion object {

        const val ACTION_DATA_READ = "ACTION_DATA_READ"
        const val ACTION_DATA_WRITE = "ACTION_DATA_WRITE"
        const val ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE"
        const val ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED"
        const val ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED"
        const val ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED"
    }

    /** Create service */
    // -------------------------------------------------------------------

    inner class LocalBinder : Binder() {
        fun getService() : CBluetoothLeService {
            return this@CBluetoothLeService
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return LocalBinder()
    }

    override fun onUnbind(intent: Intent?): Boolean {

        mBluetoothGatt?.let { gatt ->
            gatt.close()
            mBluetoothGatt = null
        }

        return super.onUnbind(intent)
    }

    // -------------------------------------------------------------------

    /** Get bluetooth adapter */
    fun initialize(): Boolean {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if(mBluetoothAdapter == null)
            return false
        return true
    }

    /** Connect gatt */
    fun connect(address: String): Boolean {

        mBluetoothAdapter?.let { adapter ->

            return try {
                val device = adapter.getRemoteDevice(address)
                mBluetoothGatt = device.connectGatt(this, false, bluetoothGattCallback)
                true
            } catch (exception: IllegalArgumentException) {
                false
            }
        }

        return false
    }

    /** Broadcast update */
    private fun broadcastUpdate(action: String, characteristic: BluetoothGattCharacteristic?) {
        val intent = Intent(action)

        characteristic?.let { it ->

            intent.putExtra("EXTRA_DATA", it.value)
        }

        sendBroadcast(intent)
    }

    /** Send data */
    fun writeCharacteristic(byteArray: ByteArray) {

        if(mBluetoothAdapter == null || mBluetoothGatt == null)
            return

        mExchangeCharacteristic?.value = byteArray
        mBluetoothGatt?.writeCharacteristic(mExchangeCharacteristic)
    }

    // -------------------------------------------------------------------

    /** Bluetooth gatt callback */
    private val bluetoothGattCallback = object : BluetoothGattCallback() {

        /** Link */
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {

            when(newState) {

                BluetoothProfile.STATE_CONNECTED -> {
                    broadcastUpdate(ACTION_GATT_CONNECTED, null)

                    gatt?.discoverServices()
                }

                BluetoothProfile.STATE_DISCONNECTED -> {
                    broadcastUpdate(ACTION_GATT_DISCONNECTED, null)
                }
            }
        }

        /** Find service */
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {

            if(status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED, null)

                gatt?.let { bluetoothGatt ->

                    for(service in bluetoothGatt.services) {
                        for(characteristic in service.characteristics) {

                            if(characteristic.properties and 0xF0 == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {

                                characteristic.let {

                                    mExchangeCharacteristic = it
                                    bluetoothGatt.setCharacteristicNotification(it, true)
                                }
                            }
                        }
                    }
                }
            }
        }

        /** Send data */
        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {

            if(status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_DATA_WRITE, characteristic)
        }

        /** receive data */
        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {

            if(status == BluetoothGatt.GATT_SUCCESS)
                broadcastUpdate(ACTION_DATA_READ, characteristic)
        }

        /** Changed data */
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {

            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic)
        }
    }
}