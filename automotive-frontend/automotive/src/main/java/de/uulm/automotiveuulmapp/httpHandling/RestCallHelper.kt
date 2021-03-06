package de.uulm.automotiveuulmapp.httpHandling

import android.content.Context
import com.android.volley.Network
import com.android.volley.Response
import com.android.volley.toolbox.BaseHttpStack
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.Volley
import de.uulm.automotiveuulmapp.topic.Callback
import org.json.JSONObject

open class RestCallHelper(private val context: Context?) {

    /**
     * Helper function to send http-requests to the REST-Api
     *
     * @param url Url of the rest-endpoint to be called
     * @param httpMethod HTTP Method to be used for the request
     * @param callback Defines behavior for successful and failed calls
     * @param body The Object in Json-Format to be sent within the http-body
     */
    open fun callRestEndpoint(
        url: String,
        httpMethod: Int,
        callback: Callback,
        body: JSONObject? = null
    ) {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)

        val customJsonRequest =
            CustomJsonRequest(httpMethod,
                url,
                body,
                Response.Listener<JSONObject> { response ->
                    callback.onSuccess(response)
                },
                Response.ErrorListener { error ->
                    callback.onFailure(error)
                })
        // Add the request to the RequestQueue
        queue.add(customJsonRequest)
    }
}
