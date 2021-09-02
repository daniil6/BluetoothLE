package sample.bluetoothle

import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast

import androidx.appcompat.app.AppCompatActivity

class CBluetoothLeActivity : AppCompatActivity() {

    private var mMacAddress : String? = null

    private var mBluetoothService : CBluetoothLeService? = null

    private lateinit var mTxtDataBluetoothLe : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetoothle_activity)

        /** GUI */
        val btnSendBluetoothLe = findViewById<Button>(R.id.btnSendBluetoothLe)
        val txtSendBluetoothLe = findViewById<EditText>(R.id.txtSendBluetoothLe)
        mTxtDataBluetoothLe = findViewById(R.id.txtDataBluetoothLe)

        /** bind bluetooth service */
        val gattServiceIntent = Intent(this, CBluetoothLeService::class.java)
        bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE)

        /** Set params Button */
        btnSendBluetoothLe.setOnClickListener {

            val text = txtSendBluetoothLe.text.toString()

            if(!TextUtils.isEmpty(text)) {
                mBluetoothService?.writeCharacteristic(text.toByteArray())

                var data = mTxtDataBluetoothLe.text.toString()
                data = data + text + '\n'
                mTxtDataBluetoothLe.text = data
            }
        }

        mMacAddress = intent.getStringExtra(resources.getString(R.string.intent_mac_address))
    }

    override fun onResume() {
        super.onResume()

        val intentFilter = IntentFilter().apply {

            addAction(CBluetoothLeService.ACTION_DATA_READ)
            addAction(CBluetoothLeService.ACTION_DATA_WRITE)
            addAction(CBluetoothLeService.ACTION_DATA_AVAILABLE)
            addAction(CBluetoothLeService.ACTION_GATT_CONNECTED)
            addAction(CBluetoothLeService.ACTION_GATT_DISCONNECTED)
            addAction(CBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED)
        }

        /** Register receiver */
        registerReceiver(mGattUpdateReceiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()

        /** Unregister receiver */
        unregisterReceiver(mGattUpdateReceiver)
    }

    /** Interface service */
    private val mServiceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {

            mBluetoothService = (service as CBluetoothLeService.LocalBinder).getService()
            mBluetoothService?.let { bluetooth ->
                if(!bluetooth.initialize())
                    finish()

                mMacAddress?.let {
                    bluetooth.connect(it)
                } ?: finish()
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            mBluetoothService = null
        }
    }

    /** Callback service */
    private val mGattUpdateReceiver: BroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {

            when(intent.action) {

                CBluetoothLeService.ACTION_DATA_READ -> {
                    Toast.makeText(applicationContext, "ACTION_DATA_READ", Toast.LENGTH_SHORT).show()
                }

                CBluetoothLeService.ACTION_DATA_WRITE -> {
                    Toast.makeText(applicationContext, "ACTION_DATA_WRITE", Toast.LENGTH_SHORT).show()
                }

                CBluetoothLeService.ACTION_DATA_AVAILABLE -> {
                    Toast.makeText(applicationContext, "ACTION_DATA_AVAILABLE", Toast.LENGTH_SHORT).show()

                    val text : String

                    intent.getByteArrayExtra("EXTRA_DATA")?.let { array ->
                        text = String(array, Charsets.UTF_8)

                        var data = mTxtDataBluetoothLe.text.toString()
                        data = data + text + '\n'
                        mTxtDataBluetoothLe.text = data
                    }
                }

                CBluetoothLeService.ACTION_GATT_CONNECTED -> {
                    Toast.makeText(applicationContext, "ACTION_GATT_CONNECTED", Toast.LENGTH_SHORT).show()
                }

                CBluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    Toast.makeText(applicationContext, "ACTION_GATT_DISCONNECTED", Toast.LENGTH_SHORT).show()
                }

                CBluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    Toast.makeText(applicationContext, "ACTION_GATT_SERVICES_DISCOVERED", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}