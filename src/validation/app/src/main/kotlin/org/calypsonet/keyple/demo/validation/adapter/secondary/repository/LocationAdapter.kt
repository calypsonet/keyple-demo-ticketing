package org.calypsonet.keyple.demo.validation.adapter.secondary.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import javax.inject.Inject
import org.calypsonet.keyple.demo.validation.data.model.AppSettings
import org.calypsonet.keyple.demo.validation.domain.port.output.LocationProvider
import org.calypsonet.keyple.demo.validation.domain.model.Location as DomainLocation
import org.calypsonet.keyple.demo.validation.data.model.Location as DataLocation

/**
 * Adapter implementing LocationProvider port.
 * Provides location information from AppSettings and loads location list from resources.
 */
class LocationAdapter @Inject constructor(private val context: Context) : LocationProvider {

    private val locationList: List<DataLocation>

    init {
        locationList =
            getGson()
                .fromJson(getFileFromResources(context = context), Array<DataLocation>::class.java)
                .toList()
    }

    override suspend fun getCurrentLocation(): DomainLocation {
        return AppSettings.location.toDomain()
    }

    override suspend fun updateLocation(location: DomainLocation) {
        AppSettings.location = location.toData()
    }

    fun getLocations(): List<DataLocation> {
        return locationList
    }

    /** Get file from raw embedded directory */
    private fun getFileFromResources(context: Context): String {
        val resId = context.resources.getIdentifier("locations", "raw", context.packageName)
        val inputStream = context.resources.openRawResource(resId)
        return parseFile(inputStream)
    }

    private fun parseFile(inputStream: InputStream): String {
        val sb = StringBuilder()
        var strLine: String?
        try {
            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                while (reader.readLine().also { strLine = it } != null) {
                    sb.append(strLine)
                }
            }
        } catch (ignore: IOException) {
            // ignore
        }
        return sb.toString()
    }

    private fun getGson(): Gson {
        val gsonBuilder = GsonBuilder()
        gsonBuilder.disableHtmlEscaping()
        gsonBuilder.setPrettyPrinting()
        gsonBuilder.setLenient()
        return gsonBuilder.create()
    }

    // Mappers
    private fun DataLocation.toDomain() = DomainLocation(id = id, name = name)
    private fun DomainLocation.toData() = DataLocation(id = id, name = name)
}
