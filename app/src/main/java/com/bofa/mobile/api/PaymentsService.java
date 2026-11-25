package com.bofa.mobile.api;

import com.bofa.mobile.model.BillPaymentRequest;
import com.bofa.mobile.model.P2pPaymentRequest;
import com.bofa.mobile.model.Payee;
import com.bofa.mobile.model.Payment;
import com.bofa.mobile.model.PaymentResponse;

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
