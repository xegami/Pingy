package com.xegami.pingy

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    // google by default
    private var selectedServer: String = "8.8.8.8"
    private val serverList = arrayOf("Google 8.8.8.8", "LoL-EUW 104.160.142.3", "Facebook 69.63.176.13")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pingCheckButton.setOnClickListener { printConnection() }

        selectServerSpinner!!.onItemSelectedListener = this
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serverList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        selectServerSpinner!!.adapter = arrayAdapter

    }

    private fun printConnection() {
        if (isOnline()) {
            if (ATHandler().execute(selectedServer).get() == "fail") {
                toast("packet loss").show()
            } else {
                toast("success").show()
            }
        } else {
            toast("no internet").show()
        }
    }

    @SuppressLint("StaticFieldLeak")
    private inner class ATHandler : AsyncTask<String, String, String>() {
        override fun onPreExecute() {
            pingCheckButtonEnabled(false)
        }

        override fun doInBackground(vararg params: String?): String {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("/system/bin/ping -c 4 ${params[0]}")
            val output = inputStreamToString(process.inputStream)

            return when (printPings(output)) {
                true -> "success"
                false -> "fail"
            }
        }

        override fun onPostExecute(result: String?) {
            pingCheckButtonEnabled(true)
        }
    }

    @TargetApi(24)
    private fun inputStreamToString(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"))
    }


    private fun printPings(output: String): Boolean {
        var pings = 0.0

        if (output.contains("0 received")) {
            return false
        }

        output.split("\n").forEach {
            if (it.contains("time=")) {
                val ping: Double = it.split("time=")[1].split(" ")[0].toDouble()
                pings += ping
            }
        }

        pingScreen.text = "${(pings / 4).toInt()} ms"

        return true
    }

    @UiThread
    private fun pingCheckButtonEnabled(state: Boolean) {
        if (state) {
            pingCheckButton.text = "GO"
            pingCheckButton.isClickable = true
        } else {
            pingCheckButton.text = "CHECKING..."
            pingCheckButton.isClickable = false
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedServer = serverList[position].split(" ")[1]
    }

    private fun isOnline(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        var isAvailable = false

        if (networkInfo != null && networkInfo.isConnected) {
            isAvailable = true
        }

        return isAvailable
    }

    private fun toast(type: String) : Toast {
        val message = when (type) {
            "no internet" -> "NO INTERNET CONNECTION"
            "packet loss" -> "100% PACKET LOSS"
            "success" -> "PING SUCCESSFULL"
            else -> "MIS MUERTOS"
        }

        return Toast.makeText(this, message, Toast.LENGTH_LONG)
    }

}
