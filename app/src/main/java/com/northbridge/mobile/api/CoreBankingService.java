package com.northbridge.mobile.api;

import com.northbridge.mobile.model.Account;
import com.northbridge.mobile.model.Customer;
import com.northbridge.mobile.model.Transaction;
import com.northbridge.mobile.model.TransactionPage;
import com.northbridge.mobile.model.TransferRequest;
import com.northbridge.mobile.model.TransferResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/** core-banking REST API (/api/v1). See docs/API.md in the core platform repository. */
public interface CoreBankingService {

    @GET("customers/me")
    Call<Customer> getCurrentCustomer();

    @GET("customers/{cif}")
    Call<Customer> getCustomer(@Path("cif") String cif);

    @GET("customers/{cif}/accounts")
    Call<List<Account>> getAccounts(@Path("cif") String cif);

    @GET("accounts/{accountNumber}")
    Call<Account> getAccount(@Path("accountNumber") String accountNumber);

    @GET("accounts/{accountNumber}/transactions")
    Call<TransactionPage> getTransactions(@Path("accountNumber") String accountNumber,
                                          @Query("from") String from,
                                          @Query("to") String to,
                                          @Query("page") int page,
                                          @Query("size") int size);

    @GET("transactions/search")
    Call<List<Transaction>> searchTransactions(@Query("q") String query,
                                               @Query("accountNumber") String accountNumber);

    @POST("transfers")
    Call<TransferResponse> createTransfer(@Body TransferRequest request);

    @GET("transfers/{transferId}")
    Call<TransferResponse> getTransfer(@Path("transferId") String transferId);
}
