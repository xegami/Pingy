package com.xegami.pingy

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.os.AsyncTask
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pingCheckButton.setOnClickListener { printConnection() }
    }

    private fun printConnection() {
        ATHandler().execute(null)
    }

    @SuppressLint("StaticFieldLeak")
    inner class ATHandler : AsyncTask<String, String, String>() {
        override fun onPreExecute() {
            pingCheckButtonEnabled(false)
        }

        override fun doInBackground(vararg params: String?): String {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("/system/bin/ping -c 4 8.8.8.8")
            val output = inputStreamToString(process.inputStream)
            printPings(output)

            return ""
        }

        override fun onPostExecute(result: String?) {
            pingCheckButtonEnabled(true)
        }
    }


    @TargetApi(24)
    private fun inputStreamToString(inputStream: InputStream): String {
        return BufferedReader(InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"))
    }

    private fun printPings(output: String) {
        var pings = 0.0

        output.split("\n").forEach {
            if (it.contains("time=")) {
                val ping: Double = it.split("time=")[1].split(" ")[0].toDouble()
                pings += ping
            }
        }

        pingScreen.text = "${(pings / 4).toInt()} ms"
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
}
