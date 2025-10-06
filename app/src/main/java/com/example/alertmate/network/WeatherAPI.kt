import com.example.alertmate.data.HourlyWeatherResponse
import com.example.alertmate.data.WeatherResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

//current weather
interface WeatherAPI {
    @GET("weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric"
    ): Response<WeatherResponse>

    @GET("forecast") // use "forecast" for 3-hour intervals (free API) OR "forecast/hourly" for Pro API
    suspend fun getHourlyWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") appid: String,
        @Query("units") units: String = "metric"
    ): Response<HourlyWeatherResponse>
}


