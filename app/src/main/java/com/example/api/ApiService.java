package com.example.api;

import com.example.model.*;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    // Auth
    @POST("api/v1/auth/login")
    Call<ApiResponse<AuthModels.AuthData>> login(@Body AuthModels.LoginRequest body);

    @POST("api/v1/auth/register/customer")
    Call<ApiResponse<AuthModels.AuthData>> registerCustomer(@Body AuthModels.RegisterCustomerRequest body);

    @POST("api/v1/auth/register/worker")
    Call<ApiResponse<AuthModels.AuthData>> registerWorker(@Body AuthModels.RegisterWorkerRequest body);

    // File Upload
    @Multipart
    @POST("api/v1/files/upload")
    Call<ApiResponse<Map<String, String>>> uploadFile(
            @Part MultipartBody.Part file,
            @Part("folder") RequestBody folder);

    @Multipart
    @POST("api/v1/auth/upload")
    Call<ApiResponse<Map<String, String>>> uploadRegistrationFile(
            @Part MultipartBody.Part file,
            @Part("folder") RequestBody folder);

    // Services
    @GET("api/v1/services")
    Call<ApiResponse<List<ServiceModel>>> getServices(@Query("q") String query);

    // Customer
    @GET("api/v1/customer/profile")
    Call<ApiResponse<Map<String, Object>>> getCustomerProfile();

    @PUT("api/v1/customer/profile")
    Call<ApiResponse<Map<String, Object>>> updateCustomerProfile(@Body Map<String, String> body);

    @GET("api/v1/customer/addresses")
    Call<ApiResponse<List<AddressModel>>> getAddresses();

    @POST("api/v1/customer/addresses")
    Call<ApiResponse<AddressModel>> addAddress(@Body Map<String, Object> body);

    @DELETE("api/v1/customer/addresses/{id}")
    Call<ApiResponse<Void>> deleteAddress(@Path("id") long id);

    @POST("api/v1/customer/jobs")
    Call<ApiResponse<JobModel>> createJob(@Body JobModel.CreateJobRequest body);

    @GET("api/v1/customer/jobs")
    Call<ApiResponse<List<JobModel>>> getCustomerJobs();

    @GET("api/v1/customer/jobs/{id}")
    Call<ApiResponse<JobModel.JobDetailData>> getCustomerJobDetail(@Path("id") long id);

    @POST("api/v1/customer/jobs/{id}/cancel")
    Call<ApiResponse<JobModel>> cancelCustomerJob(@Path("id") long id, @Body Map<String, String> body);

    @POST("api/v1/customer/jobs/{id}/pay")
    Call<ApiResponse<Map<String, Object>>> payJob(@Path("id") long id, @Body JobModel.PayJobRequest body);

    @POST("api/v1/customer/jobs/{id}/rate")
    Call<ApiResponse<JobModel>> rateWorker(@Path("id") long id, @Body JobModel.RateJobRequest body);

    @GET("api/v1/customer/payments")
    Call<ApiResponse<List<Map<String, Object>>>> getCustomerPayments();

    // Worker
    @GET("api/v1/worker/profile")
    Call<ApiResponse<WorkerModels.WorkerProfile>> getWorkerProfile();

    @PUT("api/v1/worker/profile")
    Call<ApiResponse<WorkerModels.WorkerProfile>> updateWorkerProfile(@Body Map<String, Object> body);

    @PUT("api/v1/worker/services")
    Call<ApiResponse<Void>> updateWorkerServices(@Body Map<String, List<Long>> body);

    @POST("api/v1/worker/documents")
    Call<ApiResponse<Void>> uploadWorkerDocument(@Body Map<String, String> body);

    @PUT("api/v1/worker/status")
    Call<ApiResponse<Map<String, Object>>> updateWorkerStatus(@Body Map<String, Object> body);

    @POST("api/v1/worker/location")
    Call<ApiResponse<Void>> updateWorkerLocation(@Body Map<String, Object> body);

    @GET("api/v1/worker/jobs")
    Call<ApiResponse<WorkerModels.WorkerJobsResponse>> getWorkerJobs();

    @POST("api/v1/worker/jobs/{id}/accept")
    Call<ApiResponse<JobModel>> acceptJob(@Path("id") long id);

    @POST("api/v1/worker/jobs/{id}/reject")
    Call<ApiResponse<Void>> rejectJob(@Path("id") long id);

    @POST("api/v1/worker/jobs/{id}/on-the-way")
    Call<ApiResponse<JobModel>> markOnTheWay(@Path("id") long id);

    @POST("api/v1/worker/jobs/{id}/arrived")
    Call<ApiResponse<JobModel>> markArrived(@Path("id") long id);

    @POST("api/v1/worker/jobs/{id}/start")
    Call<ApiResponse<JobModel>> startWork(@Path("id") long id);

    @POST("api/v1/worker/jobs/{id}/complete")
    Call<ApiResponse<JobModel>> completeJob(@Path("id") long id, @Body JobModel.CompleteJobRequest body);

    @GET("api/v1/worker/earnings")
    Call<ApiResponse<WorkerModels.WorkerEarningsResponse>> getWorkerEarnings();

    // Notifications & Complaints
    @POST("api/v1/notifications/fcm-token")
    Call<ApiResponse<Void>> registerFcmToken(@Body Map<String, String> body);

    @POST("api/v1/complaints")
    Call<ApiResponse<Map<String, Object>>> submitComplaint(@Body Map<String, Object> body);

    @GET("api/v1/complaints")
    Call<ApiResponse<List<AdminModels.ComplaintItem>>> getMyComplaints();

    // Admin
    @GET("api/v1/admin/dashboard")
    Call<ApiResponse<AdminModels.DashboardStats>> getAdminDashboard();

    @GET("api/v1/admin/customers")
    Call<ApiResponse<List<AdminModels.AdminCustomerItem>>> getAdminCustomers(@Query("q") String q);

    @PUT("api/v1/admin/customers/{userId}/status")
    Call<ApiResponse<Void>> setCustomerStatus(@Path("userId") long userId, @Body Map<String, Boolean> body);

    @GET("api/v1/admin/workers")
    Call<ApiResponse<List<AdminModels.AdminWorkerItem>>> getAdminWorkers(@Query("status") String status);

    @PUT("api/v1/admin/workers/{workerId}/verify")
    Call<ApiResponse<Void>> verifyWorker(@Path("workerId") long workerId);

    @PUT("api/v1/admin/workers/{workerId}/reject")
    Call<ApiResponse<Void>> rejectWorker(@Path("workerId") long workerId, @Body Map<String, String> body);

    @PUT("api/v1/admin/workers/{workerId}/suspend")
    Call<ApiResponse<Void>> suspendWorker(@Path("workerId") long workerId);

    @PUT("api/v1/admin/workers/{workerId}/activate")
    Call<ApiResponse<Void>> activateWorker(@Path("workerId") long workerId);

    @GET("api/v1/admin/services")
    Call<ApiResponse<List<ServiceModel>>> getAdminServices();

    @POST("api/v1/admin/services")
    Call<ApiResponse<ServiceModel>> addAdminService(@Body Map<String, Object> body);

    @PUT("api/v1/admin/services/{id}")
    Call<ApiResponse<ServiceModel>> updateAdminService(@Path("id") long id, @Body Map<String, Object> body);

    @DELETE("api/v1/admin/services/{id}")
    Call<ApiResponse<Void>> deleteAdminService(@Path("id") long id);

    @GET("api/v1/admin/jobs")
    Call<ApiResponse<List<JobModel>>> getAdminJobs(@Query("q") String q, @Query("status") String status);

    @PUT("api/v1/admin/jobs/{jobId}/reassign")
    Call<ApiResponse<JobModel>> reassignAdminJob(@Path("jobId") long jobId, @Body Map<String, Long> body);

    @POST("api/v1/admin/jobs/{jobId}/cancel")
    Call<ApiResponse<JobModel>> cancelAdminJob(@Path("jobId") long jobId, @Body Map<String, String> body);

    @GET("api/v1/admin/payments")
    Call<ApiResponse<List<AdminModels.AdminPaymentItem>>> getAdminPayments();

    @POST("api/v1/admin/payments/{paymentId}/refund")
    Call<ApiResponse<Void>> refundPayment(@Path("paymentId") long paymentId);

    @GET("api/v1/admin/complaints")
    Call<ApiResponse<List<AdminModels.ComplaintItem>>> getAdminComplaints();

    @PUT("api/v1/admin/complaints/{id}")
    Call<ApiResponse<Void>> updateAdminComplaint(@Path("id") long id, @Body Map<String, String> body);
}
