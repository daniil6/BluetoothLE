package sample.bluetoothle

import android.widget.*
import android.os.Looper
import android.os.Bundle
import android.os.Handler
import android.content.Intent
import android.bluetooth.BluetoothAdapter

import androidx.appcompat.app.AppCompatActivity

class CBluetoothLeListActivity : AppCompatActivity() {

    /** Bluetooth adapter */
    private var mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    /** List find Bluetooth */
    private var mListBluetoothLe = ArrayList<HashMap<String?, String?>>()

    /** Adapter for ListView */
    private lateinit var mAdapterBluetoothLe: SimpleAdapter

    companion object {

        private const val SCAN_PERIOD: Long = 10000
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bluetoothle_list_activity)

        val listViewBluetoothLe = findViewById<ListView>(R.id.listViewBluetoothLe)
        val btnRefreshBluetoothLe = findViewById<Button>(R.id.btnRefreshBluetoothLe)

        /** Init adapter of ListView */
        val layoutAdapter = android.R.layout.simple_list_item_2
        val arrayLabel = arrayOf("Name", "Address")
        val arrayValue = intArrayOf(android.R.id.text1, android.R.id.text2)
        mAdapterBluetoothLe = SimpleAdapter(this, mListBluetoothLe, layoutAdapter, arrayLabel, arrayValue)

        /** Set params of ListView */
        listViewBluetoothLe.adapter = mAdapterBluetoothLe
        listViewBluetoothLe.setOnItemClickListener { parent, item, position, id ->

            val data = mAdapterBluetoothLe.getItem(position) as HashMap<*, *>

            val mac = data["Address"] as String

            val device = mBluetoothAdapter.getRemoteDevice(mac)

            Intent(baseContext, CBluetoothLeActivity::class.java).let {

                it.putExtra(resources.getString(R.string.intent_mac_address), device.address)

                startActivity(it)
            }
        }

        /** Set params of Button */
        btnRefreshBluetoothLe.setOnClickListener { startScan() }

        startScan()
    }

    private fun startScan() {

        mListBluetoothLe.clear()
        mAdapterBluetoothLe.notifyDataSetChanged()

        Thread {
            mBluetoothAdapter.startLeScan { device, rssi, scanRecord ->

                for(item in mListBluetoothLe)
                    if(item.containsValue(device.name))
                        return@startLeScan

                val map: HashMap<String?, String?> = HashMap()
                map["Name"] = device.name
                map["Address"] = device.address

                Handler(Looper.getMainLooper()).postDelayed({
                    mListBluetoothLe.add(map)
                    mAdapterBluetoothLe.notifyDataSetChanged()
                }, 0)

                if(device.name.equals("HMSoft")) {

                    Handler(Looper.getMainLooper()).postDelayed({
                        stopScan()
                    }, SCAN_PERIOD)
                }
            }
        }.start()
    }

    private fun stopScan() {
        mBluetoothAdapter.stopLeScan{ _, _, _ -> }
    }
}