package com.northbridge.mobile.api;

import com.northbridge.mobile.model.BillPaymentRequest;
import com.northbridge.mobile.model.P2pPaymentRequest;
import com.northbridge.mobile.model.Payee;
import com.northbridge.mobile.model.Payment;
import com.northbridge.mobile.model.PaymentResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Payments hub API (/api/v1/payments): bill pay and person-to-person. */
public interface PaymentsService {

    @GET("payees")
    Call<List<Payee>> getPayees(@Query("cif") String cif);

    @POST("bill")
    Call<PaymentResponse> payBill(@Body BillPaymentRequest request);

    @POST("p2p")
    Call<PaymentResponse> sendP2p(@Body P2pPaymentRequest request);

    @GET("history")
    Call<List<Payment>> getHistory(@Query("cif") String cif);
}
