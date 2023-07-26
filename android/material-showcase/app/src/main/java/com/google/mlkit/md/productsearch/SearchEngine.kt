/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.md.productsearch

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.md.objectdetection.DetectedObjectInfo
import org.json.JSONException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/** A fake search engine to help simulate the complete work flow.  */
class SearchEngine(context: Context) {

    private val searchRequestQueue: RequestQueue = Volley.newRequestQueue(context)
    private val requestCreationExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun search(
        detectedObject: DetectedObjectInfo,
        listener: (detectedObject: DetectedObjectInfo, productList: List<Product>) -> Unit,
    ) {
        // Crops the object image out of the full image is expensive, so do it off the UI thread.
        Tasks.call<JsonObjectRequest>(requestCreationExecutor, Callable { createRequest(detectedObject) })
            .addOnSuccessListener { productRequest -> searchRequestQueue.add(productRequest.setTag(TAG)) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to create product search request!", e)
                // Remove the below dummy code after your own product search backed hooked up.
                val productList = ArrayList<Product>()
                // Got URL from serpapi.com Google Search API Playground + my account's api_key
                val url =
                    "https://serpapi.com/search.json?q=$detectedObject&gl=us&google_domain=google.com&hl=en&api_key=894f7fbcc2468434f06bb956426848a7895514341674f0adbeec4cd9cf93fc2f"
                // Hold search results
                var title: String
                var link: String

                // Create JSONObjectRequest because Search API uses JSON object
                val jsonObjectRequest = JsonObjectRequest(
                    Request.Method.GET, url, null,
                    { response ->
                        try {
                            // Get the successful responses
                            val organicResultsList = response.getJSONArray("organic_results")

                            // Loop through and retrieve title, link, snippet from each successful result object
                            for (i in 0 until organicResultsList.length()) {
                                val organicObj = organicResultsList.getJSONObject(i)
                                if (organicObj.has("title")) {
                                    title = organicObj.getString("title")
                                }
                                if (organicObj.has("link")) {
                                    link = organicObj.getString("link")
                                }

                                // Push these results as Product objects into productList
                                productList.add(
                                    Product(/* imageUrl= */"", "Product title $i", "Product subtitle $i")
                                )
                            }
                            // Invoke listener
                            listener.invoke(detectedObject, productList)
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                    }) { // Fail:
                    Toast.makeText(null, "No results found. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun shutdown() {
        searchRequestQueue.cancelAll(TAG)
        requestCreationExecutor.shutdown()
    }

    companion object {
        private const val TAG = "SearchEngine"

        @Throws(Exception::class)
        private fun createRequest(searchingObject: DetectedObjectInfo): JsonObjectRequest {
            val objectImageData = searchingObject.imageData
                ?: throw Exception("Failed to get object image data!")

            // Hooks up with your own product search backend here.
            throw Exception("Hooks up with your own product search backend.")
        }
    }
}
