import com.example.alertmate.data.OneCallResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

//current weather
interface OneCallApi {
    @GET("onecall")
    suspend fun getOneCall(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("exclude") exclude: String? = null,   // e.g. "minutely,alerts"
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "en",
        @Query("appid") apiKey: String
    ): Response<OneCallResponse>
}


