package com.bofa.mobile.ui.login;

import android.view.View;
import android.widget.TextView;

import com.bofa.mobile.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class LoginActivityTest {

    @Test
    public void emptyFormShowsRequiredFieldError() {
        LoginActivity activity = Robolectric.buildActivity(LoginActivity.class).setup().get();

        activity.findViewById(R.id.login_submit).performClick();

        TextView error = activity.findViewById(R.id.login_error);
        assertEquals(View.VISIBLE, error.getVisibility());
        assertEquals(activity.getString(R.string.login_error_required), error.getText().toString());
    }
}
