package com.motondon.signinapidemoapp;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.Scope;

import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int RC_SIGN_IN = 101;
    private static final int REQUEST_PERMISSION = 102;

    private GoogleApiClient mGoogleApiClient;
    private GoogleSignInAccount mGoogleSignInAccount;

    @BindView(R.id.sign_in_button) SignInButton btnSignIn;
    @BindView(R.id.sign_out_button) Button btnSignOut;
    @BindView(R.id.revoke_access_button) Button btnRevokeAccess;
    @BindView(R.id.request_access_button) Button btnRequestAccess;

    @BindView(R.id.tv_person_id) TextView tvPersonId;
    @BindView(R.id.tv_person_display_name) TextView tvPersonDisplayName;
    @BindView(R.id.tv_person_given_name) TextView tvPersonGivenName;
    @BindView(R.id.tv_person_family_name) TextView tvPersonFamilyName;
    @BindView(R.id.tv_person_email) TextView tvPersonEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // Check if play services installed and up to date - returns zero (0) for SUCCESS or another error code
        Log.i(TAG, "onCreate() - Is Google Play Services available and up to date? "
                + GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this));

        // Kick off the request to build GoogleApiClient.
        buildGoogleApiClient();

        btnSignIn.setEnabled(false);
        btnSignOut.setEnabled(false);
        btnRevokeAccess.setEnabled(false);
        btnRequestAccess.setEnabled(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart() - connecting...");
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient != null) {
            Log.d(TAG, "onStop() - disconnecting...");
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "onConnected() - Begin");

        OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(mGoogleApiClient);
        if (opr.isDone()) {
            // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
            // and the GoogleSignInResult will be available instantly.
            Log.d(TAG, "onStart() - Got cached sign-in");
            GoogleSignInResult result = opr.get();
            handleSignInResult(result);
        } else {
            // If the user has not previously signed in on this device or the sign-in has expired,
            // this asynchronous branch will attempt to sign in the user silently.  Cross-device
            // single sign-on will occur in this branch.
            opr.setResultCallback(googleSignInResult -> {
                handleSignInResult(googleSignInResult);
            });
        }
    }

    /**
     * Called when {@code mGoogleApiClient} is disconnected.
     */
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() - Cause: " + i);

        // Something happen with current Services connection. Disable all buttons. When connection is ok again, onConnect() will
        // be called which calls handleSignInResult() method. This one will enable buttons accordingly again.
        btnSignIn.setEnabled(false);
        btnSignOut.setEnabled(false);
        btnRevokeAccess.setEnabled(false);
        btnRequestAccess.setEnabled(false);
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution is
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.d(TAG, "onConnectionFailed() - result: " + result.toString());
    }

    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult() - requestCode: " + requestCode + " - resultCode: " + resultCode);

        switch (requestCode) {
            // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...). This is done when user clicks on
            // the SignIn button.
            case RC_SIGN_IN:
                if (resultCode == RESULT_OK) {
                    GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                    handleSignInResult(result);

                } else if( resultCode == RESULT_CANCELED ) {
                    // If you are getting here, it is most likely you forgot to add you SHA-1 certificate to the Google Developer Console. If that is
                    // the case take a look on the following link: https://developers.google.com/identity/sign-in/android/start-integrating
                    Toast.makeText(this,"You got a RESULT_CANCELED result code. Did you forget to add the certificate SHA-1 to your Google Developer Console? ",Toast.LENGTH_LONG).show();
                }
                break;

            // Result returned from launching the Intent com.google.android.gms, when user clicks on "Request Access" button
            case REQUEST_PERMISSION:
                Log.i(TAG, "onActivityResult() - requestCode: " + requestCode + " - resultCode: " + resultCode);

                // Get the result and log it. Maybe we can do something here depends on the result.
                if (data.getExtras() != null && data.getExtras().containsKey("Error")) {
                    String result = data.getExtras().getString("Error");
                    Set<Scope> grantedScopes = mGoogleSignInAccount.getGrantedScopes();

                    if (result.equals("Ok")) {
                        Log.d(TAG, "onActivityResult() - Confirmed Permission. GrantedScopes: " + grantedScopes);

                    } else if (result.equals("UserCancel")) {
                        Log.w(TAG, "onActivityResult() - User cancelled permission request. GrantedScopes: " + grantedScopes);
                    }
                }

                // If want to get current token
                if (data.getExtras() != null && data.getExtras().containsKey("authtoken")) {
                    String token = data.getExtras().getString("authtoken");
                    Log.d(TAG, "onActivityResult() - token: " + token);
                }

                // Log all received extras
                Bundle bundle = data.getExtras();
                for (String key : bundle.keySet()) {
                    Object value = bundle.get(key);
                    Log.d(TAG, "onActivityResult() - Extra: >>> " + String.format("%s %s (%s)", key,
                            value.toString(), value.getClass().getName()));
                }

                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @OnClick(R.id.sign_in_button)
    public void onSignInButtonClick() {
        Log.d(TAG, "onSignInButtonClick()");

        // If there is more than one account on the device, an account dialog will ge shown for the user to choose one account.
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @OnClick(R.id.sign_out_button)
    public void onSignOutButtonClick() {
        Log.d(TAG, "onSignOutButtonClick()");

        if (!mGoogleApiClient.isConnected()) {
            Log.e(TAG, "onSignOutButtonClick() - GoogleApiClient not connected");
            Toast.makeText(getApplicationContext(), "GoogleApiClient not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "onSignOutButtonClick() - Requesting a signOut...");

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(status -> {
            if (status.isSuccess()) {
                Log.d(TAG, "onSignOutButtonClick() - User was successfully signed out");

            } else {
                String error = status.getStatusMessage();
                Log.e(TAG, "onSignOutButtonClick() - Error while trying to sign out user. Error: " + error);
            }

            // After a successful signOut, only signIn button must be enabled.
            btnSignIn.setEnabled(true);
            btnSignOut.setEnabled(false);
            btnRevokeAccess.setEnabled(false);
            btnRequestAccess.setEnabled(true);

            // Clean up views
            cleanUp();
        });
    }

    @OnClick(R.id.revoke_access_button)
    public void onRevokeAccessButtonClick() {
        Log.d(TAG, "onRevokeAccessButtonClick()");

        if (!mGoogleApiClient.isConnected()) {
            Log.e(TAG, "onRevokeAccessButtonClick() - GoogleApiClient not connected");
            Toast.makeText(getApplicationContext(), "GoogleApiClient not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "onSignOutButtonClick() - Requesting a revokeAccess...");

        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(s -> {
            if (s.isSuccess()) {
                Log.d(TAG, "onRevokeAccessButtonClick() - Successfully revoked access for the user");

                Set<Scope> grantedScopes = mGoogleSignInAccount.getGrantedScopes();
                Log.d(TAG, "onRevokeAccessButtonClick() - GrantedScopes: " + grantedScopes);

            } else {
                String error = s.getStatusMessage();
                Log.e(TAG, "onRevokeAccessButtonClick() - Error while trying to revoke access for the user. Error: " + error);
            }

            // After a successful revokeAccess, only signIn button must be enabled.
            btnSignIn.setEnabled(true);
            btnSignOut.setEnabled(false);
            btnRevokeAccess.setEnabled(false);
            btnRequestAccess.setEnabled(true);

            // Clean up views
            cleanUp();
        });
    }

    /**
     * When sending this intent, if the access was already granted (during the signIn action), it will return true in the activityResult method.
     * But, after user revoke access, by calling it, a request access permission screen will be shown to the user, so that he can allow/deny
     * it. If it deny, next time this button is clicked, that screen will be shown again
     *
     */
    @OnClick(R.id.request_access_button)
    public void onRequestAccessButtonClick() {
        Log.d(TAG, "onRequestAccessButtonClick()");

        if (!mGoogleApiClient.isConnected()) {
            Log.e(TAG, "onRequestAccessButtonClick() - GoogleApiClient not connected");
            Toast.makeText(getApplicationContext(), "GoogleApiClient not connected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (mGoogleSignInAccount == null) {
            Log.e(TAG, "onRequestAccessButtonClick() - mGoogleSignInAccount is null");
            Toast.makeText(getApplicationContext(), "You must signIn at least once prior to request access", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent res = new Intent();
        res.addCategory("account:" + mGoogleSignInAccount.getEmail());
        res.addCategory("scope:oauth2:https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile");
        res.putExtra("service", "oauth2:https://www.googleapis.com/auth/userinfo.email https://www.googleapis.com/auth/userinfo.profile");
        Bundle extra= new Bundle();
        extra.putString("androidPackageName", getApplicationContext().getPackageName());
        res.putExtra("callerExtras", extra);
        res.putExtra("androidPackageName", getApplicationContext().getPackageName());
        res.putExtra("authAccount", mGoogleSignInAccount.getEmail());

        String mPackage = "com.google.android.gms";
        String mClass = "com.google.android.gms.auth.TokenActivity";
        res.setComponent(new ComponentName(mPackage,mClass));

        // Send the intent. See ActivityResult for the result
        Log.d(TAG, "onRequestAccessButtonClick() - Sending REQUEST_PERMISSION intent...");

        startActivityForResult(res, REQUEST_PERMISSION);
    }

    private void buildGoogleApiClient() {
        Log.d(TAG, "buildGoogleApiClient() - Building the client");

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
               // .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        }
    }

    /**
     * Method called by onConnect() or due a response in the ActivityResult for an user click on the SignIn button.
     *
     * @param result
     */
    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult() - result: " + result.isSuccess());
        if (result.isSuccess()) {

            // Signed in successfully, show authenticated UI.
            mGoogleSignInAccount = result.getSignInAccount();

            Set<Scope> grantedScopes = mGoogleSignInAccount.getGrantedScopes();
            Log.d(TAG, "handleSignInResult() - GrantedScopes: " + grantedScopes);

            tvPersonId.setText("*********************************");
            tvPersonDisplayName.setText(mGoogleSignInAccount.getDisplayName());
            tvPersonGivenName.setText(mGoogleSignInAccount.getGivenName());
            tvPersonFamilyName.setText(mGoogleSignInAccount.getFamilyName());
            tvPersonEmail.setText(mGoogleSignInAccount.getEmail());

            // Uri personPhoto = acct.getPhotoUrl();
            //tvPersonDisplayName.setText(acct.getDisplayName());

            btnSignIn.setEnabled(false);
            btnSignOut.setEnabled(true);
            btnRevokeAccess.setEnabled(true);
            btnRequestAccess.setEnabled(true);

        } else {
            cleanUp();

            btnSignIn.setEnabled(true);
            btnSignOut.setEnabled(false);
            btnRevokeAccess.setEnabled(false);
            btnRequestAccess.setEnabled(false);
        }
    }

    private void cleanUp() {

        // Signed out, show unauthenticated UI.
        tvPersonId.setText("");
        tvPersonDisplayName.setText("");
        tvPersonGivenName.setText("");
        tvPersonFamilyName.setText("");
        tvPersonEmail.setText("");
    }
}
