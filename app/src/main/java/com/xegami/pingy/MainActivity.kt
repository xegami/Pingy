package com.xegami.pingy

import android.annotation.TargetApi
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.support.annotation.UiThread
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.Editable
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_main.*

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.ref.WeakReference
import java.util.stream.Collectors


class MainActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {

    // google by default
    private var selectedServer: String = "8.8.8.8"
    private val serverList = arrayOf("Google 8.8.8.8", "LoL-EUW 104.160.142.3", "Facebook 69.63.176.13")
    private val editable = Editable.Factory.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initContent()
    }

    private fun initContent() {
//        This seems to be a bug in the Design Support Library v28.0.0.
//        floatingActionButton.scaleType = ImageView.ScaleType.CENTER

        selectServerSpinner.onItemSelectedListener = this
        val arrayAdapter = ArrayAdapter(this, R.layout.spinner_list_item, serverList)
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        selectServerSpinner.adapter = arrayAdapter

        ipAddr1.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_NEXT) {
                if (ipAddr1.text.toString().toInt() > 255) {
                    toast("No puede superar 255").show()
                    ipAddr1.text = editable.newEditable("255")
                } else {
                    ipAddr2.requestFocus()
                }
            }
            true
        }
        ipAddr2.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_NEXT) {
                if (ipAddr2.text.toString().toInt() > 255) {
                    toast("No puede superar 255").show()
                    ipAddr2.text = editable.newEditable("255")
                } else {
                    ipAddr3.requestFocus()
                }
            }
            true
        }
        ipAddr3.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_NEXT) {
                if (ipAddr3.text.toString().toInt() > 255) {
                    toast("No puede superar 255").show()
                    ipAddr3.text = editable.newEditable("255")
                } else {
                    ipAddr4.requestFocus()
                }
            }
            true
        }
        ipAddr4.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE) {
                if (ipAddr4.text.toString().toInt() > 255) {
                    toast("No puede superar 255").show()
                    ipAddr4.text = editable.newEditable("255")
                } else {
                    val imm = this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    val contentView: View = findViewById(android.R.id.content)
                    imm.hideSoftInputFromWindow(contentView.windowToken, 0)
                    ipAddr4.clearFocus()
                    selectedServer = "${ipAddr1.text}.${ipAddr2.text}.${ipAddr3.text}.${ipAddr4.text}"
                    printConnection()
                }
            }
            true
        }

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.OVAL
        shape.setColor(ContextCompat.getColor(this, R.color.signalDefault))
        pingScreen.background = shape

        pingCheckButton.setOnClickListener { printConnection() }
    }

    private fun printConnection() {
        if (ipAddr1.text.toString().toInt() > 255 ||
            ipAddr2.text.toString().toInt() > 255 ||
            ipAddr3.text.toString().toInt() > 255 ||
            ipAddr4.text.toString().toInt() > 255
        ) {
            toast("Segmentos IP no pueden superar 255").show()
            return
        }

        if (isOnline()) {
            if (ATHandler(this).execute(selectedServer).get() == "fail") {
                toast("packet loss").show()
            } else {
                toast("success").show()
            }
        } else {
            toast("no internet").show()
        }
    }

    private class ATHandler internal constructor(context: MainActivity) : AsyncTask<String, String, String>() {
        private val activity: WeakReference<MainActivity> = WeakReference(context)

        override fun onPreExecute() {
            activity.get()!!.pingCheckButtonEnabled(false)
        }

        override fun doInBackground(vararg params: String?): String {
            val runtime = Runtime.getRuntime()
            val process = runtime.exec("/system/bin/ping -c 1 ${params[0]}")
            val output = activity.get()!!.inputStreamToString(process.inputStream)

            return when (activity.get()!!.printPings(output)) {
                true -> "success"
                false -> "fail"
            }
        }

        override fun onPostExecute(result: String?) {
            activity.get()!!.pingCheckButtonEnabled(true)
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
            pingCheckButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.signalGood))
            pingCheckButton.isClickable = true
        } else {
            pingCheckButton.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.signalLost))
            pingCheckButton.isClickable = false
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        selectedServer = serverList[position].split(" ")[1]
        val arAddr = selectedServer.split(".")
        ipAddr1.text = editable.newEditable(arAddr[0])
        ipAddr2.text = editable.newEditable(arAddr[1])
        ipAddr3.text = editable.newEditable(arAddr[2])
        ipAddr4.text = editable.newEditable(arAddr[3])
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

    private fun toast(type: String): Toast {
        val message = when (type) {
            "no internet" -> "NO INTERNET CONNECTION"
            "packet loss" -> "100% PACKET LOSS"
            "success" -> "PING SUCCESSFULL"
            else -> type
        }

        return Toast.makeText(this, message, Toast.LENGTH_LONG)
    }

}
