package com.threshold.updownloader

import io.reactivex.Observable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PartMap
import retrofit2.http.Streaming
import retrofit2.http.Url

interface UpDownApi {
    // https://github.com/yourusername/awesomegames/archive/master.zip
    //for example: baseUrl is  "https://github.com/"  parameter is "yourusername/awesomegames/archive/master.zip"
    @GET
    @Streaming //For downloading,You must annotation as Streaming and return ResponseBody
    fun downloadByUrl(@Url fileUrl: String): Observable<Response<ResponseBody>>

//    @GET("files")
//    @Streaming //For downloading,You must annotation as Streaming and return ResponseBody
//    fun downloadByName(@Query("filename") filename: String): Observable<Response<ResponseBody>>

    @POST
    @Multipart
    fun uploadByUrl(@Url fileUrl: String, @PartMap params: Map<String, @JvmSuppressWildcards RequestBody>): Observable<String>

//    @POST("files")
//    @Multipart
//    fun upload(@PartMap params: Map<String, RequestBody>): Observable<List<FileBean>>
}
/*
    *  For upload,pass params like this:
        RequestBody fileBody = RequestBody.create(MediaType.parse("image/*"), imgFile);
        mParams = new HashMap<>();// Request parameter
        mParams.put("file\"; filename=\"" + imgFile.getName() + "", fileBody);

        RequestBody requestBody1 = RequestBody.create(MultipartBody.FORM, file1);
        RequestBody requestBody2 = RequestBody.create(MultipartBody.FORM, file2);
        Map<String, RequestBody> fileMap = new HashMap<>();
        fileMap.put("file\"; filename=\" api.txt", requestBody1);
        fileMap.put("file\"; filename=\" 2.txt", requestBody2);
     */