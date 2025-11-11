import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//use it to call the weather API from anywhere in the app.
object RetrofitInstance {
    private const val BASE_URL = "https://api.openweathermap.org/data/3.0/"

    //this is the actual API object we will use to make requests lazy` means: it is created only when we use it.
    val api: OneCallApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OneCallApi::class.java)
    }
}
