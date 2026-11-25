package com.example;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.api.NetworkErrorParser;
import com.example.model.JobModel;
import com.google.gson.Gson;

import org.junit.Test;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

public class KaamMitraUnitTest {

    @Test
    public void networkErrorParser_mapsTimeoutAndConnectFriendlyMessages() {
        String timeoutMsg = NetworkErrorParser.parseThrowable(new SocketTimeoutException("timeout"));
        assertTrue(timeoutMsg.toLowerCase().contains("timed out"));

        String connectMsg = NetworkErrorParser.parseThrowable(new ConnectException("Connection refused"));
        assertTrue(connectMsg.contains("Kaam-Mitra server"));
    }

    @Test
    public void jobModelSerialization_preservesFields() {
        JobModel.CreateJobRequest req = new JobModel.CreateJobRequest();
        req.serviceId = 3L;
        req.problemTitle = "AC is not cooling";
        req.address = "Rajkot";
        req.latitude = 22.3039;
        req.longitude = 70.8022;
        req.estimatedBudget = 500.0;

        Gson gson = new Gson();
        String json = gson.toJson(req);
        JobModel.CreateJobRequest decoded = gson.fromJson(json, JobModel.CreateJobRequest.class);

        assertNotNull(decoded);
        assertEquals(3L, decoded.serviceId);
        assertEquals("AC is not cooling", decoded.problemTitle);
        assertEquals(500.0, decoded.estimatedBudget, 0.01);
        assertFalse(json.isEmpty());
    }
}
