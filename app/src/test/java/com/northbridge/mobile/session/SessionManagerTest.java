package com.northbridge.mobile.session;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class SessionManagerTest {

    @Test
    public void buildsBasicAuthorizationHeader() {
        SessionManager session = new SessionManager(ApplicationProvider.getApplicationContext());
        session.setCredentials("dana.whitfield", "Welcome1!", false);
        assertEquals("Basic ZGFuYS53aGl0ZmllbGQ6V2VsY29tZTEh", session.getAuthorizationHeader());
    }
}
